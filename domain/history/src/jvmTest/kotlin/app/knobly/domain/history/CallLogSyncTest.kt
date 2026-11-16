package app.knobly.domain.history

import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.domain.port.CallLogAccess
import app.knobly.domain.port.CallLogEntry
import app.knobly.domain.port.CallLogType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class CallLogSyncTest {
    private val normalizer = LibPhoneNumberNormalizer()
    private val now = Instant.parse("2026-11-14T15:00:00Z")

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

    private fun rejected(
        id: Long,
        raw: String?,
        at: Instant,
    ) = CallRecord(id, at, normalizer.normalize(raw), CallOutcome.REJECTED, RejectionReason.CONTACTS_ONLY)

    private fun entry(
        raw: String?,
        at: Instant,
        type: CallLogType,
    ) = CallLogEntry(raw, at, type)

    private suspend fun TestScope.sync(
        history: InMemoryCallHistoryRepository,
        callLog: TestCallLog,
    ) = CallLogSync(history, callLog, normalizer, StandardTestDispatcher(testScheduler)).sync()

    /** Oldest first, for readable assertions. */
    private val InMemoryCallHistoryRepository.byTime get() = value.sortedWith(compareBy({ it.timestamp }, { it.id }))

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
                        entry("+5511912345678", now - 9.seconds, CallLogType.ANSWERED),
                        entry("35981234567", now - 4.seconds, CallLogType.MISSED),
                        entry("", now - 2.seconds, CallLogType.MISSED),
                        // Call 4 is not in the call log yet (still ringing).
                    ),
                )

            sync(history, callLog)

            assertEquals(
                listOf(CallOutcome.ANSWERED, CallOutcome.MISSED, CallOutcome.MISSED, CallOutcome.RANG),
                history.byTime.map { it.outcome },
            )
            assertTrue(history.value.all { it.source == CallSource.KNOBLY })
            assertNull(history.value.single { it.id == 4L }.callLogKey)
        }

    @Test
    fun `calls Knobly did not screen are brought in`() =
        runTest {
            val history = InMemoryCallHistoryRepository(listOf(rang(1, "11912345678", now - 1.days)))
            val callLog =
                TestCallLog(
                    listOf(
                        // Another number at the same time, and the same number much later: not call 1.
                        entry("11900000000", now - 1.days, CallLogType.ANSWERED),
                        entry("11912345678", now - 1.days + 10.minutes, CallLogType.MISSED),
                        // Blocked by the system or another app.
                        entry("03031234567", now - 1.days + 20.minutes, CallLogType.BLOCKED),
                        entry(null, now - 1.days + 30.minutes, CallLogType.MISSED),
                    ),
                )

            sync(history, callLog)

            val calls = history.byTime
            assertEquals(5, calls.size)
            assertEquals(CallOutcome.RANG, calls.first { it.id == 1L }.outcome)
            val imported = calls.filter { it.source == CallSource.CALL_LOG }
            assertEquals(
                listOf(CallOutcome.ANSWERED, CallOutcome.MISSED, CallOutcome.REJECTED, CallOutcome.MISSED),
                imported.map { it.outcome },
            )
            assertEquals("+5511900000000", imported[0].number?.e164)
            assertEquals(RejectionReason.BLOCKED_BY_SYSTEM, imported[2].reason)
            assertNull(imported[3].number)
            assertEquals(callLog.entries.map { it.key }, imported.map { it.callLogKey })
        }

    @Test
    fun `a blocked entry of a call Knobly rejected is not brought in twice`() =
        runTest {
            val history = InMemoryCallHistoryRepository(listOf(rejected(1, "03031234567", now - 1.minutes)))
            val callLog = TestCallLog(listOf(entry("03031234567", now - 1.minutes + 1.seconds, CallLogType.BLOCKED)))

            sync(history, callLog)

            val call = history.value.single()
            assertEquals(RejectionReason.CONTACTS_ONLY, call.reason)
            assertEquals(callLog.entries.single().key, call.callLogKey)
        }

    @Test
    fun `syncing again brings nothing new and keeps the outcomes`() =
        runTest {
            val history = InMemoryCallHistoryRepository(listOf(rang(1, "11912345678", now - 1.minutes)))
            val callLog =
                TestCallLog(
                    listOf(
                        entry("11912345678", now - 1.minutes, CallLogType.ANSWERED),
                        entry("11900000000", now - 2.days, CallLogType.MISSED),
                    ),
                )

            sync(history, callLog)
            sync(history, callLog)

            assertEquals(listOf(CallOutcome.MISSED, CallOutcome.ANSWERED), history.byTime.map { it.outcome })
        }

    @Test
    fun `calls removed by the user are not brought in again`() =
        runTest {
            val history = InMemoryCallHistoryRepository()
            val callLog =
                TestCallLog(
                    listOf(
                        entry("11900000000", now - 2.days, CallLogType.MISSED),
                        entry("11912345678", now - 1.days, CallLogType.ANSWERED),
                    ),
                )
            sync(history, callLog)

            history.delete(history.value.first().id)
            sync(history, callLog)
            assertEquals(listOf("+5511900000000"), history.value.map { it.number?.e164 })

            history.clear()
            sync(history, callLog)
            assertTrue(history.value.isEmpty())

            // A new call after the history was cleared is brought in.
            callLog.entries += entry("11977776666", now - 1.minutes, CallLogType.MISSED)
            sync(history, callLog)
            assertEquals(listOf("+5511977776666"), history.value.map { it.number?.e164 })
        }

    @Test
    fun `old call log entries are brought in too`() =
        runTest {
            val history = InMemoryCallHistoryRepository()
            val callLog =
                TestCallLog(
                    listOf(
                        entry("11900000000", now - 3650.days, CallLogType.MISSED),
                        entry("11912345678", now - 1.days, CallLogType.ANSWERED),
                    ),
                )

            sync(history, callLog)

            assertEquals(listOf("+5511912345678", "+5511900000000"), history.value.map { it.number?.e164 })
        }

    @Test
    fun `without the permission nothing changes`() =
        runTest {
            val history = InMemoryCallHistoryRepository(listOf(rang(1, "11912345678", now)))
            val callLog =
                TestCallLog(listOf(entry("11912345678", now, CallLogType.ANSWERED)), granted = false)

            sync(history, callLog)

            assertEquals(listOf(CallOutcome.RANG), history.value.map { it.outcome })
        }
}
