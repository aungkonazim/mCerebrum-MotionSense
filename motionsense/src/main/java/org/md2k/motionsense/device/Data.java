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

import android.util.Log;

import org.md2k.datakitapi.source.platform.PlatformType;

class Data {
    private byte[] data;
    private long timestamp;
    String platformType;

    Data(String platformType, byte[] data, long timestamp) {
        this.timestamp = timestamp;
        this.data = data;
        this.platformType=platformType;
    }

    public byte[] getData() {
        return data;
    }

    long getTimestamp() {
        return timestamp;
    }

    double[] getSequenceNumber() {
        int seq;
        if(platformType.equals(PlatformType.MOTION_SENSE))
            seq=byteArrayToIntBE(new byte[]{data[18], data[19]});
        else{
            int x=(data[19] & 0x00000000000000ff);
            int y=(data[18] & 0x03);
            seq=(y<<8)+x;
        }
        return new double[]{seq};
    }

    private int byteArrayToIntBE(byte[] bytes) {
        return java.nio.ByteBuffer.wrap(bytes).getShort();
    }

    void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    private double convertGyroADCtoSI(double x) {
        return 500.0 * x / 32768;
    }

    private double convertAccelADCtoSI(double x) {
        return 2.0 * x / 16384;
    }


    double[] getAccelerometer() {
        double[] sample = new double[3];
        sample[0] = convertAccelADCtoSI(byteArrayToIntBE(new byte[]{getData()[0], getData()[1]}));
        sample[1] = convertAccelADCtoSI(byteArrayToIntBE(new byte[]{getData()[2], getData()[3]}));
        sample[2] = convertAccelADCtoSI(byteArrayToIntBE(new byte[]{getData()[4], getData()[5]}));
        return sample;
    }

    double[] getGyroscope() {
        double[] sample = new double[3];
        sample[0] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{getData()[6], getData()[7]}));
        sample[1] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{getData()[8], getData()[9]}));
        sample[2] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{getData()[10], getData()[11]}));
        return sample;
    }
    double[] getGyroscope2(){
        double[] sample = new double[3];
        sample[0] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{getData()[12], getData()[13]}));
        sample[1] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{getData()[14], getData()[15]}));
        sample[2] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{getData()[16], getData()[17]}));
        return sample;
    }
    double[] getLED(){
        double[] sample = new double[3];
        sample[0] = convertLED1(getData()[12], getData()[13], getData()[14]);
        sample[1] = convertLED2(getData()[14], getData()[15], getData()[16]);
        sample[2] = convertLED3(getData()[16], getData()[17], getData()[18]);
        return sample;
    }
    private double convertLED1(byte msb, byte mid, byte lsb) {
        int lsbRev, msbRev, midRev;
        int msbInt, lsbInt,midInt;
        msbInt = (msb & 0x00000000000000ff);
        midInt = (mid & 0x00000000000000ff);
        lsbInt = (lsb & 0x00000000000000c0);
        msbRev = msbInt;
        lsbRev = lsbInt;
        midRev=midInt;

        return (msbRev << 10) + (midRev<<2)+lsbRev;
    }

    private double convertLED2(byte msb, byte mid, byte lsb) {
        int lsbRev, msbRev, midRev;
        int msbInt, lsbInt,midInt;
        msbInt = (msb & 0x000000000000003f);
        midInt = (mid & 0x00000000000000ff);
        lsbInt = (lsb & 0x00000000000000f0);
        msbRev = msbInt;
        lsbRev = lsbInt;
        midRev=midInt;

        return (msbRev << 12) + (midRev<<4)+lsbRev;
    }
    private double convertLED3(byte msb, byte mid, byte lsb) {
        int lsbRev, msbRev, midRev;
        int msbInt, lsbInt,midInt;
        msbInt = (msb & 0x000000000000000f);
        midInt = (mid & 0x00000000000000ff);
        lsbInt = (lsb & 0x00000000000000fc);
        msbRev = msbInt;
        lsbRev = lsbInt;
        midRev=midInt;

        return (msbRev << 14) + (midRev<<6)+lsbRev;
    }

    double[] getRawData(){
        double[] sample=new double[data.length];
        for(int i=0;i<data.length;i++)
            sample[i]=data[i];
        return sample;
    }
}