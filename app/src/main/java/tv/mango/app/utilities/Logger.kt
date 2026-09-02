package tv.mango.app.utilities

import android.util.Log
import tv.mango.app.BuildConfig

/**
 * Logging funnel.
 *
 * Every call site goes through here so release builds can be stripped of
 * logging entirely by R8: the [BuildConfig.DEBUG] check is a compile-time
 * constant, so the bodies fold away rather than costing string concatenation on
 * a device that will never print them.
 */
object Logger {

    private const val TAG = "MangoTV"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun w(message: String, error: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.w(TAG, message, error)
    }

    /** Errors are kept in release builds; they are rare and worth reporting. */
    fun e(message: String, error: Throwable? = null) {
        Log.e(TAG, message, error)
    }
}
