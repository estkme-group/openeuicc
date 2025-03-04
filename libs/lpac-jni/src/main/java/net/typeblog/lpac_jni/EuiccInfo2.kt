package net.typeblog.lpac_jni

/* Corresponds to EuiccInfo2 in SGP.22 */
data class EuiccInfo2(
    val sgp22Version: Version,
    val profileVersion: Version,
    val euiccFirmwareVersion: Version,
    val globalPlatformVersion: Version,
    val sasAccreditationNumber: String,
    val ppVersion: Version,
    val freeNvram: Int,
    val freeRam: Int,
    val euiccCiPKIdListForSigning: Set<String>,
    val euiccCiPKIdListForVerification: Set<String>,
)

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
) {
    constructor(version: String) : this(version.split('.').map(String::toInt))
    private constructor(parts: List<Int>) : this(parts[0], parts[1], parts[2])

    operator fun compareTo(other: Version): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        return patch - other.patch
    }

    override fun toString() = "$major.$minor.$patch"
}
