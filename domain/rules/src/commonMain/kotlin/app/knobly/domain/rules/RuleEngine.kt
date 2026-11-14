package app.knobly.domain.rules

import app.knobly.core.model.Decision
import app.knobly.core.model.Event

/**
 * Evaluates an event against the enabled rules in priority order (Chain of Responsibility):
 * the first rule that matches decides. Lower [Rule.priority] values are evaluated first.
 *
 * @param rules supplies the current rules; it is called on every evaluation and must return an in-memory list.
 */
class RuleEngine<E : Event>(
    private val rules: () -> List<Rule<E>>,
) {
    fun evaluate(
        event: E,
        context: RuleContext = RuleContext(),
    ): Decision =
        rules()
            .asSequence()
            .filter { it.enabled }
            .sortedBy { it.priority }
            .firstOrNull { it.matches(event, context) }
            ?.let { Decision(it.action, it.id) }
            ?: Decision.Default
}
