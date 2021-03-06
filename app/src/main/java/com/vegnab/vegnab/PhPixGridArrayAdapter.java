package com.vegnab.vegnab;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vegnab.vegnab.database.VNContract.LDebug;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PhPixGridArrayAdapter extends ArrayAdapter<String> {
        //implements AdapterView.OnItemClickListener
        // View lookup cache
        private static class ViewHolder {
            ImageView pic;
            TextView note;
        }
    private static final String LOG_TAG = PhPixGridArrayAdapter.class.getSimpleName();
    Context ctx;
    private LayoutInflater mInflater;


    public PhPixGridArrayAdapter(Context ctx, ArrayList<String> items) {
        super(ctx, R.layout.grid_ph_pix, items);
        this.ctx = ctx;
        mInflater = (LayoutInflater) ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

//	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        String imagePath = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache to be stored in tag

        View view;
        if (convertView == null) {
            // If there's no view to re-use, inflate a brand new view for row
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.grid_ph_pix, parent, false);
            viewHolder.pic = (ImageView) convertView.findViewById(R.id.phGridItemImage);
            viewHolder.note = (TextView) convertView.findViewById(R.id.phGridItemText);
            // Cache the viewHolder object inside the fresh view
            convertView.setTag(viewHolder);
        } else {
            // View is being recycled, retrieve the viewHolder object from tag
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data from the data object via the viewHolder object
        // into the template view.
        File imgFile = new  File(imagePath);
        String note;
        if (imgFile.exists()) {
            // for now, use the date as the note
            note = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(
                    new Date(imgFile.lastModified())
            );
            // get the image thumbnail
            // There isn't enough memory to open up more than a few camera photos
            // so pre-scale the target bitmap into which the file is decoded
            // Get the size of the ImageView
            int targetW = viewHolder.pic.getWidth();
            int targetH = viewHolder.pic.getHeight();
            if (LDebug.ON) Log.d(LOG_TAG, "grid cell image Ht " + targetH + ", Wd " + targetW);
            // for testing, manually override
            targetW = 100;
            targetH = 100;
            // Get the size of the image
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, bmOptions);
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
            if (LDebug.ON) Log.d(LOG_TAG, "Scale Factor " + scaleFactor + ", About to decode: " + imagePath);
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, bmOptions);
            if (LDebug.ON) Log.d(LOG_TAG, "bitmap Ht " + bitmap.getHeight() + ", width " + bitmap.getWidth());

            // associate the Bitmap to the ImageView
            viewHolder.pic.setImageBitmap(bitmap);

//            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//            phGridCellImage.setImageBitmap(myBitmap);
//            phGridCellImage.setAdjustViewBounds(true);
        } else {
            note = "(no note)";
            // set bitmap to a not-found icon
        }

        // viewHolder.pic.setImageBitmap(bitmap); // redundant
        viewHolder.note.setText(note);
        // Return the completed view to render on screen
        return convertView;
	}
}
