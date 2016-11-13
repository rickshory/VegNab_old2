package com.vegnab.vegnab.contentprovider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import com.vegnab.vegnab.BuildConfig;
import com.vegnab.vegnab.database.VegNabDbHelper;
import com.vegnab.vegnab.database.VNContract.LDebug;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;


public class ContentProvider_VegNab extends ContentProvider {
    private static final String LOG_TAG = ContentProvider_VegNab.class.getSimpleName();

    // database
    private VegNabDbHelper database; // = null; // to initialize ?

    // following class and hashmap avoids extreme error-prone redundancy of
    // simple query/insert/update/delete methods on tables
    static class TableStd {
        String tableName;
        String basePath;
        TableStd(String n, String p) {
            tableName = n;
            basePath = p;
        }
    }

    static HashMap<Integer, TableStd> tblHash = new HashMap<Integer, TableStd>();

    // used for the UriMatcher
    private static final int RAW_SQL = 1;

//	private static final String AUTHORITY = "com.vegnab.provider"; // must match in app Manifest
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";

    private static final String BASE_PATH = "data";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH);
    public static final Uri SQL_URI = Uri.parse("content://" + AUTHORITY
            + "/sql");
    private static final String CONTENT_SUBTYPE = "vnd.vegnab.data";
//    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_SUBTYPE;
//    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_SUBTYPE;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        // add any custom URI patterns
        sURIMatcher.addURI(AUTHORITY, "sql", RAW_SQL);
        if (LDebug.ON) Log.d(LOG_TAG, "added to sURIMatcher: sql, " + ", key: " + RAW_SQL);

        // set up the tables for standard methods, in their hash map
        tblHash.put(100, new TableStd("Projects","projects"));
        tblHash.put(110, new TableStd("Visits","visits"));
        tblHash.put(120, new TableStd("Locations","locations"));
        tblHash.put(130, new TableStd("LocationSources","locationsources"));
        tblHash.put(140, new TableStd("AccuracySources","accuracysources"));
        tblHash.put(150, new TableStd("Namers","namers"));
        tblHash.put(160, new TableStd("PlotTypes","plottypes"));
        tblHash.put(170, new TableStd("SubplotTypes","subplottypes"));
        tblHash.put(180, new TableStd("VegItems","vegitems"));
        tblHash.put(190, new TableStd("Placeholders","placeholders"));
        tblHash.put(200, new TableStd("PlaceHolderPix","placeholderpix"));
        tblHash.put(210, new TableStd("IdNamers","idnamers"));
        tblHash.put(220, new TableStd("IdRefs","idrefs"));
        tblHash.put(230, new TableStd("IdMethods","idmethods"));
        tblHash.put(240, new TableStd("IdLevels","idlevels"));
        tblHash.put(250, new TableStd("NRCSSpp","species"));
        tblHash.put(260, new TableStd("DocsCreated","docs"));
        tblHash.put(270, new TableStd("DocsCreatedTypes","doctypes"));
        tblHash.put(280, new TableStd("DocsSourcesTypes","docsources"));
        tblHash.put(290, new TableStd("PoliticalAdminAreas","political_admin_areas"));
        tblHash.put(300, new TableStd("Purchases","purchases"));
        // to implement new tables, enter the table name (correct spelling as
        // in the DB), and the base URI as desired.
        // Make sure the numbers never overlap any custom URI pattern

        if (LDebug.ON) Log.d(LOG_TAG, "tblHash.size: " + tblHash.size());

        // add all the standard URI patterns for each table
        for (HashMap.Entry<Integer, TableStd> t : tblHash.entrySet()) {
            // create two entries, one like:
            // sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/projects", PROJECTS);
            sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/" + t.getValue().basePath, t.getKey());
            if (LDebug.ON) Log.d(LOG_TAG, "added to sURIMatcher: " + t.getValue().basePath + ", key: " + t.getKey());
            // and one like:
            // sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/projects/#", PROJECT_ID);
            sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/" + t.getValue().basePath + "/#", ((Integer)t.getKey() + 1));
            if (LDebug.ON) Log.d(LOG_TAG, "added to sURIMatcher: " + t.getValue().basePath + "/#, key: " + ((Integer)t.getKey()+1));
        }
    }

    HashSet<String> mFields_Projects = new HashSet<String>();

    @Override
    public boolean onCreate() {
        database = new VegNabDbHelper(getContext());
        // get the list of fields from the table 'Projects' and populate a HashSet
        String s = "pragma table_info(Projects);";
        Cursor c = database.getReadableDatabase().rawQuery(s, null);
        while (c.moveToNext()) {
//			Log.d(LOG_TAG, "Project field added to HashMap: " + c.getString(c.getColumnIndexOrThrow("name")));
            mFields_Projects.add(c.getString(c.getColumnIndexOrThrow("name")));
        }
        // could extend this to other tables, but is there any point?
        // used below to check if a query is requesting non-existent fields but if so it
        // causes an unrecoverable error.
        // we would find these bugs during development; after that the queries would always
        // work unless the DB structure changed, which would require a rewrite anyway

        // can get list of tables by this query:
        // SELECT tbl_name FROM sqlite_master WHERE (type='table') AND (sql LIKE '%_id%') AND (tbl_name <> 'android_metadata');

        c.close();
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        Cursor cursor = null;
        if (LDebug.ON) Log.d(LOG_TAG, "in query; uri = " + uri);
        int uriType = sURIMatcher.match(uri);
        if (LDebug.ON) Log.d(LOG_TAG, "in query; uriType = " + uriType);
        if (uriType == RAW_SQL) { // use rawQuery
            // the full SQL statement is in 'selection' and any needed parameters in 'selectionArgs'
            cursor = database.getReadableDatabase().rawQuery(selection, selectionArgs);
        } else {
            if (tblHash.containsKey(uriType)) {
                queryBuilder.setTables(tblHash.get(uriType).tableName);
            } else if (tblHash.containsKey(uriType - 1)) {
                queryBuilder.setTables(tblHash.get(uriType - 1).tableName);
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
            } else {
                switch (uriType) {
                    // no custom Uri patterns yet
                    default:
                        throw new IllegalArgumentException("Unknown URI: " + uri);
                }
            }
            SQLiteDatabase db = database.getReadableDatabase();
            cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        }
        // assure potential listeners are notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        Uri uriToReturn = null;
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id;
        if (tblHash.containsKey(uriType)) {
            id = sqlDB.insert(tblHash.get(uriType).tableName, null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/" + tblHash.get(uriType).basePath + "/" + id);
        } else {
            switch (uriType) {
                // no custom Uri patterns to service yet
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return uriToReturn;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        String id;
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted;
        if (tblHash.containsKey(uriType)) {
            rowsDeleted = sqlDB.delete(tblHash.get(uriType).tableName, selection, selectionArgs);
        } else if (tblHash.containsKey(uriType - 1)) {
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete(tblHash.get(uriType - 1).tableName, "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete(tblHash.get(uriType - 1).tableName, "_id=" + id, selectionArgs);
            }
        } else {
            switch (uriType) {
                // no custom Uri patterns to service yet
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated;
        String id;
        if (tblHash.containsKey(uriType)) {
            rowsUpdated = sqlDB.updateWithOnConflict(tblHash.get(uriType).tableName,
                    values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
        } else if (tblHash.containsKey(uriType - 1)) {
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict(tblHash.get(uriType - 1).tableName,
                        values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict(tblHash.get(uriType - 1).tableName,
                        values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
        } else {
            switch (uriType) {
                case RAW_SQL:
                    // SQL to run is in 'selection', any parameters in 'selectionArgs'
                    sqlDB.execSQL(selection); // run SQL that creates no cursor and returns no results
                    // then use SQLite internal 'Changes' fn to retrieve number of rows changed
                    Cursor cur = sqlDB.rawQuery("SELECT Changes() AS C;", null);
                    cur.moveToFirst();
                    rowsUpdated = cur.getInt(0);
                    cur.close();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown URI:" + uri);
            }
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkFields(String tableName, String[] projection) {
        if (projection != null) {
            HashSet<String> requestedFields = new HashSet<String>(Arrays.asList(projection));
            // check if all columns requested are available
            switch (tableName) {
            case "Projects":
                // mFields_Projects is populated in onCreate
                if (!mFields_Projects.containsAll(requestedFields)) {
                    throw new IllegalArgumentException("Unknown fields in projection");
                }
                break;
            default:
                break; // for now, let all other cases go by
            }
        }
    }
}
