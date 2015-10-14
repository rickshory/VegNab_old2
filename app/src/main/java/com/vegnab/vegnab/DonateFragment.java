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
        void onINAppPurcaseComplete(DonateFragment donateFragment);
    }
    OnIAPDoneListener mIAPDoneListener;

    public static DonateFragment newInstance(Bundle args) {
        DonateFragment f = new DonateFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get a Tracker (should auto-report)
        ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
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
    setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.visit_header, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        FragmentManager fm = getActivity().getSupportFragmentManager();
//		DialogFragment editProjDlg;
        switch (item.getItemId()) { // the Activity has first opportunity to handle these
        // any not handled come here to this Fragment
            case R.id.action_app_info:
                Toast.makeText(getActivity(), "''App Info'' of Visit Header is not implemented yet", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_delete_visit:
                Toast.makeText(getActivity(), "''Delete Visit'' is not fully implemented yet", Toast.LENGTH_SHORT).show();
                Fragment newVisFragment = fm.findFragmentByTag("new_visit");
                if (newVisFragment == null) {
                    Log.d(LOG_TAG, "newVisFragment == null");
                } else {
                    Log.d(LOG_TAG, "newVisFragment: " + newVisFragment.toString());
                    FragmentTransaction transaction = fm.beginTransaction();
                    // replace the fragment in the fragment container with the stored New Visit fragment
                    transaction.replace(R.id.fragment_container, newVisFragment);
                    // we are deleting this record, so do not put the present fragment on the backstack
                    transaction.commit();
                }

    //			DelProjectDialog delProjDlg = new DelProjectDialog();
    //			delProjDlg.show(fm, "frg_del_proj");
                return true;

            case R.id.action_visit_help:
                Toast.makeText(getActivity(), "''Visit Help'' is not implemented yet",
                        Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_settings:
                Toast.makeText(getActivity(), "''Settings'' of Visit Header is not implemented yet",
                        Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // if the activity was re-created (e.g. from a screen rotate)
        // restore the previous screen, remembered by onSaveInstanceState()
        // This is mostly needed in fixed-pane layouts
        if (savedInstanceState != null) {
            mResolvingError = savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
            mCurrentSubplot = savedInstanceState.getInt(ARG_SUBPLOT, 0);
            Log.d(LOG_TAG, "In onCreateView, about to retrieve mVisitId: " + mVisitId);
            mVisitId = savedInstanceState.getLong(ARG_VISIT_ID, 0);
            Log.d(LOG_TAG, "In onCreateView, retrieved mVisitId: " + mVisitId);
            mLocIsGood = savedInstanceState.getBoolean(ARG_LOC_GOOD_FLAG, false);
            mCurLocation = savedInstanceState.getParcelable(ARG_CUR_LOCATION);
            mPrevLocation = savedInstanceState.getParcelable(ARG_PREV_LOCATION);
            mCtPlaceholders = savedInstanceState.getLong(ARG_PH_COUNT);
            if (mLocIsGood) {
                mLatitude = savedInstanceState.getDouble(ARG_LOC_LATITUDE);
                mLongitude = savedInstanceState.getDouble(ARG_LOC_LONGITUDE);
                mAccuracy = savedInstanceState.getFloat(ARG_LOC_ACCURACY);
                mLocTime = savedInstanceState.getString(ARG_LOC_TIME);
            }
        } else {
            Log.d(LOG_TAG, "In onCreateView, savedInstanceState == null, mVisitId: " + mVisitId);
        }
        // inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_visit_header, container, false);
        mViewVisitName = (EditText) rootView.findViewById(R.id.txt_visit_name);
        mViewVisitName.setOnFocusChangeListener(this);
        registerForContextMenu(mViewVisitName); // enable long-press
        mViewVisitDate = (EditText) rootView.findViewById(R.id.txt_visit_date);
        mViewVisitDate.setText(mDateFormat.format(mCalendar.getTime()));
        mViewVisitDate.setOnClickListener(this);
        mViewVisitDate.setOnFocusChangeListener(this);
        mNamerSpinner = (Spinner) rootView.findViewById(R.id.sel_spp_namer_spinner);
        mNamerSpinner.setTag(Tags.SPINNER_FIRST_USE); // flag to catch and ignore erroneous first firing
        mNamerSpinner.setEnabled(false); // will enable when data ready
        mNamerAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, null,
                new String[] {"NamerName"},
                new int[] {android.R.id.text1}, 0);
        mNamerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mNamerSpinner.setAdapter(mNamerAdapter);
        mNamerSpinner.setOnItemSelectedListener(this);
        registerForContextMenu(mNamerSpinner); // enable long-press
        // also need click, if no names & therefore selection cannot be changed
//		mNamerSpinner.setOnFocusChangeListener(this); // does not work
        // use a TextView on top of the spinner, named "lbl_spp_namer_spinner_cover"
        mLblNewNamerSpinnerCover = (TextView) rootView.findViewById(R.id.lbl_spp_namer_spinner_cover);
        mLblNewNamerSpinnerCover.setOnClickListener(this);
        registerForContextMenu(mLblNewNamerSpinnerCover); // enable long-press
        // Prepare the loader. Either re-connect with an existing one or start a new one
        getLoaderManager().initLoader(Loaders.NAMERS, null, this);
        // in layout, TextView is in front of Spinner and takes precedence
        // for testing context menu, bring spinner to front so it receives clicks
//		mNamerSpinner.bringToFront();		
        mViewVisitScribe = (EditText) rootView.findViewById(R.id.txt_visit_scribe);
        mViewVisitScribe.setOnFocusChangeListener(this);
        registerForContextMenu(mViewVisitScribe); // enable long-press
        // set up the visit Location
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        mAccuracyTargetForVisitLoc = sharedPref.getFloat(Prefs.TARGET_ACCURACY_OF_VISIT_LOCATIONS, 7.0f);
        mViewVisitLocation = (EditText) rootView.findViewById(R.id.txt_visit_location);
        mViewVisitLocation.setOnFocusChangeListener(this);
        registerForContextMenu(mViewVisitLocation); // enable long-press
        // should the following go in onCreate() ?
        Log.d(LOG_TAG, "in CreateView about to call 'buildGoogleApiClient()'");
        buildGoogleApiClient();
        Log.d(LOG_TAG, "in CreateView returned from call to 'buildGoogleApiClient()'");
        mLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10000)        // 10 seconds, in milliseconds
            .setFastestInterval(1000); // 1 second, in milliseconds

        mViewAzimuth = (EditText) rootView.findViewById(R.id.txt_visit_azimuth);
        mViewAzimuth.setOnFocusChangeListener(this);
        registerForContextMenu(mViewAzimuth); // enable long-press
        mViewVisitNotes = (EditText) rootView.findViewById(R.id.txt_visit_notes);
        mViewVisitNotes.setOnFocusChangeListener(this);
        registerForContextMenu(mViewVisitNotes); // enable long-press
        // set click listener for the button in the view
        Button b = (Button) rootView.findViewById(R.id.visit_header_go_button);
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
        if (args != null) {
            if (mVisitId == 0) {
                // On return from Subplots container, this method can re-run before
                // SaveInstanceState and so retain arguments originally passed when created,
                // such as VisitId=0.
                // Do not allow that zero to overwrite a new (nonzero) Visit ID, or
                // it will flag to create a second copy of the same header.
                mVisitId = args.getLong(ARG_VISIT_ID, 0);
                mViewVisitName.requestFocus();
            }
        // also use for special arguments like screen layout
        }
        // fire off loaders that depend on layout being ready to receive results
        getLoaderManager().initLoader(Loaders.VISIT_TO_EDIT, null, this);
        getLoaderManager().initLoader(Loaders.EXISTING_VISITS, null, this);
        getLoaderManager().initLoader(Loaders.VISIT_PLACEHOLDERS_ENTERED, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
//	    do other setup here if needed
        // re-check this every time the fragment is entered
        getLoaderManager().restartLoader(Loaders.VISIT_PLACEHOLDERS_ENTERED, null, this);
        if (!mLocIsGood) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // if namer spinner has been changed
        if (mNamerSpinner.getId() != mNamerId) {
            // attempt to save record
            saveVisitRecord();
        }
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
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
            mButtonCallback = (OnButtonListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException (activity.toString() + " must implement OnButtonListener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save the current subplot arguments in case we need to re-create the fragment
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        outState.putInt(ARG_SUBPLOT, mCurrentSubplot);
        outState.putLong(ARG_VISIT_ID, mVisitId);
        outState.putBoolean(ARG_LOC_GOOD_FLAG, mLocIsGood);
        outState.putParcelable(ARG_CUR_LOCATION, mCurLocation);
        outState.putParcelable(ARG_PREV_LOCATION, mPrevLocation);
        outState.putLong(ARG_PH_COUNT, mCtPlaceholders);
        if (mLocIsGood) {
            outState.putDouble(ARG_LOC_LATITUDE, mLatitude);
            outState.putDouble(ARG_LOC_LONGITUDE, mLongitude);
            outState.putFloat(ARG_LOC_ACCURACY, mAccuracy);
            outState.putString(ARG_LOC_TIME, mLocTime);
        }
    }

    @Override
    public void onClick(View v) {
        int numUpdated;
        switch (v.getId()) {
        case R.id.txt_visit_date:
            fireOffDatePicker();
            break;
//		case R.id.sel_spp_namer_spinner: // does not receive onClick
        case R.id.lbl_spp_namer_spinner_cover:
//			AddSpeciesNamerDialog addSppNamerDlg = AddSpeciesNamerDialog.newInstance();
//			addSppNamerDlg.setTargetFragment(this, 0); // does not work
//			FragmentManager fm = getActivity().getSupportFragmentManager();
            Log.d(LOG_TAG, "Starting 'add new' for Namer from onClick of 'lbl_spp_namer_spinner_cover'");
//			addSppNamerDlg.show(fm, "sppNamerDialog_TextClick");
            if (mCtPlaceholders == 0) { // no placeholders entered for this Visit, allowed to edit Namer
                EditNamerDialog newNmrDlg = EditNamerDialog.newInstance(0);
                newNmrDlg.show(getFragmentManager(), "frg_new_namer_fromCover");
            } else {
                ConfigurableMsgDialog nmrLockDlg = ConfigurableMsgDialog.newInstance(
                        getActivity().getResources().getString(R.string.namer_locked_title),
                        getActivity().getResources().getString(R.string.namer_locked_message));
                nmrLockDlg.show(getFragmentManager(), "frg_err_namer_lock");
            }

            break;
        case R.id.visit_header_go_button:
            // create or update the Visit record in the database, if everything is valid
            mValidationLevel = Validation.CRITICAL; // save if possible, and announce anything invalid
            numUpdated = saveVisitRecord();
            if (numUpdated == 0) {
                Log.d(LOG_TAG, "Failed to save record in onClick; mValues: " + mValues.toString());
            } else {
                Log.d(LOG_TAG, "Saved record in onClick; mValues: " + mValues.toString());
            }
            if (numUpdated == 0) {
                break;
            }
            Log.d(LOG_TAG, "in onClick, about to do 'mButtonCallback.onVisitHeaderGoButtonClicked()'");
            mButtonCallback.onVisitHeaderGoButtonClicked(mVisitId);
            Log.d(LOG_TAG, "in onClick, completed 'mButtonCallback.onVisitHeaderGoButtonClicked()'");
            break;
        }
    }





    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.d(LOG_TAG, "Connection failed, will try resolution");
        if (mResolvingError) { // already working on this
            mResolveTryCount++;
            Log.d(LOG_TAG, "Currently working on failed connection, attempt " + mResolveTryCount);
            return;
        } else  if (connectionResult.hasResolution()) {
            Log.d(LOG_TAG, "Failed connection has resolution, about to try");
            try {
                mResolvingError = true;
                // Start an Activity that tries to resolve the error
                Log.d(LOG_TAG, "About to send Intent to resolve failed connection");
                connectionResult.startResolutionForResult(getActivity(), REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                mGoogleApiClient.connect(); // error with resolution intent, try again
            }
        } else {
            Log.d(LOG_TAG, "Connection, evidently no resolution, about to try to show error dialog");
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(),getActivity(),2000).show();
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
//        	showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
            Log.d(LOG_TAG, "Connection failed with code " + connectionResult.getErrorCode());
            mViewVisitLocation.setText("Location services connection failed with code "
                    + connectionResult.getErrorCode());
        }
    }
    
    // next sections build the error dialog

    // Creates a dialog for an error message
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        dialogFragment.setTargetFragment(this, -1);
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getActivity().getSupportFragmentManager(), "errordialog");
    }

    // Called from ErrorDialogFragment when the dialog is dismissed.
    public void onDialogDismissed() {
        mResolvingError = false;
        mResolveTryCount = 0;
    }

    // A fragment to display an error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            try { // is this what occasionally crashes?, e.g. on pause while error dialog is displayed
                ((DonateFragment)getTargetFragment()).onDialogDismissed();
            } catch (Exception e) {
                Log.d(LOG_TAG, "onDismiss error: " + e);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            try { // is this what occasionally crashes?, e.g. on pause while error dialog is displayed
                super.onSaveInstanceState(outState);
            } catch (Exception e) {
                Log.d(LOG_TAG, "onSaveInstanceState error: " + e);
            }
        }
    }

    // Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}