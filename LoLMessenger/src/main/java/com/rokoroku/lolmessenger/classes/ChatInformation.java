package com.rokoroku.lolmessenger.classes;

/**
 * Created by Youngrok Kim on 13. 8. 20.
 */
public class ChatInformation {

    private String buddyID;
    private ParcelableRoster buddy;
    private ParcelableMessage latestMessage;
    private int count = 0;

    public ChatInformation() {
    }

    public ChatInformation(String buddyID, ParcelableMessage latestMessage, int count) {

        this.buddyID = buddyID;
        this.latestMessage = latestMessage;
        this.count = count;
    }


    public String getBuddyID() {
        return buddyID;
    }

    public void setBuddyID(String buddyID) {
        this.buddyID = buddyID;
    }

    public ParcelableRoster getBuddy() {
        return buddy;
    }

    public void setBuddy(ParcelableRoster buddy) {
        this.buddy = buddy;
    }

    public ParcelableMessage getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(ParcelableMessage latestMessage) {
        this.latestMessage = latestMessage;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incrementCount() {
        this.count++;
    }
}
