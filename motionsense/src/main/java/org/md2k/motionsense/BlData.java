package org.md2k.motionsense;

import android.os.Parcel;
import android.os.Parcelable;

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
public class BlData implements Parcelable{
    public static final int DATATYPE_ACLGYR=1;
    public static final int DATATYPE_BATTERY=2;
    public static final int DATATYPE_ACLGYRLED=3;
    private String deviceId;
    private int type;
    private byte[] data;

    public BlData(String deviceId, int type, byte[] data) {
        this.deviceId = deviceId;
        this.type = type;
        this.data = data;
    }

    protected BlData(Parcel in) {
        deviceId = in.readString();
        type = in.readInt();
        data = in.createByteArray();
    }

    public static final Creator<BlData> CREATOR = new Creator<BlData>() {
        @Override
        public BlData createFromParcel(Parcel in) {
            return new BlData(in);
        }

        @Override
        public BlData[] newArray(int size) {
            return new BlData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceId);
        dest.writeInt(type);
        dest.writeByteArray(data);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public int getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }
}
