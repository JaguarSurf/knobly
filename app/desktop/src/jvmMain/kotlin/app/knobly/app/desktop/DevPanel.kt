package app.knobly.app.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.core.model.Action
import app.knobly.core.model.Decision
import app.knobly.core.model.IncomingCall
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.history.CallHistoryRecorder
import app.knobly.domain.history.CallLogSync
import app.knobly.domain.port.CallLogEntry
import app.knobly.domain.port.CallLogType
import app.knobly.domain.rules.call.CallScreener
import app.knobly.platform.fake.FakeCallLogAccess
import app.knobly.platform.fake.FakeContactsAccess
import app.knobly.platform.fake.FakeScreeningRoleController
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

private data class SimulatedCall(
    val input: String,
    val call: IncomingCall,
    val decision: Decision,
)

/**
 * Developer tools next to the app, in three sections: the simulated phone ([device]: screen, font scale, language),
 * the fake platform adapters, and an incoming call simulator that goes through the real normalizer and rule engine
 * (the same code the Android CallScreeningService runs).
 */
@Composable
fun DevPanel(
    device: DeviceFrameState,
    modifier: Modifier = Modifier,
) {
    KnoblyTheme {
        Surface(modifier) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Dev tools", style = MaterialTheme.typography.headlineSmall)
                Section("Device") { DeviceControls(device) }
                Section("Platform (fakes)") { PlatformControls() }
                Section("Incoming calls") { CallSimulator() }
            }
        }
    }
}

/** Simulated phone: screen size and orientation, font scale and language. */
@Composable
private fun DeviceControls(device: DeviceFrameState) {
    Field("Screen", alignment = Alignment.Top) {
        ScreenPresets(device.preset, onSelect = { device.preset = it }, modifier = Modifier.weight(1f))
    }
    Field("Orientation") {
        Switch(checked = device.landscape, onCheckedChange = { device.landscape = it })
        Text("Landscape")
    }
    Field("Font scale") {
        Choices(FontScales, device.fontScale, onSelect = { device.fontScale = it }, label = { "$it" })
    }
    Field("Language") {
        Choices(HarnessLanguage.entries, device.language, onSelect = { device.language = it }, label = { it.label })
    }
}

/** One click per screen: a tile for each preset (name and size), and the real phone it matches, if any. */
@Composable
private fun ScreenPresets(
    selected: DevicePreset,
    onSelect: (DevicePreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3,
        ) {
            DevicePreset.entries.forEach { preset ->
                ChoiceTile(
                    selected = preset == selected,
                    onClick = { onSelect(preset) },
                    title = preset.label,
                    subtitle = preset.size.asText(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        selected.example?.let {
            Text(
                "${selected.label}: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun DpSize.asText() = "${width.value.toInt()} × ${height.value.toInt()} dp"

/** The fake platform adapters: the call screening role and the contacts permission. */
@Composable
private fun PlatformControls() {
    val roleController = koinInject<FakeScreeningRoleController>()
    val contacts = koinInject<FakeContactsAccess>()
    val roleHeld by roleController.isRoleHeld.collectAsState()
    val contactsGranted by contacts.isGranted.collectAsState()

    SwitchRow(roleHeld, roleController::setRoleHeld, "Call screening role held")
    SwitchRow(
        contactsGranted,
        contacts::setGranted,
        "Contacts permission granted",
        "Fake contacts: (11) 91234-5678 and (35) 8123-4567, saved without the 9",
    )
}

@Composable
private fun CallSimulator() {
    val screener = koinInject<CallScreener>()
    val recorder = koinInject<CallHistoryRecorder>()
    val callLog = koinInject<FakeCallLogAccess>()
    val callLogSync = koinInject<CallLogSync>()
    val normalizer = koinInject<PhoneNumberNormalizer>()
    val scope = rememberCoroutineScope()
    var answer by remember { mutableStateOf(true) }
    var number by remember { mutableStateOf("0303 123 4567") }
    val calls = remember { mutableStateListOf<SimulatedCall>() }

    fun simulate(raw: String?) {
        val call = IncomingCall(normalizer.normalize(raw), Clock.System.now())
        // Like the Android CallScreeningService: in-memory rules + contacts + engine decide, then the call is
        // stored in the history.
        scope.launch {
            val decision = screener.screen(call)
            calls.add(0, SimulatedCall(raw ?: "(hidden)", call, decision))
            recorder.record(call, decision)
            // The fake "system call log": calls that rang were answered or missed, as chosen in the panel.
            if (decision.action != Action.REJECT) {
                callLog.log(CallLogEntry(raw, call.timestamp, if (answer) CallLogType.ANSWERED else CallLogType.MISSED))
                callLogSync.sync()
            }
        }
    }

    // A call Knobly never screened (before it was installed, or without the role), only in the system call log.
    fun logOnly(type: CallLogType) {
        scope.launch {
            callLog.log(CallLogEntry(number, Clock.System.now() - 2.days, type))
            callLogSync.sync()
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            label = { Text("Caller number") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { simulate(number) }) { Text("Call") }
        OutlinedButton(onClick = { simulate(null) }) { Text("Hidden") }
    }
    SwitchRow(
        answer,
        { answer = it },
        "Calls that ring are answered",
        "Off: they are missed (outcome written to the fake call log)",
    )
    Text(
        "Only in the system call log, 2 days ago (Knobly did not screen it; brought in by the sync):",
        style = MaterialTheme.typography.bodySmall,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { logOnly(CallLogType.ANSWERED) }) { Text("Answered") }
        OutlinedButton(onClick = { logOnly(CallLogType.MISSED) }) { Text("Missed") }
        OutlinedButton(onClick = { logOnly(CallLogType.BLOCKED) }) { Text("Blocked by system") }
    }
    calls.forEach { SimulatedCallRow(it) }
}

@Composable
private fun SimulatedCallRow(item: SimulatedCall) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "${item.decision.action}  <-  ${item.input}",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
        )
        val number = item.call.number
        Text(
            "rule=${item.decision.matchedRule?.value ?: "none"}  e164=${number?.e164}  national=${number?.national}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
    }
}
