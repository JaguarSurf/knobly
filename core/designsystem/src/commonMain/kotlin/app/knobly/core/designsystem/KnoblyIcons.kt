package app.knobly.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Knobly's stroke icon set (24×24, 2 px rounded strokes), matching the mockups. Tint them with `Icon(tint = …)`.
 */
object KnoblyIcons {
    val Home: ImageVector by lazy { strokeIcon("Home", "M4 11l8-7 8 7v9a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1z") }

    val Filters: ImageVector by lazy {
        strokeIcon(
            "Filters",
            "M4 6h16",
            "M4 12h16",
            "M4 18h16",
            filled = listOf(circle(9f, 6f, 2.5f), circle(15f, 12f, 2.5f), circle(7f, 18f, 2.5f)),
        )
    }

    val History: ImageVector by lazy { strokeIcon("History", circle(12f, 12f, 9f), "M12 7v5l3 2") }

    val Stats: ImageVector by lazy { strokeIcon("Stats", "M3 17c3 0 4-6 7-6s3 4 6 4 3-7 5-7") }

    val Phone: ImageVector by lazy { strokeIcon("Phone", PHONE) }

    /** Answered incoming call: phone + arrow pointing in. */
    val PhoneIncoming: ImageVector by lazy { strokeIcon("PhoneIncoming", PHONE, "M21 3l-6 6", "M15 4v5h5") }

    /** Missed call: phone + bouncing arrow. */
    val PhoneMissed: ImageVector by lazy { strokeIcon("PhoneMissed", PHONE, "M14 3l3.5 3.5L22 2") }

    val PhoneBlocked: ImageVector by lazy { strokeIcon("PhoneBlocked", PHONE, "M21 3L3 21") }

    val Person: ImageVector by lazy { strokeIcon("Person", circle(12f, 8f, 4f), "M4 21c0-4 4-6 8-6s8 2 8 6") }

    val Rules: ImageVector by lazy { strokeIcon("Rules", "M4 7h16", "M4 12h16", "M4 17h10") }

    val Message: ImageVector by lazy { strokeIcon("Message", "M4 5h16v11H8l-4 4z") }

    val Bell: ImageVector by lazy { strokeIcon("Bell", "M6 16V11a6 6 0 0 1 12 0v5l2 2H4z", "M10 21h4") }

    val Plus: ImageVector by lazy { strokeIcon("Plus", "M12 5v14", "M5 12h14") }

    val ChevronRight: ImageVector by lazy { strokeIcon("ChevronRight", "M9 6l6 6-6 6") }

    val Back: ImageVector by lazy { strokeIcon("Back", "M19 12H5", "M12 19l-7-7 7-7") }

    val Prefix: ImageVector by lazy { strokeIcon("Prefix", "M4 7h7", "M4 12h5", "M4 17h7", "M15 9l4 3-4 3") }

    val Hidden: ImageVector by lazy { strokeIcon("Hidden", "M3 12s3-6 9-6 9 6 9 6-3 6-9 6-9-6-9-6z", "M4 4l16 16") }

    val Regex: ImageVector by lazy {
        strokeIcon("Regex", "M8 5c-3 3-3 11 0 14", "M16 5c3 3 3 11 0 14", "M12 8v6", "M9.5 9.5l5 3", "M14.5 9.5l-5 3")
    }

    val Trash: ImageVector by lazy {
        strokeIcon("Trash", "M4 7h16", "M10 11v6", "M14 11v6", "M6 7l1 13h10l1-13", "M9 7V4h6v3")
    }

    val Copy: ImageVector by lazy { strokeIcon("Copy", "M9 9h11v11H9z", "M5 15H4V4h11v1") }

    val Sun: ImageVector by lazy {
        strokeIcon(
            "Sun",
            circle(12f, 12f, 4f),
            "M12 2v2",
            "M12 20v2",
            "M4.9 4.9l1.4 1.4",
            "M17.7 17.7l1.4 1.4",
            "M2 12h2",
            "M20 12h2",
            "M4.9 19.1l1.4-1.4",
            "M17.7 6.3l1.4-1.4",
        )
    }

    val Moon: ImageVector by lazy { strokeIcon("Moon", "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z") }

    /** Half-filled circle: "follow the system" theme. */
    val ThemeSystem: ImageVector by lazy {
        strokeIcon("ThemeSystem", circle(12f, 12f, 9f), filled = listOf("M12 3a9 9 0 0 1 0 18z"))
    }

    val Check: ImageVector by lazy { strokeIcon("Check", "M5 12.5l4.5 4.5L19 7.5") }

    private const val PHONE =
        "M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2"

    private fun circle(
        cx: Float,
        cy: Float,
        r: Float,
    ) = "M${cx - r} ${cy}a$r $r 0 1 0 ${r + r} 0a$r $r 0 1 0 ${-(r + r)} 0"

    private fun strokeIcon(
        name: String,
        vararg paths: String,
        filled: List<String> = emptyList(),
    ): ImageVector =
        ImageVector
            .Builder(
                name = "Knobly.$name",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                paths.forEach { path ->
                    addPath(
                        pathData = addPathNodes(path),
                        fill = null,
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
                filled.forEach { path -> addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)) }
            }.build()
}
