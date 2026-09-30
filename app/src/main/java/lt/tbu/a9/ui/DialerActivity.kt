package lt.tbu.a9.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import lt.tbu.a9.A9App

/** Search screen: launcher icon, ACTION_ASSIST, SEARCH_LONG_PRESS. */
class DialerActivity : ComponentActivity() {
    private val container get() = (application as A9App).container
    private val vm: DialerViewModel by viewModels { DialerViewModel.Factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DialerScreen(vm, container.actions, container.icons, onClose = ::finish) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.reset()
    }

    override fun onStart() {
        super.onStart()
        container.recents.refresh()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) vm.reset()
    }
}
