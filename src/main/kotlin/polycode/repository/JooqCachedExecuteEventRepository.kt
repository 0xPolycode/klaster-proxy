package polycode.repository

import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import polycode.generated.jooq.tables.records.CachedExecuteEventRecord
import polycode.model.result.ExecuteEvent

@Repository
class JooqCachedExecuteEventRepository(private val dslContext: DSLContext) : CachedExecuteEventRepository {

    companion object : KLogging()

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
