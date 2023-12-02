LOCAL_PATH := $(call my-dir)

# function to find all *.c files under a directory
# Detecting the operating system
ifeq ($(OS),Windows_NT)
# Windows-specific commands
define all-c-files-under
  $(patsubst .\%,%, \
    $(shell cd $(LOCAL_PATH) & \
            dir /b/s $(subst /,\,$(1))\*.c) \
   )
endef
else
# UNIX commands
define all-c-files-under
  $(patsubst ./%,%, \
    $(shell cd $(LOCAL_PATH) ; \
            find $(1) -name "*.c" -and -not -name ".*" -maxdepth 1) \
   )
endef
endif

include $(CLEAR_VARS)
LOCAL_SHORT_COMMANDS := true
APP_SHORT_COMMANDS := true

# libcjson
LOCAL_MODULE := lpac-cjson
LOCAL_SRC_FILES := \
	$(call all-c-files-under, lpac/cjson)
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SHORT_COMMANDS := true
APP_SHORT_COMMANDS := true

# libasn1c, the ASN parser component from lpac
LOCAL_MODULE := lpac-asn1c
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/lpac/euicc/asn1c
LOCAL_SRC_FILES := \
	$(call all-c-files-under, lpac/euicc/asn1c/asn1)
LOCAL_CFLAGS := -DHAVE_CONFIG_H
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SHORT_COMMANDS := true
APP_SHORT_COMMANDS := true

# libeuicc component from lpac, which contains the actual implementation
LOCAL_MODULE := lpac-euicc
LOCAL_STATIC_LIBRARIES := lpac-asn1c lpac-cjson
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/lpac
LOCAL_SRC_FILES := \
	$(call all-c-files-under, lpac/euicc)
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SHORT_COMMANDS := true
APP_SHORT_COMMANDS := true

LOCAL_MODULE := lpac-jni
LOCAL_STATIC_LIBRARIES := lpac-euicc
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/lpac
LOCAL_SRC_FILES := \
	lpac-jni/lpac-jni.c \
	lpac-jni/lpac-download.c \
	lpac-jni/interface-wrapper.c
include $(BUILD_SHARED_LIBRARY)