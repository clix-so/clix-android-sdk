package so.clix.services

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import so.clix.utils.logging.ClixLogger

internal class StorageService(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("clix_preferences", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Saves a value of a specified type to the storage.
     *
     * @param key Key to save value under
     * @param value Value to save
     */
    inline fun <reified T> set(key: String, value: T?) {
        if (value == null) {
            remove(key)
            return
        }

        val serializer = serializer<T>()
        val jsonString = json.encodeToString(serializer, value)
        sharedPreferences.edit { putString(key, jsonString) }
    }

    /**
     * Retrieves a value of a specified type from the storage.
     *
     * @param key Key to get value for
     * @return Value if found and can be converted to a specified type, null otherwise
     */
    inline fun <reified T> get(key: String): T? {
        val jsonString = sharedPreferences.getString(key, null) ?: return null
        return try {
            val serializer = serializer<T>()
            json.decodeFromString(serializer, jsonString)
        } catch (e: SerializationException) {
            ClixLogger.error(message = "Failed to deserialize stored value for key $key", error = e)
            null
        }
    }

    /**
     * Removes a value from the storage.
     *
     * @param key Key to remove value for
     */
    fun remove(key: String) {
        sharedPreferences.edit { remove(key) }
    }
}
