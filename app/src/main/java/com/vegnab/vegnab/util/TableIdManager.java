package com.vegnab.vegnab.util;

import com.vegnab.vegnab.VNApplication;

import java.util.HashMap;
import android.app.Activity;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import static java.security.AccessController.getContext;

/**
 * Created by rshory on 12/6/2016.
 * For database tables that have only 2 fields, [_id] and a text field, this class allows
 *  doing something like n=getID("textString", "fieldName", "tableName")
 *  If the text string is in the table, returns the existing ID,
 *  If it is not yet in the table, adds a new record and returns that ID
 */

public class TableIdManager {
    public Activity activity;

    private long mLoaderID;
    private String mTextToFind;
    private String mFieldName;
    private String mTableName;
    private long mKey;
    private HashMap<Long, String> mExistingItems = new HashMap<Long, String>();

    TableIdManager(Activity act, String tableToUse) {
        this.activity = act;
        //((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        mLoaderID = this.activity.getApplication().getUniqueLoaderId();
        mLoaderID = VNApplication.getUniqueLoaderId();
        ((VNApplication) act.getApplication())act.getTracker(VNApplication.TrackerName.APP_TRACKER);
        ((VNApplication) getContext().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        mTableName = tableToUse;

    }
}
