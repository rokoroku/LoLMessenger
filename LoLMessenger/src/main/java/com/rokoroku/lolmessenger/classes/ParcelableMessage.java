package com.rokoroku.lolmessenger.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.rokoroku.lolmessenger.LolMessengerApplication;
import com.rokoroku.lolmessenger.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
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

    public String parseInviteMessage(String inviteMessage) {
        //Parse inviteMessage
        //e.g. <body><inviteId>453324547</inviteId><userName>꾸이란</userName><profileIconId>28</profileIconId><gameType>NORMAL_GAME</gameType><groupId></groupId><seasonRewards>false</seasonRewards><mapId>10</mapId><queueId>8</queueId><gameMode>classic_pvp</gameMode><gameDifficulty></gameDifficulty></body>

        XmlPullParserFactory factory = null;

        String gameType = null;
        int mapId = 0;
        //int queueId = 0;
        String gameMode = null;
        String gameTypeIndex = null;
        String gameDifficulty = null;
        String userName = null;
        String parsedMessage = null;

        try {
            boolean initbody = false;
            factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(inviteMessage));
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {
                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        String startTag = parser.getName();
                        if(initbody != true && startTag.equals("body")) { initbody = true; }
                        else if(initbody == true) try {
                            if(startTag.equals("userName")) { userName = parser.nextText(); }
                            if(startTag.equals("gameType")) { gameType = parser.nextText(); }
                            if(startTag.equals("mapId")) { mapId = Integer.parseInt(parser.nextText()); }
                            //if(startTag.equals("queueId")) { queueId = Integer.parseInt(parser.nextText()); }
                            if(startTag.equals("gameMode")) { gameMode = parser.nextText(); }
                            if(startTag.equals("gameTypeIndex")) { gameTypeIndex = parser.nextText(); }
                            if(startTag.equals("gameDifficulty")) { gameDifficulty = parser.nextText(); }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            //ignore
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        //String endTag = parser.getName();
                        //if(endTag.equals("body")) { Log.v("LOLMESSENGER", "EOP"); }
                        break;
                }//end switch
                eventType = parser.next();
            }//end while

            //essential : <gameType>COOP_VS_AI</gameType><groupId></groupId><mapId>1</mapId><queueId>7</queueId><gameMode>classic_pve</gameMode><gameDifficulty>MEDIUM</gameDifficulty>
            if(gameType != null) {
                if(gameType.contains("NORMAL"))        gameType = LolMessengerApplication.getContext().getString(R.string.match_type_normal);
                else if(gameType.contains("RANKED"))   gameType = LolMessengerApplication.getContext().getString(R.string.match_type_ranked);
                else if(gameType.contains("PRACTICE")) gameType = LolMessengerApplication.getContext().getString(R.string.match_type_custom);
                else if(gameType.contains("COOP"))     gameType = LolMessengerApplication.getContext().getString(R.string.match_type_coop);
            }

            if(gameMode != null) {
                if(gameMode.contains("classic"))   gameMode = LolMessengerApplication.getContext().getString(R.string.match_mode_classic);
                else if(gameMode.contains("odin")) gameMode = LolMessengerApplication.getContext().getString(R.string.match_mode_odin);
                else if(gameMode.contains("aram")) gameMode = LolMessengerApplication.getContext().getString(R.string.match_mode_aram);
            } else if(gameTypeIndex != null) {
                if(gameTypeIndex.contains("PICK_BLIND"))   gameMode = LolMessengerApplication.getContext().getString(R.string.match_mode_pick_blind);
                else if(gameTypeIndex.contains("PICK_RANDOM")) gameMode = LolMessengerApplication.getContext().getString(R.string.match_mode_pick_random);
                else if(gameTypeIndex.contains("DRAFT_STD")) gameMode = LolMessengerApplication.getContext().getString(R.string.match_mode_draft_std);
                else if(gameTypeIndex.contains("DRAFT_TOURNAMENT")) gameMode = LolMessengerApplication.getContext().getString(R.string.match_mode_draft_tournament);
            }

            parsedMessage = String.format(LolMessengerApplication.getContext().getString(R.string.message_invite_game),
                    gameMode,
                    LolMessengerApplication.getMapStringMap().get(mapId),
                    gameType);

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            //this.mode = "";
        }

        return parsedMessage;
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
