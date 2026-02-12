package so.clix.services

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.launch
import so.clix.core.Clix
import so.clix.utils.logging.ClixLogger

internal enum class SessionEvent {
    SESSION_START
}

internal class SessionService(
    private val storageService: StorageService,
    private val eventService: EventService,
    sessionTimeoutMs: Int,
) : DefaultLifecycleObserver {

    private val effectiveTimeoutMs = maxOf(sessionTimeoutMs, 5000)

    @Volatile private var pendingMessageId: String? = null

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            ClixLogger.debug("App is in background, deferring session start")
            return
        }

        val lastActivity = storageService.get<Long>(LAST_ACTIVITY_KEY)
        if (lastActivity != null) {
            val elapsed = System.currentTimeMillis() - lastActivity
            if (elapsed <= effectiveTimeoutMs) {
                updateLastActivity()
                ClixLogger.debug("Continuing existing session")
                return
            }
        }
        startNewSession()
    }

    override fun onStart(owner: LifecycleOwner) {
        val lastActivity = storageService.get<Long>(LAST_ACTIVITY_KEY)
        if (lastActivity != null) {
            val elapsed = System.currentTimeMillis() - lastActivity
            if (elapsed <= effectiveTimeoutMs) {
                updateLastActivity()
                return
            }
        }
        startNewSession()
    }

    override fun onStop(owner: LifecycleOwner) {
        updateLastActivity()
    }

    fun setPendingMessageId(messageId: String?) {
        pendingMessageId = messageId
    }

    private fun startNewSession() {
        val messageId = pendingMessageId
        pendingMessageId = null
        updateLastActivity()

        Clix.coroutineScope.launch {
            try {
                eventService.trackEvent(
                    name = SessionEvent.SESSION_START.name,
                    messageId = messageId,
                )
                ClixLogger.debug("${SessionEvent.SESSION_START.name} tracked")
            } catch (e: Exception) {
                ClixLogger.error("Failed to track ${SessionEvent.SESSION_START.name}", e)
            }
        }
    }

    private fun updateLastActivity() {
        storageService.set(LAST_ACTIVITY_KEY, System.currentTimeMillis())
    }

    companion object {
        private const val LAST_ACTIVITY_KEY = "clix_session_last_activity"
    }
}
