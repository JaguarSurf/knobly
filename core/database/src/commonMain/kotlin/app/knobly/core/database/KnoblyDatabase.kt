package app.knobly.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * The app's local database (Room, on every target). Everything stays on the device.
 *
 * Schema changes need a new [version] and a migration; exported schemas live in `core/database/schemas/`.
 */
@Database(entities = [CallEventEntity::class], version = 1, exportSchema = true)
@ConstructedBy(KnoblyDatabaseConstructor::class)
abstract class KnoblyDatabase : RoomDatabase() {
    abstract fun callEvents(): CallEventDao

    companion object {
        const val FILE_NAME = "knobly.db"
    }
}

/** Implemented by the Room compiler for each target. */
@Suppress("KotlinNoActualForExpect")
expect object KnoblyDatabaseConstructor : RoomDatabaseConstructor<KnoblyDatabase> {
    override fun initialize(): KnoblyDatabase
}

/**
 * Finishes a platform-specific builder (file path, or in-memory for tests) with the settings shared by every
 * platform: the bundled SQLite (same version everywhere) and queries off the main thread.
 */
fun RoomDatabase.Builder<KnoblyDatabase>.buildKnoblyDatabase(): KnoblyDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
