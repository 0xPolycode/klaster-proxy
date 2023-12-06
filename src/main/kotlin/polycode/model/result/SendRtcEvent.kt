package polycode.model.result

import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.TransactionHash
import polycode.util.WalletAddress

data class SendRtcEvent(
    val chainId: ChainId,
    val txHash: TransactionHash,
    val blockNumber: BlockNumber,
    val messageId: String,
    val callerAddress: WalletAddress
)
