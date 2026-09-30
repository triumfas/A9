package lt.tbu.a9.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.first
import lt.tbu.a9.R
import lt.tbu.a9.data.AppItem
import lt.tbu.a9.data.AppRepository
import lt.tbu.a9.data.SettingsRepository
import lt.tbu.a9.data.UsageRepository
import lt.tbu.a9.icons.IconLoader
import lt.tbu.a9.search.SearchEngine
import lt.tbu.a9.shortcut.ShortcutTrampolineActivity
import lt.tbu.a9.ui.DialerActivity

/** Nuolatinis pranešimas su dažniausiai naudojamomis programėlėmis. */
class QuickLaunchNotifier(
    private val context: Context,
    private val apps: AppRepository,
    private val icons: IconLoader,
    private val usage: UsageRepository,
    private val settings: SettingsRepository,
) {
    private val nm = context.getSystemService(NotificationManager::class.java)
    private val slots = intArrayOf(R.id.q0, R.id.q1, R.id.q2, R.id.q3, R.id.q4, R.id.q5)

    suspend fun update() {
        val enabled = settings.flow.first().quickLaunch
        val granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!enabled || !granted) {
            nm.cancel(ID)
            return
        }
        val all = apps.apps.value.filter { it.id !in usage.state.value.hidden }
        if (all.isEmpty()) return
        val now = System.currentTimeMillis()
        val top: List<AppItem> = all.sortedByDescending { SearchEngine.usageScore(usage.state.value.info(it.id), now) }.take(slots.size)

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.quick_launch), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
        )
        val views = RemoteViews(context.packageName, R.layout.notif_quick)
        slots.forEachIndexed { i, viewId ->
            val app = top.getOrNull(i)
            if (app == null) {
                views.setViewVisibility(viewId, android.view.View.GONE)
            } else {
                val bmp: Bitmap = Bitmap.createScaledBitmap(icons.load(app), 96, 96, true)
                views.setViewVisibility(viewId, android.view.View.VISIBLE)
                views.setImageViewBitmap(viewId, bmp)
                views.setContentDescription(viewId, app.label)
                val pi = PendingIntent.getActivity(
                    context, i, ShortcutTrampolineActivity.intent(context, app),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                views.setOnClickPendingIntent(viewId, pi)
            }
        }
        val open = PendingIntent.getActivity(
            context, 100, android.content.Intent(context, DialerActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_a9)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        nm.notify(ID, n)
    }

    private companion object {
        const val ID = 1
        const val CHANNEL = "quick_launch"
    }
}
