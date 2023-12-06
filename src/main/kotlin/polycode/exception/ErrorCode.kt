package polycode.exception

import polycode.util.annotation.Description

enum class ErrorCode {

    @Description("Indicates that the requested chainId is not supported")
    UNSUPPORTED_CHAIN_ID,

    @Description("Indicates that fetching current block number has failed on RPC")
    CANNOT_FETCH_BLOCK_NUMBER,

    @Description("Indicates that filter logs cannot be fetched from RPC")
    CANNOT_FETCH_FILTER_LOGS,

    @Description("Indicates that filter log is missing transaction hash")
    MISSING_LOG_TX_HASH,

    @Description("Indicates that call to Klaster API has failed")
    KLASTER_API_CALL_FAILED,

    @Description("Indicates that request body is not valid")
    INVALID_REQUEST_BODY,

    @Description("Indicates that query parameter is not valid")
    INVALID_QUERY_PARAM
}
