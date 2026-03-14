package net.typeblog.lpac_jni

fun interface ProfileDownloadCallback {
    fun onStatusUpdate(state: ProfileDownloadState): Boolean
}
