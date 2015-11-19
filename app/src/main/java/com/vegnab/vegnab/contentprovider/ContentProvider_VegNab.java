package com.vegnab.vegnab.contentprovider;

import java.util.Arrays;
import java.util.HashSet;

import com.vegnab.vegnab.BuildConfig;
import com.vegnab.vegnab.database.VegNabDbHelper;
import com.vegnab.vegnab.database.VNContract.LDebug;

import android.content.ContentProvider;
//import android.content.ContentResolver;
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

    // used for the UriMatcher
    private static final int RAW_SQL = 1;
    private static final int PROJECTS = 10;
    private static final int PROJECT_ID = 20;
    private static final int VISITS = 30;
    private static final int VISIT_ID = 40;
    private static final int LOCATIONS = 50;
    private static final int LOCATION_ID = 60;
    private static final int NAMERS = 70;
    private static final int NAMER_ID = 80;
    private static final int PLOTTYPES = 90;
    private static final int PLOTTYPE_ID = 100;
    private static final int SUBPLOTTYPES = 110;
    private static final int SUBPLOTTYPE_ID = 120;
    private static final int VEGITEMS = 130;
    private static final int VEGITEM_ID = 140;
    private static final int PLACEHOLDERS = 150;
    private static final int PLACEHOLDER_ID = 160;
    private static final int PLACEHOLDER_PIX = 170;
    private static final int PLACEHOLDER_PIX_ID = 180;
    private static final int IDNAMERS = 190;
    private static final int IDNAMER_ID = 200;
    private static final int IDREFS = 210;
    private static final int IDREF_ID = 220;
    private static final int IDMETHODS = 230;
    private static final int IDMETHOD_ID = 240;
    private static final int IDLEVELS = 250;
    private static final int IDLEVEL_ID = 260;
    private static final int SPECIES = 270;
    private static final int SPECIES_ID = 280;
    private static final int DOCS = 290;
    private static final int DOC_ID = 300;
    private static final int DOCTYPES = 310;
    private static final int DOCTYPE_ID = 320;
    private static final int DOCSOURCES = 330;
    private static final int DOCSOURCE_ID = 340;

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
        sURIMatcher.addURI(AUTHORITY, "sql", RAW_SQL);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/projects", PROJECTS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/projects/#", PROJECT_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/visits", VISITS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/visits/#", VISIT_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/locations", LOCATIONS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/locations/#", LOCATION_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/namers", NAMERS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/namers/#", NAMER_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/plottypes", PLOTTYPES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/plottypes/#", PLOTTYPE_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/subplottypes", SUBPLOTTYPES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/subplottypes/#", SUBPLOTTYPE_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/vegitems", VEGITEMS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/vegitems/#", VEGITEM_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/placeholders", PLACEHOLDERS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/placeholders/#", PLACEHOLDER_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/placeholderpix", PLACEHOLDER_PIX);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/placeholderpix/#", PLACEHOLDER_PIX_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/idnamers", IDNAMERS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/idnamers/#", IDNAMER_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/idrefs", IDREFS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/idrefs/#", IDREF_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/idmethods", IDMETHODS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/idmethods/#", IDMETHOD_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/idlevels", IDLEVELS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/idlevels/#", IDLEVEL_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/species", SPECIES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/species/#", SPECIES_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/docs", DOCS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/docs/#", DOC_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/doctypes", DOCTYPES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/doctypes/#", DOCTYPE_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/docsources", DOCSOURCES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/docsources/#", DOCSOURCE_ID);

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
        int uriType = sURIMatcher.match(uri);
        if (uriType == RAW_SQL) { // use rawQuery
            // the full SQL statement is in 'selection' and any needed parameters in 'selectionArgs'
            cursor = database.getReadableDatabase().rawQuery(selection, selectionArgs);
        } else {

            switch (uriType) {
            case PROJECTS:
                // fix up the following fn to work with all tables
                // check if the caller has requested a field that does not exist
                checkFields("Projects", projection);
                // assign the table
                queryBuilder.setTables("Projects");
                break;
            case PROJECT_ID:
                checkFields("Projects", projection);
                queryBuilder.setTables("Projects");
                // add the ID to the original query
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
                break;

            case VISIT_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
                // note, no break, so drops through
            case VISITS:
                queryBuilder.setTables("Visits");
                break;

            case LOCATION_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
                // note, no break, so drops through
            case LOCATIONS:
                queryBuilder.setTables("Locations");
                break;

            case NAMER_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
               if (LDebug.ON) Log.d(LOG_TAG, "NAMER_ID appendWhere");
                // note, no break, so drops through
            case NAMERS:
                queryBuilder.setTables("Namers");
               if (LDebug.ON) Log.d(LOG_TAG, "NAMERS setTables");
                break;

            case PLOTTYPE_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
               if (LDebug.ON) Log.d(LOG_TAG, "PLOTTYPE_ID appendWhere");
                // note, no break, so drops through
            case PLOTTYPES:
                queryBuilder.setTables("PlotTypes");
               if (LDebug.ON) Log.d(LOG_TAG, "PLOTTYPES setTables");
                break;

            case SUBPLOTTYPE_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
               if (LDebug.ON) Log.d(LOG_TAG, "SUBPLOTTYPE_ID appendWhere");
                // note, no break, so drops through
            case SUBPLOTTYPES:
                queryBuilder.setTables("SubplotTypes");
               if (LDebug.ON) Log.d(LOG_TAG, "SUBPLOTTYPES setTables");
                break;

            case VEGITEM_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
               if (LDebug.ON) Log.d(LOG_TAG, "VEGITEM_ID appendWhere");
                // note, no break, so drops through
            case VEGITEMS:
                queryBuilder.setTables("VegItems");
               if (LDebug.ON) Log.d(LOG_TAG, "VEGITEMS setTables");
                break;

            case PLACEHOLDER_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
               if (LDebug.ON) Log.d(LOG_TAG, "PLACEHOLDER_ID appendWhere");
                // note, no break, so drops through
            case PLACEHOLDERS:
                queryBuilder.setTables("Placeholders");
               if (LDebug.ON) Log.d(LOG_TAG, "PLACEHOLDERS setTables");
                break;

            case PLACEHOLDER_PIX_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
               if (LDebug.ON) Log.d(LOG_TAG, "PLACEHOLDER_PIX_ID appendWhere");
                // note, no break, so drops through
            case PLACEHOLDER_PIX:
                queryBuilder.setTables("Placeholders");
               if (LDebug.ON) Log.d(LOG_TAG, "PLACEHOLDER_PIX setTables");
                break;
            case IDNAMER_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
               if (LDebug.ON) Log.d(LOG_TAG, "IDNAMER_ID appendWhere");
                // note, no break, so drops through
            case IDNAMERS:
                queryBuilder.setTables("IdNamers");
               if (LDebug.ON) Log.d(LOG_TAG, "IDNAMERS setTables");
                break;
            case IDREF_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
               if (LDebug.ON) Log.d(LOG_TAG, "IDREF_ID appendWhere");
                // note, no break, so drops through
            case IDREFS:
                queryBuilder.setTables("IdRefs");
               if (LDebug.ON) Log.d(LOG_TAG, "IDREFS setTables");
                break;
            case IDMETHOD_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
               if (LDebug.ON) Log.d(LOG_TAG, "IDMETHOD_ID appendWhere");
                // note, no break, so drops through
            case IDMETHODS:
                queryBuilder.setTables("IdMethods");
               if (LDebug.ON) Log.d(LOG_TAG, "IDMETHODS setTables");
                break;
            case IDLEVEL_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
               if (LDebug.ON) Log.d(LOG_TAG, "IDLEVEL_ID appendWhere");
                // note, no break, so drops through
            case IDLEVELS:
                queryBuilder.setTables("IdLevels");
               if (LDebug.ON) Log.d(LOG_TAG, "IDLEVELS setTables");
                break;
            case SPECIES_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
               if (LDebug.ON) Log.d(LOG_TAG, "SPECIES_ID appendWhere");
                // note, no break, so drops through
            case SPECIES:
                queryBuilder.setTables("SpeciesFound");
               if (LDebug.ON) Log.d(LOG_TAG, "SPECIES setTables");
                break;
            case DOC_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
                if (LDebug.ON) Log.d(LOG_TAG, "DOC_ID appendWhere");
                // note, no break, so drops through
            case DOCS:
                queryBuilder.setTables("DocsCreated");
                if (LDebug.ON) Log.d(LOG_TAG, "DOCS setTables");
                break;
            case DOCTYPE_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
                if (LDebug.ON) Log.d(LOG_TAG, "DOCTYPE_ID appendWhere");
                // note, no break, so drops through
            case DOCTYPES:
                queryBuilder.setTables("DocsCreatedTypes");
                if (LDebug.ON) Log.d(LOG_TAG, "DOCTYPES setTables");
                break;
            case DOCSOURCE_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
                if (LDebug.ON) Log.d(LOG_TAG, "DOCSOURCE_ID appendWhere");
                // note, no break, so drops through
            case DOCSOURCES:
                queryBuilder.setTables("DocsSourcesTypes");
                if (LDebug.ON) Log.d(LOG_TAG, "DOCSOURCES setTables");
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
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
        Uri uriToReturn;
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id;
        switch (uriType) {
        case PROJECTS:
            id = sqlDB.insert("Projects", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/projects/" + id);
            break;
        case VISITS:
            id = sqlDB.insert("Visits", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/visits/" + id);
            break;
        case LOCATIONS:
            id = sqlDB.insert("Locations", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/locations/" + id);
            break;
        case NAMERS:
            id = sqlDB.insert("Namers", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/namers/" + id);
            break;
        case PLOTTYPES:
            id = sqlDB.insert("PlotTypes", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/plottypes/" + id);
            break;
        case SUBPLOTTYPES:
            id = sqlDB.insert("SubplotTypes", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/subplottypes/" + id);
            break;
        case VEGITEMS:
            id = sqlDB.insert("VegItems", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/vegitems/" + id);
            break;
        case PLACEHOLDERS:
            id = sqlDB.insert("Placeholders", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/placeholders/" + id);
            break;
        case PLACEHOLDER_PIX:
            id = sqlDB.insert("PlaceHolderPix", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/placeholderpix/" + id);
            break;
        case IDNAMERS:
            id = sqlDB.insert("IdNamers", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/idnamers/" + id);
            break;
        case IDREFS:
            id = sqlDB.insert("IdRefs", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/idrefs/" + id);
            break;
        case IDMETHODS:
            id = sqlDB.insert("IdMethods", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/idmethods/" + id);
            break;
        case IDLEVELS:
            id = sqlDB.insert("IdLevels", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/idlevels/" + id);
            break;
        case SPECIES:
            id = sqlDB.insert("RegionalSpeciesList", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/species/" + id);
            break;
        case DOCS:
            id = sqlDB.insert("DocsCreated", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/docs/" + id);
            break;
        case DOCTYPES:
            id = sqlDB.insert("DocsCreatedTypes", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/doctypes/" + id);
            break;
        case DOCSOURCES:
            id = sqlDB.insert("DocsSourcesTypes", null, values);
            uriToReturn = Uri.parse(BASE_PATH + "/docsources/" + id);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
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
        switch (uriType) {
        case PROJECTS:
            rowsDeleted = sqlDB.delete("Projects", selection, selectionArgs);
            break;
        case PROJECT_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("Projects", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("Projects", "_id=" + id, selectionArgs);
            }
            break;

        case VISITS:
            rowsDeleted = sqlDB.delete("Visits", selection, selectionArgs);
            break;
        case VISIT_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("Visits", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("Visits", "_id=" + id, selectionArgs);
            }
            break;

        case LOCATIONS:
            rowsDeleted = sqlDB.delete("Locations", selection, selectionArgs);
            break;
        case LOCATION_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("Locations", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("Locations", "_id=" + id, selectionArgs);
            }
            break;

        case NAMERS:
            rowsDeleted = sqlDB.delete("Namers", selection, selectionArgs);
            break;
        case NAMER_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("Namers", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("Namers", "_id=" + id, selectionArgs);
            }
            break;

        case PLOTTYPES:
            rowsDeleted = sqlDB.delete("PlotTypes", selection, selectionArgs);
            break;
        case PLOTTYPE_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("PlotTypes", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("PlotTypes", "_id=" + id, selectionArgs);
            }
            break;

        case SUBPLOTTYPES:
            rowsDeleted = sqlDB.delete("SubplotTypes", selection, selectionArgs);
            break;
        case SUBPLOTTYPE_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("SubplotTypes", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("SubplotTypes", "_id=" + id, selectionArgs);
            }
            break;

        case VEGITEMS:
            rowsDeleted = sqlDB.delete("VegItems", selection, selectionArgs);
            break;
        case VEGITEM_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("VegItems", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("VegItems", "_id=" + id, selectionArgs);
            }
            break;

        case PLACEHOLDERS:
            rowsDeleted = sqlDB.delete("Placeholders", selection, selectionArgs);
            break;
        case PLACEHOLDER_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("Placeholders", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("Placeholders", "_id=" + id, selectionArgs);
            }
            break;

        case PLACEHOLDER_PIX:
            rowsDeleted = sqlDB.delete("PlaceHolderPix", selection, selectionArgs);
            break;
        case PLACEHOLDER_PIX_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("PlaceHolderPix", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("PlaceHolderPix", "_id=" + id, selectionArgs);
            }
            break;

            case IDNAMERS:
                rowsDeleted = sqlDB.delete("IdNamers", selection, selectionArgs);
                break;
            case IDNAMER_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete("IdNamers", "_id=" + id, null);
                } else {
                    rowsDeleted = sqlDB.delete("IdNamers", "_id=" + id, selectionArgs);
                }
                break;

            case IDREFS:
                rowsDeleted = sqlDB.delete("IdRefs", selection, selectionArgs);
                break;
            case IDREF_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete("IdRefs", "_id=" + id, null);
                } else {
                    rowsDeleted = sqlDB.delete("IdRefs", "_id=" + id, selectionArgs);
                }
                break;

            case IDMETHODS:
                rowsDeleted = sqlDB.delete("IdMethods", selection, selectionArgs);
                break;
            case IDMETHOD_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete("IdMethods", "_id=" + id, null);
                } else {
                    rowsDeleted = sqlDB.delete("IdMethods", "_id=" + id, selectionArgs);
                }
                break;

            case IDLEVELS:
            rowsDeleted = sqlDB.delete("IdLevels", selection, selectionArgs);
            break;
        case IDLEVEL_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("IdLevels", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("IdLevels", "_id=" + id, selectionArgs);
            }
            break;

        case SPECIES:
            rowsDeleted = sqlDB.delete("RegionalSpeciesList", selection, selectionArgs);
            break;
        case SPECIES_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("RegionalSpeciesList", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("RegionalSpeciesList", "_id=" + id, selectionArgs);
            }
            break;

        case DOCS:
            rowsDeleted = sqlDB.delete("DocsCreated", selection, selectionArgs);
            break;
        case DOC_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("DocsCreated", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("DocsCreated", "_id=" + id, selectionArgs);
            }
            break;

        case DOCTYPES:
            rowsDeleted = sqlDB.delete("DocsCreatedTypes", selection, selectionArgs);
            break;
        case DOCTYPE_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("DocsCreatedTypes", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("DocsCreatedTypes", "_id=" + id, selectionArgs);
            }
            break;

        case DOCSOURCES:
            rowsDeleted = sqlDB.delete("DocsSourceTypes", selection, selectionArgs);
            break;
        case DOCSOURCE_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete("DocsSourceTypes", "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete("DocsSourceTypes", "_id=" + id, selectionArgs);
            }
            break;

        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
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

        case PROJECTS:
            rowsUpdated = sqlDB.updateWithOnConflict("Projects", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case PROJECT_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("Projects", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("Projects", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case VISITS:
            rowsUpdated = sqlDB.updateWithOnConflict("Visits", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case VISIT_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("Visits", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("Visits", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case LOCATIONS:
            rowsUpdated = sqlDB.updateWithOnConflict("Locations", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case LOCATION_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("Locations", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("Locations", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case NAMERS:
            rowsUpdated = sqlDB.updateWithOnConflict("Namers", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case NAMER_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("Namers", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("Namers", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case PLOTTYPES:
            rowsUpdated = sqlDB.updateWithOnConflict("PlotTypes", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case PLOTTYPE_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("PlotTypes", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("PlotTypes", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case SUBPLOTTYPES:
            rowsUpdated = sqlDB.updateWithOnConflict("SubplotTypes", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case SUBPLOTTYPE_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("SubplotTypes", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("SubplotTypes", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case VEGITEMS:
            rowsUpdated = sqlDB.updateWithOnConflict("VegItems", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case VEGITEM_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("VegItems", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("VegItems", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case PLACEHOLDERS:
            rowsUpdated = sqlDB.updateWithOnConflict("Placeholders", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case PLACEHOLDER_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("Placeholders", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("Placeholders", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case PLACEHOLDER_PIX:
            rowsUpdated = sqlDB.updateWithOnConflict("PlaceHolderPix", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case PLACEHOLDER_PIX_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("PlaceHolderPix", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("PlaceHolderPix", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case IDNAMERS:
            rowsUpdated = sqlDB.updateWithOnConflict("IdNamers", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case IDNAMER_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("IdNamers", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("IdNamers", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case IDREFS:
            rowsUpdated = sqlDB.updateWithOnConflict("IdRefs", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case IDREF_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("IdRefs", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("IdRefs", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case IDMETHODS:
            rowsUpdated = sqlDB.updateWithOnConflict("IdMethods", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case IDMETHOD_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("IdMethods", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("IdMethods", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case IDLEVELS:
        rowsUpdated = sqlDB.updateWithOnConflict("IdLevels", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
        break;
        case IDLEVEL_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("IdLevels", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("IdLevels", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case SPECIES:
            rowsUpdated = sqlDB.updateWithOnConflict("RegionalSpeciesList", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case SPECIES_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("RegionalSpeciesList", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("RegionalSpeciesList", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case DOCS:
            rowsUpdated = sqlDB.updateWithOnConflict("DocsCreated", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case DOC_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("DocsCreated", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("DocsCreated", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case DOCTYPES:
            rowsUpdated = sqlDB.updateWithOnConflict("DocsCreatedTypes", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case DOCTYPE_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("DocsCreatedTypes", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("DocsCreatedTypes", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        case DOCSOURCES:
            rowsUpdated = sqlDB.updateWithOnConflict("DocsSourceTypes", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        case DOCSOURCE_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.updateWithOnConflict("DocsSourceTypes", values, "_id=" + id, null, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                rowsUpdated = sqlDB.updateWithOnConflict("DocsSourceTypes", values, "_id=" + id, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            break;

        default:
            throw new IllegalArgumentException("Unknown URI:" + uri);
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
