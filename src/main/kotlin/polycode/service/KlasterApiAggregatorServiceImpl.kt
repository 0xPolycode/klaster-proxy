package polycode.service

import mu.KLogging
import org.springframework.stereotype.Service
import polycode.model.response.AggregatedKlasterApiResponse
import polycode.repository.CachedSendRtcEventRepository
import polycode.util.WalletAddress

@Service
class KlasterApiAggregatorServiceImpl(
    private val klasterWalletActivityService: KlasterWalletActivityService,
    private val cachedSendRtcEventRepository: CachedSendRtcEventRepository
) : KlasterApiAggregatorService {

    companion object : KLogging()

    override fun aggregateTransactionResponses(walletAddress: WalletAddress): AggregatedKlasterApiResponse {
        logger.info { "Get all transactions for walletAddress: $walletAddress" }

        val transactionHashes = cachedSendRtcEventRepository.getAllTxHashes(walletAddress)

        logger.debug { "Found ${transactionHashes.size} transaction hashes for walletAddress: $walletAddress" }

        val klasterApiResponses = transactionHashes.mapNotNull {
            klasterWalletActivityService.getWalletActivity("transactionHash", it.value)
        }

        logger.debug {
            "Got ${klasterApiResponses.size} non-null Klaster API responses for walletAddress: $walletAddress"
        }

        return AggregatedKlasterApiResponse(klasterApiResponses)
    }
}
