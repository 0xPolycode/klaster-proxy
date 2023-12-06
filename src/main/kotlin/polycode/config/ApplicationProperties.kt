@file:Suppress("MagicNumber")

package polycode.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Configuration
import polycode.util.ChainId
import java.math.BigInteger

@Configuration
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "polycode")
class ApplicationProperties {
    var chain: Map<ChainId, ChainProperties> = emptyMap()
    var infuraId: String = ""
    var eventCachingTaskPeriodInMinutes: Long = 5L
}

@ConstructorBinding
data class ChainProperties(
    val name: String,
    val rpcUrl: String,
    val startBlockNumber: BigInteger,
    val infuraUrl: String?
)

@ConstructorBinding
@ConfigurationProperties(prefix = "polycode.klaster-api")
data class KlasterApiProperties(
    val url: String,
    val contractAddress: String
)
