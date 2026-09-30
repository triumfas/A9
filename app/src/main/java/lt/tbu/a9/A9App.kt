package lt.tbu.a9

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import lt.tbu.a9.data.AppRepository
import lt.tbu.a9.data.ContactRepository
import lt.tbu.a9.data.RecentAppsRepository
import lt.tbu.a9.data.SettingsRepository
import lt.tbu.a9.data.UsageRepository
import lt.tbu.a9.icons.IconLoader
import lt.tbu.a9.notify.QuickLaunchNotifier
import lt.tbu.a9.ui.actions.AppActions

/** Paprastas rankinis DI: vienas konteineris visai programai. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = SettingsRepository(app)
    val usage = UsageRepository(app, scope)
    val apps = AppRepository(app, scope)
    val contacts = ContactRepository(app, scope)
    val recents = RecentAppsRepository(app, scope)
    val icons = IconLoader(app, apps)
    val actions = AppActions(app, apps, icons, usage)
    val notifier = QuickLaunchNotifier(app, apps, icons, usage, settings)

    init {
        // Atnaujintų programų ikonas perkraunam tik po sąrašo perkrovimo ir tik joms (be globalaus valymo → nėra mirgėjimo).
        apps.onPackagesUpdated = { icons.invalidatePackages(it) }
        apps.start()
        scope.launch {
            // Senos statistikos valymas – be skubos, kad nekonkuruotų su ikonų krovimu paleidžiant.
            apps.apps.collectLatest { list ->
                if (list.isNotEmpty()) {
                    delay(5_000)
                    usage.pruneMissingApps(list.map { it.id }.toSet())
                }
            }
        }
        scope.launch {
            settings.flow.map { it.iconPack }.distinctUntilChanged().collectLatest { icons.setPack(it) }
        }
        scope.launch {
            settings.flow.map { it.contactsEnabled }.distinctUntilChanged().collectLatest { contacts.setEnabled(it) }
        }
        scope.launch {
            // Quick Launch pranešimas seka naudojimą, programėlių sąrašą ir nustatymą.
            kotlinx.coroutines.flow.combine(usage.state, apps.apps, settings.flow.map { it.quickLaunch }.distinctUntilChanged(), icons.version) { _, _, _, _ -> }
                .collectLatest { notifier.update() }
        }
    }
}

class A9App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
