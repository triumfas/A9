package lt.tbu.a9.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { DARK, LIGHT, TRANSPARENT }

data class Settings(
    val theme: ThemeMode = ThemeMode.DARK,
    val iconPack: String? = null,
    val contactsEnabled: Boolean = false,
    val quickLaunch: Boolean = false,
    val panelX: Float = 0f,
    val panelY: Float = 0.85f,
    val panelScale: Float = 1f,
    val bgColor: Int = 0,          // 0 = the theme default
    val bgAlpha: Float = -1f,      // <0 = the theme default
    val accent: Int = 0,           // 0 = default orange
    val columns: Int = 4,
    val rows: Int = 1,
    val haptics: Boolean = true,
    val recentApps: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val iconPack = stringPreferencesKey("icon_pack")
        val contacts = booleanPreferencesKey("contacts")
        val quickLaunch = booleanPreferencesKey("quick_launch")
        val panelX = floatPreferencesKey("panel_x")
        val panelY = floatPreferencesKey("panel_y")
        val panelScale = floatPreferencesKey("panel_scale")
        val bgColor = intPreferencesKey("bg_color")
        val bgAlpha = floatPreferencesKey("bg_alpha")
        val accent = intPreferencesKey("accent")
        val columns = intPreferencesKey("columns")
        val rows = intPreferencesKey("rows")
        val haptics = booleanPreferencesKey("haptics")
        val recentApps = booleanPreferencesKey("recent_apps")
    }

    val flow: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            iconPack = p[K.iconPack],
            contactsEnabled = p[K.contacts] ?: false,
            quickLaunch = p[K.quickLaunch] ?: false,
            panelX = p[K.panelX] ?: 0f,
            panelY = p[K.panelY] ?: 0.85f,
            panelScale = p[K.panelScale] ?: 1f,
            bgColor = p[K.bgColor] ?: 0,
            bgAlpha = p[K.bgAlpha] ?: -1f,
            accent = p[K.accent] ?: 0,
            columns = (p[K.columns] ?: 4).coerceIn(3, 6),
            rows = (p[K.rows] ?: 1).coerceIn(1, 3),
            haptics = p[K.haptics] ?: true,
            recentApps = p[K.recentApps] ?: false,
        )
    }

    /** Choosing a theme resets custom background settings – otherwise they would completely override the theme background. */
    suspend fun setTheme(v: ThemeMode) = edit {
        it[K.theme] = v.name
        it.remove(K.bgColor)
        it.remove(K.bgAlpha)
    }
    suspend fun setIconPack(v: String?) = edit { if (v == null) it.remove(K.iconPack) else it[K.iconPack] = v }
    suspend fun setContacts(v: Boolean) = edit { it[K.contacts] = v }
    suspend fun setQuickLaunch(v: Boolean) = edit { it[K.quickLaunch] = v }
    suspend fun setHaptics(v: Boolean) = edit { it[K.haptics] = v }
    suspend fun setRecentApps(v: Boolean) = edit { it[K.recentApps] = v }
    suspend fun setBackground(color: Int, alpha: Float) = edit {
        it[K.bgColor] = color
        it[K.bgAlpha] = alpha
    }
    suspend fun setAccent(color: Int) = edit { it[K.accent] = color }
    suspend fun setGrid(columns: Int, rows: Int) = edit {
        it[K.columns] = columns.coerceIn(3, 6)
        it[K.rows] = rows.coerceIn(1, 3)
    }
    suspend fun setPanel(x: Float, y: Float, scale: Float) = edit {
        it[K.panelX] = x.coerceIn(-1f, 1f)
        it[K.panelY] = y.coerceIn(-1f, 1f)
        it[K.panelScale] = scale.coerceIn(0.7f, 1.6f)
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
