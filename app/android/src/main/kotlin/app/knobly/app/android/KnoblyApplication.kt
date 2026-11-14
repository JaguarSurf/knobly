package app.knobly.app.android

import android.app.Application
import app.knobly.app.shared.knoblyModules
import app.knobly.platform.androidscreening.androidScreeningModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KnoblyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Also runs before KnoblyCallScreeningService when the system starts the process for a call.
        startKoin {
            androidContext(this@KnoblyApplication)
            modules(knoblyModules(androidScreeningModule))
        }
    }
}
