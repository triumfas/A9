package lt.tbu.a9.ui.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.tbu.a9.search.T9Map
import lt.tbu.a9.ui.theme.LocalA9Colors
import lt.tbu.a9.ui.theme.LocalHaptics

class KeyboardActions(
    val onChar: (Char) -> Unit,
    val onBackspace: () -> Unit,
    val onClear: () -> Unit,
    val onSettings: () -> Unit,
)

/** Klavišas su švelniu fonu (aiškiai matosi, kur spausti); atsakas – ripple ir haptika. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Key(
    modifier: Modifier,
    height: Dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    tint: androidx.compose.ui.graphics.Color? = null,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val haptics = LocalHaptics.current
    val c = LocalA9Colors.current
    Box(
        modifier
            .padding(2.dp)
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(tint ?: c.key)
            .combinedClickable(
                onClick = { if (haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() },
                onLongClick = onLongClick?.let { l -> { if (haptics) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); l() } },
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun NumpadKeyboard(actions: KeyboardActions, modifier: Modifier = Modifier) {
    val c = LocalA9Colors.current
    val h = 68.dp

    // Kaip telefono rinkiklyje: skaitmuo viršuje, raidės po juo.
    @Composable
    fun DigitContent(d: Char) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(d.toString(), color = c.accent, fontSize = 30.sp, fontWeight = FontWeight.Light)
            Text(T9Map.lettersOf(d).uppercase(), color = c.text, fontSize = 12.sp, letterSpacing = 2.sp)
        }
    }

    // 3×3: T9 naudoja tik 2–9, todėl „1“ vietoje – išvalymas (ilgai – nustatymai); „0“ nėra.
    Column(modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Key(Modifier.weight(1f), h, actions.onClear, actions.onSettings, tint = c.accent.copy(alpha = 0.22f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CLEAR", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                    Text("⚙", color = c.subText, fontSize = 14.sp)
                }
            }
            listOf('2', '3').forEach { d -> Key(Modifier.weight(1f), h, { actions.onChar(d) }) { DigitContent(d) } }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf('4', '5', '6').forEach { d -> Key(Modifier.weight(1f), h, { actions.onChar(d) }) { DigitContent(d) } }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf('7', '8').forEach { d -> Key(Modifier.weight(1f), h, { actions.onChar(d) }) { DigitContent(d) } }
            Key(Modifier.weight(1f), h, { actions.onChar('9') }) { DigitContent('9') }
        }
    }
}
