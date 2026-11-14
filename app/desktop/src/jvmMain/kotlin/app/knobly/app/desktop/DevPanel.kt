package app.knobly.app.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.core.model.Decision
import app.knobly.core.model.IncomingCall
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.history.CallHistoryRecorder
import app.knobly.domain.rules.call.CallScreener
import app.knobly.platform.fake.FakeContactsAccess
import app.knobly.platform.fake.FakeScreeningRoleController
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Clock

private data class SimulatedCall(
    val input: String,
    val call: IncomingCall,
    val decision: Decision,
)

/**
 * Developer tools next to the app: toggles the fake call screening role and simulates incoming calls through
 * the real normalizer and rule engine (the same code the Android CallScreeningService runs).
 */
@Composable
fun DevPanel(modifier: Modifier = Modifier) {
    val roleController = koinInject<FakeScreeningRoleController>()
    val contacts = koinInject<FakeContactsAccess>()
    val screener = koinInject<CallScreener>()
    val recorder = koinInject<CallHistoryRecorder>()
    val normalizer = koinInject<PhoneNumberNormalizer>()
    val scope = rememberCoroutineScope()
    val roleHeld by roleController.isRoleHeld.collectAsState()
    val contactsGranted by contacts.isGranted.collectAsState()
    var number by remember { mutableStateOf("0303 123 4567") }
    val calls = remember { mutableStateListOf<SimulatedCall>() }

    fun simulate(raw: String?) {
        val call = IncomingCall(normalizer.normalize(raw), Clock.System.now())
        // Same path as the Android CallScreeningService: in-memory rules + contacts + engine.
        // Like the Android CallScreeningService: decide, then store rejected calls in the history.
        scope.launch {
            val decision = screener.screen(call)
            calls.add(0, SimulatedCall(raw ?: "(hidden)", call, decision))
            recorder.record(call, decision)
        }
    }

    KnoblyTheme {
        Surface(modifier) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Dev tools", style = MaterialTheme.typography.headlineSmall)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Switch(checked = roleHeld, onCheckedChange = roleController::setRoleHeld)
                    Text("Call screening role held (fake)")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Switch(checked = contactsGranted, onCheckedChange = contacts::setGranted)
                    Text("Contacts permission granted (fake contact: (11) 91234-5678)")
                }

                HorizontalDivider()
                Text("Incoming call simulator", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(calls) { SimulatedCallRow(it) }
                }
            }
        }
    }
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
