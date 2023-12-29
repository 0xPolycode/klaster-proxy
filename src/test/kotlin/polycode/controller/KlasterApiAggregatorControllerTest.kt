package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.model.response.AggregatedKlasterApiResponse
import polycode.service.KlasterApiAggregatorService
import polycode.util.WalletAddress

class KlasterApiAggregatorControllerTest : TestBase() { // TODO more tests

    @Test
    fun mustCorrectlyGetWalletActivity() {
        val service = mock<KlasterApiAggregatorService>()
        val walletAddress = WalletAddress("a")
        val expectedResponse = AggregatedKlasterApiResponse(emptyList(), emptyList())

        suppose("activity response will be returned") {
            call(service.aggregateTransactionResponses(walletAddress))
                .willReturn(expectedResponse)
        }

        val controller = KlasterApiAggregatorController(service, mock())

        verify("controller returns correct response") {
            val response = controller.getWalletActivity(walletAddress.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(expectedResponse))
        }
    }
}
