package polycode.exception

import org.springframework.http.HttpStatus
import polycode.util.ChainId

abstract class ServiceException(
    val errorCode: ErrorCode,
    val httpStatus: HttpStatus,
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message) {
    companion object {
        private const val serialVersionUID: Long = 8974557457024980481L
    }
}

class InvalidQueryParamException(message: String) : ServiceException(
    errorCode = ErrorCode.INVALID_QUERY_PARAM,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = -6372312381045985449L
    }
}

class UnsupportedChainIdException(chainId: ChainId) : ServiceException(
    errorCode = ErrorCode.UNSUPPORTED_CHAIN_ID,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "ChainId $chainId is not supported"
) {
    companion object {
        private const val serialVersionUID: Long = 1323260130132593220L
    }
}

class RpcException(message: String, errorCode: ErrorCode) : ServiceException(
    errorCode = errorCode,
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = 1271063349902209810L
    }
}

class KlasterApiCallFailedException : ServiceException(
    errorCode = ErrorCode.KLASTER_API_CALL_FAILED,
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
    message = "Klaster API call has failed"
) {
    companion object {
        private const val serialVersionUID: Long = 1157963592732921719L
    }
}
