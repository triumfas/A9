package lt.tbu.a9.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.tbu.a9.R
import lt.tbu.a9.data.ThemeMode
import lt.tbu.a9.ui.DialerUiState
import lt.tbu.a9.ui.DialerViewModel
import lt.tbu.a9.ui.keyboard.KeyboardActions
import lt.tbu.a9.ui.keyboard.NumpadKeyboard
import lt.tbu.a9.ui.theme.A9Theme
import lt.tbu.a9.ui.theme.DefaultAccent
import lt.tbu.a9.ui.theme.LocalA9Colors
import lt.tbu.a9.ui.theme.LocalHaptics
import lt.tbu.a9.ui.theme.baseBackground
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** HSV color state (the wheel changes hue/saturation, the slider – brightness). */
@Stable
private class Hsv(argb: Int) {
    var h by mutableFloatStateOf(0f)
    var s by mutableFloatStateOf(0f)
    var v by mutableFloatStateOf(0f)

    init {
        val out = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, out)
        h = out[0]; s = out[1]; v = out[2]
    }

    fun argb(): Int = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
}

private enum class Editing { NONE, BACKGROUND, ACCENT }

/** Appearance: a live preview on top, below – theme, background, accent, apps in the window. */
@Composable
fun AppearanceScreen(vm: DialerViewModel, state: DialerUiState, onBack: () -> Unit) {
    val c = LocalA9Colors.current
    val s = state.settings
    val base = baseBackground(s.theme)

    // Drafts: the preview reacts immediately and the value is saved when the wheel / slider is released.
    val bg = remember(s.bgColor, s.theme) { Hsv(if (s.bgColor != 0) s.bgColor else base.copy(alpha = 1f).toArgb()) }
    var bgCustom by remember(s.bgColor, s.theme) { mutableStateOf(s.bgColor != 0) }
    var alpha by remember(s.bgAlpha, s.theme) { mutableFloatStateOf(if (s.bgAlpha >= 0f) s.bgAlpha else base.alpha) }
    var alphaSet by remember(s.bgAlpha, s.theme) { mutableStateOf(s.bgAlpha >= 0f) }
    val accent = remember(s.accent) { Hsv(if (s.accent != 0) s.accent else DefaultAccent.toArgb()) }
    var accentCustom by remember(s.accent) { mutableStateOf(s.accent != 0) }
    var editing by remember { mutableStateOf(Editing.NONE) }

    val bgArgb = if (bgCustom) bg.argb() else 0
    val alphaValue = if (alphaSet) alpha else -1f
    val accentArgb = if (accentCustom) accent.argb() else 0

    fun saveBg() = vm.setBackground(if (bgCustom) bg.argb() else 0, if (alphaSet) alpha else -1f)
    fun saveAccent() = vm.setAccent(if (accentCustom) accent.argb() else 0)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.clickable(onClick = onBack).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←  " + stringResource(R.string.appearance), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Light)
        }

        PanelPreview(s.theme, bgArgb, alphaValue, accentArgb, s.columns, s.rows)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Section(R.string.theme)
            ChipRow {
                FilterChip(s.theme == ThemeMode.DARK, { vm.setTheme(ThemeMode.DARK) }, { Text(stringResource(R.string.theme_dark)) })
                FilterChip(s.theme == ThemeMode.LIGHT, { vm.setTheme(ThemeMode.LIGHT) }, { Text(stringResource(R.string.theme_light)) })
                FilterChip(s.theme == ThemeMode.TRANSPARENT, { vm.setTheme(ThemeMode.TRANSPARENT) }, { Text(stringResource(R.string.theme_transparent)) })
            }

            ColorRow(
                title = R.string.bg_section,
                color = Color(if (bgCustom) bg.argb() else base.copy(alpha = 1f).toArgb()),
                expanded = editing == Editing.BACKGROUND,
                onToggle = { editing = if (editing == Editing.BACKGROUND) Editing.NONE else Editing.BACKGROUND },
                onReset = { vm.setBackground(0, -1f) },
            ) {
                ColorWheel(bg, onChange = { bgCustom = true }, onFinished = ::saveBg)
                LabeledSlider(R.string.bg_brightness, bg.v, 0f..1f, { bg.v = it; bgCustom = true }, ::saveBg)
                // Shown as transparency: 0 % = opaque.
                LabeledSlider(R.string.bg_transparency, 1f - alpha, 0f..0.9f, { alpha = 1f - it; alphaSet = true }, ::saveBg, percent = true)
            }

            ColorRow(
                title = R.string.accent_color,
                color = Color(if (accentCustom) accent.argb() else DefaultAccent.toArgb()),
                expanded = editing == Editing.ACCENT,
                onToggle = { editing = if (editing == Editing.ACCENT) Editing.NONE else Editing.ACCENT },
                onReset = { vm.setAccent(0) },
            ) {
                ColorWheel(accent, onChange = { accentCustom = true }, onFinished = ::saveAccent)
                LabeledSlider(R.string.bg_brightness, accent.v, 0f..1f, { accent.v = it; accentCustom = true }, ::saveAccent)
            }

            Section(R.string.columns)
            ChipRow { (3..6).forEach { n -> FilterChip(s.columns == n, { vm.setGrid(n, s.rows) }, { Text(n.toString()) }) } }
            Section(R.string.rows)
            ChipRow { (1..3).forEach { n -> FilterChip(s.rows == n, { vm.setGrid(s.columns, n) }, { Text(n.toString()) }) } }
        }
    }
}

/** A row with a circle of the current color; tapping expands the editor, ↺ restores the default. */
@Composable
private fun ColorRow(
    title: Int,
    color: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    editor: @Composable () -> Unit,
) {
    val c = LocalA9Colors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(title), color = c.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Box(Modifier.size(28.dp).clip(CircleShape).background(color).border(1.dp, c.subText, CircleShape))
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onReset), contentAlignment = Alignment.Center) {
            Text("↺", color = c.text, fontSize = 18.sp)
        }
    }
    if (expanded) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { editor() }
    }
}

/** Color wheel: angle = hue, distance from the center = saturation. A color is picked by dragging or tapping. */
@Composable
private fun ColorWheel(hsv: Hsv, onChange: () -> Unit, onFinished: () -> Unit) {
    var box by remember { mutableStateOf(IntSize.Zero) }
    val change by rememberUpdatedState(onChange)
    val finished by rememberUpdatedState(onFinished)

    fun pick(p: Offset) {
        val r = minOf(box.width, box.height) / 2f
        if (r <= 0f) return
        val dx = p.x - box.width / 2f
        val dy = p.y - box.height / 2f
        hsv.s = (hypot(dx, dy) / r).coerceIn(0f, 1f)
        hsv.h = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360.0) % 360.0).toFloat()
        change()
    }

    Canvas(
        Modifier.padding(vertical = 8.dp).size(220.dp).onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { pick(it) }, onDragEnd = { finished() }, onDragCancel = { finished() }) { ch, _ ->
                    ch.consume(); pick(ch.position)
                }
            }
            .pointerInput(Unit) { detectTapGestures { pick(it); finished() } },
    ) {
        val r = size.minDimension / 2f
        drawCircle(Brush.sweepGradient(List(13) { Color.hsv((it * 30f) % 360f, 1f, 1f) }, center), radius = r)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), center, r), radius = r)
        drawCircle(Color.Black.copy(alpha = 1f - hsv.v), radius = r) // the wheel darkens as brightness decreases
        val a = Math.toRadians(hsv.h.toDouble())
        val thumb = center + Offset(cos(a).toFloat(), sin(a).toFloat()) * (hsv.s * r)
        drawCircle(Color.White, 13.dp.toPx(), thumb, style = Stroke(3.dp.toPx()))
        drawCircle(Color.hsv(hsv.h, hsv.s, hsv.v), 10.dp.toPx(), thumb)
    }
}

@Composable
private fun LabeledSlider(
    label: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
    percent: Boolean = false,
) {
    val c = LocalA9Colors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(label), color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            if (percent) Text("${(value * 100).toInt()} %", color = c.subText, fontSize = 13.sp)
        }
        Slider(value, onChange, valueRange = range, onValueChangeFinished = onFinished)
    }
}

private val Samples = listOf(
    "Bitwarden" to 0xFF1E5EFF, "Shazam" to 0xFF2D7BFF, "Maps" to 0xFF34A853, "Mail" to 0xFFEA4335, "Clock" to 0xFF7E57C2, "Camera" to 0xFF607D8B,
    "Notes" to 0xFFFFB300, "Music" to 0xFFE91E63, "Photos" to 0xFFFF7043, "Files" to 0xFF26A69A, "Weather" to 0xFF29B6F6, "Calendar" to 0xFF5C6BC0,
    "Wallet" to 0xFF43A047, "Phone" to 0xFF00897B, "Chrome" to 0xFFF4511E, "Spotify" to 0xFF1DB954, "Signal" to 0xFF3A76F0, "Teams" to 0xFF6264A7,
)

/** A smaller copy of the real window with the chosen settings (theme, background, accent, columns × rows). */
@Composable
private fun PanelPreview(theme: ThemeMode, bgColor: Int, bgAlpha: Float, accent: Int, columns: Int, rows: Int) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3A5A40), Color(0xFFB08968), Color(0xFF344E75)))),
        contentAlignment = Alignment.BottomCenter,
    ) {
        A9Theme(theme, bgColor, bgAlpha, accent) {
            val c = LocalA9Colors.current
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density * 0.6f, base.fontScale),
                LocalHaptics provides false,
            ) {
                Column(
                    Modifier.padding(12.dp).widthIn(max = 340.dp).clip(RoundedCornerShape(20.dp)).background(c.background)
                        .padding(top = 4.dp),
                ) {
                    for (r in 0 until rows) {
                        Row(Modifier.fillMaxWidth().height(100.dp)) {
                            for (i in 0 until columns) {
                                val (name, color) = Samples[(r * columns + i) % Samples.size]
                                Column(Modifier.weight(1f).padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(Color(color)))
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.Bold)) { append(name.take(2)) }
                                            append(name.drop(2))
                                        },
                                        color = c.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.padding(horizontal = 3.dp).size(width = 18.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(c.accent))
                        repeat(4) {
                            Box(Modifier.padding(horizontal = 3.dp).size(width = 8.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(c.subText.copy(alpha = 0.4f)))
                        }
                    }
                    NumpadKeyboard(KeyboardActions({}, {}, {}, {}))
                }
            }
        }
    }
}
