package com.vegnab.vegnab;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.vegnab.vegnab.database.VNContract;
import com.vegnab.vegnab.database.VNContract.LDebug;

public class LocManualEntryDialog extends DialogFragment {
    private static final String LOG_TAG = LocManualEntryDialog.class.getSimpleName();
    private TextView mManualLatitude, mManualLongitude, mManualAccuracy;
/*
    public interface OnUseLocalSppChange {
        // methods that must be implemented in the container Activity
        public void onSettingsDialogUseLocalChanged();
    }
    OnUseLocalSppChange mSetLocalSppCallback; // declare the interface
*/
    static LocManualEntryDialog newInstance(Bundle args) {
        LocManualEntryDialog f = new LocManualEntryDialog();
        f.setArguments(args);
        return f;
    }

    /*
    // Test to make sure implemented:
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // assure the container activity has implemented the callback interface
        try {
            mSetLocalSppCallback = (OnUseLocalSppChange) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException (activity.toString() + " must implement OnUseLocalSppChange");
        }
    }
*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup rootView, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loc_manual_entry, rootView);

        mManualLatitude = (TextView) view.findViewById(R.id.txt_manual_latitude);
        mManualLongitude = (TextView) view.findViewById(R.id.txt_manual_longitude);
        mManualAccuracy = (TextView) view.findViewById(R.id.txt_manual_accuracy);
        getDialog().setTitle(R.string.settings_dlg_title);
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
            // we don't use any args yet
        }
        setupUI();
    }

    void setupUI() {
/*
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        mManualLatitude.setText((sharedPref.getString(VNContract.Prefs.LOCAL_SPECIES_LIST_DESCRIPTION,
                this.getResources().getString(R.string.local_region_none))));
        mManualLongitude.setText((sharedPref.getString(VNContract.Prefs.LOCAL_SPECIES_LIST_DESCRIPTION,
                this.getResources().getString(R.string.local_region_none))));
        mManualAccuracy.setText((sharedPref.getString(VNContract.Prefs.LOCAL_SPECIES_LIST_DESCRIPTION,
                this.getResources().getString(R.string.local_region_none))));
*/
    }

    @Override
    public void onCancel (DialogInterface dialog) {
        //
        if (LDebug.ON) Log.d(LOG_TAG, "Verification in onCancel");

// maybe implement a listener
//        mSettingsListener.onSettingsComplete(LocManualEntryDialog.this);

    }
}
