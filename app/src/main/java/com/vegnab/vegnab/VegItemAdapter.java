package com.vegnab.vegnab;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class VegItemAdapter extends ResourceCursorAdapter {

    private LayoutInflater mInflater;

    public VegItemAdapter(Context ctx, int layout, Cursor c, int flags) {
        super(ctx, layout, c, flags);
        mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

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
