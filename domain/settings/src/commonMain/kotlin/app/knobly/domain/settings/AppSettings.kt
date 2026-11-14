package app.knobly.domain.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * App-wide preferences, persisted as the `app_settings` JSON document. New fields need defaults
 * (docs/adr/0007-app-updates-and-data-compatibility.md).
 */
@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    companion object {
        const val DOCUMENT_NAME = "app_settings"
    }
}

/** Light or dark theme (docs/specs/appearance.md). The `@SerialName`s are stored: never change them. */
@Serializable
enum class ThemeMode {
    /** Follow the phone's light/dark setting (the default). */
    @SerialName("system")
    SYSTEM,

    @SerialName("light")
    LIGHT,

    @SerialName("dark")
    DARK,
}
