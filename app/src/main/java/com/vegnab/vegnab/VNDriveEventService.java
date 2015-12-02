package com.vegnab.vegnab;

import android.util.Log;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;
import com.vegnab.vegnab.database.VNContract.LDebug;

/**
 * Created by rshory on 12/1/2015.
 */
public class VNDriveEventService extends DriveEventService {
    private static final String LOG_TAG = VNDriveEventService.class.getSimpleName();

      @Override
  public void onCompletion(CompletionEvent event) {
          if (LDebug.ON) Log.d(LOG_TAG, "XXX in onCompletion before super");
          if (LDebug.ON) Log.d(LOG_TAG, "XXX Drive Action completed with status: " + event.getStatus());
    super.onCompletion(event);
          if (LDebug.ON) Log.d(LOG_TAG, "XXX in onCompletion after super");
          if (LDebug.ON) Log.d(LOG_TAG, "XXX Drive Action completed with status: " + event.getStatus());
    DriveId driveId = event.getDriveId();
    if (LDebug.ON) Log.d(LOG_TAG, "XXX onComplete: " + driveId.getResourceId());
    switch (event.getStatus()) {
      case CompletionEvent.STATUS_CONFLICT:  if (LDebug.ON) Log.d(LOG_TAG, "XXX STATUS_CONFLICT"); event.dismiss(); break;
      case CompletionEvent.STATUS_FAILURE:   if (LDebug.ON) Log.d(LOG_TAG, "XXX STATUS_FAILURE");  event.dismiss(); break;
      case CompletionEvent.STATUS_SUCCESS:   if (LDebug.ON) Log.d(LOG_TAG, "XXX STATUS_SUCCESS "); event.dismiss(); break;
    }
  }
}
