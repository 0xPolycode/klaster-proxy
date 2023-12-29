package polycode.model.response

import com.fasterxml.jackson.databind.JsonNode

data class AggregatedKlasterApiResponse(
    val ccipApiResponses: List<JsonNode>,
    val txInfos: List<CcipTxInfoResponse>
)
