package com.vegnab.vegnab;

import java.util.List;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VegNabDbHelper;
import com.vegnab.vegnab.database.VNContract.Prefs;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class DelNamerDialog extends DialogFragment implements android.view.View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    VegNabDbHelper mDbHelper;
    ListView mValidNamerList;
    SimpleCursorAdapter mListAdapter; // to link the list's data

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_del_namer, root);
        mValidNamerList = (ListView) view.findViewById(R.id.list_del_namers_valid);

        String[] fromColumns = {"NamerName"};
        int[] toViews = {android.R.id.text1};
        mListAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null,
                fromColumns, toViews, 0);
        mValidNamerList.setAdapter(mListAdapter);
        // Loader Id for the list of Namers that are valid to delete
        getLoaderManager().initLoader(Loaders.VALID_DEL_NAMERS, null, this);
        mValidNamerList.setOnItemClickListener(new OnItemClickListener () {
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                    long id) {
                Cursor cr = ((SimpleCursorAdapter) mValidNamerList.getAdapter()).getCursor();
                cr.moveToPosition(position);
                String strNamer = cr.getString(cr.getColumnIndexOrThrow("NamerName"));
//				Toast.makeText(getActivity(), 
//						"strNamer: " + strNamer, 
//						Toast.LENGTH_LONG).show();
                FragmentManager fm = getActivity().getSupportFragmentManager();
                ConfirmDelNamerDialog  confDelNamerDlg = ConfirmDelNamerDialog.newInstance(id, strNamer);
                confDelNamerDlg.show(fm, "frg_conf_del_namer");
                dismiss();
            }
        });

        getDialog().setTitle(R.string.del_namer_confirm);
        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        switch (id) {
        case Loaders.VALID_DEL_NAMERS:
            baseUri = ContentProvider_VegNab.SQL_URI;
            // complex use-once query:
            // disallow the current default Namer
            // and any Namers that have done Visits (even 'deleted' Visits) or have Placeholders
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            long defaultNamerId = sharedPref.getLong(Prefs.DEFAULT_NAMER_ID, 0);
            select = "SELECT _id, NamerName FROM Namers " +
            "WHERE ((_id != ?) " +
            "AND (_id NOT IN (SELECT NamerID FROM Visits)) " +
            "AND (_id NOT IN (SELECT NamerID FROM Placeholders)));";
            String[] qryArgs = {"" + defaultNamerId};
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, qryArgs, null);
            break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor finishedCursor) {
        switch (loader.getId()) {
        case Loaders.VALID_DEL_NAMERS:
            mListAdapter.swapCursor(finishedCursor);
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
        case Loaders.VALID_DEL_NAMERS:
            mListAdapter.swapCursor(null);
            break;
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

    }
}
