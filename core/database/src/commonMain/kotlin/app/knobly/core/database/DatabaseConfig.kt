package app.knobly.core.database

/**
 * Local database settings. Room (KMP) entities, DAOs and migrations will live in this module
 * (`call_events` table, see docs/architecture.md).
 */
object DatabaseConfig {
    const val FILE_NAME = "knobly.db"
}
