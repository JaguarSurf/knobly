package app.knobly.core.model

import kotlin.time.Instant

/** Something that happened on the device and can be evaluated by the rule engine. */
sealed interface Event {
    val timestamp: Instant
}

/**
 * An incoming phone call.
 *
 * @property number the caller's number, or `null` when the number is hidden/withheld.
 */
data class IncomingCall(
    val number: PhoneNumber?,
    override val timestamp: Instant,
) : Event
