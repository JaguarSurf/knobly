package app.knobly.domain.rules.call

import app.knobly.core.model.IncomingCall
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.rules.Rule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * In-memory cache of the engine rules, rebuilt whenever the settings change. The call screening reads it without
 * touching the disk (only the very first call after the process starts waits for the settings to load).
 */
class CallRulesProvider(
    settings: Flow<CallFilterSettings>,
    normalizer: PhoneNumberNormalizer,
    scope: CoroutineScope,
) {
    private val rules: StateFlow<List<Rule<IncomingCall>>?> =
        settings.map { buildCallRules(it, normalizer) }.stateIn(scope, SharingStarted.Eagerly, null)

    /** The current rules, waiting for the first load if needed. */
    suspend fun await(): List<Rule<IncomingCall>> = rules.filterNotNull().first()
}
