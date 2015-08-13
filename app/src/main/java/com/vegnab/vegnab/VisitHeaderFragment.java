package com.vegnab.vegnab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Tags;
import com.vegnab.vegnab.database.VNContract.Validation;
import com.vegnab.vegnab.database.VegNabDbHelper;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.Prefs;

import android.annotation.SuppressLint;
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

public class VisitHeaderFragment extends Fragment implements OnClickListener,
        android.widget.AdapterView.OnItemSelectedListener,
        android.view.View.OnFocusChangeListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        ConnectionCallbacks, OnConnectionFailedListener,
        LocationListener {

    public interface EditVisitDialogListener {
        public void onEditVisitComplete(VisitHeaderFragment visitHeaderFragment);
    }
    EditVisitDialogListener mEditVisitListener;

    private static final String LOG_TAG = VisitHeaderFragment.class.getSimpleName();
    private int mValidationLevel = Validation.SILENT;
    private static final int INTERNAL_GPS = 1;
    private static final int NETWORK = 2;
    private static final int MANUAL_ENTRY = 3;
    private static final int USER_OKD_ACCURACY = 4;
    private int mLocationSource = INTERNAL_GPS; // default till changed
    protected GoogleApiClient mGoogleApiClient;
    // track the state of Google API Client, to isolate errors
    private static final int GAC_STATE_LOCATION = 1;
    private static final int GAC_STATE_DRIVE = 2;
    // we use LocationServices, and Drive, but not at the same time; start with LocationServices
    private int mGACState = GAC_STATE_LOCATION;
    private LocationRequest mLocationRequest;
    private boolean mLocIsGood = false, mLocIsSaved = false; // default until retrieved or established true
    private double mLatitude, mLongitude;
    private float mAccuracy, mAccuracyTargetForVisitLoc;
    private String mLocTime;
    private Location mCurLocation, mPrevLocation;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    private int mResolveTryCount = 0;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    long mVisitId = 0, mNamerId = 0, mLocId = 0; // zero default means new or not specified yet
    long mCtPlaceholders = -1; // count of Placeholders entered on this visit
    // if there are any, do not allow Namer to be changed because Placeholders belong to that specific Namer
    // -1 = not set, 0 = none, >0 = some
    Uri mUri;
    Uri mVisitsUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "visits");
    Uri mLocationsUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "locations");
    ContentValues mValues = new ContentValues();
    HashMap<Long, String> mExistingVisitNames = new HashMap<Long, String>();
    private EditText mViewVisitName, mViewVisitDate, mViewVisitScribe, mViewVisitLocation, mViewAzimuth, mViewVisitNotes;
    private Spinner mNamerSpinner;
    private TextView mLblNewNamerSpinnerCover;
    SimpleCursorAdapter mVisitAdapter, mNamerAdapter;
    SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private Calendar mCalendar = Calendar.getInstance();
    private DatePickerDialog.OnDateSetListener myDateListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                int dayOfMonth) {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, monthOfYear);
            mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            mViewVisitDate.setText(mDateFormat.format(mCalendar.getTime()));
        }
    };
    int mRowCt, mNamersCt = 0;
    final static String ARG_SUBPLOT = "subplot"; // dummy value, eventually get rid of this one
    final static String ARG_VISIT_ID = "visitId";
    final static String ARG_LOC_GOOD_FLAG = "locGood";
    final static String ARG_CUR_LOCATION = "curLocation";
    final static String ARG_PREV_LOCATION = "prevLocation";
    final static String ARG_LOC_LATITUDE = "locLatitude";
    final static String ARG_LOC_LONGITUDE = "locLongitude";
    final static String ARG_LOC_ACCURACY = "locAccuracy";
    final static String ARG_LOC_TIME = "locTimeStamp";
    final static String ARG_PH_COUNT = "phCount";

    int mCurrentSubplot = -1;
    OnButtonListener mButtonCallback; // declare the interface
    // declare that the container Activity must implement this interface
    public interface OnButtonListener {
        // methods that must be implemented in the container Activity
        public void onVisitHeaderGoButtonClicked(long visitId);
    }

    public static VisitHeaderFragment newInstance(Bundle args) {
        VisitHeaderFragment f = new VisitHeaderFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get a Tracker (should auto-report)
        ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        try {
            mEditVisitListener = (EditVisitDialogListener) getActivity();
            Log.d(LOG_TAG, "(EditVisitDialogListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement EditVisitDialogListener interface");
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
        case R.id.action_visit_info:
            Toast.makeText(getActivity(), "''Visit Details'' of Visit Header is not implemented yet", Toast.LENGTH_SHORT).show();
            return true;
        case R.id.action_export_visit:
            exportVisit();
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
            Toast.makeText(getActivity(), "''Visit Help'' is not implemented yet", Toast.LENGTH_SHORT).show();
            return true;
        case R.id.action_settings:
            Toast.makeText(getActivity(), "''Settings'' of Visit Header is not implemented yet", Toast.LENGTH_SHORT).show();
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
        switch (mGACState) {
        case GAC_STATE_LOCATION:
            if (!mLocIsGood) {
                mGoogleApiClient.connect();
            }
            break;

        case GAC_STATE_DRIVE:
            mGoogleApiClient.connect();
            break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
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

    private void fireOffDatePicker() {
        String s = mViewVisitDate.getText().toString();
        try { // if the EditText view contains a valid date
            mCalendar.setTime(mDateFormat.parse(s)); // use it
        } catch (java.text.ParseException e) { // otherwise
            mCalendar = Calendar.getInstance(); // use today's date
        }
        new DatePickerDialog(getActivity(), myDateListener,
                mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    public void saveDefaultNamerId(long id) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putLong(Prefs.DEFAULT_NAMER_ID, id);
        prefEditor.commit();
    }

    public void refreshNamerSpinner() {
        // when the referred Loader callback returns, will update the Namers spinner
        getLoaderManager().restartLoader(Loaders.NAMERS, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        switch (id) {

        case Loaders.VISIT_TO_EDIT:
            Uri oneVisUri = ContentUris.withAppendedId(
                            Uri.withAppendedPath(
                            ContentProvider_VegNab.CONTENT_URI, "visits"), mVisitId);
            cl = new CursorLoader(getActivity(), oneVisUri,
                    null, select, null, null);
            break;
        case Loaders.NAMERS:
            baseUri = ContentProvider_VegNab.SQL_URI;
            select = "SELECT _id, NamerName FROM Namers "
                    + "UNION SELECT 0, '(add new)' "
                    + "ORDER BY _id;";
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, null, null);
            break;
        case Loaders.EXISTING_VISITS:
            baseUri = ContentProvider_VegNab.SQL_URI;
            select = "SELECT _id, VisitName FROM Visits "
                    + "WHERE IsDeleted = 0 AND _id != ?;";
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, new String[] { "" + mVisitId }, null);
            break;

        case Loaders.VISIT_REF_LOCATION:
            Uri oneLocUri = ContentUris.withAppendedId(
                            Uri.withAppendedPath(
                            ContentProvider_VegNab.CONTENT_URI, "locations"), mLocId);
            cl = new CursorLoader(getActivity(), oneLocUri,
                    null, select, null, null);
            break;

        case Loaders.VISIT_PLACEHOLDERS_ENTERED:
            Log.d(LOG_TAG, "onCreateLoader, VISIT_PLACEHOLDERS_ENTERED");
            baseUri = ContentProvider_VegNab.SQL_URI;
            select = "SELECT COUNT(_id) AS PhCount FROM VegItems "
                    + "WHERE VisitID = ? AND SourceID = 2;";
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, new String[] { "" + mVisitId }, null);
            break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        // there will be various loaders, switch them out here
        mRowCt = c.getCount();
        switch (loader.getId()) {
        case Loaders.EXISTING_VISITS:
            mExistingVisitNames.clear();
            while (c.moveToNext()) {
//                Log.d(LOG_TAG, "onLoadFinished, add to HashMap: " + c.getString(c.getColumnIndexOrThrow("VisitName")));
                mExistingVisitNames.put(c.getLong(c.getColumnIndexOrThrow("_id")),
                        c.getString(c.getColumnIndexOrThrow("VisitName")));
            }
//            Log.d(LOG_TAG, "onLoadFinished, number of items in mExistingProjCodes: " + mExistingVisitNames.size());
//            Log.d(LOG_TAG, "onLoadFinished, items in mExistingProjCodes: " + mExistingVisitNames.toString());
            break;

        case Loaders.VISIT_TO_EDIT:
//            Log.d(LOG_TAG, "onLoadFinished, VISIT_TO_EDIT, records: " + c.getCount());
            if (c.moveToFirst()) {
                mViewVisitName.setText(c.getString(c.getColumnIndexOrThrow("VisitName")));
                mViewVisitDate.setText(c.getString(c.getColumnIndexOrThrow("VisitDate")));
                mNamerId = c.getLong(c.getColumnIndexOrThrow("NamerID"));
                mViewVisitScribe.setText(c.getString(c.getColumnIndexOrThrow("Scribe")));
                // write code to save/retrieve Locations
                mLocIsGood = (c.getInt(c.getColumnIndexOrThrow("RefLocIsGood")) != 0);
                mLocId = c.getLong(c.getColumnIndexOrThrow("RefLocID"));
                if (mLocIsGood) {
                    if (mGoogleApiClient.isConnected()) {
                        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                        mGoogleApiClient.disconnect();
                    }
                    // set a temporary "retrieving..." message
                    String msg = getActivity().getResources().getString(R.string.vis_hdr_loc_retrieving);
                    mViewVisitLocation.setText(msg);
                    // fetch the stored location
                    getLoaderManager().initLoader(Loaders.VISIT_REF_LOCATION, null, this);
                }
                // explicitly test null, to avoid spurious zeroes
                if (!(c.isNull(c.getColumnIndexOrThrow("Azimuth")))) {
                    mViewAzimuth.setText("" + c.getInt(c.getColumnIndexOrThrow("Azimuth")));
                }
                mViewVisitNotes.setText(c.getString(c.getColumnIndexOrThrow("VisitNotes")));
            }
            // if no record retrieved, recID and other defaults remain zero
            setupNamerSpinner(); // this can run multiple times, latest will be most correct
            break;

        case Loaders.VISIT_REF_LOCATION:
//            Log.d(LOG_TAG, "onLoadFinished, VISIT_REF_LOCATION, records: " + c.getCount());
            if (c.moveToFirst()) {
                mLatitude = c.getDouble(c.getColumnIndexOrThrow("Latitude"));
                mLongitude = c.getDouble(c.getColumnIndexOrThrow("Longitude"));
                mAccuracy = c.getFloat(c.getColumnIndexOrThrow("Accuracy"));
                mViewVisitLocation.setText("" + mLatitude + ", " + mLongitude
                        + "\naccuracy " + mAccuracy + "m");
            }
            break;

        case Loaders.NAMERS:
            // Swap the new cursor in.
            // The framework will take care of closing the old cursor once we return.
            mNamerAdapter.swapCursor(c);
            if (mRowCt > 0) {
                mNamersCt = mRowCt;
                setupNamerSpinner(); // this can run multiple times, latest will be most correct
            }
            break;

        case Loaders.VISIT_PLACEHOLDERS_ENTERED:
//            Log.d(LOG_TAG, "onLoadFinished, VISIT_PLACEHOLDERS_ENTERED, records: " + mRowCt);
            if (c.moveToFirst()) {
                mCtPlaceholders = c.getLong(0); // only one field
//                mCtPlaceholders = c.getLong(c.getColumnIndexOrThrow("PhCount"));
            }
            setupNamerSpinner(); // this can run multiple times, latest will be most correct
            break;
        }
    }

    public void setupNamerSpinner() {
        if (mVisitId == 0) { // new record
            // assumes there are no placeholders entered
            mNamerSpinner.setEnabled(true); // no placeholders means spinner can be activated
            setNamerSpinnerSelectionFromDefaultNamer();
        } else { // existing record
            // assumes record has a valid NamerId that corresponds to a saved Namer
            setNamerSpinnerSelection();
            if (mCtPlaceholders == 0) { // verified no placeholders, spinner active
                mNamerSpinner.setEnabled(true);
                mNamerSpinner.bringToFront();
            } else { // placeholders entered on visit (or not yet verified there are none)
                mNamerSpinner.setEnabled(false); // lock spinner to prevent Namer/Placeholder mismatch
                mLblNewNamerSpinnerCover.bringToFront(); // user sees Namer, but click brings up message
            }

        }
    }

    public void setNamerSpinnerSelectionFromDefaultNamer() {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        // if none yet, use _id = 0, generated in query as '(add new)'
        mNamerId = sharedPref.getLong(Prefs.DEFAULT_NAMER_ID, 0);
        setNamerSpinnerSelection();
        if (mNamerId == 0) {
            // user sees '(add new)', blank TextView receives click;
            mLblNewNamerSpinnerCover.bringToFront();
        } else {
            // user can operate the spinner
            mNamerSpinner.bringToFront();
        }
    }

    public void setNamerSpinnerSelection() {
        // set the current Namer to show in its spinner
        for (int i=0; i<mNamersCt; i++) {
//            Log.d(LOG_TAG, "Setting mNamerSpinner; testing index " + i);
            if (mNamerSpinner.getItemIdAtPosition(i) == mNamerId) {
//                Log.d(LOG_TAG, "Setting mNamerSpinner; found matching index " + i);
                mNamerSpinner.setSelection(i);
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed. Need to make sure it is no longer is use.
        switch (loader.getId()) {
        case Loaders.EXISTING_VISITS:
//            Log.d(LOG_TAG, "onLoaderReset, EXISTING_VISITS.");
//			don't need to do anything here, no cursor adapter
            break;
        case Loaders.VISIT_TO_EDIT:
//            Log.d(LOG_TAG, "onLoaderReset, VISIT_TO_EDIT.");
//			don't need to do anything here, no cursor adapter
            break;

        case Loaders.NAMERS:
            mNamerAdapter.swapCursor(null);
            break;

        case Loaders.VISIT_REF_LOCATION:
//            Log.d(LOG_TAG, "onLoaderReset, VISIT_REF_LOCATION.");
//			don't need to do anything here, no cursor adapter
            break;

        case Loaders.VISIT_PLACEHOLDERS_ENTERED:
            break;
        
        }
    }

    private boolean validateVisitHeader() {
        // validate all items on the screen the user can see
        Context c = getActivity();
        String stringProblem;
        String errTitle = c.getResources().getString(R.string.vis_hdr_validate_generic_title);
        ConfigurableMsgDialog flexErrDlg = new ConfigurableMsgDialog();
        mValues.clear(); // build up mValues while validating; if returns true all members are good
        String stringVisitName = mViewVisitName.getText().toString().trim();
        if (stringVisitName.length() == 0) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.vis_hdr_validate_name_none);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_visname_none");
                    mViewVisitName.requestFocus();
                }
            }
            return false;
        }
        if (!(stringVisitName.length() >= 2)) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.vis_hdr_validate_name_short);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_visname_short");
                    mViewVisitName.requestFocus();
                }
            }
            return false;
        }
        if (mExistingVisitNames.containsValue(stringVisitName)) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.vis_hdr_validate_name_dup);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_visname_duplicate");
                    mViewVisitName.requestFocus();
                }
            }
            return false;
        }
        // VisitName is OK, store it
        mValues.put("VisitName", stringVisitName);

        String stringVisitDate = mViewVisitDate.getText().toString().trim();
        if (stringVisitDate.length() == 0) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.vis_hdr_validate_date_none);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_visdate_none");
                    mViewVisitDate.requestFocus();
                }
            }
            return false;
        }
        /*
        try {
            Date date = mDateFormat.parse(stringVisitDate);
        } catch (ParseException e) {
            Log.d(LOG_TAG, "Date error: " + e.toString());
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.vis_hdr_validate_date_bad);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_visdate_bad");
                    mViewVisitDate.requestFocus();
                }
            }
            return false;
        }
        */
        mValues.put("VisitDate", stringVisitDate); // VisitDate is OK, store it

        if (mNamerId == 0) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.vis_hdr_validate_namer_none);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_namer_none");
                    mViewVisitDate.requestFocus();
                }
            }
            return false;
        }
        mValues.put("NamerID", mNamerId); // NamerID is OK, store it

        // Scribe is optional, put as-is or Null if missing
        String stringScribe = mViewVisitScribe.getText().toString().trim();
        if (stringScribe.length() == 0) {
            mValues.putNull("Scribe");
        } else {
            mValues.put("Scribe", stringScribe);
        }

        if (!mLocIsGood) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.vis_hdr_validate_loc_not_ready);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_loc_not_ready");
                }
            }
            return false;
        }
        // location is good, flag it
        mValues.put("RefLocIsGood", 1);


        // validate Azimuth
        String stringAz = mViewAzimuth.getText().toString().trim();
        if (stringAz.length() == 0) {
            mValues.putNull("Azimuth"); // null is valid
        } else {
            Log.d(LOG_TAG, "Azimuth is length " + stringAz.length());
            int Az = 0;
            try {
                Az = Integer.parseInt(stringAz);
                if ((Az < 0) || (Az > 360)) {
                    if (mValidationLevel > Validation.SILENT) {
                        stringProblem = c.getResources().getString(R.string.vis_hdr_validate_azimuth_bad);
                        if (mValidationLevel == Validation.QUIET) {
                            Toast.makeText(this.getActivity(),
                                    stringProblem,
                                    Toast.LENGTH_LONG).show();
                        }
                        if (mValidationLevel == Validation.CRITICAL) {
                            flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                            flexErrDlg.show(getFragmentManager(), "frg_err_azimuth_out_of_range");
                            mViewAzimuth.requestFocus();
                        }
                    }
                    return false;
                } else {
                    mValues.put("Azimuth", Az);
                }
            } catch(NumberFormatException e) {
                if (mValidationLevel > Validation.SILENT) {
                    stringProblem = c.getResources().getString(R.string.vis_hdr_validate_azimuth_bad);
                    if (mValidationLevel == Validation.QUIET) {
                        Toast.makeText(this.getActivity(),
                                stringProblem,
                                Toast.LENGTH_LONG).show();
                    }
                    if (mValidationLevel == Validation.CRITICAL) {
                        flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                        flexErrDlg.show(getFragmentManager(), "frg_err_azimuth_bad_number");
                        mViewAzimuth.requestFocus();
                    }
                }
                return false;
            }
        }

        // Notes is optional, put as-is or Null if missing
        String stringNotes = mViewVisitNotes.getText().toString().trim();
        if (stringNotes.length() == 0) {
            mValues.putNull("VisitNotes");
        } else {
            mValues.put("VisitNotes", stringNotes);
        }

        return true;
    }


    private int saveVisitRecord() {
        int numUpdated = 0;
        if (!validateVisitHeader()) {
            Log.d(LOG_TAG, "Failed validation in saveVisitRecord; mValues: " + mValues.toString());
            return numUpdated;
        }
        ContentResolver rs = getActivity().getContentResolver();
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        if (mVisitId == 0) { // new record
            Log.d(LOG_TAG, "saveVisitRecord; creating new record with mVisitId = " + mVisitId);
            // fill in fields the user never sees
            mValues.put("ProjID", sharedPref.getLong(Prefs.DEFAULT_PROJECT_ID, 0));
            mValues.put("PlotTypeID", sharedPref.getLong(Prefs.DEFAULT_PLOTTYPE_ID, 0));
            mValues.put("StartTime", mTimeFormat.format(new Date()));
            mValues.put("LastChanged", mTimeFormat.format(new Date()));
//			mValues.put("NamerID", mNamerId);
            // wait on 'RefLocID', location record cannot be created until the Visit record has an ID assigned
//			mValues.put("RefLocID", ); // save the Location to get this ID
//			mValues.put("RefLocIsGood", mLocIsGood ? 1 : 0);
            mValues.put("DeviceType", 2); // 1='unknown', 2='Android', this may be redundant, but flags that this was explicitly set
            mValues.put("DeviceID", sharedPref.getString(Prefs.UNIQUE_DEVICE_ID, "")); // set on first app start
            mValues.put("DeviceIDSource", sharedPref.getString(Prefs.DEVICE_ID_SOURCE, ""));
            // don't actually need the following 6 as the fields have default values
            mValues.put("IsComplete", 0); // flag to sync to cloud storage, if subscribed; option to automatically set following flag to 0 after sync
            mValues.put("ShowOnMobile", 1); // allow masking out, to reduce clutter
            mValues.put("Include", 1); // include in analysis, not used on mobile but here for completeness
            mValues.put("IsDeleted", 0); // don't allow user to actually delete a visit, just flag it; this by hard experience
            mValues.put("NumAdditionalLocations", 0); // if additional locations are mapped, maintain the count
            mValues.put("AdditionalLocationsType", 1); // 1=points, 2=line, 3=polygon
            mUri = rs.insert(mVisitsUri, mValues);
            Log.d(LOG_TAG, "new record in saveVisitRecord; returned URI: " + mUri.toString());
            long newRecId = Long.parseLong(mUri.getLastPathSegment());
            if (newRecId < 1) { // returns -1 on error, e.g. if not valid to save because of missing required field
                Log.d(LOG_TAG, "new record in saveVisitRecord has Id == " + newRecId + "); canceled");
                return 0;
            }
            mVisitId = newRecId;
            saveDefaultNamerId(mNamerId); // save the current Namer as the default for future Visits
            mNamerSpinner.setEnabled(false); // do not allow changing the Namer now that this Visit is saved
            mLblNewNamerSpinnerCover.setFocusableInTouchMode(false); // disable the Cover too
            getLoaderManager().restartLoader(Loaders.EXISTING_VISITS, null, this);

            mUri = ContentUris.withAppendedId(mVisitsUri, mVisitId);
            Log.d(LOG_TAG, "new record in saveVisitRecord; URI re-parsed: " + mUri.toString());
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putLong(Prefs.CURRENT_VISIT_ID, mVisitId);
            prefEditor.commit();
            if (mLocIsGood) { // add the location record
                mValues.clear();
                mValues.put("LocName", "Reference Location");
                mValues.put("SourceID", mLocationSource);
                mValues.put("VisitID", mVisitId);
                //mValues.put("SubplotID", 0); // N/A, for the whole site, not any subplot
                //mValues.put("ListingOrder", 0); // use the default=0
                mValues.put("Latitude", mLatitude);
                mValues.put("Longitude", mLongitude);
                mValues.put("TimeStamp", mLocTime);
                mValues.put("Accuracy", mAccuracy);
                mUri = rs.insert(mLocationsUri, mValues);
                long newLocID = Long.parseLong(mUri.getLastPathSegment());
                if (newLocID < 1) { // returns -1 on error, e.g. if not valid to save because of missing required field
                    Log.d(LOG_TAG, "new Location record in saveVisitRecord has Id == " + newLocID + "); canceled");
                } else {
                    mLocId = newLocID;
                    Log.d(LOG_TAG, "saveVisitRecord; new Location record created, locID = " + mLocId);
                    // update the Visit record to include the Location
                    mValues.clear();
                    mValues.put("RefLocID", mLocId);
                    mUri = ContentUris.withAppendedId(mVisitsUri, mVisitId);
                    numUpdated = rs.update(mUri, mValues, null, null);
                    if (numUpdated == 0) {
                        Log.d(LOG_TAG, "saveVisitRecord; new Visit record NOT updated with locID = " + mLocId);
                    } else {
                        Log.d(LOG_TAG, "saveVisitRecord; new Visit record updated with locID = " + mLocId);
                    }
                }
            }
            numUpdated = 1;
        } else { // update the existing record
            Log.d(LOG_TAG, "saveVisitRecord; updating existing record with mVisitId = " + mVisitId);
            if (mCtPlaceholders != 0) { // if there are any Placeholders entered for this visit,
                // do not allow the Namer to be changed
                if (mValues.containsKey("NamerID")) {
                    mValues.remove("NamerID");
                }
            }
            mValues.put("LastChanged", mTimeFormat.format(new Date())); // update the last-changed time
            mUri = ContentUris.withAppendedId(mVisitsUri, mVisitId);
            numUpdated = rs.update(mUri, mValues, null, null);
            Log.d(LOG_TAG, "Updated record in saveVisitRecord; numUpdated: " + numUpdated);
        }
        if (numUpdated > 0) {
            try {
                mEditVisitListener.onEditVisitComplete(VisitHeaderFragment.this);
                // sometimes this fails with null pointer exception because fragment is gone
            } catch (Exception e) {
                // ignore; fn is just to refresh the screen and that will happen on fragment rebuild
            }


        }
        return numUpdated;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        // 'parent' is the spinner
        // 'view' is one of the internal Android constants (e.g. text1=16908307, text2=16908308)
        //    in the item layout, unless set up otherwise
        // 'position' is the zero-based index in the list
        // 'id' is the (one-based) database record '_id' of the item
        // get the text by:
        //Cursor cur = (Cursor)mNamerAdapter.getItem(position);
        //String strSel = cur.getString(cur.getColumnIndex("NamerName"));
        //Log.d(LOG_TAG, strSel);
        // if spinner is filled by Content Provider, can't get text by:
        //String strSel = parent.getItemAtPosition(position).toString();
        // that returns something like below, which there is no way to get text out of:
        // "android.content.ContentResolver$CursorWrapperInner@42041b40"

        // sort out the spinners
        // can't use switch because not constants
        if (parent.getId() == mNamerSpinner.getId()) {
            // workaround for spinner firing when first set
            if(((String)parent.getTag()).equalsIgnoreCase(Tags.SPINNER_FIRST_USE)) {
                parent.setTag("");
                return;
            }
            mNamerId = id;
            if (mNamerId == 0) { // picked '(add new)'
                Log.d(LOG_TAG, "Starting 'add new' for Namer from onItemSelect");
//				AddSpeciesNamerDialog  addSppNamerDlg = AddSpeciesNamerDialog.newInstance();
//				FragmentManager fm = getActivity().getSupportFragmentManager();
//				addSppNamerDlg.show(fm, "sppNamerDialog_SpinnerSelect");
                EditNamerDialog newNmrDlg = EditNamerDialog.newInstance(0);
                newNmrDlg.show(getFragmentManager(), "frg_new_namer_fromSpinner");

            } else { // (mNamerId != 0)
                // save in app Preferences as the default Namer
                saveDefaultNamerId(mNamerId);
            }
            setNamerSpinnerSelectionFromDefaultNamer(); // in either case, reset selection
        }
        // write code for any other spinner(s) here
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
//        setNamerSpinnerSelectionFromDefaultNamer();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(!hasFocus) { // something lost focus
            mValues.clear();
            switch (v.getId()) {
            case R.id.txt_visit_name:
            case R.id.txt_visit_scribe:
            case R.id.txt_visit_azimuth:
            case R.id.txt_visit_notes:
                mValidationLevel = Validation.QUIET; // save if possible, but notify minimally
                int numUpdated = saveVisitRecord();
                if (numUpdated == 0) {
                    Log.d(LOG_TAG, "Failed to save record in onFocusChange; mValues: " + mValues.toString());
                } else {
                    Log.d(LOG_TAG, "Saved record in onFocusChange; mValues: " + mValues.toString());
                }
                break;
            }
        }
    }

    // create context menus
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
       ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        switch (v.getId()) {
        case R.id.txt_visit_name:
            inflater.inflate(R.menu.context_visit_header_visname, menu);
            break;
        case R.id.sel_spp_namer_spinner:
            inflater.inflate(R.menu.context_visit_header_namer, menu);
            break;
        case R.id.lbl_spp_namer_spinner_cover:
            inflater.inflate(R.menu.context_visit_header_namer_cover, menu);
            break;
        case R.id.txt_visit_scribe:
            inflater.inflate(R.menu.context_visit_header_scribe, menu);
            break;
        case R.id.txt_visit_location:
            inflater.inflate(R.menu.context_visit_header_location, menu);
            break;
        case R.id.txt_visit_azimuth:
            inflater.inflate(R.menu.context_visit_header_azimuth, menu);
            break;
        case R.id.txt_visit_notes:
            inflater.inflate(R.menu.context_visit_header_notes, menu);
            break;
        }
    }

    // This is executed when the user selects an option
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    if (info == null) {
        Log.d(LOG_TAG, "onContextItemSelected info is null");
    } else {
        Log.d(LOG_TAG, "onContextItemSelected info: " + info.toString());
    }
    Context c = getActivity();
    UnderConstrDialog notYetDlg = new UnderConstrDialog();
    HelpUnderConstrDialog hlpDlg = new HelpUnderConstrDialog();
    ConfigurableMsgDialog flexHlpDlg = new ConfigurableMsgDialog();
    String helpTitle, helpMessage;
        // get an Analytics event tracker
    Tracker headerContextTracker = ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);

    switch (item.getItemId()) {
    case R.id.vis_hdr_visname_help:
        Log.d(LOG_TAG, "'Visit Name Help' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Visit Name Help")
                .setValue(1)
                .build());
        // Visit Name help
        helpTitle = c.getResources().getString(R.string.vis_hdr_help_visname_title);
        helpMessage = c.getResources().getString(R.string.vis_hdr_help_visname_text);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_help_visname");
        return true;
    case R.id.vis_hdr_namer_edit:
        Log.d(LOG_TAG, "'Edit Namer' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Edit Namer")
                .setValue(1)
                .build());
        // edit Namer
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        long defaultNamerId = sharedPref.getLong(Prefs.DEFAULT_NAMER_ID, 0);
        if (defaultNamerId == 0) {
            return true;
        }
        EditNamerDialog editNmrDlg = EditNamerDialog.newInstance(defaultNamerId);
        editNmrDlg.show(getFragmentManager(), "frg_edit_namer");
        return true;
    case R.id.vis_hdr_namer_delete:
        Log.d(LOG_TAG, "'Delete Namer' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Delete Namer")
                .setValue(1)
                .build());
        // delete Namer
        DelNamerDialog delNamerDlg = new DelNamerDialog();
        delNamerDlg.show(getFragmentManager(), "frg_del_namer");
        return true;
    case R.id.vis_hdr_namer_help:
        // drop through to the same Help for the text view that covers the
        //  Namer spinner to catch the first '(add new)' click
    case R.id.vis_hdr_namer_cover_help:
        Log.d(LOG_TAG, "'Namer Help' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Namer Help")
                .setValue(1)
                .build());
        // Namer help
        helpTitle = c.getResources().getString(R.string.vis_hdr_help_namer_title);
        helpMessage = c.getResources().getString(R.string.vis_hdr_help_namer_text);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_help_namer");
        return true;
    case R.id.vis_hdr_scribe_help:
        Log.d(LOG_TAG, "'Scribe Help' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Scribe Help")
                .setValue(1)
                .build());
        // Scribe help
        helpTitle = c.getResources().getString(R.string.vis_hdr_help_scribe_title);
        helpMessage = c.getResources().getString(R.string.vis_hdr_help_scribe_text);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_help_scribe");
        return true;
    case R.id.vis_hdr_loc_restore_prev:
        Log.d(LOG_TAG, "'Restore Previous' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Restore Previous Location")
                .setValue(1)
                .build());
        // re-acquire location
        notYetDlg.show(getFragmentManager(), null);
        return true;
    case R.id.vis_hdr_loc_reacquire:
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Re-acquire Location")
                .setValue(1)
                .build());
        Log.d(LOG_TAG, "'Re-acquire' selected");
        // re-acquire location
        notYetDlg.show(getFragmentManager(), null);
        return true;
    case R.id.vis_hdr_loc_accept:
        Log.d(LOG_TAG, "'Accept accuracy' selected");
        if (mLocIsGood) { // message that accuracy is already OK
            headerContextTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Visit Header Event")
                    .setAction("Context Menu")
                    .setLabel("Accept Location Accuracy, already good")
                    .setValue(1)
                    .build());

            helpTitle = c.getResources().getString(R.string.vis_hdr_loc_good_ok_title);
            if (mLocationSource == USER_OKD_ACCURACY) {
                helpMessage = c.getResources().getString(R.string.vis_hdr_loc_good_prev_ok);
            } else {
                helpMessage = c.getResources().getString(R.string.vis_hdr_loc_good_ok_text_pre)
                    + " " + mAccuracy
                    + c.getResources().getString(R.string.vis_hdr_loc_good_ok_text_post);
            }
            flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
            flexHlpDlg.show(getFragmentManager(), "frg_loc_acc_already_ok");
            return true;
        }
        if (mCurLocation == null) { // no location at all yet
            headerContextTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Visit Header Event")
                    .setAction("Context Menu")
                    .setLabel("Accept Location Accuracy, but no location")
                    .setValue(1)
                    .build());

            helpTitle = c.getResources().getString(R.string.vis_hdr_validate_generic_title);
            helpMessage = c.getResources().getString(R.string.vis_hdr_loc_none);
            flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
            flexHlpDlg.show(getFragmentManager(), "frg_loc_err_none");
            return true;
        }
        // accept location even with poor accuracy
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Accept Location Accuracy, accepted")
                .setValue(1)
                .build());
        mLocationSource = USER_OKD_ACCURACY;
        finalizeLocation(); // depends on mCurLocation, tested above
        helpTitle = c.getResources().getString(R.string.vis_hdr_loc_good_ack_title);
        helpMessage = c.getResources().getString(R.string.vis_hdr_loc_good_ack_text_pre)
                + " " + mAccuracy
                + c.getResources().getString(R.string.vis_hdr_loc_good_ack_text_post);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_loc_acc_accept");
        return true;
    case R.id.vis_hdr_loc_manual:
        Log.d(LOG_TAG, "'Enter manually' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Enter Location Manually")
                .setValue(1)
                .build());
        // enter location manually
        notYetDlg.show(getFragmentManager(), null);
        return true;
    case R.id.vis_hdr_loc_details:
        Log.d(LOG_TAG, "'Details' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Show Location Details")
                .setValue(1)
                .build());
        // show location details
        notYetDlg.show(getFragmentManager(), null);
        return true;
    case R.id.vis_hdr_loc_help:
        Log.d(LOG_TAG, "'Location Help' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Location Help Requested")
                .setValue(1)
                .build());
        // Location help
        helpTitle = c.getResources().getString(R.string.vis_hdr_help_loc_title);
        helpMessage = c.getResources().getString(R.string.vis_hdr_help_loc_text);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_help_loc");
        return true;
    case R.id.vis_hdr_azimuth_help:
        Log.d(LOG_TAG, "'Azimuth Help' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Azimuth Help Requested")
                .setValue(1)
                .build());
        // Azimuth help
        helpTitle = c.getResources().getString(R.string.vis_hdr_help_azimuth_title);
        helpMessage = c.getResources().getString(R.string.vis_hdr_help_azimuth_text);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_help_azimuth");
        return true;
    case R.id.vis_hdr_notes_help:
        Log.d(LOG_TAG, "'Notes Help' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Notes Help Requested")
                .setValue(1)
                .build());
        // Notes help
        helpTitle = c.getResources().getString(R.string.vis_hdr_help_notes_title);
        helpMessage = c.getResources().getString(R.string.vis_hdr_help_notes_text);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_help_notes");
        return true;

    default:
        return super.onContextItemSelected(item);
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
            switch (mGACState) {
            case GAC_STATE_LOCATION:
                mViewVisitLocation.setText("Location services connection failed with code " + connectionResult.getErrorCode());
                break;
            case GAC_STATE_DRIVE: // make this show somewhere else
                mViewVisitLocation.setText("Google Drive connection failed with code " + connectionResult.getErrorCode());
                break;
            }            
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
                ((VisitHeaderFragment)getTargetFragment()).onDialogDismissed();
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
    // Runs when a GoogleApiClient object successfully connects.
    @Override
    public void onConnected(Bundle connectionHint) {
        switch (mGACState) {
        case GAC_STATE_LOCATION:
            if (!mLocIsGood) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
            break;

        case GAC_STATE_DRIVE: // this state is set by a menu action, for testing
            // test, create new contents resource
            Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(driveContentsCallback);

            break;
        }
    }
    
    private void exportVisit() {
        Toast.makeText(getActivity(), "''Export Visit'' of Visit Header is not fully implemented yet", Toast.LENGTH_SHORT).show();
        // test, create new contents resource
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        } catch (Exception e) {
            Log.d(LOG_TAG, "GoogleApiClient may not be connected yet, error code " + e);
        }
        mGACState = GAC_STATE_DRIVE;
        Log.d(LOG_TAG, "about to call 'buildGoogleApiClient()'");
        buildGoogleApiClient();
        Log.d(LOG_TAG, "about to do 'mGoogleApiClient.connect()'");
        mGoogleApiClient.connect();
        Log.d(LOG_TAG, "just after 'mGoogleApiClient.connect()'");
        // file is actually created by a callback, search in this code for:
        // ResultCallback<DriveContentsResult> driveContentsCallback
    }

    final private ResultCallback<DriveContentsResult> driveContentsCallback = new
            ResultCallback<DriveContentsResult>() {
        @Override
        public void onResult(DriveContentsResult result) {
            if (!result.getStatus().isSuccess()) {
                Log.d(LOG_TAG, "Error while trying to create new file contents");
//    			showMessage("Error while trying to create new file contents");
                Toast.makeText(getActivity(), "Error while trying to create new file contents", Toast.LENGTH_LONG).show();
                return;
            }
            final DriveContents driveContents = result.getDriveContents();
//            String appName = "VegNab Alpha Test";
            String appName = getActivity().getResources().getString(R.string.app_name);
            String visName = "" + mViewVisitName.getText().toString().trim();
            SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            final String fileName = appName + " " + ((visName == "" ? "" : visName + " "))
                    + fileNameFormat.format(new Date());
            final long visId = mVisitId;
            // perform i/o off the ui thread
            new Thread() {
                @SuppressLint("NewApi")
                @Override
                public void run() {
                                    // write content to DriveContents
                    OutputStream outputStream = driveContents.getOutputStream();
                    Writer writer = new OutputStreamWriter(outputStream);
                    try {
                        // \n writes only a '0x0a' character to the file (newline)
                        // 'normal' text files contain '0x0d' '0x0a' (carriage return and then newline)
                        writer.write("This is the output of a Visit's data.\r\n");
                        // temporarily comment out the following
//	    				if (visId == 0) {
//	    					writer.write("\nNo data yet for this Visit.\n");
//	    				} else {
                        if (true) { // for testing
                            writer.write("\r\nVisit ID = " + visId + "\r\n");
                            // test getting data from the database
                            VegNabDbHelper thdDb = new VegNabDbHelper(getActivity());
//	    					ContentResolver thdRs = getActivity().getContentResolver();
                            Cursor thdCs, thdSb, thdVg;
                            String sSQL;
                            // get the Visit Header information
                            sSQL = "SELECT Visits.VisitName, Visits.VisitDate, Projects.ProjCode, "
                                    + "PlotTypes.PlotTypeDescr, Visits.StartTime, Visits.LastChanged, "
                                    + "Namers.NamerName, Visits.Scribe, Locations.LocName, "
                                    + "Locations.VisitID, Locations.SubplotID, Locations.ListingOrder, "
                                    + "Locations.Latitude, Locations.Longitude, Locations.TimeStamp, "
                                    + "Locations.Accuracy, Locations.Altitude, LocationSources.LocationSource, "
                                    + "Visits.Azimuth, Visits.VisitNotes, Visits.DeviceType, "
                                    + "Visits.DeviceID, Visits.DeviceIDSource, Visits.IsComplete, "
                                    + "Visits.ShowOnMobile, Visits.Include, Visits.IsDeleted, "
                                    + "Visits.NumAdditionalLocations, Visits.AdditionalLocationsType, "
                                    + "Visits.AdditionalLocationSelected "
                                    + "FROM ((((Visits LEFT JOIN Projects "
                                    + "ON Visits.ProjID = Projects._id) "
                                    + "LEFT JOIN PlotTypes ON Visits.PlotTypeID = PlotTypes._id) "
                                    + "LEFT JOIN Namers ON Visits.NamerID = Namers._id) "
                                    + "LEFT JOIN Locations ON Visits.RefLocID = Locations._id) "
                                    + "LEFT JOIN LocationSources ON Locations.SourceID = LocationSources._id "
                                    + "WHERE Visits._id = " + visId + ";";
                            thdCs = thdDb.getReadableDatabase().rawQuery(sSQL, null);
                            int numCols = thdCs.getColumnCount();
                            while (thdCs.moveToNext()) {
                                for (int i=0; i<numCols; i++) {
                                    writer.write(thdCs.getColumnName(i) + "\t");
                                    try {
//	    								writer.write(thdCs.getType(i) + "\r\n");
                                        writer.write(thdCs.getString(i) + "\r\n");
                                    } catch (Exception e) {
                                        writer.write("\r\n");
                                    }
                                }
//	    						Log.d(LOG_TAG, "wrote a record");
                            }
                            Log.d(LOG_TAG, "cursor done");
                            thdCs.close();
                            Log.d(LOG_TAG, "cursor closed");
                            // get the Subplots for this Visit
                            long sbId;
                            String sbName, spCode, spDescr, spParams;
                            sSQL = "SELECT Visits._id, PlotTypes.PlotTypeDescr, PlotTypes.Code, "
                                    + "SubplotTypes.[_id] AS SubplotTypeId, "
                                    + "SubplotTypes.SubplotDescription, SubplotTypes.OrderDone, "
                                    + "SubplotTypes.PresenceOnly, SubplotTypes.HasNested, "
                                    + "SubplotTypes.SubPlotAngle, SubplotTypes.XOffset, SubplotTypes.YOffset, "
                                    + "SubplotTypes.SbWidth, SubplotTypes.SbLength, SubplotTypes.SbShape, "
                                    + "SubplotTypes.NestParam1, SubplotTypes.NestParam2, "
                                    + "SubplotTypes.NestParam3, SubplotTypes.NestParam4 "
                                    + "FROM (Visits LEFT JOIN PlotTypes ON Visits.PlotTypeID = PlotTypes._id) "
                                    + "LEFT JOIN SubplotTypes ON PlotTypes._id = SubplotTypes.PlotTypeID "
                                    + "WHERE (((Visits._id)=" + visId + ")) "
                                    + "ORDER BY SubplotTypes.OrderDone;";
                            thdSb = thdDb.getReadableDatabase().rawQuery(sSQL, null);
                            while (thdSb.moveToNext()) {
                                sbName = thdSb.getString(thdSb.getColumnIndexOrThrow("SubplotDescription"));
                                sbId = thdSb.getLong(thdSb.getColumnIndexOrThrow("SubplotTypeId"));
                                writer.write("\r\n" + sbName + "\r\n");
                                // get the data for each subplot
                                sSQL = "SELECT VegItems._id, VegItems.VisitID, VegItems.SubPlotID, "
                                        + "VegItems.OrigCode, VegItems.OrigDescr, VegItems.Height, VegItems.Cover, "
                                        + "VegItems.Presence, VegItems.IdLevelID, "
                                        + "VegItems.TimeCreated, VegItems.TimeLastChanged FROM VegItems "
                                        + "WHERE (((VegItems.VisitID)=" + visId + ") "
                                        + "AND ((VegItems.SubPlotID)=" + sbId + ")) "
                                        + "ORDER BY VegItems.TimeLastChanged DESC;";
                                thdVg = thdDb.getReadableDatabase().rawQuery(sSQL, null);
                                while (thdVg.moveToNext()) {
                                    spCode = thdVg.getString(thdVg.getColumnIndexOrThrow("OrigCode"));
                                    spDescr = thdVg.getString(thdVg.getColumnIndexOrThrow("OrigDescr"));
                                    if (thdVg.isNull(thdVg.getColumnIndexOrThrow("Presence"))) {
                                        // we should have Height and Cover
                                        spParams = "\t\t" + thdVg.getString(thdVg.getColumnIndexOrThrow("Height")) + "cm, "
                                                + thdVg.getString(thdVg.getColumnIndexOrThrow("Cover")) + "%";
                                    } else {
                                        // we should have Presence = 1 (true) or 0 (false)
                                        spParams = "\t\t"
                                                + ((thdVg.getInt(thdVg.getColumnIndexOrThrow("Presence")) == 0)
                                                ? "Absent" : "Present");
                                    }
                                    writer.write("\t" + spCode + ": " + spDescr + "\r\n");
                                    writer.write(spParams + "\r\n");
                                }
                                thdVg.close();
                            }
                            thdSb.close();
                            thdDb.close();
                            Log.d(LOG_TAG, "database closed");
                        }
                        writer.close();
                    } catch (IOException e) {
                        Log.d(LOG_TAG, "Error writing file: " + e.getMessage());
                    }
                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(fileName + ".txt")
                        .setMimeType("text/plain")
                        .setStarred(true).build();

                    // create file on root folder
                    Drive.DriveApi.getRootFolder(mGoogleApiClient)
                        .createFile(mGoogleApiClient, changeSet, driveContents)
                        .setResultCallback(fileCallback);
                }
        }.start();
        }
    };
    
    final private ResultCallback<DriveFileResult> fileCallback = new
            ResultCallback<DriveFileResult> () {
        @Override
        public void onResult(DriveFileResult result) {
            if (!result.getStatus().isSuccess()) {
                Log.d(LOG_TAG, "Error trying to create the file");
//    			showMessage("Error while trying to create new file contents");
                Toast.makeText(getActivity(), "Error trying to create the file", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(getActivity(), "Created file, content: " + result.getDriveFile().getDriveId(), Toast.LENGTH_LONG).show();
            mGACState = GAC_STATE_LOCATION;
            Log.d(LOG_TAG, "about to call 'buildGoogleApiClient()' to change back to Location");
            buildGoogleApiClient();
            Log.d(LOG_TAG, "about to do 'mGoogleApiClient.connect()'");
            mGoogleApiClient.connect();
        }
    };

    // Called by Google Play services if the connection to GoogleApiClient drops because of an error.

    public void onDisconnected() {
        Log.d(LOG_TAG, "Disconnected");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.d(LOG_TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    // Builds a GoogleApiClient.
    protected synchronized void buildGoogleApiClient() {
        try {
            mGoogleApiClient.disconnect();
        } catch (NullPointerException e) {
            Log.d(LOG_TAG, "'mGoogleApiClient' is still null");
        }
        if (servicesAvailable()) {
            // for testing, separate the states to isolate errors
            switch (mGACState) {
            case GAC_STATE_LOCATION:
                //  Uses the addApi() method to request the LocationServices API.
                // documented under FusedLocationProviderApi
                mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
                break;

            case GAC_STATE_DRIVE: // testing this
                mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
                break;
            }
        }
    }
    
    private boolean servicesAvailable() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());

        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(), 0).show();
            return false;
        }
    }

    @Override
    public void onLocationChanged(Location newLoc) {
        handleLocation(newLoc);
    }

    public void handleLocation(Location loc) {
        String s;
        mCurLocation = new Location(loc); // hold the latest value
        mAccuracy = loc.getAccuracy();
        if (mAccuracy <= mAccuracyTargetForVisitLoc) {
            finalizeLocation();
        } else {
            long n = mCurLocation.getTime();
            mLocTime = mTimeFormat.format(new Date(n));
            mLatitude = loc.getLatitude();
            mLongitude = loc.getLongitude();
            s = "" + mLatitude + ", " + mLongitude
                + "\naccuracy " + mAccuracy + "m"
                + "\ntarget accuracy " + mAccuracyTargetForVisitLoc + "m"
                + "\ncontinuing to acquire";
            mViewVisitLocation.setText(s);
        }
    }

    public void finalizeLocation() {
        String s;
        if (mCurLocation == null) {
            return;
        }
        mLocIsGood = true;
        mLatitude = mCurLocation.getLatitude();
        mLongitude = mCurLocation.getLongitude();
        mAccuracy = mCurLocation.getAccuracy();
        long n = mCurLocation.getTime();
        mLocTime = mTimeFormat.format(new Date(n));
        Log.d(LOG_TAG, "Location time: " + mLocTime);
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            // overwrite the message
            s = "" + mLatitude + ", " + mLongitude
                    + "\naccuracy " + mAccuracy + "m";
            mViewVisitLocation.setText(s);
        }
    }

    // if Google Play Services not available, would Location Services be?
    // requestSingleUpdate


    // Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}