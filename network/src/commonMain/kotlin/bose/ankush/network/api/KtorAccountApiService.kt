package bose.ankush.network.api

import bose.ankush.network.auth.interceptor.authorizedRequest
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.model.Account
import bose.ankush.network.model.AccountPhoto
import bose.ankush.network.model.ApiErrorData
import bose.ankush.network.model.ApiResponse
import bose.ankush.network.model.DeleteAccountRequest
import bose.ankush.network.model.DeleteAccountResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

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

    // Never retried: a repeat after a lost 200 would 401 and read as a failure. The interceptor
    // replays it once after a token refresh, which is safe because the first attempt was refused.
    override suspend fun deleteAccount(password: String): DeleteAccountResult {
        val response =
            httpClient.authorizedRequest(tokenManager) { authConfig ->
                delete("$baseUrl/account") {
                    authConfig()
                    contentType(ContentType.Application.Json)
                    setBody(DeleteAccountRequest(password))
                }
            }
        val errorCode =
            if (response.status == HttpStatusCode.Forbidden) response.errorCodeOrNull() else null
        return deleteAccountResult(response.status, errorCode)
    }

    /** The envelope's `data.errorCode`, or null when the body isn't one. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun HttpResponse.errorCodeOrNull(): String? =
        try {
            body<ApiResponse<ApiErrorData>>().data?.errorCode
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
}

/**
 * Maps DELETE /account's status to its outcome. Only a 403 that says `INVALID_PASSWORD` is a wrong
 * password; any other 403 is unexpected and reported as a failure, never as a sign-out.
 */
internal fun deleteAccountResult(
    status: HttpStatusCode,
    errorCode: String?,
): DeleteAccountResult =
    when {
        status.isSuccess() -> DeleteAccountResult.Deleted
        status == HttpStatusCode.Forbidden && errorCode == INVALID_PASSWORD ->
            DeleteAccountResult.IncorrectPassword

        status == HttpStatusCode.Unauthorized -> DeleteAccountResult.SessionExpired
        status == HttpStatusCode.TooManyRequests -> DeleteAccountResult.RateLimited
        status == HttpStatusCode.BadRequest -> DeleteAccountResult.InvalidRequest
        else -> DeleteAccountResult.Failed()
    }

internal const val INVALID_PASSWORD = "INVALID_PASSWORD"

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
