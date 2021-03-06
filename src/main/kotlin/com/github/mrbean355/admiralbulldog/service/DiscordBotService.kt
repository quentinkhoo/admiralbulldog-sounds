package com.github.mrbean355.admiralbulldog.service

import com.github.mrbean355.admiralbulldog.assets.SoundByte
import com.github.mrbean355.admiralbulldog.persistence.ConfigPersistence
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

var hostUrl = "http://prod.upmccxmkjx.us-east-2.elasticbeanstalk.com:8090"
private val logger = LoggerFactory.getLogger("DiscordBotService")

/**
 * Play the given [soundByte] on Discord through the bot.
 * Can pass in a custom [token] to be used instead of loading one from config.
 * @return `true` if the sound was actually played through Discord.
 */
suspend fun playSoundOnDiscord(soundByte: SoundByte, token: String = ConfigPersistence.getDiscordToken()): Boolean {
    if (token.isBlank()) {
        logger.warn("Blank token set!")
        return false
    }
    val response = service.playSound(PlaySoundRequest(loadUserId(), token, soundByte.fileName))
    if (!response.isSuccessful) {
        logger.info("Play sound through Discord failed! soundFile=$soundByte, response=$response")
        return false
    }
    return true
}

/**
 * Log an analytics event.
 */
fun logAnalyticsEvent(eventType: String, eventData: String = "") {
    GlobalScope.launch {
        service.logAnalyticsEvent(AnalyticsRequest(loadUserId(), eventType, eventData))
    }
}

private interface DiscordBotService {

    @POST("/createId")
    suspend fun createId(): Response<CreateIdResponse>

    @POST("/")
    suspend fun playSound(@Body request: PlaySoundRequest): Response<Void>

    @POST("/analytics/logEvent")
    suspend fun logAnalyticsEvent(@Body request: AnalyticsRequest): Response<Void>

}

private data class CreateIdResponse(val userId: String)

private data class AnalyticsRequest(val userId: String, val eventType: String, val eventData: String)

private data class PlaySoundRequest(val userId: String, val token: String, val soundFileName: String)

private suspend fun loadUserId(): String {
    val userId = ConfigPersistence.getId()
    if (userId != null) {
        return userId
    }
    return mutex.withLock {
        val userId2 = ConfigPersistence.getId()
        if (userId2 != null) {
            userId2
        } else {
            val response = service.createId()
            if (response.isSuccessful) {
                response.body()?.userId.orEmpty().also {
                    ConfigPersistence.setId(it)
                }
            } else {
                ""
            }
        }
    }
}

private val mutex = Mutex()
private val service by lazy {
    Retrofit.Builder()
            .client(OkHttpClient.Builder()
                    .callTimeout(10, TimeUnit.SECONDS)
                    .build())
            .baseUrl(hostUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DiscordBotService::class.java)
}
