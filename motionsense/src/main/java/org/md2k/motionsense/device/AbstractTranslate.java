package org.md2k.motionsense.device;
/*
 * Copyright (c) 2016, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import java.util.ArrayList;
import java.util.List;

abstract class AbstractTranslate {
    private static final int BUFFER_SIZE = 10;
    private List<Data> buffer = new ArrayList<>();
    private long lastSampleTimestamp;
    private long lastSampleSeqNumber;
    AbstractTranslate(){
        lastSampleSeqNumber=0;
        lastSampleSeqNumber=0;
    }
    abstract void insertData(long timeStamp, long offset, Data data);
    void insertToQueue(Data data) {
        long gyroOffset = -1;
        if (lastSampleTimestamp > 0 && data.getTimestamp() - lastSampleTimestamp > 500) {
            gyroOffset = correctTimestamp(buffer, lastSampleTimestamp, lastSampleSeqNumber) / 2;
        } else if (buffer.size() == BUFFER_SIZE) {
            gyroOffset = correctTimestamp(buffer, lastSampleTimestamp, lastSampleSeqNumber) / 2;
        }
        if (gyroOffset != -1) {
            for (int i = 0; i < buffer.size(); i++)
                insertData(buffer.get(i).getTimestamp(), gyroOffset, buffer.get(i));
            lastSampleTimestamp= buffer.get(buffer.size() - 1).getTimestamp();
            lastSampleSeqNumber= (long) buffer.get(buffer.size() - 1).getSequenceNumber()[0];
            buffer.clear();
        }
        buffer.add(data);
    }
    private long correctTimestamp(List<Data> buffer, long lastSampleTimestamp, long lastSampleSeqNum) {

        long startTS = lastSampleTimestamp;
        long endTS = buffer.get(buffer.size() - 1).getTimestamp();

        long startSeqNum = lastSampleSeqNum;
        long endSeqNum = (long) buffer.get(buffer.size() - 1).getSequenceNumber()[0];

        if (lastSampleTimestamp == 0) {
            startTS = buffer.get(0).getTimestamp();
            startSeqNum = (long) buffer.get(0).getSequenceNumber()[0];
        }
        long offset = (endTS - startTS) / (buffer.size());

        if (lastSampleTimestamp > 0 && endSeqNum > startSeqNum) {
            offset = (endTS - startTS) / (endSeqNum - startSeqNum);
            for (int i = 0; i < buffer.size(); i++) {
                buffer.get(i).setTimestamp(endTS - (endSeqNum - (long)buffer.get(i).getSequenceNumber()[0]) * offset);
            }
        } else {
            for (int i = 0; i < buffer.size(); i++) {
                buffer.get(i).setTimestamp(startTS + (i) * offset);
            }
        }
//        Log.d(TAG,"[MOTION_SENSE_SEQ] seq=("+startSeqNum+", "+endSeqNum+"), diff="+(endSeqNum-startSeqNum)+ "; buffSize="+(buffer.size())+"; offset="+offset);

        return offset;
    }

}
