package app.knobly.app.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import app.knobly.core.designsystem.KnoblyBackground
import app.knobly.core.designsystem.KnoblyIcons
import app.knobly.core.designsystem.KnoblyNavBar
import app.knobly.core.designsystem.KnoblyNavItem
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.feature.calls.FiltersScreen
import app.knobly.feature.history.HistoryScreen
import app.knobly.feature.onboarding.CallFilteringCard
import app.knobly.feature.stats.StatsScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object FiltersRoute

@Serializable data object HistoryRoute

@Serializable data object StatsRoute

/** The bottom navigation tabs. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
) {
    FILTERS(FiltersRoute, FiltersRoute::class, "Filters", KnoblyIcons.Filters, "tab.filters"),
    HISTORY(HistoryRoute, HistoryRoute::class, "History", KnoblyIcons.History, "tab.history"),
    STATS(StatsRoute, StatsRoute::class, "Statistics", KnoblyIcons.Stats, "tab.stats"),
}

/** Root of the shared UI, used by every platform (Android, iOS, desktop harness). */
@Composable
fun App(modifier: Modifier = Modifier) {
    KnoblyTheme {
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

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        bottomBar = {
            KnoblyNavBar(
                items = destinations.map { KnoblyNavItem(it.label, it.icon, it.testTag) },
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    navController.navigate(destinations[index].route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = FiltersRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<FiltersRoute> {
                // The app layer composes features: the call filtering card (:feature:onboarding) sits on top of the
                // Filters tab (:feature:calls) while filtering is off. Features never depend on each other.
                Column(Modifier.fillMaxSize()) {
                    CallFilteringCard(Modifier.padding(16.dp))
                    FiltersScreen(Modifier.weight(1f))
                }
            }
            composable<HistoryRoute> { HistoryScreen() }
            composable<StatsRoute> { StatsScreen() }
        }
    }
}
