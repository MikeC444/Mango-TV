package tv.mango.app.theme

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import tv.mango.app.repository.HomeScreenConfigRepository
import tv.mango.app.settings.home.HomeScreenConfig

/**
 * The current Home Screen appearance, held where every screen in the
 * application can reach it without a `Context` and without a trip through
 * [tv.mango.app.di.AppGraph].
 *
 * This is what makes the customisation live rather than cosmetic. A view class
 * such as `TvCardView` or `NavRail` is constructed fresh every time the screen
 * that holds it is navigated to - the application already tears down and
 * rebuilds its fragments on every section change - so a view reading
 * [config] at construction time picks up whatever a viewer last saved without
 * this object needing to push updates into already-built views. The one
 * exception is chrome that outlives navigation, `NavRail` chief among them,
 * which observes [config] directly and re-applies it in place.
 *
 * Started once, from [tv.mango.app.MangoApplication.onCreate], and never
 * stopped - it is meant to live exactly as long as the process, the same as
 * [tv.mango.app.di.AppGraph] itself.
 */
object RuntimeTheme {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _config = MutableStateFlow(HomeScreenConfig.default())
    val config: StateFlow<HomeScreenConfig> = _config

    /** The palette derived from [config]'s colours - what almost every call site actually wants. */
    val colors: MangoColors get() = MangoColors.resolve(_config.value.colors)

    private var started = false

    fun start(repository: HomeScreenConfigRepository) {
        if (started) return
        started = true
        scope.launch {
            repository.config.collect { _config.value = it }
        }
    }
}
