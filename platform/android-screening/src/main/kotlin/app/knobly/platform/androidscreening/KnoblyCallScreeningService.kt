package app.knobly.platform.androidscreening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import app.knobly.core.model.Action
import app.knobly.core.model.Decision
import app.knobly.core.model.IncomingCall
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.rules.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.TimeSource

/**
 * Called by the system for every incoming call. It must answer within a few seconds, so it only runs the
 * in-memory rule engine before [respondToCall]; anything that does I/O happens afterwards, asynchronously.
 */
class KnoblyCallScreeningService :
    CallScreeningService(),
    KoinComponent {
    private val engine: RuleEngine<IncomingCall> by inject()
    private val normalizer: PhoneNumberNormalizer by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val started = TimeSource.Monotonic.markNow()
        val hidden = callDetails.handlePresentation != TelecomManager.PRESENTATION_ALLOWED
        val call =
            IncomingCall(
                number = if (hidden) null else normalizer.normalize(callDetails.handle?.schemeSpecificPart),
                timestamp = Clock.System.now(),
            )
        val decision = engine.evaluate(call)
        respondToCall(callDetails, decision.toCallResponse())
        val elapsed = started.elapsedNow()

        // After responding. Spike: log only (no phone numbers); the event will be stored in :core:database.
        scope.launch {
            Log.i(
                TAG,
                "action=${decision.action} rule=${decision.matchedRule?.value} hidden=${call.number == null} " +
                    "took=${elapsed.inWholeMicroseconds}us",
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun Decision.toCallResponse(): CallResponse {
        val builder = CallResponse.Builder()
        when (action) {
            Action.ALLOW -> {
                // The default response lets the call through.
            }

            Action.SILENCE -> {
                builder.setSilenceCall(true)
            }

            Action.REJECT -> {
                builder
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
            }
        }
        return builder.build()
    }

    private companion object {
        const val TAG = "KnoblyScreening"
    }
}
