package net.typeblog.lpac_jni

// TODO: We need to export profilePolicyRules here as well (currently unsupported by lpac)
data class RemoteProfileInfo(
    val iccid: String,
    val name: String,
    val providerName: String,
    val profileClass: ProfileClass,
    // Carrier privilege access rules (REF-AR-DO) from the profile metadata
    val accessRules: List<RemoteProfileAccessRule>,
) {
    // Used by JNI (see lpac-download.c)
    @Suppress("unused")
    constructor(
        iccid: String,
        name: String,
        providerName: String,
        profileClass: ProfileClass,
        accessRules: Array<RemoteProfileAccessRule>,
    ) : this(iccid, name, providerName, profileClass, accessRules.toList())
}

// packageName is null if the rule applies to any app signed with the certificate
data class RemoteProfileAccessRule(
    val certificateHash: String,
    val packageName: String?,
)
