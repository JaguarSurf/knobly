package app.knobly.platform.androidscreening

import android.Manifest
import android.content.Context
import android.provider.CallLog
import androidx.activity.ComponentActivity
import app.knobly.domain.port.CallLogAccess
import app.knobly.domain.port.CallLogEntry
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

/**
 * [CallLogAccess] backed by `READ_CALL_LOG`: reads the incoming calls of the system call log, only to learn whether
 * the calls that rang were answered. Nothing is written to the call log.
 */
class AndroidCallLogAccess(
    context: Context,
) : CallLogAccess {
    private val appContext = context.applicationContext
    private val permission = AndroidPermission(context, Manifest.permission.READ_CALL_LOG)

    override val isGranted: StateFlow<Boolean> = permission.isGranted

    fun bind(activity: ComponentActivity) = permission.bind(activity)

    override suspend fun requestAccess(): Boolean = permission.request()

    override fun openAppSettings() = permission.openAppSettings()

    override fun incomingCalls(since: Instant): List<CallLogEntry> {
        if (!permission.isGrantedNow()) return emptyList()
        val types = (ANSWERED_TYPES + MISSED_TYPES).joinToString(",")
        return appContext.contentResolver
            .query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                "${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.TYPE} IN ($types)",
                arrayOf(since.toEpochMilliseconds().toString()),
                "${CallLog.Calls.DATE} ASC",
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            CallLogEntry(
                                number = cursor.getString(0),
                                timestamp = Instant.fromEpochMilliseconds(cursor.getLong(1)),
                                answered = cursor.getInt(2) in ANSWERED_TYPES,
                            ),
                        )
                    }
                }
            }.orEmpty()
    }

    private companion object {
        val ANSWERED_TYPES = setOf(CallLog.Calls.INCOMING_TYPE, CallLog.Calls.ANSWERED_EXTERNALLY_TYPE)

        /** Missed, declined by the user, or sent to voicemail. Blocked calls are Knobly's own rejections. */
        val MISSED_TYPES = setOf(CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE, CallLog.Calls.VOICEMAIL_TYPE)
    }
}
