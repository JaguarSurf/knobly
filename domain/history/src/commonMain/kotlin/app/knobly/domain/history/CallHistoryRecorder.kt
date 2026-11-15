package app.knobly.domain.history

import app.knobly.core.model.Action
import app.knobly.core.model.Decision
import app.knobly.core.model.IncomingCall
import app.knobly.domain.rules.call.CallFilterRuleIds
import app.knobly.domain.rules.call.CallFilterSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Stores every screened incoming call in the history, after the call screening has answered (never before: the
 * answer must be fast). Rejected calls keep a snapshot of the rule; calls that rang start as [CallOutcome.RANG] until
 * [CallLogSync] finds them in the call log. Also applies the retention: calls older than [retention] are
 * removed whenever a new one is stored.
 */
class CallHistoryRecorder(
    private val history: CallHistoryRepository,
    private val settings: Flow<CallFilterSettings>,
    private val clock: Clock,
    private val retention: Duration = RETENTION,
) {
    suspend fun record(
        call: IncomingCall,
        decision: Decision,
    ) {
        history.add(if (decision.action == Action.REJECT) rejected(call, decision) else rang(call))
        history.deleteOlderThan(clock.now() - retention)
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

    companion object {
        /** How long calls are kept: 3 years, covering the yearly statistics (docs/specs/call-history.md). */
        val RETENTION: Duration = (3 * 365 + 1).days
    }
}
