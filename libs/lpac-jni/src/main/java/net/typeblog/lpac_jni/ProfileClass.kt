package net.typeblog.lpac_jni

enum class ProfileClass {
    Testing,
    Provisioning,
    Operational;

    companion object {
        @JvmStatic
        fun fromString(str: String?) =
            when (str?.lowercase()) {
                "test" -> Testing
                "provisioning" -> Provisioning
                "operational" -> Operational
                else -> Operational
            }
    }
}
