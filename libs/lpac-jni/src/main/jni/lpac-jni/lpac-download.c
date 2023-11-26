#include <euicc/es9p.h>
#include <euicc/es10b.h>
#include <stdlib.h>
#include <string.h>
#include "lpac-download.h"

jobject download_state_preparing;
jobject download_state_connecting;
jobject download_state_authenticating;
jobject download_state_downloading;
jobject download_state_finalizing;

jmethodID on_state_update;

void lpac_download_init() {
    LPAC_JNI_SETUP_ENV;

    jclass download_state_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState");
    jfieldID download_state_preparing_field = (*env)->GetStaticFieldID(env, download_state_class, "Preparing", "Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;");
    download_state_preparing = (*env)->GetStaticObjectField(env, download_state_class, download_state_preparing_field);
    download_state_preparing = (*env)->NewGlobalRef(env, download_state_preparing);
    jfieldID download_state_connecting_field = (*env)->GetStaticFieldID(env, download_state_class, "Connecting", "Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;");
    download_state_connecting = (*env)->GetStaticObjectField(env, download_state_class, download_state_connecting_field);
    download_state_connecting = (*env)->NewGlobalRef(env, download_state_connecting);
    jfieldID download_state_authenticating_field = (*env)->GetStaticFieldID(env, download_state_class, "Authenticating", "Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;");
    download_state_authenticating = (*env)->GetStaticObjectField(env, download_state_class, download_state_authenticating_field);
    download_state_authenticating = (*env)->NewGlobalRef(env, download_state_authenticating);
    jfieldID download_state_downloading_field = (*env)->GetStaticFieldID(env, download_state_class, "Downloading", "Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;");
    download_state_downloading = (*env)->GetStaticObjectField(env, download_state_class, download_state_downloading_field);
    download_state_downloading = (*env)->NewGlobalRef(env, download_state_downloading);
    jfieldID download_state_finalizng_field = (*env)->GetStaticFieldID(env, download_state_class, "Finalizing", "Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;");
    download_state_finalizing = (*env)->GetStaticObjectField(env, download_state_class, download_state_finalizng_field);
    download_state_finalizing = (*env)->NewGlobalRef(env, download_state_finalizing);

    jclass download_callback_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/ProfileDownloadCallback");
    on_state_update = (*env)->GetMethodID(env, download_callback_class, "onStateUpdate", "(Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;)V");
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_downloadProfile(JNIEnv *env, jobject thiz, jlong handle,
                                                    jstring smdp, jstring matching_id,
                                                    jstring imei, jstring confirmation_code,
                                                    jobject callback) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    struct es9p_get_bound_profile_package_resp es9p_get_bound_profile_package_resp;
    struct es9p_initiate_authentication_resp es9p_initiate_authentication_resp;
    struct es10b_authenticate_server_param es10b_authenticate_server_param;
    struct es9p_authenticate_client_resp es9p_authenticate_client_resp;
    struct es10b_prepare_download_param es10b_prepare_download_param;
    char *b64_authenticate_server_response = NULL;
    char *b64_prepare_download_response = NULL;
    char *b64_euicc_challenge = NULL;
    char *b64_euicc_info_1 = NULL;
    char *transaction_id = NULL;
    const char *_confirmation_code = NULL;
    const char *_matching_id = NULL;
    const char *_smdp = NULL;
    const char *_imei = NULL;
    int ret;

    if (confirmation_code != NULL)
        _confirmation_code = (*env)->GetStringUTFChars(env, confirmation_code, NULL);
    if (matching_id != NULL)
        _matching_id = (*env)->GetStringUTFChars(env, matching_id, NULL);
    _smdp = (*env)->GetStringUTFChars(env, smdp, NULL);
    if (imei != NULL)
        _imei = (*env)->GetStringUTFChars(env, imei, NULL);

    (*env)->CallVoidMethod(env, callback, on_state_update, download_state_preparing);
    ret = es10b_get_euicc_challenge(ctx, &b64_euicc_challenge);
    if (ret < 0)
        goto out;

    ret = es10b_get_euicc_info(ctx, &b64_euicc_info_1);
    if (ret < 0)
        goto out;

    (*env)->CallVoidMethod(env, callback, on_state_update, download_state_connecting);
    ret = es9p_initiate_authentication(ctx, _smdp, b64_euicc_challenge, b64_euicc_info_1, &es9p_initiate_authentication_resp);
    if (ret < 0)
        goto out;

    transaction_id = strdup(es9p_initiate_authentication_resp.transaction_id);
    es10b_authenticate_server_param.b64_server_signed_1 = es9p_initiate_authentication_resp.b64_server_signed_1;
    es10b_authenticate_server_param.b64_server_signature_1 = es9p_initiate_authentication_resp.b64_server_signature_1;
    es10b_authenticate_server_param.b64_euicc_ci_pkid_to_be_used = es9p_initiate_authentication_resp.b64_euicc_ci_pkid_to_be_used;
    es10b_authenticate_server_param.b64_server_certificate = es9p_initiate_authentication_resp.b64_server_certificate;
    es10b_authenticate_server_param.matchingId = _matching_id;
    es10b_authenticate_server_param.imei = _imei;
    es10b_authenticate_server_param.tac = NULL;

    (*env)->CallVoidMethod(env, callback, on_state_update, download_state_authenticating);
    ret = es10b_authenticate_server(ctx, &b64_authenticate_server_response, &es10b_authenticate_server_param);
    if (ret < 0)
        goto out;

    ret = es9p_authenticate_client(ctx, _smdp, transaction_id, b64_authenticate_server_response, &es9p_authenticate_client_resp);
    if (ret < 0)
        goto out;

    es10b_prepare_download_param.b64_smdp_signed_2 = es9p_authenticate_client_resp.b64_smdp_signed_2;
    es10b_prepare_download_param.b64_smdp_signature_2 = es9p_authenticate_client_resp.b64_smdp_signature_2;
    es10b_prepare_download_param.b64_smdp_certificate = es9p_authenticate_client_resp.b64_smdp_certificate;
    es10b_prepare_download_param.hexstr_transcation_id = transaction_id;
    es10b_prepare_download_param.str_checkcode = _confirmation_code;

    (*env)->CallVoidMethod(env, callback, on_state_update, download_state_downloading);
    ret = es10b_prepare_download(ctx, &b64_prepare_download_response, &es10b_prepare_download_param);
    if (ret < 0)
        goto out;

    ret = es9p_get_bound_profile_package(ctx, _smdp, transaction_id, b64_prepare_download_response, &es9p_get_bound_profile_package_resp);
    if (ret < 0)
        goto out;

    (*env)->CallVoidMethod(env, callback, on_state_update, download_state_finalizing);
    ret = es10b_load_bound_profile_package(ctx, es9p_get_bound_profile_package_resp.b64_bpp);

out:
    free(b64_authenticate_server_response);
    free(b64_prepare_download_response);
    free(b64_euicc_info_1);
    free(b64_euicc_challenge);
    free(transaction_id);
    if (_confirmation_code != NULL)
        (*env)->ReleaseStringUTFChars(env, confirmation_code, _confirmation_code);
    if (_matching_id != NULL)
        (*env)->ReleaseStringUTFChars(env, matching_id, _matching_id);
    (*env)->ReleaseStringUTFChars(env, smdp, _smdp);
    if (_imei != NULL)
        (*env)->ReleaseStringUTFChars(env, imei, _imei);
    return ret;
}
