#define LOG_NDEBUG 0
#define LOG_TAG "SprdStreamSource"
#include <utils/Log.h>
#include <cutils/properties.h>

#include "SprdStreamSource.h"

#include <unistd.h>
#include <math.h>

#include <errno.h>
#include <string.h>

#include <time.h>
#include <stdio.h>
#include <fcntl.h>

namespace android {

#define DEBUG_LOGD   ALOGI
/////////////////////////////////////////////////////////////////////
#undef LOG_TAG
#define LOG_TAG "SprdStreamSource"

const static int64_t kBufferFilledEventTimeOutNs = 3000000000LL;

SprdStreamSource::SprdStreamSource(
        const sp<MetaData> &format,
        void *dataSource)
    : m_Format(format),
      m_DataSource(dataSource),
      m_bStarted(false),
      m_bForeStop(false),
      m_nNum(0),
      m_bIsVideo(true),
      mEOSResult(0)
{
    const char *mime = NULL;
        if (!format->findCString(kKeyMIMEType, &mime)) {
        DEBUG_LOGD("[%p]SprdStreamSource::SprdStreamSource kKeyMIMEType unknow");
         }
     if (mime && !strncasecmp(mime, "audio/", 6))
    {
       m_bIsVideo = false ;
    }
    DEBUG_LOGD("[%p]SprdStreamSource::SprdStreamSource m_bIsVideo %d ", this,m_bIsVideo);
}

 sp<MetaData> SprdStreamSource:: getFormat()
 {
     Mutex::Autolock autoLock(m_Lock);
     return m_Format;
 }
SprdStreamSource::~SprdStreamSource()
{
    DEBUG_LOGD("[%p]SprdStreamSource::~SprdStreamSource", this);
    if (m_bStarted)
        stop();
}

status_t SprdStreamSource::start(MetaData *params)
{
    Mutex::Autolock autoLock(m_Lock);

    DEBUG_LOGD("[%p]SprdStreamSource::start", this);

    status_t         err         = NO_MEMORY;
    bool            bRet    = false;

    if (m_bStarted)
        goto success;

    m_bStarted = true;
    m_bForeStop = false;
    m_nInitialDelayUs = 300000; //300 um to syc with audio
    m_nStartSysTime    = nanoseconds_to_milliseconds(systemTime());
success:
    DEBUG_LOGD("[%p]SprdStreamSource::start SUCCESS!", this);
        return OK;
}

status_t SprdStreamSource::stop()
{
    Mutex::Autolock autoLock(m_Lock);

    status_t err;

    DEBUG_LOGD("[%p]SprdStreamSource::stop", this);
    if (!m_bStarted)
        goto success;

    m_bStarted = false;

    mEOSResult = ERROR_END_OF_STREAM ;

    m_DataGet.signal();

    //relese the share mem
    //........

success:
    DEBUG_LOGD("[%p]SprdStreamSource::stop SUCCESS!", this);
    return OK;
}

status_t SprdStreamSource::read(
        MediaBuffer **out, const ReadOptions *options)
{
    Mutex::Autolock autoLock(m_Lock);
    uint32_t    nStart = 0;
    uint32_t    nEnd;
    status_t     err    = UNKNOWN_ERROR;

    char     cHeader[16];
    uint32_t    nSize;
    uint32_t    nPts = 0;//not used
    int         type = 0;//not used
    MediaBuffer*        pMediaBuffer = NULL;
     if(!m_bStarted)
     {
    DEBUG_LOGD("[%p]VideoPhoneSource::read error not start");
        goto fail;
     }
    *out = NULL;

   while (mEOSResult == OK && mBuffers.empty())
   {
           DEBUG_LOGD("[%p]VideoPhoneSource::read error not data wait");
      status_t waiterr = m_DataGet .waitRelative(m_Lock, kBufferFilledEventTimeOutNs);
      if(waiterr != OK)
      {
         DEBUG_LOGD("[%p]VideoPhoneSource::read waittimeout");
         goto fail ;
      }
       }
     DEBUG_LOGD("[%p]VideoPhoneSource::read mBuffers =%d num = %d",this,mBuffers.size(),m_nNum );

    if (!mBuffers.empty()) {
    m_nNum++;

        buffer_item *buffe = *mBuffers.begin();
        int nLen = buffe->len ;

       pMediaBuffer = new MediaBuffer(nLen);
       memcpy(pMediaBuffer->data(), buffe->data, nLen);
        mBuffers.erase(mBuffers.begin());
        if(m_bIsVideo)
       {
        video_srteam_t streambuf;
        int ret = video_stream_init(&streambuf,(unsigned char *)pMediaBuffer->data(),nLen);
        if( 0 !=ret )
        {
                goto fail ;
        }
        int is_I_vop;
        ret =get_video_stream_info(&streambuf,&is_I_vop);
        if((ret==0)&&(is_I_vop==1)){
            pMediaBuffer->meta_data()->setInt32(kKeyIsSyncFrame, 1);
        }
        if(m_nNum ==1)
            pMediaBuffer->meta_data()->setInt32(kKeyIsSyncFrame, 1);
    }
    pMediaBuffer->meta_data()->setInt64(
                    kKeyTime,
                    buffe->timestap
                   /* 1000 * (nanoseconds_to_milliseconds(systemTime()) -m_nStartSysTime)*/);
        free(buffe->data);
        delete buffe ;
       *out = pMediaBuffer;
        return OK;
    }
 fail:

    //err = ERROR_UNSUPPORTED;
    if(pMediaBuffer == NULL)
    {
         pMediaBuffer = new MediaBuffer(100);
    }
    if (pMediaBuffer != NULL){
        *out = pMediaBuffer;
        pMediaBuffer->set_range(0, 0);
        pMediaBuffer->meta_data()->clear();
        pMediaBuffer->meta_data()->setInt64(
                        kKeyTime,
                        1000 * (nanoseconds_to_milliseconds(systemTime()) -m_nStartSysTime));
    }
    DEBUG_LOGD("*****VideoPhoneSource::read FAIL!******");
    return mEOSResult;
}

int SprdStreamSource::write(buffer_item*buf,int nLen)
{
    DEBUG_LOGD("[%p]SprdStreamSource::write nLen: %d", this, nLen);
    Mutex::Autolock autoLock(m_Lock);
    buffer_item * pMediaBuffer = new buffer_item;

    uint8_t *pdata = (uint8_t *) malloc(nLen);
    memcpy(pdata,buf->data,nLen);
    pMediaBuffer->data = pdata ;
    pMediaBuffer->len = nLen ;
    pMediaBuffer->timestap = buf->timestap ;
    DEBUG_LOGD("[%p]streamSource::write OK", this);
    mBuffers.push_back(pMediaBuffer);
    m_DataGet.signal();
       return  1 ;
}

/////////////////////////////////////////////////////////////////////
#undef LOG_TAG
#define LOG_TAG "EsdsGenerator"

/*uint8_t EsdsGenerator::m_Mpeg4Header[100] = {0x00, 0x00, 0x01, 0xb0, 0x05, 0x00, 0x00, 0x01, 0xb5, 0x09, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
 0x01, 0x20, 0x00, 0x84, 0x40, 0x07, 0xa8, 0x2c, 0x20, 0x90, 0xa2, 0x8f};
{0x00, 0x00, 0x01, 0xb0, 0x01, 0x00, 0x00, 0x01, 0xb5, 0x09, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
 0x01, 0x20, 0x00, 0x84, 0x5d, 0x4c, 0x28, 0x2c, 0x20, 0x90, 0xa2, 0x1f};*/
/*{0x00, 0x00, 0x01, 0xb0, 0x14, 0x00, 0x00, 0x01, 0xb5, 0x09, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
 0x01, 0x20, 0x00, 0x84, 0x40, 0xfa, 0x28, 0x2c, 0x20, 0x90, 0xa2, 0x1f, 0x00, 0x00, 0x00, 0x00};
int EsdsGenerator::m_iMpeg4Header_size = 28;*/

static EsdsGenerator* g_Esds = NULL;
uint8_t EsdsGenerator::m_Mpeg4Header[2048] = {0};
int EsdsGenerator::m_iMpeg4Header_size = 0;

EsdsGenerator::EsdsGenerator()
{
    memset(m_EsdsBuffer, 0, 150);
    m_iEsds_size = 0;
}

EsdsGenerator::~EsdsGenerator()
{
}

void EsdsGenerator::generateEsds(sp<MetaData> AVMeta)
{
    DEBUG_LOGD("generateEsds()");

    g_Esds = new EsdsGenerator();

    //writeInt32(0);             // version=0, flags=0
    g_Esds->writeInt8(0x03);  // ES_DescrTag
    g_Esds->writeInt8(23 + EsdsGenerator::m_iMpeg4Header_size);
    g_Esds->writeInt16(0x0000);  // ES_ID
    g_Esds->writeInt8(0x1f);
    g_Esds->writeInt8(0x04);  // DecoderConfigDescrTag
    g_Esds->writeInt8(15 + EsdsGenerator::m_iMpeg4Header_size);
    g_Esds->writeInt8(0x20);  // objectTypeIndication ISO/IEC 14492-2
    g_Esds->writeInt8(0x11);  // streamType VisualStream

    static const uint8_t kData[] = {
        0x01, 0x77, 0x00,
        0x00, 0x03, 0xe8, 0x00,
        0x00, 0x03, 0xe8, 0x00
    };
    g_Esds->writeEsds(kData, sizeof(kData));

    g_Esds->writeInt8(0x05);  // DecoderSpecificInfoTag

    g_Esds->writeInt8(EsdsGenerator::m_iMpeg4Header_size);
    g_Esds->writeEsds(EsdsGenerator::m_Mpeg4Header,EsdsGenerator::m_iMpeg4Header_size);

    static const uint8_t kData2[] = {
        0x06,  // SLConfigDescriptorTag
        0x01,
        0x02
    };
    g_Esds->writeEsds(kData2, sizeof(kData2));

    AVMeta->setData(
                kKeyESDS, 0,
                g_Esds->m_EsdsBuffer, g_Esds->m_iEsds_size);
    delete g_Esds;
}

void EsdsGenerator::writeEsds(const void *ptr, size_t size)
{
    if (m_iEsds_size + size > 1500) {
        DEBUG_LOGD("writeEsds(), buffer overflow");
        return;
    }

    memcpy(m_EsdsBuffer + m_iEsds_size, ptr, size);
    m_iEsds_size += size;
    DEBUG_LOGD("writeEsds(), size: %d, m_iEsds_size: %d", size, m_iEsds_size);
}

void EsdsGenerator::writeInt8(int8_t x)
{
    DEBUG_LOGD("writeInt8(), x: %d, 0x%x", x, x);
    writeEsds(&x, 1);
}

void EsdsGenerator::writeInt16(int16_t x)
{
    DEBUG_LOGD("writeInt16(), x: %d, 0x%x", x, x);
    writeEsds(&x, 2);
}

void EsdsGenerator::writeInt32(int32_t x)
{
    DEBUG_LOGD("writeInt32(), x: %d, 0x%x", x, x);
    writeEsds(&x, 4);
}


}  // namespace android

