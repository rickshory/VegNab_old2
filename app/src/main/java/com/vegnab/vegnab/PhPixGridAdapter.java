package com.vegnab.vegnab;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.widget.ResourceCursorAdapter;
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

	private Context context;
	private int layoutResourceId;
	private ArrayList data = new ArrayList();

	public GridViewAdapter(Context context, int layoutResourceId, ArrayList data) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		ViewHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new ViewHolder();
			holder.imageTitle = (TextView) row.findViewById(R.id.phGridItemText);
			holder.image = (ImageView) row.findViewById(R.id.phGridItemImage);
			row.setTag(holder);
		} else {
			holder = (ViewHolder) row.getTag();
		}

		VNContract.VNGridImageItem item = data.get(position);
		holder.imageTitle.setText(item.getTitle());
		holder.image.setImageBitmap(item.getImage());
		return row;
	}

	static class ViewHolder {
		TextView imageTitle;
		ImageView image;
	}
}
    @Override
    public void bindView(View v, Context ctx, Cursor c) {
// example of formatting by position
//		if(c.getPosition()%2==1) {
//			view.setBackgroundColor(ctx.getResources().getColor(R.color.background_odd));
//		} else {
//			view.setBackgroundColor(ctx.getResources().getColor(R.color.background_even));
//		}

/*CREATE TABLE IF NOT EXISTS "PlaceHolderPix" (
"_id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,
"PlaceHolderID" INTEGER NOT NULL,
"PhotoPath" VARCHAR(255),
"PhotoTimeStamp" TIMESTAMP NOT NULL DEFAULT (DATETIME('now')),
"PhotoNotes" VARCHAR(255),
"PhotoURL" VARCHAR(255),*/
        String note = c.getString(c.getColumnIndexOrThrow("PhotoNotes"));
        if (note == null) {
            note = "(no note)";
        }
        String path = c.getString(c.getColumnIndexOrThrow("PhotoPath"));
        File imgFile = new  File(path);
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            mImgOriginal.setImageBitmap(myBitmap);
            mImgOriginal.setAdjustViewBounds(true);
        }






    }
}
