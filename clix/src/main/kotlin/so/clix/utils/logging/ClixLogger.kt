package so.clix.utils.logging

import android.util.Log

internal object ClixLogger {
    @Volatile private var logLevel = ClixLogLevel.INFO
    private const val TAG = "Clix"
    private const val DEFAULT_ERROR_MESSAGE = "Unknown error"

    @Synchronized
    fun setLogLevel(level: ClixLogLevel) {
        logLevel = level
    }

    fun error(message: String?, error: Throwable? = null) {
        log(ClixLogLevel.ERROR, message, error)
    }

    fun warn(message: String?, error: Throwable? = null) {
        log(ClixLogLevel.WARN, message, error)
    }

    fun info(message: String?, error: Throwable? = null) {
        log(ClixLogLevel.INFO, message, error)
    }

    fun debug(message: String?, error: Throwable? = null) {
        log(ClixLogLevel.DEBUG, message, error)
    }

    fun log(
        level: ClixLogLevel = ClixLogLevel.DEBUG,
        message: String? = DEFAULT_ERROR_MESSAGE,
        error: Throwable? = null,
    ) {
        if (level > logLevel) {
            return
        }

        when (level) {
            ClixLogLevel.DEBUG -> {
                Log.d(TAG, message, error)
            }

            ClixLogLevel.INFO -> {
                Log.i(TAG, message, error)
            }

            ClixLogLevel.WARN -> {
                Log.w(TAG, message, error)
            }

            ClixLogLevel.ERROR -> {
                Log.e(TAG, message, error)
            }

            ClixLogLevel.NONE -> {}
        }
    }
}
