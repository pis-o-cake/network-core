package piece.of.cake.lib.networkcore

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import piece.of.cake.lib.networkcore.constants.ErrorCode
import piece.of.cake.lib.networkcore.model.NetworkResult
import piece.of.cake.lib.networkcore.util.safeApiCall
import piece.of.cake.lib.networkcore.util.safeApiCallEmpty
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Integration tests for safeApiCall with MockWebServer
 */
class SafeApiCallTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: TestApi

    interface TestApi {
        @GET("users/{id}")
        suspend fun getUser(@Path("id") id: Long): Response<TestResponse<TestUser>>

        @DELETE("users/{id}")
        suspend fun deleteUser(@Path("id") id: Long): Response<TestResponse<Unit>>
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TestApi::class.java)
    }

    @After
    fun tearDown() {
        try {
            mockWebServer.shutdown()
        } catch (e: Exception) {
            // Ignore shutdown errors
        }
    }

    // region Success Cases

    @Test
    fun `safeApiCall returns Success when API returns valid data`() = runTest {
        val json = """
            {
                "code": 200,
                "message": "OK",
                "data": {
                    "id": 1,
                    "name": "John"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json")
        )

        val result = safeApiCall { api.getUser(1) }

        assertThat(result).isInstanceOf(NetworkResult.Success::class.java)
        val success = result as NetworkResult.Success
        assertThat(success.data.id).isEqualTo(1)
        assertThat(success.data.name).isEqualTo("John")
    }

    @Test
    fun `safeApiCallEmpty returns Empty on success without data`() = runTest {
        val json = """
            {
                "code": 200,
                "message": "Deleted successfully",
                "data": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json")
        )

        val result = safeApiCallEmpty { api.deleteUser(1) }

        assertThat(result).isInstanceOf(NetworkResult.Empty::class.java)
        val empty = result as NetworkResult.Empty
        assertThat(empty.message).isEqualTo("Deleted successfully")
    }

    // endregion

    // region HTTP Error Cases

    @Test
    fun `safeApiCall returns Error on HTTP 404`() = runTest {
        val httpCode = 404

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(httpCode)
                .setBody("Not Found")
        )

        val result = safeApiCall { api.getUser(999) }

        assertThat(result).isInstanceOf(NetworkResult.Error::class.java)
        val error = result as NetworkResult.Error
        assertThat(error.httpCode).isEqualTo(httpCode)
        assertThat(error.code).isEqualTo("${ErrorCode.HTTP_PREFIX}$httpCode")
    }

    @Test
    fun `safeApiCall returns Error on HTTP 500`() = runTest {
        val httpCode = 500

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(httpCode)
                .setBody("Internal Server Error")
        )

        val result = safeApiCall { api.getUser(1) }

        assertThat(result).isInstanceOf(NetworkResult.Error::class.java)
        val error = result as NetworkResult.Error
        assertThat(error.httpCode).isEqualTo(httpCode)
        assertThat(error.code).isEqualTo("${ErrorCode.HTTP_PREFIX}$httpCode")
    }

    // endregion

    // region Business Error Cases

    @Test
    fun `safeApiCall returns Error when server returns failure response`() = runTest {
        val json = """
            {
                "code": 401,
                "message": "Unauthorized",
                "data": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json")
        )

        val result = safeApiCall { api.getUser(1) }

        assertThat(result).isInstanceOf(NetworkResult.Error::class.java)
        val error = result as NetworkResult.Error
        assertThat(error.code).isEqualTo("401")
        assertThat(error.message).isEqualTo("Unauthorized")
    }

    @Test
    fun `safeApiCall returns Error when data is null on success`() = runTest {
        val json = """
            {
                "code": 200,
                "message": "OK",
                "data": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json")
        )

        val result = safeApiCall { api.getUser(1) }

        assertThat(result).isInstanceOf(NetworkResult.Error::class.java)
        val error = result as NetworkResult.Error
        assertThat(error.code).isEqualTo(ErrorCode.NULL_DATA)
    }

    // endregion

    // region Network Error Cases

    @Test
    fun `safeApiCall returns NetworkError on connection failure`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        val result = safeApiCall { api.getUser(1) }

        assertThat(result).isInstanceOf(NetworkResult.NetworkError::class.java)
    }

    @Test
    fun `safeApiCall returns NetworkError on timeout`() = runTest {
        val timeoutMillis = 100L
        val delaySeconds = 10L

        mockWebServer.enqueue(
            MockResponse()
                .setBodyDelay(delaySeconds, TimeUnit.SECONDS)
                .setBody("{}")
        )

        // Create API with short timeout
        val shortTimeoutApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                okhttp3.OkHttpClient.Builder()
                    .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                    .build()
            )
            .build()
            .create(TestApi::class.java)

        val result = safeApiCall { shortTimeoutApi.getUser(1) }

        assertThat(result).isInstanceOf(NetworkResult.NetworkError::class.java)
    }

    // endregion

    // region Empty Body Cases

    @Test
    fun `safeApiCall returns NetworkError when body is empty`() = runTest {
        // Gson throws EOFException for empty body, which is IOException
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )

        val result = safeApiCall { api.getUser(1) }

        // Empty body causes Gson to throw EOFException (IOException subclass)
        assertThat(result).isInstanceOf(NetworkResult.NetworkError::class.java)
    }

    // endregion
}
