package im.angry.openeuicc.util

data class ActivationCode(
    val address: String,
    val matchingId: String? = null,
    val oid: String? = null,
    val confirmationCodeRequired: Boolean = false,
) {
    companion object {
        fun fromString(input: String): ActivationCode {
            val components = input.removePrefix("LPA:").split('$')
            if (components.size < 2 || components[0] != "1") {
                throw IllegalArgumentException("Invalid activation code format")
            }
            return ActivationCode(
                address = components[1].trim(),
                matchingId = components.getOrNull(2)?.trim()?.ifBlank { null },
                oid = components.getOrNull(3)?.trim()?.ifBlank { null },
                confirmationCodeRequired = components.getOrNull(4)?.trim() == "1"
            )
        }
    }

    override fun toString(): String {
        val parts = arrayOf(
            "1",
            address,
            matchingId ?: "",
            oid ?: "",
            if (confirmationCodeRequired) "1" else ""
        )
        return parts.joinToString("$").trimEnd('$')
    }
}