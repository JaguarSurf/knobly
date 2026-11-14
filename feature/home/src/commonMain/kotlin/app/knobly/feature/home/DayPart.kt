package app.knobly.feature.home

/** Part of the day used for the greeting on the home screen. */
enum class DayPart {
    MORNING,
    AFTERNOON,
    EVENING,
    ;

    companion object {
        private const val NOON = 12
        private const val EVENING_START = 18
        private const val MORNING_START = 5

        fun fromHour(hour: Int): DayPart =
            when (hour) {
                in MORNING_START until NOON -> MORNING
                in NOON until EVENING_START -> AFTERNOON
                else -> EVENING
            }
    }
}
