package app.knobly.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallEventDao {
    @Insert
    suspend fun insert(event: CallEventEntity): Long

    /** Newest first. */
    @Query("SELECT * FROM call_events ORDER BY timestamp_utc DESC, id DESC")
    fun observeAll(): Flow<List<CallEventEntity>>

    @Query("SELECT * FROM call_events WHERE action = 'REJECT' ORDER BY timestamp_utc DESC, id DESC LIMIT 1")
    fun observeLatestRejected(): Flow<CallEventEntity?>

    /** Allowed calls at or after [timestampUtc] whose outcome is not known yet, oldest first. */
    @Query(
        "SELECT * FROM call_events WHERE action = 'ALLOW' AND outcome IS NULL AND timestamp_utc >= :timestampUtc " +
            "ORDER BY timestamp_utc",
    )
    suspend fun pendingOutcomes(timestampUtc: Long): List<CallEventEntity>

    @Query("UPDATE call_events SET outcome = :outcome WHERE id = :id")
    suspend fun setOutcome(
        id: Long,
        outcome: String,
    )

    /** Events at or after [timestampUtc] (epoch milliseconds), newest first. */
    @Query("SELECT * FROM call_events WHERE timestamp_utc >= :timestampUtc ORDER BY timestamp_utc DESC, id DESC")
    fun observeSince(timestampUtc: Long): Flow<List<CallEventEntity>>

    @Query("DELETE FROM call_events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM call_events")
    suspend fun deleteAll()

    /** Retention: removes the events older than [timestampUtc] (epoch milliseconds). */
    @Query("DELETE FROM call_events WHERE timestamp_utc < :timestampUtc")
    suspend fun deleteOlderThan(timestampUtc: Long): Int
}
