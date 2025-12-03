package piece.of.cake.lib.networkcore.model

/**
 * Abstracted interface for server response formats.
 *
 * Designed with method-based approach to support various server response formats.
 * Implement this interface in your server response DTO for each project.
 *
 * @param T Response data type
 */
interface BaseResponse<T> {

    /**
     * Determines if the server response is successful.
     *
     * Examples:
     * - res == true
     * - code == 200
     * - status == "OK"
     */
    fun isSuccess(): Boolean

    /**
     * Returns the response data.
     * Returns actual data on success, null on failure.
     */
    fun getData(): T?

    /**
     * Returns the error code on failure.
     * Recommended to return null on success.
     */
    fun getErrorCode(): String?

    /**
     * Returns the error or response message.
     */
    fun getMessage(): String?
}
