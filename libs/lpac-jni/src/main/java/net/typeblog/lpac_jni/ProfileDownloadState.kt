package net.typeblog.lpac_jni

sealed class ProfileDownloadState {
    class Preparing : ProfileDownloadState()
    class Connecting : ProfileDownloadState()
    class Authenticating : ProfileDownloadState()
    class ConfirmingDownload(val metadata: RemoteProfileInfo?) : ProfileDownloadState() {
        // Can be set by the callback before confirming, when the confirmation code only became
        // known after the download started. Takes precedence over ProfileDownloadInput's.
        var confirmationCode: String? = null
    }
    class Downloading : ProfileDownloadState()
    class Finalizing : ProfileDownloadState()
}
