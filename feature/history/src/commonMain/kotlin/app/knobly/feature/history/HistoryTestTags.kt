package app.knobly.feature.history

/** Test tags of the History tab, shared with UI tests. */
object HistoryTestTags {
    const val CLEAR = "history.clear"
    const val CLEAR_CONFIRM = "history.clearConfirm"
    const val DETAILS = "history.details"
    const val COPY = "history.copy"
    const val DELETE = "history.delete"
    const val CALL_LOG = "history.callLog"
    const val CALL_LOG_ALLOW = "history.callLogAllow"
    const val FROM_CALL_LOG = "history.fromCallLog"

    fun filter(filter: HistoryFilter) = "history.filter.${filter.name}"

    fun entry(id: Long) = "history.entry.$id"
}
