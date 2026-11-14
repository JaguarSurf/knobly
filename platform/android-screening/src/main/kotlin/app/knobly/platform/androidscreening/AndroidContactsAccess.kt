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
import app.knobly.domain.port.ContactsAccess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [ContactsAccess] backed by `READ_CONTACTS` and `ContactsContract`: reads every saved phone number (the domain
 * indexes and matches them) and a cheap version of the contacts (the latest update and deletion times).
 *
 * Requesting the permission needs an activity: [bind] it from `onCreate`. Reads work from any context (also from
 * the call screening service), but block: never call [phoneNumbers] on the main thread.
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

    override fun phoneNumbers(): List<String> {
        if (!isGrantedNow()) return emptyList()
        val phones = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val column = ContactsContract.CommonDataKinds.Phone.NUMBER
        return appContext.contentResolver
            .query(phones, arrayOf(column), null, null, null)
            ?.use { cursor -> buildList { while (cursor.moveToNext()) cursor.getString(0)?.let(::add) } }
            .orEmpty()
    }

    /**
     * When the most recently changed contact was updated, and when the most recently deleted one was deleted: any
     * insert, edit or delete changes one of them. Two one-row queries.
     */
    override fun contactsVersion(): String {
        if (!isGrantedNow()) return ""
        val updated =
            latest(
                ContactsContract.Contacts.CONTENT_URI,
                ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP,
            )
        val deleted =
            latest(
                ContactsContract.DeletedContacts.CONTENT_URI,
                ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP,
            )
        return "$updated/$deleted"
    }

    private fun latest(
        table: Uri,
        column: String,
    ): Long {
        val uri = table.buildUpon().appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY, "1").build()
        return appContext.contentResolver
            .query(uri, arrayOf(column), null, null, "$column DESC")
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L } ?: 0L
    }

    private fun refresh() {
        granted.value = isGrantedNow()
    }

    private fun isGrantedNow(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
}
