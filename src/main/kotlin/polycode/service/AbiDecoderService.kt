package polycode.service

import polycode.util.AbiType

interface AbiDecoderService {
    fun decode(types: List<AbiType>, encodedInput: String): List<Any>
}
