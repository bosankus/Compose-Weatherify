package bose.ankush.network.api

import bose.ankush.network.auth.interceptor.authorizedRequest
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.model.Account
import bose.ankush.network.model.AccountPhoto
import bose.ankush.network.model.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData

class KtorAccountApiService(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager,
    private val baseUrl: String,
) : AccountApiService {
    override suspend fun getAccount(): ApiResponse<Account> =
        httpClient
            .authorizedRequest(tokenManager) { authConfig ->
                get("$baseUrl/account") { authConfig() }
            }.body()

    override suspend fun uploadAccountPhoto(
        bytes: ByteArray,
        contentType: String,
        fileName: String,
    ): ApiResponse<AccountPhoto> =
        httpClient
            .authorizedRequest(tokenManager) { authConfig ->
                submitFormWithBinaryData(
                    url = "$baseUrl/account/photo",
                    formData = photoUploadForm(bytes, contentType, fileName),
                ) { authConfig() }
            }.body()

    override suspend fun deleteAccountPhoto(): ApiResponse<AccountPhoto> =
        httpClient
            .authorizedRequest(tokenManager) { authConfig ->
                delete("$baseUrl/account/photo") { authConfig() }
            }.body()
}

/**
 * The multipart body POST /account/photo parses: one file part named [PHOTO_FIELD] carrying
 * its own content type, which the server checks against JPEG, PNG, and WebP.
 */
internal fun photoUploadForm(
    bytes: ByteArray,
    contentType: String,
    fileName: String,
): List<PartData> =
    formData {
        append(
            key = PHOTO_FIELD,
            value = bytes,
            headers =
                Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                },
        )
    }

internal const val PHOTO_FIELD = "file"
