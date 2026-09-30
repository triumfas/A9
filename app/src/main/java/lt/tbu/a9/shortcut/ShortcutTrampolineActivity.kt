package lt.tbu.a9.shortcut

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.UserManager
import lt.tbu.a9.A9App
import lt.tbu.a9.data.AppItem

/** Be UI: paleidžia programėlę per LauncherApps (veikia ir darbo profiliui), tada užsidaro. */
class ShortcutTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val comp = ComponentName.unflattenFromString(intent.getStringExtra(EXTRA_COMPONENT).orEmpty())
            val serial = intent.getLongExtra(EXTRA_USER_SERIAL, -1)
            val user = getSystemService(UserManager::class.java).getUserForSerialNumber(serial)
            if (comp != null && user != null) {
                getSystemService(LauncherApps::class.java).startMainActivity(comp, user, null, null)
                (application as A9App).container.usage.recordLaunch("${comp.flattenToShortString()}@$serial")
            }
        } catch (e: Exception) {
            // Programėlė išdiegta ar nebepasiekiama – tiesiog užsidarome.
        }
        finish()
    }

    companion object {
        private const val EXTRA_COMPONENT = "component"
        private const val EXTRA_USER_SERIAL = "user_serial"

        fun intent(context: Context, app: AppItem): Intent = intent(context, app.component, app.userSerial)

        fun intent(context: Context, component: ComponentName, userSerial: Long): Intent =
            Intent(context, ShortcutTrampolineActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                // Unikalus data, kad skirtingi PendingIntent nesusijungtų.
                .setData(Uri.parse("a9://launch/${component.flattenToShortString()}/$userSerial"))
                .putExtra(EXTRA_COMPONENT, component.flattenToString())
                .putExtra(EXTRA_USER_SERIAL, userSerial)
    }
}
