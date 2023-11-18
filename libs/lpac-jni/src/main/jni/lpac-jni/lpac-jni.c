#include <euicc/interface.h>
#include <malloc.h>
#include <string.h>
#include "lpac-jni.h"
#include "interface-wrapper.h"

JavaVM  *jvm = NULL;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;
    interface_wrapper_init();
    return 1;
}

JNIEXPORT jlong JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_createContext(JNIEnv *env, jobject thiz,
                                                  jobject apdu_interface,
                                                  jobject http_interface) {
    struct euicc_ctx *ctx = malloc(sizeof(struct euicc_ctx));
    struct lpac_jni_ctx *_ctx = malloc(sizeof(struct lpac_jni_ctx));
    memset(ctx, 0, sizeof(struct lpac_jni_ctx));
    memset(_ctx, 0, sizeof(struct lpac_jni_ctx));
    ctx->interface.apdu = &lpac_jni_apdu_interface;
    ctx->interface.http = &lpac_jni_apdu_interface;
    _ctx->apdu_interface = (*env)->NewGlobalRef(env, apdu_interface);
    _ctx->http_interface = (*env)->NewGlobalRef(env, http_interface);
    ctx->userdata = (void *) _ctx;
    return (jlong) ctx;
}

JNIEXPORT void JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_destroyContext(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    struct lpac_jni_ctx *_ctx = LPAC_JNI_CTX(ctx);
    (*env)->DeleteGlobalRef(env, _ctx->apdu_interface);
    (*env)->DeleteGlobalRef(env, _ctx->http_interface);
    free(_ctx);
    free(ctx);
}