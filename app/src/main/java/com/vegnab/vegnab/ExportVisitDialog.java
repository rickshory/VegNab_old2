package com.vegnab.vegnab;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ExportVisitDialog extends DialogFragment implements android.view.View.OnClickListener {
    private static final String LOG_TAG = ExportVisitDialog.class.getSimpleName();

    public interface ExportVisitListener {
        void onExportVisitRequest(Bundle paramsBundle);
    }
    ExportVisitListener mExpVisListener;
    
    private long mVisToExportRecId = 0; // zero default means new or not specified yet
    private String mVisExportVisName = null;
    private String mVisExportFileName = null;

    private TextView mTxtHeader, mTxtFileNameToExport;
    SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    static ExportVisitDialog newInstance(Bundle args) {
        ExportVisitDialog f = new ExportVisitDialog();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mExpVisListener = (ExportVisitListener) getActivity();
            Log.d(LOG_TAG, "(ExportVisitListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement ExportVisitListener interface");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_export_visit, root);

        mTxtHeader = (TextView) view.findViewById(R.id.lbl_export_visit);
        mTxtFileNameToExport = (TextView) view.findViewById(R.id.lbl_export_visit_filename);

        getDialog().setTitle(R.string.export_visit_dlg_title);
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
            mVisToExportRecId = args.getLong(MainVNActivity.ARG_VISIT_TO_EXPORT_ID);
            mVisExportVisName = args.getString(MainVNActivity.ARG_VISIT_TO_EXPORT_NAME);
            mVisExportFileName = args.getString(MainVNActivity.ARG_VISIT_TO_EXPORT_FILENAME);
            mTxtFileNameToExport.setText(mVisExportFileName);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.export_visit_cancel_button:
                this.dismiss();
                break;

            case R.id.export_visit_export_button:
                Bundle expArgs = new Bundle();
                // for testing, send same args as received
                // in future, allow user to override
                expArgs.putLong(MainVNActivity.ARG_VISIT_TO_EXPORT_ID, mVisToExportRecId);
                expArgs.putString(MainVNActivity.ARG_VISIT_TO_EXPORT_NAME, mVisExportVisName);
                expArgs.putString(MainVNActivity.ARG_VISIT_TO_EXPORT_FILENAME, mVisExportFileName);
                // put any other parameters in, such as
                // format of output, whether to resolve Placeholders, etc.
                mExpVisListener.onExportVisitRequest(expArgs);
                break;
        }
    }
}
