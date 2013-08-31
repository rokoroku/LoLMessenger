package com.rokoroku.lolmessenger.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.rokoroku.lolmessenger.classes.ChatInformation;
import com.rokoroku.lolmessenger.classes.ParcelableMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Youngrok Kim on 13. 8. 19.
 */
public class SQLiteDbAdapter extends SQLiteOpenHelper {

    // All Static variables
    private static final String TAG = "SQLiteOpenAdapter";

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "lolmessenger";

    // Contacts table name
    private static final String TABLE_NAME = "chatlog";

    // Contacts Table Columns names
    private static final String KEY_FROM = "from_id";
    private static final String KEY_TO = "to_id";
    private static final String KEY_BODY = "msg_body";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_FLAG = "flag";

    public SQLiteDbAdapter(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
                + KEY_FROM + " TEXT,"
                + KEY_TO + " TEXT,"
                + KEY_BODY + " TEXT,"
                + KEY_TIMESTAMP + " INTEGER,"    //The value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the magnitude of the value.
                + KEY_FLAG + " INTEGER"
                + ")";
        db.execSQL(query);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

        // Create tables again
        onCreate(db);
    }

    /**
     * CRUD Operations
     */

    // Create
    public void addRecord(ParcelableMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_FROM, message.getFromID());
        values.put(KEY_TO, message.getToID());
        values.put(KEY_BODY, message.getBody());
        values.put(KEY_TIMESTAMP, message.getTimeStamp());
        values.put(KEY_FLAG, message.isFlag() ? 1 : 0);

        // Inserting Row
        db.insert(TABLE_NAME, null, values);
        //db.close(); // Closing database connection
    }

    // Retrieve
    public List<ParcelableMessage> getMessages(String user, String buddy) {
        List<ParcelableMessage> messageList = new ArrayList<ParcelableMessage>();

        // Select All Query
        /*String selectQuery = "SELECT  * FROM " + TABLE_NAME
                + " WHERE " + KEY_FROM + " = " + from
                + " AND " + KEY_TO + " = " + to
                + " UNION ALL "
                + " SELECT  * FROM " + TABLE_NAME
                + " WHERE " + KEY_FROM + " = " + to
                + " AND " + KEY_TO + " = " + from
                + " ORDER BY " + KEY_TIMESTAMP;
        */
        String query = "SELECT * FROM " + TABLE_NAME
                + " WHERE (" + KEY_FROM + " = '" + user
                + "' AND " + KEY_TO + " = '" + buddy
                + "' ) OR (" + KEY_FROM + " = '" + buddy
                + "' AND " + KEY_TO + " = '" + user
                + "' ) ORDER BY " + KEY_TIMESTAMP;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                ParcelableMessage message = new ParcelableMessage();
                message.setFromID(cursor.getString(0));
                message.setToID(cursor.getString(1));
                message.setBody(cursor.getString(2));
                message.setTimeStamp(cursor.getLong(3));
                if( cursor.getInt(4) > 0 ) { message.setFlag(); }
                else { message.unsetFlag(); }
                messageList.add(message);
            } while (cursor.moveToNext());
        }

        // return contact list
        return messageList;
    }

    // Update
    public int updateRecord(ParcelableMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_FROM, message.getFromID());
        values.put(KEY_TO, message.getToID());
        values.put(KEY_BODY, message.getBody());
        values.put(KEY_TIMESTAMP, message.getTimeStamp());
        values.put(KEY_FLAG, message.isFlag() ? 1 : 0);

        // updating row
        return db.update(TABLE_NAME, values, String.format("%s = ?", KEY_TIMESTAMP),
                new String[]{String.valueOf(message.getTimeStamp())});
    }

    // Update
    public int updateRecordsAsRead(String user, String buddy) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_NAME
                + " SET " + KEY_FLAG + " = 0 "
                + " WHERE (" + KEY_FROM + " = '" + user
                + "' AND " + KEY_TO + " = '" + buddy
                + "' ) OR (" + KEY_FROM + " = '" + buddy
                + "' AND " + KEY_TO + " = '" + user
                + "') AND " + KEY_FLAG + " = 1";

        // updating row
        Cursor cursor = db.rawQuery(query, null);
        return cursor.getCount();
    }

    public int getNotReadMessageCount(String user) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME
                + " WHERE " + KEY_TO + " = '" + user
                + "' AND " + KEY_FLAG + " = 1";

        // updating row
        Cursor cursor = db.rawQuery(query, null);
        return cursor.getCount();
    }

    // Delete related records
    public int deleteRecords(String user, String buddy) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME
                + " WHERE (" + KEY_FROM + " = '" + user
                + "' AND " + KEY_TO + " = '" + buddy + "')"
                + " OR (" + KEY_FROM + " = '" + buddy
                + "' AND " + KEY_TO + " = '" + user + "')";
        Cursor cursor = db.rawQuery(query, null);
        return cursor.getCount();
    }

    // Delete related records
    public int deleteAllRecords(String user) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME
                + " WHERE (" + KEY_FROM + " = '" + user
                + " OR " + KEY_TO + " = '" + user+ "'";
        Cursor cursor = db.rawQuery(query, null);
        return cursor.getCount();
    }

    public ArrayList<ChatInformation> getChatList(String user) {
        String query;
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<String> idList = new ArrayList<String>();
        ArrayList<ChatInformation> chatList = new ArrayList<ChatInformation>();

        //1. get idList related with USERID
        query = "SELECT * FROM " + TABLE_NAME
                + " WHERE (" + KEY_TO + " = '" + user
                + "' OR " + KEY_FROM + " = '" + user
                + "') ORDER BY " + KEY_TIMESTAMP + " DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String buddyID;
                // From USER
                if(cursor.getString(0).equals(user)) {
                    buddyID = cursor.getString(1);
                } else {
                    buddyID = cursor.getString(0);
                }

                //new entry
                if(!idList.contains(buddyID)) {
                    idList.add(buddyID);
                    ParcelableMessage message = new ParcelableMessage();
                    message.setFromID(cursor.getString(0));
                    message.setToID(cursor.getString(1));
                    message.setBody(cursor.getString(2));
                    message.setTimeStamp(cursor.getLong(3));
                    if( cursor.getInt(4) > 0 ) { message.setFlag(); }
                    else { message.unsetFlag(); }
                    ChatInformation chatInfo = new ChatInformation(buddyID, message, cursor.getInt(4));
                    chatList.add(chatInfo);
                }
                //existing entry : just increment message count
                else if( cursor.getInt(4) > 0 ) chatList.get(idList.indexOf(buddyID)).incrementCount();
            } while (cursor.moveToNext());
        }

        //2. return
        return chatList;
    }

}
