package net.typeblog.lpac_jni

// TODO: We need to export profilePolicyRules here as well (currently unsupported by lpac)
data class RemoteProfileInfo(
    val iccid: String,
    val name: String,
    val providerName: String,
    val profileClass: ProfileClass,
)
