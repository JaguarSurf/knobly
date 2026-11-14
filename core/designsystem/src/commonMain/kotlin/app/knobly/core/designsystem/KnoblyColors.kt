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
)

/** Dark theme "Noturno", mint accent. */
val DarkKnoblyColors =
    KnoblyColors(
        isDark = true,
        backgroundTop = Color(0xFF171233),
        backgroundMiddle = Color(0xFF0E0B1F),
        backgroundBottom = Color(0xFF0B0918),
        backgroundGlow = Color(0x4D1FB894),
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
    )

/** Light theme "Pedra", sage accent. */
val LightKnoblyColors =
    KnoblyColors(
        isDark = false,
        backgroundTop = Color(0xFFF4F1EC),
        backgroundMiddle = Color(0xFFE9E4DC),
        backgroundBottom = Color(0xFFE1DAD0),
        backgroundGlow = Color(0x99FFFFFF),
        glass = Color(0x8CFFFFFF),
        glassStrong = Color(0xCCFFFFFF),
        glassBorder = Color(0xE6FFFFFF),
        glassShadow = Color(0x14504632),
        surface = Color(0xFFFBF9F6),
        accent = Color(0xFF5E6E4B),
        onAccent = Color(0xFFFFFFFF),
        glow = Color.Transparent,
        primaryButton = Color(0xFFB9A888),
        onPrimaryButton = Color(0xFF2E2B26),
        selected = Color(0xFF2E2B26),
        onSelected = Color(0xFFF4F1EC),
        text = Color(0xFF2E2B26),
        textSecondary = Color(0xFF6B645A),
        textSoft = Color(0xFF4A453E),
        divider = Color(0x142E2B26),
        toggleOn = Color(0xFF5E6E4B),
        toggleOff = Color(0x292E2B26),
        thumbOn = Color(0xFFFFFFFF),
        thumbOff = Color(0xFFFFFFFF),
        statusActive = Color(0xFF5E8C4A),
        statusInactive = Color(0xFFA39C90),
        alert = Color(0xFFC8553D),
        callRejected = Color(0xFFC0392B),
        callAnswered = Color(0xFF3F7F3A),
        callMissed = Color(0xFFA86A12),
    )

internal val LocalKnoblyColors = staticCompositionLocalOf { DarkKnoblyColors }
