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

public class DelProjectDialog extends DialogFragment implements android.view.View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    VegNabDbHelper mDbHelper;
    ListView mValidProjList;
    SimpleCursorAdapter mListAdapter; // to link the list's data

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_del_project, root);
        mValidProjList = (ListView) view.findViewById(R.id.list_del_projects_valid);

        String[] fromColumns = {"ProjCode"};
        int[] toViews = {android.R.id.text1};
        mListAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null,
                fromColumns, toViews, 0);
        mValidProjList.setAdapter(mListAdapter);
        // Loader Id for the list of Projects that are valid to delete
        getLoaderManager().initLoader(Loaders.VALID_DEL_PROJECTS, null, this);
        mValidProjList.setOnItemClickListener(new OnItemClickListener () {
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                    long id) {
                Cursor cr = ((SimpleCursorAdapter) mValidProjList.getAdapter()).getCursor();
                cr.moveToPosition(position);
                String projCd = cr.getString(cr.getColumnIndexOrThrow("ProjCode"));
//				Toast.makeText(getActivity(), 
//						"projCd: " + projCd, 
//						Toast.LENGTH_LONG).show();
                FragmentManager fm = getActivity().getSupportFragmentManager();
                ConfirmDelProjDialog  confDelProjDlg = ConfirmDelProjDialog.newInstance(id, projCd);
                confDelProjDlg.show(fm, "frg_conf_del_proj");
                dismiss();
            }
        });

        getDialog().setTitle(R.string.action_delete_proj);
        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        switch (id) {
        case Loaders.VALID_DEL_PROJECTS:
            baseUri = ContentProvider_VegNab.SQL_URI;
            // complex use-once query: only use Project not marked deleted, and
            // disallow the first Project,
            // the current Default project, and any Projects that have Visits
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            long defaultProjId = sharedPref.getLong(Prefs.DEFAULT_PROJECT_ID, 1);
            select = "SELECT _id, ProjCode FROM Projects " +
            "WHERE ((IsDeleted = 0) AND (_id != 1) AND (_id != ?) " +
            "AND (_id NOT IN (SELECT ProjId FROM Visits WHERE IsDeleted = 0)));";
            String[] qryArgs = {"" + defaultProjId};
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, qryArgs, null);
            break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor finishedCursor) {
        switch (loader.getId()) {
        case Loaders.VALID_DEL_PROJECTS:
            mListAdapter.swapCursor(finishedCursor);
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
        case Loaders.VALID_DEL_PROJECTS:
            mListAdapter.swapCursor(null);
            break;
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

    }
}
