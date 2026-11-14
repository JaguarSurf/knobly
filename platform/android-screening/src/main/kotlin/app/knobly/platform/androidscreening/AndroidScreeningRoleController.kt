package app.knobly.platform.androidscreening

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.knobly.domain.port.ScreeningRoleController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [ScreeningRoleController] backed by `RoleManager.ROLE_CALL_SCREENING`.
 *
 * Requesting the role shows a system dialog, so an activity must be [bind]-ed (from `onCreate`) first.
 * Must be used from the main thread.
 */
class AndroidScreeningRoleController(
    context: Context,
) : ScreeningRoleController {
    private val roleManager: RoleManager? = context.getSystemService(RoleManager::class.java)
    private val held = MutableStateFlow(isHeldNow())
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var pendingRequest: CompletableDeferred<Boolean>? = null

    override val isRoleAvailable: Boolean =
        roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true

    override val isRoleHeld: StateFlow<Boolean> = held.asStateFlow()

    /** Registers the role request dialog for [activity]. Call it from `onCreate`, before the activity starts. */
    fun bind(activity: ComponentActivity) {
        launcher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                refresh()
                pendingRequest?.complete(held.value)
                pendingRequest = null
            }
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                // The user may grant or revoke the role in the system settings while the app is in background.
                override fun onResume(owner: LifecycleOwner) = refresh()

                override fun onDestroy(owner: LifecycleOwner) {
                    launcher = null
                    pendingRequest?.complete(held.value)
                    pendingRequest = null
                }
            },
        )
    }

    override suspend fun requestRole(): Boolean {
        val manager = roleManager
        val activityLauncher = launcher
        return when {
            held.value -> {
                true
            }

            !isRoleAvailable || manager == null || activityLauncher == null -> {
                false
            }

            else -> {
                val request = CompletableDeferred<Boolean>().also { pendingRequest = it }
                activityLauncher.launch(manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                request.await()
            }
        }
    }

    private fun refresh() {
        held.value = isHeldNow()
    }

    private fun isHeldNow(): Boolean = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
}
