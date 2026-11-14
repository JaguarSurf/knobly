package app.knobly.feature.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.knobly.core.designsystem.GlassCard
import app.knobly.core.designsystem.KnoblyBackground
import app.knobly.core.designsystem.KnoblyFaders
import app.knobly.core.designsystem.KnoblyIcons
import app.knobly.core.designsystem.KnoblyPrimaryButton
import app.knobly.core.designsystem.KnoblyRoundIcon
import app.knobly.core.designsystem.KnoblyStatus
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.feature.home.resources.Res
import app.knobly.feature.home.resources.home_calls
import app.knobly.feature.home.resources.home_calls_enable
import app.knobly.feature.home.resources.home_calls_off
import app.knobly.feature.home.resources.home_calls_on
import app.knobly.feature.home.resources.home_calls_unavailable
import app.knobly.feature.home.resources.home_coming_soon
import app.knobly.feature.home.resources.home_controls
import app.knobly.feature.home.resources.home_greeting_afternoon
import app.knobly.feature.home.resources.home_greeting_evening
import app.knobly.feature.home.resources.home_greeting_morning
import app.knobly.feature.home.resources.home_headline_end
import app.knobly.feature.home.resources.home_headline_start
import app.knobly.feature.home.resources.home_module_messages
import app.knobly.feature.home.resources.home_module_notifications
import app.knobly.feature.home.resources.home_more_controls
import app.knobly.feature.home.resources.home_open_calls
import app.knobly.feature.home.resources.home_status_off
import app.knobly.feature.home.resources.home_status_protected
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

/** The control center: status of each module (calls for now) and the way into its controls. */
@Composable
fun HomeScreen(
    onOpenCalls: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val callFilteringOn by viewModel.isCallFilteringOn.collectAsStateWithLifecycle()
    val hour =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .hour
    HomeContent(
        dayPart = DayPart.fromHour(hour),
        isCallFilteringAvailable = viewModel.isCallFilteringAvailable,
        isCallFilteringOn = callFilteringOn,
        onEnableCallFiltering = viewModel::enableCallFiltering,
        onOpenCalls = onOpenCalls,
        modifier = modifier,
    )
}

@Composable
fun HomeContent(
    dayPart: DayPart,
    isCallFilteringAvailable: Boolean,
    isCallFilteringOn: Boolean,
    onEnableCallFiltering: () -> Unit,
    onOpenCalls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = KnoblyTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState(),
                ).padding(horizontal = 18.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    when (dayPart) {
                        DayPart.MORNING -> Res.string.home_greeting_morning
                        DayPart.AFTERNOON -> Res.string.home_greeting_afternoon
                        DayPart.EVENING -> Res.string.home_greeting_evening
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary,
            )
            val start = stringResource(Res.string.home_headline_start)
            val end = stringResource(Res.string.home_headline_end)
            Text(
                buildAnnotatedString {
                    append(start)
                    append("\n")
                    withStyle(SpanStyle(color = c.accent)) { append(end) }
                },
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        CallsHeroCard(isCallFilteringAvailable, isCallFilteringOn, onEnableCallFiltering)

        Text(
            stringResource(Res.string.home_controls),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleTile(
                    title = stringResource(Res.string.home_calls),
                    subtitle =
                        stringResource(
                            if (isCallFilteringOn) Res.string.home_status_protected else Res.string.home_status_off,
                        ),
                    icon = KnoblyIcons.Phone,
                    highlighted = isCallFilteringOn,
                    onClick = onOpenCalls,
                    onClickLabel = stringResource(Res.string.home_open_calls),
                    modifier = Modifier.weight(1f).testTag(HomeTestTags.CALLS_TILE),
                )
                ModuleTile(
                    title = stringResource(Res.string.home_module_messages),
                    subtitle = stringResource(Res.string.home_coming_soon),
                    icon = KnoblyIcons.Message,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleTile(
                    title = stringResource(Res.string.home_module_notifications),
                    subtitle = stringResource(Res.string.home_coming_soon),
                    icon = KnoblyIcons.Bell,
                    modifier = Modifier.weight(1f),
                )
                MoreControlsTile(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CallsHeroCard(
    isAvailable: Boolean,
    isOn: Boolean,
    onEnable: () -> Unit,
) {
    val c = KnoblyTheme.colors
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 26.dp, contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KnoblyStatus(
                    stringResource(if (isOn) Res.string.home_status_protected else Res.string.home_status_off),
                    active = isOn,
                )
                Text(stringResource(Res.string.home_calls), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(
                        when {
                            !isAvailable -> Res.string.home_calls_unavailable
                            isOn -> Res.string.home_calls_on
                            else -> Res.string.home_calls_off
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            KnoblyFaders(active = isOn)
        }
        if (isAvailable && !isOn) {
            KnoblyPrimaryButton(
                text = stringResource(Res.string.home_calls_enable),
                onClick = onEnable,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag(HomeTestTags.ENABLE_CALL_FILTERING),
            )
        }
    }
}

@Composable
private fun ModuleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    val c = KnoblyTheme.colors
    val enabled = onClick != null
    GlassCard(
        modifier =
            modifier
                .clip(RoundedCornerShape(22.dp))
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(Modifier.alpha(if (enabled) 1f else 0.7f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KnoblyRoundIcon(icon, highlighted = highlighted)
                if (enabled) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                        Icon(
                            KnoblyIcons.ChevronRight,
                            contentDescription = null,
                            tint = c.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) c.text else c.textSoft)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun MoreControlsTile(modifier: Modifier = Modifier) {
    val c = KnoblyTheme.colors
    Column(
        modifier =
            modifier
                .heightIn(min = 120.dp)
                .drawBehind {
                    drawRoundRect(
                        color = c.textSecondary.copy(alpha = 0.35f),
                        cornerRadius = CornerRadius(22.dp.toPx()),
                        style =
                            Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                            ),
                    )
                }.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Icon(KnoblyIcons.Plus, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
        Text(
            stringResource(Res.string.home_more_controls),
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun HomeContentPreview() {
    KnoblyTheme {
        KnoblyBackground {
            HomeContent(
                dayPart = DayPart.EVENING,
                isCallFilteringAvailable = true,
                isCallFilteringOn = true,
                onEnableCallFiltering = {},
                onOpenCalls = {},
            )
        }
    }
}
