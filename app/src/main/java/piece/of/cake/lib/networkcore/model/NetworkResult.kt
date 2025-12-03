package piece.of.cake.lib.networkcore.model

import java.io.IOException

/**
 * Sealed class that wraps API call results in a type-safe manner.
 *
 * @param T Data type returned on success
 */
sealed class NetworkResult<out T> {

    /**
     * Loading state
     */
    data object Loading : NetworkResult<Nothing>()

    /**
     * Success with data
     */
    data class Success<T>(val data: T) : NetworkResult<T>()

    /**
     * Success without data - for DELETE, POST, etc. with no response body
     */
    data class Empty(val message: String? = null) : NetworkResult<Nothing>()

    /**
     * Server or business logic error
     *
     * @param httpCode HTTP status code (nullable - null for business errors)
     * @param code Error code (server-defined or internal)
     * @param message Error message
     */
    data class Error(
        val httpCode: Int? = null,
        val code: String,
        val message: String?
    ) : NetworkResult<Nothing>()

    /**
     * Network connection error (timeout, connection failure, etc.)
     */
    data class NetworkError(val exception: IOException) : NetworkResult<Nothing>()

    /**
     * Unexpected exception
     */
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>()

    /**
     * Returns true if the result is successful
     */
    val isSuccess: Boolean
        get() = this is Success || this is Empty

    /**
     * Returns true if the result is a failure
     */
    val isFailure: Boolean
        get() = !isSuccess

    /**
     * Extracts Success data or returns null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Extracts Success data or returns default value
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    /**
     * Extracts Success data or throws exception
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Empty -> throw IllegalStateException("Result is Empty, no data available")
        is Error -> throw IllegalStateException("Result is Error: [$code] $message")
        is NetworkError -> throw exception
        is Exception -> throw throwable
        is Loading -> throw IllegalStateException("Result is still Loading")
    }

    /**
     * Callback for Success state
     */
    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Callback for Empty state
     */
    inline fun onEmpty(action: (String?) -> Unit): NetworkResult<T> {
        if (this is Empty) action(message)
        return this
    }

    /**
     * Callback for Error state
     */
    inline fun onError(action: (Error) -> Unit): NetworkResult<T> {
        if (this is Error) action(this)
        return this
    }

    /**
     * Callback for NetworkError state
     */
    inline fun onNetworkError(action: (IOException) -> Unit): NetworkResult<T> {
        if (this is NetworkError) action(exception)
        return this
    }

    /**
     * Callback for Exception state
     */
    inline fun onException(action: (Throwable) -> Unit): NetworkResult<T> {
        if (this is Exception) action(throwable)
        return this
    }

    /**
     * Callback for Loading state
     */
    inline fun onLoading(action: () -> Unit): NetworkResult<T> {
        if (this is Loading) action()
        return this
    }

    /**
     * Callback for any failure state (Error, NetworkError, Exception)
     */
    inline fun onFailure(action: (NetworkResult<Nothing>) -> Unit): NetworkResult<T> {
        if (isFailure) {
            @Suppress("UNCHECKED_CAST")
            action(this as NetworkResult<Nothing>)
        }
        return this
    }
}
