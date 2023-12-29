package polycode.model.result

import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ChainlinkChainSelector
import polycode.util.ContractAddress
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import java.math.BigInteger

sealed interface CcipTxInfo

data class CcipWalletCreateInfo(
    val chainId: ChainId,
    val txHash: TransactionHash,
    val blockNumber: BlockNumber,
    val controllerWallet: WalletAddress,
    val destChains: Set<ChainlinkChainSelector>,
    val salt: String
) : CcipTxInfo

data class CcipErc20TransferInfo(
    val chainId: ChainId,
    val txHash: TransactionHash,
    val blockNumber: BlockNumber,
    val controllerWallet: WalletAddress,
    val destChains: Set<ChainlinkChainSelector>,
    val salt: String,
    val tokenAddress: ContractAddress,
    val tokenReceiver: WalletAddress,
    val tokenAmount: BigInteger
) : CcipTxInfo

data class CcipBasicInfo(
    val chainId: ChainId,
    val txHash: TransactionHash,
    val blockNumber: BlockNumber,
    val controllerWallet: WalletAddress
) : CcipTxInfo
