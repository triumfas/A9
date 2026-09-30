package lt.tbu.a9.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import lt.tbu.a9.search.UsageInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class UsageState(
    val stats: Map<String, Pair<Int, Long>> = emptyMap(), // id → (launch count, last launch time)
    val pinned: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val actions: Map<String, Int> = emptyMap(), // long-press action usage counts (for ordering the menu)
) {
    fun info(id: String): UsageInfo {
        val s = stats[id]
        return UsageInfo(s?.first ?: 0, s?.second ?: 0L, id in pinned)
    }
}

/** Launch statistics, pinned and hidden apps – a single JSON file. */
class UsageRepository(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "usage.json")
    private val writeLock = Mutex()
    private val _state = MutableStateFlow(UsageState())
    val state: StateFlow<UsageState> = _state

    init { _state.value = read() }

    fun recordLaunch(id: String) = mutate { s ->
        val old = s.stats[id]?.first ?: 0
        s.copy(stats = s.stats + (id to (old + 1 to System.currentTimeMillis())))
    }

    fun togglePin(id: String) = mutate { s -> s.copy(pinned = if (id in s.pinned) s.pinned - id else s.pinned + id) }
    fun hide(id: String) = mutate { s -> s.copy(hidden = s.hidden + id, pinned = s.pinned - id) }
    fun unhideAll() = mutate { s -> s.copy(hidden = emptySet()) }
    fun unhide(id: String) = mutate { s -> s.copy(hidden = s.hidden - id) }
    fun clearStats() = mutate { s -> s.copy(stats = emptyMap(), actions = emptyMap()) }
    fun recordAction(key: String) = mutate { s -> s.copy(actions = s.actions + (key to ((s.actions[key] ?: 0) + 1))) }

    /**
     * Removes statistics of uninstalled apps. Safe: only entries whose apps are not in the list right now
     * AND that have been unused for at least [minAgeMs] are deleted – a temporarily unavailable app (SD card, work profile) loses nothing.
     */
    fun pruneMissingApps(existing: Set<String>, minAgeMs: Long = 30L * 24 * 3600 * 1000) {
        val cutoff = System.currentTimeMillis() - minAgeMs
        val stale = _state.value.stats.filter { (id, v) -> !id.startsWith("c:") && id !in existing && v.second < cutoff }.keys
        if (stale.isEmpty()) return
        mutate { it.copy(stats = it.stats - stale) }
    }

    private fun mutate(f: (UsageState) -> UsageState) {
        _state.update(f)
        val snapshot = _state.value
        scope.launch(Dispatchers.IO) { writeLock.withLock { write(snapshot) } }
    }

    private fun read(): UsageState = try {
        if (!file.exists()) UsageState() else {
            val o = JSONObject(file.readText())
            val stats = HashMap<String, Pair<Int, Long>>()
            o.optJSONObject("stats")?.let { j -> j.keys().forEach { k -> val a = j.getJSONArray(k); stats[k] = a.getInt(0) to a.getLong(1) } }
            val actions = HashMap<String, Int>()
            o.optJSONObject("actions")?.let { j -> j.keys().forEach { k -> actions[k] = j.getInt(k) } }
            UsageState(stats, o.strings("pinned"), o.strings("hidden"), actions)
        }
    } catch (e: Exception) {
        UsageState()
    }

    private fun write(s: UsageState) {
        val o = JSONObject()
        o.put("stats", JSONObject().also { j -> s.stats.forEach { (k, v) -> j.put(k, JSONArray().put(v.first).put(v.second)) } })
        o.put("pinned", JSONArray(s.pinned.toList()))
        o.put("hidden", JSONArray(s.hidden.toList()))
        o.put("actions", JSONObject().also { j -> s.actions.forEach { (k, v) -> j.put(k, v) } })
        val tmp = File(file.path + ".tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(file)
    }

    private fun JSONObject.strings(key: String): Set<String> {
        val a = optJSONArray(key) ?: return emptySet()
        return (0 until a.length()).map { a.getString(it) }.toSet()
    }
}
