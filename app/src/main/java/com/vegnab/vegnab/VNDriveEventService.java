package com.vegnab.vegnab;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.LDebug;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by rshory on 12/1/2015.
 */
public class VNDriveEventService extends DriveEventService {
    private static final String LOG_TAG = VNDriveEventService.class.getSimpleName();

      @Override
    public void onCompletion(CompletionEvent event) {
        super.onCompletion(event);
          int cmpStatus = 7; // default 'Error'
        DriveId driveId = event.getDriveId();
        if (LDebug.ON) Log.d(LOG_TAG, "XXX Resource ID: " + driveId.getResourceId()); // returns the final part of the URL
        if (LDebug.ON) Log.d(LOG_TAG, "XXX Tracking Tags: " + event.getTrackingTags()); // first one is tracking tag sent
        if (LDebug.ON) Log.d(LOG_TAG, "XXX Account Name: " + event.getAccountName()); // returns null
        if (LDebug.ON) Log.d(LOG_TAG, "XXX DriveID: " + event.getDriveId()); // returns cryptic string something like expanded form of Drive ID sent
        if (LDebug.ON) Log.d(LOG_TAG, "XXX toString: " + event.toString()); // returns DriveID, status, and trackingTag
        if (LDebug.ON) Log.d(LOG_TAG, "XXX describe: " + event.describeContents()); // returns zero

          String resID = driveId.getResourceId();
          List tags = event.getTrackingTags();
          String trackTag;
          try {
              trackTag = tags.get(0).toString();
          } catch (Exception e) {
              trackTag = "Error: " + event.toString();
          }

          if (LDebug.ON) Log.d(LOG_TAG, "XXX first tracking tag: " + trackTag); //
          cmpStatus = event.getStatus() + 1; // status code to store in DB is 1-based

          // following is redundant and can be removed
        switch (event.getStatus()) {
            case CompletionEvent.STATUS_CONFLICT:
                if (LDebug.ON) Log.d(LOG_TAG, "XXX STATUS_CONFLICT");
                cmpStatus = 3;
                event.dismiss();
                break;
            case CompletionEvent.STATUS_FAILURE:
                if (LDebug.ON) Log.d(LOG_TAG, "XXX STATUS_FAILURE");
                cmpStatus = 2;
                event.dismiss();
                break;
            case CompletionEvent.STATUS_SUCCESS:
                if (LDebug.ON) Log.d(LOG_TAG, "XXX STATUS_SUCCESS ");
                cmpStatus = 1;
                event.dismiss();
                break;
        }

        int numUpdated = 0;
        Uri uri, docsUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "docs");

        ContentResolver rs = getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put("DocStatusID", cmpStatus); // ' try to update status ...
        contentValues.put("DocResourceID", resID);
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        contentValues.put("TimePosted", dateTimeFormat.format(new Date()));
        String whereClause = "DocName = ?"; // ... of record where filename was sent as the tracking tag
        String[] whereArgs;
        whereArgs = new String[] {trackTag};
        numUpdated = rs.update(docsUri, contentValues, whereClause, whereArgs);
        if (LDebug.ON) Log.d(LOG_TAG, "Updated doc record in onCompletion; numUpdated: " + numUpdated);
        if (numUpdated == 0) { // did not find a record to update
            // create a new record
            // following putNull's may not be necessary, but flag that this does not match a found record
            contentValues.putNull("DocTypeID");
            contentValues.putNull("DocSourceTypeID");
            contentValues.putNull("DocSourceRecID");
            contentValues.put("DocName", resID); // same as field 'DocResourceID'
            uri = rs.insert(docsUri, contentValues);
        }
    }
}
