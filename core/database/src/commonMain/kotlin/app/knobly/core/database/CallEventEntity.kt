package app.knobly.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A call screened by Knobly (today only rejected calls are stored). Rule details are a snapshot taken when the call
 * was screened, so the history stays readable after the rule is edited or deleted.
 */
@Entity(tableName = "call_events", indices = [Index("timestamp_utc")])
data class CallEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch milliseconds (UTC). */
    @ColumnInfo(name = "timestamp_utc") val timestampUtc: Long,
    /** `null` when the number was hidden. */
    @ColumnInfo(name = "number_raw") val numberRaw: String?,
    @ColumnInfo(name = "number_e164") val numberE164: String?,
    @ColumnInfo(name = "number_national") val numberNational: String?,
    /** `REJECT` (later also `SILENCE`). */
    val action: String,
    /** Why: `contacts_only`, `custom_rule` or `unknown`. */
    val reason: String,
    @ColumnInfo(name = "matched_rule_id") val matchedRuleId: String?,
    @ColumnInfo(name = "rule_type") val ruleType: String?,
    @ColumnInfo(name = "rule_value") val ruleValue: String?,
    @ColumnInfo(name = "rule_label") val ruleLabel: String?,
)
