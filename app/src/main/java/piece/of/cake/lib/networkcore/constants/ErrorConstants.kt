package piece.of.cake.lib.networkcore.constants

/**
 * Error codes used in NetworkResult.Error
 *
 * These are developer-facing codes, not user-facing messages.
 * Apps should map these codes to localized user messages.
 */
object ErrorCode {
    /** HTTP error prefix (e.g., HTTP_404, HTTP_500) */
    const val HTTP_PREFIX = "HTTP_"

    /** Response body is null */
    const val EMPTY_BODY = "EMPTY_BODY"

    /** Server returned success but data field is null */
    const val NULL_DATA = "NULL_DATA"

    /** Unknown error from server */
    const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
}

/**
 * Default error messages (English)
 *
 * These are fallback messages for developers.
 * Apps should provide localized messages to users.
 */
object ErrorMessage {
    const val RESPONSE_BODY_NULL = "Response body is null"
    const val SUCCESS_BUT_DATA_NULL = "Success but data is null"
}
