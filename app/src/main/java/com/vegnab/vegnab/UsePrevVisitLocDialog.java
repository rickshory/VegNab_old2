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
    final static String ARG_VISIT_ID_TO_UNHIDE = "visIdUnHide";
    ListView mHiddenVisitsList;
    public interface ConfirmUnHideVisitDialogListener {
        void onUnHideVisitConfirm(DialogFragment dialog);
    }
    ConfirmUnHideVisitDialogListener mConfirmUnHideVisitListener;

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
            mConfirmUnHideVisitListener = (ConfirmUnHideVisitDialogListener) getActivity();
           if (LDebug.ON) Log.d(LOG_TAG, "(ConfirmUnHideVisitDialogListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement ConfirmUnHideVisitDialogListener interface");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_unhide_visit, root);
        mHiddenVisitsList = (ListView) view.findViewById(R.id.list_hidden_visits);

        String[] fromColumns = {"VisitName", "VisitDate"}; // VisitName, VisitDate
        int[] toViews = {android.R.id.text1, android.R.id.text2};
        mListAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_2, null,
                fromColumns, toViews, 0);
        mHiddenVisitsList.setAdapter(mListAdapter);
        getLoaderManager().initLoader(Loaders.HIDDEN_VISITS, null, this);
        mHiddenVisitsList.setOnItemClickListener(new OnItemClickListener () {
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                    long id) {
                Cursor cr = ((SimpleCursorAdapter) mHiddenVisitsList.getAdapter()).getCursor();
                cr.moveToPosition(position);
               if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView setOnItemClickListener, list item clicked, id = " + id);
                Bundle args = getArguments();
                if (args != null) {
                    args.putLong(ARG_VISIT_ID_TO_UNHIDE, id);
                   if (LDebug.ON) Log.d(LOG_TAG, "put ARG_VISIT_ID_TO_UNHIDE =" + id);
                } else {
                   if (LDebug.ON) Log.d(LOG_TAG, "getArguments() returned null");
                }
               if (LDebug.ON) Log.d(LOG_TAG, "About to call onUnHideVisitConfirm=" + id);
                mConfirmUnHideVisitListener.onUnHideVisitConfirm(UsePrevVisitLocDialog.this);
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
