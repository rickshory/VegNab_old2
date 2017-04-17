package com.vegnab.vegnab;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vegnab.vegnab.database.VNContract;
import com.vegnab.vegnab.database.VNContract.LDebug;

import java.util.HashMap;

// android.app.DialogFragment; // maybe use this instead

public class EditSpellingDialog extends DialogFragment {
    private int mValidationLevel = VNContract.Validation.CRITICAL;
    private static final String LOG_TAG = EditSpellingDialog.class.getSimpleName();
    private TextView mEditItem;
    private String mItemText;

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
                        // send args generalized for ? main
//                        args.putString(MainVNActivity.ARG_?);
// maybe just update the database here, rather than sending back to the app?

                        // We send the dialog only to dismiss it in the activity. Can we dismiss it here?
                        mEditSpellingCallback.onEditSpelling(EditSpellingDialog.this, args);
                    } catch (Exception e) {
                        // ignore; if fails, will not update spelling
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
        if (a.containsKey(FixSpellingsFragment.ARG_ITEM_TO_EDIT))
            mEditItem.setText(""
                + a.getString(FixSpellingsFragment.ARG_ITEM_TO_EDIT));
        HashMap<Long, String> existingItems = new HashMap<Long, String>();
        if (a.containsKey(FixSpellingsFragment.ARG_EXISTING_VALUES)) {
            try {
                existingItems = (HashMap<Long, String>) savedInstanceState.getSerializable(
                        FixSpellingsFragment.ARG_EXISTING_VALUES);
            } catch (Exception e) {
                if (LDebug.ON) Log.d(LOG_TAG, "exception: " + e.getMessage());
            }

        }
        // also get ARG_TABLE_NAME, ARG_FIELD_NAME, ARG_RECORD_ID
        // ARG_TEXT_FORMAT, ARG_LENGTH_MIN, ARG_LENGTH_MAX, ARG_EXISTING_VALUES

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

        // validate based on
        // ARG_TEXT_FORMAT, ARG_LENGTH_MIN, ARG_LENGTH_MAX, ARG_EXISTING_VALUES
        // if valid, maybe go ahead and update the database using
        // ARG_TABLE_NAME, ARG_FIELD_NAME, ARG_RECORD_ID

        return false; // for now, return false
    } // end of validation
}
