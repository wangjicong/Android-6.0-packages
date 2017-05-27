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
 */

#ifndef SPRDSTREAM_RECORDER_H_

#define SPRDSTREAM_RECORDER_H_

#include <utils/String8.h>
#include <utils/Vector.h>
#include <sys/types.h>
#include "SprdStreamRecoderClient.h"
#include <utils/threads.h>

namespace android {

//Please update media/java/android/media/MediaRecorder.java if the following is updated.

struct MediaSource;
struct SprdMediaWriter;
class MetaData;
class MediaProfiles;
class SprdStreamRecorderClient ;
struct AVOutputFormat ;
struct AVFormatContext;
struct AVStream ;



struct SprdStreamRecorder:public virtual RefBase  {
    SprdStreamRecorder();
    virtual ~SprdStreamRecorder();

    virtual status_t setOutputFormat(output_format of);
    virtual status_t setOutputFile(const char *path);
   status_t  setVideoSize(int width, int height);
   status_t  setAudioEncoder(audio_encoder ae) ;
   status_t  setVideoEncoder(video_encoder ve) ;
   status_t   SetCodecSpecificData(uint8_t *data,int len);

    virtual status_t setParameters(const String8& params);
    virtual status_t setListener(const sp<SprdStreamRecorderClient>& listener);

    virtual status_t start();
    virtual status_t pause();
    virtual status_t stop();
    virtual status_t close();
    virtual status_t reset();
    virtual status_t dump(int fd, const Vector<String16>& args) const;
    void   writeAudio(buffer_item *data ,int len) ;
    void    writeVideo(buffer_item *data ,int len) ;

private:
    sp<SprdMediaWriter> mWriter;
    sp<SprdStreamRecorderClient> mListener;
    int mOutputFd;
    sp<MediaSource>    audioSource  ;
    sp<MediaSource>    videoSource ;

    output_format mOutputFormat;
    audio_encoder mAudioEncoder;
    video_encoder mVideoEncoder;

    bool mUse64BitFileOffset;
    int32_t mVideoWidth, mVideoHeight;
    int64_t            m_nStartSysTime;

    int32_t mVideoBitRate;
    int32_t mAudioBitRate;
    int32_t mAudioChannels;
    int32_t mSampleRate;
    int32_t mInterleaveDurationUs;

    int32_t mMovieTimeScale;
    int32_t mVideoTimeScale;
    int32_t mAudioTimeScale;
    int64_t mMaxFileSizeBytes;
    int64_t mMaxFileDurationUs;
    int64_t mTrackEveryTimeDurationUs;
    int32_t mRotationDegrees;  // Clockwise
    int32_t mLatitudex10000;
    int32_t mLongitudex10000;
    int32_t mStartTimeOffsetMs;

    String8 mParams;


    mutable Mutex mLock;
    bool mStarted;
    bool mPause;
    AVOutputFormat *m_stFmt;
    AVFormatContext *m_stOc;
    AVStream *m_stAudio_st, *m_stVideo_st;
    uint8_t *m_AudioBuffer,*m_VideoBuffer;

    status_t setupMPEG4Recording(
        int outputFd,
        int32_t videoWidth, int32_t videoHeight,
        int32_t videoBitRate,
        int32_t *totalBitRate,
        sp<SprdMediaWriter> *mediaWriter);
    void setupMPEG4MetaData(int64_t startTimeUs, int32_t totalBitRate,
        sp<MetaData> *meta);

    status_t startMPEG4Recording();
    status_t startAMRRecording();
    status_t startAACRecording();
    status_t startRawAudioRecording();
    status_t startRTPRecording();
    status_t startMPEG2TSRecording();

   status_t setupAudioSource(const sp<SprdMediaWriter>& writer);
   status_t setupVideoSource(const sp<SprdMediaWriter>& writer);

    // Encoding parameter handling utilities
    status_t setParameter(const String8 &key, const String8 &value);

    status_t setParamAudioTimeScale(int32_t timeScale);
    status_t setParamTrackTimeStatus(int64_t timeDurationUs);
    status_t setParamInterleaveDuration(int32_t durationUs);
    status_t setParam64BitFileOffset(bool use64BitFileOffset);
    status_t setParamMaxFileDurationUs(int64_t timeUs);
    status_t setParamMaxFileSizeBytes(int64_t bytes);
    status_t setParamMovieTimeScale(int32_t timeScale);
    status_t setParamGeoDataLongitude(int64_t longitudex10000);
    status_t setParamGeoDataLatitude(int64_t latitudex10000);

    status_t setParamVideoEncodingBitRate(int32_t bitRate);
    status_t setParamVideoTimeScale(int32_t timeScale);
    status_t setParamVideoRotation(int32_t degrees);
    status_t setParamAudioEncodingBitRate(int32_t bitRate);
    status_t setParamAudioNumberOfChannels(int32_t channles);
    status_t setParamAudioSamplingRate(int32_t sampleRate);
    SprdStreamRecorder(const SprdStreamRecorder &);
    SprdStreamRecorder &operator=(const SprdStreamRecorder &);
};

}  // namespace android

#endif  // STAGEFRIGHT_RECORDER_H_

