package app.knobly.domain.history

import androidx.room.Room
import app.knobly.core.database.KnoblyDatabase
import app.knobly.core.database.buildKnoblyDatabase
import app.knobly.core.model.Action
import app.knobly.core.model.Decision
import app.knobly.core.model.IncomingCall
import app.knobly.core.model.RuleId
import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.domain.rules.call.CallFilterRuleIds
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.rules.call.CustomRule
import app.knobly.domain.rules.call.CustomRuleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The recorder on top of the real Room database (in memory, bundled SQLite). */
class CallHistoryTest {
    private val database = Room.inMemoryDatabaseBuilder<KnoblyDatabase>().buildKnoblyDatabase()
    private val history = RoomCallHistoryRepository(database.callEvents())
    private val normalizer = LibPhoneNumberNormalizer()
    private val now = Instant.parse("2026-11-14T15:00:00Z")
    private val clock =
        object : Clock {
            override fun now() = now
        }
    private val telemarketing = CustomRule("tm", CustomRuleType.PREFIX, "0303", "Telemarketing")
    private val settings =
        CallFilterSettings(contactsOnly = true, customRulesEnabled = true, customRules = listOf(telemarketing))
    private val recorder = CallHistoryRecorder(history, flowOf(settings), clock)

    @AfterTest
    fun tearDown() = database.close()

    private fun call(
        raw: String?,
        at: Instant = now,
    ) = IncomingCall(normalizer.normalize(raw), at)

    private fun rejectedBy(rule: RuleId) = Decision(Action.REJECT, rule)

    @Test
    fun `a call rejected by a custom rule keeps a snapshot of the rule`() =
        runTest {
            recorder.record(call("0303 123 4567"), rejectedBy(CallFilterRuleIds.custom(telemarketing)))

            val stored = history.calls.first().single()
            assertEquals(RejectionReason.CUSTOM_RULE, stored.reason)
            assertEquals(RuleSnapshot("tm", CustomRuleType.PREFIX, "0303", "Telemarketing"), stored.rule)
            assertEquals("+553031234567", stored.number?.e164)
            assertEquals(now, stored.timestamp)
        }

    @Test
    fun `contacts only and hidden numbers`() =
        runTest {
            recorder.record(call(null), rejectedBy(CallFilterRuleIds.CONTACTS_ONLY))

            val stored = history.calls.first().single()
            assertEquals(RejectionReason.CONTACTS_ONLY, stored.reason)
            assertNull(stored.number)
            assertNull(stored.rule)
        }

    @Test
    fun `allowed calls are not recorded`() =
        runTest {
            recorder.record(call("11912345678"), Decision.Default)
            recorder.record(call("11912345678"), Decision(Action.ALLOW, CallFilterRuleIds.CONTACTS_ALLOW))

            assertTrue(history.calls.first().isEmpty())
        }

    @Test
    fun `a rule deleted before recording is shown as unknown`() =
        runTest {
            recorder.record(call("0304 765 4321"), rejectedBy(RuleId("custom:gone")))

            assertEquals(
                RejectionReason.UNKNOWN,
                history.calls
                    .first()
                    .single()
                    .reason,
            )
        }

    @Test
    fun `newest first, delete one, clear all`() =
        runTest {
            recorder.record(call("0303 111 1111", now - 2.minutes), rejectedBy(CallFilterRuleIds.CONTACTS_ONLY))
            recorder.record(call("0303 222 2222", now - 1.minutes), rejectedBy(CallFilterRuleIds.CONTACTS_ONLY))

            val calls = history.calls.first()
            assertEquals(listOf("03032222222", "03031111111"), calls.map { it.number?.raw })

            history.delete(calls.first().id)
            assertEquals(listOf("03031111111"), history.calls.first().map { it.number?.raw })

            history.clear()
            assertTrue(history.calls.first().isEmpty())
        }

    @Test
    fun `calls older than three years are removed when a new one is recorded`() =
        runTest {
            recorder.record(call("0303 111 1111", now - 1097.days), rejectedBy(CallFilterRuleIds.CONTACTS_ONLY))
            recorder.record(call("0303 222 2222", now - 1095.days), rejectedBy(CallFilterRuleIds.CONTACTS_ONLY))
            recorder.record(call("0303 333 3333"), rejectedBy(CallFilterRuleIds.CONTACTS_ONLY))

            assertEquals(listOf("03033333333", "03032222222"), history.calls.first().map { it.number?.raw })
        }

    @Test
    fun `calls since an instant`() =
        runTest {
            recorder.record(call("0303 111 1111", now - 2.days), rejectedBy(CallFilterRuleIds.CONTACTS_ONLY))
            recorder.record(call("0303 222 2222", now - 1.days), rejectedBy(CallFilterRuleIds.CONTACTS_ONLY))

            assertEquals(listOf("03032222222"), history.callsSince(now - 1.days).first().map { it.number?.raw })
        }
}
