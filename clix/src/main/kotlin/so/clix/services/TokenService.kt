package so.clix.services

import kotlinx.serialization.Serializable

@Serializable
internal data class Token(val token: String, val timestamp: Long = System.currentTimeMillis())

internal class TokenService(private val storageService: StorageService) {
    private val currentTokenKey = "clix_current_token"
    private val previousTokensKey = "clix_previous_tokens"

    fun getCurrentToken(): String? = storageService.get<Token>(currentTokenKey)?.token

    fun getPreviousTokens(): List<String> =
        storageService.get<List<String>>(previousTokensKey) ?: emptyList()

    fun saveToken(token: String) {
        val previousTokens = getPreviousTokens().toMutableList()

        getCurrentToken()?.let { currentToken -> previousTokens.add(currentToken) }

        if (previousTokens.size > MAX_PREVIOUS_TOKENS) {
            previousTokens.subList(0, previousTokens.size - MAX_PREVIOUS_TOKENS).clear()
        }

        storageService.set(previousTokensKey, previousTokens)

        storageService.set(currentTokenKey, Token(token = token))
    }

    fun clearTokens() {
        storageService.remove(currentTokenKey)
        storageService.remove(previousTokensKey)
    }

    fun reset() {
        clearTokens()
    }

    companion object {
        private const val MAX_PREVIOUS_TOKENS = 5
    }
}
