package com.vegnab.vegnab;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
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
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.Prefs;
import com.vegnab.vegnab.database.VegNabDbHelper;

public class UnHideVisitDialog extends DialogFragment implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = UnHideVisitDialog.class.getSimpleName();
    ListView mHiddenVisitsList;
    SimpleCursorAdapter mListAdapter; // to link the list's data
    ContentValues mValues = new ContentValues();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_unhide_visit, root);
        mHiddenVisitsList = (ListView) view.findViewById(R.id.list_hidden_visits);

        String[] fromColumns = {"VisitName"}; // VisitName, VisitDate
        int[] toViews = {android.R.id.text1};
        mListAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null,
                fromColumns, toViews, 0);
        mHiddenVisitsList.setAdapter(mListAdapter);
        getLoaderManager().initLoader(Loaders.HIDDEN_VISITS, null, this);
        mHiddenVisitsList.setOnItemClickListener(new OnItemClickListener () {
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                    long id) {
                Cursor cr = ((SimpleCursorAdapter) mHiddenVisitsList.getAdapter()).getCursor();
                cr.moveToPosition(position);
                String visNm = cr.getString(cr.getColumnIndexOrThrow("VisitName"));
//				Toast.makeText(getActivity(), 
//						"VisitName: " + visNm, 
//						Toast.LENGTH_LONG).show();
                // if we needed to confirm, could do something like this:
//                FragmentManager fm = getActivity().getSupportFragmentManager();
//                ConfirmDelProjDialog  confDelProjDlg = ConfirmDelProjDialog.newInstance(id, visNm);
//                confDelProjDlg.show(fm, "frg_conf_del_proj");
                mValues.clear();
                mValues.put("ShowOnMobile", 1);
                Uri uri = ContentUris.withAppendedId(
                        Uri.withAppendedPath(
                                ContentProvider_VegNab.CONTENT_URI, "visits"), id);
                Log.d(LOG_TAG, "In UnHideVisitDialog, URI: " + uri.toString());
                ContentResolver rs = getActivity().getContentResolver();
                int numUpdated = rs.update(uri, mValues, null, null);
                Log.d(LOG_TAG, "In UnHideVisitDialog, numUpdated: " + numUpdated);
                dismiss();
            }
        });

        getDialog().setTitle(R.string.action_unhide_visit);
        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        switch (id) {
        case Loaders.HIDDEN_VISITS:
            baseUri = ContentProvider_VegNab.SQL_URI;
            select = "SELECT _id, VisitName, VisitDate FROM Visits "
                    + "WHERE ShowOnMobile = 0 AND IsDeleted = 0 "
                    + "ORDER BY VisitDate DESC;";
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, null, null);
            break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor finishedCursor) {
        switch (loader.getId()) {
        case Loaders.HIDDEN_VISITS:
            mListAdapter.swapCursor(finishedCursor);
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
        case Loaders.HIDDEN_VISITS:
            mListAdapter.swapCursor(null);
            break;
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

    }
}
