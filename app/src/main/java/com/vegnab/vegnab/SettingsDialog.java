package com.vegnab.vegnab;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.vegnab.vegnab.database.VNContract.LDebug;

import java.text.SimpleDateFormat;
import java.util.Locale;

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
    }
}
