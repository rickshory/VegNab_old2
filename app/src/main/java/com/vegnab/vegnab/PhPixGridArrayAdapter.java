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
import com.vegnab.vegnab.util.VNGridImageItem;

import java.io.File;
import java.util.ArrayList;

public class PhPixGridArrayAdapter extends ArrayAdapter<VNGridImageItem> {
        //implements AdapterView.OnItemClickListener
    private static final String LOG_TAG = PhPixGridArrayAdapter.class.getSimpleName();
    Context ctx;
    private LayoutInflater mInflater;

    public PhPixGridArrayAdapter(Context ctx, int layout, ArrayList<VNGridImageItem> items) {
        super(ctx, layout, items);
        this.ctx = ctx;
        mInflater = (LayoutInflater) ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
	public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        String imagePath = null;
        VNGridImageItem gridItem = getItem(position);

        if (convertView == null) {
            view = mInflater.inflate(R.layout.grid_ph_pix, null);
        } else {
            // convertView was already laid out
            view = convertView;
            imagePath = (String) convertView.getTag();
        }
        // get the title
        TextView phGridCellText = (TextView) view.findViewById(R.id.phGridItemText);
        String note = gridItem.getTitle();
        if (note == null) {
            note = "(no note)";
        }
        phGridCellText.setText(note);

        // get the path and use it as this view's tag
        imagePath = gridItem.getPath();
        view.setTag(imagePath);

        // get the image thumbnail

        ImageView phGridCellImage = (ImageView) view.findViewById(R.id.phGridItemImage);
        File imgFile = new  File(imagePath);
        if (imgFile.exists()) {
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
            phGridCellImage.setImageBitmap(bitmap);

//            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//            phGridCellImage.setImageBitmap(myBitmap);
//            phGridCellImage.setAdjustViewBounds(true);
        } else {
            // set bitmap to a not-found icon
        }

        return view;

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
	}
    //

    public class GridImageItem {
        public Bitmap image;
        public String title;
        public String path;
    }

    /*
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
// above is from original example
    //Item click listener for pictures grid
    final AdapterView.OnItemClickListener mPixGrid_ItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Context c = getActivity();
//            Toast.makeText(getActivity(), "Item Clicked: " + position + ", id=" + id, Toast.LENGTH_SHORT).show();
            mPixMatchCursor.moveToPosition(position);
            String path = mPixMatchCursor.getString(mPixMatchCursor.getColumnIndexOrThrow("PhotoPath"));
//            Toast.makeText(getActivity(), "" + path, Toast.LENGTH_SHORT).show();
            Uri uri = getImageContentUri(c, path);
//            Toast.makeText(getActivity(), "" + uri.toString(), Toast.LENGTH_SHORT).show();
            if (uri == null) {
                Toast.makeText(c, c.getResources().getString(R.string.ph_pix_grid_pic_no_file),
                        Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/*");
                startActivity(intent);
            }
        }
    };
// below is from original example
        Intent trans=new Intent(MainActivity.this,Listed.class);
        trans.putExtra("first",p[arg2]);
        startActivity(trans);
    }
*/
}
