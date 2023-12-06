package polycode.repository

import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import polycode.generated.jooq.tables.CachedSendRtcEventTable
import polycode.generated.jooq.tables.records.CachedSendRtcEventRecord
import polycode.model.result.SendRtcEvent
import polycode.util.TransactionHash
import polycode.util.WalletAddress

@Repository
class JooqCachedSendRtcEventRepository(private val dslContext: DSLContext) : CachedSendRtcEventRepository {

    companion object : KLogging()

    override fun getAllTxHashes(callerAddress: WalletAddress): Set<TransactionHash> {
        logger.debug { "Get all tx hashes for callerAddress: $callerAddress" }

        return dslContext.selectDistinct(CachedSendRtcEventTable.TX_HASH)
            .from(CachedSendRtcEventTable)
            .where(CachedSendRtcEventTable.CALLER_ADDRESS.eq(callerAddress.rawValue))
            .fetchSet { TransactionHash(it.get(CachedSendRtcEventTable.TX_HASH)) }
    }

    override fun insertAll(events: List<SendRtcEvent>) {
        logger.info { "Insert SendRtc events: $events" }

        dslContext.batchInsert(
            events.map { event ->
                CachedSendRtcEventRecord(
                    chainId = event.chainId.value,
                    txHash = event.txHash.value,
                    blockNumber = event.blockNumber.value.longValueExact(),
                    messageId = event.messageId,
                    callerAddress = event.callerAddress.rawValue
                )
            }
        ).execute()
    }
}
