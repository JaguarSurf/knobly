package app.knobly.platform.androidscreening

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One runtime permission: its current state, the system dialog to request it and the app settings screen. Requesting
 * needs an activity: [bind] it from `onCreate`. Shared by the contacts and call log adapters.
 */
internal class AndroidPermission(
    context: Context,
    private val permission: String,
) {
    private val appContext = context.applicationContext
    private val granted = MutableStateFlow(isGrantedNow())
    private var launcher: ActivityResultLauncher<String>? = null
    private var pendingRequest: CompletableDeferred<Boolean>? = null

    val isGranted: StateFlow<Boolean> = granted.asStateFlow()

    fun bind(activity: ComponentActivity) {
        launcher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                refresh()
                pendingRequest?.complete(granted.value)
                pendingRequest = null
            }
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                // The user may grant or revoke the permission in the system settings while the app is in background.
                override fun onResume(owner: LifecycleOwner) = refresh()

                override fun onDestroy(owner: LifecycleOwner) {
                    launcher = null
                    pendingRequest?.complete(granted.value)
                    pendingRequest = null
                }
            },
        )
    }

    suspend fun request(): Boolean {
        refresh()
        val activityLauncher = launcher
        return when {
            granted.value -> {
                true
            }

            activityLauncher == null -> {
                false
            }

            else -> {
                val request = CompletableDeferred<Boolean>().also { pendingRequest = it }
                activityLauncher.launch(permission)
                request.await()
            }
        }
    }

    fun openAppSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", appContext.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    /** Checked on every use: the permission may have been revoked since. */
    fun isGrantedNow(): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    private fun refresh() {
        granted.value = isGrantedNow()
    }
}
