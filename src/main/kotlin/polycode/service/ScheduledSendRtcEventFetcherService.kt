package polycode.service

import mu.KLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.config.ApplicationProperties
import polycode.config.KlasterApiProperties
import polycode.repository.CachedSendRtcEventRepository
import polycode.repository.CcipTxInfoRepository
import polycode.repository.LatestFetchedSendRtcEventBlockNumberRepository
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import java.util.concurrent.TimeUnit

@Service
class ScheduledSendRtcEventFetcherService(
    private val blockchainService: BlockchainService,
    private val cachedSendRtcEventRepository: CachedSendRtcEventRepository,
    private val latestFetchedSendRtcEventBlockNumberRepository: LatestFetchedSendRtcEventBlockNumberRepository,
    private val ccipTxInfoRepository: CcipTxInfoRepository,
    private val applicationProperties: ApplicationProperties,
    private val klasterApiProperties: KlasterApiProperties,
    scheduledExecutorServiceProvider: ScheduledExecutorServiceProvider
) : DisposableBean, InitializingBean {

    companion object : KLogging()

    private val scheduler = scheduledExecutorServiceProvider.newSingleThreadScheduledExecutor("SendRtcFetcher")

    override fun afterPropertiesSet() {
        this.scheduler.scheduleAtFixedRate(
            command = { updateSendRtcEvents() },
            initialDelay = 0L,
            period = applicationProperties.eventCachingTaskPeriodInSeconds,
            unit = TimeUnit.SECONDS
        )
    }

    override fun destroy() {
        this.scheduler.shutdown()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun updateSendRtcEvents() {
        logger.info { "Running scheduled SendRtc event fetching task, chains: ${applicationProperties.chain.keys}" }

        applicationProperties.chain.entries.forEach {
            logger.info { "Processing chainId: ${it.key}, name: ${it.value.name}" }

            try {
                val chainSpec = ChainSpec(chainId = it.key, customRpcUrl = null)
                val foundEvents = blockchainService.findSendRtcEvents(
                    chainSpec = chainSpec,
                    contractAddress = ContractAddress(klasterApiProperties.contractAddress),
                    fromBlock = latestFetchedSendRtcEventBlockNumberRepository.get(it.key)
                        ?: BlockNumber(it.value.startBlockNumber)
                )

                cachedSendRtcEventRepository.insertAll(foundEvents.first)
                latestFetchedSendRtcEventBlockNumberRepository.upsert(
                    chainId = it.key,
                    blockNumber = foundEvents.second
                )

                val txHashesWithoutInfo = ccipTxInfoRepository.getAllTxHashesWithoutTxInfo(chainSpec.chainId)

                logger.info { "Found ${txHashesWithoutInfo.size} txHashes without txInfo" }

                txHashesWithoutInfo.forEach { txHash ->
                    ccipTxInfoRepository.insert(blockchainService.getCcipTxInfo(chainSpec, txHash))
                }
            } catch (ex: Exception) {
                logger.error(ex) { "Failed to process chainId: ${it.key}" }
            }
        }

        logger.info { "Scheduled SendRtc event fetching task finished" }
    }
}
