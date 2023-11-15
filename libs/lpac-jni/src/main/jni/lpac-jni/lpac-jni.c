#include <euicc/interface.h>
#include <malloc.h>
#include <string.h>
#include "lpac-jni.h"
#include "interface-wrapper.h"

pthread_mutex_t global_lock = PTHREAD_MUTEX_INITIALIZER;
struct lpac_jni_ctx *jni_ctx = NULL;
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
    LPAC_JNI_BEGIN;
    struct lpac_jni_ctx *_ctx = malloc(sizeof(struct lpac_jni_ctx));
    memset(_ctx, 0, sizeof(struct lpac_jni_ctx));
    _ctx->ctx.interface.apdu = &apdu_interface_wrapper;
    _ctx->ctx.interface.http = &http_interface_wrapper;
    _ctx->apdu_interface = (*env)->NewGlobalRef(env, apdu_interface);
    _ctx->http_interface = (*env)->NewGlobalRef(env, http_interface);
    LPAC_JNI_END((jlong) _ctx);
}

JNIEXPORT void JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_destroyContext(JNIEnv *env, jobject thiz, jlong handle) {
    LPAC_JNI_BEGIN;
    struct lpac_jni_ctx *_ctx = (struct lpac_jni_ctx *) handle;
    (*env)->DeleteGlobalRef(env, _ctx->apdu_interface);
    (*env)->DeleteGlobalRef(env, _ctx->http_interface);
    if (jni_ctx == _ctx) {
        jni_ctx = NULL;
    }
    free(_ctx);
    LPAC_JNI_END0;
}

JNIEXPORT void JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_setCurrentContext(JNIEnv *env, jobject thiz, jlong handle) {
    LPAC_JNI_BEGIN;
    jni_ctx = (struct lpac_jni_ctx *) handle;
    LPAC_JNI_END0;
}