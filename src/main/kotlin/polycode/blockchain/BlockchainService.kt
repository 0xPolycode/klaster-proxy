package polycode.blockchain

import polycode.blockchain.properties.ChainSpec
import polycode.model.result.CcipTxInfo
import polycode.model.result.ExecuteEvent
import polycode.model.result.SendRtcEvent
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.TransactionHash

interface BlockchainService {

    fun findSendRtcAndExecuteEvents(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        fromBlock: BlockNumber
    ): Triple<List<SendRtcEvent>, List<ExecuteEvent>, BlockNumber>

    fun getCcipTxInfo(
        chainSpec: ChainSpec,
        txHash: TransactionHash
    ): CcipTxInfo
}
