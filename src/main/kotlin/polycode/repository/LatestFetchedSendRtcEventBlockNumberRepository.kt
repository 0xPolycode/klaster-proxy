package polycode.repository

import polycode.util.BlockNumber
import polycode.util.ChainId

interface LatestFetchedSendRtcEventBlockNumberRepository {
    fun get(chainId: ChainId): BlockNumber?
    fun upsert(chainId: ChainId, blockNumber: BlockNumber)
}
