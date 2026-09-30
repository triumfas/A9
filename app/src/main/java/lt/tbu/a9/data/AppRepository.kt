package lt.tbu.a9.data

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Įdiegtų programėlių sąrašas (visi profiliai), atsinaujina per [LauncherApps.Callback]. */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps

    @Volatile private var infos: Map<String, LauncherActivityInfo> = emptyMap()
    private var refreshJob: Job? = null

    /** Sąrašo kopija diske: po „force close“ rodome ją iškart, kol LauncherApps dar kraunasi. */
    private val cacheFile = File(context.filesDir, "apps_cache.json")
    private var lastSaved: String? = null

    /** Paketai, pasikeitę nuo paskutinio sąrašo perkrovimo (atnaujinta / pridėta) – jų ikonas verta perkrauti. */
    private val changedPackages: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet())
    var onPackagesUpdated: ((Set<String>) -> Unit)? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = scheduleRefresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) { changedPackages += packageName; scheduleRefresh() }
        override fun onPackageChanged(packageName: String, user: UserHandle) { changedPackages += packageName; scheduleRefresh() }
        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = scheduleRefresh()
        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = scheduleRefresh()
    }

    fun start() {
        launcherApps.registerCallback(callback)
        scope.launch(Dispatchers.IO) {
            val cached = readCache()
            if (cached.isNotEmpty() && _apps.value.isEmpty()) _apps.value = cached
        }
        scheduleRefresh(0)
    }

    fun activityInfo(id: String): LauncherActivityInfo? = infos[id]

    /** Debounce: kelios pakeitimų žinutės iš eilės → vienas perkrovimas. */
    fun scheduleRefresh(delayMs: Long = 300) {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            delay(delayMs)
            loadNow()
        }
    }

    private fun saveCache(list: List<AppItem>) {
        try {
            val arr = JSONArray()
            list.forEach {
                arr.put(
                    JSONObject().put("l", it.label).put("c", it.component.flattenToString())
                        .put("u", it.userSerial).put("s", it.isSystem).put("w", it.isWorkProfile),
                )
            }
            val json = arr.toString()
            if (json == lastSaved) return
            val tmp = File(cacheFile.path + ".tmp")
            tmp.writeText(json)
            tmp.renameTo(cacheFile)
            lastSaved = json
        } catch (e: Exception) {
            // Talpykla nėra kritinė.
        }
    }

    private fun readCache(): List<AppItem> = try {
        if (!cacheFile.exists()) emptyList() else {
            val text = cacheFile.readText()
            lastSaved = text
            val arr = JSONArray(text)
            val out = ArrayList<AppItem>(arr.length())
            val myUser = Process.myUserHandle()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val comp = ComponentName.unflattenFromString(o.getString("c")) ?: continue
                val serial = o.getLong("u")
                val user = userManager.getUserForSerialNumber(serial) ?: continue
                out += AppItem(appId(comp, serial), o.getString("l"), comp, user, serial, o.getBoolean("s"), user != myUser)
            }
            out
        }
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun loadNow() = withContext(Dispatchers.IO) {
        val self = context.packageName
        val myUser = Process.myUserHandle()
        val newInfos = HashMap<String, LauncherActivityInfo>()
        val list = ArrayList<AppItem>()
        for (user in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(user)
            for (info in launcherApps.getActivityList(null, user)) {
                if (info.componentName.packageName == self) continue
                val id = appId(info.componentName, serial)
                newInfos[id] = info
                list += AppItem(
                    id = id,
                    label = info.label?.toString().orEmpty().ifBlank { info.componentName.packageName },
                    component = info.componentName,
                    user = user,
                    userSerial = serial,
                    isSystem = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    isWorkProfile = user != myUser,
                )
            }
        }
        list.sortBy { it.label.lowercase() }
        infos = newInfos
        saveCache(list)
        _apps.value = list
        val changed = synchronized(changedPackages) { changedPackages.toSet().also { changedPackages.clear() } }
        if (changed.isNotEmpty()) onPackagesUpdated?.invoke(changed)
    }
}
