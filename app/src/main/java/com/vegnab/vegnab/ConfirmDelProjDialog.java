package com.vegnab.vegnab;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

public class ConfirmDelProjDialog extends DialogFragment {
	private static final String LOG_TAG = ConfirmDelProjDialog.class.getSimpleName();
	long mProjRecId = 0;
	String mProjCode = "";
	ContentValues mValues = new ContentValues();
	
	static ConfirmDelProjDialog newInstance(long projRecId, String projCode) {
		ConfirmDelProjDialog f = new ConfirmDelProjDialog();
		// supply arguments
		Bundle args = new Bundle();
		args.putLong("projRecId", projRecId);
		args.putString("projCode", projCode);
		f.setArguments(args);
		return f;
	};

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null) {
			mProjRecId = args.getLong("projRecId");
			mProjCode = args.getString("projCode");
//			Log.v(LOG_TAG, "In ConfirmDelProjDialog DialogFragment, onCreateDialog, mProjRecId: " + mProjRecId);
//			Log.v(LOG_TAG, "In ConfirmDelProjDialog DialogFragment, onCreateDialog, mProjCode: " + mProjCode);
		}
		AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
		bld.setTitle(R.string.del_proj_confirm).setMessage(mProjCode)
			.setPositiveButton(R.string.action_affirm, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.v(LOG_TAG, "In ConfirmDelProjDialog DialogFragment, onCreateDialog, Positive button clicked");
					Log.v(LOG_TAG, "In ConfirmDelProjDialog about to delete Project record id=" + mProjRecId);
					
					//old, not used any more: "DELETE FROM Projects WHERE _id = ?;" {"" + mProjRecId}
					// "UPDATE Projects SET IsDeleted = 1 WHERE _id = ?;" {"" + mProjRecId}
					mValues.clear();
					mValues.put("IsDeleted", 1);
					Uri uri = ContentUris.withAppendedId(
							Uri.withAppendedPath(
							ContentProvider_VegNab.CONTENT_URI, "projects"), mProjRecId);
					Log.v(LOG_TAG, "In ConfirmDelProjDialog URI: " + uri.toString());
					ContentResolver rs = getActivity().getContentResolver();
					int numUpdated = rs.update(uri, mValues, null, null);
					Log.v(LOG_TAG, "In ConfirmDelProjDialog numUpdated: " + numUpdated);
				}
				
			})
			.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	Log.v(LOG_TAG, "In ConfirmDelProjDialog DialogFragment, onCreateDialog, Negative button clicked");
	                // User cancelled the dialog
	            }
	        });
		return bld.create();
	}
}
