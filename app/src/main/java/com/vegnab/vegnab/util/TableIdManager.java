package com.vegnab.vegnab.util;

import com.vegnab.vegnab.VNApplication;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;

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

import static java.security.AccessController.getContext;

/**
 * Created by rshory on 12/6/2016.
 * For database tables that have only 2 fields, [_id] and a text field, this class allows
 *  doing something like n=getID("textString", "fieldName", "tableName")
 *  If the text string is in the table, returns the existing ID,
 *  If it is not yet in the table, adds a new record and returns that ID
 */

public class TableIdManager implements LoaderManager.LoaderCallbacks<Cursor> {
    public Activity mActivity;
    private long mLoaderID;
    private String mTextToFind;
    private String mFieldName;
    private String mTableName;
    private long mKey;
    private HashMap<Long, String> mExistingItems = new HashMap<Long, String>();

    TableIdManager(Activity act, String tableToUse) {
        // format of Tracker; does not work here
        //((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        mActivity = act;
        VNApplication app = (VNApplication) act.getApplication();
        mLoaderID = app.getUniqueLoaderId();
        mTableName = tableToUse;

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }
        Uri baseUri = ContentProvider_VegNab.SQL_URI;
        String select =  "SELECT * FROM " + mTableName + ";";
        CursorLoader cl = new CursorLoader(mActivity, baseUri, null, select, null, null);


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // will use following to get string field name
        // getColumnName(int columnIndex)
        // Returns the column name at the given zero-based column index.

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
