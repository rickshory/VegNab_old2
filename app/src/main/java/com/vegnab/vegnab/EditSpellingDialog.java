package com.vegnab.vegnab;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract;
import com.vegnab.vegnab.database.VNContract.LDebug;
import com.vegnab.vegnab.util.InputFilterSppNamer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// android.app.DialogFragment; // maybe use this instead

public class EditSpellingDialog extends DialogFragment {
    private int mValidationLevel = VNContract.Validation.CRITICAL;
    private static final String LOG_TAG = EditSpellingDialog.class.getSimpleName();
    private TextView mEditItem;

    public interface SpellingEditListener {
        // methods that must be implemented in the container Activity
        void onEditSpelling(DialogFragment dialog, Bundle args);
    }
    // Use this instance of the interface to deliver action events
    SpellingEditListener mEditSpellingCallback; // declare the interface

    final static String ARG_TOOLBAR_HEADER = "hdrStr";

    static EditSpellingDialog newInstance(Bundle args) {
        EditSpellingDialog f = new EditSpellingDialog();
        f.setArguments(args);
        return f;
    }

    // Test to make sure implemented:
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // assure the container activity has implemented the callback interface
        try {
            mEditSpellingCallback = (SpellingEditListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException (activity.toString()
                + " must implement SpellingEditListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup rootView, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_spelling, rootView);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.edit_spelling_toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDialog().onBackPressed();
                }
            });
            toolbar.setTitle(this.getArguments().getString(ARG_TOOLBAR_HEADER));
        }
        Button  saveButton = (Button) view.findViewById(R.id.btn_edit_spelling_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (validateEditSpelling()) {
//                    Toast.makeText(getContext(),
//                            "Validated OK",
//                            Toast.LENGTH_LONG).show();
                    try { // can fail with null pointer exception if fragment is gone
                        Bundle args = new Bundle();
                        // maybe send in bundle that fragment(s) that are up, and need refresh
                        // send args generalized for ? main
//                        args.putString(MainVNActivity.ARG_?);
                        // already updated the database, after validation
                        // here, rather than sending back to the app?
                        // We send the dialog only to dismiss it in the activity. Can we dismiss it here?
                        mEditSpellingCallback.onEditSpelling(EditSpellingDialog.this, args);
                    } catch (Exception e) {
                        // ignore; if fails, will not dismiss dialog or fragment called from
                    }
                } else {
//                    Toast.makeText(getContext(),
//                            "Did not validate",
//                            Toast.LENGTH_LONG).show();
                }
            }
        });
        mEditItem = (EditText) view.findViewById(R.id.txt_edit_spelling);
        Bundle a = this.getArguments();
        if (a.containsKey(FixSpellingsFragment.ARG_ITEM_TO_EDIT)) {
            mEditItem.setText(""
                    + a.getString(FixSpellingsFragment.ARG_ITEM_TO_EDIT));
        }
        if (a.containsKey(FixSpellingsFragment.ARG_INPUT_TYPE)) {
            mEditItem.setInputType(a.getInt(FixSpellingsFragment.ARG_INPUT_TYPE));
        }
        // if a species Namer, validate for filename too because this will be a
        //  folder name for Placeholder pictures
        if (a.containsKey(FixSpellingsFragment.ARG_TABLE_URI)) {
            if (a.getString(FixSpellingsFragment.ARG_TABLE_URI) == "namers") {
                if (LDebug.ON) Log.d(LOG_TAG, "Editing a namer, about to set filename inputFilter");
                mEditItem.setFilters(new InputFilter[] { new InputFilterSppNamer() });
            }
        }

        // check for and add length filter
        if (a.containsKey(FixSpellingsFragment.ARG_LENGTH_MAX)) {
            if (LDebug.ON) Log.d(LOG_TAG, "About to set item max length to "
                    + a.getInt(FixSpellingsFragment.ARG_LENGTH_MAX));
            InputFilter curFilters[];
            InputFilter.LengthFilter lengthFilter;
            boolean alreadyHasALengthFilter = false;
            lengthFilter = new InputFilter.LengthFilter(a.getInt(FixSpellingsFragment.ARG_LENGTH_MAX));
            curFilters = mEditItem.getFilters();
            if (curFilters != null) {
                if (LDebug.ON) Log.d(LOG_TAG, "There were already " + curFilters.length + " filters");
                for (int idx = 0; idx < curFilters.length; idx++) {
                    if (curFilters[idx] instanceof InputFilter.LengthFilter) {
                        curFilters[idx] = lengthFilter;
                        alreadyHasALengthFilter = true;
                        if (LDebug.ON) Log.d(LOG_TAG, "There was already a length filter, now replaced");
                    }
                }
                if (!alreadyHasALengthFilter) {
                    // there are filters, but a length filter is not one of them
                    // add the new one
                    InputFilter newFilters[] = new InputFilter[curFilters.length + 1];
                    System.arraycopy(curFilters, 0, newFilters, 0, curFilters.length);
                    newFilters[curFilters.length] = lengthFilter;
                    mEditItem.setFilters(newFilters);
                    if (LDebug.ON) Log.d(LOG_TAG, "No length filter yet, new one added");
                }
            } else { // no filters yet, set the filters array to only this length filter
                mEditItem.setFilters(new InputFilter[] { lengthFilter });
                if (LDebug.ON) Log.d(LOG_TAG, "No existing filters, length filter added");
            }
        }

        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Remove dialog title
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        // during startup, check if arguments are passed to the fragment
        // this is where to do this because the layout has been applied
        // to the fragment
        Bundle args = getArguments();
        setupUI();
    }

    void setupUI() {
    }

    @Override
    public void onCancel (DialogInterface dialog) {
        //
        if (LDebug.ON) Log.d(LOG_TAG, "Verification in onCancel");

// maybe implement a listener
//        mSettingsListener.onSettingsComplete(LocManualEntryDialog.this);

    }

    private boolean validateEditSpelling() {
        // validate all user-accessible items
        Context c = getActivity();
        String stringProblem;
        String errTitle = c.getResources().getString(R.string.vis_hdr_validate_generic_title);
        ConfigurableMsgDialog flexErrDlg;

        int lengthMin = 0, lengthMax = Integer.MAX_VALUE;
        long recId;
        String tableUriName, fieldName;
        Bundle a = this.getArguments();
        if (a.containsKey(FixSpellingsFragment.ARG_RECORD_ID)) {
            recId = a.getLong(FixSpellingsFragment.ARG_RECORD_ID);
        } else return false; // won't be able to do anything without record ID, or these other params
        if (a.containsKey(FixSpellingsFragment.ARG_TABLE_URI)) {
            tableUriName = a.getString(FixSpellingsFragment.ARG_TABLE_URI);
        } else return false;
        if (a.containsKey(FixSpellingsFragment.ARG_FIELD_NAME)) {
            fieldName = a.getString(FixSpellingsFragment.ARG_FIELD_NAME);
        } else return false;
        // validate based on input type too?
        if (a.containsKey(FixSpellingsFragment.ARG_LENGTH_MIN)) {
            lengthMin = a.getInt(FixSpellingsFragment.ARG_LENGTH_MIN);
        }
        if (a.containsKey(FixSpellingsFragment.ARG_LENGTH_MAX)) {
            lengthMax = a.getInt(FixSpellingsFragment.ARG_LENGTH_MAX);
        }
        HashMap<Long, String> existingItems = new HashMap<Long, String>();
        if (a.containsKey(FixSpellingsFragment.ARG_EXISTING_VALUES)) {
            try {
                existingItems = (HashMap<Long, String>) a.getSerializable(
                        FixSpellingsFragment.ARG_EXISTING_VALUES);
            } catch (Exception e) {
                if (LDebug.ON) Log.d(LOG_TAG, "exception: " + e.getMessage());
                return false;
            }
        }

        // validate item
        String stItem = mEditItem.getText().toString().trim();
        if (stItem.length() == 0) {
            if (LDebug.ON) Log.d(LOG_TAG, "Item is length zero");
            if (mValidationLevel > VNContract.Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.edit_spellings_item_missing);
                if (mValidationLevel == VNContract.Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == VNContract.Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "edit_spellings_item_missing");
                }
            } // end of validation not silent
            mEditItem.requestFocus();
            return false; // end of Item length zero
        } else if (stItem.length() < lengthMin) {
            if (LDebug.ON) Log.d(LOG_TAG, "Item is too short, < " + lengthMin);
            if (mValidationLevel > VNContract.Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.edit_spellings_item_too_short);
                if (mValidationLevel == VNContract.Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == VNContract.Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "edit_spellings_item_too_short");
                    mEditItem.requestFocus();
                }
            } // end of validation not silent
            mEditItem.requestFocus();
            return false; // end of Item too short
        } else if (stItem.length() > lengthMax) {
            if (LDebug.ON) Log.d(LOG_TAG, "Item is too long, > " + lengthMax);
            if (mValidationLevel > VNContract.Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.edit_spellings_item_too_long);
                if (mValidationLevel == VNContract.Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == VNContract.Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "edit_spellings_item_too_long");
                    mEditItem.requestFocus();
                }
            } // end of validation not silent
            mEditItem.requestFocus();
            return false; // end of Item too long
        } else { // test the list of existing items
            if (existingItems.containsKey(recId)) {
                // remove the item matching the one we are working on, so
                // as to not interfere with duplicate checking
                existingItems.remove(recId);
            }
            if (existingItems.containsValue(stItem)) {
                // if it still contains this value, it's a duplicate
                if (LDebug.ON) Log.d(LOG_TAG, "Item already in: " + stItem);
                if (mValidationLevel > VNContract.Validation.SILENT) {
                    stringProblem = c.getResources().getString(R.string.edit_spellings_item_already_in);
                    if (mValidationLevel == VNContract.Validation.QUIET) {
                        Toast.makeText(this.getActivity(),
                                stringProblem,
                                Toast.LENGTH_LONG).show();
                    }
                    if (mValidationLevel == VNContract.Validation.CRITICAL) {
                        flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                        flexErrDlg.show(getFragmentManager(), "edit_spellings_item_already_in");
                        mEditItem.requestFocus();
                    }
                } // end of validation not silent
                mEditItem.requestFocus();
                return false; // end of Item too long
            }
        } // end of validate item
        // if we got to here, we have everything we need to update the item
        // if this was a species Namer, update the folder for pictures
        if (tableUriName == "namers") {
            if (LDebug.ON) Log.d(LOG_TAG, "Editing a namer, about to get existing name");
            if (a.containsKey(FixSpellingsFragment.ARG_ITEM_TO_EDIT)) {
                String currentNamerString = a.getString(FixSpellingsFragment.ARG_ITEM_TO_EDIT);
                if (LDebug.ON) Log.d(LOG_TAG, "Existing namer:" + currentNamerString);
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    //min SDK version is Honeycomb 3.0, API level 11, so can depend on following
                    File appPixDir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), BuildConfig.PUBLIC_DB_FOLDER);
                    if (appPixDir.exists()) {
                        if (LDebug.ON) Log.d(LOG_TAG, "appPixDir exists: " + appPixDir.getAbsolutePath());
                        File namerPixDir = new File(appPixDir, currentNamerString);
                        if (namerPixDir.exists()){
                            if (LDebug.ON) Log.d(LOG_TAG, "namerPixDir exists: " + namerPixDir.getAbsolutePath());
                            if (namerPixDir.isDirectory()) {
                                if (LDebug.ON) Log.d(LOG_TAG, "namerPixDir is a folder");
                                // to correctly update Media after renaming,
                                // will send a list that contains both the old filenames ("deleted")
                                // and the new filenames ("added")
                                // first get the old filenames
                                List<File> files = getListFiles(namerPixDir);
                                // for testing, list them
                                for (File f : files) {
                                    if (LDebug.ON) Log.d(LOG_TAG, "existing file: " + f.getAbsolutePath());
                                }
                                String pixDirNameOrig = namerPixDir.getAbsolutePath();
                                File namerPixDirNew = new File(appPixDir, stItem);
                                String pixDirNameNew = namerPixDirNew.getAbsolutePath();
                                boolean namerPixDirChanged = namerPixDir.renameTo(namerPixDirNew);
                                if (namerPixDirChanged) {
                                    /* fix this, not correct
                                    // make renamed folder visible
                                    MediaScannerConnection.scanFile(getActivity().getApplicationContext(),
                                            new String[]{namerPixDirNew.getAbsolutePath()}, null, null);
                                    */
                                    if (LDebug.ON) Log.d(LOG_TAG, "folder '" + pixDirNameOrig
                                            + "' renamed '" + pixDirNameNew + "'" );
                                    if (LDebug.ON) Log.d(LOG_TAG, "from getAbsolutePath: " + namerPixDir.getAbsolutePath());
                                } else {
                                    if (LDebug.ON) Log.d(LOG_TAG, "could not rename namerPixDir");
                                }
//                                String appPixDirName = namerPixDir.getParent();
//                                if (LDebug.ON) Log.d(LOG_TAG, "appPixDirName: " + appPixDirName);
//                                if (LDebug.ON) Log.d(LOG_TAG, "appPixDirName length: " + appPixDirName.length());
                            } else {
                                if (LDebug.ON) Log.d(LOG_TAG, "namerPixDir is not a folder");
                            }
                        } else {
                            if (LDebug.ON) Log.d(LOG_TAG, "namerPixDir does not exist");
                        }
                    } else {
                        if (LDebug.ON) Log.d(LOG_TAG, "appPixDir does not exists");
                    }
                } else {
                    if (LDebug.ON) Log.d(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
                }
            } else {
                if (LDebug.ON) Log.d(LOG_TAG, "no Namer in args");
            }
        }

        // update the item in the database
        Uri uri, tblUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, tableUriName);
        uri = ContentUris.withAppendedId(tblUri, recId);
        ContentValues values = new ContentValues();
        values.put(fieldName, stItem);
        if (LDebug.ON) Log.d(LOG_TAG, "about to update record; values: "
                + values.toString().trim() + "; URI: " + uri.toString());
        ContentResolver rs = getActivity().getContentResolver();
        int numUpdated = rs.update(uri, values, null, null);
        if (numUpdated == 1) return true;

        return false;
    } // end of validation

    List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                inFiles.add(file);
            }
        }
        return inFiles;
    }

}
