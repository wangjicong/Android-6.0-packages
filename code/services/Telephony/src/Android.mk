ifndef PDK_FUSION_PLATFORM_ZIP
LOCAL_PATH:= $(call my-dir)
# Build the test package
include $(call all-makefiles-under,$(LOCAL_PATH))
endif