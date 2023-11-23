package net.typeblog.lpac_jni

internal object LpacJni {
    init {
        System.loadLibrary("lpac-jni")
    }

    external fun createContext(apduInterface: ApduInterface, httpInterface: HttpInterface): Long
    external fun destroyContext(handle: Long)
}