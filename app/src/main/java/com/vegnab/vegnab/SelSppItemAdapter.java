package com.vegnab.vegnab;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class SelSppItemAdapter extends ResourceCursorAdapter {

    private LayoutInflater mInflater;

    public SelSppItemAdapter(Context ctx, int layout, Cursor c, int flags) {
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
        TextView sppText = (TextView) v.findViewById(R.id.spp_descr_text);
        sppText.setText(c.getString(c.getColumnIndexOrThrow("MatchTxt")));
        int lstN = c.getInt(c.getColumnIndexOrThrow("SubListOrder"));
        int isPh = c.getInt(c.getColumnIndexOrThrow("IsPlaceholder"));
        int isId = c.getInt(c.getColumnIndexOrThrow("IsIdentified"));
        if (isPh == 1) { // a placeholder, assign a color for easier spotting
            if (lstN == 1) { // matched by Placeholder code
                if (isId == 0) { // not identified yet
                    v.setBackgroundColor(ctx.getResources().getColor(R.color.match_placeholder_code));
                } else { // has been identified
                    v.setBackgroundColor(ctx.getResources().getColor(R.color.match_placeholder_code_identified));
                }
            } else { // matched by Placeholder description
                if (isId == 0) { // not identified yet
                    v.setBackgroundColor(ctx.getResources().getColor(R.color.match_placeholder_text));
                } else { // has been identified
                    v.setBackgroundColor(ctx.getResources().getColor(R.color.match_placeholder_text_identified));
                }
            }
        } else { // not a placeholder, assign distinctive colors
            switch (lstN) {
                case 1: // a previously found code
                    v.setBackgroundColor(ctx.getResources().getColor(R.color.match_previously_found_species_code));
                    break;
                case 2: // a previously found code, best match
                    v.setBackgroundColor(ctx.getResources().getColor(R.color.match_previously_found_species_text));
                    break;
                case 3: // not previously found but a local species, match by code
                    v.setBackgroundColor(ctx.getResources().getColor(R.color.match_local_species_code));
                    break;
                case 4: // not previously found but a local species, match by text
                    v.setBackgroundColor(ctx.getResources().getColor(R.color.match_local_species_text));
                    break;
                case 5: // not a local species, match by code, low priority
                    v.setBackgroundColor(ctx.getResources().getColor(R.color.match_nonlocal_species_code));
                    break;
                case 6: // not a local species, match by text, lowest priority
                    v.setBackgroundColor(ctx.getResources().getColor(R.color.match_nonlocal_species_text));
                    break;

            }
        }
    }
}
