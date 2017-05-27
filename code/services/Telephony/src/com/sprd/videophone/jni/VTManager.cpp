/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

//------------- for log BEGIN --------------------

#define LOG_NDEBUG 0
#define LOG_TAG "VTManager-JNI"

#include <utils/Log.h>

#define LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG_TAG,__VA_ARGS__)
//------------- for log END --------------------

//------------- for data type BEGIN --------------------
#include <stdint.h>
#include <stdlib.h> // for free, malloc, etc
#include <string.h>
#include <jni.h>
#include "JNIHelp.h"
#include <utils/threads.h>
#include <utils/Mutex.h>
#include <assert.h>
#include <limits.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>

#include "../libsprdstreamrecoder/SprdStreamRecorder.h"

// ----------------------------------------------------------------------------
using namespace android;
//namespace android {

// ----------------------------------------------------------------------------
#define CHECK_RT(val)\
    do { \
        status_t r = val; \
        if (r != OK) return r; \
    } while(0)

struct vtmgr_fields_t {
    jfieldID mRecorderThread;
    jfieldID mStopWaitRequestForAT;
    jfieldID m_Mpeg4Header;
    jfieldID m_iMpeg4Header_size;
};
static vtmgr_fields_t vtmgr_fields;
static int64_t mStartSysTime = 0;

struct recorder_fields_t {
    jfieldID mRecorder;
};
static recorder_fields_t recorder_fields;

static Mutex sLock;
//------------- for data type END --------------------

static SprdStreamRecorder* getRecorder(JNIEnv* env, jobject thiz)
{
    Mutex::Autolock l(sLock);
    jobject recorderObj = env->GetObjectField(thiz, vtmgr_fields.mRecorderThread);
    SprdStreamRecorder* const p = (SprdStreamRecorder*)env->GetObjectField(recorderObj, recorder_fields.mRecorder);
    return p;
}

static SprdStreamRecorder* setRecorder(JNIEnv* env, jobject thiz, const SprdStreamRecorder* recorder)
{
    Mutex::Autolock l(sLock);
    jobject recorderObj = env->GetObjectField(thiz, vtmgr_fields.mRecorderThread);
    SprdStreamRecorder* old = (SprdStreamRecorder*)env->GetObjectField(recorderObj, recorder_fields.mRecorder);
    /*if (recorder.get()) {
        recorder->incStrong(thiz);
    }
    if (old != 0) {
        old->decStrong(thiz);
    }*/
    env->SetObjectField(recorderObj, recorder_fields.mRecorder, (jobject)recorder);
    return old;
}

//----------------------------------------------------

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/sprd/videophone/vtmanager/VTManager.java
 */
const int AT_NONE = 0;
const int AT_TIMEOUT = -1;
const int AT_SELECT_ERR = -2;
const int AT_UPBUFFER_EMPTY = 1;
const int AT_REQUEST_IFRAME = 2;
const int AT_BOTH = 3;
int vt_pipe_upbuffer_empty = -1;
#define MAX(a,b) ((a)>(b) ? (a):(b))

jint
native_waitRequestForAT(JNIEnv *env, jobject thiz)
{
    //LOGV("---------native_waitRequestForAT---------");
    int retval = AT_NONE;
    if (vt_pipe_upbuffer_empty < 0) vt_pipe_upbuffer_empty = open("/dev/rpipe/ril.vt.0", O_RDWR);
    if (vt_pipe_upbuffer_empty > 0){
        do {
            struct timeval tv = {0 , 200*1000};
            tv.tv_sec = 0;
            tv.tv_usec = 200 * 1000;
            fd_set rfds;
            FD_ZERO(&rfds);
            FD_SET(vt_pipe_upbuffer_empty, &rfds);

            retval = select(vt_pipe_upbuffer_empty+1, &rfds, NULL, NULL, &tv);
            if (retval == -1) {
                LOGE("select err");
                retval = AT_SELECT_ERR;
            } else if (retval > 0) {
                ssize_t len = 0;
                char buf[128];
                //LOGV("read vt_pipe, retval: %d", retval);
                if (FD_ISSET(vt_pipe_upbuffer_empty, &rfds)) {
                    len = read(vt_pipe_upbuffer_empty, buf, sizeof(buf) - 1);
                    retval = AT_UPBUFFER_EMPTY;
                }
                break;
            } else {
                //LOGE("select timeout");
                retval = AT_TIMEOUT;
            }
            jboolean stop = env->GetBooleanField(thiz, vtmgr_fields.mStopWaitRequestForAT);
            bool bStop = stop;
            if (bStop) {
                LOGE("bStop");
                break;
            }
        } while (1);
    }else {
        LOGE("vt_pipe_upbuffer_empty: %d", vt_pipe_upbuffer_empty);
        LOGE("vt_pipe errno: %d, %s", errno, strerror(errno));
    }
    return retval;
}
jint native_closePipe(JNIEnv *env, jobject thiz)
{
    if (vt_pipe_upbuffer_empty > 0) {
        close(vt_pipe_upbuffer_empty);
    }
    vt_pipe_upbuffer_empty = -1 ;
   return 0;
}

#define EXTRACT_RGB565(b, g, r, data) \
    r = ((data) & 31); \
    g = ((data >> 5) & 63); \
    b = ((data >> 11) & 31 );

#define RGB_16_SIZE 2
#define CLIP(v) ( (v)<0 ? 0 : ((v) > 255 ? 255 : (v)) )
#define GENY16(r, g, b) CLIP((( (80593 * r)+(77855 * g)+(30728 * b)) >> 15))
#define GENU16(r, g, b) CLIP(128+ ( ( -(45483 * r)-(43936 * g)+(134771 * b)) >> 15 ))
#define GENV16(r, g, b) CLIP(128+ ( ( (134771 * r)-(55532 * g)-(21917 * b)) >> 15  ))

void RGB565toYUV420(unsigned char *pIn, unsigned char *pOut, int height, int width)
{
    LOGV("RGB565toYUV420");

    int   col, row;
    unsigned char     *pu8_yn, *pu8_ys, *pu8_uv;
    unsigned char     *pu8_y_data, *pu8_uv_data;
    unsigned char     *pu8_rgbn_data, *pu8_rgbn;
    unsigned short   u16_pix1, u16_pix2, u16_pix3, u16_pix4;

    int    i32_r00, i32_r01, i32_r10, i32_r11;
    int    i32_g00, i32_g01, i32_g10, i32_g11;
    int    i32_b00, i32_b01, i32_b10, i32_b11;

    int    i32_y00, i32_y01, i32_y10, i32_y11;
    int    i32_u00, i32_u01, i32_u10, i32_u11;
    int    i32_v00, i32_v01, i32_v10, i32_v11;

    pu8_rgbn_data   = pIn;
    pu8_y_data = pOut;
    pu8_uv_data = pOut + height*width ;

    for(row = height; row != 0; row-=2 ){
        /* Current Y plane row pointers */
        pu8_yn = pu8_y_data;
        /* Next Y plane row pointers */
        pu8_ys = pu8_yn + width;
        /* Current U plane row pointer */
        pu8_uv = pu8_uv_data;

        pu8_rgbn = pu8_rgbn_data;

        for(col = width; col != 0; col-=2){
             /* Get four RGB 565 samples from input data */
            u16_pix1 = *( (unsigned short *) pu8_rgbn);
            u16_pix2 = *( (unsigned short *) (pu8_rgbn + RGB_16_SIZE));
            u16_pix3 = *( (unsigned short *) (pu8_rgbn + width*RGB_16_SIZE));
            u16_pix4 = *( (unsigned short *) (pu8_rgbn + width*RGB_16_SIZE + RGB_16_SIZE));
            /* Unpack RGB565 to 8bit R, G, B */
            /* (x,y) */
            EXTRACT_RGB565(i32_r00,i32_g00,i32_b00,u16_pix1);
            /* (x+1,y) */
            EXTRACT_RGB565(i32_r10,i32_g10,i32_b10,u16_pix2);
            /* (x,y+1) */
            EXTRACT_RGB565(i32_r01,i32_g01,i32_b01,u16_pix3);
            /* (x+1,y+1) */
            EXTRACT_RGB565(i32_r11,i32_g11,i32_b11,u16_pix4);

            /* Convert RGB value to YUV */
            i32_u00 = GENU16(i32_r00, i32_g00, i32_b00);
            i32_v00 = GENV16(i32_r00, i32_g00, i32_b00);
            /* luminance value */
            i32_y00 = GENY16(i32_r00, i32_g00, i32_b00);

            i32_u10 = GENU16(i32_r10, i32_g10, i32_b10);
            i32_v10 = GENV16(i32_r10, i32_g10, i32_b10);
            /* luminance value */
            i32_y10 = GENY16(i32_r10, i32_g10, i32_b10);

            i32_u01 = GENU16(i32_r01, i32_g01, i32_b01);
            i32_v01 = GENV16(i32_r01, i32_g01, i32_b01);
            /* luminance value */
            i32_y01 = GENY16(i32_r01, i32_g01, i32_b01);

            i32_u11 = GENU16(i32_r11, i32_g11, i32_b11);
            i32_v11 = GENV16(i32_r11, i32_g11, i32_b11);
            /* luminance value */
            i32_y11 = GENY16(i32_r11, i32_g11, i32_b11);

            /* Store luminance data */
            pu8_yn[0] = (unsigned char)i32_y00;
            pu8_yn[1] = (unsigned char)i32_y10;
            pu8_ys[0] = (unsigned char)i32_y01;
            pu8_ys[1] = (unsigned char)i32_y11;

            /* Store chroma data */
            pu8_uv[0] = (unsigned char)((i32_u00 + i32_u01 + i32_u10 + i32_u11 + 2) >> 2);
            pu8_uv[1] = (unsigned char)((i32_v00 + i32_v01 + i32_v10 + i32_v11 + 2) >> 2);

            /* Prepare for next column */
            pu8_rgbn += 2*RGB_16_SIZE;
             /* Update current Y plane line pointer*/
            pu8_yn += 2;
            /* Update next Y plane line pointer*/
            pu8_ys += 2;
            /* Update U plane line pointer*/
            pu8_uv +=2;
        }
        /* Prepare pointers for the next row */
        pu8_y_data += width*2;
        pu8_uv_data += width;//width*2;
        pu8_rgbn_data += width*2*RGB_16_SIZE;
    }
}

static void writeToFile(const char* fn, void* data, int len){
    static int iWrite = 0;
    if (iWrite >= 2)
        return;
    LOGE("fhy: writeToFile(%s) len: %d", fn, len);
    FILE* file = fopen(fn,"ab");
    if (file == NULL){
        LOGE("fhy: fopen(%s) fail: %d", fn, file);
        return;
    }
    int wLen = fwrite(data, 1, len, file);
    LOGE("fhy: fwrite(), wLen: %d", wLen);
    fclose(file);
    iWrite++;
}

jint
native_RGB565toYUV420(JNIEnv *env, jobject thiz, jbyteArray pJIn, jint size, jbyteArray pJOut,
    jint height, jint width)
{
    LOGV("native_RGB565toYUV420, size: %d", size);
    unsigned char* pInBytes =  (unsigned char*)new char[size];
    env->GetByteArrayRegion(pJIn, 0, size, (jbyte*)pInBytes);
    if (pInBytes == NULL) {
        LOGE("native_RGB565toYUV420, pInBytes is null");
        return -2;
    }
    //writeToFile("/data/fhy_in", pInBytes, size);
    unsigned char* pOutBytes = (unsigned char*)new char[width*height*2];
    RGB565toYUV420((unsigned char *)pInBytes, pOutBytes, height, width);
    //writeToFile("/data/fhy_out", pOutBytes, 176*144*1.5);
    env->SetByteArrayRegion(pJOut, 0, 38016, (jbyte*)pOutBytes);
    delete [] pInBytes;
    delete [] pOutBytes;
    return 0;
}

static status_t prepareRecorder(SprdStreamRecorder* recorder, int type, const char* fn, int videoType) {
    LOGV("prepareRecorder E");
    if (recorder == NULL) {
        LOGE("mediaphone: recorder is not initialized");
        return NO_INIT;
    }

    if ((type == 0) || (type == 2)){
        //CHECK_RT(recorder->setVideoFrameRate(15));
        CHECK_RT(recorder->setVideoSize(176, 144));
        CHECK_RT(recorder->setParameters(String8("video-param-encoding-bitrate=48000")));
        CHECK_RT(recorder->setVideoEncoder((videoType == 1)?VIDEO_ENCODER_H263:VIDEO_ENCODER_MPEG_4_SP));
    }
    if ((type == 0) || (type == 1)){
        CHECK_RT(recorder->setAudioEncoder(AUDIO_ENCODER_AMR_NB));
    }
    CHECK_RT(recorder->setOutputFormat(OUTPUT_FORMAT_THREE_GPP));
    CHECK_RT(recorder->setOutputFile(fn));
    //CHECK_RT(recorder->reset());
    LOGV("prepareRecorder X");
    return OK;
}

static status_t
native_enableRecord(JNIEnv *env, jobject thiz, jboolean isEnable, int type, jstring fileName, int videoType)
{
    LOGV("native_enableRecord: isEnable %d, type: %d", isEnable, type);
    SprdStreamRecorder* recorder = getRecorder(env, thiz);
    if (isEnable) {
        const char *fn = env->GetStringUTFChars(fileName, NULL);
        if (fn == NULL) {
            jniThrowException(env, "java/lang/IllegalStateException", "file name is null");
            return INVALID_OPERATION;
        }
        if (recorder != NULL) {
            LOGE("old recorder hasn't been released");
            recorder->stop();
            recorder->close();
            delete recorder;
            recorder = NULL;
        }
        recorder = new SprdStreamRecorder();
        if (recorder == NULL) {
            jniThrowException(env, "java/lang/RuntimeException", "Failed to new SprdStreamRecorder");
            return NO_MEMORY;
        }
        setRecorder(env, thiz, recorder);
        CHECK_RT(prepareRecorder(recorder, type, fn, videoType));
        if (videoType != 1) { // mpeg4
            int iMpeg4Header_size = env->GetIntField(thiz, vtmgr_fields.m_iMpeg4Header_size);
            jbyteArray jMpeg4Header = (jbyteArray)env->GetObjectField(thiz, vtmgr_fields.m_Mpeg4Header);
            unsigned char* pMpeg4Header =  (unsigned char*)new char[iMpeg4Header_size];
            env->GetByteArrayRegion(jMpeg4Header, 0, iMpeg4Header_size, (jbyte*)pMpeg4Header);
            if (pMpeg4Header == NULL) {
                LOGE("native_enableRecord, pMpeg4Header is null");
                return INVALID_OPERATION;
            }
            LOGV("native_enableRecord: iMpeg4Header_size %d, pMpeg4Header: 0x%x, 0x%x", iMpeg4Header_size, pMpeg4Header[0], pMpeg4Header[1]);
            recorder->SetCodecSpecificData(pMpeg4Header, iMpeg4Header_size);
            delete []pMpeg4Header;
        }
        mStartSysTime = 0;
        CHECK_RT(recorder->start());
    } else {
        if (recorder == NULL) {
         //   jniThrowException(env, "java/lang/IllegalStateException", "recorder is null");
            LOGV (" java/lang/IllegalStateException recorder is null");
            return INVALID_OPERATION;
        }
        recorder->stop();
        LOGV("recorder stop ok");
        recorder->close();
        delete recorder;
        setRecorder(env, thiz, NULL);
        mStartSysTime = 0;
    }
    LOGV("native_enableRecord X");
    return OK;
}

static status_t
native_writeAudio(JNIEnv *env, jobject thiz, jobject pJIn, jint size, jlong timeStamp) {
    LOGV("native_writeAudio, size: %d, timeStamp: %ld", size, timeStamp);
    if ((pJIn == NULL) || (size <= 0)) {
        LOGE("native_writeAudio, data is invalid");
        return INVALID_OPERATION;
    }
    SprdStreamRecorder* recorder = getRecorder(env, thiz);
    if (recorder == NULL) {
        LOGE("native_writeAudio, recorder is null");
        return INVALID_OPERATION;
    }

    // direct buffer and direct access supported?
    jbyte* pInBytes = (jbyte*) env->GetDirectBufferAddress(pJIn);
    if(pInBytes==NULL) {
        LOGE("Buffer direct access is not supported, can't record");
        return INVALID_OPERATION;
    }

    buffer_item item = {0};
    item.data = (uint8_t *)pInBytes;
    item.len = size;
    item.timestap = timeStamp;
    recorder->writeAudio(&item, size);
    return OK;
}

static status_t
native_writeVideo(JNIEnv *env, jobject thiz, jbyteArray pJIn, jint size) {
    LOGV("native_writeVideo, size: %d", size);
    SprdStreamRecorder* recorder = getRecorder(env, thiz);
    if (recorder == NULL) {
        LOGE("native_writeVideo, recorder is null");
        return INVALID_OPERATION;
    }
    unsigned char* pInBytes =  (unsigned char*)new char[size];
    env->GetByteArrayRegion(pJIn, 0, size, (jbyte*)pInBytes);
    if (pInBytes == NULL) {
        LOGE("native_writeVideo, pInBytes is null");
        return INVALID_OPERATION;
    }
    int64_t timeStamp = 0;
    if (mStartSysTime == 0) {
        mStartSysTime = systemTime();
    } else {
        timeStamp = systemTime() - mStartSysTime;
    }

    buffer_item item = {0};
    item.data = (uint8_t *)pInBytes;
    item.len = size;
    item.timestap = nanoseconds_to_microseconds(timeStamp);//timestamp;
    recorder->writeVideo(&item, size);

    delete  []pInBytes;
    return OK;
}
// ----------------------------------------------------------------------------

static JNINativeMethod gMethods[] = {
    {"native_waitRequestForAT",       "()I",          (void *)native_waitRequestForAT},
    {"native_closePipe",       "()I",          (void *)native_closePipe},
    {"native_RGB565toYUV420",    "([BI[BII)I",       (void *)native_RGB565toYUV420},
    {"native_enableRecord",        "(ZILjava/lang/String;I)I",   (void *)native_enableRecord},
    {"native_writeAudio",        "(Ljava/lang/Object;IJ)I",   (void *)native_writeAudio},
    {"native_writeVideo",        "([BI)I",   (void *)native_writeVideo},
};

static const char* const kClassPathName = "com/sprd/videophone/vtmanager/VTManager";
static const char* const kRecorderClassPathName = "com/sprd/videophone/vtmanager/RecorderThread";

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;
    jclass clazz_vtmgr = NULL;
    jclass clazz_recorder = NULL;

    LOGV("JNI_OnLoad E");
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    clazz_vtmgr = env->FindClass(kClassPathName);
    if (clazz_vtmgr == NULL) {
        ALOGE("Can't find %s", kClassPathName);
        goto bail;
    }

    clazz_recorder = env->FindClass(kRecorderClassPathName);
    if (clazz_recorder == NULL) {
        ALOGE("Can't find %s", kRecorderClassPathName);
        goto bail;
    }
    env->ExceptionClear();

    if (env->RegisterNatives(clazz_vtmgr, gMethods, NELEM(gMethods)) != JNI_OK)
        goto bail;

    vtmgr_fields.mRecorderThread = env->GetFieldID(clazz_vtmgr, "mRecorderThread", "Lcom/sprd/videophone/vtmanager/RecorderThread;");
    if (vtmgr_fields.mRecorderThread == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "Can't find VTManager.mRecorderThread");
        return result;
    }

    vtmgr_fields.mStopWaitRequestForAT = env->GetFieldID(clazz_vtmgr, "mStopWaitRequestForAT", "Z");
    if (vtmgr_fields.mStopWaitRequestForAT == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "Can't find MediaPhone.mStopWaitRequestForAT");
        return result;
    }

    vtmgr_fields.m_Mpeg4Header = env->GetFieldID(clazz_vtmgr, "m_Mpeg4Header", "[B");
    if (vtmgr_fields.m_Mpeg4Header == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "Can't find VTManager.m_Mpeg4Header");
        return result;
    }

    vtmgr_fields.m_iMpeg4Header_size = env->GetFieldID(clazz_vtmgr, "m_iMpeg4Header_size", "I");
    if (vtmgr_fields.m_iMpeg4Header_size == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "Can't find VTManager.m_iMpeg4Header_size");
        return result;
    }

    recorder_fields.mRecorder = env->GetFieldID(clazz_recorder, "mRecorder", "Ljava/lang/Object;");
    if (recorder_fields.mRecorder == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "Can't find RecorderThread.mRecorder");
        return result;
    }
    result = JNI_VERSION_1_4;

bail:
    env->DeleteLocalRef(clazz_vtmgr);
    env->DeleteLocalRef(clazz_recorder);
    return result;
}

//}; // namespace android
