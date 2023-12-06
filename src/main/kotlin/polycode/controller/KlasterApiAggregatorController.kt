package polycode.controller

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import polycode.exception.InvalidQueryParamException
import polycode.model.response.AggregatedKlasterApiResponse
import polycode.service.KlasterApiAggregatorService
import polycode.service.KlasterWalletActivityService
import polycode.util.WalletAddress

@Validated
@RestController
class KlasterApiAggregatorController(
    private val klasterApiAggregatorService: KlasterApiAggregatorService,
    private val klasterWalletActivityService: KlasterWalletActivityService
) {

    @GetMapping("/api/get-activity")
    fun getWalletActivity(@RequestParam(required = true) wallet: String): ResponseEntity<AggregatedKlasterApiResponse> {
        return ResponseEntity.ok(klasterApiAggregatorService.aggregateTransactionResponses(WalletAddress(wallet)))
    }

    @GetMapping("/api/query-ccip")
    fun queryCcip(
        @RequestParam(required = false) messageId: String?,
        @RequestParam(required = false) txHash: String?
    ): ResponseEntity<JsonNode> {
        return if (messageId != null && txHash == null) {
            ResponseEntity.ok(klasterWalletActivityService.getWalletActivity("messageId", messageId))
        } else if (messageId == null && txHash != null) {
            ResponseEntity.ok(klasterWalletActivityService.getWalletActivity("transactionHash", txHash))
        } else {
            throw InvalidQueryParamException("Either messageId or txHash query param must be specified and not both")
        }
    }
}
