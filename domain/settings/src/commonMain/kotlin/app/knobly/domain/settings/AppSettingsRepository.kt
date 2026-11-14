package app.knobly.domain.settings

import app.knobly.core.datastore.DocumentStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Reads and changes the [AppSettings]. Loading starts as soon as the repository is created (create it at app start),
 * so the theme is usually known before the first frame.
 */
class AppSettingsRepository(
    private val store: DocumentStore<AppSettings>,
    scope: CoroutineScope,
) {
    /** `null` until the settings are loaded. */
    val themeMode: StateFlow<ThemeMode?> =
        store.data.map { it.themeMode }.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setThemeMode(mode: ThemeMode) {
        store.update { it.copy(themeMode = mode) }
    }
}
