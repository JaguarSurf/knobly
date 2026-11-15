package app.knobly.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Knobly's color tokens (docs/design/visual-identity.md). Material's color scheme is derived from these, but
 * components should read the tokens through [KnoblyTheme.colors].
 */
@Immutable
data class KnoblyColors(
    val isDark: Boolean,
    // Background: vertical gradient with a soft glow near the top
    val backgroundTop: Color,
    val backgroundMiddle: Color,
    val backgroundBottom: Color,
    val backgroundGlow: Color,
    /** A second, fainter glow near the bottom end (light theme); transparent when unused. */
    val backgroundGlowSecondary: Color,
    // Glass surfaces (cards, navigation bar)
    val glass: Color,
    val glassStrong: Color,
    val glassBorder: Color,
    val glassShadow: Color,
    val surface: Color,
    // Brand
    val accent: Color,
    val onAccent: Color,
    val glow: Color,
    val primaryButton: Color,
    val onPrimaryButton: Color,
    // Selection (pills, segmented controls, navigation)
    val selected: Color,
    val onSelected: Color,
    // Text
    val text: Color,
    val textSecondary: Color,
    val textSoft: Color,
    val divider: Color,
    // Toggles
    val toggleOn: Color,
    val toggleOff: Color,
    val thumbOn: Color,
    val thumbOff: Color,
    // Status
    val statusActive: Color,
    val statusInactive: Color,
    val alert: Color,
    /** Call outcomes (History, Statistics, Home): rejected red, answered green, missed amber. */
    val callRejected: Color,
    val callAnswered: Color,
    val callMissed: Color,
    /** Icon colors of the Home modules (calls, messages, notifications). */
    val moduleCalls: Color,
    val moduleMessages: Color,
    val moduleNotifications: Color,
)

/** Dark theme "Noturno", mint accent. */
val DarkKnoblyColors =
    KnoblyColors(
        isDark = true,
        backgroundTop = Color(0xFF171233),
        backgroundMiddle = Color(0xFF0E0B1F),
        backgroundBottom = Color(0xFF0B0918),
        backgroundGlow = Color(0x4D1FB894),
        backgroundGlowSecondary = Color.Transparent,
        glass = Color(0x0DFFFFFF),
        glassStrong = Color(0x1AFFFFFF),
        glassBorder = Color(0x17FFFFFF),
        glassShadow = Color.Transparent,
        surface = Color(0xFF1C1640),
        accent = Color(0xFF6FE3C6),
        onAccent = Color(0xFF0E0B1F),
        glow = Color(0x4D1FB894),
        primaryButton = Color(0xFFC7F5EA),
        onPrimaryButton = Color(0xFF1A1433),
        selected = Color(0xFFC7F5EA),
        onSelected = Color(0xFF1A1433),
        text = Color(0xFFF2EFFF),
        textSecondary = Color(0xFFA7A1C2),
        textSoft = Color(0xFFCFCAE3),
        divider = Color(0x12FFFFFF),
        toggleOn = Color(0xFF1FB894),
        toggleOff = Color(0x24FFFFFF),
        thumbOn = Color(0xFFFFFFFF),
        thumbOff = Color(0xFFC9C3E0),
        statusActive = Color(0xFF6EE7B7),
        statusInactive = Color(0xFF7D7799),
        alert = Color(0xFFFF8A7A),
        callRejected = Color(0xFFFF6B6B),
        callAnswered = Color(0xFF6EE7B7),
        callMissed = Color(0xFFF5B860),
        moduleCalls = Color(0xFFA7A1C2),
        moduleMessages = Color(0xFFA7A1C2),
        moduleNotifications = Color(0xFFA7A1C2),
    )

/**
 * Light theme "Noturno Partido": soft lavender with a teal-mint accent, colored module icons, and blocks of a softened
 * Noturno (the Home status card and the navigation bar) drawn with [LightBlockKnoblyColors].
 */
val LightKnoblyColors =
    KnoblyColors(
        isDark = false,
        backgroundTop = Color(0xFFF4F3FA),
        backgroundMiddle = Color(0xFFEAE8F4),
        backgroundBottom = Color(0xFFDFDCEE),
        backgroundGlow = Color(0xE6FFFFFF),
        backgroundGlowSecondary = Color(0x2982E1C8),
        glass = Color(0x8CFFFFFF),
        glassStrong = Color(0xD9FFFFFF),
        glassBorder = Color(0xE6FFFFFF),
        glassShadow = Color(0x1A463A8C),
        surface = Color(0xFFFBFAFE),
        accent = Color(0xFF13705F),
        onAccent = Color(0xFFFFFFFF),
        glow = Color.Transparent,
        primaryButton = Color(0xFFBDEFE1),
        onPrimaryButton = Color(0xFF221D3E),
        selected = Color(0xFF221D3E),
        onSelected = Color(0xFFF4F3FA),
        text = Color(0xFF221D3E),
        textSecondary = Color(0xFF5D587A),
        textSoft = Color(0xFF3E3960),
        divider = Color(0x14221D3E),
        toggleOn = Color(0xFF13705F),
        toggleOff = Color(0x29221D3E),
        thumbOn = Color(0xFFFFFFFF),
        thumbOff = Color(0xFFFFFFFF),
        statusActive = Color(0xFF1F8A6E),
        statusInactive = Color(0xFFA19CB5),
        alert = Color(0xFFC8553D),
        callRejected = Color(0xFFC0392B),
        callAnswered = Color(0xFF3F7F3A),
        callMissed = Color(0xFFA86A12),
        moduleCalls = Color(0xFF2E8E79),
        moduleMessages = Color(0xFF7A64C8),
        moduleNotifications = Color(0xFFA8741F),
    )

/**
 * The softened Noturno used for the dark blocks inside the light theme ([KnoblyBlock], [KnoblyNavBar]). The block's
 * background is the vertical gradient [KnoblyColors.backgroundTop] → [KnoblyColors.backgroundBottom].
 */
val LightBlockKnoblyColors =
    DarkKnoblyColors.copy(
        backgroundTop = Color(0xFF36305F),
        backgroundMiddle = Color(0xFF2F2A55),
        backgroundBottom = Color(0xFF28234A),
        backgroundGlow = Color(0x5282E1C8),
        glass = Color(0x17FFFFFF),
        glassStrong = Color(0x24FFFFFF),
        glassBorder = Color(0x1FFFFFFF),
        surface = Color(0xFF302A55),
        accent = Color(0xFF9AEBD5),
        onAccent = Color(0xFF221D3E),
        glow = Color(0x5282E1C8),
        primaryButton = Color(0xFFCFF5EB),
        onPrimaryButton = Color(0xFF221D3E),
        selected = Color(0xFFCFF5EB),
        onSelected = Color(0xFF221D3E),
        text = Color(0xFFF4F2FF),
        textSecondary = Color(0xFFC2BDDB),
        textSoft = Color(0xFFDAD6EE),
        statusActive = Color(0xFF9EEBC9),
    )

internal val LocalKnoblyColors = staticCompositionLocalOf { DarkKnoblyColors }

/** Colors of the dark blocks of the current theme: the dark theme itself, or [LightBlockKnoblyColors]. */
internal val LocalKnoblyBlockColors = staticCompositionLocalOf { DarkKnoblyColors }
