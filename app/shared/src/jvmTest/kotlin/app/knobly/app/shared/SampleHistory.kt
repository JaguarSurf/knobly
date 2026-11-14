package app.knobly.app.shared

import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.domain.history.RejectedCall
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
    ) = RejectedCall(
        id = id,
        timestamp = TEST_NOW - ago,
        number = normalizer.normalize(raw),
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
}
