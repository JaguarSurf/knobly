package app.knobly.domain.history

import app.knobly.core.database.CallEventDao
import app.knobly.core.database.CallEventEntity
import app.knobly.core.model.Action
import app.knobly.core.model.PhoneNumber
import app.knobly.domain.rules.call.CustomRuleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/** [CallHistoryRepository] on the `call_events` table of the local database. */
class RoomCallHistoryRepository(
    private val dao: CallEventDao,
) : CallHistoryRepository {
    override val calls: Flow<List<CallRecord>> = dao.observeAll().map { events -> events.map { it.toCallRecord() } }

    override val latestRejected: Flow<CallRecord?> = dao.observeLatestRejected().map { it?.toCallRecord() }

    override fun callsSince(instant: Instant): Flow<List<CallRecord>> =
        dao.observeSince(instant.toEpochMilliseconds()).map { events -> events.map { it.toCallRecord() } }

    override suspend fun add(call: CallRecord) {
        dao.insert(call.toEntity())
    }

    override suspend fun addAll(calls: List<CallRecord>) {
        if (calls.isNotEmpty()) dao.insertAll(calls.map { it.toEntity() })
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clear() = dao.deleteAll()

    override suspend fun deleteOlderThan(instant: Instant) {
        dao.deleteOlderThan(instant.toEpochMilliseconds())
    }

    override suspend fun unlinked(instant: Instant): List<CallRecord> =
        dao.unlinked(instant.toEpochMilliseconds()).map { it.toCallRecord() }

    override suspend fun knownCallLogKeys(instant: Instant): Set<String> =
        dao.knownCallLogKeys(instant.toEpochMilliseconds()).toSet()

    override suspend fun link(
        id: Long,
        callLogKey: String,
        outcome: CallOutcome?,
    ) {
        dao.link(id, callLogKey, outcome?.stored())
    }
}

private const val REASON_CONTACTS_ONLY = "contacts_only"
private const val REASON_CUSTOM_RULE = "custom_rule"
private const val REASON_UNKNOWN = "unknown"
private const val REASON_SYSTEM_BLOCKED = "system_blocked"
private const val REASON_NONE = "none"
private const val OUTCOME_ANSWERED = "answered"
private const val OUTCOME_MISSED = "missed"

internal fun CallRecord.toEntity() =
    CallEventEntity(
        timestampUtc = timestamp.toEpochMilliseconds(),
        numberRaw = number?.raw,
        numberE164 = number?.e164,
        numberNational = number?.national,
        action = if (outcome == CallOutcome.REJECTED) Action.REJECT.name else Action.ALLOW.name,
        reason =
            when (reason) {
                RejectionReason.CONTACTS_ONLY -> REASON_CONTACTS_ONLY
                RejectionReason.CUSTOM_RULE -> REASON_CUSTOM_RULE
                RejectionReason.UNKNOWN -> REASON_UNKNOWN
                RejectionReason.BLOCKED_BY_SYSTEM -> REASON_SYSTEM_BLOCKED
                null -> if (outcome == CallOutcome.REJECTED) REASON_UNKNOWN else REASON_NONE
            },
        matchedRuleId = rule?.id,
        ruleType = rule?.type?.name,
        ruleValue = rule?.value,
        ruleLabel = rule?.label,
        outcome = outcome.stored(),
        source = if (source == CallSource.CALL_LOG) CallEventEntity.SOURCE_CALL_LOG else CallEventEntity.SOURCE_KNOBLY,
        callLogKey = callLogKey,
    )

/** Only the outcomes of calls that rang are stored; rejected calls are told apart by `action`. */
private fun CallOutcome.stored(): String? =
    when (this) {
        CallOutcome.ANSWERED -> OUTCOME_ANSWERED
        CallOutcome.MISSED -> OUTCOME_MISSED
        CallOutcome.REJECTED, CallOutcome.RANG -> null
    }

internal fun CallEventEntity.toCallRecord(): CallRecord {
    val number = numberRaw?.let { PhoneNumber(raw = it, e164 = numberE164, national = numberNational ?: it) }
    val at = Instant.fromEpochMilliseconds(timestampUtc)
    val source = if (source == CallEventEntity.SOURCE_CALL_LOG) CallSource.CALL_LOG else CallSource.KNOBLY
    if (action != Action.REJECT.name) {
        val outcome =
            when (outcome) {
                OUTCOME_ANSWERED -> CallOutcome.ANSWERED
                OUTCOME_MISSED -> CallOutcome.MISSED
                else -> CallOutcome.RANG
            }
        return CallRecord(
            id = id,
            timestamp = at,
            number = number,
            outcome = outcome,
            source = source,
            callLogKey = callLogKey,
        )
    }
    val ruleId = matchedRuleId
    val type = ruleType?.let { name -> CustomRuleType.entries.firstOrNull { it.name == name } }
    val snapshot =
        if (ruleId != null && type != null) {
            RuleSnapshot(id = ruleId, type = type, value = ruleValue.orEmpty(), label = ruleLabel.orEmpty())
        } else {
            null
        }
    return CallRecord(
        id = id,
        timestamp = at,
        number = number,
        outcome = CallOutcome.REJECTED,
        reason = rejectionReason(reason, hasRule = snapshot != null),
        rule = snapshot,
        source = source,
        callLogKey = callLogKey,
    )
}

private fun rejectionReason(
    stored: String,
    hasRule: Boolean,
): RejectionReason =
    when (stored) {
        REASON_CONTACTS_ONLY -> RejectionReason.CONTACTS_ONLY
        REASON_CUSTOM_RULE -> if (hasRule) RejectionReason.CUSTOM_RULE else RejectionReason.UNKNOWN
        REASON_SYSTEM_BLOCKED -> RejectionReason.BLOCKED_BY_SYSTEM
        else -> RejectionReason.UNKNOWN
    }
