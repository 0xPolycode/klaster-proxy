package polycode.repository

import polycode.model.result.ExecuteEvent
import polycode.util.TransactionHash
import polycode.util.WalletAddress

interface CachedExecuteEventRepository {
    fun getAllTxHashes(callerAddress: WalletAddress): Set<TransactionHash>
    fun insertAll(events: List<ExecuteEvent>)
}
