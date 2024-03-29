package polycode.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import polycode.generated.jooq.enums.CcipTxType
import polycode.generated.jooq.tables.CachedExecuteEventTable
import polycode.generated.jooq.tables.CachedSendRtcEventTable
import polycode.generated.jooq.tables.CcipTxInfoTable
import polycode.generated.jooq.tables.records.CcipTxInfoRecord
import polycode.model.result.CcipBasicInfo
import polycode.model.result.CcipErc20TransferInfo
import polycode.model.result.CcipNativeTransferTransferInfo
import polycode.model.result.CcipTxInfo
import polycode.model.result.CcipWalletCreateInfo
import polycode.util.Balance
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ChainlinkChainSelector
import polycode.util.ContractAddress
import polycode.util.FunctionSignature
import polycode.util.TransactionHash
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.math.BigInteger

@Repository
@Suppress("LongMethod")
class JooqCcipTxInfoRepository(private val dslContext: DSLContext) : CcipTxInfoRepository {

    companion object : KLogging() {
        private const val TRANSMIT_FN_SIGNATURE = "0xb1dc65a4"
    }

    override fun getByTxHashes(txHashes: Set<TransactionHash>): List<CcipTxInfo> {
        logger.debug { "Get txInfo by txHashes: $txHashes" }

        return dslContext.selectFrom(CcipTxInfoTable)
            .where(
                DSL.and(
                    CcipTxInfoTable.TX_HASH.`in`(txHashes.map { it.value }),
                    CcipTxInfoTable.FN_SIGNATURE.ne(TRANSMIT_FN_SIGNATURE)
                )
            )
            .orderBy(CcipTxInfoTable.TX_DATE.desc())
            .fetch {
                when (it.txType) {
                    CcipTxType.OTHER ->
                        CcipBasicInfo(
                            chainId = ChainId(it.chainId),
                            txHash = TransactionHash(it.txHash),
                            fnSignature = FunctionSignature(it.fnSignature),
                            blockNumber = BlockNumber(it.blockNumber.toBigInteger()),
                            controllerWallet = WalletAddress(it.controllerWallet),
                            txValue = Balance(it.txValue),
                            txDate = UtcDateTime(it.txDate)
                        )

                    CcipTxType.WALLET_CREATE ->
                        CcipWalletCreateInfo(
                            chainId = ChainId(it.chainId),
                            txHash = TransactionHash(it.txHash),
                            fnSignature = FunctionSignature(it.fnSignature),
                            blockNumber = BlockNumber(it.blockNumber.toBigInteger()),
                            controllerWallet = WalletAddress(it.controllerWallet),
                            txDate = UtcDateTime(it.txDate),
                            destChains = it.destChains!!.map { v -> ChainlinkChainSelector(v.toBigInteger()) }.toSet(),
                            salt = it.salt!!
                        )

                    CcipTxType.ERC20_TRANSFER ->
                        CcipErc20TransferInfo(
                            chainId = ChainId(it.chainId),
                            txHash = TransactionHash(it.txHash),
                            fnSignature = FunctionSignature(it.fnSignature),
                            blockNumber = BlockNumber(it.blockNumber.toBigInteger()),
                            controllerWallet = WalletAddress(it.controllerWallet),
                            txValue = Balance(it.txValue),
                            txDate = UtcDateTime(it.txDate),
                            destChains = it.destChains!!.map { v -> ChainlinkChainSelector(v.toBigInteger()) }.toSet(),
                            salt = it.salt!!,
                            tokenAddress = ContractAddress(it.tokenAddress!!),
                            tokenReceiver = WalletAddress(it.tokenReceiver!!),
                            tokenAmount = Balance(it.tokenAmount!!)
                        )

                    CcipTxType.NATIVE_TRANSFER ->
                        CcipNativeTransferTransferInfo(
                            chainId = ChainId(it.chainId),
                            txHash = TransactionHash(it.txHash),
                            fnSignature = FunctionSignature(it.fnSignature),
                            blockNumber = BlockNumber(it.blockNumber.toBigInteger()),
                            controllerWallet = WalletAddress(it.controllerWallet),
                            txValue = Balance(it.txValue),
                            txDate = UtcDateTime(it.txDate),
                            destChains = it.destChains!!.map { v -> ChainlinkChainSelector(v.toBigInteger()) }.toSet(),
                            salt = it.salt!!
                        )
                }
            }
    }

    override fun getAllTxHashesWithoutTxInfo(chainId: ChainId): Set<TransactionHash> {
        logger.debug { "Get all txHashes without txInfo, chainId: $chainId" }

        return dslContext.selectDistinct(CachedSendRtcEventTable.TX_HASH)
            .from(CachedSendRtcEventTable)
            .where(CachedSendRtcEventTable.CHAIN_ID.eq(chainId.value))
            .union(
                DSL.selectDistinct(CachedExecuteEventTable.TX_HASH)
                    .from(CachedExecuteEventTable)
                    .where(CachedExecuteEventTable.CHAIN_ID.eq(chainId.value))
            )
            .except(
                DSL.selectDistinct(CcipTxInfoTable.TX_HASH)
                    .from(CcipTxInfoTable)
                    .where(CcipTxInfoTable.CHAIN_ID.eq(chainId.value))
            )
            .fetchSet { TransactionHash(it.value1()) }
    }

    override fun insert(txInfo: CcipTxInfo) {
        logger.info { "Insert txInfo: $txInfo" }

        val record = when (txInfo) {
            is CcipBasicInfo ->
                CcipTxInfoRecord(
                    chainId = txInfo.chainId.value,
                    txHash = txInfo.txHash.value,
                    txType = CcipTxType.OTHER,
                    fnSignature = txInfo.fnSignature.value,
                    blockNumber = txInfo.blockNumber.value.longValueExact(),
                    controllerWallet = txInfo.controllerWallet.rawValue,
                    txValue = txInfo.txValue.rawValue,
                    txDate = txInfo.txDate.value,
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
                    fnSignature = txInfo.fnSignature.value,
                    blockNumber = txInfo.blockNumber.value.longValueExact(),
                    controllerWallet = txInfo.controllerWallet.rawValue,
                    txValue = BigInteger.ZERO,
                    txDate = txInfo.txDate.value,
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
                    fnSignature = txInfo.fnSignature.value,
                    blockNumber = txInfo.blockNumber.value.longValueExact(),
                    controllerWallet = txInfo.controllerWallet.rawValue,
                    txValue = txInfo.txValue.rawValue,
                    txDate = txInfo.txDate.value,
                    destChains = txInfo.destChains.map { it.value.toBigDecimal() }.toTypedArray(),
                    salt = txInfo.salt,
                    tokenAddress = txInfo.tokenAddress.rawValue,
                    tokenReceiver = txInfo.tokenReceiver.rawValue,
                    tokenAmount = txInfo.tokenAmount.rawValue
                )

            is CcipNativeTransferTransferInfo ->
                CcipTxInfoRecord(
                    chainId = txInfo.chainId.value,
                    txHash = txInfo.txHash.value,
                    txType = CcipTxType.NATIVE_TRANSFER,
                    fnSignature = txInfo.fnSignature.value,
                    blockNumber = txInfo.blockNumber.value.longValueExact(),
                    controllerWallet = txInfo.controllerWallet.rawValue,
                    txValue = txInfo.txValue.rawValue,
                    txDate = txInfo.txDate.value,
                    destChains = txInfo.destChains.map { it.value.toBigDecimal() }.toTypedArray(),
                    salt = txInfo.salt,
                    tokenAddress = null,
                    tokenReceiver = null,
                    tokenAmount = null
                )
        }

        dslContext.executeInsert(record)
    }
}
