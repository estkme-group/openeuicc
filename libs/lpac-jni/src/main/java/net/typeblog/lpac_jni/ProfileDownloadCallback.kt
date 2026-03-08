package net.typeblog.lpac_jni

fun interface ProfileDownloadCallback {
    fun onStateUpdate(state: ProfileDownloadState)

    /**
     * Optionally override this to abort / continue a download based on metadata acquired in the process
     * Note that not all ES9P servers may return metadata.
     */
    fun onConfirmMetadata(metadata: RemoteProfileInfo?): Boolean = true
}
