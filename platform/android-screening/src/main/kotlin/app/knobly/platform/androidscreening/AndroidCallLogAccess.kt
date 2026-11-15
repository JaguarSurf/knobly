package app.knobly.platform.androidscreening

import android.Manifest
import android.content.Context
import android.provider.CallLog
import androidx.activity.ComponentActivity
import app.knobly.domain.port.CallLogAccess
import app.knobly.domain.port.CallLogEntry
import app.knobly.domain.port.CallLogType
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

/**
 * [CallLogAccess] backed by `READ_CALL_LOG`: reads the incoming calls of the system call log, to bring into the
 * history the calls Knobly did not screen and to learn whether the calls that rang were answered. Only the number,
 * the time and the type are read; nothing is written to the call log.
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
        val types = (ANSWERED_TYPES + MISSED_TYPES + BLOCKED_TYPES).joinToString(",")
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
                                type =
                                    when (cursor.getInt(2)) {
                                        in ANSWERED_TYPES -> CallLogType.ANSWERED
                                        in BLOCKED_TYPES -> CallLogType.BLOCKED
                                        else -> CallLogType.MISSED
                                    },
                            ),
                        )
                    }
                }
            }.orEmpty()
    }

    private companion object {
        val ANSWERED_TYPES = setOf(CallLog.Calls.INCOMING_TYPE, CallLog.Calls.ANSWERED_EXTERNALLY_TYPE)

        /** Missed, declined by the user, or sent to voicemail. */
        val MISSED_TYPES = setOf(CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE, CallLog.Calls.VOICEMAIL_TYPE)

        /**
         * Blocked by the system or a call screening app. Knobly's own rejections are logged with this type too (despite
         * `setSkipCallLog`); [app.knobly.domain.history.CallLogSync] links them to the rejected calls.
         */
        val BLOCKED_TYPES = setOf(CallLog.Calls.BLOCKED_TYPE)
    }
}
