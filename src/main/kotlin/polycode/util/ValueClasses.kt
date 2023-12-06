package polycode.util

import org.web3j.abi.datatypes.Address
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger

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

@JvmInline
value class ChainId(val value: Long)

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
