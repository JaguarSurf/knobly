package app.knobly.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// One DAO for both tables: deleting a call and remembering its call log entry happen in the same transaction.
@Suppress("TooManyFunctions")
@Dao
interface CallEventDao {
    @Insert
    suspend fun insert(event: CallEventEntity): Long

    @Insert
    suspend fun insertAll(events: List<CallEventEntity>)

    /** Newest first. */
    @Query("SELECT * FROM call_events ORDER BY timestamp_utc DESC, id DESC")
    fun observeAll(): Flow<List<CallEventEntity>>

    @Query("SELECT * FROM call_events WHERE action = 'REJECT' ORDER BY timestamp_utc DESC, id DESC LIMIT 1")
    fun observeLatestRejected(): Flow<CallEventEntity?>

    /**
     * Calls screened by Knobly at or after [timestampUtc] that are not linked to a call log entry yet, oldest first.
     */
    @Query(
        "SELECT * FROM call_events WHERE source = 'knobly' AND call_log_key IS NULL " +
            "AND timestamp_utc >= :timestampUtc ORDER BY timestamp_utc, id",
    )
    suspend fun unlinked(timestampUtc: Long): List<CallEventEntity>

    /**
     * The call log entries already in the history or removed by the user, at or after [timestampUtc]: they are not
     * brought in again.
     */
    @Query(
        "SELECT call_log_key FROM call_events WHERE call_log_key IS NOT NULL AND timestamp_utc >= :timestampUtc " +
            "UNION SELECT call_log_key FROM ignored_call_log_entries WHERE timestamp_utc >= :timestampUtc",
    )
    suspend fun knownCallLogKeys(timestampUtc: Long): List<String>

    /** Links a screened call to its call log entry and, for calls that rang, stores how it ended. */
    @Query("UPDATE call_events SET call_log_key = :callLogKey, outcome = COALESCE(:outcome, outcome) WHERE id = :id")
    suspend fun link(
        id: Long,
        callLogKey: String,
        outcome: String?,
    )

    /** Events at or after [timestampUtc] (epoch milliseconds), newest first. */
    @Query("SELECT * FROM call_events WHERE timestamp_utc >= :timestampUtc ORDER BY timestamp_utc DESC, id DESC")
    fun observeSince(timestampUtc: Long): Flow<List<CallEventEntity>>

    /** Deletes one event; its call log entry, if any, is not brought in again. */
    @Transaction
    suspend fun delete(id: Long) {
        ignoreCallLogEntryOf(id)
        deleteEvent(id)
    }

    /** Deletes every event; their call log entries are not brought in again. */
    @Transaction
    suspend fun deleteAll() {
        ignoreAllCallLogEntries()
        deleteAllEvents()
    }

    @Query(
        "INSERT OR IGNORE INTO ignored_call_log_entries (call_log_key, timestamp_utc) " +
            "SELECT call_log_key, timestamp_utc FROM call_events WHERE id = :id AND call_log_key IS NOT NULL",
    )
    suspend fun ignoreCallLogEntryOf(id: Long)

    @Query(
        "INSERT OR IGNORE INTO ignored_call_log_entries (call_log_key, timestamp_utc) " +
            "SELECT call_log_key, timestamp_utc FROM call_events WHERE call_log_key IS NOT NULL",
    )
    suspend fun ignoreAllCallLogEntries()

    @Query("DELETE FROM call_events WHERE id = :id")
    suspend fun deleteEvent(id: Long)

    @Query("DELETE FROM call_events")
    suspend fun deleteAllEvents()
}
