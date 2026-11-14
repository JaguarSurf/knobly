package app.knobly.app.shared

import app.knobly.core.datastore.InMemoryDocumentStore
import app.knobly.domain.history.InMemoryCallHistoryRepository
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.settings.AppSettings
import app.knobly.platform.fake.FakeContactsAccess
import app.knobly.platform.fake.FakeScreeningRoleController

/** The fakes behind a UI test, to arrange state and check effects. */
internal class TestEnv(
    val role: FakeScreeningRoleController,
    val contacts: FakeContactsAccess,
    val settings: InMemoryDocumentStore<CallFilterSettings>,
    val history: InMemoryCallHistoryRepository,
    val appSettings: InMemoryDocumentStore<AppSettings>,
)
