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

public class SettingsDialog extends DialogFragment {
    private static final String LOG_TAG = SettingsDialog.class.getSimpleName();

    private CheckBox mCkSpeciesOnce, mCkUseLocal;
    private TextView mLocalRegionName;
    private Boolean mOrigStateOfUseLocal; // to track change
    public interface OnUseLocalSppChange {
        // methods that must be implemented in the container Activity
        public void onSettingsDialogUseLocalChanged();
    }
    OnUseLocalSppChange mSetLocalSppCallback; // declare the interface

    static SettingsDialog newInstance(Bundle args) {
        SettingsDialog f = new SettingsDialog();
        f.setArguments(args);
        return f;
    }

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup rootView, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, rootView);
        mCkSpeciesOnce = (CheckBox) view.findViewById(R.id.ck_spp_once);
        mCkUseLocal = (CheckBox) view.findViewById(R.id.ck_use_local);
        mLocalRegionName = (TextView) view.findViewById(R.id.txt_local_name);
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
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        mCkSpeciesOnce.setChecked((sharedPref.getBoolean(VNContract.Prefs.SPECIES_ONCE, true)));
        mCkUseLocal.setChecked((sharedPref.getBoolean(VNContract.Prefs.USE_LOCAL_SPP, true)));
        mLocalRegionName.setText((sharedPref.getString(VNContract.Prefs.LOCAL_SPECIES_LIST_DESCRIPTION,
                this.getResources().getString(R.string.local_region_none))));
    }

    @Override
    public void onCancel (DialogInterface dialog) {
        // update Preferences

        if (LDebug.ON) Log.d(LOG_TAG, "Saving preferences in onCancel");
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor;
        prefEditor = sharedPref.edit();
        prefEditor.putBoolean(VNContract.Prefs.SPECIES_ONCE, mCkSpeciesOnce.isChecked());
        // test if 'UseLocal' has changed, and if so update the database
        if (!((mCkUseLocal.isChecked()) == (sharedPref.getBoolean(VNContract.Prefs.USE_LOCAL_SPP, true)))) {
            // wait to call the update till Prefs have updated
            mCkUseLocal.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSetLocalSppCallback.onSettingsDialogUseLocalChanged();
                }
            }, 50);
        }
        prefEditor.putBoolean(VNContract.Prefs.USE_LOCAL_SPP, mCkUseLocal.isChecked());
        prefEditor.commit();
// maybe implement a listener
//        mSettingsListener.onSettingsComplete(SettingsDialog.this);

    }
}
