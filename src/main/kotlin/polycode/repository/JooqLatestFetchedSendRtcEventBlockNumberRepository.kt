package polycode.repository

import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import polycode.generated.jooq.tables.LatestFetchedSendRtcEventBlockNumberTable
import polycode.generated.jooq.tables.records.LatestFetchedSendRtcEventBlockNumberRecord
import polycode.util.BlockNumber
import polycode.util.ChainId
import java.math.BigInteger

@Repository
class JooqLatestFetchedSendRtcEventBlockNumberRepository(private val dslContext: DSLContext) :
    LatestFetchedSendRtcEventBlockNumberRepository {

    companion object : KLogging()

    override fun get(chainId: ChainId): BlockNumber? {
        logger.info { "Get latest block number for chainId: $chainId" }

        return dslContext.select(LatestFetchedSendRtcEventBlockNumberTable.BLOCK_NUMBER)
            .from(LatestFetchedSendRtcEventBlockNumberTable)
            .where(LatestFetchedSendRtcEventBlockNumberTable.CHAIN_ID.eq(chainId.value))
            .fetchOne(LatestFetchedSendRtcEventBlockNumberTable.BLOCK_NUMBER)
            ?.let(BigInteger::valueOf)
            ?.let(::BlockNumber)
    }

    override fun upsert(chainId: ChainId, blockNumber: BlockNumber) {
        logger.info { "Set latest block number for chainId: $chainId, blockNumber: $blockNumber" }

        dslContext.insertInto(LatestFetchedSendRtcEventBlockNumberTable)
            .set(
                LatestFetchedSendRtcEventBlockNumberRecord(
                    chainId = chainId.value,
                    blockNumber = blockNumber.value.longValueExact()
                )
            )
            .onConflict(LatestFetchedSendRtcEventBlockNumberTable.CHAIN_ID)
            .doUpdate()
            .set(LatestFetchedSendRtcEventBlockNumberTable.BLOCK_NUMBER, blockNumber.value.longValueExact())
            .execute()
    }
}
