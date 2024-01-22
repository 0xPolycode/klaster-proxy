package polycode.model.result

import polycode.util.Balance
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ChainlinkChainSelector
import polycode.util.ContractAddress
import polycode.util.FunctionSignature
import polycode.util.TransactionHash
import polycode.util.UtcDateTime
import polycode.util.WalletAddress

sealed interface CcipTxInfo

data class CcipWalletCreateInfo(
    val chainId: ChainId,
    val txHash: TransactionHash,
    val fnSignature: FunctionSignature,
    val blockNumber: BlockNumber,
    val controllerWallet: WalletAddress,
    val txDate: UtcDateTime,
    val destChains: Set<ChainlinkChainSelector>,
    val salt: String
) : CcipTxInfo

data class CcipErc20TransferInfo(
    val chainId: ChainId,
    val txHash: TransactionHash,
    val fnSignature: FunctionSignature,
    val blockNumber: BlockNumber,
    val controllerWallet: WalletAddress,
    val txValue: Balance,
    val txDate: UtcDateTime,
    val destChains: Set<ChainlinkChainSelector>,
    val salt: String,
    val tokenAddress: ContractAddress,
    val tokenReceiver: WalletAddress,
    val tokenAmount: Balance
) : CcipTxInfo

data class CcipNativeTransferTransferInfo(
    val chainId: ChainId,
    val txHash: TransactionHash,
    val fnSignature: FunctionSignature,
    val blockNumber: BlockNumber,
    val controllerWallet: WalletAddress,
    val txValue: Balance,
    val txDate: UtcDateTime,
    val destChains: Set<ChainlinkChainSelector>,
    val salt: String
) : CcipTxInfo

data class CcipBasicInfo(
    val chainId: ChainId,
    val txHash: TransactionHash,
    val fnSignature: FunctionSignature,
    val blockNumber: BlockNumber,
    val controllerWallet: WalletAddress,
    val txValue: Balance,
    val txDate: UtcDateTime
) : CcipTxInfo
