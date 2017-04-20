package com.vegnab.vegnab;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
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

import java.util.HashMap;

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
                    Toast.makeText(getContext(),
                            "Validated OK",
                            Toast.LENGTH_LONG).show();
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
                    Toast.makeText(getContext(),
                            "Did not validate",
                            Toast.LENGTH_LONG).show();
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
        if (a.containsKey(FixSpellingsFragment.ARG_LENGTH_MAX)) {
            // not sure if input type is a filter, so add on to any existing
            InputFilter[] filterArray = mEditItem.getFilters();
            filterArray[filterArray.length] =
                    new InputFilter.LengthFilter(a.getInt(FixSpellingsFragment.ARG_LENGTH_MAX));
            mEditItem.setFilters(filterArray);
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
        String tableName, fieldName;
        Bundle a = this.getArguments();
        if (a.containsKey(FixSpellingsFragment.ARG_RECORD_ID)) {
            recId = a.getLong(FixSpellingsFragment.ARG_RECORD_ID);
        } else return false; // won't be able to do anything without record ID
        if (a.containsKey(FixSpellingsFragment.ARG_TABLE_NAME)) {
            tableName = a.getString(FixSpellingsFragment.ARG_TABLE_NAME);
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
        Uri uri, tblUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, tableName);
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

}
