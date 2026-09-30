package lt.tbu.a9.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.tbu.a9.R
import lt.tbu.a9.data.AppItem
import lt.tbu.a9.data.ContactItem
import lt.tbu.a9.data.Item
import lt.tbu.a9.data.ThemeMode
import lt.tbu.a9.icons.IconLoader
import lt.tbu.a9.ui.actions.AppActions
import lt.tbu.a9.ui.keyboard.KeyboardActions
import lt.tbu.a9.ui.keyboard.NumpadKeyboard
import lt.tbu.a9.ui.results.ResultPager
import lt.tbu.a9.ui.settings.AppearanceScreen
import lt.tbu.a9.ui.settings.HiddenAppsScreen
import lt.tbu.a9.ui.settings.SettingsScreen
import lt.tbu.a9.ui.theme.A9Theme
import lt.tbu.a9.ui.theme.LocalA9Colors
import lt.tbu.a9.ui.theme.LocalHaptics

private const val MIN_SCALE = 0.7f
private const val MAX_SCALE = 1.6f

/** Plaukiojanti plokštė (ne visas ekranas): padėtį ir dydį galima keisti redagavimo režime. */
@Composable
fun DialerScreen(vm: DialerViewModel, actions: AppActions, icons: IconLoader, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val showHidden by vm.showHidden.collectAsStateWithLifecycle()
    val noRipple = remember { MutableInteractionSource() }

    A9Theme(state.settings.theme, state.settings.bgColor, state.settings.bgAlpha, state.settings.accent) {
      CompositionLocalProvider(LocalHaptics provides state.settings.haptics) {
        val c = LocalA9Colors.current
        val s = state.settings
        val editing = state.editLayout

        // Vietinė būsena, kad tempimas/keitimas būtų sklandus; į DataStore rašoma pabaigus.
        var bx by remember { mutableFloatStateOf(s.panelX) }
        var by by remember { mutableFloatStateOf(s.panelY) }
        var scale by remember { mutableFloatStateOf(s.panelScale) }
        LaunchedEffect(s.panelX, s.panelY, s.panelScale) { bx = s.panelX; by = s.panelY; scale = s.panelScale }
        var panelSize by remember { mutableStateOf(IntSize.Zero) }

        BoxWithConstraints(
            Modifier.fillMaxSize()
                .background(Color.Transparent)
                .clickable(interactionSource = noRipple, indication = null, enabled = !editing, onClick = onClose)
                .systemBarsPadding(),
        ) {
            val containerW = constraints.maxWidth
            val containerH = constraints.maxHeight

            if (state.showSettings || state.showAppearance || showHidden) {
                // Nustatymai visada tamsūs ir nepermatomi (nepriklauso nuo vartotojo fono), tik akcentas – pasirinktas.
                A9Theme(ThemeMode.DARK, accent = s.accent) {
                    val nc = LocalA9Colors.current
                    Box(Modifier.fillMaxSize().background(nc.background).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
                        if (showHidden) {
                            BackHandler { vm.setHiddenVisible(false) }
                            HiddenAppsScreen(vm, icons, onBack = { vm.setHiddenVisible(false) })
                        } else if (state.showAppearance) {
                            BackHandler { vm.setAppearanceVisible(false) }
                            AppearanceScreen(vm, state, onBack = { vm.setAppearanceVisible(false) })
                        } else {
                            BackHandler { vm.setSettingsVisible(false) }
                            SettingsScreen(vm, state, onBack = { vm.setSettingsVisible(false) })
                        }
                    }
                }
            } else {
                BackHandler(enabled = editing || state.query.isNotEmpty()) { if (editing) vm.endEditLayout() else vm.clear() }

                val open: (Item) -> Unit = { item ->
                    val ok = when (item) {
                        is AppItem -> actions.launch(item)
                        is ContactItem -> actions.openContact(item)
                    }
                    if (ok) onClose()
                }
                val kb = KeyboardActions(vm::type, vm::backspace, vm::clear) { vm.setSettingsVisible(true) }
                val base = LocalDensity.current

                // Visas plokštės turinys mastelio keičiamas tankiu: išdėstymas prisitaiko prie lango dydžio.
                CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
                    Column(
                        Modifier.align(BiasAlignment(bx, by))
                            .onSizeChanged { panelSize = it }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .widthIn(max = 340.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.background)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    ) {
                        if (editing) {
                            EditBar(
                                onMinus = { scale = (scale - 0.1f).coerceIn(MIN_SCALE, MAX_SCALE); vm.savePanel(bx, by, scale) },
                                onPlus = { scale = (scale + 0.1f).coerceIn(MIN_SCALE, MAX_SCALE); vm.savePanel(bx, by, scale) },
                                onReset = { bx = 0f; by = 0.85f; scale = 1f; vm.savePanel(bx, by, scale) },
                                onDone = { vm.endEditLayout() },
                                dragModifier = Modifier.pointerInput(containerW, containerH) {
                                    detectDragGestures(onDragEnd = { vm.savePanel(bx, by, scale) }) { change, drag ->
                                        change.consume()
                                        val freeW = containerW - panelSize.width
                                        val freeH = containerH - panelSize.height
                                        if (freeW > 0) bx = (bx + 2f * drag.x / freeW).coerceIn(-1f, 1f)
                                        if (freeH > 0) by = (by + 2f * drag.y / freeH).coerceIn(-1f, 1f)
                                    }
                                },
                            )
                        }
                        ResultPager(
                            state.results, state.pinned, state.query, icons, actions,
                            onOpen = open, onPin = vm::togglePin, onHide = vm::hide, onClear = vm::clear, onLaunched = onClose,
                            columns = s.columns, rows = s.rows,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        NumpadKeyboard(kb)
                    }
                }
            }
        }
      }
    }
}

@Composable
private fun EditBar(onMinus: () -> Unit, onPlus: () -> Unit, onReset: () -> Unit, onDone: () -> Unit, dragModifier: Modifier) {
    val c = LocalA9Colors.current
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(40.dp).then(dragModifier), contentAlignment = Alignment.Center) {
            Text("☰  " + stringResource(R.string.drag_to_move), color = c.subText, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        EditButton("−", onMinus)
        EditButton("+", onPlus)
        EditButton("↺", onReset)
        EditButton("✓", onDone, accent = true)
    }
}

@Composable
private fun EditButton(glyph: String, onClick: () -> Unit, accent: Boolean = false) {
    val c = LocalA9Colors.current
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(glyph, color = if (accent) c.accent else c.text, fontSize = 20.sp)
    }
}

