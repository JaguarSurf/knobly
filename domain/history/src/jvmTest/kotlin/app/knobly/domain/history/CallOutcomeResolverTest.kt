package app.knobly.domain.history

import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.domain.port.CallLogAccess
import app.knobly.domain.port.CallLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class CallOutcomeResolverTest {
    private val normalizer = LibPhoneNumberNormalizer()
    private val now = Instant.parse("2026-11-14T15:00:00Z")
    private val clock =
        object : Clock {
            override fun now() = now
        }

    private class TestCallLog(
        var entries: List<CallLogEntry>,
        granted: Boolean = true,
    ) : CallLogAccess {
        override val isGranted: StateFlow<Boolean> = MutableStateFlow(granted)

        override suspend fun requestAccess() = isGranted.value

        override fun openAppSettings() = Unit

        override fun incomingCalls(since: Instant) = entries.filter { it.timestamp >= since }
    }

    private fun rang(
        id: Long,
        raw: String?,
        at: Instant,
    ) = CallRecord(id, at, normalizer.normalize(raw), CallOutcome.RANG)

    private suspend fun TestScope.outcomes(
        history: InMemoryCallHistoryRepository,
        callLog: TestCallLog,
    ): List<CallOutcome> {
        CallOutcomeResolver(history, callLog, normalizer, clock, StandardTestDispatcher(testScheduler)).resolvePending()
        return history.calls
            .first()
            .sortedBy { it.id }
            .map { it.outcome }
    }

    @Test
    fun `calls that rang get their outcome from the call log`() =
        runTest {
            val history =
                InMemoryCallHistoryRepository(
                    listOf(
                        rang(1, "11912345678", now - 10.seconds),
                        rang(2, "035 98123-4567", now - 5.seconds),
                        rang(3, null, now - 3.seconds),
                        rang(4, "21999998888", now - 1.seconds),
                    ),
                )
            val callLog =
                TestCallLog(
                    listOf(
                        // The system logs the number in its own format, a moment after Knobly screened it.
                        CallLogEntry("+5511912345678", now - 9.seconds, answered = true),
                        CallLogEntry("35981234567", now - 4.seconds, answered = false),
                        CallLogEntry("", now - 2.seconds, answered = false),
                        // Call 4 is not in the call log yet (still ringing).
                    ),
                )

            assertEquals(
                listOf(CallOutcome.ANSWERED, CallOutcome.MISSED, CallOutcome.MISSED, CallOutcome.RANG),
                outcomes(history, callLog),
            )
        }

    @Test
    fun `a call log entry from another number or much later does not match`() =
        runTest {
            val history = InMemoryCallHistoryRepository(listOf(rang(1, "11912345678", now - 1.days)))
            val callLog =
                TestCallLog(
                    listOf(
                        CallLogEntry("11900000000", now - 1.days, answered = true),
                        CallLogEntry("11912345678", now - 1.days + 10.minutes, answered = true),
                    ),
                )

            assertEquals(listOf(CallOutcome.RANG), outcomes(history, callLog))
        }

    @Test
    fun `without the permission nothing changes`() =
        runTest {
            val history = InMemoryCallHistoryRepository(listOf(rang(1, "11912345678", now)))
            val callLog = TestCallLog(listOf(CallLogEntry("11912345678", now, answered = true)), granted = false)

            assertEquals(listOf(CallOutcome.RANG), outcomes(history, callLog))
        }
}
