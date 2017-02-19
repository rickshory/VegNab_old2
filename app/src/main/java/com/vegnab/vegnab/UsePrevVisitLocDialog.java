package com.vegnab.vegnab;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.LDebug;
import com.vegnab.vegnab.database.VNContract.Loaders;

public class UsePrevVisitLocDialog extends DialogFragment implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = UsePrevVisitLocDialog.class.getSimpleName();
    final static String ARG_VISIT_USE_LOC = "visIdUse";
    final static String ARG_LATITIDE_STRING = "prevVisLatStr";
    final static String ARG_LONGITUDE_STRING = "prevVisLonStr";
    final static String ARG_ACCURACY_STRING = "prevVisAccStr";

    ListView mPrevVisitLocsList;
    public interface UsePrevVisitLocDialogListener {
        void onUsePrevVisitLoc(DialogFragment dialog, Bundle args);
    }
    UsePrevVisitLocDialogListener mUsePrevVisitLocListener;

    SimpleCursorAdapter mListAdapter; // to link the list's data

    // don't receive anything yet, maybe pass cursor?
    static UsePrevVisitLocDialog newInstance(Bundle args) {
        UsePrevVisitLocDialog f = new UsePrevVisitLocDialog();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mUsePrevVisitLocListener = (UsePrevVisitLocDialogListener) getActivity();
           if (LDebug.ON) Log.d(LOG_TAG, "(UsePrevVisitLocDialogListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement UsePrevVisitLocDialogListener interface");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_visit_locs, root);
        mPrevVisitLocsList = (ListView) view.findViewById(R.id.list_prev_visit_locs);

        String[] fromColumns = {"LatLon", "VisDescr"};
        int[] toViews = {android.R.id.text1, android.R.id.text2};
        mListAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_2, null,
                fromColumns, toViews, 0);
        mPrevVisitLocsList.setAdapter(mListAdapter);
        getLoaderManager().initLoader(Loaders.PREVIOUS_VISIT_LOCATIONS, null, this);
        mPrevVisitLocsList.setOnItemClickListener(new OnItemClickListener () {
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                    long id) {
                Cursor cr = ((SimpleCursorAdapter) mPrevVisitLocsList.getAdapter()).getCursor();
                cr.moveToPosition(position);
               if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView setOnItemClickListener, list item clicked, id = " + id);
                try { // can fail with null pointer exception if fragment is gone
                    Bundle args = new Bundle();
                    // send args generalized for VisitHeaderFragment
                    args.putDouble(VisitHeaderFragment.ARG_LOC_LATITUDE,
                            cr.getDouble(cr.getColumnIndexOrThrow("Latitude")));
                    args.putDouble(VisitHeaderFragment.ARG_LOC_LONGITUDE,
                            cr.getDouble(cr.getColumnIndexOrThrow("Longitude")));
                    args.putFloat(VisitHeaderFragment.ARG_LOC_ACCURACY, (float)0);
                    args.putString(VisitHeaderFragment.ARG_LOC_ACC_SOURCE, "User supplied");
                    args.putString(VisitHeaderFragment.ARG_LOC_PROVIDER, "Manual entry");

                    // We send the dialog only to dismiss it in the activity. Can we dismiss it here?
                    if (LDebug.ON) Log.d(LOG_TAG, "About to call onUsePrevVisitLoc");
                    mUsePrevVisitLocListener.onUsePrevVisitLoc(UsePrevVisitLocDialog.this, args);
//                    dismiss();
                } catch (Exception e) {
                    // ignore; if fails, will not update with previous plot location
                }
            }
        });

        getDialog().setTitle(R.string.action_use_prev_visit_loc);
        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        switch (id) {
        case Loaders.PREVIOUS_VISIT_LOCATIONS:
            baseUri = ContentProvider_VegNab.SQL_URI;
            // need an ID field in the query, even if we don't use it, to make the list work right
            select = "SELECT Locations._id, "
                    + "(Locations.Latitude || ', ' || Locations.Longitude) AS LatLon, "
                    + "(Visits.VisitDate || ', ' || Visits.VisitName ||  "
                    + "(IFNULL((', ' || Visits.VisitNotes), ''))) AS VisDescr, "
                    + "Locations.Latitude, Locations.Longitude, Locations.Accuracy "
                    + "FROM Visits LEFT JOIN Locations ON Visits.RefLocID = Locations._id "
                    + "WHERE Visits.ShowOnMobile = 1 AND Visits.IsDeleted = 0 "
                    + "AND Locations.Latitude LIKE '%' AND Locations.Longitude LIKE '%' "
                    + "AND Visits._id != ? "
                    + "ORDER BY Visits.VisitDate DESC;";
            // get the current visit ID, which was  passed in a bundle, to exclude it from the list of choices
            String[] params = new String[] {"" + getArguments().getLong(VisitHeaderFragment.ARG_VISIT_ID) };
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, params, null);
            break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor finishedCursor) {
        switch (loader.getId()) {
        case Loaders.PREVIOUS_VISIT_LOCATIONS:
            mListAdapter.swapCursor(finishedCursor);
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
        case Loaders.PREVIOUS_VISIT_LOCATIONS:
            mListAdapter.swapCursor(null);
            break;
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

    }
}
