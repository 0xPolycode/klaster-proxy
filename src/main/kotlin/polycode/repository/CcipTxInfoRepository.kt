package polycode.repository

import polycode.model.result.CcipTxInfo
import polycode.util.ChainId
import polycode.util.TransactionHash

interface CcipTxInfoRepository {
    fun getByTxHashes(txHashes: Set<TransactionHash>): List<CcipTxInfo>
    fun getAllTxHashesWithoutTxInfo(chainId: ChainId): List<TransactionHash>
    fun insert(txInfo: CcipTxInfo)
}
