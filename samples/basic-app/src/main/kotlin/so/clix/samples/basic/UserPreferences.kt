package so.clix.samples.basic

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Utility class to manage user preferences, including the user ID, project ID, and API key.
 */
class UserPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PROJECT_ID = "project_id"
        private const val KEY_API_KEY = "api_key"
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

    /**
     * Saves the project ID to SharedPreferences.
     *
     * @param projectId The project ID to save
     */
    fun saveProjectId(projectId: String) {
        sharedPreferences.edit {
            putString(KEY_PROJECT_ID, projectId)
        }
    }

    /**
     * Retrieves the stored project ID from SharedPreferences.
     *
     * @return The stored project ID, or null if not found
     */
    fun getProjectId(): String? {
        return sharedPreferences.getString(KEY_PROJECT_ID, null)
    }

    /**
     * Saves the API key to SharedPreferences.
     *
     * @param apiKey The API key to save
     */
    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit {
            putString(KEY_API_KEY, apiKey)
        }
    }

    /**
     * Retrieves the stored API key from SharedPreferences.
     *
     * @return The stored API key, or null if not found
     */
    fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_API_KEY, null)
    }
}
