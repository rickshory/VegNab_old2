package com.vegnab.vegnab;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class HelpUnderConstrDialog extends DialogFragment {
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.msg_help_apology)
			   .setPositiveButton(R.string.action_affirm, new DialogInterface.OnClickListener() {
				   public void onClick(DialogInterface dialog, int id) {
				   }
			   });
		return builder.create();
	}
}
