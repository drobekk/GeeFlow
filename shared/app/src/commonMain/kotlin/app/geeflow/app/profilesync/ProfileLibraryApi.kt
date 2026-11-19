package app.geeflow.app.profilesync

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class ProfileLibraryApi {
    private val log = Logger.withTag("ProfileLibrarySync")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
        expectSuccess = false
    }

    suspend fun fetch(token: String): ProfileLibraryCatalog {
        val response = client.get(CATALOG_URL) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
        if (response.status != HttpStatusCode.OK) {
            log.w { "Catalog read returned HTTP ${response.status.value}" }
            throw ProfileLibraryRequestException(response.status.value)
        }
        return response.body()
    }

    suspend fun replace(
        token: String,
        expectedRevision: Long,
        catalog: ProfileLibraryCatalog,
    ): ProfileLibraryCatalog {
        val response = client.put(CATALOG_URL) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.IfMatch, "\"$expectedRevision\"")
            contentType(ContentType.Application.Json)
            setBody(catalog)
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.Conflict -> {
                val conflict = json.decodeFromString<ProfileLibraryConflict>(response.bodyAsText())
                throw ProfileLibraryRevisionConflict(conflict.catalog)
            }
            else -> {
                log.w { "Catalog write returned HTTP ${response.status.value}" }
                throw ProfileLibraryRequestException(response.status.value)
            }
        }
    }

    companion object {
        const val CATALOG_URL = "https://hostinghk.pangxinlabi.top/profile-library/api/v1/catalog"
        private const val CONNECT_TIMEOUT_MILLIS = 10_000L
        private const val REQUEST_TIMEOUT_MILLIS = 20_000L
    }
}

@Serializable
private data class ProfileLibraryConflict(
    val error: String,
    val catalog: ProfileLibraryCatalog,
)

class ProfileLibraryRequestException(val statusCode: Int) : IllegalStateException(
    when (statusCode) {
        401 -> "Profile library token was rejected"
        404 -> "Profile library service was not found"
        else -> "Profile library request failed with HTTP $statusCode"
    },
)

class ProfileLibraryRevisionConflict(val catalog: ProfileLibraryCatalog) : IllegalStateException(
    "Profile library changed while this device was syncing",
)
