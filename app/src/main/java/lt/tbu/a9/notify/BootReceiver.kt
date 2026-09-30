package lt.tbu.a9.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lt.tbu.a9.A9App

/** Restores the Quick Launch notification after a reboot (the Application container already triggers an update; here we wait for the list). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val c = (context.applicationContext as A9App).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                c.apps.loadNow()
                c.notifier.update()
            } finally {
                pending.finish()
            }
        }
    }
}
