package com.rokoroku.lolmessenger.classes;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by Youngrok Kim on 13. 8. 14.
 */
public class ParcelableMessage implements Parcelable {

    private String fromID;
    private String toID;
    private String body;
    private long  timeStamp;
    private int flag;

    public ParcelableMessage() {
    }

    public ParcelableMessage(String from, String to, String body, int flag) {
        // flag : read or not
        this.fromID = from;
        this.toID = to;
        this.body = body;
        this.timeStamp = new Date().getTime();
        this.flag = flag;
    }

    public String getFromID() {
        return fromID;
    }

    public void setFromID(String userId) {
        this.fromID = userId;
    }

    public String getToID() {
        return toID;
    }

    public void setToID(String toID) {
        this.toID = toID;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean isFlag() {
        return flag > 0;
    }

    public void setFlag() {
        this.flag = 1;
    }

    public void unsetFlag() {
        this.flag = 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(fromID);
        parcel.writeString(toID);
        parcel.writeString(body);
        parcel.writeLong(timeStamp);
        parcel.writeInt(flag);
    }

    public static final Parcelable.Creator<ParcelableMessage> CREATOR = new Creator<ParcelableMessage>() {
        public ParcelableMessage createFromParcel(Parcel source) {
            ParcelableMessage r = new ParcelableMessage();
            r.fromID = source.readString();
            r.toID = source.readString();
            r.body = source.readString();
            r.timeStamp = source.readLong();
            r.flag = source.readInt();
            return r;
        }
        public ParcelableMessage[] newArray(int size) {
            return new ParcelableMessage[size];
        }
    };

    @Override
    public String toString() {
        return "ParcelableMessage{" +
                "fromID='" + fromID + '\'' +
                ", toID='" + toID + '\'' +
                ", body='" + body + '\'' +
                ", timeStamp=" + timeStamp + '\'' +
                ", flag=" + flag +
                '}';
    }
}
