package app.knobly.platform.androidscreening

import android.Manifest
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import app.knobly.domain.port.ContactNumber
import app.knobly.domain.port.ContactsAccess
import kotlinx.coroutines.flow.StateFlow

/**
 * [ContactsAccess] backed by `READ_CONTACTS` and `ContactsContract`: reads every saved phone number and name (the
 * domain indexes and matches them) and a cheap version of the contacts (the latest update and deletion times).
 *
 * Requesting the permission needs an activity: [bind] it from `onCreate`. Reads work from any context (also from
 * the call screening service), but block: never call them on the main thread.
 */
class AndroidContactsAccess(
    context: Context,
) : ContactsAccess {
    private val appContext = context.applicationContext
    private val permission = AndroidPermission(context, Manifest.permission.READ_CONTACTS)

    override val isGranted: StateFlow<Boolean> = permission.isGranted

    fun bind(activity: ComponentActivity) = permission.bind(activity)

    override suspend fun requestAccess(): Boolean = permission.request()

    override fun openAppSettings() = permission.openAppSettings()

    override fun phoneNumbers(): List<ContactNumber> {
        if (!permission.isGrantedNow()) return emptyList()
        val phones = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val columns =
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        return appContext.contentResolver
            .query(phones, columns, null, null, null)
            ?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) cursor.getString(0)?.let { add(ContactNumber(it, cursor.getString(1))) }
                }
            }.orEmpty()
    }

    /**
     * When the most recently changed contact was updated, and when the most recently deleted one was deleted: any
     * insert, edit or delete changes one of them. Two one-row queries.
     */
    override fun contactsVersion(): String {
        if (!permission.isGrantedNow()) return ""
        val updated =
            latest(ContactsContract.Contacts.CONTENT_URI, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
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
}
