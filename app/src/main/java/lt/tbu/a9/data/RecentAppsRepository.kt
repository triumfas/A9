package lt.tbu.a9.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Neseniai naudotos programos visame telefone (paketas → paskutinio atidarymo laikas).
 * Android neleidžia skaityti tikro Recents sąrašo, todėl naudojamas Usage access (UsageStatsManager).
 */
class RecentAppsRepository(private val context: Context, private val scope: CoroutineScope) {
    private val usm = context.getSystemService(UsageStatsManager::class.java)
    private val _recent = MutableStateFlow<Map<String, Long>>(emptyMap())
    val recent: StateFlow<Map<String, Long>> = _recent

    /** Ar vartotojas įjungė Usage access šiai programai (specialus leidimas). */
    fun hasAccess(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) { _recent.value = if (hasAccess()) load() else emptyMap() }
    }

    private fun load(): Map<String, Long> {
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - WINDOW_MS, now)
        val e = UsageEvents.Event()
        val map = HashMap<String, Long>()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED && e.packageName != context.packageName) {
                map[e.packageName] = maxOf(map[e.packageName] ?: 0L, e.timeStamp)
            }
        }
        return map
    }

    private companion object {
        const val WINDOW_MS = 3L * 24 * 3600 * 1000
    }
}
