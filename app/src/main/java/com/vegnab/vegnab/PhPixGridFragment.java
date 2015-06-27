package com.vegnab.vegnab;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract;

public class PhPixGridFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = PhPixGridFragment.class.getSimpleName();
    final static String ARG_PLACEHOLDER_ID = "phId";
    long mPlaceholderId;
    private GridView mPhPixGridView;
    private PhPixGridAdapter mPhPixGridAdapter;

    public static PhPixGridFragment newInstance(Bundle args) {
        PhPixGridFragment f = new PhPixGridFragment();
        f.setArguments(args);
        return f;
    }

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

        View rootView = inflater.inflate(R.layout.fragment_ph_pix_grid, container, false);
        mPhPixGridView = (GridView) rootView.findViewById(R.id.phPixGridView);
        //mPhPixGridAdapter = new PhPixGridAdapter(this, R.layout.grid_item_layout, getData());
        mPhPixGridAdapter = new PhPixGridAdapter(getActivity(), R.layout.grid_ph_pix, null, 0);
        mPhPixGridView.setAdapter(mPhPixGridAdapter);

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
//            start loader to populate grid
//            mTxtNote.setText(args.getString(ARG_NOTE_ID));
        }
    }


    /* adapt following to work with cursor rather than array
        private ArrayList<ImageItem> getData() {
            final ArrayList<ImageItem> imageItems = new ArrayList<>();
            TypedArray imgs = getResources().obtainTypedArray(R.array.image_ids);
            for (int i = 0; i < imgs.length(); i++) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), imgs.getResourceId(i, -1));
                imageItems.add(new ImageItem(bitmap, "Image#" + i));
            }
            return imageItems;
    */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        switch (id) {
            case VNContract.Loaders.PLACEHOLDER_TO_EDIT:
                Uri onePlaceholderUri = ContentUris.withAppendedId(
                        Uri.withAppendedPath(
                                ContentProvider_VegNab.CONTENT_URI, "placeholders"), mPlaceholderId);
                cl = new CursorLoader(getActivity(), onePlaceholderUri,
                        null, select, null, null);
                break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        // there will be various loaders, switch them out here
//        mRowCt = c.getCount();
        switch (loader.getId()) {

            case VNContract.Loaders.PLACEHOLDER_TO_EDIT:
                Log.d(LOG_TAG, "onLoadFinished, PLACEHOLDER_TO_EDIT, records: " + c.getCount());
                if (c.moveToFirst()) {
    //				mPlaceholderId = c.getLong(c.getColumnIndexOrThrow("_id"));
//                    mViewPlaceholderCode.setText(c.getString(c.getColumnIndexOrThrow("PlaceHolderCode")));
//                    mViewPlaceholderDescription.setText(c.getString(c.getColumnIndexOrThrow("Description")));
//                    mViewPlaceholderHabitat.setText(c.getString(c.getColumnIndexOrThrow("Habitat")));
//                    mViewPlaceholderIdentifier.setText(c.getString(c.getColumnIndexOrThrow("LabelNum")));
    //				mPhVisitId = c.getLong(c.getColumnIndexOrThrow("VisitIdWhereFirstFound"));
    //				mPhProjId = c.getLong(c.getColumnIndexOrThrow("ProjID"));
    //				mPhNamerId = c.getLong(c.getColumnIndexOrThrow("NamerID"));
                } else { // no record to edit yet, set up new record
//                    mViewPlaceholderCode.setText(mPlaceholderCode);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed. Need to make sure it is no longer is use.
        switch (loader.getId()) {

        case VNContract.Loaders.PLACEHOLDER_TO_EDIT:
            Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDER_TO_EDIT.");
//			don't need to do anything here, no cursor adapter
            break;
        }
    }
}