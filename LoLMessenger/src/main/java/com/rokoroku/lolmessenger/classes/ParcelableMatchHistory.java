package com.rokoroku.lolmessenger.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.achimala.leaguelib.models.LeagueSummoner;
import com.achimala.leaguelib.models.MatchHistoryEntry;
import com.achimala.leaguelib.models.MatchHistoryStatType;
import com.rokoroku.lolmessenger.R;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by Youngrok Kim on 13. 8. 28.
 */
public class ParcelableMatchHistory implements Parcelable {

    private int    matchID;
    private String matchType;
    private String matchMode;
    private int    matchMapID;
    private Date   matchDate;
    private String champion;
    private int    earnedIP;
    private int    earnedEXP;
    private int    kill;
    private int    death;
    private int    assist;
    private boolean leave;
    private boolean victory;

    public ParcelableMatchHistory() {
    }

    public ParcelableMatchHistory(MatchHistoryEntry matchHistoryEntry, LeagueSummoner summoner) {
        matchID   = matchHistoryEntry.getGameId();
        matchType = matchHistoryEntry.getQueue().name();
        matchMode = matchHistoryEntry.getGameMode();
        matchMapID  = matchHistoryEntry.getGameMapID();
        matchDate = matchHistoryEntry.getCreationDate();
        if(matchHistoryEntry.getChampionSelectionForSummoner(summoner).getName() != null) {
            champion  = (matchHistoryEntry.getChampionSelectionForSummoner(summoner).getName());
        } else {
            champion = String.valueOf( matchHistoryEntry.getChampionSelectionForSummoner(summoner).getName() );
            Log.e("matchhistory", "unparsable champion : " + champion);
        }
        earnedIP  = matchHistoryEntry.getIpEarned();
        earnedEXP  = matchHistoryEntry.getExpEarned();
        kill   = matchHistoryEntry.getStat(MatchHistoryStatType.CHAMPIONS_KILLED);
        death  = matchHistoryEntry.getStat(MatchHistoryStatType.NUM_DEATHS);
        assist = matchHistoryEntry.getStat(MatchHistoryStatType.ASSISTS);
        victory= matchHistoryEntry.getStat(MatchHistoryStatType.WIN)>0;
    }

    public int getMatchID() {
        return matchID;
    }

    public void setMatchID(int matchID) {
        this.matchID = matchID;
    }

    public String getChampion() {
        return champion;
    }

    public void setChampion(String champion) {
        this.champion = champion;
    }

    public int getEarnedEXP() {
        return earnedEXP;
    }

    public void setEarnedEXP(int earnedEXP) {
        this.earnedEXP = earnedEXP;
    }

    public int getMatchMapID() {
        return matchMapID;
    }

    public void setMatchMapID(int matchMapID) {
        this.matchMapID = matchMapID;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getMatchMode() {
        return matchMode;
    }

    public void setMatchMode(String matchMode) {
        this.matchMode = matchMode;
    }

    public Date getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(Date matchDate) {
        this.matchDate = matchDate;
    }

    public int getEarnedIP() {
        return earnedIP;
    }

    public void setEarnedIP(int earnedIP) {
        this.earnedIP = earnedIP;
    }

    public boolean isLeave() {
        return leave;
    }

    public void setLeave(boolean leave) {
        this.leave = leave;
    }

    public boolean isVictory() {
        return victory;
    }

    public void setVictory(boolean victory) {
        this.victory = victory;
    }

    public int getKill() {
        return kill;
    }

    public void setKill(int kill) {
        this.kill = kill;
    }

    public int getDeath() {
        return death;
    }

    public void setDeath(int death) {
        this.death = death;
    }

    public int getAssist() {
        return assist;
    }

    public void setAssist(int assist) {
        this.assist = assist;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(matchID);
        parcel.writeString(matchType);
        parcel.writeString(matchMode);
        parcel.writeInt(matchMapID);
        parcel.writeLong(matchDate.getTime());
        parcel.writeString(champion);
        parcel.writeInt(earnedIP);
        parcel.writeInt(earnedEXP);
        parcel.writeInt(kill);
        parcel.writeInt(death);
        parcel.writeInt(assist);
        parcel.writeInt(leave ? 1 : 0);
        parcel.writeInt(victory ? 1 : 0);
    }

    public static final Parcelable.Creator<ParcelableMatchHistory> CREATOR = new Creator<ParcelableMatchHistory>() {
        public ParcelableMatchHistory createFromParcel(Parcel source) {
            ParcelableMatchHistory r = new ParcelableMatchHistory();
            r.matchID = source.readInt();
            r.matchType = source.readString();
            r.matchMode = source.readString();
            r.matchMapID = source.readInt();
            r.matchDate = new Date(source.readLong());
            r.champion = source.readString();
            r.earnedIP = source.readInt();
            r.earnedEXP = source.readInt();
            r.kill   = source.readInt();
            r.death  = source.readInt();
            r.assist = source.readInt();
            r.leave  = source.readInt()>0;
            r.victory= source.readInt()>0;
            return r;
        }
        public ParcelableMatchHistory[] newArray(int size) {
            return new ParcelableMatchHistory[size];
        }
    };
}
