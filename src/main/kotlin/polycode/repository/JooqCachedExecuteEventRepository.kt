package polycode.repository

import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import polycode.generated.jooq.tables.CachedExecuteEventTable
import polycode.generated.jooq.tables.records.CachedExecuteEventRecord
import polycode.model.result.ExecuteEvent
import polycode.util.TransactionHash
import polycode.util.WalletAddress

@Repository
class JooqCachedExecuteEventRepository(private val dslContext: DSLContext) : CachedExecuteEventRepository {

    companion object : KLogging()

    override fun getAllTxHashes(callerAddress: WalletAddress): Set<TransactionHash> {
        logger.debug { "Get all tx hashes for callerAddress: $callerAddress" }

        return dslContext.selectDistinct(CachedExecuteEventTable.TX_HASH)
            .from(CachedExecuteEventTable)
            .where(CachedExecuteEventTable.CALLER_ADDRESS.eq(callerAddress.rawValue))
            .fetchSet { TransactionHash(it.get(CachedExecuteEventTable.TX_HASH)) }
    }

    override fun insertAll(events: List<ExecuteEvent>) {
        logger.info { "Insert Execute events: $events" }

        dslContext.batchInsert(
            events.map { event ->
                CachedExecuteEventRecord(
                    chainId = event.chainId.value,
                    txHash = event.txHash.value,
                    blockNumber = event.blockNumber.value.longValueExact(),
                    callerAddress = event.callerAddress.rawValue
                )
            }
        ).execute()
    }
}
