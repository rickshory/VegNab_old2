package com.vegnab.vegnab;

import com.vegnab.vegnab.database.VNContract.LDebug;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;

import android.app.AlertDialog;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

public class ConfirmDelNamerDialog extends DialogFragment {
    private static final String LOG_TAG = ConfirmDelNamerDialog.class.getSimpleName();
    public interface EditNamerDialogListener {
        public void onEditNamerComplete(DialogFragment dialog);
    }
    EditNamerDialogListener mEditNamerListener;
    long mNamerRecId = 0;
    String mStringNamer = "";

    static ConfirmDelNamerDialog newInstance(long namerRecId, String stringNamer) {
        ConfirmDelNamerDialog f = new ConfirmDelNamerDialog();
        // supply arguments
        Bundle args = new Bundle();
        args.putLong("namerRecId", namerRecId);
        args.putString("stringNamer", stringNamer);
        f.setArguments(args);
        return f;
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mEditNamerListener = (EditNamerDialogListener) getActivity();
           if (LDebug.ON) Log.d(LOG_TAG, "(EditNamerDialogListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement EditNamerDialogListener interface");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            mNamerRecId = args.getLong("namerRecId");
            mStringNamer = args.getString("stringNamer");
//			Log.d(LOG_TAG, "In DialogFragment, onCreateDialog, mNamerRecId: " + mNamerRecId);
//			Log.d(LOG_TAG, "In DialogFragment, onCreateDialog, mStringNamer: " + mStringNamer);
        }

        AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
        bld.setTitle(R.string.del_namer_confirm).setMessage(mStringNamer)
            .setPositiveButton(R.string.action_affirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                   if (LDebug.ON) Log.d(LOG_TAG, "In DialogFragment, onCreateDialog, Positive button clicked");
                   if (LDebug.ON) Log.d(LOG_TAG, "About to delete Namer record id=" + mNamerRecId);

                    //old, not used any more: "DELETE FROM Namers WHERE _id = ?;" {"" + mNamerRecId}
                    Uri uri = ContentUris.withAppendedId(
                            Uri.withAppendedPath(
                            ContentProvider_VegNab.CONTENT_URI, "namers"), mNamerRecId);
                   if (LDebug.ON) Log.d(LOG_TAG, "In ConfirmDelNamerDialog URI: " + uri.toString());
                    ContentResolver rs = getActivity().getContentResolver();
                    int numDeleted = rs.delete(uri, null, null);
                   if (LDebug.ON) Log.d(LOG_TAG, "numDeleted: " + numDeleted);
                    mEditNamerListener.onEditNamerComplete(ConfirmDelNamerDialog.this);
                }
            })
            .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                   if (LDebug.ON) Log.d(LOG_TAG, "In DialogFragment, onCreateDialog, Negative button clicked");
                    // User cancelled the dialog
                }
            });
        return bld.create();
    }
}
