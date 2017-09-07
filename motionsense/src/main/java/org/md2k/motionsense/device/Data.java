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
        if(platformType.equals(PlatformType.MOTION_SENSE))
            return new double[]{byteArrayToIntBE(new byte[]{data[18], data[19]})};
        else{
            int x=data[19];
            int y=(data[18] >>6);
            return new double[]{(y<<8)+x};
        }
    }

    private int byteArrayToIntBE(byte[] bytes) {
        return java.nio.ByteBuffer.wrap(bytes).getShort();
    }

    void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    private double convertGyroADCtoSI(double x) {
        return 250.0 * x / 32768;
    }

    private double convertAccelADCtoSI(double x) {
        return 1.0 * x / 16384;
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
        sample[0] = convertLEDValue(getData()[12], getData()[13], getData()[14]);
        sample[1] = convertLEDValue(getData()[14], getData()[15], getData()[16]);
        sample[2] = convertLEDValue(getData()[16], getData()[17], getData()[18]);
        return sample;
    }
    private double convertLEDValue(byte msb, byte mid, byte lsb) {
        int lsbRev, msbRev, midRev;
        int msbInt, lsbInt,midInt;
        msbInt = (msb & 0x0000000000000003);
        lsbInt = (lsb & 0x00000000000000ff);
        midInt = (mid & 0x00000000000000ff);
//        byte[] bytes=new byte[]{msb,mid,lsb};

//        return byteArrayToIntBE();
//        msbRev=reverseByte(msbInt);
//        lsbRev=reverseByte(lsbInt);
//        midRev=reverseByte(midInt);
        msbRev = msbInt;
        lsbRev = lsbInt;
        midRev=midInt;


//        if(lsb<0) lsbRev=-(int)lsb+128; else lsbRev=lsb;
//          if(msb<0) msbRev=-(int)msb+128; else msbRev=msb;
//        lsbRev=reverseByte(lsb);
//        msbRev=reverseByte(msb);
//        return java.nio.ByteBuffer.wrap(new byte[]{lsbRev, msbRev,0}).getInt();
//        return lsbRev<<16+midRev<<8+msbRev;
//        int value = (msbRev << 16) + midRev<<8+lsbRev;
        int value = (msbRev << 16) + (midRev<<8)+lsbRev;

        //       Log.d(TAG,"("+msbInt+","+midInt+","+lsbInt+")"+" ("+msbRev+","+midRev+","+lsbRev+")"+ value);
        return value;
    }
    double[] getRawData(){
        double[] sample=new double[data.length];
        for(int i=0;i<data.length;i++)
            sample[i]=data[i];
        return sample;
    }
}