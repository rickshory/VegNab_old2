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
//        // supply arguments
//        Bundle args = new Bundle();
//        args.putLong("vegItemRecId", vegItemRecId);
//        args.putString("stringVegItem", stringVegItem);
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
//			Log.d(LOG_TAG, "In DialogFragment, onCreateDialog, mVegItemRecId: " + mVegItemRecId);
//			Log.d(LOG_TAG, "In DialogFragment, onCreateDialog, mStringVegItem: " + mStringVegItem);
        }

        Builder bld = new Builder(getActivity());
        bld.setTitle(R.string.del_namer_confirm).setMessage(mStringVegItem)
            .setPositiveButton(R.string.action_affirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(LOG_TAG, "In DialogFragment, onCreateDialog, Positive button clicked");
                    Log.d(LOG_TAG, "About to delete Namer record id=" + mVegItemRecId);

                    //old, not used any more: "DELETE FROM Namers WHERE _id = ?;" {"" + mVegItemRecId}
                    Uri uri = ContentUris.withAppendedId(
                            Uri.withAppendedPath(
                            ContentProvider_VegNab.CONTENT_URI, "namers"), mVegItemRecId);
                    Log.d(LOG_TAG, "In ConfirmDelNamerDialog URI: " + uri.toString());
                    ContentResolver rs = getActivity().getContentResolver();
                    int numDeleted = rs.delete(uri, null, null);
                    Log.d(LOG_TAG, "numDeleted: " + numDeleted);
                    mConfirmDeleteVegItemListener.onDeleteVegItemConfirm(ConfirmDelVegItemDialog.this);
                }
            })
            .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(LOG_TAG, "In DialogFragment, onCreateDialog, Negative button clicked");
                    // User cancelled the dialog
                }
            });
        return bld.create();
    }
}
