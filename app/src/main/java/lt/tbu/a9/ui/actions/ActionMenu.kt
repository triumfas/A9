package lt.tbu.a9.ui.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch
import lt.tbu.a9.R
import lt.tbu.a9.data.AppItem
import lt.tbu.a9.data.ContactItem
import lt.tbu.a9.data.Item
import lt.tbu.a9.ui.theme.LocalA9Colors

/** Programos veiksmai; numatytoji tvarka – nuo arčiausio ikonos. Vėliau rikiuojama pagal naudojimo dažnį. */
private enum class AppAct(val labelRes: Int) {
    INFO(R.string.action_app_info),
    PIN(R.string.action_pin),
    ADD_HOME(R.string.action_add_home),
    HIDE(R.string.action_hide),
    PLAY(R.string.action_play_store),
    UNINSTALL(R.string.action_uninstall),
    FORCE_STOP(R.string.action_force_stop),
}

private class MenuRow(val label: String, val run: () -> Unit)

/**
 * Kompaktiškas „long-press“ meniu. Atsiveria virš ikonos (jei ji apatinėje ekrano pusėje) arba po ja;
 * dažniausiai naudojami veiksmai yra arčiausiai ikonos.
 */
@Composable
fun ActionMenu(
    item: Item,
    expanded: Boolean,
    pinned: Boolean,
    above: Boolean,
    actions: AppActions,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onHide: () -> Unit,
    onLaunched: () -> Unit,
) {
    if (!expanded) return
    val c = LocalA9Colors.current
    val scope = rememberCoroutineScope()

    fun ext(ok: Boolean) { if (ok) onLaunched() }

    // Eilutės nuo arčiausios ikonos iki tolimiausios.
    val rows: List<MenuRow> = when (item) {
        is AppItem -> {
            val counts = remember { actions.actionCounts() }
            AppAct.entries
                .filter { it != AppAct.UNINSTALL || !item.isSystem }
                .sortedWith(compareByDescending<AppAct> { counts[it.name] ?: 0 }.thenBy { it.ordinal })
                .map { act ->
                    val label = if (act == AppAct.PIN && pinned) stringResource(R.string.action_unpin) else stringResource(act.labelRes)
                    MenuRow(label) {
                        actions.recordAction(act.name)
                        when (act) {
                            AppAct.INFO -> ext(actions.appInfo(item))
                            AppAct.PIN -> onPin()
                            AppAct.ADD_HOME -> scope.launch { actions.pinToHome(item) }
                            AppAct.HIDE -> onHide()
                            AppAct.PLAY -> ext(actions.playStore(item))
                            AppAct.UNINSTALL -> ext(actions.uninstall(item))
                            AppAct.FORCE_STOP -> ext(actions.forceStop(item))
                        }
                    }
                }
        }
        is ContactItem -> buildList {
            item.phones.take(3).forEach { number ->
                add(MenuRow(stringResource(R.string.action_call, number)) { ext(actions.call(item, number)) })
                add(MenuRow(stringResource(R.string.action_sms, number)) { ext(actions.sms(item, number)) })
            }
            add(MenuRow(stringResource(R.string.action_open_contact)) { ext(actions.openContact(item)) })
        }
    }
    // Virš ikonos arčiausia eilutė turi būti apačioje, todėl sąrašą apverčiame.
    val shown = if (above) rows.asReversed() else rows

    val margin = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.roundToPx() }
    val provider = remember(above, margin) { MenuPositionProvider(above, margin) }

    Popup(popupPositionProvider = provider, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Surface(shape = RoundedCornerShape(14.dp), color = c.menu, shadowElevation = 8.dp) {
            Column(Modifier.width(IntrinsicSize.Max).defaultMinSize(minWidth = 190.dp).padding(vertical = 4.dp)) {
                shown.forEach { row ->
                    Box(
                        Modifier.fillMaxWidth().height(38.dp).clickable { onDismiss(); row.run() }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) { Text(row.label, color = c.text, fontSize = 14.sp, maxLines = 1) }
                }
            }
        }
    }
}

private class MenuPositionProvider(private val above: Boolean, private val margin: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(margin, maxX)
        val rawY = if (above) anchorBounds.top - popupContentSize.height - margin / 2 else anchorBounds.bottom + margin / 2
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        return IntOffset(x, rawY.coerceIn(margin, maxY))
    }
}
