package so.clix.services

import android.content.Context
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class TokenServiceTest {

    private lateinit var context: Context
    private lateinit var storageService: StorageService
    private lateinit var tokenService: TokenService

    @Before
    fun setup() {
        // Use Robolectric's application context
        context = RuntimeEnvironment.getApplication()
        storageService = StorageService(context)

        // Create a real TokenService with the Robolectric context
        tokenService = TokenService(storageService)

        // Clear any existing preferences before each test
        val sharedPreferences =
            context.getSharedPreferences("clix_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    @Test
    fun `it should return null when current token is not found`() {
        // Given
        // No token is set in the storage

        // When
        val result = tokenService.getCurrentToken()

        // Then
        assertNull(result)
    }

    @Test
    fun `it should return token when current token is found`() {
        // Given
        val token = "test-token"
        storageService.set("clix_current_push_token", Token(token))

        // When
        val result = tokenService.getCurrentToken()

        // Then
        assertEquals(token, result)
    }

    @Test
    fun `it should return empty list when previous tokens are not found`() {
        // Given
        // No previous tokens are set in the storage

        // When
        val result = tokenService.getPreviousTokens()

        // Then
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `it should return previous tokens when found`() {
        // Given
        val previousTokens = listOf("token1", "token2")
        storageService.set("clix_push_tokens", previousTokens)

        // When
        val result = tokenService.getPreviousTokens()

        // Then
        assertEquals(previousTokens, result)
    }

    @Test
    fun `it should save token and update previous tokens when current token exists`() {
        // Given
        val currentToken = "current-token"
        val newToken = "new-token"
        val previousTokens = listOf("token1", "token2")

        // Set up initial state
        storageService.set("clix_current_push_token", Token(currentToken))
        storageService.set("clix_push_tokens", previousTokens)

        // When
        tokenService.saveToken(newToken)

        // Then
        val savedToken = tokenService.getCurrentToken()
        val savedPreviousTokens = tokenService.getPreviousTokens()

        assertEquals(newToken, savedToken)
        assertEquals(3, savedPreviousTokens.size)
        assertEquals(currentToken, savedPreviousTokens.last())
    }

    @Test
    fun `it should save token without updating previous tokens when current token does not exist`() {
        // Given
        val newToken = "new-token"
        val previousTokens = listOf("token1", "token2")

        // Set up initial state
        storageService.set("clix_push_tokens", previousTokens)

        // When
        tokenService.saveToken(newToken)

        // Then
        val savedToken = tokenService.getCurrentToken()
        val savedPreviousTokens = tokenService.getPreviousTokens()

        assertEquals(newToken, savedToken)
        assertEquals(2, savedPreviousTokens.size)
    }

    @Test
    fun `it should limit the number of previous tokens to 5`() {
        // Given
        val currentToken = "current-token"
        val newToken = "new-token"
        val previousTokens = listOf("token1", "token2", "token3", "token4", "token5")

        // Set up initial state
        storageService.set("clix_current_push_token", Token(currentToken))
        storageService.set("clix_push_tokens", previousTokens)

        // When
        tokenService.saveToken(newToken)

        // Then
        val savedPreviousTokens = tokenService.getPreviousTokens()

        assertEquals(5, savedPreviousTokens.size)
        assertEquals(currentToken, savedPreviousTokens.last())
        assertEquals("token2", savedPreviousTokens.first())
    }

    @Test
    fun `it should clear tokens`() {
        // Given
        val token = "test-token"
        val previousTokens = listOf("token1", "token2")

        // Set up initial state
        storageService.set("clix_current_push_token", Token(token))
        storageService.set("clix_push_tokens", previousTokens)

        // When
        tokenService.clearTokens()

        // Then
        val savedToken = tokenService.getCurrentToken()
        val savedPreviousTokens = tokenService.getPreviousTokens()

        assertNull(savedToken)
        assertEquals(emptyList<String>(), savedPreviousTokens)
    }

    @Test
    fun `it should reset tokens`() {
        // Given
        val token = "test-token"
        val previousTokens = listOf("token1", "token2")

        // Set up initial state
        storageService.set("clix_current_push_token", Token(token))
        storageService.set("clix_push_tokens", previousTokens)

        // When
        tokenService.reset()

        // Then
        val savedToken = tokenService.getCurrentToken()
        val savedPreviousTokens = tokenService.getPreviousTokens()

        assertNull(savedToken)
        assertEquals(emptyList<String>(), savedPreviousTokens)
    }
}
