package com.vegnab.vegnab.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.vegnab.vegnab.database.VNContract;

import java.io.File;
import java.util.Date;

/**
 * Created by rshory on 5/20/2017.
 */

// class for use in grid view
public class VNGridImageItem {
    private static final String LOG_TAG = VNGridImageItem.class.getSimpleName();
    private Bitmap image;
    private String title;
    private String path;
    private boolean isComplete;

    public VNGridImageItem(String path) {
        this.image = null;
        this.title = "";
        this.path = path;
        this.isComplete = false;
        tryToComplete();
    }

    public VNGridImageItem() {
        this.image = null;
        this.title = null;
        this.path = null;
        this.isComplete = false;
    }

    public VNGridImageItem(Bitmap image, String title, String path) {
        this.image = image;
        this.title = title;
        this.path = path;
        this.isComplete = true;
    }

    public Bitmap getImage() {
        return image;
    }
    public void setImage(Bitmap image) {
        this.image = image;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
        tryToComplete();
    }

    private void tryToComplete() {
        File imgFile = new  File(this.path);
        if (imgFile.exists()) {
            Date lastModified = new Date(imgFile.lastModified());
            this.title = lastModified.toString();
            // There isn't enough memory to open up more than a few camera photos
            // so pre-scale the target bitmap into which the file is decoded
            // Get the size of the ImageView
//            int targetW = phGridCellImage.getWidth();
//            int targetH = phGridCellImage.getHeight();
//            if (VNContract.LDebug.ON) Log.d(LOG_TAG, "grid cell image Ht " + targetH + ", Wd " + targetW);
            // for testing, manually override
            int targetW = 100;
            int targetH = 100;
            // Get the size of the image
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(this.path, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;
            // determine the aspect ratio
            int scaleFactor = 1;
            if ((targetW > 0) || (targetH > 0)) {
                scaleFactor = Math.min(photoW/targetW, photoH/targetH);
            }
            // Set bitmap options to scale the image decode target
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            // bmOptions.inPurgeable = true;
            // Decode the JPEG file into a Bitmap
            if (VNContract.LDebug.ON) Log.d(LOG_TAG, "Scale Factor " + scaleFactor + ", About to decode: " + this.path);
            Bitmap bitmap = BitmapFactory.decodeFile(this.path, bmOptions);
            if (VNContract.LDebug.ON) Log.d(LOG_TAG, "bitmap Ht " + bitmap.getHeight() + ", width " + bitmap.getWidth());

            // set the image
            this.image = bitmap;
            this.isComplete = true;

//            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//            phGridCellImage.setImageBitmap(myBitmap);
//            phGridCellImage.setAdjustViewBounds(true);
        } else {
            this.isComplete = false;
            // maybe set bitmap to a not-found icon
        }

    }
}
