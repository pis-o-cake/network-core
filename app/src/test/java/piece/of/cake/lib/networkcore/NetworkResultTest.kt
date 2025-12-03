package piece.of.cake.lib.networkcore

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import piece.of.cake.lib.networkcore.model.NetworkResult
import java.io.IOException

/**
 * Unit tests for NetworkResult
 */
class NetworkResultTest {

    // region isSuccess / isFailure

    @Test
    fun `Success isSuccess returns true`() {
        val result: NetworkResult<String> = NetworkResult.Success("data")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.isFailure).isFalse()
    }

    @Test
    fun `Empty isSuccess returns true`() {
        val result: NetworkResult<String> = NetworkResult.Empty("done")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.isFailure).isFalse()
    }

    @Test
    fun `Error isFailure returns true`() {
        val result: NetworkResult<String> = NetworkResult.Error(
            httpCode = 400,
            code = "BAD_REQUEST",
            message = "Invalid input"
        )

        assertThat(result.isSuccess).isFalse()
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `NetworkError isFailure returns true`() {
        val result: NetworkResult<String> = NetworkResult.NetworkError(IOException("timeout"))

        assertThat(result.isSuccess).isFalse()
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `Exception isFailure returns true`() {
        val result: NetworkResult<String> = NetworkResult.Exception(RuntimeException("unexpected"))

        assertThat(result.isSuccess).isFalse()
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `Loading isFailure returns true`() {
        val result: NetworkResult<String> = NetworkResult.Loading

        assertThat(result.isSuccess).isFalse()
        assertThat(result.isFailure).isTrue()
    }

    // endregion

    // region getOrNull

    @Test
    fun `getOrNull returns data on Success`() {
        val result: NetworkResult<String> = NetworkResult.Success("hello")

        assertThat(result.getOrNull()).isEqualTo("hello")
    }

    @Test
    fun `getOrNull returns null on Error`() {
        val result: NetworkResult<String> = NetworkResult.Error(
            code = "ERROR",
            message = "error"
        )

        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `getOrNull returns null on Empty`() {
        val result: NetworkResult<String> = NetworkResult.Empty()

        assertThat(result.getOrNull()).isNull()
    }

    // endregion

    // region getOrDefault

    @Test
    fun `getOrDefault returns data on Success`() {
        val result: NetworkResult<String> = NetworkResult.Success("actual")

        assertThat(result.getOrDefault("default")).isEqualTo("actual")
    }

    @Test
    fun `getOrDefault returns default on Error`() {
        val result: NetworkResult<String> = NetworkResult.Error(
            code = "ERROR",
            message = "error"
        )

        assertThat(result.getOrDefault("default")).isEqualTo("default")
    }

    // endregion

    // region getOrThrow

    @Test
    fun `getOrThrow returns data on Success`() {
        val result: NetworkResult<String> = NetworkResult.Success("data")

        assertThat(result.getOrThrow()).isEqualTo("data")
    }

    @Test(expected = IllegalStateException::class)
    fun `getOrThrow throws on Error`() {
        val result: NetworkResult<String> = NetworkResult.Error(
            code = "ERROR",
            message = "error"
        )

        result.getOrThrow()
    }

    @Test(expected = IllegalStateException::class)
    fun `getOrThrow throws on Empty`() {
        val result: NetworkResult<String> = NetworkResult.Empty()

        result.getOrThrow()
    }

    @Test(expected = IOException::class)
    fun `getOrThrow throws original exception on NetworkError`() {
        val result: NetworkResult<String> = NetworkResult.NetworkError(IOException("network"))

        result.getOrThrow()
    }

    @Test(expected = RuntimeException::class)
    fun `getOrThrow throws original exception on Exception`() {
        val result: NetworkResult<String> = NetworkResult.Exception(RuntimeException("runtime"))

        result.getOrThrow()
    }

    // endregion

    // region Callback Chaining

    @Test
    fun `onSuccess is called for Success`() {
        var called = false
        var receivedData: String? = null

        NetworkResult.Success("test")
            .onSuccess {
                called = true
                receivedData = it
            }

        assertThat(called).isTrue()
        assertThat(receivedData).isEqualTo("test")
    }

    @Test
    fun `onSuccess is not called for Error`() {
        var called = false

        NetworkResult.Error(code = "E", message = "err")
            .onSuccess { called = true }

        assertThat(called).isFalse()
    }

    @Test
    fun `onError is called for Error`() {
        var called = false
        var receivedCode: String? = null

        NetworkResult.Error(httpCode = 500, code = "SERVER_ERROR", message = "Internal")
            .onError {
                called = true
                receivedCode = it.code
            }

        assertThat(called).isTrue()
        assertThat(receivedCode).isEqualTo("SERVER_ERROR")
    }

    @Test
    fun `onEmpty is called for Empty`() {
        var called = false
        var receivedMessage: String? = null

        NetworkResult.Empty("deleted")
            .onEmpty {
                called = true
                receivedMessage = it
            }

        assertThat(called).isTrue()
        assertThat(receivedMessage).isEqualTo("deleted")
    }

    @Test
    fun `onNetworkError is called for NetworkError`() {
        var called = false

        NetworkResult.NetworkError(IOException("timeout"))
            .onNetworkError { called = true }

        assertThat(called).isTrue()
    }

    @Test
    fun `onException is called for Exception`() {
        var called = false

        NetworkResult.Exception(RuntimeException("error"))
            .onException { called = true }

        assertThat(called).isTrue()
    }

    @Test
    fun `onLoading is called for Loading`() {
        var called = false

        NetworkResult.Loading
            .onLoading { called = true }

        assertThat(called).isTrue()
    }

    @Test
    fun `onFailure is called for all failure states`() {
        var errorCalled = false
        var networkErrorCalled = false
        var exceptionCalled = false

        NetworkResult.Error(code = "E", message = "e")
            .onFailure { errorCalled = true }

        NetworkResult.NetworkError(IOException())
            .onFailure { networkErrorCalled = true }

        NetworkResult.Exception(RuntimeException())
            .onFailure { exceptionCalled = true }

        assertThat(errorCalled).isTrue()
        assertThat(networkErrorCalled).isTrue()
        assertThat(exceptionCalled).isTrue()
    }

    @Test
    fun `onFailure is not called for Success`() {
        var called = false

        NetworkResult.Success("data")
            .onFailure { called = true }

        assertThat(called).isFalse()
    }

    @Test
    fun `callback chaining returns same result`() {
        val original = NetworkResult.Success("data")

        val chained = original
            .onSuccess { }
            .onError { }
            .onNetworkError { }

        assertThat(chained).isSameInstanceAs(original)
    }

    // endregion
}
