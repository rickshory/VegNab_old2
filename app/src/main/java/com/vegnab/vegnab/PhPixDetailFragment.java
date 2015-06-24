package com.vegnab.vegnab;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vegnab.vegnab.database.VNContract.Tags;

public class PhPixDetailFragment extends Fragment {

    final static String ARG_PIX_ID = "pixId";
    final static String ARG_NOTE_ID = "noteId";

    TextView mTxtNote;
    ImageView mImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // if the activity was re-created (e.g. from a screen rotate)
        // restore the previous parameters remembered by onSaveInstanceState()
        // This is mostly needed in fixed-pane layouts
/*		if (savedInstanceState != null) {
            // restore parameters
            }
        } else {
            // default parameters
        } */
        // inflate the layout for this fragment

        View rootView = inflater.inflate(R.layout.fragment_ph_pix_detail, container, false);
        mTxtNote = (TextView) rootView.findViewById(R.id.phDetailNote);
        mImageView = (ImageView) rootView.findViewById(R.id.phDetailImage);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        // during startup, check if arguments are passed to the fragment
        // this is where to do this because the layout has been applied
        // to the fragment
        Bundle args = getArguments();
        if (args != null) {
            mTxtNote.setText(args.getString(ARG_NOTE_ID));
//            mImageView.setImageBitmap(args.getParcelable(ARG_PIX_ID));
        }
    }
}
