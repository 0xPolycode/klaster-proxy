package polycode.controller

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import polycode.exception.InvalidQueryParamException
import polycode.exception.ServiceException
import polycode.model.response.AggregatedKlasterApiResponse
import polycode.model.response.HasWalletActivityData
import polycode.model.response.HasWalletActivityErrorData
import polycode.model.response.HasWalletActivityErrorResponse
import polycode.model.response.HasWalletActivityResponse
import polycode.model.response.HasWalletActivitySuccessResponse
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

    @GetMapping("/api/has-activity")
    fun hasWalletActivity(@RequestParam(required = true) address: String): ResponseEntity<HasWalletActivityResponse> {
        return try {
            ResponseEntity.ok(
                HasWalletActivitySuccessResponse(
                    HasWalletActivityData(
                        klasterApiAggregatorService.checkIfWalletAddressHasCcipResponse(WalletAddress(address))
                    )
                )
            )
        } catch (e: ServiceException) {
            ResponseEntity.ok(
                HasWalletActivityErrorResponse(
                    HasWalletActivityErrorData(
                        code = e.errorCode.name,
                        message = e.message
                    ),
                    HasWalletActivityData(false)
                )
            )
        }
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
