package so.clix.samples.basic

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Utility class to manage user preferences, including the user ID.
 */
class UserPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
    }

    /**
     * Saves the user ID to SharedPreferences.
     *
     * @param userId The user ID to save
     */
    fun saveUserId(userId: String) {
        sharedPreferences.edit {
            putString(KEY_USER_ID, userId)
        }
    }

    /**
     * Retrieves the stored user ID from SharedPreferences.
     *
     * @return The stored user ID, or null if not found
     */
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    /**
     * Removes the stored user ID from SharedPreferences.
     */
    fun removeUserId() {
        sharedPreferences.edit {
            remove(KEY_USER_ID)
        }
    }
}
