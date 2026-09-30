package lt.tbu.a9.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import lt.tbu.a9.data.ThemeMode

/** Spalvų paletė: tamsūs/šviesūs klavišai su oranžiniu akcentu. */
data class A9Colors(
    val background: Color,
    val key: Color,
    val text: Color,
    val subText: Color,
    val accent: Color,
    val menu: Color,
    val dark: Boolean,
)

val DefaultAccent = Color(0xFFFF8A00)

private val DarkColors = A9Colors(Color(0xFF181818), Color(0xFF2A2A2A), Color(0xFFF2F2F2), Color(0xFF9A9A9A), DefaultAccent, Color(0xFF333333), true)
private val LightColors = A9Colors(Color(0xFFF4F4F4), Color(0xFFFFFFFF), Color(0xFF202020), Color(0xFF777777), DefaultAccent, Color(0xFFFFFFFF), false)
private val TransparentColors = A9Colors(Color(0xB3000000), Color(0x40FFFFFF), Color(0xFFFFFFFF), Color(0xFFCCCCCC), DefaultAccent, Color(0xF0303030), true)

val LocalA9Colors = staticCompositionLocalOf { DarkColors }

/** Ar naudoti vibracijos atsaką (nustatymuose įjungiama/išjungiama). */
val LocalHaptics = staticCompositionLocalOf { true }

private fun base(mode: ThemeMode) = when (mode) {
    ThemeMode.DARK -> DarkColors
    ThemeMode.LIGHT -> LightColors
    ThemeMode.TRANSPARENT -> TransparentColors
}

/** Temos numatytasis fonas (spalva ir permatomumas) – nustatymų pradinėms reikšmėms. */
fun baseBackground(mode: ThemeMode): Color = base(mode).background

/**
 * [bgColor] != 0 – vartotojo fono spalva (ARGB); [bgAlpha] >= 0 – vartotojo permatomumas (0..1);
 * [accent] != 0 – vartotojo akcento spalva. Tekstas pritaikomas pagal pasirinktos fono spalvos šviesumą.
 */
@Composable
fun A9Theme(mode: ThemeMode, bgColor: Int = 0, bgAlpha: Float = -1f, accent: Int = 0, content: @Composable () -> Unit) {
    var c = base(mode)
    if (accent != 0) c = c.copy(accent = Color(accent).copy(alpha = 1f))
    if (bgColor != 0 || bgAlpha >= 0f) {
        val rgb = if (bgColor != 0) Color(bgColor).copy(alpha = 1f) else c.background.copy(alpha = 1f)
        val a = if (bgAlpha >= 0f) bgAlpha else c.background.alpha
        c = if (bgColor != 0) {
            val dark = rgb.luminance() < 0.5f
            c.copy(
                background = rgb.copy(alpha = a),
                key = if (dark) Color(0x33FFFFFF) else Color(0x14000000),
                text = if (dark) Color(0xFFF2F2F2) else Color(0xFF202020),
                subText = if (dark) Color(0xFF9A9A9A) else Color(0xFF666666),
                menu = if (dark) Color(0xFF333333) else Color(0xFFFFFFFF),
                dark = dark,
            )
        } else c.copy(background = rgb.copy(alpha = a))
    }
    val scheme = if (c.dark) darkColorScheme(primary = c.accent, surface = c.menu, background = c.background, onSurface = c.text)
    else lightColorScheme(primary = c.accent, surface = c.menu, background = c.background, onSurface = c.text)
    CompositionLocalProvider(LocalA9Colors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
