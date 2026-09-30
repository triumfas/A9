package lt.tbu.a9.ui.results

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import lt.tbu.a9.search.Highlighter
import lt.tbu.a9.R
import lt.tbu.a9.data.AppItem
import lt.tbu.a9.data.Item
import lt.tbu.a9.icons.IconLoader
import lt.tbu.a9.ui.actions.ActionMenu
import lt.tbu.a9.ui.actions.AppActions
import lt.tbu.a9.ui.theme.LocalA9Colors
import lt.tbu.a9.ui.theme.LocalHaptics



/** Height of one row: icon + a two-line name, with no empty gap. */
private const val CELL_H = 100

@Composable
fun ItemIcon(item: Item, icons: IconLoader, size: Dp) {
    val version by icons.version.collectAsState()
    val bmp by produceState<ImageBitmap?>(null, item.id, version) { value = icons.load(item).asImageBitmap() }
    bmp?.let { Image(it, contentDescription = null, modifier = Modifier.size(size)) } ?: Spacer(Modifier.size(size))
}

/** [columns] × [rows] icons per page; more results – another page (swipe sideways). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResultPager(
    results: List<Item>,
    pinned: Set<String>,
    query: String,
    icons: IconLoader,
    actions: AppActions,
    onOpen: (Item) -> Unit,
    onPin: (String) -> Unit,
    onHide: (String) -> Unit,
    onClear: () -> Unit,
    onLaunched: () -> Unit,
    columns: Int,
    rows: Int,
    modifier: Modifier = Modifier,
) {
    val c = LocalA9Colors.current
    val pages = remember(results, columns, rows) { results.chunked(columns * rows) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    LaunchedEffect(query) { pagerState.scrollToPage(0) }

    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height((CELL_H * rows).dp)) {
            if (pages.isEmpty()) {
                if (query.isNotEmpty()) {
                    Text(
                        stringResource(R.string.no_results),
                        color = c.subText, fontSize = 16.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).clickable(onClick = onClear).padding(16.dp),
                    )
                }
            } else {
                HorizontalPager(pagerState, Modifier.fillMaxSize(), key = { p -> pages[p].first().id }) { page ->
                    Column(Modifier.fillMaxSize()) {
                        val pageItems = pages[page]
                        for (r in 0 until rows) {
                            Row(Modifier.fillMaxWidth().height(CELL_H.dp)) {
                                for (i in 0 until columns) {
                                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                        pageItems.getOrNull(r * columns + i)?.let { item ->
                                            ResultCell(item, item.id in pinned, query, icons, actions, onOpen, { onPin(item.id) }, { onHide(item.id) }, onLaunched)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        PageDots(pages.size, pagerState.currentPage)
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    val c = LocalA9Colors.current
    Row(Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (count > 1) {
            // Long lists: show at most 12 indicators around the current page.
            val start = (current - 5).coerceIn(0, maxOf(0, count - 12))
            for (i in start until minOf(count, start + 12)) {
                Box(
                    Modifier.padding(horizontal = 3.dp).width(if (i == current) 18.dp else 8.dp).height(3.dp)
                        .clip(RoundedCornerShape(2.dp)).background(if (i == current) c.accent else c.subText.copy(alpha = 0.4f)),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultCell(
    item: Item,
    pinned: Boolean,
    query: String,
    icons: IconLoader,
    actions: AppActions,
    onOpen: (Item) -> Unit,
    onPin: () -> Unit,
    onHide: () -> Unit,
    onLaunched: () -> Unit,
) {
    val c = LocalA9Colors.current
    val view = LocalView.current
    val haptics = LocalHaptics.current
    var menu by remember { mutableStateOf(false) }
    var above by remember { mutableStateOf(true) }
    val windowH = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height

    Box(
        Modifier.onGloballyPositioned { above = it.positionInWindow().y + it.size.height / 2f > windowH / 2f },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = { onOpen(item) },
                    onLongClick = { if (haptics) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); menu = true },
                )
                .padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ItemIcon(item, icons, 58.dp)
            // Letters matching the query are highlighted – this is the feedback for the input.
            val label = remember(item.label, pinned, query) {
                val hit = Highlighter.matchIndices(item.label, query)
                buildAnnotatedString {
                    if (pinned) append("★ ")
                    item.label.forEachIndexed { i, ch ->
                        if (i in hit) pushStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.Bold)) else pushStyle(SpanStyle())
                        append(ch)
                        pop()
                    }
                    if (item is AppItem && item.isWorkProfile) append(" 💼")
                }
            }
            Text(
                label,
                color = c.text, fontSize = 12.sp, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        ActionMenu(item, menu, pinned, above, actions, { menu = false }, onPin, onHide, onLaunched)
    }
}
