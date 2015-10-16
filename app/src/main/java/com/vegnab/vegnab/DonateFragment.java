package com.vegnab.vegnab;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.internal.widget.AdapterViewCompat.AdapterContextMenuInfo;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.Prefs;
import com.vegnab.vegnab.database.VNContract.Tags;
import com.vegnab.vegnab.database.VNContract.Validation;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class DonateFragment extends Fragment implements OnClickListener {

    private static final String LOG_TAG = DonateFragment.class.getSimpleName();

    OnButtonListener mButtonCallback; // declare the interface
    // declare that the container Activity must implement this interface
    public interface OnButtonListener {
        // methods that must be implemented in the container Activity
        void onDonateButtonClicked(Bundle args);
    }

    public interface OnIAPDoneListener {
        void onINAppPurchaseComplete(DonateFragment donateFragment);
    }
    OnIAPDoneListener mIAPDoneListener;

    public static DonateFragment newInstance(Bundle args) {
        DonateFragment f = new DonateFragment();
        f.setArguments(args);
        return f;
    }
    private RadioGroup mDonationOptsRadioGp;
    private RadioButton mDonate001_00;
    private RadioButton mDonate003_00;
    private RadioButton mDonate010_00;
    private RadioButton mDonate030_00;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get a Tracker (should auto-report)
        ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        setHasOptionsMenu(true);
    }

/*
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.donate, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
*/
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        FragmentManager fm = getActivity().getSupportFragmentManager();
        switch (item.getItemId()) { // the Activity has first opportunity to handle these
        // any not handled come here to this Fragment
            case R.id.action_donate_info:
                Toast.makeText(getActivity(), "''App Info'' of Visit Header is not implemented yet", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // if the activity was re-created (e.g. from a screen rotate)
        // restore the previous screen, remembered by onSaveInstanceState()
        // This is mostly needed in fixed-pane layouts
        if (savedInstanceState != null) {
            // mDonationOptionSelected = savedInstanceState.getLong(ARG_DONATION, 0);
        } else {
            Log.d(LOG_TAG, "In onCreateView, savedInstanceState == null");
        }
        // inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_donate, container, false);
        mDonationOptsRadioGp = (RadioGroup) rootView.findViewById(R.id.radio_group_opts_resolve_phs);
        // Prepare the loader
        // maybe use a loader for purchases later, but none now
        //getLoaderManager().initLoader(Loaders.DONATIONS, null, this);

        // set click listener for the button in the view
        Button b = (Button) rootView.findViewById(R.id.donate_go_button);
        b.setOnClickListener(this);
        // if more, loop through all the child items of the ViewGroup rootView and
        // set the onclicklistener for all the Button instances found
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(getActivity()).reportActivityStart(getActivity());
        // check if arguments are passed to the fragment that will change the layout
        Bundle args = getArguments();
/*
        if (args != null) {
            if (mDonationOptionSelected == 0) {
                // On return from Subplots container, this method can re-run before
                // SaveInstanceState and so retain arguments originally passed when created,
                // such as VisitId=0.
                // Do not allow that zero to overwrite a new (nonzero) Visit ID, or
                // it will flag to create a second copy of the same header.
                mDonationOptionSelected = args.getLong(ARG_DONATION, 0);
                // set up on screen
            }
        // also use for special arguments like screen layout
        }
*/
        // fire off loaders that depend on layout being ready to receive results
        // not yet used
//        getLoaderManager().initLoader(Loaders.DONATE, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
//	    do other setup here if needed
        // re-check this every time the fragment is entered
//        getLoaderManager().restartLoader(Loaders.VISIT_PLACEHOLDERS_ENTERED, null, this);
//        if (!mLocIsGood) {
//            mGoogleApiClient.connect();
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // if namer spinner has been changed
//        if (mNamerSpinner.getId() != mNamerId) {
//            // attempt to save record
//            saveVisitRecord();
//        }
//        if (mGoogleApiClient.isConnected()) {
//            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
//            mGoogleApiClient.disconnect();
//        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // assure the container activity has implemented the callback interface
        try {
            mButtonCallback = (OnButtonListener) getActivity();
            Log.d(LOG_TAG, "(OnButtonListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement OnButtonListener interface");
        }
        try {
            mIAPDoneListener = (OnIAPDoneListener) getActivity();
            Log.d(LOG_TAG, "(IAPDoneListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement IAPDoneListener interface");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        outState.putLong(ARG_DONATION, mDonationOptionSelected);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

        case R.id.donate_go_button:
            // maybe implement the tracker here
            Bundle args = new Bundle();
            // put in any needed parameters
            switch (mDonationOptsRadioGp.getCheckedRadioButtonId()) {
                case R.id.radio_amt_usd001_00:
                    Log.d(LOG_TAG, "mDonationOptsRadioGp radio button selected: R.id.radio_amt_usd001_00");
                    break;
                case R.id.radio_amt_usd003_00:
                    Log.d(LOG_TAG, "mDonationOptsRadioGp radio button selected: R.id.radio_amt_usd003_00");
                    break;
                case R.id.radio_amt_usd010_00:
                    Log.d(LOG_TAG, "mDonationOptsRadioGp radio button selected: R.id.radio_amt_usd010_00");
                    break;
                case R.id.radio_amt_usd030_00:
                    Log.d(LOG_TAG, "mDonationOptsRadioGp radio button selected: R.id.radio_amt_usd030_00");
                    break;
                default:
                    Log.d(LOG_TAG, "mDonationOptsRadioGp no radio button selected");
                    break;
            }

            //
            Log.d(LOG_TAG, "in onClick, about to do 'mButtonCallback.onDonateButtonClicked(args)'");
            mButtonCallback.onDonateButtonClicked(args);
            Log.d(LOG_TAG, "in onClick, completed 'mButtonCallback.onDonateButtonClicked(args)'");
            break;
        }
    }

/*
    // Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
*/
}