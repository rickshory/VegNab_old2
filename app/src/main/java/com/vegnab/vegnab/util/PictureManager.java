package com.vegnab.vegnab.util;

import android.os.Environment;
import android.util.Log;

import com.vegnab.vegnab.R;
import com.vegnab.vegnab.database.VNContract;

import java.io.File;

/**
 * Created by rshory on 5/4/2017.
 * shared methods for picture locations and names
 */

public class PictureManager {

    private static final String LOG_TAG = PictureManager.class.getSimpleName();

    private File getStorageDir(String albumName) {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            //min SDK version is Honeycomb 3.0, API level 11, so can depend on following
            storageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), albumName);
//            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());
            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        if (VNContract.LDebug.ON) Log.d(LOG_TAG, "Could not create folder: " + albumName);
                        return null;
                    }
                }
            }
        } else {
            if (VNContract.LDebug.ON) Log.d(LOG_TAG, "External storage is not mounted READ/WRITE.");
        }
        return storageDir;
    }


}
