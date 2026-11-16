package app.knobly.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An incoming call: screened by Knobly (rejected, or allowed to ring), or brought in from the system call log. Rule
 * details are a snapshot taken when the call was screened, so the history stays readable after the rule is edited or
 * deleted.
 */
@Entity(tableName = "call_events", indices = [Index("timestamp_utc"), Index("call_log_key")])
data class CallEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch milliseconds (UTC). */
    @ColumnInfo(name = "timestamp_utc") val timestampUtc: Long,
    /** `null` when the number was hidden. */
    @ColumnInfo(name = "number_raw") val numberRaw: String?,
    @ColumnInfo(name = "number_e164") val numberE164: String?,
    @ColumnInfo(name = "number_national") val numberNational: String?,
    /** Knobly's decision: `REJECT` or `ALLOW` (later also `SILENCE`). */
    val action: String,
    /**
     * Why it was rejected: `contacts_only`, `custom_rule`, `unknown` or `system_blocked` (blocked by the system or
     * another app, from the call log); `none` for allowed calls.
     */
    val reason: String,
    @ColumnInfo(name = "matched_rule_id") val matchedRuleId: String?,
    @ColumnInfo(name = "rule_type") val ruleType: String?,
    @ColumnInfo(name = "rule_value") val ruleValue: String?,
    @ColumnInfo(name = "rule_label") val ruleLabel: String?,
    /** How an allowed call ended, from the system call log: `answered`, `missed`; `null` while unknown. Since v2. */
    @ColumnInfo(name = "outcome") val outcome: String? = null,
    /** Where the entry came from: `knobly` (screened) or `call_log` (brought in from the system call log). Since v3. */
    @ColumnInfo(name = "source", defaultValue = SOURCE_KNOBLY) val source: String = SOURCE_KNOBLY,
    /** The system call log entry of this call (`<epoch millis>:<number>`), once known. Since v3. */
    @ColumnInfo(name = "call_log_key") val callLogKey: String? = null,
) {
    companion object {
        const val SOURCE_KNOBLY = "knobly"
        const val SOURCE_CALL_LOG = "call_log"
    }
}

/**
 * A call log entry the user removed from the history (one entry, or the whole history): it is not brought in again.
 * Since v3.
 */
@Entity(tableName = "ignored_call_log_entries")
data class IgnoredCallLogEntryEntity(
    @PrimaryKey @ColumnInfo(name = "call_log_key") val callLogKey: String,
    /** Epoch milliseconds (UTC) of the call. */
    @ColumnInfo(name = "timestamp_utc") val timestampUtc: Long,
)
