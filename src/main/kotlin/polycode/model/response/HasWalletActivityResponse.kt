package polycode.model.response

sealed interface HasWalletActivityResponse

data class HasWalletActivitySuccessResponse(
    val data: HasWalletActivityData
) : HasWalletActivityResponse

data class HasWalletActivityErrorResponse(
    val error: HasWalletActivityErrorData,
    val data: HasWalletActivityData
) : HasWalletActivityResponse

data class HasWalletActivityData(
    val result: Boolean
)

data class HasWalletActivityErrorData(
    val code: String,
    val message: String
)
