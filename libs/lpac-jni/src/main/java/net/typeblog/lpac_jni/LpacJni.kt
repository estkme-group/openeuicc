package net.typeblog.lpac_jni

internal object LpacJni {
    init {
        System.loadLibrary("lpac-jni")
    }

    external fun createContext(apduInterface: ApduInterface, httpInterface: HttpInterface): Long
    external fun destroyContext(handle: Long)

    external fun euiccInit(handle: Long): Int
    external fun euiccSetMss(handle: Long, mss: Byte)
    external fun euiccFini(handle: Long)

    // es10c
    // null returns signify errors
    external fun es10cGetEid(handle: Long): String?
    external fun es10cGetProfilesInfo(handle: Long): Long
    external fun es10cEnableProfile(handle: Long, iccid: String, refresh: Boolean): Int
    external fun es10cDisableProfile(handle: Long, iccid: String, refresh: Boolean): Int
    external fun es10cDeleteProfile(handle: Long, iccid: String): Int
    external fun es10cSetNickname(handle: Long, iccid: String, nick: String): Int

    // es10b
    external fun es10bListNotification(handle: Long): Long // A native pointer to a linked list. Handle with linked list-related methods below. May be 0 (null)
    external fun es10bDeleteNotification(handle: Long, seqNumber: Long): Int

    // es9p + es10b
    // We do not expose all of the functions because of tediousness :)
    external fun downloadProfile(handle: Long, smdp: String, matchingId: String?, imei: String?,
                                 confirmationCode: String?, callback: ProfileDownloadCallback): Int
    external fun handleNotification(handle: Long, seqNumber: Long): Int
    // Cancel any ongoing es9p and/or es10b sessions
    external fun cancelSessions(handle: Long)

    // es10cex (actually part of es10b)
    external fun es10cexGetEuiccInfo2(handle: Long): Long

    // C <-> Java struct / linked list handling
    // C String arrays
    external fun stringArrNext(curr: Long): Long
    external fun stringDeref(curr: Long): String
    // Profiles
    external fun profilesNext(curr: Long): Long
    external fun profilesFree(head: Long): Long
    external fun profileGetIccid(curr: Long): String
    external fun profileGetIsdpAid(curr: Long): String
    external fun profileGetName(curr: Long): String
    external fun profileGetNickname(curr: Long): String
    external fun profileGetServiceProvider(curr: Long): String
    external fun profileGetStateString(curr: Long): String
    external fun profileGetClassString(curr: Long): String
    // Notifications
    external fun notificationsNext(curr: Long): Long
    external fun notificationGetSeq(curr: Long): Long
    external fun notificationGetOperationString(curr: Long): String
    external fun notificationGetAddress(curr: Long): String
    external fun notificationGetIccid(curr: Long): String
    external fun notificationsFree(head: Long)
    // EuiccInfo2
    external fun euiccInfo2Free(info: Long)
    external fun euiccInfo2GetProfileVersion(info: Long): String
    external fun euiccInfo2GetEuiccFirmwareVersion(info: Long): String
    external fun euiccInfo2GetGlobalPlatformVersion(info: Long): String
    external fun euiccInfo2GetSasAcreditationNumber(info: Long): String
    external fun euiccInfo2GetPpVersion(info: Long): String
    external fun euiccInfo2GetFreeNonVolatileMemory(info: Long): Long
    external fun euiccInfo2GetFreeVolatileMemory(info: Long): Long
    // C String Arrays
    external fun euiccInfo2GetEuiccCiPKIdListForSigning(info: Long): Long
    external fun euiccInfo2GetEuiccCiPKIdListForVerification(info: Long): Long
}