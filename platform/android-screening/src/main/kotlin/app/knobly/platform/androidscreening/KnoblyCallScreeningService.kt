package app.knobly.platform.androidscreening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import app.knobly.core.model.Action
import app.knobly.core.model.Decision
import app.knobly.core.model.IncomingCall
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.rules.call.CallScreener
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
 * Called by the system for every incoming call; it must answer within a few seconds. The decision is made off the
 * main thread by [CallScreener] (in-memory rules + a contacts lookup), which lets the call through if anything is
 * slow. Nothing is written before answering.
 */
class KnoblyCallScreeningService :
    CallScreeningService(),
    KoinComponent {
    private val screener: CallScreener by inject()
    private val normalizer: PhoneNumberNormalizer by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val started = TimeSource.Monotonic.markNow()
        val hidden = callDetails.handlePresentation != TelecomManager.PRESENTATION_ALLOWED
        val rawNumber = if (hidden) null else callDetails.handle?.schemeSpecificPart
        scope.launch {
            val call = IncomingCall(number = normalizer.normalize(rawNumber), timestamp = Clock.System.now())
            val decision = screener.screen(call)
            respondToCall(callDetails, decision.toCallResponse())
            // Spike-level logging only (no phone numbers); filtered calls are stored in the history in a later step.
            Log.i(
                TAG,
                "action=${decision.action} rule=${decision.matchedRule?.value} hidden=${call.number == null} " +
                    "took=${started.elapsedNow().inWholeMilliseconds}ms",
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "KnoblyScreening"
    }
}

/**
 * Reject = the call never happened for the user: no ringing, no in-call screen, no notification and no entry in the
 * system call log (docs/specs/call-filters.md). It is kept in Knobly's own history instead.
 */
internal fun Decision.toCallResponse(): CallScreeningService.CallResponse {
    val builder = CallScreeningService.CallResponse.Builder()
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
                .setSkipCallLog(true)
                .setSkipNotification(true)
        }
    }
    return builder.build()
}
