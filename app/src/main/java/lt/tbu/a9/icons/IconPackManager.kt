package lt.tbu.a9.icons

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class IconPackInfo(val packageName: String, val label: String)

/** A loaded icon pack: component → drawable name (ADW/Nova/Go appfilter.xml). */
class IconPack(private val context: Context, val packageName: String) {
    private val res = context.packageManager.getResourcesForApplication(packageName)
    private val byComponent = HashMap<String, String>()
    private val byPackage = HashMap<String, String>()

    init { parse() }

    fun drawableFor(pkg: String, cls: String): Drawable? {
        val name = byComponent["$pkg/$cls"] ?: byPackage[pkg] ?: return null
        val id = res.getIdentifier(name, "drawable", packageName)
        if (id == 0) return null
        return try { res.getDrawableForDensity(id, context.resources.displayMetrics.densityDpi, null) } catch (e: Exception) { null }
    }

    private fun parse() {
        try {
            val parser: XmlPullParser = run {
                val resId = res.getIdentifier("appfilter", "xml", packageName)
                if (resId != 0) res.getXml(resId)
                else XmlPullParserFactory.newInstance().newPullParser().also {
                    val assets = context.createPackageContext(packageName, 0).assets
                    it.setInput(assets.open("appfilter.xml"), null)
                }
            }
            var ev = parser.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && parser.name == "item") {
                    val comp = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    val m = comp?.let { Regex("ComponentInfo\\{([^/]+)/([^}]+)}").find(it) }
                    if (m != null && drawable != null) {
                        val pkg = m.groupValues[1]
                        val cls = m.groupValues[2].let { if (it.startsWith(".")) pkg + it else it }
                        byComponent["$pkg/$cls"] = drawable
                        byPackage.putIfAbsent(pkg, drawable)
                    }
                }
                ev = parser.next()
            }
        } catch (e: Exception) {
            // A broken or unsupported pack – the default icons are used.
        }
    }
}

object IconPackManager {
    private val actions = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME", "com.gau.go.launcherex.theme")

    fun installed(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        return actions.flatMap { a -> pm.queryIntentActivities(Intent(a), PackageManager.GET_META_DATA) }
            .map { it.activityInfo.packageName }.distinct()
            .mapNotNull { pkg ->
                runCatching { IconPackInfo(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()) }.getOrNull()
            }.sortedBy { it.label.lowercase() }
    }
}
