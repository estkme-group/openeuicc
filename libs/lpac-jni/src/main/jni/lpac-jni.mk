LOCAL_PATH := $(call my-dir)

# function to find all *.c files under a directory
define all-c-files-under
$(patsubst $(LOCAL_PATH)/%,%, \
  $(wildcard $(LOCAL_PATH)/$(strip $(1))/*.c) \
 )
endef

include $(CLEAR_VARS)
# libcjson
LOCAL_MODULE := lpac-cjson
LOCAL_SRC_FILES := \
	$(call all-c-files-under, lpac/cjson)
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
# libeuicc component from lpac, which contains the actual implementation
LOCAL_MODULE := lpac-euicc
LOCAL_STATIC_LIBRARIES := lpac-cjson
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/lpac
LOCAL_SRC_FILES := \
	$(call all-c-files-under, lpac/euicc)
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := lpac-jni
LOCAL_STATIC_LIBRARIES := lpac-euicc
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/lpac
LOCAL_SRC_FILES := \
	$(call all-c-files-under, lpac-jni)
include $(BUILD_SHARED_LIBRARY)