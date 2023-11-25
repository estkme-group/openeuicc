package net.typeblog.lpac_jni

interface ProfileDownloadCallback {
    enum class DownloadState(val progress: Int) {
        Preparing(0),
        Connecting(20), // Before {server,client} authentication
        Authenticating(40), // {server,client} authentication
        Downloading(60), // prepare download, get bpp from es9p
        Finalizing(80), // load bpp
    }

    fun onStateUpdate(state: DownloadState)
}