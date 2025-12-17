package so.clix.samples.basic

import android.content.Context
import kotlinx.serialization.json.Json
import so.clix.core.ClixConfig
import so.clix.utils.logging.ClixLogLevel

object ClixConfiguration {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val logLevel: ClixLogLevel = ClixLogLevel.DEBUG

    lateinit var config: ClixConfig
        private set

    fun initialize(context: Context) {
        config =
            json
                .decodeFromString<ClixConfig>(
                    context.assets.open("ClixConfig.json").bufferedReader().readText()
                )
                .copy(logLevel = logLevel)
    }
}
