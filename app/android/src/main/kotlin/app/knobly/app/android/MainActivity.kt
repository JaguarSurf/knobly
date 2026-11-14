package app.knobly.app.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.knobly.app.shared.App
import app.knobly.platform.androidscreening.AndroidContactsAccess
import app.knobly.platform.androidscreening.AndroidScreeningRoleController
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Both system dialogs (call screening role, contacts permission) need an activity to show up.
        get<AndroidScreeningRoleController>().bind(this)
        get<AndroidContactsAccess>().bind(this)
        setContent {
            // Status and navigation bar icons follow the app's theme, which may differ from the phone's.
            App(
                onDarkThemeChange = { dark ->
                    val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                },
            )
        }
    }
}
