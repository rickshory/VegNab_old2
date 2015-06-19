package com.vegnab.vegnab;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;

public class ConfirmDelVegItemDialog extends DialogFragment {
    private static final String LOG_TAG = ConfirmDelVegItemDialog.class.getSimpleName();

    final static String ARG_VI_REC_ID = "viRecId";
    final static String ARG_VI_MSG_STRING = "viMsgString";
    
    public interface ConfirmDeleteVegItemDialogListener {
        void onDeleteVegItemConfirm(DialogFragment dialog);
    }
    ConfirmDeleteVegItemDialogListener mConfirmDeleteVegItemListener;
    long mVegItemRecId = 0;
    String mStringVegItem = "";

    static ConfirmDelVegItemDialog newInstance(Bundle args) {
        ConfirmDelVegItemDialog f = new ConfirmDelVegItemDialog();
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mConfirmDeleteVegItemListener = (ConfirmDeleteVegItemDialogListener) getActivity();
            Log.d(LOG_TAG, "(ConfirmDeleteVegItemDialogListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement ConfirmDeleteVegItemDialogListener interface");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            mVegItemRecId = args.getLong(ARG_VI_REC_ID);
            mStringVegItem = args.getString(ARG_VI_MSG_STRING);
        }

        Builder bld = new Builder(getActivity());
        bld.setTitle(R.string.veg_subpl_list_ctx_delete_verify_title).setMessage(mStringVegItem)
            .setPositiveButton(R.string.action_affirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(LOG_TAG, "In DialogFragment, onCreateDialog, Positive button clicked");
                    Log.d(LOG_TAG, "About to call onDeleteVegItemConfirm=" + mVegItemRecId);
                    mConfirmDeleteVegItemListener.onDeleteVegItemConfirm(ConfirmDelVegItemDialog.this);
                }
            })
            .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(LOG_TAG, "In DialogFragment, onCreateDialog, Negative button clicked");
                }
            });
        return bld.create();
    }
}
