package app.knobly.app.shared

import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.domain.history.CallOutcome
import app.knobly.domain.history.CallRecord
import app.knobly.domain.history.CallSource
import app.knobly.domain.history.RejectionReason
import app.knobly.domain.history.RuleSnapshot
import app.knobly.domain.rules.call.CustomRuleType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Rejected calls relative to [TEST_NOW] (14 Nov 2026, 14:00 in São Paulo), like the History mockup. */
internal object SampleHistory {
    private val normalizer = LibPhoneNumberNormalizer()
    val telemarketing = RuleSnapshot("tm", CustomRuleType.PREFIX, "0303", "Telemarketing")
    private val donations = RuleSnapshot("don", CustomRuleType.PREFIX, "0304", "Doações")
    private val collector = RuleSnapshot("debt", CustomRuleType.EXACT_NUMBER, "(11) 4002-8922", "Cobrança insistente")

    private fun call(
        id: Long,
        ago: Duration,
        raw: String?,
        rule: RuleSnapshot? = null,
    ) = CallRecord(
        id = id,
        timestamp = TEST_NOW - ago,
        number = normalizer.normalize(raw),
        outcome = CallOutcome.REJECTED,
        reason = if (rule != null) RejectionReason.CUSTOM_RULE else RejectionReason.CONTACTS_ONLY,
        rule = rule,
    )

    val calls =
        listOf(
            call(1, 28.minutes, "03031234567", telemarketing),
            call(2, 2.hours + 55.minutes, "21998765432"),
            call(3, 4.hours + 13.minutes, null),
            call(4, 1.days - 5.hours - 10.minutes, "1140028922", collector),
            call(5, 1.days - 1.hours - 22.minutes, "03047654321", donations),
            call(6, 1.days + 3.hours + 52.minutes, "03035550101", telemarketing),
            call(7, 5.days, "11987654321"),
        )

    private fun allowed(
        id: Long,
        ago: Duration,
        raw: String,
        outcome: CallOutcome,
    ) = CallRecord(id = id, timestamp = TEST_NOW - ago, number = normalizer.normalize(raw), outcome = outcome)

    /** [calls] plus calls that rang: answered (one from the fake contact Maria), missed and not yet known. */
    val mixed: List<CallRecord> =
        calls +
            listOf(
                allowed(20, 12.minutes, "11912345678", CallOutcome.ANSWERED),
                allowed(21, 1.hours + 40.minutes, "21987650000", CallOutcome.MISSED),
                allowed(22, 3.hours + 5.minutes, "31991112222", CallOutcome.RANG),
                allowed(23, 1.days - 2.hours, "11912345678", CallOutcome.ANSWERED),
            )

    /** Calls Knobly did not screen, brought in from the system call log: one blocked by the system, one answered. */
    val fromCallLog: List<CallRecord> =
        listOf(
            CallRecord(
                id = 30,
                timestamp = TEST_NOW - 6.days,
                number = normalizer.normalize("08001234567"),
                outcome = CallOutcome.REJECTED,
                reason = RejectionReason.BLOCKED_BY_SYSTEM,
                source = CallSource.CALL_LOG,
                callLogKey = "a",
            ),
            CallRecord(
                id = 31,
                timestamp = TEST_NOW - 6.days - 2.hours,
                number = normalizer.normalize("11912345678"),
                outcome = CallOutcome.ANSWERED,
                source = CallSource.CALL_LOG,
                callLogKey = "b",
            ),
        )

    /**
     * About a year and a half of incoming calls (deterministic, ~1-4 a day): rejected, answered and missed, for the
     * Statistics screenshots. Ends at [TEST_NOW].
     */
    val longHistory: List<CallRecord> =
        (0 until 540).flatMap { daysAgo ->
            val perDay = 1 + (daysAgo * 7 + daysAgo / 30) % 4
            (0 until perDay).map { index ->
                val rule = listOf(null, telemarketing, null, donations, collector, telemarketing)[(daysAgo + index) % 6]
                val id = daysAgo * 10L + index + 100
                val ago = daysAgo.days + (1 + (index * 5 + daysAgo) % 9).hours + (index * 13).minutes
                when ((daysAgo * 3 + index) % 5) {
                    0 -> {
                        allowed(id, ago, "11912345678", CallOutcome.ANSWERED)
                    }

                    1 -> {
                        allowed(id, ago, "21987650000", CallOutcome.ANSWERED)
                    }

                    2 -> {
                        allowed(id, ago, "31991112222", CallOutcome.MISSED)
                    }

                    else -> {
                        call(
                            id,
                            ago,
                            if (rule ==
                                null
                            ) {
                                "1198765${(4321 + daysAgo) % 10000}"
                            } else {
                                "03031234567"
                            },
                            rule,
                        )
                    }
                }
            }
        }
}
