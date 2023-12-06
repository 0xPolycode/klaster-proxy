package polycode.blockchain

import mu.KLogging
import org.springframework.stereotype.Service
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.EthLog
import polycode.blockchain.properties.ChainPropertiesHandler
import polycode.blockchain.properties.ChainSpec
import polycode.config.ApplicationProperties
import polycode.exception.ErrorCode
import polycode.exception.RpcException
import polycode.model.result.SendRtcEvent
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.TransactionHash
import polycode.util.WalletAddress

@Service
@Suppress("TooManyFunctions")
class Web3jBlockchainService(applicationProperties: ApplicationProperties) : BlockchainService {

    companion object : KLogging() {
        private const val BATCH_BLOCK_LIMIT = 10_000L
        private const val SEND_RTC_EVENT_TOPIC = "0xaf3488626caa53f1d25b5a3850e43280695530019d11e81740d16209391587da"
    }

    private val chainHandler = ChainPropertiesHandler(applicationProperties)

    override fun findSendRtcEvents(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        fromBlock: BlockNumber
    ): Pair<List<SendRtcEvent>, BlockNumber> {
        logger.debug {
            "Find transaction hashes by event logs, chainSpec: $chainSpec, contractAddress: $contractAddress," +
                " fromBlock: $fromBlock"
        }

        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val lastBlockNumber = blockchainProperties.web3j.ethBlockNumber().sendSafely()?.blockNumber
            ?: throw RpcException("Cannot fetch current block number from RPC", ErrorCode.CANNOT_FETCH_BLOCK_NUMBER)

        logger.debug { "Block range from: ${fromBlock.value} to: $lastBlockNumber" }

        val range = fromBlock.value.longValueExact().rangeTo(lastBlockNumber.longValueExact())
            .step(BATCH_BLOCK_LIMIT).toList() + lastBlockNumber.longValueExact()

        val foundEvents = range.distinct().zipWithNext().flatMap {
            val from = it.first.toBigInteger()
            val to = it.second.toBigInteger()

            val filter = EthFilter(
                BlockNumber(from).toWeb3Parameter(),
                BlockNumber(to).toWeb3Parameter(),
                contractAddress.rawValue
            )

            filter.addSingleTopic(SEND_RTC_EVENT_TOPIC)

            val filterLog = retryOnceDelayed { blockchainProperties.web3j.ethGetLogs(filter).sendSafely() }
                ?: throw RpcException("Cannot get filter logs from RPC", ErrorCode.CANNOT_FETCH_FILTER_LOGS)

            val events = filterLog.logs.map { log ->
                logger.debug { "Found log: $log" }

                when (log) {
                    is EthLog.LogObject ->
                        SendRtcEvent(
                            chainId = chainSpec.chainId,
                            txHash = TransactionHash(log.transactionHash),
                            blockNumber = BlockNumber(log.blockNumber),
                            messageId = log.topics[1],
                            callerAddress = WalletAddress(log.topics[2])
                        )

                    else -> throw RpcException(
                        "Filter log is missing event data",
                        ErrorCode.MISSING_LOG_TX_HASH
                    )
                }
            }

            events
        }

        return Pair(foundEvents, BlockNumber(lastBlockNumber))
    }

    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    private fun <S, T : Response<*>?> Request<S, T>.sendSafely(): T? {
        try {
            val value = this.send()

            if (value?.hasError() == true) {
                logger.warn { "Web3j call errors: ${value.error.message}" }
                return null
            }

            return value
        } catch (ex: Exception) {
            logger.warn("Failed blockchain call", ex)
            return null
        }
    }

    @Suppress("MagicNumber")
    private fun <R> retryOnceDelayed(call: () -> R?): R? {
        return call() ?: run {
            logger.debug { "Waiting for 1 second and retrying RPC API call..." }
            Thread.sleep(1000L)
            call()
        }
    }
}
