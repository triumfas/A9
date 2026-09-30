package lt.tbu.a9.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.tbu.a9.R
import lt.tbu.a9.data.AppItem
import lt.tbu.a9.icons.IconLoader
import lt.tbu.a9.search.T9Map
import lt.tbu.a9.ui.DialerViewModel
import lt.tbu.a9.ui.results.ItemIcon
import lt.tbu.a9.ui.theme.LocalA9Colors

/** Paslėptų programų valdymas: „Paslėptos“ (atslėpti) arba „Visos“ (surasti ir paslėpti). */
@Composable
fun HiddenAppsScreen(vm: DialerViewModel, icons: IconLoader, onBack: () -> Unit) {
    val c = LocalA9Colors.current
    val apps by vm.allApps.collectAsStateWithLifecycle()
    val hidden by vm.hiddenIds.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(hidden.isEmpty()) }

    val list = remember(apps, hidden, query, showAll) {
        val q = T9Map.normalize(query).filter { !it.isWhitespace() }
        apps.asSequence()
            .filter { showAll || it.id in hidden }
            .filter { q.isEmpty() || T9Map.normalize(it.label).contains(q) || it.packageName.lowercase().contains(q) }
            .toList()
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clickable(onClick = onBack).padding(16.dp)) {
                Text("←  " + stringResource(R.string.hidden_apps), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Light)
            }
            if (hidden.isNotEmpty()) TextButton(onClick = vm::unhideAll) { Text(stringResource(R.string.unhide_all)) }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_apps)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        ChipRow {
            FilterChip(!showAll, { showAll = false }, { Text(stringResource(R.string.hidden_filter, hidden.size)) })
            FilterChip(showAll, { showAll = true }, { Text(stringResource(R.string.all_apps)) })
        }

        LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
            items(list, key = { it.id }) { app -> AppRow(app, app.id in hidden, icons) { if (app.id in hidden) vm.unhide(app.id) else vm.hide(app.id) } }
        }
    }
}

@Composable
private fun AppRow(app: AppItem, isHidden: Boolean, icons: IconLoader, onToggle: () -> Unit) {
    val c = LocalA9Colors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(app, icons, 40.dp)
        Text(
            app.label + if (app.isWorkProfile) " 💼" else "",
            color = c.text, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
        )
        Switch(isHidden, { onToggle() })
    }
}
