package app.knobly.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.knobly.core.designsystem.resources.Res
import app.knobly.core.designsystem.resources.inter_medium
import app.knobly.core.designsystem.resources.inter_regular
import app.knobly.core.designsystem.resources.inter_semibold
import app.knobly.core.designsystem.resources.poppins_medium
import app.knobly.core.designsystem.resources.poppins_semibold
import org.jetbrains.compose.resources.Font

/** Poppins: titles, large numbers and primary buttons. Bundled with the app (SIL OFL), never downloaded. */
@Composable
fun poppinsFamily(): FontFamily =
    FontFamily(
        Font(Res.font.poppins_medium, FontWeight.Medium),
        Font(Res.font.poppins_semibold, FontWeight.SemiBold),
    )

/** Inter: interface text. Bundled with the app (SIL OFL), never downloaded. */
@Composable
fun interFamily(): FontFamily =
    FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
    )

@Composable
internal fun knoblyTypography(): Typography {
    val poppins = poppinsFamily()
    val inter = interFamily()

    fun poppins(
        size: Int,
        weight: FontWeight = FontWeight.SemiBold,
        lineHeight: Int = size + 8,
    ) = TextStyle(fontFamily = poppins, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp)

    fun inter(
        size: Int,
        weight: FontWeight = FontWeight.Normal,
        lineHeight: Int = size + 6,
    ) = TextStyle(fontFamily = inter, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp)

    return Typography(
        displayLarge = poppins(48, lineHeight = 54),
        displayMedium = poppins(44, lineHeight = 50),
        displaySmall = poppins(38, lineHeight = 44),
        headlineLarge = poppins(32, lineHeight = 40),
        headlineMedium = poppins(28, lineHeight = 34),
        headlineSmall = poppins(22, lineHeight = 28),
        titleLarge = poppins(20, lineHeight = 26),
        titleMedium = inter(15, FontWeight.SemiBold, lineHeight = 21),
        titleSmall = inter(13, FontWeight.SemiBold, lineHeight = 18),
        bodyLarge = inter(16, lineHeight = 24),
        bodyMedium = inter(14, lineHeight = 21),
        bodySmall = inter(12, lineHeight = 17),
        labelLarge = poppins(15, lineHeight = 20),
        labelMedium = inter(13, FontWeight.SemiBold, lineHeight = 18),
        labelSmall = inter(12, FontWeight.Medium, lineHeight = 16),
    )
}
