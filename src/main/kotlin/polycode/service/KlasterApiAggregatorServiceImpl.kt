package polycode.service

import mu.KLogging
import org.springframework.stereotype.Service
import polycode.model.response.AggregatedKlasterApiResponse
import polycode.model.response.CcipTxInfoResponse
import polycode.repository.CachedExecuteEventRepository
import polycode.repository.CachedSendRtcEventRepository
import polycode.repository.CcipTxInfoRepository
import polycode.util.WalletAddress

@Service
class KlasterApiAggregatorServiceImpl(
    private val klasterWalletActivityService: KlasterWalletActivityService,
    private val cachedSendRtcEventRepository: CachedSendRtcEventRepository,
    private val cachedExecuteEventRepository: CachedExecuteEventRepository,
    private val ccipTxInfoRepository: CcipTxInfoRepository
) : KlasterApiAggregatorService {

    companion object : KLogging()

    override fun aggregateTransactionResponses(walletAddress: WalletAddress): AggregatedKlasterApiResponse {
        logger.info { "Get all transactions for walletAddress: $walletAddress" }

        val transactionHashes = cachedSendRtcEventRepository.getAllTxHashes(walletAddress)

        logger.debug { "Found ${transactionHashes.size} SendRTC transaction hashes for walletAddress: $walletAddress" }

        val klasterApiResponses = transactionHashes.mapNotNull {
            klasterWalletActivityService.getWalletActivity("transactionHash", it.value)
        }

        logger.debug {
            "Got ${klasterApiResponses.size} non-null Klaster API responses for walletAddress: $walletAddress"
        }

        val allTxHashes = transactionHashes + cachedExecuteEventRepository.getAllTxHashes(walletAddress)

        logger.debug { "Found ${allTxHashes.size} total transaction hashes for walletAddress: $walletAddress" }

        val txInfos = ccipTxInfoRepository.getByTxHashes(allTxHashes)

        logger.debug {
            "Fetched ${txInfos.size} txInfos for walletAddress: $walletAddress"
        }

        return AggregatedKlasterApiResponse(klasterApiResponses, txInfos.map(CcipTxInfoResponse::fromTxInfo))
    }

    override fun checkIfWalletAddressHasCcipResponse(walletAddress: WalletAddress): Boolean {
        logger.info { "Check if address has response from CCIP API, walletAddress: $walletAddress" }

        val transactionHashes = cachedSendRtcEventRepository.getAllTxHashes(walletAddress)

        logger.debug { "Found ${transactionHashes.size} transaction hashes for walletAddress: $walletAddress" }

        return transactionHashes.any {
            klasterWalletActivityService.getWalletActivity("transactionHash", it.value) != null
        }
    }
}
