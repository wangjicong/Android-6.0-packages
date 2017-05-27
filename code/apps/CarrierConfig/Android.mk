LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := telephony-common

LOCAL_PACKAGE_NAME := CarrierConfig
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

#SPRD: [Bug475223] Build version code based on seconds since 1970-01-01 00:00:00 UTC.
#In order to make sure carrier config data cache stored in user equipment can be updated after OTA.
LOCAL_AAPT_FLAGS := --version-code $(shell date +%s)

include $(BUILD_PACKAGE)

# This finds and builds the test apk as well, so a single make does both.
include $(call all-makefiles-under,$(LOCAL_PATH))
