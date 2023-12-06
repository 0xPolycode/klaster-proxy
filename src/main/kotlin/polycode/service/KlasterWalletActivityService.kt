package polycode.service

import com.fasterxml.jackson.databind.JsonNode

interface KlasterWalletActivityService {
    fun getWalletActivity(filerName: String, filterValue: String): JsonNode?
}
