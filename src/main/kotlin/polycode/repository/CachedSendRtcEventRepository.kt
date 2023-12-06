package polycode.repository

import polycode.model.result.SendRtcEvent
import polycode.util.TransactionHash
import polycode.util.WalletAddress

interface CachedSendRtcEventRepository {
    fun getAllTxHashes(callerAddress: WalletAddress): Set<TransactionHash>
    fun insertAll(events: List<SendRtcEvent>)
}
