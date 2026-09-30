package lt.tbu.a9.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lt.tbu.a9.AppContainer
import lt.tbu.a9.data.AppItem
import lt.tbu.a9.data.ContactItem
import lt.tbu.a9.data.Item
import lt.tbu.a9.data.Settings
import lt.tbu.a9.data.ThemeMode
import lt.tbu.a9.search.SearchEngine
import lt.tbu.a9.search.SearchEntry
import lt.tbu.a9.search.Tokenizer

data class DialerUiState(
    val query: String = "",
    val results: List<Item> = emptyList(),
    val pinned: Set<String> = emptySet(),
    val hiddenCount: Int = 0,
    val settings: Settings = Settings(),
    val showSettings: Boolean = false,
    val editLayout: Boolean = false,
    val showAppearance: Boolean = false,
)

private data class Inputs(val query: String, val settings: Settings, val showSettings: Boolean, val editLayout: Boolean, val showAppearance: Boolean)

class DialerViewModel(private val c: AppContainer) : ViewModel() {
    // Settings are read immediately (~ms) so the first frame does not jump from the default theme to the chosen one.
    private val initialSettings: Settings = runBlocking { c.settings.flow.first() }
    private val query = MutableStateFlow("")
    private val showSettings = MutableStateFlow(false)
    private val editLayout = MutableStateFlow(false)
    private val showAppearance = MutableStateFlow(false)
    private val _showHidden = MutableStateFlow(false)
    val showHidden: StateFlow<Boolean> = _showHidden
    val allApps: StateFlow<List<AppItem>> = c.apps.apps
    val hiddenIds: StateFlow<Set<String>> = c.usage.state.map { it.hidden }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    private val engine = SearchEngine()
    private var indexedApps: List<AppItem>? = null
    private var indexedContacts: List<ContactItem>? = null
    private var itemsById: Map<String, Item> = emptyMap()

    val state: StateFlow<DialerUiState> = combine(
        combine(query, c.settings.flow, showSettings, editLayout, showAppearance, ::Inputs),
        c.apps.apps, c.contacts.contacts, c.usage.state, c.recents.recent,
    ) { input, apps, contacts, usage, sysRecent ->
        rebuildIndexIfNeeded(apps, contacts)
        val results: List<Item> = if (input.query.isEmpty()) {
            // Empty query: pinned → last launched via A9 → recently used (on the phone, if enabled)
            // → the rest by name.
            val visible = apps.filter { it.id !in usage.hidden }
            val sys = if (input.settings.recentApps) sysRecent else emptyMap()
            val lastA9 = visible.filter { (usage.stats[it.id]?.second ?: 0L) > 0L }.maxByOrNull { usage.stats[it.id]!!.second }?.id
            fun lastUsed(a: AppItem) = maxOf(usage.stats[a.id]?.second ?: 0L, sys[a.packageName] ?: 0L)
            visible.sortedWith(
                compareByDescending<AppItem> { it.id in usage.pinned }
                    .thenByDescending { it.id == lastA9 }
                    .thenByDescending { lastUsed(it) }
                    .thenBy { it.label.lowercase() },
            )
        } else {
            engine.search(input.query, usage::info, limit = 80).asSequence()
                .filter { it !in usage.hidden }
                .mapNotNull { itemsById[it] }
                .toList()
        }
        DialerUiState(input.query, results, usage.pinned, usage.hidden.size, input.settings, input.showSettings, input.editLayout, input.showAppearance)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DialerUiState(settings = initialSettings))

    private fun rebuildIndexIfNeeded(apps: List<AppItem>, contacts: List<ContactItem>) {
        if (apps === indexedApps && contacts === indexedContacts) return
        indexedApps = apps
        indexedContacts = contacts
        val entries = ArrayList<SearchEntry>(apps.size + contacts.size)
        val map = HashMap<String, Item>(apps.size + contacts.size)
        apps.forEach { entries += SearchEntry(it.id, Tokenizer.forApp(it.label, it.packageName), it.label); map[it.id] = it }
        contacts.forEach { entries += SearchEntry(it.id, Tokenizer.forContact(it.label, it.phones), it.label); map[it.id] = it }
        engine.setEntries(entries)
        itemsById = map
    }

    fun type(ch: Char) = query.update { if (it.length < 30) it + ch else it }
    fun backspace() = query.update { it.dropLast(1) }
    fun clear() { query.value = "" }
    fun reset() { query.value = ""; showSettings.value = false; editLayout.value = false; showAppearance.value = false; _showHidden.value = false }
    fun setHiddenVisible(v: Boolean) { _showHidden.value = v }
    fun setAppearanceVisible(v: Boolean) { showAppearance.value = v }
    fun startEditLayout() { showSettings.value = false; editLayout.value = true }
    fun endEditLayout() { editLayout.value = false }
    fun savePanel(x: Float, y: Float, scale: Float) = viewModelScope.launch { c.settings.setPanel(x, y, scale) }
    fun setBackground(color: Int, alpha: Float) = viewModelScope.launch { c.settings.setBackground(color, alpha) }
    fun setAccent(color: Int) = viewModelScope.launch { c.settings.setAccent(color) }
    fun setGrid(columns: Int, rows: Int) = viewModelScope.launch { c.settings.setGrid(columns, rows) }
    fun setSettingsVisible(v: Boolean) { showSettings.value = v }

    fun togglePin(id: String) = c.usage.togglePin(id)
    fun hide(id: String) = c.usage.hide(id)
    fun unhide(id: String) = c.usage.unhide(id)
    fun unhideAll() = c.usage.unhideAll()
    fun clearStats() = c.usage.clearStats()
    fun reindex() { c.apps.scheduleRefresh(0); c.contacts.scheduleRefresh(0) }

    fun setTheme(v: ThemeMode) = viewModelScope.launch { c.settings.setTheme(v) }
    fun setIconPack(v: String?) = viewModelScope.launch { c.settings.setIconPack(v) }
    fun setContacts(v: Boolean) = viewModelScope.launch { c.settings.setContacts(v) }
    fun setQuickLaunch(v: Boolean) = viewModelScope.launch { c.settings.setQuickLaunch(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { c.settings.setHaptics(v) }
    fun setRecentApps(v: Boolean) = viewModelScope.launch { c.settings.setRecentApps(v) }
    fun hasUsageAccess() = c.recents.hasAccess()
    fun refreshRecents() = c.recents.refresh()

    class Factory(private val c: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DialerViewModel(c) as T
    }
}
