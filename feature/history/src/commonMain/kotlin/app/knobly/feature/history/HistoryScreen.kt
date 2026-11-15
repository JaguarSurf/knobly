package app.knobly.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.knobly.core.designsystem.GlassCard
import app.knobly.core.designsystem.KnoblyChip
import app.knobly.core.designsystem.KnoblyIcons
import app.knobly.core.designsystem.KnoblyPrimaryButton
import app.knobly.core.designsystem.KnoblySecondaryButton
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.domain.history.CallOutcome
import app.knobly.feature.history.resources.Res
import app.knobly.feature.history.resources.history_call_log_allow
import app.knobly.feature.history.resources.history_call_log_denied
import app.knobly.feature.history.resources.history_call_log_message
import app.knobly.feature.history.resources.history_call_log_title
import app.knobly.feature.history.resources.history_clear
import app.knobly.feature.history.resources.history_empty_filter
import app.knobly.feature.history.resources.history_empty_message
import app.knobly.feature.history.resources.history_empty_title
import app.knobly.feature.history.resources.history_filter_all
import app.knobly.feature.history.resources.history_filter_answered
import app.knobly.feature.history.resources.history_filter_missed
import app.knobly.feature.history.resources.history_filter_rejected
import app.knobly.feature.history.resources.history_footer
import app.knobly.feature.history.resources.history_hidden_number
import app.knobly.feature.history.resources.history_open_settings
import app.knobly.feature.history.resources.history_subtitle
import app.knobly.feature.history.resources.history_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The History tab: calls rejected by Knobly (docs/specs/call-history.md). */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        onFilterChange = viewModel::setFilter,
        onEntryClick = viewModel::select,
        onClearClick = viewModel::requestClear,
        onAllowCallLog = viewModel::allowCallLog,
        onOpenCallLogSettings = viewModel::openCallLogSettings,
        modifier = modifier,
    )
    state.selected?.let { entry ->
        HistoryDetailsSheet(
            entry = entry,
            onDismiss = viewModel::dismissDetails,
            onDelete = { viewModel.delete(entry.id) },
        )
    }
    if (state.confirmingClear) {
        ClearHistoryDialog(count = state.totalCount, onConfirm = viewModel::clear, onDismiss = viewModel::cancelClear)
    }
}

@Composable
fun HistoryContent(
    state: HistoryUiState,
    onFilterChange: (HistoryFilter) -> Unit,
    onEntryClick: (Long) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAllowCallLog: () -> Unit = {},
    onOpenCallLogSettings: () -> Unit = {},
) {
    val c = KnoblyTheme.colors
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") { Header(showClear = state.totalCount > 0, onClearClick = onClearClick) }
        if (state.loading) return@LazyColumn

        if (state.totalCount == 0) {
            item(key = "empty") {
                EmptyCard(
                    stringResource(Res.string.history_empty_title),
                    stringResource(Res.string.history_empty_message),
                )
            }
            return@LazyColumn
        }

        item(key = "filters") { Filters(state.filter, onFilterChange) }
        if (state.callLogPrompt != CallLogPrompt.NONE) {
            item(key = "call-log") { CallLogCard(state.callLogPrompt, onAllowCallLog, onOpenCallLogSettings) }
        }
        if (state.days.isEmpty()) {
            item(
                key = "empty-filter",
            ) { EmptyCard(title = null, message = stringResource(Res.string.history_empty_filter)) }
        }
        items(state.days, key = {
            it.entries
                .first()
                .date
                .toString()
        }) { day -> Day(day, onEntryClick) }
        item(key = "footer") {
            Text(
                stringResource(Res.string.history_footer),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun Header(
    showClear: Boolean,
    onClearClick: () -> Unit,
) {
    val c = KnoblyTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.history_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            if (showClear) {
                IconAction(
                    icon = KnoblyIcons.Trash,
                    label = stringResource(Res.string.history_clear),
                    onClick = onClearClick,
                    modifier = Modifier.testTag(HistoryTestTags.CLEAR),
                )
            }
        }
        Text(
            stringResource(Res.string.history_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
    }
}

@Composable
private fun IconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = KnoblyTheme.colors
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(c.glass)
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = c.textSoft, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Filters(
    selected: HistoryFilter,
    onFilterChange: (HistoryFilter) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HistoryFilter.entries.forEach { filter ->
            KnoblyChip(
                text =
                    stringResource(
                        when (filter) {
                            HistoryFilter.ALL -> Res.string.history_filter_all
                            HistoryFilter.ANSWERED -> Res.string.history_filter_answered
                            HistoryFilter.MISSED -> Res.string.history_filter_missed
                            HistoryFilter.REJECTED -> Res.string.history_filter_rejected
                        },
                    ),
                selected = filter == selected,
                onClick = { onFilterChange(filter) },
                modifier = Modifier.testTag(HistoryTestTags.filter(filter)),
            )
        }
    }
}

@Composable
private fun Day(
    day: HistoryDay,
    onEntryClick: (Long) -> Unit,
) {
    val c = KnoblyTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            day.label.text().uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = c.textSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp).semantics { heading() },
        )
        GlassCard(Modifier.fillMaxWidth(), cornerRadius = 22.dp, contentPadding = PaddingValues(horizontal = 16.dp)) {
            day.entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = c.divider)
                EntryRow(entry, onClick = { onEntryClick(entry.id) })
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
) {
    val c = KnoblyTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 12.dp)
                .testTag(HistoryTestTags.entry(entry.id)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutcomeBadge(entry.outcome)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val number = entry.number ?: stringResource(Res.string.history_hidden_number)
            Text(
                entry.name ?: number,
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
            )
            Text(
                if (entry.name != null) "${entry.statusText()} · $number" else entry.statusText(),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        Text(
            entry.time,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = c.textSoft,
        )
    }
}

/** The call's icon: blocked (rejected), arrow in (answered), bouncing arrow (missed) or a plain phone (rang). */
@Composable
internal fun OutcomeBadge(
    outcome: CallOutcome,
    modifier: Modifier = Modifier,
) {
    val c = KnoblyTheme.colors
    val (icon, tint) =
        when (outcome) {
            CallOutcome.REJECTED -> KnoblyIcons.PhoneBlocked to c.callRejected
            CallOutcome.ANSWERED -> KnoblyIcons.PhoneIncoming to c.callAnswered
            CallOutcome.MISSED -> KnoblyIcons.PhoneMissed to c.callMissed
            CallOutcome.RANG -> KnoblyIcons.Phone to c.textSecondary
        }
    Box(
        modifier.size(40.dp).clip(CircleShape).background(c.glassStrong),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Offers the call log permission, to tell the calls that rang apart (answered or missed). */
@Composable
private fun CallLogCard(
    prompt: CallLogPrompt,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = KnoblyTheme.colors
    GlassCard(Modifier.fillMaxWidth().testTag(HistoryTestTags.CALL_LOG), cornerRadius = 22.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(Res.string.history_call_log_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(
                    if (prompt ==
                        CallLogPrompt.DENIED
                    ) {
                        Res.string.history_call_log_denied
                    } else {
                        Res.string.history_call_log_message
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
            if (prompt == CallLogPrompt.DENIED) {
                KnoblySecondaryButton(
                    stringResource(Res.string.history_open_settings),
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                KnoblyPrimaryButton(
                    stringResource(Res.string.history_call_log_allow),
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth().testTag(HistoryTestTags.CALL_LOG_ALLOW),
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(
    title: String?,
    message: String,
) {
    val c = KnoblyTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (title != null) {
                OutcomeBadge(CallOutcome.RANG)
                Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
