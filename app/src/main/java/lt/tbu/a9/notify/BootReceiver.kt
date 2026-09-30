package lt.tbu.a9.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lt.tbu.a9.A9App

/** Po perkrovimo atkuria Quick Launch pranešimą (Application konteineris jau paleidžia atnaujinimą; čia laukiam sąrašo). */
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
