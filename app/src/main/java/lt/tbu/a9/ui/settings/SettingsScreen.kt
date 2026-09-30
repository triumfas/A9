package lt.tbu.a9.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.tbu.a9.R
import lt.tbu.a9.icons.IconPackManager
import lt.tbu.a9.ui.DialerUiState
import lt.tbu.a9.ui.DialerViewModel
import lt.tbu.a9.ui.theme.LocalA9Colors

@Composable
fun SettingsScreen(vm: DialerViewModel, state: DialerUiState, onBack: () -> Unit) {
    val c = LocalA9Colors.current
    val context = LocalContext.current
    val s = state.settings
    var packDialog by remember { mutableStateOf(false) }
    var usageAccess by remember { mutableStateOf(vm.hasUsageAccess()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { usageAccess = vm.hasUsageAccess(); vm.refreshRecents() }

    val contactsPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        vm.setContacts(r[Manifest.permission.READ_CONTACTS] == true)
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> vm.setQuickLaunch(granted) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        Row(Modifier.clickable(onClick = onBack).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←  " + stringResource(R.string.settings), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Light)
        }

        ClickRow(stringResource(R.string.appearance), stringResource(R.string.appearance_summary)) { vm.setAppearanceVisible(true) }

        ClickRow(stringResource(R.string.icon_pack), s.iconPack ?: stringResource(R.string.default_icons)) { packDialog = true }

        SwitchRow(stringResource(R.string.contacts_search), stringResource(R.string.contacts_search_summary), s.contactsEnabled) { on ->
            if (!on) vm.setContacts(false)
            else contactsPerm.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE))
        }
        SwitchRow(stringResource(R.string.quick_launch), stringResource(R.string.quick_launch_summary), s.quickLaunch) { on ->
            if (!on) vm.setQuickLaunch(false)
            else if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
            else vm.setQuickLaunch(true)
        }

        SwitchRow(
            stringResource(R.string.recent_apps),
            stringResource(if (s.recentApps && !usageAccess) R.string.recent_apps_no_access else R.string.recent_apps_summary),
            s.recentApps,
        ) { on ->
            vm.setRecentApps(on)
            if (on && !usageAccess) {
                context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, android.net.Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
        SwitchRow(stringResource(R.string.haptics), stringResource(R.string.haptics_summary), s.haptics) { vm.setHaptics(it) }

        ClickRow(stringResource(R.string.window_layout), stringResource(R.string.window_layout_summary)) { vm.startEditLayout() }
        ClickRow(stringResource(R.string.reindex), null) { vm.reindex() }
        ClickRow(stringResource(R.string.hidden_apps), stringResource(R.string.hidden_count, state.hiddenCount)) { vm.setHiddenVisible(true) }
        ClickRow(stringResource(R.string.reset_stats), null) { vm.clearStats() }
    }

    if (packDialog) {
        val packs = remember { IconPackManager.installed(context) }
        AlertDialog(
            onDismissRequest = { packDialog = false },
            confirmButton = { TextButton({ packDialog = false }) { Text(stringResource(android.R.string.cancel)) } },
            title = { Text(stringResource(R.string.icon_pack)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ClickRow(stringResource(R.string.default_icons), null) { vm.setIconPack(null); packDialog = false }
                    if (packs.isEmpty()) Text(stringResource(R.string.no_icon_packs), color = c.subText, modifier = Modifier.padding(16.dp))
                    packs.forEach { p -> ClickRow(p.label, null) { vm.setIconPack(p.packageName); packDialog = false } }
                }
            },
        )
    }
}

@Composable
internal fun Section(res: Int) {
    val c = LocalA9Colors.current
    Text(stringResource(res), color = c.accent, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
internal fun ChipRow(content: @Composable () -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
internal fun ClickRow(title: String, summary: String?, onClick: () -> Unit) {
    val c = LocalA9Colors.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, color = c.text, fontSize = 16.sp)
        if (summary != null) Text(summary, color = c.subText, fontSize = 13.sp)
    }
}

@Composable
private fun SwitchRow(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalA9Colors.current
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontSize = 16.sp)
            Text(summary, color = c.subText, fontSize = 13.sp)
        }
        Switch(checked, onChange)
    }
}
