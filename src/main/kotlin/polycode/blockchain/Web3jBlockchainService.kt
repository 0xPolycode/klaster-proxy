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
import polycode.model.result.CcipBasicInfo
import polycode.model.result.CcipErc20TransferInfo
import polycode.model.result.CcipNativeTransferTransferInfo
import polycode.model.result.CcipTxInfo
import polycode.model.result.CcipWalletCreateInfo
import polycode.model.result.ExecuteEvent
import polycode.model.result.SendRtcEvent
import polycode.service.AbiDecoderService
import polycode.util.AddressType
import polycode.util.Balance
import polycode.util.BlockNumber
import polycode.util.ChainlinkChainSelector
import polycode.util.ContractAddress
import polycode.util.DynamicArrayType
import polycode.util.DynamicBytesType
import polycode.util.FunctionSignature
import polycode.util.StaticBytesType
import polycode.util.StringType
import polycode.util.TransactionHash
import polycode.util.UintType
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.math.BigInteger

@Service
@Suppress("TooManyFunctions")
class Web3jBlockchainService(
    val abiDecoderService: AbiDecoderService,
    applicationProperties: ApplicationProperties
) : BlockchainService {

    companion object : KLogging() {
        private const val BATCH_BLOCK_LIMIT = 10_000L
        private const val SEND_RTC_EVENT_TOPIC = "0xaf3488626caa53f1d25b5a3850e43280695530019d11e81740d16209391587da"
        private const val EXECUTE_EVENT_TOPIC = "0x164339a296eae5d2a4502454bf4bfcd3a03e70fc5b9116b91eb4b055e22a5aa5"
        private const val HEX_RADIX = 16
        private const val FN_SIGNATURE_LENGTH = 10

        private const val EXECUTE_FUNCTION_SIGNATURE = "0xe6114eb4"
        private const val TRANSFER_FUNCTION_SIGNATURE = "0xa9059cbb"
        private const val EXEC_TRANSFER_FUNCTION_SIGNATURE = "0x6a761202"

        private val EXECUTE_FUNCTION_ARGS = listOf(
            DynamicArrayType(UintType),
            StringType,
            AddressType,
            UintType,
            DynamicBytesType,
            UintType,
            StaticBytesType(32)
        )

        private val TRANSFER_FUNCTION_ARGS = listOf(
            AddressType,
            UintType
        )

        private val EXEC_TRANSFER_FUNCTION_ARGS = listOf(
            AddressType,
            UintType,
            DynamicBytesType,
            UintType,
            UintType,
            UintType,
            UintType,
            AddressType,
            AddressType,
            DynamicBytesType
        )
    }

    @Suppress("UNCHECKED_CAST", "MagicNumber")
    private inner class ExecuteFunctionArgsExtractor(data: String) {
        val execChainSelectors: List<ChainlinkChainSelector>
        val salt: String
        val destination: WalletAddress
        val value: Balance
        val data: String
        val gasLimit: BigInteger
        val extraData: List<Byte>

        init {
            val decoded = abiDecoderService.decode(EXECUTE_FUNCTION_ARGS, data)

            this.execChainSelectors = (decoded[0] as List<BigInteger>).map(::ChainlinkChainSelector)
            this.salt = decoded[1] as String
            this.destination = WalletAddress(decoded[2] as String)
            this.value = Balance(decoded[3] as BigInteger)
            this.data = "0x" + (decoded[4] as List<Byte>).joinToString(separator = "") {
                it.toUByte().toString(HEX_RADIX).padStart(2, '0').removePrefix("0x")
            }
            this.gasLimit = decoded[5] as BigInteger
            this.extraData = decoded[6] as List<Byte>
        }
    }

    private inner class TransferFunctionArgsExtractor(data: String) {
        val destination: WalletAddress
        val amount: Balance

        init {
            val decoded = abiDecoderService.decode(TRANSFER_FUNCTION_ARGS, data)

            this.destination = WalletAddress(decoded[0] as String)
            this.amount = Balance(decoded[1] as BigInteger)
        }
    }

    @Suppress("UNCHECKED_CAST", "MagicNumber")
    private inner class ExecTransferFunctionArgsExtractor(data: String) {
        val to: WalletAddress
        val value: BigInteger
        val data: String
        val operation: BigInteger
        val safeTxGas: BigInteger
        val baseGas: BigInteger
        val gasPrice: BigInteger
        val gasToken: ContractAddress
        val refundReceiver: WalletAddress
        val signatures: List<Byte>

        init {
            val decoded = abiDecoderService.decode(EXEC_TRANSFER_FUNCTION_ARGS, data)

            this.to = WalletAddress(decoded[0] as String)
            this.value = decoded[1] as BigInteger
            this.data = "0x" + (decoded[2] as List<Byte>).joinToString(separator = "") {
                it.toUByte().toString(HEX_RADIX).padStart(2, '0').removePrefix("0x")
            }
            this.operation = decoded[3] as BigInteger
            this.safeTxGas = decoded[4] as BigInteger
            this.baseGas = decoded[5] as BigInteger
            this.gasPrice = decoded[6] as BigInteger
            this.gasToken = ContractAddress(decoded[7] as String)
            this.refundReceiver = WalletAddress(decoded[8] as String)
            this.signatures = decoded[9] as List<Byte>
        }
    }

    private val chainHandler = ChainPropertiesHandler(applicationProperties)

    override fun findSendRtcAndExecuteEvents(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        fromBlock: BlockNumber
    ): Triple<List<SendRtcEvent>, List<ExecuteEvent>, BlockNumber> {
        logger.debug {
            "Find transaction hashes by event logs, chainSpec: $chainSpec, contractAddress: $contractAddress," +
                " fromBlock: $fromBlock"
        }

        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val lastBlockNumber = blockchainProperties.web3j.ethBlockNumber().sendSafely()?.blockNumber
            ?: throw RpcException("Cannot fetch current block number from RPC", ErrorCode.CANNOT_FETCH_BLOCK_NUMBER)

        logger.debug { "Block range from: ${fromBlock.value} to: $lastBlockNumber" }

        val range = (
            fromBlock.value.longValueExact().rangeTo(lastBlockNumber.longValueExact())
                .step(BATCH_BLOCK_LIMIT).toList() + lastBlockNumber.longValueExact()
            ).distinct()

        fun <T> List<Long>.findEvents(topic: String, toEvent: (EthLog.LogObject) -> T) =
            this.zipWithNext().flatMap {
                val from = it.first.toBigInteger()
                val to = it.second.toBigInteger()

                val filter = EthFilter(
                    BlockNumber(from).toWeb3Parameter(),
                    BlockNumber(to).toWeb3Parameter(),
                    contractAddress.rawValue
                )

                filter.addSingleTopic(topic)

                val filterLog = retryOnceDelayed { blockchainProperties.web3j.ethGetLogs(filter).sendSafely() }
                    ?: throw RpcException("Cannot get filter logs from RPC", ErrorCode.CANNOT_FETCH_FILTER_LOGS)

                val events = filterLog.logs.map { log ->
                    logger.debug { "Found log: $log" }

                    when (log) {
                        is EthLog.LogObject -> toEvent(log)
                        else -> throw RpcException(
                            "Filter log is missing event data",
                            ErrorCode.MISSING_LOG_TX_HASH
                        )
                    }
                }

                events
            }

        val sendRtcEvents = range.findEvents(SEND_RTC_EVENT_TOPIC) { log ->
            SendRtcEvent(
                chainId = chainSpec.chainId,
                txHash = TransactionHash(log.transactionHash),
                blockNumber = BlockNumber(log.blockNumber),
                messageId = log.topics[1],
                callerAddress = WalletAddress(log.topics[2])
            )
        }

        val executeEvents = range.findEvents(EXECUTE_EVENT_TOPIC) { log ->
            ExecuteEvent(
                chainId = chainSpec.chainId,
                txHash = TransactionHash(log.transactionHash),
                blockNumber = BlockNumber(log.blockNumber),
                callerAddress = WalletAddress(log.topics[1])
            )
        }

        return Triple(sendRtcEvents, executeEvents, BlockNumber(lastBlockNumber))
    }

    override fun getCcipTxInfo(
        chainSpec: ChainSpec,
        txHash: TransactionHash
    ): CcipTxInfo {
        logger.debug { "Get CCIP transaction info, chainSpec: $chainSpec, txHash: $txHash" }

        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val transaction = blockchainProperties.web3j.ethGetTransactionByHash(txHash.value).sendSafely()
            ?.transaction?.orElse(null)
            ?: throw RpcException(
                "Cannot find transaction with hash: ${txHash.value}",
                ErrorCode.CANNOT_FETCH_TRANSACTION
            )

        val input = transaction.input.lowercase()
        val blockNumber = BlockNumber(transaction.blockNumber)
        val from = WalletAddress(transaction.from)
        val blockTimestamp = blockchainProperties.web3j.ethGetBlockByNumber(blockNumber.toWeb3Parameter(), false)
            .sendSafely()?.block?.timestamp?.longValueExact()
            ?: throw RpcException(
                "Cannot fetch block with number: ${blockNumber.value}",
                ErrorCode.CANNOT_FETCH_BLOCK
            )
        val txDate = UtcDateTime.ofEpochSeconds(blockTimestamp)
        val fnSignature = FunctionSignature(input.take(FN_SIGNATURE_LENGTH))

        return processExecuteFunctionData(
            fnSignature = fnSignature,
            input = input,
            chainSpec = chainSpec,
            from = from,
            txHash = txHash,
            txDate = txDate,
            blockNumber = blockNumber
        ) ?: processExecTransferFunctionData(
            fnSignature = fnSignature,
            input = input,
            chainSpec = chainSpec,
            from = from,
            txHash = txHash,
            txDate = txDate,
            blockNumber = blockNumber
        ) ?: CcipBasicInfo(
            chainId = chainSpec.chainId,
            txHash = txHash,
            txDate = txDate,
            fnSignature = fnSignature,
            txValue = Balance(transaction.value),
            blockNumber = blockNumber,
            controllerWallet = from
        )
    }

    private fun processExecuteFunctionData(
        fnSignature: FunctionSignature,
        input: String,
        chainSpec: ChainSpec,
        from: WalletAddress,
        txHash: TransactionHash,
        txDate: UtcDateTime,
        blockNumber: BlockNumber
    ): CcipTxInfo? =
        if (input.startsWith(EXECUTE_FUNCTION_SIGNATURE)) {
            val executeExtractor = ExecuteFunctionArgsExtractor(input.removePrefix(EXECUTE_FUNCTION_SIGNATURE))

            val destChains = executeExtractor.execChainSelectors.toSet()
            val salt = executeExtractor.salt
            val data = executeExtractor.data.lowercase()
            val value = executeExtractor.value

            if (data.startsWith(TRANSFER_FUNCTION_SIGNATURE)) {
                val transferExtractor = TransferFunctionArgsExtractor(data.removePrefix(TRANSFER_FUNCTION_SIGNATURE))

                CcipErc20TransferInfo(
                    chainId = chainSpec.chainId,
                    txHash = txHash,
                    fnSignature = fnSignature,
                    blockNumber = blockNumber,
                    controllerWallet = from,
                    txDate = txDate,
                    txValue = value,
                    destChains = destChains,
                    salt = salt,
                    tokenAddress = executeExtractor.destination.toContractAddress(),
                    tokenReceiver = transferExtractor.destination,
                    tokenAmount = transferExtractor.amount
                )
            } else if (value.rawValue == BigInteger.ZERO) {
                CcipWalletCreateInfo(
                    chainId = chainSpec.chainId,
                    txHash = txHash,
                    fnSignature = fnSignature,
                    blockNumber = blockNumber,
                    controllerWallet = from,
                    txDate = txDate,
                    destChains = destChains,
                    salt = salt
                )
            } else {
                CcipNativeTransferTransferInfo(
                    chainId = chainSpec.chainId,
                    txHash = txHash,
                    fnSignature = fnSignature,
                    blockNumber = blockNumber,
                    controllerWallet = from,
                    txDate = txDate,
                    txValue = value,
                    destChains = destChains,
                    salt = salt
                )
            }
        } else null

    private fun processExecTransferFunctionData(
        fnSignature: FunctionSignature,
        input: String,
        chainSpec: ChainSpec,
        from: WalletAddress,
        txHash: TransactionHash,
        txDate: UtcDateTime,
        blockNumber: BlockNumber
    ): CcipTxInfo? =
        if (input.startsWith(EXEC_TRANSFER_FUNCTION_SIGNATURE)) {
            val extractor = ExecTransferFunctionArgsExtractor(input.removePrefix(EXEC_TRANSFER_FUNCTION_SIGNATURE))
            val data = extractor.data.lowercase()

            processExecuteFunctionData(
                fnSignature = fnSignature,
                input = data,
                chainSpec = chainSpec,
                from = from,
                txHash = txHash,
                txDate = txDate,
                blockNumber = blockNumber
            )
        } else null

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
