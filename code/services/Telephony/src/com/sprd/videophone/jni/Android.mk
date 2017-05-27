# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
$(info "Included jni")

ifndef PDK_FUSION_PLATFORM_ZIP
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE    := libvtmanager
LOCAL_SRC_FILES := VTManager.cpp

LOCAL_SHARED_LIBRARIES := \
    libandroid_runtime \
    libnativehelper \
    libutils \
    libbinder \
    libcutils \
    libsprdstreamrecoder


LOCAL_C_INCLUDES += \
    frameworks/native/include/utils \
    frameworks/base/core/jni \
    frameworks/base/include/android_runtime \
    frameworks/av/media/libsprdstreamrecoder \
    $(JNI_H_INCLUDE)

include $(BUILD_SHARED_LIBRARY)
endif