#pragma once
#include <euicc/euicc.h>
#include <pthread.h>
#include <jni.h>

struct lpac_jni_ctx {
    jobject apdu_interface;
    jobject http_interface;
};

#define LPAC_JNI_CTX(ctx) ((struct lpac_jni_ctx *) ctx->userdata)
#define LPAC_JNI_SETUP_ENV \
    JNIEnv *env; \
    (*jvm)->AttachCurrentThread(jvm, &env, NULL)

extern JavaVM *jvm;
extern jclass string_class;

jstring toJString(JNIEnv *env, const char *pat);