package app.knobly.platform.androidscreening

import androidx.room.Room
import app.knobly.core.database.KnoblyDatabase
import app.knobly.core.database.buildKnoblyDatabase
import app.knobly.core.datastore.createJsonDocumentStore
import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.port.ContactsAccess
import app.knobly.domain.port.ScreeningRoleController
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.rules.call.SCREENING_DISPATCHER
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

/** Android adapters of the platform ports, plus Android-specific wiring of the shared graph. */
val androidScreeningModule =
    module {
        single { AndroidScreeningRoleController(androidContext()) } bind ScreeningRoleController::class
        single { AndroidContactsAccess(androidContext()) } bind ContactsAccess::class
        single<PhoneNumberNormalizer> { LibPhoneNumberNormalizer() }
        single {
            createJsonDocumentStore(
                path = File(androidContext().filesDir, "datastore/call_filters.json").absolutePath,
                serializer = CallFilterSettings.serializer(),
                default = CallFilterSettings(),
                scope = get(),
            )
        }
        single {
            val context = androidContext()
            Room
                .databaseBuilder<KnoblyDatabase>(
                    context,
                    context.getDatabasePath(KnoblyDatabase.FILE_NAME).absolutePath,
                ).buildKnoblyDatabase()
        }
        // Contacts lookups block on the content provider: screen calls on the IO dispatcher.
        single<CoroutineDispatcher>(named(SCREENING_DISPATCHER)) { Dispatchers.IO }
    }
