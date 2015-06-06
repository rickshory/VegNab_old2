package com.vegnab.vegnab;

import java.util.HashMap;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.Prefs;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EditNamerDialog extends DialogFragment implements android.view.View.OnClickListener,
        android.view.View.OnFocusChangeListener, LoaderManager.LoaderCallbacks<Cursor>
        {
    private static final String LOG_TAG = EditNamerDialog.class.getSimpleName();
    public interface EditNamerDialogListener {
        public void onEditNamerComplete(DialogFragment dialog);
    }
    EditNamerDialogListener mEditNamerListener;
    long mNamerRecId = 0; // zero default means new or not specified yet
    Uri mUri, mNamersUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "namers");
    ContentValues mValues = new ContentValues();
    HashMap<Long, String> mExistingNamers = new HashMap<Long, String>();
    private EditText mEditNamerName;
    private TextView mTxtNamerMsg;
    String mStringNamer;

    static EditNamerDialog newInstance(long namerId) {
        EditNamerDialog f = new EditNamerDialog();
        // supply namerId as an argument
        Bundle args = new Bundle();
        args.putLong("namerId", namerId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mEditNamerListener = (EditNamerDialogListener) getActivity();
            Log.d(LOG_TAG, "(EditNamerDialogListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement EditNamerDialogListener interface");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_namer, root);
        mTxtNamerMsg = (TextView) view.findViewById(R.id.lbl_namer);
        mEditNamerName = (EditText) view.findViewById(R.id.txt_edit_namer);
        // attempt to automatically show soft keyboard
        mEditNamerName.requestFocus();
        getDialog().getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mEditNamerName.setOnFocusChangeListener(this);

//		if (mNamerRecId == 0) { // new record
//			getDialog().setTitle(R.string.add_namer_title);
//		} else { // existing record being edited
//			getDialog().setTitle(R.string.edit_namer_title_edit);
//		}
        return view;
    }

    @Override
    public void onClick(View v) {
        // don't need onClick here
    }

    @Override
    public void onStart() {
        super.onStart();
        // during startup, check if arguments are passed to the fragment
        // this is where to do this because the layout has been applied
        // to the fragment
        Bundle args = getArguments();

        if (args != null) {
            mNamerRecId = args.getLong("namerId");
            // request existing Namers ASAP, this doesn't use the UI
            getLoaderManager().initLoader(Loaders.EXISTING_NAMERS, null, this);
            getLoaderManager().initLoader(Loaders.NAMER_TO_EDIT, null, this);
            // will insert values into screen when cursor is finished
        }
        if (mNamerRecId == 0) { // new record
            getDialog().setTitle(R.string.add_namer_title);
            mTxtNamerMsg.setText(R.string.add_namer_header);
        } else { // existing record being edited
            getDialog().setTitle(R.string.edit_namer_title_edit);
            mTxtNamerMsg.setText(R.string.edit_namer_label_namername);
        }

    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(!hasFocus) { // something lost focus
            mValues.clear();
            switch (v.getId()) {
            case R.id.txt_edit_namer:
                mValues.put("NamerName", mEditNamerName.getText().toString().trim());
                break;

            default: // save everything
                mValues.put("NamerName", mEditNamerName.getText().toString().trim());

                }
            Log.d(LOG_TAG, "Saving record in onFocusChange; mValues: " + mValues.toString().trim());
            int numUpdated = saveNamerRecord();
            }
        }


    @Override
    public void onCancel (DialogInterface dialog) {
        // update the project record in the database, if everything valid
        mValues.clear();
        mValues.put("NamerName", mEditNamerName.getText().toString().trim());
        Log.d(LOG_TAG, "Saving record in onCancel; mValues: " + mValues.toString());
        int numUpdated = saveNamerRecord();
        if (numUpdated > 0) {
            mEditNamerListener.onEditNamerComplete(EditNamerDialog.this);
        }
    }


    private int saveNamerRecord () {
        Context c = getActivity();
        // test field for validity
        String namerString = mValues.getAsString("NamerName");

        if (namerString.length() == 0) {
            Toast.makeText(this.getActivity(),
                    c.getResources().getString(R.string.add_namer_missing),
                    Toast.LENGTH_LONG).show();
            return 0;
        }
        if (!(namerString.length() >= 2)) {
            Toast.makeText(this.getActivity(),
                    c.getResources().getString(R.string.err_need_2_chars),
                    Toast.LENGTH_LONG).show();
            return 0;
        }
        if (mExistingNamers.containsValue(namerString)) {
            Toast.makeText(this.getActivity(),
                    c.getResources().getString(R.string.add_namer_duplicate),
                    Toast.LENGTH_LONG).show();
            return 0;
        }
        ContentResolver rs = getActivity().getContentResolver();
        if (mNamerRecId == -1) {
            Log.d(LOG_TAG, "entered saveNamerRecord with (mNamerRecId == -1); canceled");
            return 0;
        }
        if (mNamerRecId == 0) { // new record
            mUri = rs.insert(mNamersUri, mValues);
            Log.d(LOG_TAG, "new record in saveNamerRecord; returned URI: " + mUri.toString());
            long newRecId = Long.parseLong(mUri.getLastPathSegment());
            if (newRecId < 1) { // returns -1 on error, e.g. if not valid to save because of missing required field
                Log.d(LOG_TAG, "new record in saveNamerRecord has Id == " + newRecId + "); canceled");
                return 0;
            }
            mNamerRecId = newRecId;
            getLoaderManager().restartLoader(Loaders.EXISTING_NAMERS, null, this);
            mUri = ContentUris.withAppendedId(mNamersUri, mNamerRecId);
            Log.d(LOG_TAG, "new record in saveNamerRecord; URI re-parsed: " + mUri.toString());
            // set default Namer
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putLong(Prefs.DEFAULT_NAMER_ID, mNamerRecId);
            prefEditor.commit();
            return 1;
        } else {
            mUri = ContentUris.withAppendedId(mNamersUri, mNamerRecId);
            Log.d(LOG_TAG, "about to update record in saveNamerRecord; mValues: " + mValues.toString() + "; URI: " + mUri.toString());
            int numUpdated = rs.update(mUri, mValues, null, null);
            Log.d(LOG_TAG, "Saved record in saveNamerRecord; numUpdated: " + numUpdated);
            return numUpdated;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        switch (id) {
        case Loaders.EXISTING_NAMERS:
            // get the existing Namers, other than the current one, to disallow duplicates
            Uri allNamersUri = Uri.withAppendedPath(
                    ContentProvider_VegNab.CONTENT_URI, "namers");
            String[] projection = {"_id", "NamerName"};
            select = "(_id <> " + mNamerRecId + ")";
            cl = new CursorLoader(getActivity(), allNamersUri,
                    projection, select, null, null);
            break;
        case Loaders.NAMER_TO_EDIT:
            // First, create the base URI
            // could test here, based on e.g. filters
//			mNamersUri = ContentProvider_VegNab.CONTENT_URI; // get the whole list
            Uri oneNamerUri = ContentUris.withAppendedId(
                            Uri.withAppendedPath(
                            ContentProvider_VegNab.CONTENT_URI, "namers"), mNamerRecId);
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the dataset being displayed
            // Could build a WHERE clause such as
            // String select = "(Default = true)";
            cl = new CursorLoader(getActivity(), oneNamerUri,
                    null, select, null, null);
            break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        switch (loader.getId()) {
        case Loaders.EXISTING_NAMERS:
            mExistingNamers.clear();
            while (c.moveToNext()) {
                Log.d(LOG_TAG, "onLoadFinished, add to HashMap: " + c.getString(c.getColumnIndexOrThrow("NamerName")));
                mExistingNamers.put(c.getLong(c.getColumnIndexOrThrow("_id")),
                        c.getString(c.getColumnIndexOrThrow("NamerName")));
            }
            Log.d(LOG_TAG, "onLoadFinished, number of items in mExistingNamers: " + mExistingNamers.size());
            Log.d(LOG_TAG, "onLoadFinished, items in mExistingNamers: " + mExistingNamers.toString());

            break;
        case Loaders.NAMER_TO_EDIT:
            Log.d(LOG_TAG, "onLoadFinished, records: " + c.getCount());
            if (c.moveToFirst()) {
                mEditNamerName.setText(c.getString(c.getColumnIndexOrThrow("NamerName")));
            }
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
        case Loaders.NAMER_TO_EDIT:
            // maybe nothing to do here since no adapter
            break;
        }
    }
}
