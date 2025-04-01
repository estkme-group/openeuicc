package im.angry.openeuicc.util

data class LPAString(
    val address: String,
    val matchingId: String?,
    val oid: String?,
    val confirmationCodeRequired: Boolean,
) {
    companion object {
        fun parse(input: String): LPAString {
            var token = input
            if (token.startsWith("LPA:", ignoreCase = true)) token = token.drop(4)
            val components = token.split('$').map { it.trim().ifBlank { null } }
            require(components.getOrNull(0) == "1") { "Invalid AC_Format" }
            return LPAString(
                requireNotNull(components.getOrNull(1)) { "SM-DP+ is required" },
                components.getOrNull(2),
                components.getOrNull(3),
                components.getOrNull(4) == "1"
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