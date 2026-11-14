package app.knobly.core.model

import kotlin.jvm.JvmInline

/** What to do with an event. */
enum class Action { ALLOW, SILENCE, REJECT }

@JvmInline
value class RuleId(
    val value: String,
)

/**
 * The outcome of evaluating an event.
 *
 * @property matchedRule the rule that decided, or `null` when no rule matched (default decision).
 */
data class Decision(
    val action: Action,
    val matchedRule: RuleId?,
) {
    companion object {
        /** No rule matched: let the event through. */
        val Default = Decision(Action.ALLOW, matchedRule = null)
    }
}
