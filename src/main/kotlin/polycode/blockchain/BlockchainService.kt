package polycode.blockchain

import polycode.blockchain.properties.ChainSpec
import polycode.model.result.CcipTxInfo
import polycode.model.result.SendRtcEvent
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.TransactionHash

interface BlockchainService {

    fun findSendRtcEvents(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        fromBlock: BlockNumber
    ): Pair<List<SendRtcEvent>, BlockNumber>

    fun getCcipTxInfo(
        chainSpec: ChainSpec,
        txHash: TransactionHash
    ): CcipTxInfo
}
