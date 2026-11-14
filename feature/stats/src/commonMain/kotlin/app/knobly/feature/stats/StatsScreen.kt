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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.knobly.core.designsystem.GlassCard
import app.knobly.core.designsystem.KnoblyLineChart
import app.knobly.core.designsystem.KnoblySegmentedControl
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.domain.stats.CallStats
import app.knobly.domain.stats.FilterCount
import app.knobly.domain.stats.StatsPeriod
import app.knobly.feature.stats.resources.Res
import app.knobly.feature.stats.resources.stats_by_filter
import app.knobly.feature.stats.resources.stats_by_filter_empty
import app.knobly.feature.stats.resources.stats_footer
import app.knobly.feature.stats.resources.stats_module_calls
import app.knobly.feature.stats.resources.stats_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The Statistics tab: rejected calls per hour, day, week, month and year (docs/specs/call-statistics.md). */
@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatsContent(state, onPeriodChange = viewModel::setPeriod, modifier = modifier)
}

@Composable
fun StatsContent(
    state: StatsUiState,
    onPeriodChange: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
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
        state.stats?.let { stats ->
            ChartCard(stats)
            ByFilterCard(stats.byFilter)
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
private fun ChartCard(stats: CallStats) {
    val c = KnoblyTheme.colors
    val caption = stats.period.captionText(stats.total)
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) {}.testTag(StatsTestTags.TOTAL),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    stats.total.toString(),
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
            values = stats.buckets.map { it.count },
            labels = stats.period.bucketLabels(stats.buckets.map { it.start }),
            contentDescription = "${stats.total} $caption",
            modifier = Modifier.padding(top = 12.dp).testTag(StatsTestTags.CHART),
        )
    }
}

@Composable
private fun ByFilterCard(byFilter: List<FilterCount>) {
    val c = KnoblyTheme.colors
    GlassCard(Modifier.fillMaxWidth().testTag(StatsTestTags.BY_FILTER)) {
        Text(
            stringResource(Res.string.stats_by_filter),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        if (byFilter.isEmpty()) {
            Text(
                stringResource(Res.string.stats_by_filter_empty),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        val max = byFilter.maxOfOrNull { it.count } ?: 1
        byFilter.forEach { item -> FilterRow(item, fraction = item.count.toFloat() / max) }
    }
}

@Composable
private fun FilterRow(
    item: FilterCount,
    fraction: Float,
) {
    val c = KnoblyTheme.colors
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(item.filter.text(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(
                item.count.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            )
        }
        val shape = RoundedCornerShape(3.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(shape)
                .background(c.glassStrong),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(shape)
                    .background(Brush.horizontalGradient(listOf(c.toggleOn, c.accent))),
            )
        }
    }
}
