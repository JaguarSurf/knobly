package app.knobly.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.knobly.app.shared.App
import app.knobly.platform.androidscreening.AndroidScreeningRoleController
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        get<AndroidScreeningRoleController>().bind(this)
        setContent { App() }
    }
}
