package com.rokoroku.lolmessenger.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.achimala.leaguelib.models.LeagueSummoner;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.packet.Presence;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * Created by Youngrok Kim on 13. 8. 13.
 */
public class ParcelableRoster implements Parcelable {

    //Roster Field
    private String userID;
    private String userName;
    private String group;

    //Presense Field
    private String availablity = "unavailable";
    private String mode = "chat";

    //Presense Status Field
    private int profileIcon = 0;         // 안되는듯
    private int summonerLevel = 0;        // 30
    private String statusMsg;             // 내 상태
    private int wins = 0;                 // 1000
    private int leaves = 0;               // 0
    private int odinWins = 0;             // 도미니언
    private int rankedWins = 0;           // 500
    private int rankedLosses = 0;         // 0
    private String rankedLeagueName;      // Taric&apos;s Elementalists
    private String rankedLeagueDivision;  // IV
    private String rankedLeagueTier;      // PLATINUM
    private String rankedLeagueQueue;     // RANKED_SOLO_5x5, RANKED_TEAM_5x5
    private String gameQueueType;         // NORMAL, ARAM_UNRANKED_5x5,
    private String skinname;              // Sona, Random, ...
    private String gameStatus;            // OutOfGame
    private long timeStamp;               // 1376385325742

    //Field from LeagueSummoner
    private int leaguePoints;

    //Constructor
    public ParcelableRoster() {
    }

    //Constructor2
    public ParcelableRoster(RosterEntry entry, Presence presence) {

        if(entry != null) {
            // entry data 파싱
            this.userID = entry.getUser();
            this.userName = entry.getName();
            for(RosterGroup rosterGroup : entry.getGroups()) { this.group = rosterGroup.getName(); }
        }

        if(presence != null) {
            // 접속상태 확인
            this.availablity = presence.getType().toString();
            if(availablity.equals("available")) parseStatusString( presence.getStatus() );
            if(presence.getMode() != null) this.mode = presence.getMode().toString();
        }
    }

    public ParcelableRoster(LeagueSummoner summoner) {
        this.userID = new String("sum" + summoner.getId() + "@pvp.net");
        fillSummonerData(summoner);
    }

    public void fillSummonerData(LeagueSummoner summoner) {

        this.summonerLevel = summoner.getLevel();
        this.userName = summoner.getName();
        this.profileIcon = summoner.getProfileIconId();
        //this.loses = summoner.getLeagueStats().getLosses();

        if(summoner.getLeagueStats()!=null) {
            this.rankedWins = summoner.getLeagueStats().getWins();
            this.rankedLeagueTier = summoner.getLeagueStats().getTier().name();
            this.rankedLeagueDivision = summoner.getLeagueStats().getRank().name();
            this.rankedLeagueName = summoner.getLeagueStats().getLeagueName();
            this.rankedLosses = summoner.getLeagueStats().getLosses();
            this.leaguePoints = summoner.getLeagueStats().getLeaguePoints();
        } else {
            this.rankedLeagueTier = "UNRANKED";
        }
    }

    public void parseStatusString(String status) {

        // presence status 파싱
        XmlPullParserFactory factory = null;

        try {
            boolean initbody = false;
            factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(status));
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {
                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        String startTag = parser.getName();
                        if(initbody != true && startTag.equals("body")) { initbody = true; }
                        else if(initbody == true) try {
                            if(startTag.equals("profileIcon")) { profileIcon = Integer.parseInt(parser.nextText()); }
                            if(startTag.equals("level")) { summonerLevel = Integer.parseInt(parser.nextText()); }
                            if(startTag.equals("wins")) { wins = Integer.parseInt(parser.nextText()); }
                            if(startTag.equals("leaves")) { leaves = Integer.parseInt(parser.nextText()); }
                            if(startTag.equals("odinWins")) { odinWins = Integer.parseInt(parser.nextText()); }
                            //if(startTag.equals("odinLeaves")) { odinLeaves = Integer.parseInt(parser.nextText()); }
                            //if(startTag.equals("queueType")) { queueType = parser.nextText(); }
                            //if(startTag.equals("rankedLosses")) { rankedLosses = Integer.parseInt(parser.nextText()); }
                            //if(startTag.equals("tier")) { tier = parser.nextText(); }
                            if(startTag.equals("statusMsg")) { statusMsg = parser.nextText(); }
                            if(startTag.equals("rankedLeagueName")) { rankedLeagueName = parser.nextText(); }
                            if(startTag.equals("rankedLeagueDivision")) { rankedLeagueDivision = parser.nextText(); }
                            if(startTag.equals("rankedLeagueTier")) { rankedLeagueTier = parser.nextText(); }
                            if(startTag.equals("rankedLeagueQueue")) { rankedLeagueQueue = parser.nextText(); }
                            if(startTag.equals("skinname")) { skinname = parser.nextText(); }
                            if(startTag.equals("gameQueueType")) { gameQueueType = parser.nextText(); }
                            if(startTag.equals("gameStatus")) { gameStatus = parser.nextText(); }
                            if(startTag.equals("timeStamp")) { timeStamp = Long.parseLong( parser.nextText()); }
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

        } catch (XmlPullParserException e) {
            e.printStackTrace();
            Log.i("ParcelableRoster", "weird user infromation :" + userID );
            this.statusMsg = "";
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Log.i("ParcelableRoster", "weird user infromation :" + userID + " (" + userName +")");
            this.statusMsg = "";
        }
    }


    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getAvailablity() {
        return availablity;
    }

    public void setAvailablity(String availablity) {
        this.availablity = availablity;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getProfileIcon() {
        return profileIcon;
    }

    public void setProfileIcon(int profileIcon) {
        this.profileIcon = profileIcon;
    }

    public int getSummonerLevel() {
        return summonerLevel;
    }

    public void setSummonerLevel(int summonerLevel) {
        this.summonerLevel = summonerLevel;
    }

    public String getStatusMsg() {
        return statusMsg;
    }

    public void setStatusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLeaves() {
        return leaves;
    }

    public void setLeaves(int leaves) {
        this.leaves = leaves;
    }

    public int getOdinWins() {
        return odinWins;
    }

    public void setOdinWins(int odinWins) {
        this.odinWins = odinWins;
    }

    public int getRankedWins() {
        return rankedWins;
    }

    public void setRankedWins(int rankedWins) {
        this.rankedWins = rankedWins;
    }

    public int getRankedLosses() {
        return rankedLosses;
    }

    public void setRankedLosses(int rankedLosses) {
        this.rankedLosses = rankedLosses;
    }

    public String getRankedLeagueName() {
        return rankedLeagueName;
    }

    public void setRankedLeagueName(String rankedLeagueName) {
        this.rankedLeagueName = rankedLeagueName;
    }

    public String getRankedLeagueDivision() {
        return rankedLeagueDivision;
    }

    public void setRankedLeagueDivision(String rankedLeagueDivision) {
        this.rankedLeagueDivision = rankedLeagueDivision;
    }

    public String getRankedLeagueTier() {
        return rankedLeagueTier;
    }

    public void setRankedLeagueTier(String rankedLeagueTier) {
        this.rankedLeagueTier = rankedLeagueTier;
    }

    public String getRankedLeagueQueue() {
        return rankedLeagueQueue;
    }

    public void setRankedLeagueQueue(String rankedLeagueQueue) {
        this.rankedLeagueQueue = rankedLeagueQueue;
    }

    public int getLeaguePoints() {
        return leaguePoints;
    }

    public void setLeaguePoints(int leaguePoints) {
        this.leaguePoints = leaguePoints;
    }

    public String getGameQueueType() {
        return gameQueueType;
    }

    public void setGameQueueType(String gameQueueType) {
        this.gameQueueType = gameQueueType;
    }

    public String getSkinname() {
        return skinname;
    }

    public void setSkinname(String skinname) {
        this.skinname = skinname;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(String gameStatus) {
        this.gameStatus = gameStatus;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String toPresencePacketString() {
        String statusMsgText = statusMsg;
        if(statusMsgText != null) {
            statusMsgText = statusMsgText.replace("&", "&amp;");
            statusMsgText = statusMsgText.replace("<", "&lt;");
            statusMsgText = statusMsgText.replace(">", "&gt;");
        }

        String status = "<body>"
                + "<profileIcon>" + profileIcon + "</profileIcon>"
                + ((summonerLevel > 0) ? "<level>" + summonerLevel + "</level>" : "<level />" )
                + "<wins>" + wins + "</wins>"
                + "<leaves>" + leaves + "</leaves>"
                + "<odinWins>" + odinWins + "</odinWins>"
                + "<odinLeaves>0</odinLeaves>"
                + "<queueType />"
                + ((rankedWins > 0) ? "<rankedWins>" + wins + "</rankedWins>" : "" )
                + "<rankedLosses>0</rankedLosses>"
                + "<rankedRating>0</rankedRating>"
                + (rankedLeagueName != null ? "<rankedLeagueName>" + rankedLeagueName + "</rankedLeagueName>" : "<rankedLeagueName />")
                + (rankedLeagueDivision != null? "<rankedLeagueDivision>" + rankedLeagueDivision + "</rankedLeagueDivision>" : "<rankedLeagueDivision />")
                + (rankedLeagueTier != null ? ( !rankedLeagueTier.equals("UNRANKED") ? "<rankedLeagueTier>" + rankedLeagueTier + "</rankedLeagueTier>" : "<rankedLeagueTier />" ) : "<rankedLeagueTier />")
                + (rankedLeagueQueue != null ? "<rankedLeagueQueue>" + rankedLeagueQueue + "</rankedLeagueQueue>" : "<rankedLeagueQueue />")
                + "<gameStatus>outOfGame</gameStatus>"
                + (statusMsg != null ? "<statusMsg>" + statusMsgText + "</statusMsg>" : "<statusMsg />")
                + "</body>\n";

        return status;

    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(userID);
        parcel.writeString(userName);
        parcel.writeString(group);
        parcel.writeString(availablity);
        parcel.writeString(mode);
        parcel.writeInt(profileIcon);
        parcel.writeInt(summonerLevel);
        parcel.writeString(statusMsg);
        parcel.writeInt(wins);
        parcel.writeInt(leaves);
        parcel.writeInt(odinWins);
        parcel.writeInt(rankedWins);
        parcel.writeInt(rankedLosses);
        parcel.writeString(rankedLeagueName);
        parcel.writeString(rankedLeagueDivision);
        parcel.writeString(rankedLeagueTier);
        parcel.writeString(rankedLeagueQueue);
        parcel.writeString(gameQueueType);
        parcel.writeString(skinname);
        parcel.writeString(gameStatus);
        parcel.writeLong(timeStamp);
        parcel.writeInt(leaguePoints);
    }

    public static final Parcelable.Creator<ParcelableRoster> CREATOR = new Creator<ParcelableRoster>() {
        public ParcelableRoster createFromParcel(Parcel source) {
            ParcelableRoster r = new ParcelableRoster();
            r.userID   = source.readString();
            r.userName    = source.readString();
            r.group    = source.readString();
            r.availablity  = source.readString();
            r.mode = source.readString();
            r.profileIcon = source.readInt();
            r.summonerLevel = source.readInt();
            r.statusMsg = source.readString();
            r.wins = source.readInt();
            r.leaves = source.readInt();
            r.odinWins = source.readInt();
            r.rankedWins = source.readInt();
            r.rankedLosses = source.readInt();
            r.rankedLeagueName = source.readString();
            r.rankedLeagueDivision = source.readString();
            r.rankedLeagueTier = source.readString();
            r.rankedLeagueQueue = source.readString();
            r.gameQueueType = source.readString();
            r.skinname = source.readString();
            r.gameStatus = source.readString();
            r.timeStamp = source.readLong();
            r.leaguePoints = source.readInt();
            return r;
        }
        public ParcelableRoster[] newArray(int size) {
            return new ParcelableRoster[size];
        }
    };
}
