package app.knobly.platform.androidscreening

import app.knobly.core.phone.LibPhoneNumberNormalizer
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.port.ScreeningRoleController
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

/** Android adapters of the platform ports. */
val androidScreeningModule =
    module {
        single { AndroidScreeningRoleController(androidContext()) } bind ScreeningRoleController::class
        single<PhoneNumberNormalizer> { LibPhoneNumberNormalizer() }
    }
