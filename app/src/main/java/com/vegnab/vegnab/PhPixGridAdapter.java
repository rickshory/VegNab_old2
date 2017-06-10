package com.vegnab.vegnab;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.vegnab.vegnab.database.VNContract.LDebug;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PhPixGridAdapter extends ResourceCursorAdapter {
    private static final String LOG_TAG = PhPixGridAdapter.class.getSimpleName();

    private LayoutInflater mInflater;

    public PhPixGridAdapter(Context ctx, int layout, Cursor c, int flags) {
        super(ctx, layout, c, flags);
        mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // following commented-out techniques may give smoother scrolling, but code at bottom is OK for now
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
    // break the following apart to use with cursor instead of array
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
        // available fields: PhotoPath, PlaceHolderID, PhotoTimeStamp, PhotoNotes, PhotoURL
        TextView phGridCellText = (TextView) v.findViewById(R.id.phGridItemText);
/*
        String note = c.getString(c.getColumnIndexOrThrow("PhotoNotes"));
        if (note == null) {
            note = "(no note)";
        }
        phGridCellText.setText(note);
*/
        ImageView phGridCellImage = (ImageView) v.findViewById(R.id.phGridItemImage);
        String path = c.getString(c.getColumnIndexOrThrow("PhotoPath"));
        File imgFile = new  File(path);
        if (imgFile.exists()) {
            // for now, use the date as the note
            phGridCellText.setText(new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(
                    new Date(imgFile.lastModified())));
            // There isn't enough memory to open up more than a few camera photos
            // so pre-scale the target bitmap into which the file is decoded
            // Get the size of the ImageView
            int targetW = phGridCellImage.getWidth();
            int targetH = phGridCellImage.getHeight();
           if (LDebug.ON) Log.d(LOG_TAG, "grid cell image Ht " + targetH + ", Wd " + targetW);
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
           if (LDebug.ON) Log.d(LOG_TAG, "Scale Factor " + scaleFactor + ", About to decode: " + path);
            Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);
           if (LDebug.ON) Log.d(LOG_TAG, "bitmap Ht " + bitmap.getHeight() + ", width " + bitmap.getWidth());

            // associate the Bitmap to the ImageView
            phGridCellImage.setImageBitmap(bitmap);

//            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//            phGridCellImage.setImageBitmap(myBitmap);
//            phGridCellImage.setAdjustViewBounds(true);
        } else {
            // set bitmap to a not-found icon
        }
    }
}
