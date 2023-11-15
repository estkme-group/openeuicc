package net.typeblog.lpac_jni

private object LpacJni {
    init {
        System.loadLibrary("lpac-jni")
    }

    external fun createContext(apduInterface: ApduInterface, httpInterface: HttpInterface): Long
    external fun destroyContext(handle: Long)
    external fun setCurrentContext(handle: Long)
}