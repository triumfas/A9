package lt.tbu.a9.ui.actions

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import lt.tbu.a9.data.AppItem
import lt.tbu.a9.data.AppRepository
import lt.tbu.a9.data.ContactItem
import lt.tbu.a9.data.UsageRepository
import lt.tbu.a9.icons.IconLoader
import lt.tbu.a9.shortcut.ShortcutTrampolineActivity

/** All launch and long-press actions. Does not change UI state, only calls into the system. */
class AppActions(
    private val context: Context,
    private val apps: AppRepository,
    private val icons: IconLoader,
    private val usage: UsageRepository,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    fun launch(app: AppItem): Boolean = try {
        launcherApps.startMainActivity(app.component, app.user, null, null)
        usage.recordLaunch(app.id)
        true
    } catch (e: Exception) {
        apps.scheduleRefresh(0) // the app was most likely uninstalled
        false
    }

    fun appInfo(app: AppItem): Boolean = safe {
        launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
    }

    /** A non-root app cannot stop another one, so this opens the App info screen with “Force stop”. */
    fun forceStop(app: AppItem) = appInfo(app)

    fun actionCounts(): Map<String, Int> = usage.state.value.actions
    fun recordAction(key: String) = usage.recordAction(key)

    fun uninstall(app: AppItem): Boolean = safe {
        val i = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
            .putExtra(Intent.EXTRA_USER, app.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }

    fun playStore(app: AppItem): Boolean {
        val pkg = app.packageName
        return safe { context.startActivity(view("market://details?id=$pkg")) } ||
            safe { context.startActivity(view("https://play.google.com/store/apps/details?id=$pkg")) }
    }

    suspend fun pinToHome(app: AppItem): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val bmp = icons.load(app)
        val info = ShortcutInfoCompat.Builder(context, "app:" + app.id)
            .setShortLabel(app.label)
            .setIcon(IconCompat.createWithBitmap(bmp))
            .setIntent(ShortcutTrampolineActivity.intent(context, app))
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    fun call(contact: ContactItem, number: String): Boolean {
        val granted = context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val action = if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val ok = safe { context.startActivity(Intent(action, Uri.fromParts("tel", number, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        if (ok) usage.recordLaunch(contact.id)
        return ok
    }

    fun sms(contact: ContactItem, number: String): Boolean = safe {
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openContact(contact: ContactItem): Boolean {
        val ok = safe {
            val uri = ContactsContract.Contacts.getLookupUri(contact.contactId, contact.lookupKey)
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        if (ok) usage.recordLaunch(contact.id)
        return ok
    }

    private fun view(uri: String) = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private inline fun safe(block: () -> Unit): Boolean = try {
        block(); true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    } catch (e: IllegalStateException) {
        false
    }
}
