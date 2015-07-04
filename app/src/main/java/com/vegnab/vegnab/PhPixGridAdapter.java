package com.vegnab.vegnab;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.vegnab.vegnab.database.VNContract;

import java.io.File;
import java.util.ArrayList;

public class PhPixGridAdapter extends ResourceCursorAdapter {
    private static final String LOG_TAG = PhPixGridAdapter.class.getSimpleName();

    private LayoutInflater mInflater;

    public PhPixGridAdapter(Context ctx, int layout, Cursor c, int flags) {
        super(ctx, layout, c, flags);
        mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

/*    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ImageView photo = (ImageView) view;
        String url_medium = cursor.getString(cursor.getColumnIndex(KarmaDbAdapter.PHOTO_URL_MEDIUM));
        ImageCache.download(url_medium, photo);

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ImageView v = new ImageView(context);
        v.setLayoutParams(new GridView.LayoutParams(100, 100));
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bindView(v, context, cursor);
        return v;
    }
}*/

//	private Context context;
//	private int layoutResourceId;
//	private ArrayList data = new ArrayList();
//
//	public GridViewAdapter(Context context, int layoutResourceId, ArrayList data) {
//		super(context, layoutResourceId, data);
//		this.layoutResourceId = layoutResourceId;
//		this.context = context;
//		this.data = data;
//	}
//
    // break the following apart to use with cursor
//	@Override
//	public View getView(int position, View convertView, ViewGroup parent) {
//		View row = convertView;
//		ViewHolder holder = null;
//
    // adapt the following for newView
//		if (row == null) {
//			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
//			row = inflater.inflate(layoutResourceId, parent, false);
//			holder = new ViewHolder();
//			holder.imageTitle = (TextView) row.findViewById(R.id.phGridItemText);
//			holder.image = (ImageView) row.findViewById(R.id.phGridItemImage);
//			row.setTag(holder);
//		} else {
    // adapt the following for bindView
//			holder = (ViewHolder) row.getTag();
//		}
//
//		VNContract.VNGridImageItem item = data.get(position);
//		holder.imageTitle.setText(item.getTitle());
//		holder.image.setImageBitmap(item.getImage());
//		return row;
//	}
//
//	static class ViewHolder {
//		TextView imageTitle;
//		ImageView image;
//	}

    @Override
    public void bindView(View v, Context ctx, Cursor c) {
        TextView phGridCellText = (TextView) v.findViewById(R.id.phGridItemText);
        String note = c.getString(c.getColumnIndexOrThrow("PhotoNotes"));
        if (note == null) {
            note = "(no note)";
        }
        phGridCellText.setText(note);

/*CREATE TABLE IF NOT EXISTS "PlaceHolderPix" (
"_id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,
"PlaceHolderID" INTEGER NOT NULL,
"PhotoPath" VARCHAR(255),
"PhotoTimeStamp" TIMESTAMP NOT NULL DEFAULT (DATETIME('now')),
"PhotoNotes" VARCHAR(255),
"PhotoURL" VARCHAR(255),*/
        ImageView phGridCellImage = (ImageView) v.findViewById(R.id.phGridItemImage);
        String path = c.getString(c.getColumnIndexOrThrow("PhotoPath"));
        File imgFile = new  File(path);
        if (imgFile.exists()) {
            // There isn't enough memory to open up more than a few camera photos
            // so pre-scale the target bitmap into which the file is decoded
            // Get the size of the ImageView
            int targetW = phGridCellImage.getWidth();
            int targetH = phGridCellImage.getHeight();
            Log.d(LOG_TAG, "grid cell image Ht " + targetH + ", Wd " + targetW);
            // for testing, manually override
            targetW = 100;
            targetH = 100;
            // Get the size of the image
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bmOptions);
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
            Log.d(LOG_TAG, "Scale Factor " + scaleFactor + ", About to decode: " + path);
            Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);
            Log.d(LOG_TAG, "bitmap Ht " + bitmap.getHeight() + ", width " + bitmap.getWidth());

            // associate the Bitmap to the ImageView
            phGridCellImage.setImageBitmap(bitmap);

/*         mTestImageView.setVisibility(View.VISIBLE);*/
//            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//            phGridCellImage.setImageBitmap(myBitmap);
//            phGridCellImage.setAdjustViewBounds(true);
        }
    }
}
