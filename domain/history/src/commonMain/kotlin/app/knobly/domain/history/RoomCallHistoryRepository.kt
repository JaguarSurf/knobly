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
    override val calls: Flow<List<RejectedCall>> = dao.observeAll().map { events -> events.map { it.toRejectedCall() } }

    override val latest: Flow<RejectedCall?> = dao.observeLatest().map { it?.toRejectedCall() }

    override fun callsSince(instant: Instant): Flow<List<RejectedCall>> =
        dao.observeSince(instant.toEpochMilliseconds()).map { events -> events.map { it.toRejectedCall() } }

    override suspend fun add(call: RejectedCall) {
        dao.insert(call.toEntity())
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clear() = dao.deleteAll()

    override suspend fun deleteOlderThan(instant: Instant) {
        dao.deleteOlderThan(instant.toEpochMilliseconds())
    }
}

private const val REASON_CONTACTS_ONLY = "contacts_only"
private const val REASON_CUSTOM_RULE = "custom_rule"
private const val REASON_UNKNOWN = "unknown"

internal fun RejectedCall.toEntity() =
    CallEventEntity(
        timestampUtc = timestamp.toEpochMilliseconds(),
        numberRaw = number?.raw,
        numberE164 = number?.e164,
        numberNational = number?.national,
        action = Action.REJECT.name,
        reason =
            when (reason) {
                RejectionReason.CONTACTS_ONLY -> REASON_CONTACTS_ONLY
                RejectionReason.CUSTOM_RULE -> REASON_CUSTOM_RULE
                RejectionReason.UNKNOWN -> REASON_UNKNOWN
            },
        matchedRuleId = rule?.id,
        ruleType = rule?.type?.name,
        ruleValue = rule?.value,
        ruleLabel = rule?.label,
    )

internal fun CallEventEntity.toRejectedCall(): RejectedCall {
    val ruleId = matchedRuleId
    val type = ruleType?.let { name -> CustomRuleType.entries.firstOrNull { it.name == name } }
    val snapshot =
        if (ruleId != null && type != null) {
            RuleSnapshot(id = ruleId, type = type, value = ruleValue.orEmpty(), label = ruleLabel.orEmpty())
        } else {
            null
        }
    return RejectedCall(
        id = id,
        timestamp = Instant.fromEpochMilliseconds(timestampUtc),
        number = numberRaw?.let { PhoneNumber(raw = it, e164 = numberE164, national = numberNational ?: it) },
        reason =
            when (reason) {
                REASON_CONTACTS_ONLY -> RejectionReason.CONTACTS_ONLY
                REASON_CUSTOM_RULE -> if (snapshot != null) RejectionReason.CUSTOM_RULE else RejectionReason.UNKNOWN
                else -> RejectionReason.UNKNOWN
            },
        rule = snapshot,
    )
}
