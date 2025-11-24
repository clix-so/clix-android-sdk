package so.clix.samples.basic

import so.clix.notification.ClixMessagingService

class MessagingService : ClixMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AppState.updateFCMToken(token)
    }
}
