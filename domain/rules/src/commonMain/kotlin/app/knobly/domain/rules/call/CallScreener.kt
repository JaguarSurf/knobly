package app.knobly.domain.rules.call

import app.knobly.core.model.Decision
import app.knobly.core.model.IncomingCall
import app.knobly.core.model.PhoneNumber
import app.knobly.domain.port.ContactsAccess
import app.knobly.domain.rules.ContactLookup
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
 * Runs on [dispatcher] (contacts lookups may block) and gives up after [timeout], letting the call through: a
 * filter must never make the phone miss a call because it was slow.
 */
class CallScreener(
    private val rules: CallRulesProvider,
    private val contacts: ContactsAccess,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timeout: Duration = 3.seconds,
) {
    suspend fun screen(call: IncomingCall): Decision {
        val work =
            scope.async(dispatcher) {
                val current = rules.await()
                RuleEngine { current }.evaluate(call, RuleContext(MemoizedContactLookup(contacts)))
            }
        return withTimeoutOrNull(timeout) { work.await() } ?: run {
            work.cancel()
            Decision.Default
        }
    }
}

/** Looks each number up at most once per call (both contact rules may ask). */
private class MemoizedContactLookup(
    private val contacts: ContactsAccess,
) : ContactLookup {
    private val results = mutableMapOf<PhoneNumber, Boolean>()
    override val isAvailable: Boolean = contacts.isGranted.value

    override fun isContact(number: PhoneNumber): Boolean = results.getOrPut(number) { contacts.isContact(number) }
}

/**
 * Name of the dependency-injection binding for the dispatcher [CallScreener] runs on. Platforms whose contacts
 * lookups block (Android) bind it to their IO dispatcher; otherwise `Dispatchers.Default` is used.
 */
const val SCREENING_DISPATCHER = "screeningDispatcher"
