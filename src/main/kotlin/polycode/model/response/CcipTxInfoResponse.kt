package polycode.model.response

import polycode.generated.jooq.enums.CcipTxType
import polycode.model.result.CcipBasicInfo
import polycode.model.result.CcipErc20TransferInfo
import polycode.model.result.CcipNativeTransferTransferInfo
import polycode.model.result.CcipTxInfo
import polycode.model.result.CcipWalletCreateInfo
import java.math.BigInteger

data class CcipTxInfoResponse(
    val txType: CcipTxType,
    val chainId: Long,
    val txHash: String,
    val blockNumber: BigInteger,
    val controllerWallet: String,
    val txDate: String,
    val txValue: BigInteger,
    val chainlinkChainSelectors: List<BigInteger>?,
    val salt: String?,
    val tokenAddress: String?,
    val tokenReceiver: String?,
    val tokenAmount: BigInteger?
) {
    companion object {
        fun fromTxInfo(txInfo: CcipTxInfo): CcipTxInfoResponse {
            return when (txInfo) {
                is CcipBasicInfo ->
                    CcipTxInfoResponse(
                        txType = CcipTxType.OTHER,
                        chainId = txInfo.chainId.value,
                        txHash = txInfo.txHash.value,
                        blockNumber = txInfo.blockNumber.value,
                        controllerWallet = txInfo.controllerWallet.rawValue,
                        txDate = txInfo.txDate.iso,
                        txValue = txInfo.txValue.rawValue,
                        chainlinkChainSelectors = null,
                        salt = null,
                        tokenAddress = null,
                        tokenReceiver = null,
                        tokenAmount = null
                    )

                is CcipWalletCreateInfo ->
                    CcipTxInfoResponse(
                        txType = CcipTxType.WALLET_CREATE,
                        chainId = txInfo.chainId.value,
                        txHash = txInfo.txHash.value,
                        blockNumber = txInfo.blockNumber.value,
                        controllerWallet = txInfo.controllerWallet.rawValue,
                        txDate = txInfo.txDate.iso,
                        txValue = BigInteger.ZERO,
                        chainlinkChainSelectors = txInfo.destChains.map { it.value },
                        salt = txInfo.salt,
                        tokenAddress = null,
                        tokenReceiver = null,
                        tokenAmount = null
                    )

                is CcipErc20TransferInfo ->
                    CcipTxInfoResponse(
                        txType = CcipTxType.ERC20_TRANSFER,
                        chainId = txInfo.chainId.value,
                        txHash = txInfo.txHash.value,
                        blockNumber = txInfo.blockNumber.value,
                        controllerWallet = txInfo.controllerWallet.rawValue,
                        txDate = txInfo.txDate.iso,
                        txValue = txInfo.txValue.rawValue,
                        chainlinkChainSelectors = txInfo.destChains.map { it.value },
                        salt = txInfo.salt,
                        tokenAddress = txInfo.tokenAddress.rawValue,
                        tokenReceiver = txInfo.tokenReceiver.rawValue,
                        tokenAmount = txInfo.tokenAmount.rawValue
                    )

                is CcipNativeTransferTransferInfo ->
                    CcipTxInfoResponse(
                        txType = CcipTxType.NATIVE_TRANSFER,
                        chainId = txInfo.chainId.value,
                        txHash = txInfo.txHash.value,
                        blockNumber = txInfo.blockNumber.value,
                        controllerWallet = txInfo.controllerWallet.rawValue,
                        txDate = txInfo.txDate.iso,
                        txValue = txInfo.txValue.rawValue,
                        chainlinkChainSelectors = txInfo.destChains.map { it.value },
                        salt = txInfo.salt,
                        tokenAddress = null,
                        tokenReceiver = null,
                        tokenAmount = null
                    )
            }
        }
    }
}
