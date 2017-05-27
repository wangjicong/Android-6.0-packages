LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES:=                         \
    SprdMPEG4Writer.cpp    \
                Utils.cpp        \
        SprdStreamSource.cpp    \
        SprdStreamRecorder.cpp \
        MediaBuffer.cpp        \
        MediaSource.cpp        \
        MetaData.cpp

LOCAL_C_INCLUDES:= \
    $(JNI_H_INCLUDE) \
        $(LOCAL_PATH)/libffmpeg


LOCAL_SHARED_LIBRARIES := \
        libutils          \
        libcutils         \

LOCAL_SHARED_LIBRARIES += \
    liblog           \
        libdl

LOCAL_CFLAGS += -Wno-multichar
LOCAL_MODULE_TAGS := optional


LOCAL_MODULE:= libsprdstreamrecoder

include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))
