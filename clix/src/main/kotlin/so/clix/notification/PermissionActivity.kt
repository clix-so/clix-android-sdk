package so.clix.notification

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import so.clix.core.Clix
import so.clix.utils.logging.ClixLogger

/**
 * Internal transparent activity for requesting notification permission. Handles app restart
 * scenarios by persisting permission state using StorageService.
 */
internal class PermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            savePendingState(true)

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE,
            )
        } else {
            finishWithResult(true)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE) {
            val granted =
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

            savePermissionResult(granted)
            finishWithResult(granted)
        }
    }

    private fun finishWithResult(granted: Boolean) {
        resultCallback?.invoke(granted)
        resultCallback = null

        savePendingState(false)

        finish()
        overridePendingTransition(0, 0)
    }

    private fun savePendingState(pending: Boolean) {
        Clix.storageService.set(KEY_PENDING, pending)
    }

    private fun savePermissionResult(granted: Boolean) {
        Clix.storageService.set(KEY_RESULT, granted)
        Clix.storageService.set(KEY_TIMESTAMP, System.currentTimeMillis())
    }

    internal companion object {
        private const val REQUEST_CODE = 1001
        private const val KEY_PENDING = "clix_permission_pending"
        private const val KEY_RESULT = "clix_permission_result"
        private const val KEY_TIMESTAMP = "clix_permission_timestamp"
        private const val RESULT_TIMEOUT_MS = 5000L

        private var resultCallback: ((Boolean) -> Unit)? = null

        /**
         * Request notification permission without requiring Activity parameter. Uses Application
         * Context to start the permission activity.
         *
         * @param context Application context
         * @return true if permission was granted, false otherwise
         */
        internal suspend fun requestPermission(context: android.content.Context): Boolean {
            val wasPending = Clix.storageService.get<Boolean>(KEY_PENDING) ?: false

            if (wasPending) {
                val timestamp = Clix.storageService.get<Long>(KEY_TIMESTAMP) ?: 0
                val isRecent = (System.currentTimeMillis() - timestamp) < RESULT_TIMEOUT_MS

                if (isRecent) {
                    val result = Clix.storageService.get<Boolean>(KEY_RESULT) ?: false
                    Clix.storageService.remove(KEY_PENDING)

                    ClixLogger.debug("Recovered permission result after app restart: $result")
                    return result
                } else {
                    ClixLogger.warn("Permission result timeout - clearing stale state")
                    Clix.storageService.remove(KEY_PENDING)
                }
            }

            return suspendCancellableCoroutine { continuation ->
                resultCallback = { granted -> continuation.resume(granted) }

                // Use application context to start activity
                val intent = Intent(context, PermissionActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }
}
