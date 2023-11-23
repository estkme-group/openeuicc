package net.typeblog.lpac_jni

internal object LpacJni {
    init {
        System.loadLibrary("lpac-jni")
    }

    external fun createContext(apduInterface: ApduInterface, httpInterface: HttpInterface): Long
    external fun destroyContext(handle: Long)

    // es10x
    external fun es10xInit(handle: Long): Int
    external fun es10xFini(handle: Long)

    // es10c
    // null returns signify errors
    external fun es10cGetEid(handle: Long): String?
    external fun es10cGetProfilesInfo(handle: Long): Array<LocalProfileInfo>?
}