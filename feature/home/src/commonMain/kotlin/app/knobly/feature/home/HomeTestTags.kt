package app.knobly.feature.home

import app.knobly.domain.settings.ThemeMode

/** Test tags of the home (control center) screen, shared with UI tests. */
object HomeTestTags {
    const val CALLS_TILE = "home.callsTile"
    const val ENABLE_CALL_FILTERING = "home.enableCallFiltering"
    const val STATS = "home.stats"
    const val LATEST_REJECTION = "home.latestRejection"
    const val THEME = "home.theme"

    fun themeOption(mode: ThemeMode) = "home.theme.${mode.name}"
}
