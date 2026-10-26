package net.typeblog.lpac_jni

sealed class ProfileDownloadState {
    class Preparing : ProfileDownloadState()
    class Connecting : ProfileDownloadState()
    class Authenticating : ProfileDownloadState()
    class ConfirmingDownload(val metadata: RemoteProfileInfo?) : ProfileDownloadState() {
        // FIXME: Pass this back to JNI better. Something that's not a random property.
        var confirmationCode: String? = null
    }
    class Downloading : ProfileDownloadState()
    class Finalizing : ProfileDownloadState()
}
