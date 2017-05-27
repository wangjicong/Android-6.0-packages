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

#define LOG_NDEBUG 0

//#define USE_FFMPEG
#define LOG_TAG "SprdStreamRecorder"
#include <utils/Log.h>
#include <cutils/properties.h>

#include "SprdMPEG4Writer.h"
#include "SprdStreamRecorder.h"


#include "SprdStreamSource.h"
#include "Utils.h"
#include "MetaData.h"

#include <utils/Errors.h>
#include <sys/types.h>
#include <ctype.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

namespace android {

#ifdef USE_FFMPEG
extern "C" {
#include "libavformat/avformat.h"
#include "libavformat/avio.h"
#include "libavutil/avutil.h"
}
#endif

const char *MEDIA_MIMETYPE_IMAGE_JPEG = "image/jpeg";

const char *MEDIA_MIMETYPE_VIDEO_VPX = "video/x-vnd.on2.vp8";
const char *MEDIA_MIMETYPE_VIDEO_AVC = "video/avc";
const char *MEDIA_MIMETYPE_VIDEO_MPEG4 = "video/mp4v-es";
const char *MEDIA_MIMETYPE_VIDEO_H263 = "video/3gpp";
const char *MEDIA_MIMETYPE_VIDEO_MPEG2 = "video/mpeg2";
const char *MEDIA_MIMETYPE_VIDEO_RAW = "video/raw";

const char *MEDIA_MIMETYPE_AUDIO_AMR_NB = "audio/3gpp";
const char *MEDIA_MIMETYPE_AUDIO_AMR_WB = "audio/amr-wb";
const char *MEDIA_MIMETYPE_AUDIO_MPEG = "audio/mpeg";
const char *MEDIA_MIMETYPE_AUDIO_MPEG_LAYER_I = "audio/mpeg-L1";
const char *MEDIA_MIMETYPE_AUDIO_MPEG_LAYER_II = "audio/mpeg-L2";
const char *MEDIA_MIMETYPE_AUDIO_AAC = "audio/mp4a-latm";
const char *MEDIA_MIMETYPE_AUDIO_QCELP = "audio/qcelp";
const char *MEDIA_MIMETYPE_AUDIO_VORBIS = "audio/vorbis";
const char *MEDIA_MIMETYPE_AUDIO_G711_ALAW = "audio/g711-alaw";
const char *MEDIA_MIMETYPE_AUDIO_G711_MLAW = "audio/g711-mlaw";
const char *MEDIA_MIMETYPE_AUDIO_RAW = "audio/raw";
const char *MEDIA_MIMETYPE_AUDIO_IMAADPCM = "audio/ima-adpcm";
const char *MEDIA_MIMETYPE_AUDIO_FLAC = "audio/flac";
const char *MEDIA_MIMETYPE_AUDIO_AAC_ADTS = "audio/aac-adts";

const char *MEDIA_MIMETYPE_CONTAINER_MPEG4 = "video/mpeg4";
const char *MEDIA_MIMETYPE_CONTAINER_WAV = "audio/wav";
const char *MEDIA_MIMETYPE_CONTAINER_OGG = "application/ogg";
const char *MEDIA_MIMETYPE_CONTAINER_MATROSKA = "video/x-matroska";
const char *MEDIA_MIMETYPE_CONTAINER_MPEG2TS = "video/mp2ts";
const char *MEDIA_MIMETYPE_CONTAINER_AVI = "video/avi";
const char *MEDIA_MIMETYPE_CONTAINER_FLV = "video/flv";
const char *MEDIA_MIMETYPE_CONTAINER_MPEG2PS = "video/mp2p";

const char *MEDIA_MIMETYPE_CONTAINER_WVM = "video/wvm";

const char *MEDIA_MIMETYPE_TEXT_3GPP = "text/3gpp-tt";

SprdStreamRecorder::SprdStreamRecorder()
    : mWriter(NULL),
      mOutputFd(-1),
      mStarted(false),
      mPause(false)
      {
    ALOGV("Constructor");
    mOutputFormat  = OUTPUT_FORMAT_THREE_GPP;
    mVideoWidth    = 176;
    mVideoHeight   = 144;
    mVideoBitRate  = 192000;
    mSampleRate    = 8000;
    mAudioChannels = 1;
    mAudioBitRate  = 12200;
    mInterleaveDurationUs = 0;
    mUse64BitFileOffset = false;
    mMovieTimeScale  = 90000;
    mAudioTimeScale  = 90000;
    mVideoTimeScale  = 90000;
    mStartTimeOffsetMs = -1;
    mMaxFileDurationUs = 0;
    mMaxFileSizeBytes = 0;
    mTrackEveryTimeDurationUs = 0;
    mLatitudex10000 = -3600000;
    mLongitudex10000 = -3600000;
       mRotationDegrees = 0;
    m_stFmt = NULL ;
    m_stOc = NULL ;
    m_stAudio_st = NULL;
    m_stVideo_st = NULL;
    mVideoEncoder =VIDEO_ENCODER_LIST_END ;
    mAudioEncoder =AUDIO_ENCODER_LIST_END ;
    m_VideoBuffer = NULL;
    m_AudioBuffer = NULL;
    mOutputFd = -1;
    reset();
}

SprdStreamRecorder::~SprdStreamRecorder() {
    ALOGV("Destructor");
    stop();
}



status_t SprdStreamRecorder::setOutputFormat(output_format of) {
    ALOGV("setOutputFormat: %d", of);
    if (of < OUTPUT_FORMAT_DEFAULT ||
        of >= OUTPUT_FORMAT_LIST_END) {
        ALOGE("Invalid output format: %d", of);
        return BAD_VALUE;
    }
    if (of == OUTPUT_FORMAT_DEFAULT) {
        mOutputFormat = OUTPUT_FORMAT_THREE_GPP;
    } else {
        mOutputFormat = of;
    }
    return OK;
}

status_t SprdStreamRecorder::setOutputFile(const char *path) {
    ALOGE("setOutputFile(const char*) must not be called");
    // We don't actually support this at all, as the media_server process
    // no longer has permissions to create files.
#ifndef  USE_FFMPEG
   if (mOutputFd >0 )
   {
       ::close(mOutputFd) ;
    }

    mOutputFd = open(path, O_CREAT | O_LARGEFILE | O_TRUNC | O_RDWR, S_IRUSR | S_IWUSR);
    if (mOutputFd >  0)
    {
          return OK;
     }
     else
     {
           ALOGI(" SprdStreamRecorder::setOutputFile  Creat file erro ") ;
           return -EPERM;
     }
#else
      av_register_all();
      m_stFmt  = av_guess_format(NULL, path, NULL);
      if (!m_stFmt) {
       ALOGI("Could not deduce output format from file extension: using MPEG.\n");
        m_stFmt = av_guess_format("mpeg", NULL, NULL);
    }
    if (!m_stFmt) {
        ALOGI( "Could not find suitable output format\n");
         return -EPERM;
    }

     m_stOc = avformat_alloc_context();
    if (!m_stOc) {
        ALOGI( "Memory error\n");
        return -EPERM;
    }
   m_stOc->nb_streams = 0 ;
    m_stOc->oformat = m_stFmt;
   m_stFmt->flags = AVFMT_NOTIMESTAMPS ;
    snprintf(m_stOc->filename, sizeof(m_stOc->filename), "%s", path);

    if (av_set_parameters(m_stOc, NULL) < 0) {
        ALOGI( "Invalid output format parameters\n");
        return -EPERM;
    }
   /* open the output file, if needed */
    if (!(m_stFmt->flags & AVFMT_NOFILE)) {
        if (url_fopen(&m_stOc->pb, m_stOc->filename, URL_WRONLY) < 0) {
            ALOGI("Could not open '%s'\n",  m_stOc->filename);
            return -EPERM;
        }
    }
    return OK;
#endif

}

    void   SprdStreamRecorder::writeAudio(buffer_item *data ,int len)
    {
         ALOGI(" SprdStreamRecorder::writeAudio len %d,timestap = %lld",len,data->timestap) ;
         Mutex::Autolock autoLock(mLock);
     #ifndef  USE_FFMPEG
          if(audioSource != 0)
          {
                 reinterpret_cast<SprdStreamSource *>(audioSource.get())->write(data,len) ;
          }
          else
          {
                ALOGI(" SprdStreamRecorder::writeAudio erro  no audioSource");
          }
    #else
      AVPacket pkt;
      if (!m_stOc ||!m_stAudio_st)
      {
         ALOGI( "Error while writing audio frame  m_stOc = null  \n");
         return ;
      }
       ALOGI( "av_init_packet");
          av_init_packet(&pkt);
      memcpy(m_AudioBuffer, data->data, len);
          ALOGI( "memcpy  m_AudioBuffer end ");

          pkt.data = m_AudioBuffer;
      pkt.size = len ;
          pkt.stream_index = m_stAudio_st ->index;
          pkt.flags |= AV_PKT_FLAG_KEY;

         pkt.dts = pkt.pts= av_rescale(data->timestap,m_stAudio_st->codec->time_base.den,1000000);
         /* write the compressed frame in the media file */
          ALOGI( "av_interleaved_write_audioframe  stream_index = %d ,pts =%lld", pkt.stream_index, pkt.pts);

         if (av_interleaved_write_frame(m_stOc, &pkt) != 0)
     {
           ALOGI( "Error while writing audio frame\n");
          }
      m_stAudio_st->codec->frame_number++ ;
    #endif
    }
    void    SprdStreamRecorder::writeVideo(buffer_item  *data ,int len)
    {
           ALOGI(" SprdStreamRecorder::writeVideo len %d,timestap =%lld",len,data->timestap);
           Mutex::Autolock autoLock(mLock);
    #ifndef  USE_FFMPEG
           if(videoSource != 0)
           {
                reinterpret_cast<SprdStreamSource *>(videoSource.get())->write(data,len) ;
           }
           else
           {
              ALOGI(" SprdStreamRecorder::writeVideo erro  no VideoSource");
           }
     #else
      AVPacket pkt;
      if (!m_stOc ||!m_stVideo_st)
      {
         ALOGI( "Error while writing video frame  m_stOc = null  \n");
         return ;
      }
          ALOGI( "av_init_packet");
          av_init_packet(&pkt);

          ALOGI( "memcpy m_VideoBuffer end ");
          memcpy(m_VideoBuffer, data->data, len);
      video_srteam_t streambuf;
      int ret = video_stream_init(&streambuf,(unsigned char *)m_VideoBuffer,len);
           if( 0 !=ret )
       {
          ALOGI( " video_stream_init fail   \n");
       }
        int is_I_vop;
        ret =get_video_stream_info(&streambuf,&is_I_vop);

        if((ret==0)&&(is_I_vop==1)){
               pkt.flags |= AV_PKT_FLAG_KEY;
        }

          pkt.stream_index= m_stVideo_st->index;
          pkt.data= m_VideoBuffer;
      pkt.size = len ;
          pkt.dts = pkt.pts= av_rescale(data->timestap,  m_stVideo_st->codec->time_base.den,1000000);

         /* write the compressed frame in the media file */
          ALOGI( "av_interleaved_write_videoframe  stream_index = %d,pts =%lld", pkt.stream_index,pkt.pts);

          if (av_interleaved_write_frame(m_stOc, &pkt) != 0)
          {
              ALOGI( "Error while writing video  frame\n");
          }
       m_stVideo_st->codec->frame_number++ ;
       //  free(video_outbuf);
     #endif

    }

// Attempt to parse an int64 literal optionally surrounded by whitespace,
// returns true on success, false otherwise.
static bool safe_strtoi64(const char *s, int64_t *val) {
    char *end;

    // It is lame, but according to man page, we have to set errno to 0
    // before calling strtoll().
    errno = 0;
    *val = strtoll(s, &end, 10);

    if (end == s || errno == ERANGE) {
        return false;
    }

    // Skip trailing whitespace
    while (isspace(*end)) {
        ++end;
    }

    // For a successful return, the string must contain nothing but a valid
    // int64 literal optionally surrounded by whitespace.

    return *end == '\0';
}

// Return true if the value is in [0, 0x007FFFFFFF]
static bool safe_strtoi32(const char *s, int32_t *val) {
    int64_t temp;
    if (safe_strtoi64(s, &temp)) {
        if (temp >= 0 && temp <= 0x007FFFFFFF) {
            *val = static_cast<int32_t>(temp);
            return true;
        }
    }
    return false;
}

// Trim both leading and trailing whitespace from the given string.
static void TrimString(String8 *s) {
    size_t num_bytes = s->bytes();
    const char *data = s->string();

    size_t leading_space = 0;
    while (leading_space < num_bytes && isspace(data[leading_space])) {
        ++leading_space;
    }

    size_t i = num_bytes;
    while (i > leading_space && isspace(data[i - 1])) {
        --i;
    }

    s->setTo(String8(&data[leading_space], i - leading_space));
}

status_t SprdStreamRecorder::setParamAudioSamplingRate(int32_t sampleRate) {
    ALOGV("setParamAudioSamplingRate: %d", sampleRate);
    if (sampleRate <= 0) {
        ALOGE("Invalid audio sampling rate: %d", sampleRate);
        return BAD_VALUE;
    }

    // Additional check on the sample rate will be performed later.
    mSampleRate = sampleRate;
    return OK;
}

status_t SprdStreamRecorder::setParamAudioNumberOfChannels(int32_t channels) {
    ALOGV("setParamAudioNumberOfChannels: %d", channels);
    if (channels <= 0 || channels >= 3) {
        ALOGE("Invalid number of audio channels: %d", channels);
        return BAD_VALUE;
    }

    // Additional check on the number of channels will be performed later.
    mAudioChannels = channels;
    return OK;
}

status_t SprdStreamRecorder::setParamAudioEncodingBitRate(int32_t bitRate) {
    ALOGV("setParamAudioEncodingBitRate: %d", bitRate);
    if (bitRate <= 0) {
        ALOGE("Invalid audio encoding bit rate: %d", bitRate);
        return BAD_VALUE;
    }

    // The target bit rate may not be exactly the same as the requested.
    // It depends on many factors, such as rate control, and the bit rate
    // range that a specific encoder supports. The mismatch between the
    // the target and requested bit rate will NOT be treated as an error.
    mAudioBitRate = bitRate;
    return OK;
}

status_t SprdStreamRecorder::setParamVideoEncodingBitRate(int32_t bitRate) {
    ALOGV("setParamVideoEncodingBitRate: %d", bitRate);
    if (bitRate <= 0) {
        ALOGE("Invalid video encoding bit rate: %d", bitRate);
        return BAD_VALUE;
    }

    // The target bit rate may not be exactly the same as the requested.
    // It depends on many factors, such as rate control, and the bit rate
    // range that a specific encoder supports. The mismatch between the
    // the target and requested bit rate will NOT be treated as an error.
    mVideoBitRate = bitRate;
    return OK;
}

// Always rotate clockwise, and only support 0, 90, 180 and 270 for now.
status_t SprdStreamRecorder::setParamVideoRotation(int32_t degrees) {
    ALOGV("setParamVideoRotation: %d", degrees);
    if (degrees < 0 || degrees % 90 != 0) {
        ALOGE("Unsupported video rotation angle: %d", degrees);
        return BAD_VALUE;
    }
    mRotationDegrees = degrees % 360;
    return OK;
}

status_t SprdStreamRecorder::setParamMaxFileDurationUs(int64_t timeUs) {
    ALOGV("setParamMaxFileDurationUs: %lld us", timeUs);

    // This is meant for backward compatibility for MediaRecorder.java
    if (timeUs <= 0) {
        ALOGW("Max file duration is not positive: %lld us. Disabling duration limit.", timeUs);
        timeUs = 0; // Disable the duration limit for zero or negative values.
    } else if (timeUs <= 100000LL) {  // XXX: 100 milli-seconds
        ALOGE("Max file duration is too short: %lld us", timeUs);
        return BAD_VALUE;
    }

    if (timeUs <= 15 * 1000000LL) {
        ALOGW("Target duration (%lld us) too short to be respected", timeUs);
    }
    mMaxFileDurationUs = timeUs;
    return OK;
}

status_t SprdStreamRecorder::setParamMaxFileSizeBytes(int64_t bytes) {
    ALOGV("setParamMaxFileSizeBytes: %lld bytes", bytes);

    // This is meant for backward compatibility for MediaRecorder.java
    if (bytes <= 0) {
        ALOGW("Max file size is not positive: %lld bytes. "
             "Disabling file size limit.", bytes);
        bytes = 0; // Disable the file size limit for zero or negative values.
    } else if (bytes <= 1024) {  // XXX: 1 kB
        ALOGE("Max file size is too small: %lld bytes", bytes);
        return BAD_VALUE;
    }

    if (bytes <= 100 * 1024) {
        ALOGW("Target file size (%lld bytes) is too small to be respected", bytes);
    }

    mMaxFileSizeBytes = bytes;
    return OK;
}

status_t SprdStreamRecorder::setParam64BitFileOffset(bool use64Bit) {
    ALOGV("setParam64BitFileOffset: %s",
        use64Bit? "use 64 bit file offset": "use 32 bit file offset");
    mUse64BitFileOffset = use64Bit;
    return OK;
}

status_t SprdStreamRecorder::setParamTrackTimeStatus(int64_t timeDurationUs) {
    ALOGV("setParamTrackTimeStatus: %lld", timeDurationUs);
    if (timeDurationUs < 20000) {  // Infeasible if shorter than 20 ms?
        ALOGE("Tracking time duration too short: %lld us", timeDurationUs);
        return BAD_VALUE;
    }
    mTrackEveryTimeDurationUs = timeDurationUs;
    return OK;
}



status_t SprdStreamRecorder::setParamMovieTimeScale(int32_t timeScale) {
    ALOGV("setParamMovieTimeScale: %d", timeScale);

    // The range is set to be the same as the audio's time scale range
    // since audio's time scale has a wider range.
    if (timeScale < 600 || timeScale > 96000) {
        ALOGE("Time scale (%d) for movie is out of range [600, 96000]", timeScale);
        return BAD_VALUE;
    }
    mMovieTimeScale = timeScale;
    return OK;
}

status_t SprdStreamRecorder::setParamVideoTimeScale(int32_t timeScale) {
    ALOGV("setParamVideoTimeScale: %d", timeScale);

    // 60000 is chosen to make sure that each video frame from a 60-fps
    // video has 1000 ticks.
    if (timeScale < 600 || timeScale > 60000) {
        ALOGE("Time scale (%d) for video is out of range [600, 60000]", timeScale);
        return BAD_VALUE;
    }
    mVideoTimeScale = timeScale;
    return OK;
}

status_t SprdStreamRecorder::setParamAudioTimeScale(int32_t timeScale) {
    ALOGV("setParamAudioTimeScale: %d", timeScale);

    // 96000 Hz is the highest sampling rate support in AAC.
    if (timeScale < 600 || timeScale > 96000) {
        ALOGE("Time scale (%d) for audio is out of range [600, 96000]", timeScale);
        return BAD_VALUE;
    }
    mAudioTimeScale = timeScale;
    return OK;
}
status_t SprdStreamRecorder::setParamGeoDataLongitude(
    int64_t longitudex10000) {

    if (longitudex10000 > 1800000 || longitudex10000 < -1800000) {
        return BAD_VALUE;
    }
    mLongitudex10000 = longitudex10000;
    return OK;
}

status_t SprdStreamRecorder::setParamGeoDataLatitude(
    int64_t latitudex10000) {

    if (latitudex10000 > 900000 || latitudex10000 < -900000) {
        return BAD_VALUE;
    }
    mLatitudex10000 = latitudex10000;
    return OK;
}

 status_t SprdStreamRecorder::setParamInterleaveDuration(int32_t durationUs)
 {
    ALOGV("setParamInterleaveDuration: %d", durationUs);
    if (durationUs <= 500000) {           //  500 ms
        // If interleave duration is too small, it is very inefficient to do
        // interleaving since the metadata overhead will count for a significant
        // portion of the saved contents
        ALOGE("Audio/video interleave duration is too small: %d us", durationUs);
        return BAD_VALUE;
    } else if (durationUs >= 10000000) {  // 10 seconds
        // If interleaving duration is too large, it can cause the recording
        // session to use too much memory since we have to save the output
        // data before we write them out
        ALOGE("Audio/video interleave duration is too large: %d us", durationUs);
        return BAD_VALUE;
    }
    mInterleaveDurationUs = durationUs;
    return OK;
  }
status_t SprdStreamRecorder::setParameter(
        const String8 &key, const String8 &value) {
    ALOGV("setParameter: key (%s) => value (%s)", key.string(), value.string());
    if (key == "max-duration") {
        int64_t max_duration_ms;
        if (safe_strtoi64(value.string(), &max_duration_ms)) {
            return setParamMaxFileDurationUs(1000LL * max_duration_ms);
        }
    } else if (key == "max-filesize") {
        int64_t max_filesize_bytes;
        if (safe_strtoi64(value.string(), &max_filesize_bytes)) {
            return setParamMaxFileSizeBytes(max_filesize_bytes);
        }
    } else if (key == "interleave-duration-us") {
        int32_t durationUs;
        if (safe_strtoi32(value.string(), &durationUs)) {
            return setParamInterleaveDuration(durationUs);
        }
    } else if (key == "param-movie-time-scale") {
        int32_t timeScale;
        if (safe_strtoi32(value.string(), &timeScale)) {
            return setParamMovieTimeScale(timeScale);
        }
    } else if (key == "param-use-64bit-offset") {
        int32_t use64BitOffset;
        if (safe_strtoi32(value.string(), &use64BitOffset)) {
            return setParam64BitFileOffset(use64BitOffset != 0);
        }
    } else if (key == "param-geotag-longitude") {
        int64_t longitudex10000;
        if (safe_strtoi64(value.string(), &longitudex10000)) {
            return setParamGeoDataLongitude(longitudex10000);
        }
    } else if (key == "param-geotag-latitude") {
        int64_t latitudex10000;
        if (safe_strtoi64(value.string(), &latitudex10000)) {
            return setParamGeoDataLatitude(latitudex10000);
        }
    } else if (key == "param-track-time-status") {
        int64_t timeDurationUs;
        if (safe_strtoi64(value.string(), &timeDurationUs)) {
            return setParamTrackTimeStatus(timeDurationUs);
        }
    } else if (key == "audio-param-sampling-rate") {
        int32_t sampling_rate;
        if (safe_strtoi32(value.string(), &sampling_rate)) {
            return setParamAudioSamplingRate(sampling_rate);
        }
    } else if (key == "audio-param-number-of-channels") {
        int32_t number_of_channels;
        if (safe_strtoi32(value.string(), &number_of_channels)) {
            return setParamAudioNumberOfChannels(number_of_channels);
        }
    } else if (key == "audio-param-encoding-bitrate") {
        int32_t audio_bitrate;
        if (safe_strtoi32(value.string(), &audio_bitrate)) {
            return setParamAudioEncodingBitRate(audio_bitrate);
        }
    } else if (key == "audio-param-time-scale") {
        int32_t timeScale;
        if (safe_strtoi32(value.string(), &timeScale)) {
            return setParamAudioTimeScale(timeScale);
        }
    } else if (key == "video-param-encoding-bitrate") {
        int32_t video_bitrate;
        if (safe_strtoi32(value.string(), &video_bitrate)) {
            return setParamVideoEncodingBitRate(video_bitrate);
        }
    } else if (key == "video-param-rotation-angle-degrees") {
        int32_t degrees;
        if (safe_strtoi32(value.string(), &degrees)) {
            return setParamVideoRotation(degrees);
        }
     }
   else if (key == "video-param-time-scale") {
        int32_t timeScale;
        if (safe_strtoi32(value.string(), &timeScale)) {
            return setParamVideoTimeScale(timeScale);
        }
    }    else
    {
        ALOGE("setParameter: failed to find key %s", key.string());
    }
    return BAD_VALUE;
}

status_t SprdStreamRecorder::setParameters(const String8 &params) {
    ALOGV("setParameters: %s", params.string());
    const char *cparams = params.string();
    const char *key_start = cparams;
    for (;;) {
        const char *equal_pos = strchr(key_start, '=');
        if (equal_pos == NULL) {
            ALOGE("Parameters %s miss a value", cparams);
            return BAD_VALUE;
        }
        String8 key(key_start, equal_pos - key_start);
        TrimString(&key);
        if (key.length() == 0) {
            ALOGE("Parameters %s contains an empty key", cparams);
            return BAD_VALUE;
        }
        const char *value_start = equal_pos + 1;
        const char *semicolon_pos = strchr(value_start, ';');
        String8 value;
        if (semicolon_pos == NULL) {
            value.setTo(value_start);
        } else {
            value.setTo(value_start, semicolon_pos - value_start);
        }
        if (setParameter(key, value) != OK) {
            return BAD_VALUE;
        }
        if (semicolon_pos == NULL) {
            break;  // Reaches the end
        }
        key_start = semicolon_pos + 1;
    }
    return OK;
}

status_t SprdStreamRecorder::setVideoSize(int width, int height) {
    ALOGV("setVideoSize: %dx%d", width, height);
    if (width <= 0 || height <= 0) {
        ALOGE("Invalid video size: %dx%d", width, height);
        return BAD_VALUE;
    }

    // Additional check on the dimension will be performed later
    mVideoWidth = width;
    mVideoHeight = height;

    return OK;
}
status_t SprdStreamRecorder::setAudioEncoder(audio_encoder ae) {
    ALOGV("setAudioEncoder: %d", ae);
    if (ae < AUDIO_ENCODER_DEFAULT ||
        ae >= AUDIO_ENCODER_LIST_END) {
        ALOGE("Invalid audio encoder: %d", ae);
        return BAD_VALUE;
    }

    if (ae == AUDIO_ENCODER_DEFAULT) {
        mAudioEncoder = AUDIO_ENCODER_AMR_NB;
    } else {
        mAudioEncoder = ae;
    }
   return OK;
}
status_t SprdStreamRecorder::SetCodecSpecificData(uint8_t *data,int len)
{
     ALOGE("SetCodecSpecificData for codectype %d,len =%d ",mVideoEncoder,len);
     if(data == NULL || len == 0 )
     {
           ALOGI("error CodecSpecificData set") ;
       return OK;
     }
     if(mVideoEncoder == VIDEO_ENCODER_MPEG_4_SP )
     {
             EsdsGenerator::m_iMpeg4Header_size = len ;
        memcpy(EsdsGenerator::m_Mpeg4Header,data,EsdsGenerator::m_iMpeg4Header_size);
     }
     return OK;
}

status_t SprdStreamRecorder::setVideoEncoder(video_encoder ve) {
    ALOGV("setVideoEncoder: %d", ve);
    if (ve < VIDEO_ENCODER_DEFAULT ||
        ve >= VIDEO_ENCODER_LIST_END) {
        ALOGE("Invalid video encoder: %d", ve);
        return BAD_VALUE;
    }

    if (ve == VIDEO_ENCODER_DEFAULT) {
        mVideoEncoder = VIDEO_ENCODER_H263;
    } else {
        mVideoEncoder = ve;
    }
    return OK;
}

status_t SprdStreamRecorder::setListener(const sp<SprdStreamRecorderClient> &listener) {
    mListener = listener;

    return OK;
}
status_t SprdStreamRecorder::setupAudioSource(const sp<SprdMediaWriter>& writer)
{
    sp<MetaData> encMeta = new MetaData;
    const char *mime;
    switch (mAudioEncoder) {
        case AUDIO_ENCODER_AMR_NB:
        case AUDIO_ENCODER_DEFAULT:
            mime = MEDIA_MIMETYPE_AUDIO_AMR_NB;
            break;
        case AUDIO_ENCODER_AMR_WB:
            mime = MEDIA_MIMETYPE_AUDIO_AMR_WB;
            break;
        case AUDIO_ENCODER_AAC:
            mime = MEDIA_MIMETYPE_AUDIO_AAC;
            break;
        default:
            ALOGE("Unknown audio encoder: %d", mAudioEncoder);
            return NULL;
    }
    encMeta->setCString(kKeyMIMEType, mime);
    encMeta->setInt32(kKeyChannelCount, mAudioChannels);
    encMeta->setInt32(kKeySampleRate, mSampleRate);
    encMeta->setInt32(kKeyBitRate, mAudioBitRate);
    if (mAudioTimeScale > 0) {
        encMeta->setInt32(kKeyTimeScale, mAudioTimeScale);
    }
    audioSource = new  SprdStreamSource(encMeta,NULL);
   if (audioSource == NULL) {
       ALOGE( "creat audiosource fail");
        return UNKNOWN_ERROR;
    }
    writer->addSource(audioSource);
    return OK;
}

status_t SprdStreamRecorder::setupVideoSource(const sp<SprdMediaWriter>& writer)
{
    sp<MetaData> enc_meta = new MetaData;


    enc_meta->setInt32(kKeyBitRate, mVideoBitRate);
    switch (mVideoEncoder) {
        case VIDEO_ENCODER_H263:
            enc_meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_H263);
            break;
        case VIDEO_ENCODER_MPEG_4_SP:
            enc_meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_MPEG4);
            break;
        case VIDEO_ENCODER_H264:
            enc_meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_AVC);
            break;
        default:
            CHECK(!"Should not be here, unsupported video encoding.");
            break;
    }
    enc_meta->setInt32(kKeyWidth, mVideoWidth);
    enc_meta->setInt32(kKeyHeight, mVideoHeight);

    if (mVideoTimeScale > 0) {
        enc_meta->setInt32(kKeyTimeScale, mVideoTimeScale);
    }
    EsdsGenerator::generateEsds(enc_meta);
    videoSource = new  SprdStreamSource(enc_meta,NULL);
    if (videoSource == NULL) {
        ALOGE( "creat Videosource fail");
        return UNKNOWN_ERROR;
    }
    writer->addSource(videoSource);
    return OK;
}

status_t SprdStreamRecorder::start() {
    Mutex::Autolock autoLock(mLock);
#ifndef USE_FFMPEG
    CHECK(mOutputFd >= 0);
    if(mWriter!= NULL  && mPause){
    mPause = false;
    mStarted = true;
    mWriter->start();
    return OK;
    }
    if (mWriter != NULL) {
        ALOGE("File writer is not avaialble");
        return UNKNOWN_ERROR;
    }
    status_t status = OK;

    ALOGI("SprdStreamRecorder::start %d", mOutputFormat);
    switch (mOutputFormat) {
        case OUTPUT_FORMAT_DEFAULT:
        case OUTPUT_FORMAT_THREE_GPP:
        case OUTPUT_FORMAT_MPEG_4:
            status = startMPEG4Recording();
           break;
        case OUTPUT_FORMAT_AMR_NB:
        case OUTPUT_FORMAT_AMR_WB:
            status = startAMRRecording();
            break;

        case OUTPUT_FORMAT_AAC_ADIF:
        case OUTPUT_FORMAT_AAC_ADTS:
            status = startAACRecording();
            break;

        case OUTPUT_FORMAT_RTP_AVP:
            status = startRTPRecording();
            break;

        case OUTPUT_FORMAT_MPEG2TS:
            status = startMPEG2TSRecording();
            break;
        default:
            ALOGE("Unsupported output file format: %d", mOutputFormat);
            status = UNKNOWN_ERROR;
            break;
    }

    if ((status == OK) && (!mStarted)) {
        mStarted = true;
    }
    return status;
#else
   if(m_stOc)
   {
        if(!m_stVideo_st && (mVideoEncoder !=VIDEO_ENCODER_LIST_END) )
          {
               AVCodec *codec;
               AVCodecContext *c;
           m_stVideo_st  = av_new_stream(m_stOc, 0);
           if (!m_stVideo_st) {
                 ALOGI( "Could not alloc video stream\n");
               return  UNKNOWN_ERROR;
            }
            c = m_stVideo_st->codec;
        c->codec_id = CODEC_ID_H263;
            c->codec_type = AVMEDIA_TYPE_VIDEO;

        c->time_base.den =  mVideoTimeScale ;//mVideoTimeScale;
            c->time_base.num = 1;
        c->pix_fmt = PIX_FMT_YUV420P ;
      // c->time_base.den = mVideoTimeScale ;
        c->height = mVideoHeight;
        c->width = mVideoWidth ;
#if 0
        ALOGI( "avcodec_find_encoder  Video codec_id = %d\n",c->codec_id);
        /* find the video encoder */
        codec = avcodec_find_encoder(c->codec_id);
        if (!codec) {
             ALOGI( "codec not found\n");
            // return  UNKNOWN_ERROR;
        }

        /* open the codec */
        if (avcodec_open(c, codec) < 0) {
            ALOGI( "could not open video codec\n");
          //  return  UNKNOWN_ERROR;
        }
 #endif
        ALOGI( "malloc videoBuffer ");
        m_VideoBuffer =  (uint8_t *)malloc(200000);
         if(!m_VideoBuffer)
          {
              ALOGI( "malloc videoBuffer fail ");
          return UNKNOWN_ERROR ;
          }

           }
      if(!m_stAudio_st && (mAudioEncoder !=AUDIO_ENCODER_LIST_END) )
          {
               AVCodec *codec;
               AVCodecContext *c;
            m_stAudio_st = av_new_stream(m_stOc, 1);
                 if (!m_stAudio_st) {
                 ALOGI( "Could not alloc  audio stream\n");
                return UNKNOWN_ERROR;
            }
        c = m_stAudio_st->codec;

        c->codec_id = CODEC_ID_AMR_NB;
        c->codec_type = AVMEDIA_TYPE_AUDIO;

        /* put sample parameters */
        c->sample_fmt = SAMPLE_FMT_S16;
        c->bit_rate = mAudioBitRate;
        c->channels = mAudioChannels;
        c->time_base.den =  c->sample_rate = mSampleRate ;

        c->time_base.num =  c->frame_size = 1 ;
#if 0
       ALOGI( "avcodec_find_encoder  Audio codec_id = %d\n",c->codec_id);

        /* find the video encoder */
        codec = avcodec_find_encoder(c->codec_id);
        if (!codec) {
             ALOGI( "codec not found\n");
            // return  UNKNOWN_ERROR;
        }
            /* open the codec */
        if (avcodec_open(c, codec) < 0) {
            ALOGI( "could not open audio  codec\n");
          //  return  UNKNOWN_ERROR;
         }
#endif
              ALOGI( "malloc audioBuffer ");
          m_AudioBuffer =  (uint8_t *)malloc(10000);
          if(!m_AudioBuffer)
          {
              ALOGI( "malloc audioBuffer fail ");
              return UNKNOWN_ERROR ;

          }
           }

       m_nStartSysTime    = nanoseconds_to_milliseconds(systemTime());
      ALOGI("begin av_write_header") ;
          av_write_header(m_stOc);
          ALOGI("end av_write_header");
   }
  return  OK;
#endif
}

    status_t  SprdStreamRecorder::startAMRRecording()
    {
        //todo
        return OK ;
    }
    status_t  SprdStreamRecorder::startAACRecording()
    {
       //todo
        return OK ;
    }
    status_t  SprdStreamRecorder::startRawAudioRecording()
    {
       //todo
        return OK ;
    }
    status_t   SprdStreamRecorder::startRTPRecording()
    {
    //todo
     return OK ;
    }
    status_t    SprdStreamRecorder::startMPEG2TSRecording()
    {
        //todo
         return OK ;
    }

status_t SprdStreamRecorder::setupMPEG4Recording(
        int outputFd,
        int32_t videoWidth, int32_t videoHeight,
        int32_t videoBitRate,
        int32_t *totalBitRate,
        sp<SprdMediaWriter> *mediaWriter) {

     mediaWriter->clear();
    *totalBitRate = 0;
      status_t err = OK;

        sp<SprdMediaWriter> writer = new SprdMPEG4Writer(outputFd);

    if (mVideoEncoder !=VIDEO_ENCODER_LIST_END)
        {
        err = setupVideoSource(writer);
        if (err != OK)
        {
                writer.clear() ;
            return err;
        }

        *totalBitRate = (*totalBitRate) +  mVideoBitRate;
        }
       if (mAudioEncoder !=AUDIO_ENCODER_LIST_END)
       {
        err = setupAudioSource(writer);

        if (err != OK)
        {
            writer.clear() ;
            return err;
        }
        *totalBitRate += mAudioBitRate;
           }

    if (mInterleaveDurationUs > 0) {
        reinterpret_cast<SprdMPEG4Writer *>(writer.get())->
            setInterleaveDuration(mInterleaveDurationUs);
    }
    if (mLongitudex10000 > -3600000 && mLatitudex10000 > -3600000) {
        reinterpret_cast<SprdMPEG4Writer *>(writer.get())->
            setGeoData(mLatitudex10000, mLongitudex10000);
    }
    if (mMaxFileDurationUs != 0) {
        writer->setMaxFileDuration(mMaxFileDurationUs);
    }
    if (mMaxFileSizeBytes != 0) {
        writer->setMaxFileSize(mMaxFileSizeBytes);
    }
    if (mStartTimeOffsetMs > 0) {
        reinterpret_cast<SprdMPEG4Writer *>(writer.get())->
            setStartTimeOffsetMs(mStartTimeOffsetMs);
    }

    writer->setListener(mListener);
    *mediaWriter = writer;
    return OK;
}

void SprdStreamRecorder::setupMPEG4MetaData(int64_t startTimeUs, int32_t totalBitRate,
        sp<MetaData> *meta) {
    (*meta)->setInt64(kKeyTime, startTimeUs);
    (*meta)->setInt32(kKeyFileType, mOutputFormat);
    (*meta)->setInt32(kKeyBitRate, totalBitRate);
    (*meta)->setInt32(kKey64BitFileOffset, mUse64BitFileOffset);
    if (mMovieTimeScale > 0) {
        (*meta)->setInt32(kKeyTimeScale, mMovieTimeScale);
    }
    if (mTrackEveryTimeDurationUs > 0) {
        (*meta)->setInt64(kKeyTrackTimeStatus, mTrackEveryTimeDurationUs);
    }
    if (mRotationDegrees != 0) {
        (*meta)->setInt32(kKeyRotation, mRotationDegrees);
    }
}

status_t SprdStreamRecorder::startMPEG4Recording() {
    int32_t totalBitRate;
    status_t err = setupMPEG4Recording(
            mOutputFd, mVideoWidth, mVideoHeight,
            mVideoBitRate, &totalBitRate, &mWriter);
    if (err != OK) {
        return err;
    }

    int64_t startTimeUs = systemTime() / 1000;
    sp<MetaData> meta = new MetaData;
    setupMPEG4MetaData(startTimeUs, totalBitRate, &meta);

    err = mWriter->start(meta.get());
    if (err != OK) {
        return err;
    }

    return OK;
}

status_t SprdStreamRecorder::pause() {
    ALOGV("pause");
#ifndef USE_FFMPEG
    if (mWriter == NULL) {
        return UNKNOWN_ERROR;
    }
    mWriter->pause();

    if (mStarted) {
        mStarted = false;
        mPause = true;
   }
#endif
    return OK;
}

status_t SprdStreamRecorder::stop() {
    ALOGI("stop");
#ifndef USE_FFMPEG
    status_t err = OK;

    if (mWriter != NULL) {
        err = mWriter->stop();
        mWriter.clear();
    mWriter = NULL;
    }

    if (mOutputFd >= 0) {
        ::close(mOutputFd);
        mOutputFd = -1;
    }
    if (mStarted) {
        mStarted = false;
    }
#else
    if(m_stOc)
    {
         ALOGE("write_trailer begin");
        av_write_trailer(m_stOc);
    ALOGE("write_trailer end");
        /* free the streams */
      for(int32_t  i = 0; i < m_stOc->nb_streams; i++) {
    //avcodec_close(m_stOc->streams[i]->codec) ;
        av_freep(&m_stOc->streams[i]->codec);
        av_freep(&m_stOc->streams[i]);
       }

    if (!(m_stFmt->flags & AVFMT_NOFILE)) {
        /* close the output file */
           url_fclose(m_stOc->pb);
    }
    /* free the stream */
    av_free(m_stOc);
    m_stOc = NULL ;
    }
    if(m_VideoBuffer)
    {
         free(m_VideoBuffer) ;
    m_VideoBuffer = NULL ;
    }
    if(m_AudioBuffer)
    {
         free(m_AudioBuffer) ;
         m_AudioBuffer = NULL ;
    }
#endif
    return OK;
}

status_t SprdStreamRecorder::close() {
    ALOGV("close");
    stop();

    return OK;
}

status_t SprdStreamRecorder::reset() {
    ALOGV("reset");
    stop();
    // Default parameters
    return OK;
}


status_t SprdStreamRecorder::dump(
        int fd, const Vector<String16>& args) const {
    ALOGV("dump");
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;
    if (mWriter != 0) {
        mWriter->dump(fd, args);
    } else {
        snprintf(buffer, SIZE, "   No file writer\n");
        result.append(buffer);
    }
    snprintf(buffer, SIZE, "   Recorder: %p\n", this);
    snprintf(buffer, SIZE, "   Output file (fd %d):\n", mOutputFd);
    result.append(buffer);
    snprintf(buffer, SIZE, "     File format: %d\n", mOutputFormat);
    result.append(buffer);
    snprintf(buffer, SIZE, "     Max file size (bytes): %lld\n", mMaxFileSizeBytes);
    result.append(buffer);
    snprintf(buffer, SIZE, "     Max file duration (us): %lld\n", mMaxFileDurationUs);
    result.append(buffer);
    snprintf(buffer, SIZE, "     File offset length (bits): %d\n", mUse64BitFileOffset? 64: 32);
    result.append(buffer);
    snprintf(buffer, SIZE, "     Interleave duration (us): %d\n", mInterleaveDurationUs);
    result.append(buffer);
    snprintf(buffer, SIZE, "     Progress notification: %lld us\n", mTrackEveryTimeDurationUs);
    result.append(buffer);
    snprintf(buffer, SIZE, "   Audio\n");
    result.append(buffer);
    snprintf(buffer, SIZE, "     Bit rate (bps): %d\n", mAudioBitRate);
    result.append(buffer);
    snprintf(buffer, SIZE, "     Sampling rate (hz): %d\n", mSampleRate);
    result.append(buffer);
    snprintf(buffer, SIZE, "     Number of channels: %d\n", mAudioChannels);
    result.append(buffer);
    snprintf(buffer, SIZE, "   Video\n");
    result.append(buffer);
    snprintf(buffer, SIZE, "     Start time offset (ms): %d\n", mStartTimeOffsetMs);
    result.append(buffer);
    snprintf(buffer, SIZE, "     Frame size (pixels): %dx%d\n", mVideoWidth, mVideoHeight);
    result.append(buffer);
    snprintf(buffer, SIZE, "     Bit rate (bps): %d\n", mVideoBitRate);
    result.append(buffer);
    ::write(fd, result.string(), result.size());
    return OK;
}
}  // namespace android

