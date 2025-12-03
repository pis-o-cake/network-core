package piece.of.cake.lib.networkcore.util

import piece.of.cake.lib.networkcore.model.BaseResponse
import piece.of.cake.lib.networkcore.model.NetworkResult
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Safely wraps Retrofit API calls.
 *
 * Handles HTTP errors, server business errors, network errors, and exceptions.
 *
 * @param T Response data type
 * @param R Server response type implementing BaseResponse
 * @param call Retrofit suspend function
 * @return NetworkResult<T>
 */
suspend fun <T, R : BaseResponse<T>> safeApiCall(
    call: suspend () -> Response<R>
): NetworkResult<T> {
    return try {
        val response = call()

        handleResponse(response)
    } catch (e: SocketTimeoutException) {
        NetworkResult.NetworkError(e)
    } catch (e: UnknownHostException) {
        NetworkResult.NetworkError(e)
    } catch (e: IOException) {
        NetworkResult.NetworkError(e)
    } catch (e: Exception) {
        NetworkResult.Exception(e)
    }
}

/**
 * For APIs that return success without data (DELETE, etc.)
 *
 * @param R Server response type implementing BaseResponse
 * @param call Retrofit suspend function
 * @return NetworkResult<Unit>
 */
suspend fun <R : BaseResponse<*>> safeApiCallEmpty(
    call: suspend () -> Response<R>
): NetworkResult<Unit> {
    return try {
        val response = call()

        handleEmptyResponse(response)
    } catch (e: SocketTimeoutException) {
        NetworkResult.NetworkError(e)
    } catch (e: UnknownHostException) {
        NetworkResult.NetworkError(e)
    } catch (e: IOException) {
        NetworkResult.NetworkError(e)
    } catch (e: Exception) {
        NetworkResult.Exception(e)
    }
}

/**
 * Handles response with data
 */
private fun <T, R : BaseResponse<T>> handleResponse(response: Response<R>): NetworkResult<T> {
    if (!response.isSuccessful) {
        return NetworkResult.Error(
            httpCode = response.code(),
            code = "HTTP_${response.code()}",
            message = response.message()
        )
    }

    val body = response.body()
        ?: return NetworkResult.Error(
            httpCode = response.code(),
            code = "EMPTY_BODY",
            message = "Response body is null"
        )

    if (!body.isSuccess()) {
        return NetworkResult.Error(
            httpCode = response.code(),
            code = body.getErrorCode() ?: "UNKNOWN_ERROR",
            message = body.getMessage()
        )
    }

    val data = body.getData()
        ?: return NetworkResult.Error(
            httpCode = response.code(),
            code = "NULL_DATA",
            message = body.getMessage() ?: "Success but data is null"
        )

    return NetworkResult.Success(data)
}

/**
 * Handles response without data
 */
private fun <R : BaseResponse<*>> handleEmptyResponse(response: Response<R>): NetworkResult<Unit> {
    if (!response.isSuccessful) {
        return NetworkResult.Error(
            httpCode = response.code(),
            code = "HTTP_${response.code()}",
            message = response.message()
        )
    }

    val body = response.body()
        ?: return NetworkResult.Empty()

    if (!body.isSuccess()) {
        return NetworkResult.Error(
            httpCode = response.code(),
            code = body.getErrorCode() ?: "UNKNOWN_ERROR",
            message = body.getMessage()
        )
    }

    return NetworkResult.Empty(body.getMessage())
}
