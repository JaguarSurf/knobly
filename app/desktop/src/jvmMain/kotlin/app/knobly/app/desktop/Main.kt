package app.knobly.app.desktop

import androidx.room.Room
import app.knobly.app.shared.knoblyModules
import app.knobly.core.database.KnoblyDatabase
import app.knobly.core.database.buildKnoblyDatabase
import app.knobly.core.datastore.DocumentStoreFactory
import app.knobly.core.datastore.JsonDocumentStoreFactory
import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.port.CallLogAccess
import app.knobly.domain.port.ContactsAccess
import app.knobly.domain.port.ScreeningRoleController
import app.knobly.platform.fake.FakeCallLogAccess
import app.knobly.platform.fake.FakeContactsAccess
import app.knobly.platform.fake.FakeScreeningRoleController
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File
import javax.swing.SwingUtilities

/** The harness's settings and database (delete it to start over). */
private val harnessDir = File(System.getProperty("java.io.tmpdir"), "knobly-desktop").apply { mkdirs() }

fun main() {
    startKoin {
        modules(
            knoblyModules(
                module {
                    single { FakeScreeningRoleController() } bind ScreeningRoleController::class
                    single { FakeContactsAccess() } bind ContactsAccess::class
                    single { FakeCallLogAccess() } bind CallLogAccess::class
                    single<PhoneNumberNormalizer> { LibPhoneNumberNormalizer() }
                    // Persisted between harness runs, outside the project.
                    single {
                        Room
                            .databaseBuilder<KnoblyDatabase>(
                                name = File(harnessDir, KnoblyDatabase.FILE_NAME).absolutePath,
                            ).buildKnoblyDatabase()
                    }
                    single<DocumentStoreFactory> { JsonDocumentStoreFactory(harnessDir.absolutePath, scope = get()) }
                },
            ),
        )
    }

    SwingUtilities.invokeLater(::showHarnessWindow)
}
