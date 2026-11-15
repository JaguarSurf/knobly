package app.knobly.app.desktop

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Phone screen sizes in dp (portrait), which is what Compose lays out with: pixels / density.
 * For a real phone, divide its resolution by its density (e.g. 1080 x 2340 px at 450 dpi = 2.8125 → 384 x 832 dp;
 * `adb shell wm size` and `adb shell wm density` print both).
 */
enum class DevicePreset(
    val label: String,
    val size: DpSize,
    /** A real phone with this size: its resolution and density. */
    val example: String? = null,
) {
    SMALL("Small", DpSize(320.dp, 568.dp)),
    COMPACT("Compact", DpSize(360.dp, 800.dp), "720 × 1600 px @ 2.0"),
    GALAXY_A15("Galaxy A15", DpSize(384.dp, 832.dp), "1080 × 2340 px @ 2.8125"),
    MEDIUM("Medium", DpSize(393.dp, 852.dp)),
    LARGE("Large", DpSize(412.dp, 915.dp), "1080 × 2400 px @ 2.625"),
    FOLDABLE("Foldable", DpSize(673.dp, 841.dp)),
}

/** The app's languages (composeResources/values and values-pt). [SYSTEM] follows the computer's locale. */
enum class HarnessLanguage(
    val label: String,
    val languageTag: String?,
) {
    SYSTEM("System", null),
    PT_BR("Português", "pt-BR"),
    EN("English", "en"),
}

/** Android's font size steps (Settings > Display > Font size goes up to 2.0 since Android 14). */
val FontScales = listOf(0.85f, 1f, 1.15f, 1.3f, 1.5f, 2f)

/** The simulated phone: screen size, orientation and the system font scale. Shared by the frame and the panel. */
@Stable
class DeviceFrameState(
    preset: DevicePreset = DevicePreset.LARGE,
) {
    var preset by mutableStateOf(preset)
    var landscape by mutableStateOf(false)
    var fontScale by mutableFloatStateOf(1f)
    var language by mutableStateOf(HarnessLanguage.SYSTEM)

    /** The computer's locale, captured before the harness changes the JVM default. */
    private val systemLocale: Locale = Locale.getDefault()

    val locale: Locale
        get() = language.languageTag?.let(Locale::forLanguageTag) ?: systemLocale

    val size: DpSize
        get() = if (landscape) DpSize(preset.size.height, preset.size.width) else preset.size
}

/**
 * Lays [content] out at exactly the simulated phone size (scrolling when the window is smaller), with the simulated
 * font scale and language. The density is kept: changing it would only zoom the picture, not change the layout.
 *
 * Compose resources read the JVM default locale, which is not observable state: a language change sets it and then
 * recreates [content] (it starts again on its first screen).
 */
@Composable
fun DeviceFrame(
    state: DeviceFrameState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val locale = state.locale
    remember(locale) { Locale.setDefault(locale) }
    Box(modifier.verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
        CompositionLocalProvider(LocalDensity provides Density(density.density, state.fontScale)) {
            Box(Modifier.requiredSize(state.size)) { key(locale) { content() } }
        }
    }
}
