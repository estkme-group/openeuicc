package im.angry.openeuicc.vendored

import net.typeblog.lpac_jni.Version

private val prefix = Regex("^89044045(84|21)67274948") // SIMLink EID prefix

fun getSIMLinkVersion(eid: String, version: Version?): String? {
    if (version == null || prefix.find(eid, 0) == null) return null
    return when {
        // @formatter:off
        version >= Version(37,  1, 41) -> "v3.1 (beta 1)"
        version >= Version(36, 18,  5) -> "v3 (final)"
        version >= Version(36, 17, 39) -> "v3 (beta)"
        version >= Version(36, 17,  4) -> "v2s"
        version >= Version(36,  9,  3) -> "v2.1"
        version >= Version(36,  7,  2) -> "v2"
        // @formatter:on
        else -> null
    }
}
