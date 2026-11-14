package app.knobly.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.knobly.core.designsystem.KnoblyIcons
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.domain.settings.ThemeMode
import app.knobly.feature.home.resources.Res
import app.knobly.feature.home.resources.home_theme
import app.knobly.feature.home.resources.home_theme_dark
import app.knobly.feature.home.resources.home_theme_light
import app.knobly.feature.home.resources.home_theme_system
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Round button with the current theme's icon; opens System / Light / Dark. */
@Composable
internal fun ThemeMenuButton(
    mode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = KnoblyTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(Res.string.home_theme, stringResource(mode.title))
    Box(modifier) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(c.glass)
                .clickable(role = Role.Button, onClickLabel = label) { expanded = true }
                .testTag(HomeTestTags.THEME),
            contentAlignment = Alignment.Center,
        ) {
            Icon(mode.icon, contentDescription = label, tint = c.textSoft, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = c.surface,
        ) {
            ThemeMode.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(option.title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.text,
                        )
                    },
                    leadingIcon = { Icon(option.icon, contentDescription = null, tint = c.textSoft) },
                    trailingIcon = {
                        if (option == mode) Icon(KnoblyIcons.Check, contentDescription = null, tint = c.accent)
                    },
                    onClick = {
                        expanded = false
                        onModeChange(option)
                    },
                    modifier =
                        Modifier.semantics { selected = option == mode }.testTag(
                            HomeTestTags.themeOption(option),
                        ),
                )
            }
        }
    }
}

private val ThemeMode.title: StringResource
    get() =
        when (this) {
            ThemeMode.SYSTEM -> Res.string.home_theme_system
            ThemeMode.LIGHT -> Res.string.home_theme_light
            ThemeMode.DARK -> Res.string.home_theme_dark
        }

private val ThemeMode.icon: ImageVector
    get() =
        when (this) {
            ThemeMode.SYSTEM -> KnoblyIcons.ThemeSystem
            ThemeMode.LIGHT -> KnoblyIcons.Sun
            ThemeMode.DARK -> KnoblyIcons.Moon
        }
