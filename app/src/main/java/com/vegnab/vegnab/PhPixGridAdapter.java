package com.vegnab.vegnab;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class PhPixGridAdapter extends ResourceCursorAdapter {

    private LayoutInflater mInflater;

    public PhPixGridAdapter(Context ctx, int layout, Cursor c, int flags) {
        super(ctx, layout, c, flags);
        mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
/*	private Context context;
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
			holder.imageTitle = (TextView) row.findViewById(R.id.text);
			holder.image = (ImageView) row.findViewById(R.id.image);
			row.setTag(holder);
		} else {
			holder = (ViewHolder) row.getTag();
		}

		ImageItem item = data.get(position);
		holder.imageTitle.setText(item.getTitle());
		holder.image.setImageBitmap(item.getImage());
		return row;
	}

	static class ViewHolder {
		TextView imageTitle;
		ImageView image;
	}
}*/
    @Override
    public void bindView(View v, Context ctx, Cursor c) {
// example of formatting by position
//		if(c.getPosition()%2==1) {
//			view.setBackgroundColor(ctx.getResources().getColor(R.color.background_odd));
//		} else {
//			view.setBackgroundColor(ctx.getResources().getColor(R.color.background_even));
//		}
        int cfCode = c.getInt(c.getColumnIndexOrThrow("IdLevelID")); // not used yet
        String sppLine = ""; // not used yet
        TextView vegText = (TextView) v.findViewById(R.id.veg_descr_text);
        vegText.setText(c.getString(c.getColumnIndexOrThrow("SppLine")));

        TextView vegHt = (TextView) v.findViewById(R.id.veg_height_text);
        String ht = c.getString(c.getColumnIndexOrThrow("Height"));
        if (ht == null) {
            vegHt.setVisibility(View.GONE);
        } else {
            vegHt.setText(ht + "cm");
        }

        TextView vegCov = (TextView) v.findViewById(R.id.veg_cover_text);
        String cv = c.getString(c.getColumnIndexOrThrow("Cover"));
        if (cv == null) {
            vegCov.setVisibility(View.GONE);
        } else {
            vegCov.setText(cv + "%");
        }

        CheckBox vegPresence = (CheckBox) v.findViewById(R.id.veg_presence_ck);
        // explicitly test Presence for null
        if (c.isNull(c.getColumnIndexOrThrow("Presence"))) {
            vegPresence.setVisibility(View.GONE);
        } else {
            int presence = c.getInt(c.getColumnIndexOrThrow("Presence"));
            if (presence != 0) {
                vegPresence.setChecked(true);
            } else {
                vegPresence.setChecked(false);
            }
        }
    }
}
