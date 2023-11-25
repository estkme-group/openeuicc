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
    external fun es10cEnableProfile(handle: Long, iccid: String): Int
    external fun es10cDisableProfile(handle: Long, iccid: String): Int
    external fun es10cDeleteProfile(handle: Long, iccid: String): Int
    external fun es10cSetNickname(handle: Long, iccid: String, nick: String): Int

    // es9p + es10b
    // We do not expose all of the functions because of tediousness :)
    external fun downloadProfile(handle: Long, smdp: String, matchingId: String, imei: String,
                                 confirmationCode: String?, callback: ProfileDownloadCallback): Int
}