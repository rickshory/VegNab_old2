package com.vegnab.vegnab;

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

import com.vegnab.vegnab.database.VNContract;
import com.vegnab.vegnab.database.VNContract.LDebug;

public class SettingsDialog extends DialogFragment {
    private static final String LOG_TAG = SettingsDialog.class.getSimpleName();

    private CheckBox mCkSpeciesOnce;

    static SettingsDialog newInstance(Bundle args) {
        SettingsDialog f = new SettingsDialog();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup rootView, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, rootView);
        mCkSpeciesOnce = (CheckBox) view.findViewById(R.id.ck_spp_once);
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
    }

    @Override
    public void onCancel (DialogInterface dialog) {
        // update Preferences

        if (LDebug.ON) Log.d(LOG_TAG, "Saving preferences in onCancel");

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor;
        prefEditor = sharedPref.edit();
        prefEditor.putBoolean(VNContract.Prefs.SPECIES_ONCE, mCkSpeciesOnce.isChecked());
        prefEditor.commit();
// maybe implement a listener
//        mSettingsListener.onSettingsComplete(SettingsDialog.this);

    }
}