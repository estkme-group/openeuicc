#include <string.h>
#include <malloc.h>
#include "interface-wrapper.h"
#include "lpac-jni.h"

jmethodID method_apdu_connect;
jmethodID method_apdu_disconnect;
jmethodID method_apdu_logical_channel_open;
jmethodID method_apdu_logical_channel_close;
jmethodID method_apdu_transmit;

jmethodID method_http_transmit;

jfieldID field_resp_rcode;
jfieldID field_resp_data;

void interface_wrapper_init() {
    LPAC_JNI_SETUP_ENV;
    jclass apdu_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/ApduInterface");
    method_apdu_connect = (*env)->GetMethodID(env, apdu_class, "connect", "()V");
    method_apdu_disconnect = (*env)->GetMethodID(env, apdu_class, "disconnect", "()V");
    method_apdu_logical_channel_open = (*env)->GetMethodID(env, apdu_class, "logicalChannelOpen", "([B)I");
    method_apdu_logical_channel_close = (*env)->GetMethodID(env, apdu_class, "logicalChannelClose", "(I)V");
    method_apdu_transmit = (*env)->GetMethodID(env, apdu_class, "transmit", "([B)[B");

    jclass http_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/HttpInterface");
    method_http_transmit = (*env)->GetMethodID(env, http_class, "transmit",
                                               "(Ljava/lang/String;[B)Lnet/typeblog/lpac_jni/HttpInterface$HttpResponse;");

    jclass resp_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/HttpInterface$HttpResponse");
    field_resp_rcode = (*env)->GetFieldID(env, resp_class, "rcode", "I");
    field_resp_data = (*env)->GetFieldID(env, resp_class, "data", "[B");
}

static int apdu_interface_connect(void) {
    LPAC_JNI_BEGIN;
    LPAC_JNI_ASSERT_CTX;
    LPAC_JNI_SETUP_ENV;
    (*env)->CallVoidMethod(env, jni_ctx->apdu_interface, method_apdu_connect);
    LPAC_JNI_END(!((*env)->ExceptionCheck(env) == JNI_FALSE));
}

static void apdu_interface_disconnect(void) {
    LPAC_JNI_BEGIN;
    LPAC_JNI_ASSERT_CTX;
    LPAC_JNI_SETUP_ENV;
    (*env)->CallVoidMethod(env, jni_ctx->apdu_interface, method_apdu_disconnect);
    LPAC_JNI_END0;
}

static int apdu_interface_logical_channel_open(const uint8_t *aid, uint8_t aid_len) {
    LPAC_JNI_BEGIN;
    LPAC_JNI_ASSERT_CTX;
    LPAC_JNI_SETUP_ENV;
    jbyteArray jbarr = (*env)->NewByteArray(env, aid_len);
    (*env)->SetByteArrayRegion(env, jbarr, 0, aid_len, (const jbyte *) aid);
    jint ret = (*env)->CallIntMethod(env, jni_ctx->apdu_interface, method_apdu_logical_channel_open, jbarr);
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
        LPAC_JNI_END(-1);
    } else {
        LPAC_JNI_END(ret);
    }
}

static void apdu_interface_logical_channel_close(uint8_t channel) {
    LPAC_JNI_BEGIN;
    LPAC_JNI_ASSERT_CTX;
    LPAC_JNI_SETUP_ENV;
    (*env)->CallVoidMethod(env, jni_ctx->apdu_interface, method_apdu_logical_channel_close, channel);
    LPAC_JNI_END0;
}

static int apdu_interface_transmit(uint8_t **rx, uint32_t *rx_len, const uint8_t *tx, uint32_t tx_len) {
    LPAC_JNI_BEGIN;
    LPAC_JNI_ASSERT_CTX;
    LPAC_JNI_SETUP_ENV;
    jbyteArray txArr = (*env)->NewByteArray(env, tx_len);
    (*env)->SetByteArrayRegion(env, txArr, 0, tx_len, (const jbyte *) tx);
    jbyteArray ret = (jbyteArray) (*env)->CallObjectMethod(env, jni_ctx->apdu_interface, method_apdu_transmit, txArr);
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
        LPAC_JNI_END(-1);
    }
    *rx_len = (*env)->GetArrayLength(env, ret);
    *rx = malloc(*rx_len * sizeof(uint8_t));
    (*env)->GetByteArrayRegion(env, ret, 0, *rx_len, *rx);
    LPAC_JNI_END(0);
}

static int http_interface_transmit(const char *url, uint32_t *rcode, uint8_t **rx, uint32_t *rx_len, const uint8_t *tx, uint32_t tx_len) {
    LPAC_JNI_BEGIN;
    LPAC_JNI_ASSERT_CTX;
    LPAC_JNI_SETUP_ENV;
    jstring jurl = (*env)->NewString(env, url, strlen(url));
    jbyteArray txArr = (*env)->NewByteArray(env, tx_len);
    (*env)->SetByteArrayRegion(env, txArr, 0, tx_len, (const jbyte *) tx);
    jobject ret = (*env)->CallObjectMethod(env, jni_ctx->http_interface, method_http_transmit, jurl, txArr);
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
        LPAC_JNI_END(-1);
    }
    *rcode = (*env)->GetIntField(env, ret, field_resp_rcode);
    jbyteArray rxArr = (jbyteArray) (*env)->GetObjectField(env, ret, field_resp_data);
    *rx_len = (*env)->GetArrayLength(env, rxArr);
    *rx = malloc(*rx_len * sizeof(uint8_t));
    (*env)->GetByteArrayRegion(env, rxArr, 0, *rx_len, *rx);
    LPAC_JNI_END(0);
}

struct euicc_apdu_interface apdu_interface_wrapper = {
        .connect = apdu_interface_connect,
        .disconnect = apdu_interface_disconnect,
        .logic_channel_open = apdu_interface_logical_channel_open,
        .logic_channel_close = apdu_interface_logical_channel_close,
        .transmit = apdu_interface_transmit
};
struct euicc_http_interface http_interface_wrapper = {
        .transmit = http_interface_transmit
};