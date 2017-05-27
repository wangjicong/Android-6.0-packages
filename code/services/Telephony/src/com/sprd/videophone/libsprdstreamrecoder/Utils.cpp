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

//#define ALOG_NDEBUG 0
#define LOG_TAG "ESDS"
#include <utils/Log.h>
#include <utils/Errors.h>
#include "Utils.h"
#include <stdint.h>

#include <string.h>

namespace android {

int video_stream_init(video_srteam_t *pStream,unsigned char *start,int length)
{
    if(length<=0)
        return 1;
    pStream->start = start;
    pStream->curent = start;
    pStream->current_bit = 0;
    pStream->length = length;
    return 0;
}

static unsigned int show_video_bits(video_srteam_t *pStream,int num)//num<=32
{
    unsigned int first32bits;
    unsigned int firstByte = 0;
    unsigned int secondByte = 0;
    unsigned int thirdByte = 0;
    unsigned int fourthByte = 0;
    unsigned int fifthByte = 0;
    if((pStream->curent)<(pStream->start+pStream->length ))
        firstByte =  *pStream->curent;
    if((pStream->curent+1)<(pStream->start+pStream->length ))
        secondByte =  *(pStream->curent+1);
    if((pStream->curent+2)<(pStream->start+pStream->length ))
        thirdByte =  *(pStream->curent+2);
    if((pStream->curent+3)<(pStream->start+pStream->length ))
        fourthByte =  *(pStream->curent+3);
    if((pStream->curent+4)<(pStream->start+pStream->length ))
        fifthByte =  *(pStream->curent+4);

    first32bits = (firstByte<<24)|(secondByte<<16)|(thirdByte<<8)|(fourthByte);
    if(pStream->current_bit!=0)
        first32bits = (first32bits<<pStream->current_bit)|(fifthByte>>(8-pStream->current_bit));

    return first32bits>>(32-num);
}
static void  flush_video_bits(video_srteam_t *pStream,int num)//num<=32
{
    pStream->curent += (pStream->current_bit+num)/8;
    pStream->current_bit = (pStream->current_bit+num)%8;
}
static unsigned int read_video_bits(video_srteam_t *pStream,int num)//num<=32
{
    unsigned int tmp = show_video_bits(pStream,num);
    flush_video_bits(pStream,num);
    return tmp;
}

static int decode_h263_header(video_srteam_t *pStream,int *is_I_vop)
{
    unsigned int tmpVar = show_video_bits(pStream,22);
    if(0x20 != tmpVar)
        return 1;
    flush_video_bits(pStream,22);
    tmpVar = read_video_bits(pStream,9);
    if(!(tmpVar&0x1))
        return 1;
    tmpVar = read_video_bits(pStream,7);
    if(tmpVar>>3)
        return 1;
    tmpVar = tmpVar&0x07;
    if(tmpVar==7) //do not  support  EXTENDED_PTYPE
        return 1;
    tmpVar = read_video_bits(pStream,11);
    if((tmpVar>>10)==0){
        *is_I_vop = 1;
    }else{
        *is_I_vop = 0;
    }
    return 0;
}

static int decode_mpeg4_header(video_srteam_t *pStream,int *is_I_vop)
{
    unsigned int tmpVar, uStartCode = 0;
    int loopNum = 0;
    int vopType;
    while(uStartCode!=0x1B6){
        uStartCode = show_video_bits(pStream,32);
        if(0x1B6 == uStartCode){
            tmpVar = read_video_bits(pStream,32);
            tmpVar =  read_video_bits(pStream,3);
            vopType = tmpVar>>1;
            if(vopType==0){
                *is_I_vop = 1;
            }else{
                *is_I_vop = 0;
            }
        }else{
            read_video_bits(pStream,8);
        }
        loopNum++;
        if(loopNum>2048)
            return 1;
    }
    return 0;
}

int get_video_stream_info(video_srteam_t *pStream,int *is_I_vop)
{
    int is_h263;
    *is_I_vop = 0;
    is_h263 = (show_video_bits(pStream,21)==0x10);
    if(is_h263){
        return decode_h263_header(pStream,is_I_vop);
    }else{
        return decode_mpeg4_header(pStream,is_I_vop);
    }
}

ESDS::ESDS(const void *data, size_t size)
    : mData(new uint8_t[size]),
      mSize(size),
      mInitCheck(NO_INIT),
      mDecoderSpecificOffset(0),
      mDecoderSpecificLength(0),
      mObjectTypeIndication(0) {
    memcpy(mData, data, size);

    mInitCheck = parse();
}

ESDS::~ESDS() {
    delete[] mData;
    mData = NULL;
}

status_t ESDS::InitCheck() const {
    return mInitCheck;
}

status_t ESDS::getObjectTypeIndication(uint8_t *objectTypeIndication) const {
    if (mInitCheck != OK) {
        return mInitCheck;
    }

    *objectTypeIndication = mObjectTypeIndication;

    return OK;
}

status_t ESDS::getCodecSpecificInfo(const void **data, size_t *size) const {
    if (mInitCheck != OK) {
        return mInitCheck;
    }

    *data = &mData[mDecoderSpecificOffset];
    *size = mDecoderSpecificLength;

    return OK;
}

status_t ESDS::skipDescriptorHeader(
        size_t offset, size_t size,
        uint8_t *tag, size_t *data_offset, size_t *data_size) const {
    if (size == 0) {
        return ERROR_MALFORMED;
    }

    *tag = mData[offset++];
    --size;

    *data_size = 0;
    bool more;
    do {
        if (size == 0) {
            return ERROR_MALFORMED;
        }

        uint8_t x = mData[offset++];
        --size;

        *data_size = (*data_size << 7) | (x & 0x7f);
        more = (x & 0x80) != 0;
    }
    while (more);

    ALOGV("tag=0x%02x data_size=%d", *tag, *data_size);

    if (*data_size > size) {
        return ERROR_MALFORMED;
    }

    *data_offset = offset;

    return OK;
}

status_t ESDS::parse() {
    uint8_t tag;
    size_t data_offset;
    size_t data_size;
    status_t err =
        skipDescriptorHeader(0, mSize, &tag, &data_offset, &data_size);

    if (err != OK) {
        return err;
    }

    if (tag != kTag_ESDescriptor) {
        return ERROR_MALFORMED;
    }

    return parseESDescriptor(data_offset, data_size);
}

status_t ESDS::parseESDescriptor(size_t offset, size_t size) {
    if (size < 3) {
        return ERROR_MALFORMED;
    }

    offset += 2;  // skip ES_ID
    size -= 2;

    unsigned streamDependenceFlag = mData[offset] & 0x80;
    unsigned URL_Flag = mData[offset] & 0x40;
    unsigned OCRstreamFlag = mData[offset] & 0x20;

    ++offset;
    --size;

    if (streamDependenceFlag) {
        offset += 2;
        size -= 2;
    }

    if (URL_Flag) {
        if (offset >= size) {
            return ERROR_MALFORMED;
        }
        unsigned URLlength = mData[offset];
        offset += URLlength + 1;
        size -= URLlength + 1;
    }

    if (OCRstreamFlag) {
        offset += 2;
        size -= 2;

        if ((offset >= size || mData[offset] != kTag_DecoderConfigDescriptor)
                && offset - 2 < size
                && mData[offset - 2] == kTag_DecoderConfigDescriptor) {
            // Content found "in the wild" had OCRstreamFlag set but was
            // missing OCR_ES_Id, the decoder config descriptor immediately
            // followed instead.
            offset -= 2;
            size += 2;

            ALOGW("Found malformed 'esds' atom, ignoring missing OCR_ES_Id.");
        }
    }

    if (offset >= size) {
        return ERROR_MALFORMED;
    }

    uint8_t tag;
    size_t sub_offset, sub_size;
    status_t err = skipDescriptorHeader(
            offset, size, &tag, &sub_offset, &sub_size);

    if (err != OK) {
        return err;
    }

    if (tag != kTag_DecoderConfigDescriptor) {
        return ERROR_MALFORMED;
    }

    err = parseDecoderConfigDescriptor(sub_offset, sub_size);

    return err;
}

status_t ESDS::parseDecoderConfigDescriptor(size_t offset, size_t size) {
    if (size < 13) {
        return ERROR_MALFORMED;
    }

    mObjectTypeIndication = mData[offset];

    offset += 13;
    size -= 13;

    if (size == 0) {
        mDecoderSpecificOffset = 0;
        mDecoderSpecificLength = 0;
        return OK;
    }

    uint8_t tag;
    size_t sub_offset, sub_size;
    status_t err = skipDescriptorHeader(
            offset, size, &tag, &sub_offset, &sub_size);

    if (err != OK) {
        return err;
    }

    if (tag != kTag_DecoderSpecificInfo) {
        return ERROR_MALFORMED;
    }

    mDecoderSpecificOffset = sub_offset;
    mDecoderSpecificLength = sub_size;

    return OK;
}
}  // namespace android

