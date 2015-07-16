package com.vegnab.vegnab;

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
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.Prefs;

import java.util.HashMap;

public class ConfigurableEditDialog extends DialogFragment implements
        View.OnFocusChangeListener, LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = ConfigurableEditDialog.class.getSimpleName();
    public interface ConfigurableEditDialogListener {
        void onConfigurableEditComplete(DialogFragment dialog);
    }


    ConfigurableEditDialogListener mEditListener;
    long mItemRecId = 0; // zero default means new or not specified yet
    Uri mUri, mItemsUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "namers");
    ContentValues mValues = new ContentValues();
    HashMap<Long, String> mExistingItems = new HashMap<Long, String>();
    private TextView mTxtHeaderMsg;
    private EditText mEditItem;
    String mStringItem;

    public static final String DIALOG_TITLE = "DialogTitle";
    public static final String DIALOG_MESSAGE = "DialogMessage";
    public static final String ITEM_REC_ID = "ItemRecId";
    public static final String ITEM_INPUT_TYPE_CODE = "ItemInputTypeCode";
    public static final String MAX_ITEM_LENGTH = "MaxItemLength";
    public static final String ITEM_HINT = "ItemHint";

    private String mDialogTitle = "", mDialogMessage = "", mItemHint = "";

    private int mInputTypeCode = InputType.TYPE_CLASS_TEXT;
    private int maxLength = 0; // zero flag means no text limit

    static ConfigurableEditDialog newInstance(Bundle args) {
        ConfigurableEditDialog f = new ConfigurableEditDialog();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mEditListener = (ConfigurableEditDialogListener) getActivity();
            Log.d(LOG_TAG, "(ConfigurableEditDialogListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement ConfigurableEditDialogListener interface");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        mItemRecId = getArguments().getLong(ITEM_REC_ID);
        mDialogTitle = getArguments().getString(DIALOG_TITLE);
        mDialogMessage = getArguments().getString(DIALOG_MESSAGE);
        mItemHint = getArguments().getString(ITEM_HINT);
        mInputTypeCode = getArguments().getInt(ITEM_INPUT_TYPE_CODE); // text format, e.g. name, date, number
        maxLength = getArguments().getInt(MAX_ITEM_LENGTH);

        View view = inflater.inflate(R.layout.fragment_configurable_edit, root);
        mTxtHeaderMsg = (TextView) view.findViewById(R.id.lbl_hdr_msg);
        mEditItem = (EditText) view.findViewById(R.id.txt_edit_item);
        // set input type
        mEditItem.setInputType(mInputTypeCode);
       /* example       android:inputType="textPersonName|textCapWords"
        android:maxLines="1"*/
        // set maximum lines
//        mEditItem.setMaxLines(); //controls outer boundaries not inner text lines

        // set maximum number of characters
        if (maxLength != 0) { // zero flag means no limit
            InputFilter[] FilterArray = new InputFilter[1];
            FilterArray[0] = new InputFilter.LengthFilter(maxLength);
            mEditItem.setFilters(FilterArray);
        }
        mEditItem.setHint(mItemHint);
        // attempt to automatically show soft keyboard
        mEditItem.requestFocus();
        getDialog().getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mEditItem.setOnFocusChangeListener(this);

//		if (mItemRecId == 0) { // new record
//			getDialog().setTitle(R.string.add_namer_title);
//		} else { // existing record being edited
//			getDialog().setTitle(R.string.edit_namer_title_edit);
//		}
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // during startup, check if arguments are passed to the fragment
        // this is where to do this because the layout has been applied
        // to the fragment
        Bundle args = getArguments();

        if (args != null) {
            mItemRecId = args.getLong("namerId");
            // request existing Items ASAP, this doesn't use the UI
            getLoaderManager().initLoader(Loaders.EXISTING_ITEMS, null, this);
            getLoaderManager().initLoader(Loaders.ITEM_TO_EDIT, null, this);
            // will insert values into screen when cursor is finished
        }
        if (mItemRecId == 0) { // new record
            getDialog().setTitle(R.string.add_namer_title);
            mTxtHeaderMsg.setText(R.string.add_namer_header);
        } else { // existing record being edited
            getDialog().setTitle(R.string.edit_namer_title_edit);
            mTxtHeaderMsg.setText(R.string.edit_namer_label_namername);
        }

    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(!hasFocus) { // something lost focus
            mValues.clear();
            switch (v.getId()) {
            case R.id.txt_edit_item:
                mValues.put("ItemName", mEditItem.getText().toString().trim());
                break;

            default: // save everything
                mValues.put("ItemName", mEditItem.getText().toString().trim());

                }
            Log.d(LOG_TAG, "Saving record in onFocusChange; mValues: " + mValues.toString().trim());
            int numUpdated = saveItemRecord();
            }
        }


    @Override
    public void onCancel (DialogInterface dialog) {
        // update the project record in the database, if everything valid
        mValues.clear();
        mValues.put("ItemName", mEditItem.getText().toString().trim());
        Log.d(LOG_TAG, "Saving record in onCancel; mValues: " + mValues.toString());
        int numUpdated = saveItemRecord();
        if (numUpdated > 0) {
            mEditListener.onConfigurableEditComplete(ConfigurableEditDialog.this);
        }
    }


    private int saveItemRecord () {
        Context c = getActivity();
        // test field for validity
        String namerString = mValues.getAsString("ItemName");

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
        if (mExistingItems.containsValue(namerString)) {
            Toast.makeText(this.getActivity(),
                    c.getResources().getString(R.string.add_namer_duplicate),
                    Toast.LENGTH_LONG).show();
            return 0;
        }
        ContentResolver rs = getActivity().getContentResolver();
        if (mItemRecId == -1) {
            Log.d(LOG_TAG, "entered saveItemRecord with (mItemRecId == -1); canceled");
            return 0;
        }
        if (mItemRecId == 0) { // new record
            mUri = rs.insert(mItemsUri, mValues);
            Log.d(LOG_TAG, "new record in saveItemRecord; returned URI: " + mUri.toString());
            long newRecId = Long.parseLong(mUri.getLastPathSegment());
            if (newRecId < 1) { // returns -1 on error, e.g. if not valid to save because of missing required field
                Log.d(LOG_TAG, "new record in saveItemRecord has Id == " + newRecId + "); canceled");
                return 0;
            }
            mItemRecId = newRecId;
            getLoaderManager().restartLoader(Loaders.EXISTING_ITEMS, null, this);
            mUri = ContentUris.withAppendedId(mItemsUri, mItemRecId);
            Log.d(LOG_TAG, "new record in saveItemRecord; URI re-parsed: " + mUri.toString());
            // set default Item
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putLong(Prefs.DEFAULT_NAMER_ID, mItemRecId);
            prefEditor.commit();
            return 1;
        } else {
            mUri = ContentUris.withAppendedId(mItemsUri, mItemRecId);
            Log.d(LOG_TAG, "about to update record in saveItemRecord; mValues: " + mValues.toString() + "; URI: " + mUri.toString());
            int numUpdated = rs.update(mUri, mValues, null, null);
            Log.d(LOG_TAG, "Saved record in saveItemRecord; numUpdated: " + numUpdated);
            return numUpdated;
        }
    }

/*        public static final int ITEMS = 110; // all Items, to choose from
        public static final int ITEM_TO_EDIT = 111;
        public static final int EXISTING_ITEMS = 112; // Items other than the current, to check duplicates*/            
            
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        switch (id) {
        case Loaders.EXISTING_ITEMS:
            // get the existing Items, other than the current one, to disallow duplicates
            Uri allItemsUri = Uri.withAppendedPath(
                    ContentProvider_VegNab.CONTENT_URI, "namers");
            String[] projection = {"_id", "ItemName"};
            select = "(_id <> " + mItemRecId + ")";
            cl = new CursorLoader(getActivity(), allItemsUri,
                    projection, select, null, null);
            break;
        case Loaders.ITEM_TO_EDIT:
            // First, create the base URI
            // could test here, based on e.g. filters
//			mItemsUri = ContentProvider_VegNab.CONTENT_URI; // get the whole list
            Uri oneItemUri = ContentUris.withAppendedId(
                            Uri.withAppendedPath(
                            ContentProvider_VegNab.CONTENT_URI, "namers"), mItemRecId);
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the dataset being displayed
            // Could build a WHERE clause such as
            // String select = "(Default = true)";
            cl = new CursorLoader(getActivity(), oneItemUri,
                    null, select, null, null);
            break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        switch (loader.getId()) {
        case Loaders.EXISTING_ITEMS:
            mExistingItems.clear();
            while (c.moveToNext()) {
                Log.d(LOG_TAG, "onLoadFinished, add to HashMap: " + c.getString(c.getColumnIndexOrThrow("ItemName")));
                mExistingItems.put(c.getLong(c.getColumnIndexOrThrow("_id")),
                        c.getString(c.getColumnIndexOrThrow("ItemName")));
            }
            Log.d(LOG_TAG, "onLoadFinished, number of items in mExistingItems: " + mExistingItems.size());
            Log.d(LOG_TAG, "onLoadFinished, items in mExistingItems: " + mExistingItems.toString());

            break;
        case Loaders.ITEM_TO_EDIT:
            Log.d(LOG_TAG, "onLoadFinished, records: " + c.getCount());
            if (c.moveToFirst()) {
                mEditItem.setText(c.getString(c.getColumnIndexOrThrow("ItemName")));
            }
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
        case Loaders.ITEM_TO_EDIT:
            // maybe nothing to do here since no adapter
            break;
        }
    }
}
