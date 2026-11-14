package app.knobly.domain.rules.call

import app.knobly.core.model.Decision
import app.knobly.core.model.IncomingCall
import app.knobly.domain.rules.RuleContext
import app.knobly.domain.rules.RuleEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Decides what to do with an incoming call: the single entry point used by the Android `CallScreeningService` and by
 * the desktop harness's call simulator.
 *
 * Runs on [dispatcher] (the contacts index may have to be built) and gives up after [timeout], letting the call
 * through: a filter must never make the phone miss a call because it was slow.
 */
class CallScreener(
    private val rules: CallRulesProvider,
    private val contacts: ContactDirectory,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timeout: Duration = 3.seconds,
) {
    suspend fun screen(call: IncomingCall): Decision {
        val work =
            scope.async(dispatcher) {
                val current = rules.await()
                RuleEngine { current }.evaluate(call, RuleContext(contacts))
            }
        return withTimeoutOrNull(timeout) { work.await() } ?: run {
            work.cancel()
            Decision.Default
        }
    }
}

/**
 * Name of the dependency-injection binding for the dispatcher [CallScreener] and [ContactDirectory] run on. Platforms
 * whose contacts reads block (Android) bind it to their IO dispatcher; otherwise `Dispatchers.Default` is used.
 */
const val SCREENING_DISPATCHER = "screeningDispatcher"
