package com.vegnab.vegnab.util;

import com.vegnab.vegnab.VNApplication;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract;

import java.util.HashMap;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import static java.security.AccessController.getContext;

/**
 * Created by rshory on 12/6/2016.
 * For database tables that have only 2 fields, [_id] and a text field, this class allows
 *  doing something like n=getID("textString", "fieldName", "tableName")
 *  If the text string is in the table, returns the existing ID,
 *  If it is not yet in the table, adds a new record and returns that ID
 */

public class TableIdManager implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = TableIdManager.class.getSimpleName();
    public Activity mActivity;
    private long mLoaderID;
    private String mTextToFind;
    private String mFieldName;
    private String mTableName;
    private long mKey;
    private HashMap<Long, String> mExistingItems = new HashMap<Long, String>();
    private LoaderManager mLoaderManager;

    TableIdManager(Activity act, String tableToUse) {
        // format of Tracker; does not work here
        //((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        mActivity = act;
        VNApplication app = (VNApplication) act.getApplication();
        mLoaderID = app.getUniqueLoaderId();
        mTableName = tableToUse;
        // fire off this database request
        mLoaderManager = (LoaderManager) mActivity.getLoaderManager().initLoader(mLoaderID, null,
                (android.app.LoaderManager.LoaderCallbacks<Cursor>) this);

    }

    /*logic to check presence
    * if (mDupCodes.containsValue(mIDConfidence + mStrVegCode)) {
                if (LDebug.ON) Log.d(LOG_TAG, "saveVegItemRecord, Conf&Code already exist: " + mIDConfidence + mStrVegCode);
                return 0;*/

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri = ContentProvider_VegNab.SQL_URI;
        //        String select =  "SELECT * FROM " + mTableName + ";";
        String select = "SELECT * FROM ?;";
        String[] params = new String[]{"" + mTableName};
        //        CursorLoader cl = new CursorLoader(mActivity, baseUri, null, select, null, null);
        // will table name work correctly as a parameter?
        CursorLoader cl = new CursorLoader(mActivity, baseUri, null, select, params, null);
        return cl;
}

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        if (VNContract.LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, records: " + c.getCount());
        mExistingItems.clear();
        // don't even need to know the string column name, but if we want it:
        // getColumnName(int columnIndex)
        // returns the column name at the given zero-based column index
        while (c.moveToNext()) {
            mExistingItems.put(c.getLong(c.getColumnIndexOrThrow("_id")),
                    c.getString(1)); // the string is always in the '2nd' (0-indexed) column
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // nothing to do here since no adapter
    }
}
