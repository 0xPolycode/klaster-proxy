package polycode.service

import polycode.model.response.AggregatedKlasterApiResponse
import polycode.util.WalletAddress

interface KlasterApiAggregatorService {
    fun aggregateTransactionResponses(walletAddress: WalletAddress): AggregatedKlasterApiResponse
    fun checkIfWalletAddressHasCcipResponse(walletAddress: WalletAddress): Boolean
}
