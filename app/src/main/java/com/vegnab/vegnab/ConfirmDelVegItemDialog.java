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
    public interface DeleteVegItemDialogListener {
        public void onDeleteVegItemConfirm(DialogFragment dialog);
    }
    DeleteVegItemDialogListener mDeleteVegItemListener;
    long mNamerRecId = 0;
    String mStringNamer = "";

    static ConfirmDelVegItemDialog newInstance(long namerRecId, String stringNamer) {
        ConfirmDelVegItemDialog f = new ConfirmDelVegItemDialog();
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
            mDeleteVegItemListener = (DeleteVegItemDialogListener) getActivity();
            Log.d(LOG_TAG, "(DeleteVegItemDialogListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement DeleteVegItemDialogListener interface");
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

        Builder bld = new Builder(getActivity());
        bld.setTitle(R.string.del_namer_confirm).setMessage(mStringNamer)
            .setPositiveButton(R.string.action_affirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(LOG_TAG, "In DialogFragment, onCreateDialog, Positive button clicked");
                    Log.d(LOG_TAG, "About to delete Namer record id=" + mNamerRecId);

                    //old, not used any more: "DELETE FROM Namers WHERE _id = ?;" {"" + mNamerRecId}
                    Uri uri = ContentUris.withAppendedId(
                            Uri.withAppendedPath(
                            ContentProvider_VegNab.CONTENT_URI, "namers"), mNamerRecId);
                    Log.d(LOG_TAG, "In ConfirmDelNamerDialog URI: " + uri.toString());
                    ContentResolver rs = getActivity().getContentResolver();
                    int numDeleted = rs.delete(uri, null, null);
                    Log.d(LOG_TAG, "numDeleted: " + numDeleted);
                    mDeleteVegItemListener.onDeleteVegItemConfirm(ConfirmDelVegItemDialog.this);
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
