package polycode.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import polycode.generated.jooq.enums.CcipTxType
import polycode.generated.jooq.tables.CachedSendRtcEventTable
import polycode.generated.jooq.tables.CcipTxInfoTable
import polycode.generated.jooq.tables.records.CcipTxInfoRecord
import polycode.model.result.CcipBasicInfo
import polycode.model.result.CcipErc20TransferInfo
import polycode.model.result.CcipTxInfo
import polycode.model.result.CcipWalletCreateInfo
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ChainlinkChainSelector
import polycode.util.ContractAddress
import polycode.util.TransactionHash
import polycode.util.WalletAddress

@Repository
class JooqCcipTxInfoRepository(private val dslContext: DSLContext) : CcipTxInfoRepository {

    companion object : KLogging()

    override fun getByTxHashes(txHashes: Set<TransactionHash>): List<CcipTxInfo> {
        logger.debug { "Get txInfo by txHashes: $txHashes" }

        return dslContext.selectFrom(CcipTxInfoTable)
            .where(CcipTxInfoTable.TX_HASH.`in`(txHashes.map { it.value }))
            .fetch {
                when (it.txType) {
                    CcipTxType.OTHER ->
                        CcipBasicInfo(
                            chainId = ChainId(it.chainId),
                            txHash = TransactionHash(it.txHash),
                            blockNumber = BlockNumber(it.blockNumber.toBigInteger()),
                            controllerWallet = WalletAddress(it.controllerWallet)
                        )

                    CcipTxType.WALLET_CREATE ->
                        CcipWalletCreateInfo(
                            chainId = ChainId(it.chainId),
                            txHash = TransactionHash(it.txHash),
                            blockNumber = BlockNumber(it.blockNumber.toBigInteger()),
                            controllerWallet = WalletAddress(it.controllerWallet),
                            destChains = it.destChains!!.map { v -> ChainlinkChainSelector(v.toBigInteger()) }.toSet(),
                            salt = it.salt!!
                        )

                    CcipTxType.ERC20_TRANSFER ->
                        CcipErc20TransferInfo(
                            chainId = ChainId(it.chainId),
                            txHash = TransactionHash(it.txHash),
                            blockNumber = BlockNumber(it.blockNumber.toBigInteger()),
                            controllerWallet = WalletAddress(it.controllerWallet),
                            destChains = it.destChains!!.map { v -> ChainlinkChainSelector(v.toBigInteger()) }.toSet(),
                            salt = it.salt!!,
                            tokenAddress = ContractAddress(it.tokenAddress!!),
                            tokenReceiver = WalletAddress(it.tokenReceiver!!),
                            tokenAmount = it.tokenAmount!!
                        )
                }
            }
    }

    override fun getAllTxHashesWithoutTxInfo(chainId: ChainId): List<TransactionHash> {
        logger.debug { "Get all txHashes without txInfo, chainId: $chainId" }

        return dslContext.selectDistinct(CachedSendRtcEventTable.TX_HASH)
            .from(CachedSendRtcEventTable)
            .where(CachedSendRtcEventTable.CHAIN_ID.eq(chainId.value))
            .except(
                DSL.selectDistinct(CcipTxInfoTable.TX_HASH)
                    .from(CcipTxInfoTable)
                    .where(CcipTxInfoTable.CHAIN_ID.eq(chainId.value))
            )
            .fetch { TransactionHash(it.value1()) }
    }

    override fun insert(txInfo: CcipTxInfo) {
        logger.info { "Insert txInfo: $txInfo" }

        val record = when (txInfo) {
            is CcipBasicInfo ->
                CcipTxInfoRecord(
                    chainId = txInfo.chainId.value,
                    txHash = txInfo.txHash.value,
                    txType = CcipTxType.OTHER,
                    blockNumber = txInfo.blockNumber.value.longValueExact(),
                    controllerWallet = txInfo.controllerWallet.rawValue,
                    destChains = null,
                    salt = null,
                    tokenAddress = null,
                    tokenReceiver = null,
                    tokenAmount = null
                )

            is CcipWalletCreateInfo ->
                CcipTxInfoRecord(
                    chainId = txInfo.chainId.value,
                    txHash = txInfo.txHash.value,
                    txType = CcipTxType.WALLET_CREATE,
                    blockNumber = txInfo.blockNumber.value.longValueExact(),
                    controllerWallet = txInfo.controllerWallet.rawValue,
                    destChains = txInfo.destChains.map { it.value.toBigDecimal() }.toTypedArray(),
                    salt = txInfo.salt,
                    tokenAddress = null,
                    tokenReceiver = null,
                    tokenAmount = null
                )

            is CcipErc20TransferInfo ->
                CcipTxInfoRecord(
                    chainId = txInfo.chainId.value,
                    txHash = txInfo.txHash.value,
                    txType = CcipTxType.ERC20_TRANSFER,
                    blockNumber = txInfo.blockNumber.value.longValueExact(),
                    controllerWallet = txInfo.controllerWallet.rawValue,
                    destChains = txInfo.destChains.map { it.value.toBigDecimal() }.toTypedArray(),
                    salt = txInfo.salt,
                    tokenAddress = txInfo.tokenAddress.rawValue,
                    tokenReceiver = txInfo.tokenReceiver.rawValue,
                    tokenAmount = txInfo.tokenAmount
                )
        }

        dslContext.executeInsert(record)
    }
}
