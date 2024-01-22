package polycode.util

import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Uint
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@JvmInline
value class UtcDateTime private constructor(val value: OffsetDateTime) {
    companion object {
        private val ZONE_OFFSET = ZoneOffset.UTC
        operator fun invoke(value: OffsetDateTime) = UtcDateTime(value.withOffsetSameInstant(ZONE_OFFSET))

        fun ofEpochSeconds(value: Long) = UtcDateTime(
            OffsetDateTime.ofInstant(Instant.ofEpochSecond(value), ZONE_OFFSET)
        )

        fun ofInstant(instant: Instant) = UtcDateTime(
            OffsetDateTime.ofInstant(instant, ZONE_OFFSET)
        )
    }

    val iso: String
        get() = DateTimeFormatter.ISO_DATE_TIME.format(value)
}

sealed interface EthereumAddress {
    val value: Address
    val rawValue: String
        get() = value.value

    fun toWalletAddress() = WalletAddress(value)
    fun toContractAddress() = ContractAddress(value)
}

object ZeroAddress : EthereumAddress {
    override val value: Address = Address("0")
}

@JvmInline
value class WalletAddress private constructor(override val value: Address) : EthereumAddress {
    companion object {
        operator fun invoke(value: Address) = WalletAddress(value.toString())
    }

    constructor(value: String) : this(Address(value.lowercase()))
}

@JvmInline
value class ContractAddress private constructor(override val value: Address) : EthereumAddress {
    companion object {
        operator fun invoke(value: Address) = ContractAddress(value.toString())
    }

    constructor(value: String) : this(Address(value.lowercase()))
}

sealed interface EthereumUint {
    val value: Uint
    val rawValue: BigInteger
        get() = value.value
}

@JvmInline
value class Balance(override val value: Uint) : EthereumUint {
    companion object {
        val ZERO = Balance(BigInteger.ZERO)
    }

    constructor(value: BigInteger) : this(Uint(value))
}

@JvmInline
value class ChainId(val value: Long)

@JvmInline
value class ChainlinkChainSelector(val value: BigInteger)

sealed interface BlockParameter {
    fun toWeb3Parameter(): DefaultBlockParameter
}

@JvmInline
value class BlockNumber(val value: BigInteger) : BlockParameter {
    override fun toWeb3Parameter(): DefaultBlockParameter = DefaultBlockParameter.valueOf(value)
}

enum class BlockName(private val web3BlockName: DefaultBlockParameterName) : BlockParameter {
    EARLIEST(DefaultBlockParameterName.EARLIEST),
    LATEST(DefaultBlockParameterName.LATEST),
    PENDING(DefaultBlockParameterName.PENDING);

    override fun toWeb3Parameter() = web3BlockName
}

@JvmInline
value class TransactionHash private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = TransactionHash("0x" + value.removePrefix("0x").lowercase())
    }
}

@JvmInline
value class FunctionSignature private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = FunctionSignature("0x" + value.removePrefix("0x").lowercase())
    }
}
