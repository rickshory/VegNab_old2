package com.vegnab.vegnab;

import android.util.Log;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;
import com.vegnab.vegnab.database.VNContract.LDebug;

import java.util.List;

/**
 * Created by rshory on 12/1/2015.
 */
public class VNDriveEventService extends DriveEventService {
    private static final String LOG_TAG = VNDriveEventService.class.getSimpleName();

      @Override
    public void onCompletion(CompletionEvent event) {
        super.onCompletion(event);
        DriveId driveId = event.getDriveId();
        if (LDebug.ON) Log.d(LOG_TAG, "XXX Resource ID: " + driveId.getResourceId()); // returns the final part of the URL
        if (LDebug.ON) Log.d(LOG_TAG, "XXX Tracking Tags: " + event.getTrackingTags()); // first one is tracking tag sent
        if (LDebug.ON) Log.d(LOG_TAG, "XXX Account Name: " + event.getAccountName()); // returns null
        if (LDebug.ON) Log.d(LOG_TAG, "XXX DriveID: " + event.getDriveId()); // returns cryptic string something like expanded form of Drive ID sent
        if (LDebug.ON) Log.d(LOG_TAG, "XXX toString: " + event.toString()); // returns DriveID, status, and trackingTag
        if (LDebug.ON) Log.d(LOG_TAG, "XXX describe: " + event.describeContents()); // returns zero

          List tags = event.getTrackingTags();
          String trackTag = tags.get(0).toString();
          if (LDebug.ON) Log.d(LOG_TAG, "XXX first tracking tag: " + trackTag); //

        switch (event.getStatus()) {
            case CompletionEvent.STATUS_CONFLICT:
                if (LDebug.ON) Log.d(LOG_TAG, "XXX STATUS_CONFLICT");
                event.dismiss();
                break;
            case CompletionEvent.STATUS_FAILURE:
                if (LDebug.ON) Log.d(LOG_TAG, "XXX STATUS_FAILURE");
                event.dismiss();
                break;
            case CompletionEvent.STATUS_SUCCESS:
                if (LDebug.ON) Log.d(LOG_TAG, "XXX STATUS_SUCCESS ");
                event.dismiss();
                break;
        }
    }
}
