package com.your.sourbase.core.network
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
/** Safe repository failure with no exception text crossing presentation boundaries. */
data class ApiError(val message: String? = null)
/** Network/local outcome. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}
@Serializable
private data class ErrorDetail(val message: String? = null)
@Serializable
private data class ErrorBody(val message: String? = null, val details: List<ErrorDetail> = emptyList())
private val ErrorJson = Json { ignoreUnknownKeys = true }
/** Parses bounded backend messages; malformed bodies fall back to a resource in presentation. */
fun backendMessage(body: String?): String? {
    if (body == null || body.length > 8192) return null
    val parsed = runCatching { ErrorJson.decodeFromString<ErrorBody>(body) }.getOrNull() ?: return null
    fun String.valid() = isNotBlank() && length <= 512 && none { it.isISOControl() }
    return parsed.details.mapNotNull { it.message?.takeIf(String::valid) }.takeIf { it.isNotEmpty() }
        ?.joinToString("\n") ?: parsed.message?.takeIf(String::valid)
}
/** Structured request boundary: cancellation always propagates. */
suspend fun <T> safeRequest(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: HttpException) {
    val message = runCatching {
        failure.response()?.errorBody()?.use { body ->
            val source = body.source()
            source.request(8193)
            if (source.buffer.size > 8192) null else backendMessage(source.buffer.readUtf8())
        }
    }.getOrNull()
    ApiResult.Failure(ApiError(message))
} catch (_: Exception) {
    ApiResult.Failure(ApiError())
}
