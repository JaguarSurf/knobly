package app.knobly.domain.rules

import app.knobly.core.model.Action
import app.knobly.core.model.Decision
import app.knobly.core.model.IncomingCall
import app.knobly.core.model.PhoneNumber
import app.knobly.core.model.RuleId
import app.knobly.domain.rules.call.HiddenNumberRule
import app.knobly.domain.rules.call.PrefixRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class RuleEngineTest {
    private val now = Instant.fromEpochSeconds(1_800_000_000)

    private fun call(national: String?) =
        IncomingCall(
            number = national?.let { PhoneNumber(raw = it, e164 = null, national = it) },
            timestamp = now,
        )

    private fun prefix(
        id: String,
        priority: Int,
        action: Action,
        vararg prefixes: String,
        enabled: Boolean = true,
    ) = PrefixRule(RuleId(id), priority, action, prefixes.toList(), enabled)

    @Test
    fun `no rules allows the call`() {
        val engine = RuleEngine<IncomingCall> { emptyList() }
        assertEquals(Decision.Default, engine.evaluate(call("11912345678")))
    }

    @Test
    fun `no matching rule allows the call`() {
        val engine = RuleEngine { listOf(prefix("tm", 1, Action.REJECT, "0303")) }
        assertEquals(Decision.Default, engine.evaluate(call("11912345678")))
    }

    @Test
    fun `the matching rule decides`() {
        val engine = RuleEngine { listOf(prefix("tm", 1, Action.REJECT, "0303")) }
        assertEquals(Decision(Action.REJECT, RuleId("tm")), engine.evaluate(call("03031234567")))
    }

    @Test
    fun `rules are evaluated by priority and not by list order`() {
        val engine =
            RuleEngine {
                listOf(
                    prefix("late", priority = 20, action = Action.REJECT, "03"),
                    prefix("early", priority = 10, action = Action.SILENCE, "0303"),
                )
            }
        assertEquals(Decision(Action.SILENCE, RuleId("early")), engine.evaluate(call("03031234567")))
    }

    @Test
    fun `disabled rules are skipped`() {
        val engine =
            RuleEngine {
                listOf(
                    prefix("off", 1, Action.REJECT, "0303", enabled = false),
                    prefix("on", 2, Action.SILENCE, "0303"),
                )
            }
        assertEquals(Decision(Action.SILENCE, RuleId("on")), engine.evaluate(call("03031234567")))
    }

    @Test
    fun `rules are read on every evaluation`() {
        var rules = emptyList<Rule<IncomingCall>>()
        val engine = RuleEngine { rules }
        assertEquals(Action.ALLOW, engine.evaluate(call("03031234567")).action)

        rules = listOf(prefix("tm", 1, Action.REJECT, "0303"))
        assertEquals(Action.REJECT, engine.evaluate(call("03031234567")).action)
    }

    @Test
    fun `hidden number rule matches only calls without a number`() {
        val engine = RuleEngine { listOf(HiddenNumberRule(RuleId("hidden"), 1, Action.SILENCE)) }
        assertEquals(Action.SILENCE, engine.evaluate(call(null)).action)
        assertEquals(Action.ALLOW, engine.evaluate(call("11912345678")).action)
    }

    @Test
    fun `prefix rule ignores hidden numbers`() {
        val engine = RuleEngine { listOf(prefix("tm", 1, Action.REJECT, "0303")) }
        assertEquals(Decision.Default, engine.evaluate(call(null)))
    }

    @Test
    fun `prefix with a plus is a country code and only matches the E164 form`() {
        val saoPaulo = PhoneNumber(raw = "+5511912345678", e164 = "+5511912345678", national = "11912345678")
        val usa = PhoneNumber(raw = "+16502530000", e164 = "+16502530000", national = "6502530000")
        val engine = RuleEngine { listOf(prefix("us", 1, Action.REJECT, "+1")) }
        assertEquals(Action.REJECT, engine.evaluate(IncomingCall(usa, now)).action)
        // "+1" must not match DDD 11
        assertEquals(Action.ALLOW, engine.evaluate(IncomingCall(saoPaulo, now)).action)
    }

    @Test
    fun `prefix with trunk 0 matches normalized numbers without it`() {
        // What LibPhoneNumberNormalizer returns for "0303 123 4567" and "011 91234-5678"
        val telemarketing = PhoneNumber(raw = "03031234567", e164 = "+553031234567", national = "3031234567")
        val saoPaulo = PhoneNumber(raw = "011912345678", e164 = "+5511912345678", national = "11912345678")
        val engine =
            RuleEngine {
                listOf(
                    prefix("tm", 1, Action.REJECT, "0303"),
                    prefix("sp", 2, Action.SILENCE, "011"),
                )
            }
        assertEquals(Action.REJECT, engine.evaluate(IncomingCall(telemarketing, now)).action)
        assertEquals(Action.SILENCE, engine.evaluate(IncomingCall(saoPaulo, now)).action)
    }
}
