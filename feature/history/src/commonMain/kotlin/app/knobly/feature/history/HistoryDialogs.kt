package app.knobly.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.knobly.core.designsystem.GlassCard
import app.knobly.core.designsystem.KnoblyIcons
import app.knobly.core.designsystem.KnoblySecondaryButton
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.feature.history.resources.Res
import app.knobly.feature.history.resources.history_cancel
import app.knobly.feature.history.resources.history_clear_confirm
import app.knobly.feature.history.resources.history_clear_message
import app.knobly.feature.history.resources.history_clear_title
import app.knobly.feature.history.resources.history_copied
import app.knobly.feature.history.resources.history_copy
import app.knobly.feature.history.resources.history_delete
import app.knobly.feature.history.resources.history_details_reason
import app.knobly.feature.history.resources.history_details_when
import app.knobly.feature.history.resources.history_hidden_number
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Details of one rejected call, with "copy number" and "delete entry". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryDetailsSheet(
    entry: HistoryEntry,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = KnoblyTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        contentColor = c.text,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                .testTag(HistoryTestTags.DETAILS),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BlockedBadge()
                Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        entry.number ?: stringResource(Res.string.history_hidden_number),
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    )
                    Text(
                        stringResource(Res.string.history_details_when, entry.date.text(showYear = true), entry.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }
            GlassCard(Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(16.dp)) {
                Text(
                    stringResource(Res.string.history_details_reason),
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textSecondary,
                )
                Text(
                    entry.reasonText(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            entry.number?.let { number -> CopyNumberButton(number) }
            KnoblySecondaryButton(
                text = stringResource(Res.string.history_delete),
                onClick = onDelete,
                icon = KnoblyIcons.Trash,
                modifier = Modifier.fillMaxWidth().testTag(HistoryTestTags.DELETE),
            )
        }
    }
}

@Composable
private fun CopyNumberButton(number: String) {
    // LocalClipboard (its replacement) needs a platform-specific ClipEntry; plain text is all we need here.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var copied by remember(number) { mutableStateOf(false) }
    KnoblySecondaryButton(
        text = stringResource(if (copied) Res.string.history_copied else Res.string.history_copy),
        onClick = {
            clipboard.setText(AnnotatedString(number))
            copied = true
        },
        icon = KnoblyIcons.Copy,
        modifier = Modifier.fillMaxWidth().testTag(HistoryTestTags.COPY),
    )
}

/** Confirmation before deleting the whole history. */
@Composable
internal fun ClearHistoryDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = KnoblyTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        titleContentColor = c.text,
        textContentColor = c.textSoft,
        title = { Text(stringResource(Res.string.history_clear_title)) },
        text = { Text(pluralStringResource(Res.plurals.history_clear_message, count, count)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(HistoryTestTags.CLEAR_CONFIRM)) {
                Text(stringResource(Res.string.history_clear_confirm), color = c.alert)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.history_cancel), color = c.text) }
        },
    )
}
