#ifndef SPRD_STREAM_SOURCE_H_

#define SPRD_STREAM_SOURCE_H_


#include "MediaBuffer.h"
#include "MetaData.h"
#include  "MediaSource.h"
#include <utils/threads.h>

#include <stdio.h>
#include <fcntl.h>
#include <utils/List.h>
#include "SprdStreamRecoderClient.h"


namespace android {

class SprdStreamSource : public MediaSource
{
public:

        SprdStreamSource(const sp<MetaData> &format,
                        void *dataSource);

        virtual status_t start(MetaData *params = NULL);

        virtual status_t stop();

        virtual sp<MetaData> getFormat();

        virtual status_t read(
            MediaBuffer **buffer, const ReadOptions *options = NULL);
      int write(buffer_item* buf, int nLen);
protected:
        virtual ~SprdStreamSource();
private:
    friend class EsdsGenerator;
    //bool                m_bFirstGet;
        Mutex             m_Lock;

    Condition         m_DataGet;

        sp<MetaData>     m_Format;


        void *     m_DataSource;

        bool             m_bStarted;

        bool             m_bForeStop;

       List<buffer_item *> mBuffers;
       status_t mEOSResult;



    int64_t            m_nStartSysTime;
        int64_t            m_nInitialDelayUs;

    pthread_t         m_Thread;

    int            m_nNum;
    bool            m_bIsVideo;
};
class EsdsGenerator{
public:
    static void    generateEsds(sp<MetaData> AVMeta);
    static uint8_t        m_Mpeg4Header[2048];
    static int            m_iMpeg4Header_size;

private:
    EsdsGenerator();
    ~EsdsGenerator();
    void            writeEsds(const void *ptr, size_t size);
    void            writeInt8(int8_t x);
    void            writeInt16(int16_t x);
    void            writeInt32(int32_t x);

private:
//    static uint8_t        m_Mpeg4Header[100];
//    static int            m_iMpeg4Header_size;
    uint8_t            m_EsdsBuffer[150];
    int                m_iEsds_size;
};

}  // namespace android
#endif

