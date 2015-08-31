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

public class ExportVisitDialog extends DialogFragment {
    private static final String LOG_TAG = ExportVisitDialog.class.getSimpleName();

    public interface ExportVisitListener {
        void onExportVisitRequest(Bundle paramsBundle);
    }
    ExportVisitListener mExpVisListener;

    public static final String VISIT_TO_EXPORT_ID = "VisToExportId";
    private long mVisToExportRecId = 0; // zero default means new or not specified yet

    public static final String VIS_EXPORT_FILENAME = "VisExportFileName";
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
//        mTxtFileNameToExport = (TextView) view.findViewById(R.id.lbl_spp_item);

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
//            mVegItemRecId = args.getLong(VEG_ITEM_REC_ID);
//            mStrVegCode = args.getString(VEG_CODE);
        }
    }
}
