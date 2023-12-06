package polycode.service

import com.fasterxml.jackson.databind.JsonNode
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import polycode.exception.KlasterApiCallFailedException

@Service
class KlasterWalletActivityServiceImpl(private val klasterRestTemplate: RestTemplate) : KlasterWalletActivityService {

    companion object : KLogging()

    override fun getWalletActivity(filerName: String, filterValue: String): JsonNode? {
        try {
            val response = klasterRestTemplate.getForEntity(
                "/api/query${createQuery(filerName, filterValue)}",
                JsonNode::class.java
            )

            if (response.statusCode.is2xxSuccessful) {
                return response.body
            }

            logger.warn { "Klaster API call failed" }
            throw KlasterApiCallFailedException()
        } catch (ex: RestClientException) {
            logger.warn(ex) { "Klaster API client call exception" }
            throw KlasterApiCallFailedException()
        }
    }

    private fun createQuery(filterName: String, filterValue: String): String {
        return "?query=LATEST_TRANSACTIONS_QUERY" +
            "&variables=%7B%22first%22:100,%22offset%22:0,%22condition%22:%7B%22$filterName%22:%22$filterValue%22%7D%7D"
    }
}
