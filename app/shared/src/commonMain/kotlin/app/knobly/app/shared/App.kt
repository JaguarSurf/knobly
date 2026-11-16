package app.knobly.app.shared

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.knobly.app.shared.resources.Res
import app.knobly.app.shared.resources.nav_filters
import app.knobly.app.shared.resources.nav_history
import app.knobly.app.shared.resources.nav_home
import app.knobly.app.shared.resources.nav_stats
import app.knobly.core.designsystem.KnoblyBackground
import app.knobly.core.designsystem.KnoblyIcons
import app.knobly.core.designsystem.KnoblyNavBar
import app.knobly.core.designsystem.KnoblyNavItem
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.domain.settings.AppSettingsRepository
import app.knobly.domain.settings.ThemeMode
import app.knobly.feature.calls.FiltersScreen
import app.knobly.feature.calls.RuleEditorScreen
import app.knobly.feature.history.HistoryScreen
import app.knobly.feature.home.HomeScreen
import app.knobly.feature.onboarding.CallFilteringCard
import app.knobly.feature.stats.StatsScreen
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.reflect.KClass

@Serializable data object HomeRoute

@Serializable data object FiltersRoute

@Serializable data object HistoryRoute

@Serializable data object StatsRoute

/** The rule editor; `ruleId` null creates a new rule. */
@Serializable data class RuleEditorRoute(
    val ruleId: String? = null,
)

/**
 * Every screen change crossfades for this long (Navigation's own default). The system back (gesture or button) uses
 * the same crossfade: Navigation's default predictive back transition shrinks the leaving screen to 70% instead.
 */
private const val NAV_FADE_MILLIS = 700

/** The bottom navigation tabs. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: StringResource,
    val icon: ImageVector,
    val testTag: String,
) {
    HOME(HomeRoute, HomeRoute::class, Res.string.nav_home, KnoblyIcons.Home, "tab.home"),
    FILTERS(FiltersRoute, FiltersRoute::class, Res.string.nav_filters, KnoblyIcons.Filters, "tab.filters"),
    HISTORY(HistoryRoute, HistoryRoute::class, Res.string.nav_history, KnoblyIcons.History, "tab.history"),
    STATS(StatsRoute, StatsRoute::class, Res.string.nav_stats, KnoblyIcons.Stats, "tab.stats"),
}

/**
 * Root of the shared UI, used by every platform (Android, iOS, desktop harness).
 *
 * The theme follows the user's choice (System, Light or Dark, docs/specs/appearance.md); [darkTheme] forces one
 * (screenshots). [onDarkThemeChange] lets the platform match its own chrome, e.g. Android's status bar icons.
 */
@Composable
fun App(
    modifier: Modifier = Modifier,
    darkTheme: Boolean? = null,
    onDarkThemeChange: (Boolean) -> Unit = {},
) {
    val themeMode by koinInject<AppSettingsRepository>().themeMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val dark =
        darkTheme ?: when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM, null -> systemDark
        }
    LaunchedEffect(dark) { onDarkThemeChange(dark) }
    KnoblyTheme(darkTheme = dark) {
        KnoblyBackground(modifier.fillMaxSize()) {
            MainScaffold()
        }
    }
}

@Composable
private fun MainScaffold(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val destinations = TopLevelDestination.entries
    val selectedIndex = destinations.indexOfFirst { currentDestination?.hasRoute(it.routeClass) == true }
    val navigateTo: (TopLevelDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        bottomBar = {
            // Only on the top-level tabs (hidden in the rule editor).
            if (selectedIndex < 0) return@Scaffold
            KnoblyNavBar(
                items = destinations.map { KnoblyNavItem(stringResource(it.label), it.icon, it.testTag) },
                selectedIndex = selectedIndex,
                onSelect = { index -> navigateTo(destinations[index]) },
                modifier = Modifier.navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(NAV_FADE_MILLIS)) },
            exitTransition = { fadeOut(tween(NAV_FADE_MILLIS)) },
            predictivePopEnterTransition = { fadeIn(tween(NAV_FADE_MILLIS)) },
            predictivePopExitTransition = { fadeOut(tween(NAV_FADE_MILLIS)) },
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpenCalls = { navigateTo(TopLevelDestination.FILTERS) },
                    onOpenHistory = { navigateTo(TopLevelDestination.HISTORY) },
                )
            }
            composable<FiltersRoute> {
                // The app layer composes features: the call filtering card (:feature:onboarding) sits on top of the
                // Filters tab (:feature:calls) while filtering is off. Features never depend on each other.
                FiltersScreen(
                    onNewRule = { navController.navigate(RuleEditorRoute()) },
                    onEditRule = { id -> navController.navigate(RuleEditorRoute(id)) },
                    topContent = { CallFilteringCard() },
                )
            }
            composable<RuleEditorRoute> { entry ->
                RuleEditorScreen(
                    ruleId = entry.toRoute<RuleEditorRoute>().ruleId,
                    onClose = { navController.popBackStack() },
                )
            }
            composable<HistoryRoute> { HistoryScreen() }
            composable<StatsRoute> { StatsScreen() }
        }
    }
}
