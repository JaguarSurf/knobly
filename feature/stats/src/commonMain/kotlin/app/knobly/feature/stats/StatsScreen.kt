package app.knobly.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.knobly.core.designsystem.GlassCard
import app.knobly.core.designsystem.KnoblyChip
import app.knobly.core.designsystem.KnoblyLineChart
import app.knobly.core.designsystem.KnoblyPrimaryButton
import app.knobly.core.designsystem.KnoblySecondaryButton
import app.knobly.core.designsystem.KnoblySegmentedControl
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.domain.stats.CallCategory
import app.knobly.domain.stats.CallStats
import app.knobly.domain.stats.FilterCount
import app.knobly.domain.stats.StatsPeriod
import app.knobly.feature.stats.resources.Res
import app.knobly.feature.stats.resources.stats_by_filter
import app.knobly.feature.stats.resources.stats_by_filter_empty
import app.knobly.feature.stats.resources.stats_by_type
import app.knobly.feature.stats.resources.stats_call_log_allow
import app.knobly.feature.stats.resources.stats_call_log_denied
import app.knobly.feature.stats.resources.stats_call_log_message
import app.knobly.feature.stats.resources.stats_call_log_title
import app.knobly.feature.stats.resources.stats_footer
import app.knobly.feature.stats.resources.stats_module_calls
import app.knobly.feature.stats.resources.stats_open_settings
import app.knobly.feature.stats.resources.stats_title
import app.knobly.feature.stats.resources.stats_type_rang
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The Statistics tab: incoming calls per hour, day, week, month and year (docs/specs/call-statistics.md). */
@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatsContent(
        state,
        onPeriodChange = viewModel::setPeriod,
        onCategoryChange = viewModel::setCategory,
        onAllowCallLog = viewModel::allowCallLog,
        onOpenCallLogSettings = viewModel::openCallLogSettings,
        modifier = modifier,
    )
}

@Composable
fun StatsContent(
    state: StatsUiState,
    onPeriodChange: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
    onCategoryChange: (CallCategory) -> Unit = {},
    onAllowCallLog: () -> Unit = {},
    onOpenCallLogSettings: () -> Unit = {},
) {
    val c = KnoblyTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(Res.string.stats_title), style = MaterialTheme.typography.headlineMedium)
        KnoblySegmentedControl(
            options = StatsPeriod.entries.map { stringResource(it.title) },
            selectedIndex = state.period.ordinal,
            onSelect = { onPeriodChange(StatsPeriod.entries[it]) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CallCategory.entries.forEach { category ->
                KnoblyChip(
                    stringResource(category.title),
                    selected = category == state.category,
                    onClick = { onCategoryChange(category) },
                    modifier = Modifier.testTag(StatsTestTags.category(category)),
                )
            }
        }
        state.stats?.let { stats ->
            ChartCard(stats, state.category)
            if (state.askCallLog) CallLogCard(state.callLogDenied, onAllowCallLog, onOpenCallLogSettings)
            ByTypeCard(stats)
            if (state.category == CallCategory.ALL || state.category == CallCategory.REJECTED) {
                ByFilterCard(stats.byFilter)
            }
        }
        Text(
            stringResource(Res.string.stats_footer),
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChartCard(
    stats: CallStats,
    category: CallCategory,
) {
    val c = KnoblyTheme.colors
    val total = stats.totals.of(category)
    val caption = captionText(stats.period, category, total)
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) {}.testTag(StatsTestTags.TOTAL),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    total.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp, fontFeatureSettings = "tnum"),
                )
                Text(caption, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            Text(
                stringResource(Res.string.stats_module_calls),
                style = MaterialTheme.typography.bodySmall,
                color = c.accent,
            )
        }
        KnoblyLineChart(
            values = stats.buckets.map { it.counts.of(category) },
            labels = stats.period.bucketLabels(stats.buckets.map { it.start }),
            contentDescription = "$total $caption",
            modifier = Modifier.padding(top = 12.dp).testTag(StatsTestTags.CHART),
        )
    }
}

/** Answered, missed, "received" (outcome unknown) and rejected calls of the period. */
@Composable
private fun ByTypeCard(stats: CallStats) {
    val c = KnoblyTheme.colors
    val totals = stats.totals
    val rows =
        buildList {
            add(Triple(stringResource(CallCategory.ANSWERED.title), totals.answered, c.callAnswered))
            add(Triple(stringResource(CallCategory.MISSED.title), totals.missed, c.callMissed))
            if (totals.rang > 0) add(Triple(stringResource(Res.string.stats_type_rang), totals.rang, c.textSecondary))
            add(Triple(stringResource(CallCategory.REJECTED.title), totals.rejected, c.callRejected))
        }
    val max = rows.maxOf { it.second }.coerceAtLeast(1)
    GlassCard(Modifier.fillMaxWidth().testTag(StatsTestTags.BY_TYPE)) {
        CardTitle(stringResource(Res.string.stats_by_type))
        rows.forEach { (label, count, color) -> BarRow(label, count, count.toFloat() / max, color) }
    }
}

@Composable
private fun ByFilterCard(byFilter: List<FilterCount>) {
    val c = KnoblyTheme.colors
    GlassCard(Modifier.fillMaxWidth().testTag(StatsTestTags.BY_FILTER)) {
        CardTitle(stringResource(Res.string.stats_by_filter))
        if (byFilter.isEmpty()) {
            Text(
                stringResource(Res.string.stats_by_filter_empty),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        val max = byFilter.maxOfOrNull { it.count } ?: 1
        byFilter.forEach { item -> BarRow(item.filter.text(), item.count, item.count.toFloat() / max, c.callRejected) }
    }
}

/** Offers the call log permission, to tell the calls that rang apart (answered or missed). */
@Composable
private fun CallLogCard(
    denied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = KnoblyTheme.colors
    GlassCard(Modifier.fillMaxWidth().testTag(StatsTestTags.CALL_LOG), cornerRadius = 22.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(Res.string.stats_call_log_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(if (denied) Res.string.stats_call_log_denied else Res.string.stats_call_log_message),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
            if (denied) {
                KnoblySecondaryButton(
                    stringResource(Res.string.stats_open_settings),
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                KnoblyPrimaryButton(
                    stringResource(Res.string.stats_call_log_allow),
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
}

@Composable
private fun BarRow(
    label: String,
    count: Int,
    fraction: Float,
    color: Color,
) {
    val c = KnoblyTheme.colors
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(count.toString(), style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"))
        }
        val shape = RoundedCornerShape(3.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(shape)
                .background(c.glassStrong),
        ) {
            if (count > 0) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(shape)
                        .background(color),
                )
            }
        }
    }
}
