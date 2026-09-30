package lt.tbu.a9.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.provider.ContactsContract
import android.util.LruCache
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import lt.tbu.a9.data.AppItem
import lt.tbu.a9.data.AppRepository
import lt.tbu.a9.data.ContactItem
import lt.tbu.a9.data.Item

/** Icon loading with an LRU cache. [version] increases when the icon pack changes. */
class IconLoader(private val context: Context, private val apps: AppRepository) {
    private val cache = LruCache<String, Bitmap>(300)
    @Volatile private var pack: IconPack? = null
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    val sizePx: Int = (56 * context.resources.displayMetrics.density * 1.5f).toInt()

    /** Disk icon cache: after a force close icons are read from PNG files (~ms) instead of being re-rendered by the system. */
    private val diskDir = File(context.cacheDir, "icons").apply { mkdirs() }
    private fun safeName(s: String) = s.replace(Regex("[^A-Za-z0-9._@-]"), "_")
    private fun fileFor(item: AppItem) = File(diskDir, safeName(item.id) + ".png")

    private fun readDisk(item: AppItem): Bitmap? {
        val f = fileFor(item)
        if (!f.exists()) return null
        // The app may have been updated while the process was dead – then the file is stale.
        val updated = runCatching { context.packageManager.getPackageInfo(item.packageName, 0).lastUpdateTime }.getOrDefault(0L)
        if (f.lastModified() < updated) { f.delete(); return null }
        return runCatching { BitmapFactory.decodeFile(f.path) }.getOrNull()
    }

    private fun writeDisk(item: AppItem, bmp: Bitmap) {
        runCatching { fileFor(item).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }

    /** Evicts only icons of the given packages from the cache (after an app update); the rest stay – no flicker. */
    fun invalidatePackages(packages: Set<String>) {
        // Cache key – “package/class@profile”.
        val diskStale = diskDir.listFiles()?.filter { f -> packages.any { f.name.startsWith(safeName(it) + "_") } }.orEmpty()
        diskStale.forEach { it.delete() }
        val stale = cache.snapshot().keys.filter { key -> packages.any { key.startsWith("$it/") } }
        if (stale.isEmpty() && diskStale.isEmpty()) return
        stale.forEach { cache.remove(it) }
        _version.value++
    }

    fun setPack(packageName: String?) {
        if (packageName == pack?.packageName) return
        pack = packageName?.let { runCatching { IconPack(context, it) }.getOrNull() }
        cache.evictAll()
        diskDir.listFiles()?.forEach { it.delete() }
        _version.value++
    }

    suspend fun load(item: Item): Bitmap = cache.get(item.id) ?: withContext(Dispatchers.IO) {
        val bmp = when (item) {
            is AppItem -> readDisk(item) ?: loadApp(item).also {
                // Write only the real icon to disk (not the temporary fallback while the list is still loading from the cache).
                if (pack != null || apps.activityInfo(item.id) != null) writeDisk(item, it)
            }
            is ContactItem -> loadContact(item)
        }
        cache.put(item.id, bmp)
        bmp
    }

    private fun loadApp(item: AppItem): Bitmap {
        val drawable: Drawable? = pack?.drawableFor(item.packageName, item.component.className)
            ?: apps.activityInfo(item.id)?.getBadgedIcon(0)
            ?: runCatching { context.packageManager.getApplicationIcon(item.packageName) }.getOrNull()
        return drawable?.toBitmap(sizePx) ?: initials(item.label)
    }

    private fun loadContact(item: ContactItem): Bitmap {
        val uri = ContactsContract.Contacts.getLookupUri(item.contactId, item.lookupKey)
        val photo = runCatching {
            ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, uri, false)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        return if (photo != null) circle(photo) else initials(item.label, round = true)
    }

    private fun Drawable.toBitmap(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        setBounds(0, 0, size, size)
        draw(c)
        return bmp
    }

    private fun circle(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p)
        p.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        c.drawBitmap(Bitmap.createScaledBitmap(src, sizePx, sizePx, true), 0f, 0f, p)
        return out
    }

    private fun initials(label: String, round: Boolean = true): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val hue = (label.hashCode().mod(360)).toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.45f, 0.65f)) }
        if (round) c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p) else c.drawRoundRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), sizePx / 5f, sizePx / 5f, p)
        val text = label.trim().split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = sizePx * 0.42f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        c.drawText(text, sizePx / 2f, sizePx / 2f - (tp.descent() + tp.ascent()) / 2f, tp)
        return out
    }
}
