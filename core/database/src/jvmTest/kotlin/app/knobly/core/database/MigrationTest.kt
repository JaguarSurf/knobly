package app.knobly.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Every schema change must keep the users' data (docs/adr/0007-app-updates-and-data-compatibility.md): a database
 * created with an old schema is migrated and read back with the current code.
 */
class MigrationTest {
    private val directory: Path = Files.createTempDirectory("knobly-migration")
    private val file = directory.resolve("knobly.db")

    @get:Rule
    val helper =
        MigrationTestHelper(
            schemaDirectoryPath = Path("schemas"),
            databasePath = file,
            driver = BundledSQLiteDriver(),
            databaseClass = KnoblyDatabase::class,
        )

    @AfterTest
    fun tearDown() {
        directory.toFile().deleteRecursively()
    }

    @Test
    fun `version 1 rejected calls survive the migration to version 2`() =
        runTest {
            helper.createDatabase(1).apply {
                execSQL(
                    "INSERT INTO call_events (timestamp_utc, number_raw, number_e164, number_national, action, " +
                        "reason, matched_rule_id, rule_type, rule_value, rule_label) VALUES " +
                        "(1794600000000, '03031234567', '+553031234567', '3031234567', 'REJECT', 'custom_rule', " +
                        "'tm', 'PREFIX', '0303', 'Telemarketing')",
                )
                close()
            }

            helper.runMigrationsAndValidate(2).close()

            val database =
                androidx.room.Room
                    .databaseBuilder<KnoblyDatabase>(name = file.toString())
                    .buildKnoblyDatabase()
            val event =
                database
                    .callEvents()
                    .observeAll()
                    .first()
                    .single()
            assertEquals("REJECT", event.action)
            assertEquals("Telemarketing", event.ruleLabel)
            assertNull(event.outcome)
            database.close()
        }

    @Test
    fun `version 2 calls survive the migration to version 3 as screened by Knobly`() =
        runTest {
            helper.createDatabase(2).apply {
                execSQL(
                    "INSERT INTO call_events (timestamp_utc, number_raw, number_e164, number_national, action, " +
                        "reason, matched_rule_id, rule_type, rule_value, rule_label, outcome) VALUES " +
                        "(1794600000000, '11912345678', '+5511912345678', '11912345678', 'ALLOW', 'none', " +
                        "NULL, NULL, NULL, NULL, 'answered')",
                )
                close()
            }

            helper.runMigrationsAndValidate(3).close()

            val database =
                androidx.room.Room
                    .databaseBuilder<KnoblyDatabase>(name = file.toString())
                    .buildKnoblyDatabase()
            val dao = database.callEvents()
            val event = dao.observeAll().first().single()
            assertEquals("answered", event.outcome)
            assertEquals(CallEventEntity.SOURCE_KNOBLY, event.source)
            assertNull(event.callLogKey)
            // Not linked to the call log yet: the next sync links it instead of bringing it in again.
            assertEquals(listOf(event.id), dao.unlinked(0).map { it.id })
            assertEquals(emptyList(), dao.knownCallLogKeys(0))
            database.close()
        }
}
