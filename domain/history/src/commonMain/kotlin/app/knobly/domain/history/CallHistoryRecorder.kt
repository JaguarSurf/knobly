package app.knobly.domain.history

import app.knobly.core.model.Action
import app.knobly.core.model.Decision
import app.knobly.core.model.IncomingCall
import app.knobly.domain.rules.call.CallFilterRuleIds
import app.knobly.domain.rules.call.CallFilterSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Stores every screened incoming call in the history, after the call screening has answered (never before: the
 * answer must be fast). Rejected calls keep a snapshot of the rule; calls that rang start as [CallOutcome.RANG] until
 * [CallLogSync] finds them in the call log. Calls are kept until the user deletes them (no retention limit).
 */
class CallHistoryRecorder(
    private val history: CallHistoryRepository,
    private val settings: Flow<CallFilterSettings>,
) {
    suspend fun record(
        call: IncomingCall,
        decision: Decision,
    ) {
        history.add(if (decision.action == Action.REJECT) rejected(call, decision) else rang(call))
    }

    private fun rang(call: IncomingCall) =
        CallRecord(id = 0, timestamp = call.timestamp, number = call.number, outcome = CallOutcome.RANG)

    private suspend fun rejected(
        call: IncomingCall,
        decision: Decision,
    ): CallRecord {
        val matched = decision.matchedRule
        val rule =
            settings
                .first()
                .customRules
                .firstOrNull { CallFilterRuleIds.custom(it) == matched }
        val reason =
            when {
                matched == CallFilterRuleIds.CONTACTS_ONLY -> RejectionReason.CONTACTS_ONLY
                rule != null -> RejectionReason.CUSTOM_RULE
                else -> RejectionReason.UNKNOWN
            }
        return CallRecord(
            id = 0,
            timestamp = call.timestamp,
            number = call.number,
            outcome = CallOutcome.REJECTED,
            reason = reason,
            rule = rule?.let { RuleSnapshot(it.id, it.type, it.value, it.label) },
        )
    }
}
