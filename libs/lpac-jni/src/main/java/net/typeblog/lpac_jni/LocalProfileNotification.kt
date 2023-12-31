package net.typeblog.lpac_jni

data class LocalProfileNotification(
    val seqNumber: Long,
    val profileManagementOperation: Operation,
    val notificationAddress: String,
    val iccid: String,
) {
    enum class Operation {
        Install,
        Enable,
        Disable,
        Delete;

        companion object {
            @JvmStatic
            fun fromString(str: String?) =
                when (str?.lowercase()) {
                    "install" -> Install
                    "enable" -> Enable
                    "disable" -> Disable
                    "delete" -> Delete
                    else -> throw IllegalArgumentException("Unknown operation $str")
                }
        }
    }
}