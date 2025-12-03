package piece.of.cake.lib.networkcore

import com.google.gson.annotations.SerializedName
import piece.of.cake.lib.networkcore.model.BaseResponse

/**
 * Test implementation of BaseResponse
 */
data class TestResponse<T>(
    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val msg: String?,

    @SerializedName("data")
    @get:JvmName("getResponseData")
    val result: T?
) : BaseResponse<T> {
    override fun isSuccess(): Boolean = code == 200
    override fun getData(): T? = result
    override fun getErrorCode(): String? = if (code != 200) code.toString() else null
    override fun getMessage(): String? = msg
}

/**
 * Test data class
 */
data class TestUser(
    val id: Long,
    val name: String
)
