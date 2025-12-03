package piece.of.cake.lib.networkcore

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for BaseResponse implementation
 */
class BaseResponseTest {

    @Test
    fun `TestResponse isSuccess returns true when code is 200`() {
        val response = TestResponse(
            code = 200,
            msg = "OK",
            result = "data"
        )

        assertThat(response.isSuccess()).isTrue()
        assertThat(response.getErrorCode()).isNull()
    }

    @Test
    fun `TestResponse isSuccess returns false when code is not 200`() {
        val response = TestResponse(
            code = 400,
            msg = "Bad Request",
            result = null as String?
        )

        assertThat(response.isSuccess()).isFalse()
        assertThat(response.getErrorCode()).isEqualTo("400")
    }

    @Test
    fun `TestResponse getData returns data`() {
        val user = TestUser(id = 1, name = "John")
        val response = TestResponse(
            code = 200,
            msg = "OK",
            result = user
        )

        assertThat(response.getData()).isEqualTo(user)
    }

    @Test
    fun `TestResponse getMessage returns message`() {
        val response = TestResponse(
            code = 500,
            msg = "Internal Server Error",
            result = null as String?
        )

        assertThat(response.getMessage()).isEqualTo("Internal Server Error")
    }

    @Test
    fun `different BaseResponse implementations work correctly`() {
        // Simulating different server format
        data class LegacyResponse<T>(
            val res: Boolean,
            val msg: String?,
            @get:JvmName("getResponseResult")
            val result: T?
        ) : piece.of.cake.lib.networkcore.model.BaseResponse<T> {
            override fun isSuccess(): Boolean = res
            override fun getData(): T? = result
            override fun getErrorCode(): String? = if (!res) "LEGACY_ERROR" else null
            override fun getMessage(): String? = msg
        }

        val legacy = LegacyResponse(
            res = true,
            msg = "Success",
            result = "data"
        )

        assertThat(legacy.isSuccess()).isTrue()
        assertThat(legacy.getData()).isEqualTo("data")
        assertThat(legacy.getMessage()).isEqualTo("Success")
    }
}
