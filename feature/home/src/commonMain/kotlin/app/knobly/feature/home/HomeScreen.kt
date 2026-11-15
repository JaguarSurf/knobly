package app.knobly.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
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
import app.knobly.core.designsystem.poppinsFamily
import app.knobly.domain.settings.ThemeMode
import app.knobly.feature.home.resources.Res
import app.knobly.feature.home.resources.home_active_filters
import app.knobly.feature.home.resources.home_calls
import app.knobly.feature.home.resources.home_calls_enable
import app.knobly.feature.home.resources.home_calls_no_filters
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
import app.knobly.feature.home.resources.home_hidden_number
import app.knobly.feature.home.resources.home_latest_date
import app.knobly.feature.home.resources.home_latest_today
import app.knobly.feature.home.resources.home_latest_yesterday
import app.knobly.feature.home.resources.home_module_messages
import app.knobly.feature.home.resources.home_module_notifications
import app.knobly.feature.home.resources.home_more_controls
import app.knobly.feature.home.resources.home_open_calls
import app.knobly.feature.home.resources.home_open_history
import app.knobly.feature.home.resources.home_stat_today
import app.knobly.feature.home.resources.home_stat_week
import app.knobly.feature.home.resources.home_status_no_filters
import app.knobly.feature.home.resources.home_status_off
import app.knobly.feature.home.resources.home_status_protected
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The control center: status of each module (calls for now) and the way into its controls. */
@Composable
fun HomeScreen(
    onOpenCalls: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
    themeViewModel: ThemeViewModel = koinViewModel(),
) {
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val callsStatus by viewModel.callsStatus.collectAsStateWithLifecycle()
    val activity by viewModel.activity.collectAsStateWithLifecycle()
    HomeContent(
        dayPart = viewModel.dayPart,
        isCallFilteringAvailable = viewModel.isCallFilteringAvailable,
        callsStatus = callsStatus,
        activity = activity,
        onEnableCallFiltering = viewModel::enableCallFiltering,
        onOpenCalls = onOpenCalls,
        onOpenHistory = onOpenHistory,
        themeMode = themeMode,
        onThemeModeChange = themeViewModel::setThemeMode,
        modifier = modifier,
    )
}

@Composable
fun HomeContent(
    dayPart: DayPart,
    isCallFilteringAvailable: Boolean,
    callsStatus: CallsStatus,
    activity: CallsActivity,
    onEnableCallFiltering: () -> Unit,
    onOpenCalls: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
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
        Row(verticalAlignment = Alignment.Top) {
            Greeting(dayPart, Modifier.weight(1f))
            ThemeMenuButton(themeMode, onThemeModeChange)
        }

        CallsHeroCard(isCallFilteringAvailable, callsStatus, activity, onEnableCallFiltering)

        Text(
            stringResource(Res.string.home_controls),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleTile(
                    title = stringResource(Res.string.home_calls),
                    subtitle = callsStatus.shortText(),
                    icon = KnoblyIcons.Phone,
                    highlighted = callsStatus.isProtected,
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

        activity.latest?.let { LatestRejectionCard(it, onOpenHistory) }
    }
}

@Composable
private fun CallsHeroCard(
    isAvailable: Boolean,
    status: CallsStatus,
    activity: CallsActivity,
    onEnable: () -> Unit,
) {
    val c = KnoblyTheme.colors
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 26.dp, contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KnoblyStatus(
                    stringResource(
                        when {
                            status.isProtected -> Res.string.home_status_protected
                            status.screeningOn -> Res.string.home_status_no_filters
                            else -> Res.string.home_status_off
                        },
                    ),
                    active = status.isProtected,
                )
                Text(stringResource(Res.string.home_calls), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(
                        when {
                            !isAvailable -> Res.string.home_calls_unavailable
                            status.isProtected -> Res.string.home_calls_on
                            status.screeningOn -> Res.string.home_calls_no_filters
                            else -> Res.string.home_calls_off
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            KnoblyFaders(active = status.isProtected)
        }
        if (status.screeningOn) {
            // Below the text and the faders, across the whole card: on narrow phones a pill moves to the next line
            // instead of breaking its text.
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatPill(activity.today, stringResource(Res.string.home_stat_today))
                StatPill(activity.lastSevenDays, stringResource(Res.string.home_stat_week))
            }
        }
        if (isAvailable && !status.screeningOn) {
            KnoblyPrimaryButton(
                text = stringResource(Res.string.home_calls_enable),
                onClick = onEnable,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag(HomeTestTags.ENABLE_CALL_FILTERING),
            )
        }
    }
}

/** "Boa noite" + "Seu celular, sob controle." */
@Composable
private fun Greeting(
    dayPart: DayPart,
    modifier: Modifier = Modifier,
) {
    val c = KnoblyTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
}

/** "3 hoje": a number of rejected calls in a small glass pill. */
@Composable
private fun StatPill(
    count: Int,
    label: String,
) {
    val c = KnoblyTheme.colors
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = poppinsFamily(), fontWeight = FontWeight.SemiBold, color = c.text)) {
                append(count.toString())
            }
            append(" ")
            append(label)
        },
        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
        color = c.textSoft,
        maxLines = 1,
        softWrap = false,
        modifier =
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(c.glass)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag(HomeTestTags.STATS),
    )
}

/** The latest rejected call, opening the History tab (Home mockup). */
@Composable
private fun LatestRejectionCard(
    latest: LatestRejection,
    onOpenHistory: () -> Unit,
) {
    val c = KnoblyTheme.colors
    val openHistory = stringResource(Res.string.home_open_history)
    GlassCard(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClickLabel = openHistory, role = Role.Button, onClick = onOpenHistory)
            .testTag(HomeTestTags.LATEST_REJECTION),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(c.glassStrong),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    KnoblyIcons.PhoneBlocked,
                    contentDescription = null,
                    tint = c.callRejected,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    latest.number ?: stringResource(Res.string.home_hidden_number),
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                )
                Text(
                    when (latest.daysAgo) {
                        0 -> {
                            stringResource(Res.string.home_latest_today, latest.time)
                        }

                        1 -> {
                            stringResource(Res.string.home_latest_yesterday, latest.time)
                        }

                        else -> {
                            stringResource(
                                Res.string.home_latest_date,
                                latest.date.day,
                                latest.date.month.ordinal + 1,
                                latest.time,
                            )
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            Icon(
                KnoblyIcons.ChevronRight,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Short status for the calls tile: "2 filtros ativos", "Nenhum filtro ativo" or "Desligado". */
@Composable
private fun CallsStatus.shortText(): String =
    when {
        isProtected -> pluralStringResource(Res.plurals.home_active_filters, activeFilterCount, activeFilterCount)
        screeningOn -> stringResource(Res.string.home_status_no_filters)
        else -> stringResource(Res.string.home_status_off)
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
                callsStatus = CallsStatus(screeningOn = true, activeFilterCount = 2),
                activity = CallsActivity(today = 3, lastSevenDays = 23),
                onEnableCallFiltering = {},
                onOpenCalls = {},
                onOpenHistory = {},
            )
        }
    }
}
