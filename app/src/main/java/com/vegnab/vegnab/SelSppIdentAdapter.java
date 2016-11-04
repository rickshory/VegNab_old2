package com.vegnab.vegnab;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class SelSppIdentAdapter extends ResourceCursorAdapter {

    private LayoutInflater mInflater;

    public SelSppIdentAdapter(Context ctx, int layout, Cursor c, int flags) {
        super(ctx, layout, c, flags);
        mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View v, Context ctx, Cursor c) {
// example of formatting by position
//		if(c.getPosition()%2==1) {
//			view.setBackgroundColor(ContextCompat.getColor(ctx, R.color.background_odd));
//		} else {
//			view.setBackgroundColor(ContextCompat.getColor(ctx, R.color.background_even));
//		}
        TextView sppText = (TextView) v.findViewById(R.id.spp_descr_text);
        sppText.setText(c.getString(c.getColumnIndexOrThrow("MatchTxt")));
    }
}
