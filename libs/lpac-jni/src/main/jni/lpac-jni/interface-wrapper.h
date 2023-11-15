#pragma once
#undef NDEBUG
#include <assert.h>
#include <euicc/interface.h>

extern struct euicc_apdu_interface apdu_interface_wrapper;
extern struct euicc_http_interface http_interface_wrapper;

void interface_wrapper_init();

#define LPAC_JNI_ASSERT_CTX assert(jni_ctx != NULL)
#define LPAC_JNI_SETUP_ENV \
    JNIEnv *env; \
    (*jvm)->AttachCurrentThread(jvm, &env, NULL)