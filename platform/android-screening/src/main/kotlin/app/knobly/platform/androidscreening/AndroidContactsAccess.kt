package app.knobly.platform.androidscreening

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.knobly.core.model.PhoneNumber
import app.knobly.domain.port.ContactsAccess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [ContactsAccess] backed by `READ_CONTACTS` and `ContactsContract.PhoneLookup`, which matches the same number in
 * different formats (`+55 11 …`, `011 …`, `11 …`) the way the system dialer does.
 *
 * Requesting the permission needs an activity: [bind] it from `onCreate`. Lookups work from any context (also from
 * the call screening service), but block: never call [isContact] on the main thread.
 */
class AndroidContactsAccess(
    context: Context,
) : ContactsAccess {
    private val appContext = context.applicationContext
    private val granted = MutableStateFlow(isGrantedNow())
    private var launcher: ActivityResultLauncher<String>? = null
    private var pendingRequest: CompletableDeferred<Boolean>? = null

    override val isGranted: StateFlow<Boolean> = granted.asStateFlow()

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

    override suspend fun requestAccess(): Boolean {
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
                activityLauncher.launch(Manifest.permission.READ_CONTACTS)
                request.await()
            }
        }
    }

    override fun openAppSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", appContext.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    override fun isContact(number: PhoneNumber): Boolean {
        if (!isGrantedNow()) return false
        val lookupNumber = number.e164 ?: number.raw
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(lookupNumber))
        return appContext.contentResolver
            .query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            ?.use { it.moveToFirst() } ?: false
    }

    private fun refresh() {
        granted.value = isGrantedNow()
    }

    private fun isGrantedNow(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
}
