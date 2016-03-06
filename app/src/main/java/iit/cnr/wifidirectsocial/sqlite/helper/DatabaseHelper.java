
package iit.cnr.wifidirectsocial.sqlite.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import iit.cnr.wifidirectsocial.sqlite.model.Group;
import iit.cnr.wifidirectsocial.sqlite.model.Group_peer;
import iit.cnr.wifidirectsocial.sqlite.model.Peer;

/**
 * Created by sorbeppe84 on 14/07/2015.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    // Logcat tag
    private static final String LOG = "DatabaseHelper";

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "database.sql";

    // Table Names
    private static final String TABLE_GROUP = "groups";
    private static final String TABLE_PEER = "peers";
    private static final String TABLE_GROUP_PEER = "group_peers";

    // Common column names
    private static final String KEY_ID = "id";
    private static final String KEY_CREATED_AT = "created_at";

    // GROUPS Table - column names

    private static final String KEY_MAC_GO = "mac_go";
    private static final String KEY_GROUP_NAME = "group_name";
    private static final String KEY_DESTROYED_AT = "destroyed_at";
    private static final String KEY_LAST_TIME_SEEN = "last_time_seen";
    // PEERS Table - column names
    private static final String KEY_MAC_PEER = "mac_peer";
    private static final String KEY_ADDED_AT = "added_at";

    // GROUP_PEER Table - column names
    private static final String KEY_GROUP_ID = "group_id";
    private static final String KEY_MAC_ADDRESS_GO = "mac_address_go";
    private static final String KEY_PEER_ID = "peer_id";
    private static final String KEY_LAST_TIME_STAMP = "last_time_stamp";

    // Table Create Statements

    // Group table create statement
    private static final String CREATE_TABLE_GROUP = "CREATE TABLE IF NOT EXISTS "
            + TABLE_GROUP + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_MAC_GO
            + " TEXT," + KEY_GROUP_NAME + " TEXT," + KEY_CREATED_AT
            + " TEXT, " + KEY_DESTROYED_AT + " TEXT, " + KEY_LAST_TIME_SEEN + " TEXT " + ")";

    // Peer table create statement
    private static final String CREATE_TABLE_PEER = "CREATE TABLE IF NOT EXISTS " + TABLE_PEER
            + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT ," + KEY_MAC_PEER + " TEXT, " + KEY_ADDED_AT + " TEXT " + ")";

    // Group_peer table create statement
    private static final String CREATE_TABLE_GROUP_PEER = "CREATE TABLE IF NOT EXISTS "
            + TABLE_GROUP_PEER + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_MAC_ADDRESS_GO + " TEXT," + KEY_GROUP_NAME + " TEXT," + KEY_PEER_ID + " INTEGER,"
            + KEY_LAST_TIME_STAMP + " TEXT" + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }



    @Override
    public void onCreate(SQLiteDatabase db) {

        // creating required tables
        db.execSQL(CREATE_TABLE_GROUP);
        db.execSQL(CREATE_TABLE_PEER);
        db.execSQL(CREATE_TABLE_GROUP_PEER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // on upgrade drop older tables

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUP);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PEER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUP_PEER);

        // create new tables

        onCreate(db);
    }


    // ------------------------ "groups" table methods ----------------//

    /**
     * Creating a group with peers
     */
    public long createGroup(Group group, long[] peer_ids) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MAC_GO, group.getMacAddressGo());
        values.put(KEY_GROUP_NAME, group.getGroupName());
        values.put(KEY_CREATED_AT, getDateTime());
        values.put(KEY_DESTROYED_AT,"");
        values.put(KEY_LAST_TIME_SEEN,"");
        // insert row
        long group_id = db.insert(TABLE_GROUP, null, values);

        // assigning peers to group

        for (long peer_id : peer_ids) {
            createGroupPeer(group.getMacAddressGo(),group.getGroupName(), peer_id);
        }


        return group_id;
    }

    /**
     * Creating a group without peers
     */
    public long createGroup(Group group) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MAC_GO, group.getMacAddressGo());
        values.put(KEY_GROUP_NAME, group.getGroupName());
        values.put(KEY_CREATED_AT, getDateTime());
        values.put(KEY_DESTROYED_AT, group.getDestroyedAt());
        values.put(KEY_LAST_TIME_SEEN, group.getLastTimeSeen());
        // insert row
        long group_id = db.insert(TABLE_GROUP, null, values);

        Log.i(LOG,"Il gruppo: " + group.getGroupName() + " creato dal GO con mac_address: "+ group.getMacAddressGo() + " e' stato aggiunto sul database ");

        return group_id;
    }

    /*
  * Ritorna il numero di volte che e' stato creato un gruppo con lo stesso nome
  * */
    public int getNrOccurrencesGreatestGroup(String mac_address_go,String group_name) {
        int count = 0;

        String selectQuery = "SELECT  * FROM " + TABLE_GROUP + " WHERE "
                + KEY_MAC_GO + " LIKE '" + mac_address_go + "' AND " + KEY_GROUP_NAME + " LIKE '" + group_name + "'";
        //String selectQuery = "SELECT  COUNT(*) FROM " + TABLE_GROUP + " WHERE "
        //        + KEY_GROUP_NAME + " = " + groupName;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {

                count = count + 1;

            } while (c.moveToNext());
        }

        return count;
    }

    /*
* Ritorna la durata totale (in secondi) di un GROUP_NAME attraverso la somma delle singole occorrenze dello stesso Gruppo (Stesso mac_go e group_name)
* */
    public long getTotalLifeTimeGreatestGroup(String mac_address_go,String group_name) {
        long totalLifeTime = 0;
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP + " WHERE "
                + KEY_MAC_GO + " LIKE '" + mac_address_go + "' AND " + KEY_GROUP_NAME + " LIKE '" + group_name + "'";

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                long lifeTimeSingleIdGroupName = diffTraDueDate(c.getString(c.getColumnIndex(KEY_CREATED_AT)),c.getString(c.getColumnIndex(KEY_DESTROYED_AT)));
                totalLifeTime = totalLifeTime + lifeTimeSingleIdGroupName;

            } while (c.moveToNext());
        }

        return totalLifeTime/1000;
    }

    /*
* Ritorna il valore in sec della recency (quanto recentemente ho visto il gruppo passato in ingresso)
* */
    public long getRecencyGreatestGroup(String mac_address_go, String group_name) {
        long recency = 0 ;
        //Tale query ritorna il valore massimo(ovvero l'istante di tempo piu' recente in secondi) tra le due colonne destroyed_at e last_time_seen
        String selectQuery = "select case when destroyed_at !=" + "" +  " then max(strftime('%s', destroyed_at), strftime('%s', last_time_seen))" +
                "else strftime('%s', last_time_seen) end" +  "from groups" +  "order by case when destroyed_at !=" + "" +
                "then max(strftime('%s', destroyed_at), strftime('%s', last_time_seen)) else strftime('%s', last_time_seen) end" + " desc limit 1";

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery,null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            //Ritorna il valore della query in secondi
            long valueFromQuery = c.getInt(0);
            long secSinceEpocTime = getNumberOfSecondsSinceEpocTime(getDateTime());
            recency = secSinceEpocTime - valueFromQuery;

        }

        return recency;
    }


    /*
* getting all groups
* */
    public List<Group> getAllOccurencesOfTheSameGroup(String mac_address_go,String group_name) {
        List<Group> groups = new ArrayList<Group>();
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP + " WHERE "
                + KEY_MAC_GO + " LIKE '" + mac_address_go + "' AND " + KEY_GROUP_NAME + " LIKE '" + group_name + "'";

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Group gp = new Group();
                gp.setId(c.getInt(c.getColumnIndex(KEY_ID)));
                gp.setMacAddressGo((c.getString(c.getColumnIndex(KEY_MAC_GO))));
                gp.setGroupName(c.getString(c.getColumnIndex(KEY_GROUP_NAME)));
                gp.setCreatedAt(c.getString(c.getColumnIndex(KEY_CREATED_AT)));
                gp.setDestroyedAt(c.getString(c.getColumnIndex(KEY_DESTROYED_AT)));
                gp.setLastTimeSeen(c.getString(c.getColumnIndex(KEY_LAST_TIME_SEEN)));
                // adding to Group list
                groups.add(gp);
            } while (c.moveToNext());
        }

        return groups;
    }



    /*
* get single group
*/
    public Group getGroupByMacGoAndGroupName(String mac_address_go, String group_name) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_GROUP + " WHERE "
                + KEY_MAC_GO + " LIKE '" + mac_address_go + "' AND " + KEY_GROUP_NAME + " LIKE '" + group_name + "'";
        Log.e(LOG, selectQuery);

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null)
            c.moveToFirst();

        Group gp = new Group();
        gp.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        gp.setMacAddressGo((c.getString(c.getColumnIndex(KEY_MAC_GO))));
        gp.setGroupName(c.getString(c.getColumnIndex(KEY_GROUP_NAME)));
        gp.setCreatedAt(c.getString(c.getColumnIndex(KEY_CREATED_AT)));
        gp.setDestroyedAt(c.getString(c.getColumnIndex(KEY_DESTROYED_AT)));
        gp.setLastTimeSeen(c.getString(c.getColumnIndex(KEY_LAST_TIME_SEEN)));
        return gp;
    }


    /*
 * get single group
 */
    public Group getGroupById(long group_id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_GROUP + " WHERE "
                + KEY_ID + " = " + group_id;

        Log.e(LOG, selectQuery);

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null)
            c.moveToFirst();

        Group gp = new Group();
        gp.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        gp.setMacAddressGo((c.getString(c.getColumnIndex(KEY_MAC_GO))));
        gp.setGroupName(c.getString(c.getColumnIndex(KEY_GROUP_NAME)));
        gp.setCreatedAt(c.getString(c.getColumnIndex(KEY_CREATED_AT)));
        gp.setDestroyedAt(c.getString(c.getColumnIndex(KEY_DESTROYED_AT)));
        gp.setLastTimeSeen(c.getString(c.getColumnIndex(KEY_LAST_TIME_SEEN)));
        return gp;
    }




    /*
 * getting all groups
 * */
    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<Group>();
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Group gp = new Group();
                gp.setId(c.getInt(c.getColumnIndex(KEY_ID)));
                gp.setMacAddressGo((c.getString(c.getColumnIndex(KEY_MAC_GO))));
                gp.setGroupName(c.getString(c.getColumnIndex(KEY_GROUP_NAME)));
                gp.setCreatedAt(c.getString(c.getColumnIndex(KEY_CREATED_AT)));
                gp.setDestroyedAt(c.getString(c.getColumnIndex(KEY_DESTROYED_AT)));
                gp.setLastTimeSeen(c.getString(c.getColumnIndex(KEY_LAST_TIME_SEEN)));
                // adding to Group list
                groups.add(gp);
            } while (c.moveToNext());
        }

        return groups;
    }

    /*
* getting all groups
* */
    public void stampAllGroup() {
        List<Group> groups = new ArrayList<Group>();
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP;

        Log.e(LOG, selectQuery + " I gruppi memorizzati sul databse sono: ");

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Group gp = new Group();
                gp.setId(c.getInt(c.getColumnIndex(KEY_ID)));
                gp.setMacAddressGo((c.getString(c.getColumnIndex(KEY_MAC_GO))));
                gp.setGroupName(c.getString(c.getColumnIndex(KEY_GROUP_NAME)));
                gp.setCreatedAt(c.getString(c.getColumnIndex(KEY_CREATED_AT)));
                gp.setDestroyedAt(c.getString(c.getColumnIndex(KEY_DESTROYED_AT)));
                gp.setLastTimeSeen(c.getString(c.getColumnIndex(KEY_LAST_TIME_SEEN)));
                // display every group
                Log.e(LOG, KEY_ID + " " + gp.getId() + " " + KEY_MAC_GO + " " + gp.getMacAddressGo() + "  " + KEY_GROUP_NAME + " " +
                        gp.getGroupName() + " " + KEY_CREATED_AT + " " + gp.getCreatedAt() + " " + KEY_DESTROYED_AT + " " + gp.getDestroyedAt() + " " + KEY_LAST_TIME_SEEN +
                        " " + gp.getLastTimeSeen());

            } while (c.moveToNext());
        }


    }

    /*
  * Updating only column DESTROYED_AT group
 */
    public int updateGroupDestroyedAt(String mac_address_go,String group_name) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_DESTROYED_AT, getDateTime());

        Log.i(LOG, "Il timeStamp: " + getDateTime() + "in cui e' stato distrutto il gruppo: " + group_name +
                " creato dal GO con mac_address: " + mac_address_go + " e' stato aggiunto sul database");
        // updating row
        return db.update(TABLE_GROUP, values, KEY_MAC_GO + " = ?" + " AND " + KEY_GROUP_NAME + " = ? " ,
                new String[] { mac_address_go,group_name });


    }



    /*
   * Updating only column LAST_TIME_SEEN group
  */
    public int updateGroupLastTimeSeen(String mac_address_go,String group_name) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_LAST_TIME_SEEN, getDateTime());

        Log.i(LOG, "Il timeStamp: " + getDateTime() + " in cui ho visto l'ultima volta il gruppo: " + group_name + " creato dal GO con mac_address: " + mac_address_go + " e' stato aggiunto sul database");

        // updating row
        return db.update(TABLE_GROUP, values, KEY_MAC_GO + " = ?" + " AND " + KEY_GROUP_NAME + " = ? " ,
                new String[] { mac_address_go,group_name });

    }

    /*
     * Updating a group
    */
    public int updateGroup(Group group) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MAC_GO, group.getMacAddressGo());
        values.put(KEY_GROUP_NAME, group.getGroupName());
        values.put(KEY_CREATED_AT, getDateTime());

        // updating row
        return db.update(TABLE_GROUP, values, KEY_ID + " = ?",
                new String[] { String.valueOf(group.getId()) });
    }


    /*
     * Deleting a group
     */
    public void deleteGroup(long group_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GROUP, KEY_ID + " = ?",
                new String[] { String.valueOf(group_id) });
    }

    // Deleting all Group
    public void deleteAllGroup() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GROUP,null,null);
    }




    // ------------------------ "peers" table methods ----------------//

    /**
     * Creating peer
     */
    public long createPeer(Peer peer) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MAC_PEER, peer.getMacAddress());
        values.put(KEY_ADDED_AT, peer.getAddedAt());

        //insert row
        long peer_id = db.insert(TABLE_PEER, null, values);

        Log.i(LOG, "Il peer con mac-address: " + peer.getMacAddress() + " e' stato aggiunto sul database");
        return peer_id;
    }



    /**
     * getting all peers
     * */
    public List<Peer> getAllPeers() {
        List<Peer> peers = new ArrayList<Peer>();
        String selectQuery = "SELECT  * FROM " + TABLE_PEER;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Peer p = new Peer();
                p.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                p.setMacAddress(c.getString(c.getColumnIndex(KEY_MAC_PEER)));
                p.setAddedAt(c.getString(c.getColumnIndex(KEY_ADDED_AT)));
                // adding to tags list
                peers.add(p);
            } while (c.moveToNext());
        }
        return peers;
    }


    /*
* get single PEER
*/
    public Peer getPeer(long peer_id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_PEER + " WHERE "
                + KEY_ID + " = " + peer_id;

        Log.e(LOG, selectQuery);

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null)
            c.moveToFirst();

        Peer p = new Peer();
        p.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        p.setMacAddress((c.getString(c.getColumnIndex(KEY_MAC_PEER))));
        p.setAddedAt(c.getString(c.getColumnIndex(KEY_ADDED_AT)));
        return p;
    }


    /*
* Verify if a Peer is content on database
*/
    public boolean isPeerContentInDatabase(String macAddress) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean content;

        String selectQuery = "SELECT  * FROM " + TABLE_PEER + " WHERE "
                + KEY_MAC_PEER + " LIKE '" + macAddress + "'";

        Log.e(LOG, selectQuery);

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null) {

            c.moveToFirst();
            content = true;
        } else {

            content = false;
        }
        return content;
    }



    /**
     * Updating a peer
     */
    public int updatePeer(Peer peer) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MAC_PEER, peer.getMacAddress());
        values.put(KEY_ADDED_AT, getDateTime());
        // updating row
        return db.update(TABLE_PEER, values, KEY_ID + " = ?",
                new String[] { String.valueOf(peer.getId()) });
    }


    //TODO: deletePeer
    /*
     * Deleting a peer
     */
    public void deletePeer(long peer_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PEER, KEY_ID + " = ?",
                new String[] { String.valueOf(peer_id) });
    }

    // Deleting all PEERS
    public void deleteAllPeer() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PEER,null,null);
    }



// ------------------------ "group_peer" table methods ----------------//

    /**
     * Creating group_peer
     */

    //TODO: capire che valore dare a KEY_LAST_TIME_STAMP

    public long createGroupPeer(String mac_address_go,String group_name, long peer_id) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MAC_ADDRESS_GO, mac_address_go);
        values.put(KEY_GROUP_NAME, group_name);
        values.put(KEY_PEER_ID, peer_id);
        values.put(KEY_LAST_TIME_STAMP, getDateTime());

        long id = db.insert(TABLE_GROUP_PEER, null, values);

        Log.i(LOG, "Il peer con ID: " + peer_id + " e'stato aggiunto sul database al gruppo: " + group_name + " il cui GO ha mac_address: " + mac_address_go );
        return id;
    }

    /**
     * getting all group_peer
     * */
    public List<Group_peer> getAllGroupPeer() {
        List<Group_peer> group_peer = new ArrayList<Group_peer>();
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP_PEER;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Group_peer gp = new Group_peer();
                gp.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                gp.setMacAddressGo(c.getString(c.getColumnIndex(KEY_MAC_ADDRESS_GO)));
                gp.setGroupName(c.getString(c.getColumnIndex(KEY_GROUP_NAME)));
                gp.setPeerId(c.getInt(c.getColumnIndex(KEY_PEER_ID)));
                gp.setLastTimeStamp(c.getString(c.getColumnIndex(KEY_LAST_TIME_STAMP)));
                // adding to tags list
                group_peer.add(gp);

            } while (c.moveToNext());
        }
        return group_peer;
    }

    /**
     * getting group_peer for the same group
     * */
    public List<Group_peer> getGroupPeer(String mac_address_go, String group_name) {
        List<Group_peer> group_peer = new ArrayList<Group_peer>();

        String selectQuery = "SELECT  * FROM " + TABLE_GROUP_PEER + " WHERE "
                + KEY_MAC_ADDRESS_GO + " LIKE '" + mac_address_go + "' AND " + KEY_GROUP_NAME + " LIKE '" + group_name + "'";

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Group_peer gp = new Group_peer();
                gp.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                gp.setMacAddressGo(c.getString(c.getColumnIndex(KEY_MAC_ADDRESS_GO)));
                gp.setGroupName(c.getString(c.getColumnIndex(KEY_GROUP_NAME)));
                gp.setPeerId(c.getInt(c.getColumnIndex(KEY_PEER_ID)));
                gp.setLastTimeStamp(c.getString(c.getColumnIndex(KEY_LAST_TIME_STAMP)));
                // adding to tags list
                group_peer.add(gp);

            } while (c.moveToNext());
        }
        return group_peer;
    }






    /**
     * display all group_peer and display
     * */
    public void stampAllGroupPeer() {
        ;
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP_PEER;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Group_peer gp = new Group_peer();
                gp.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                gp.setMacAddressGo(c.getString(c.getColumnIndex(KEY_MAC_ADDRESS_GO)));
                gp.setGroupName(c.getString(c.getColumnIndex(KEY_GROUP_NAME)));
                gp.setLastTimeStamp(c.getString(c.getColumnIndex(KEY_LAST_TIME_STAMP)));
                // adding to tags list
                Log.e(LOG, KEY_ID + " " + gp.getId() + " " + KEY_MAC_ADDRESS_GO + " " + gp.getMacAddressGo() + "  " + KEY_GROUP_NAME + " " + gp.getGroupName() + " "
                        + KEY_PEER_ID + " " + gp.getPeerId() + " " + KEY_LAST_TIME_STAMP + " " + gp.getLastTimeStamp());

            } while (c.moveToNext());
        }

    }

    /**
     * Updating a group peer
     */

    public int updateGroupPeer(long id, long peer_id) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PEER_ID, peer_id);

        // updating row
        return db.update(TABLE_GROUP, values, KEY_ID + " = ?",
                new String[] { String.valueOf(id) });
    }

    /**
     * Deleting a group peer
     */
    public void deleteGroupPeer(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GROUP, KEY_ID + " = ?",
                new String[] { String.valueOf(id) });
    }

    // Deleting all GROUP_PEERS
    public void deleteAllGroupPeer() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GROUP_PEER,null,null);
    }



    /**
     * Tal Metodo ritorna data e ora attuali
     * */
    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }


    // Calcola la differenza tra due Date (sottoforma di stringhe) date in ingresso in millisecondi

    public long diffTraDueDate(String strDate1,String strDate2 ) {

        try {


            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            fmt.setLenient(false);

            // Parses the two strings.
            Date d1 = fmt.parse(strDate1);
            Date d2 = fmt.parse(strDate2);

            // Calculates the difference in milliseconds.

            long millisDiff = d2.getTime() - d1.getTime();
            //System.out.println(millisDiff);
            return millisDiff;

        }  catch (Exception e) {

            System.err.println(e);

        }
        return 0;

    }

    //Ritorna il numero di secondi dalla data January 1, 1970, 00:00:00 GMT
    public long getNumberOfSecondsSinceEpocTime(String strDate1) {

        try {

            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            fmt.setLenient(false);

            // Parses the two strings.
            System.out.println(strDate1);
            Date d1 = fmt.parse(strDate1);

            // Calculates the difference in milliseconds.

            long millis = d1.getTime();
            long sec = millis/1000;
            System.out.println(sec);

            return sec;


        }  catch (Exception e) {

            System.err.println(e);

        }
        return 0;

    }



    // closing database
    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }



}