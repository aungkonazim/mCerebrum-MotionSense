package org.md2k.motionsense;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by monowar on 7/12/16.
 */
public class BlData implements Parcelable{
    public static final int DATATYPE_ACLGYR=1;
    public static final int DATATYPE_BATTERY=2;
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
