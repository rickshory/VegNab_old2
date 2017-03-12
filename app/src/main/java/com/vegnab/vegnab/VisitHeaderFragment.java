package com.vegnab.vegnab;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Tags;
import com.vegnab.vegnab.database.VNContract.Validation;
import com.vegnab.vegnab.database.VNContract.LDebug;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.Prefs;
import com.vegnab.vegnab.database.VNContract.VNPermissions;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VisitHeaderFragment extends Fragment implements OnClickListener,
        android.widget.AdapterView.OnItemSelectedListener,
        android.view.View.OnFocusChangeListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        ConnectionCallbacks, OnConnectionFailedListener,
        LocationListener {

    public interface EditVisitDialogListener {
        void onEditVisitComplete(VisitHeaderFragment visitHeaderFragment);
    }
    EditVisitDialogListener mEditVisitListener;

    private static final String LOG_TAG = VisitHeaderFragment.class.getSimpleName();
    private int mValidationLevel = Validation.SILENT;
    private static final int INTERNAL_GPS = 1;
    private static final int NETWORK = 2;
    private static final int MANUAL_ENTRY = 3;
    private int mLocationSource = INTERNAL_GPS; // default till changed
    protected GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean mLocIsGood = false, mLocIsSaved = false; // default until retrieved or established true
    private boolean mUserOKdAccuracy = false;
    private double mLatitude, mLongitude;
    private float mAccuracy, mAccuracyTargetForVisitLoc;
    private String mLocTime, mAccSource, mLocProvider;
    private Location mCurLocation, mLastLocation,
            mPrevLocation = new Location ("gps"); // use string constructor as default
    private boolean mHasPrevLoc = false, mHasLocPermission = true,
            mGotSomeLocation = false, mOtherVisLocsAvailable = true;
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
    HashMap<Long, String> mExistingLocationProviders = new HashMap<Long, String>();
    HashMap<Long, String> mExistingLocAccuracySources = new HashMap<Long, String>();
    private EditText mViewVisitName, mViewVisitDate, mViewVisitScribe, mViewVisitLocation,
            mViewAzimuth, mViewVisitNotes;
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
    final static String ARG_LOC_PREV_FLAG = "locPrevExists";
    final static String ARG_OTHER_VIS_LOCS_AVAILABLE = "locsAvailFromOtherVisits";
    final static String ARG_LOC_PERMISSION_FLAG = "hasLocPermission";
    final static String ARG_CUR_LOCATION = "curLocation";
    final static String ARG_PREV_LOCATION = "prevLocation";
    final static String ARG_LOC_LATITUDE = "locLatitude";
    final static String ARG_LOC_LONGITUDE = "locLongitude";
    final static String ARG_LOC_ACCURACY = "locAccuracy";
    final static String ARG_LOC_ACC_SOURCE = "locAccSource";
    final static String ARG_LOC_TIME = "locTimeStamp";
    final static String ARG_LOC_PROVIDER = "locProvider";
    final static String ARG_PH_COUNT = "phCount";

    int mCurrentSubplot = -1;
    OnButtonListener mButtonCallback; // declare the interface
    // declare that the container Activity must implement this interface
    public interface OnButtonListener {
        // methods that must be implemented in the container Activity
        void onVisitHeaderGoButtonClicked(long visitId);
    }
    OnSppLocationChange mSppLocChangeCallback; // declare the interface
    public interface OnSppLocationChange {
        // methods that must be implemented in the container Activity
        void onSppLocalChanged();
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
           if (LDebug.ON) Log.d(LOG_TAG, "(EditVisitDialogListener) getActivity()");
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

            case R.id.action_delete_visit:
                Toast.makeText(getActivity(), "''Delete Visit'' is not fully implemented yet", Toast.LENGTH_SHORT).show();
                Fragment newVisFragment = fm.findFragmentByTag("new_visit");
                if (newVisFragment == null) {
                   if (LDebug.ON) Log.d(LOG_TAG, "newVisFragment == null");
                } else {
                   if (LDebug.ON) Log.d(LOG_TAG, "newVisFragment: " + newVisFragment.toString());
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
           if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView, about to retrieve mVisitId: " + mVisitId);
            mVisitId = savedInstanceState.getLong(ARG_VISIT_ID, 0);
           if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView, retrieved mVisitId: " + mVisitId);
            mLocIsGood = savedInstanceState.getBoolean(ARG_LOC_GOOD_FLAG, false);
            mHasPrevLoc = savedInstanceState.getBoolean(ARG_LOC_PREV_FLAG, false);
            mOtherVisLocsAvailable = savedInstanceState.getBoolean(ARG_OTHER_VIS_LOCS_AVAILABLE, true);
            mHasLocPermission = savedInstanceState.getBoolean(ARG_LOC_PERMISSION_FLAG, true);
            mCurLocation = savedInstanceState.getParcelable(ARG_CUR_LOCATION);
            mPrevLocation = savedInstanceState.getParcelable(ARG_PREV_LOCATION);
            mCtPlaceholders = savedInstanceState.getLong(ARG_PH_COUNT);
            if (mLocIsGood) {
                mLatitude = savedInstanceState.getDouble(ARG_LOC_LATITUDE);
                mLongitude = savedInstanceState.getDouble(ARG_LOC_LONGITUDE);
                mAccuracy = savedInstanceState.getFloat(ARG_LOC_ACCURACY);
                mAccSource = savedInstanceState.getString(ARG_LOC_ACC_SOURCE);
                mLocTime = savedInstanceState.getString(ARG_LOC_TIME);
                mLocProvider = savedInstanceState.getString(ARG_LOC_PROVIDER);
            }
        } else {
           if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView, savedInstanceState == null, mVisitId: " + mVisitId);
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
       if (LDebug.ON) Log.d(LOG_TAG, "in CreateView about to call 'buildGoogleApiClient()'");
        buildGoogleApiClient();
       if (LDebug.ON) Log.d(LOG_TAG, "in CreateView returned from call to 'buildGoogleApiClient()'");
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
        // set click listener for the buttons in the view
        Button btGo = (Button) rootView.findViewById(R.id.visit_header_go_button);
        btGo.setOnClickListener(this);
        Button btOpt = (Button) rootView.findViewById(R.id.vis_hdr_opt_fields_button);
        btOpt.setOnClickListener(this);
        btOpt.setText(getActivity().getResources().getString(R.string.vis_hdr_opt_fields_button_show_msg));
        // if more, loop through all the child items of the ViewGroup rootView and
        // set the onclicklistener for all the Button instances found

        // initially hide the optional fields
        ViewGroup vgOptFlds = (ViewGroup) rootView.findViewById(R.id.vis_hdr_opt_fields_view_group);
        vgOptFlds.setVisibility(View.GONE);
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
                if (mVisitId == 0) {
                    // if it is really a new record
                    mViewVisitName.requestFocus();
                    // show the on-screen keyboard, ready to enter the new visit name
                    mViewVisitName.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(mViewVisitName, 0);
                        }
                    }, 50);
                }
            }
        // also use for special arguments like screen layout
        }
        // fire off loaders that depend on layout being ready to receive results
        getLoaderManager().initLoader(Loaders.VISIT_TO_EDIT, null, this);
        getLoaderManager().initLoader(Loaders.EXISTING_VISITS, null, this);
        getLoaderManager().initLoader(Loaders.VISIT_PLACEHOLDERS_ENTERED, null, this);
        getLoaderManager().initLoader(Loaders.VISITS_HAVING_LOC_AVAILABLE, null, this);
        // have these two loaders ready too, even though they don't involve the UI
        getLoaderManager().initLoader(Loaders.EXISTING_LOC_PROVIDERS, null, this);
        getLoaderManager().initLoader(Loaders.EXISTING_LOC_ACCURACY_SOURCES, null, this);
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
        try {
            mSppLocChangeCallback = (OnSppLocationChange) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException (activity.toString() + " must implement OnSppLocationChange");
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
        outState.putBoolean(ARG_LOC_PREV_FLAG, mHasPrevLoc);
        outState.putBoolean(ARG_OTHER_VIS_LOCS_AVAILABLE, mOtherVisLocsAvailable);
        outState.putBoolean(ARG_LOC_PERMISSION_FLAG, mHasLocPermission);
        outState.putParcelable(ARG_CUR_LOCATION, mCurLocation);
        outState.putParcelable(ARG_PREV_LOCATION, mPrevLocation);
        outState.putLong(ARG_PH_COUNT, mCtPlaceholders);
        if (mLocIsGood) {
            outState.putDouble(ARG_LOC_LATITUDE, mLatitude);
            outState.putDouble(ARG_LOC_LONGITUDE, mLongitude);
            outState.putFloat(ARG_LOC_ACCURACY, mAccuracy);
            outState.putString(ARG_LOC_ACC_SOURCE, mAccSource);
            outState.putString(ARG_LOC_TIME, mLocTime);
            outState.putString(ARG_LOC_PROVIDER, mLocProvider);
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
           if (LDebug.ON) Log.d(LOG_TAG, "Starting 'add new' for Namer from onClick of 'lbl_spp_namer_spinner_cover'");
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
               if (LDebug.ON) Log.d(LOG_TAG, "Failed to save record in onClick; mValues: " + mValues.toString());
            } else {
               if (LDebug.ON) Log.d(LOG_TAG, "Saved record in onClick; mValues: " + mValues.toString());
            }
            if (numUpdated == 0) {
                break;
            }
           if (LDebug.ON) Log.d(LOG_TAG, "in onClick, about to do 'mButtonCallback.onVisitHeaderGoButtonClicked()'");
            mButtonCallback.onVisitHeaderGoButtonClicked(mVisitId);
           if (LDebug.ON) Log.d(LOG_TAG, "in onClick, completed 'mButtonCallback.onVisitHeaderGoButtonClicked()'");
            break;

        case R.id.vis_hdr_opt_fields_button:
            String btnMsg;
            Button b = (Button) this.getView().findViewById(R.id.vis_hdr_opt_fields_button);
            // show or hide the optional fields
            ViewGroup vgOptFlds = (ViewGroup) this.getView().findViewById(R.id.vis_hdr_opt_fields_view_group);
            if (vgOptFlds.getVisibility() == View.VISIBLE) {
                vgOptFlds.setVisibility(View.GONE);
                btnMsg = getActivity().getResources().getString(R.string.vis_hdr_opt_fields_button_show_msg);
            } else { // optional fields are not visible, show them
                vgOptFlds.setVisibility(View.VISIBLE);
                btnMsg = getActivity().getResources().getString(R.string.vis_hdr_opt_fields_button_hide_msg);
            }
            b.setText(btnMsg);
            break;
        } // end of switch/case for onClick

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
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
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
           if (LDebug.ON) Log.d(LOG_TAG, "onCreateLoader, VISIT_PLACEHOLDERS_ENTERED");
            baseUri = ContentProvider_VegNab.SQL_URI;
            select = "SELECT COUNT(_id) AS PhCount FROM VegItems "
                    + "WHERE VisitID = ? AND SourceID = 2;";
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, new String[] { "" + mVisitId }, null);
            break;

        case Loaders.EXISTING_LOC_PROVIDERS:
            Uri mLocProvidersUri = Uri.withAppendedPath(
                    ContentProvider_VegNab.CONTENT_URI, "locationsources");
            cl = new CursorLoader(getActivity(), mLocProvidersUri,
                    null, select, null, null);
            break;

        case Loaders.EXISTING_LOC_ACCURACY_SOURCES:
            Uri mLocAccSourcesUri = Uri.withAppendedPath(
                    ContentProvider_VegNab.CONTENT_URI, "accuracysources");
            cl = new CursorLoader(getActivity(), mLocAccSourcesUri,
                    null, select, null, null);
            break;

        case Loaders.VISITS_HAVING_LOC_AVAILABLE:
            baseUri = ContentProvider_VegNab.SQL_URI;
            // only need a count, so only get ID column
            select = "SELECT Visits._id "
                    + "FROM Visits LEFT JOIN Locations ON Visits.RefLocID = Locations._id "
                    + "WHERE Visits.ShowOnMobile = 1 AND Visits.IsDeleted = 0 "
                    + "AND Locations.Latitude LIKE '%' AND Locations.Longitude LIKE '%' "
                    + "AND Visits._id != ?;";
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, new String[] { "" + mVisitId }, null);
            break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        // there will be various loaders, switch them out here
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor;
        mRowCt = c.getCount();
        switch (loader.getId()) {
        case Loaders.EXISTING_VISITS:
            mExistingVisitNames.clear();
            while (c.moveToNext()) {
//               if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, add to HashMap: " + c.getString(c.getColumnIndexOrThrow("VisitName")));
                mExistingVisitNames.put(c.getLong(c.getColumnIndexOrThrow("_id")),
                        c.getString(c.getColumnIndexOrThrow("VisitName")));
            }
//           if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, number of items in mExistingProjCodes: " + mExistingVisitNames.size());
//           if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, items in mExistingProjCodes: " + mExistingVisitNames.toString());
            break;

        case Loaders.VISIT_TO_EDIT:
//           if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, VISIT_TO_EDIT, records: " + c.getCount());
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
//           if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, VISIT_REF_LOCATION, records: " + c.getCount());
            if (c.moveToFirst()) {
                mLatitude = c.getDouble(c.getColumnIndexOrThrow("Latitude"));
                mLongitude = c.getDouble(c.getColumnIndexOrThrow("Longitude"));
                mAccuracy = c.getFloat(c.getColumnIndexOrThrow("Accuracy"));
                // mAccSource
                Context ct = getActivity();
                mViewVisitLocation.setText("" + mLatitude + ", " + mLongitude
                        + ((mAccuracy == 0.0) ? ""
                        : "\n" + ct.getResources().getString(R.string.loc_vw_acc) + " "
                        + mAccuracy + ct.getResources().getString(R.string.loc_vw_m)));
                mGotSomeLocation = true;

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
//           if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, VISIT_PLACEHOLDERS_ENTERED, records: " + mRowCt);
            if (c.moveToFirst()) {
                mCtPlaceholders = c.getLong(0); // only one field
//                mCtPlaceholders = c.getLong(c.getColumnIndexOrThrow("PhCount"));
            }
            setupNamerSpinner(); // this can run multiple times, latest will be most correct
            break;

        case Loaders.EXISTING_LOC_PROVIDERS:
            mExistingLocationProviders.clear();
            while (c.moveToNext()) {
                if (LDebug.ON) Log.d(LOG_TAG,
                        "onLoadFinished, add to HashMap mExistingLocationProviders: "
                        + c.getString(c.getColumnIndexOrThrow("LocationSource")));
                mExistingLocationProviders.put(c.getLong(c.getColumnIndexOrThrow("_id")),
                        c.getString(c.getColumnIndexOrThrow("LocationSource")));
            }
            if (LDebug.ON) Log.d(LOG_TAG,
                    "onLoadFinished, number of items in mExistingLocationProviders: "
                            + mExistingLocationProviders.size());
            if (LDebug.ON) Log.d(LOG_TAG,
                    "onLoadFinished, items in mExistingLocationProviders: "
                            + mExistingLocationProviders.toString());
            break;

        case Loaders.EXISTING_LOC_ACCURACY_SOURCES:
            mExistingLocAccuracySources.clear();
            while (c.moveToNext()) {
                if (LDebug.ON) Log.d(LOG_TAG,
                        "onLoadFinished, add to HashMap mExistingLocAccuracySources: "
                        + c.getString(c.getColumnIndexOrThrow("AccuracySource")));
                mExistingLocAccuracySources.put(c.getLong(c.getColumnIndexOrThrow("_id")),
                        c.getString(c.getColumnIndexOrThrow("AccuracySource")));
            }
            if (LDebug.ON) Log.d(LOG_TAG,
                    "onLoadFinished, number of items in mExistingLocAccuracySources: "
                            + mExistingLocAccuracySources.size());
            if (LDebug.ON) Log.d(LOG_TAG,
                    "onLoadFinished, items in mExistingLocAccuracySources: "
                            + mExistingLocAccuracySources.toString());
            break;

        case Loaders.VISITS_HAVING_LOC_AVAILABLE:
            if (c.getCount() == 0) {
                // no other visits available to copy location from
                mOtherVisLocsAvailable = false;
                // do we ever need to update this?
                // no, even if current visit saved, it is not available as an "other" visit
            }

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
//           if (LDebug.ON) Log.d(LOG_TAG, "Setting mNamerSpinner; testing index " + i);
            if (mNamerSpinner.getItemIdAtPosition(i) == mNamerId) {
//               if (LDebug.ON) Log.d(LOG_TAG, "Setting mNamerSpinner; found matching index " + i);
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
//           if (LDebug.ON) Log.d(LOG_TAG, "onLoaderReset, EXISTING_VISITS.");
//			don't need to do anything here, no cursor adapter
            break;
        case Loaders.VISIT_TO_EDIT:
//           if (LDebug.ON) Log.d(LOG_TAG, "onLoaderReset, VISIT_TO_EDIT.");
//			don't need to do anything here, no cursor adapter
            break;

        case Loaders.NAMERS:
            mNamerAdapter.swapCursor(null);
            break;

        case Loaders.VISIT_REF_LOCATION:
//           if (LDebug.ON) Log.d(LOG_TAG, "onLoaderReset, VISIT_REF_LOCATION.");
//			don't need to do anything here, no cursor adapter
            break;

        case Loaders.VISIT_PLACEHOLDERS_ENTERED:
            break;

        case Loaders.EXISTING_LOC_PROVIDERS:
            break;

        case Loaders.EXISTING_LOC_ACCURACY_SOURCES:
            break;

        case Loaders.VISITS_HAVING_LOC_AVAILABLE:
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
           if (LDebug.ON) Log.d(LOG_TAG, "Date error: " + e.toString());
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
           if (LDebug.ON) Log.d(LOG_TAG, "Azimuth is length " + stringAz.length());
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
           if (LDebug.ON) Log.d(LOG_TAG, "Failed validation in saveVisitRecord; mValues: " + mValues.toString());
            return numUpdated;
        }
        ContentResolver rs = getActivity().getContentResolver();
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        if (mVisitId == 0) { // new record
           if (LDebug.ON) Log.d(LOG_TAG, "saveVisitRecord; creating new record with mVisitId = " + mVisitId);
            // fill in fields the user never sees
            mValues.put("ProjID", sharedPref.getLong(Prefs.DEFAULT_PROJECT_ID, 0));
            mValues.put("PlotTypeID", sharedPref.getLong(Prefs.DEFAULT_PLOTTYPE_ID, 0));
            mValues.put("StartTime", mTimeFormat.format(new Date()));
            mValues.put("LastChanged", mTimeFormat.format(new Date()));
//			mValues.put("NamerID", mNamerId);
            // wait on 'RefLocID', location record cannot be created until the Visit record has an ID assigned
//			mValues.put("RefLocID", ); // save the Location to get this ID
//			mValues.put("RefLocIsGood", mLocIsGood ? 1 : 0);
            mValues.put("DeviceType", 2); // 1='unknown', 2='Android', this may be redundant, but
                // flags that this was explicitly set
            mValues.put("DeviceID", sharedPref.getString(Prefs.UNIQUE_DEVICE_ID, "")); // set on first app start
            mValues.put("DeviceIDSource", sharedPref.getString(Prefs.DEVICE_ID_SOURCE, ""));
            // don't actually need the following 6 as the fields have default values
            mValues.put("IsComplete", 0); // flag to sync to cloud storage, if subscribed; option to
                // automatically set following flag to 0 after sync
            mValues.put("ShowOnMobile", 1); // allow masking out, to reduce clutter
            mValues.put("Include", 1); // include in analysis, not used on mobile but here for completeness
            mValues.put("IsDeleted", 0); // don't allow user to actually delete a visit, just
                // flag it; this by hard experience
            mValues.put("NumAdditionalLocations", 0); // if additional locations are mapped, maintain the count
            mValues.put("AdditionalLocationsType", 1); // 1=points, 2=line, 3=polygon
            mUri = rs.insert(mVisitsUri, mValues);
           if (LDebug.ON) Log.d(LOG_TAG, "new record in saveVisitRecord; returned URI: " + mUri.toString());
            long newRecId = Long.parseLong(mUri.getLastPathSegment());
            if (newRecId < 1) { // returns -1 on error, e.g. if not valid to save because of
                // missing required field
               if (LDebug.ON) Log.d(LOG_TAG, "new record in saveVisitRecord has Id == " + newRecId + "); canceled");
                return 0;
            }
            mVisitId = newRecId;
            saveDefaultNamerId(mNamerId); // save the current Namer as the default for future Visits
            mNamerSpinner.setEnabled(false); // do not allow changing the Namer now that this Visit is saved
            mLblNewNamerSpinnerCover.setFocusableInTouchMode(false); // disable the Cover too
            getLoaderManager().restartLoader(Loaders.EXISTING_VISITS, null, this);

            mUri = ContentUris.withAppendedId(mVisitsUri, mVisitId);
           if (LDebug.ON) Log.d(LOG_TAG, "new record in saveVisitRecord; URI re-parsed: " + mUri.toString());
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putLong(Prefs.CURRENT_VISIT_ID, mVisitId);
            prefEditor.commit();
            if (saveVisitLoc() == 1) { // successfully created and saved location
                // update it in the Visit record
                mValues.clear();
                mValues.put("RefLocID", mLocId);
                mUri = ContentUris.withAppendedId(mVisitsUri, mVisitId);
                numUpdated = rs.update(mUri, mValues, null, null);
                if (numUpdated == 0) {
                    if (LDebug.ON) Log.d(LOG_TAG, "saveVisitRecord; new Visit record NOT updated with locID = " + mLocId);
                } else {
                    if (LDebug.ON) Log.d(LOG_TAG, "saveVisitRecord; new Visit record updated with locID = " + mLocId);
                }
            }
            numUpdated = 1;
        } else { // update the existing record
           if (LDebug.ON) Log.d(LOG_TAG, "saveVisitRecord; updating existing record with mVisitId = " + mVisitId);
            if (mCtPlaceholders != 0) { // if there are any Placeholders entered for this visit,
                // do not allow the Namer to be changed
                if (mValues.containsKey("NamerID")) {
                    mValues.remove("NamerID");
                }
            }
            mValues.put("LastChanged", mTimeFormat.format(new Date())); // update the last-changed time
            mUri = ContentUris.withAppendedId(mVisitsUri, mVisitId);
            numUpdated = rs.update(mUri, mValues, null, null);
           if (LDebug.ON) Log.d(LOG_TAG, "Updated record in saveVisitRecord; numUpdated: " + numUpdated);
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

    private int saveVisitLoc() {
        // return values:
        // 1 create succeeded
        // 2 update succeeded
        // 3 fail; location not good yet
        // 4 fail; visit not saved yet
        // 5 fail; could not create
        // 6 fail; could not update
        if (!mLocIsGood) return 3;
        if (mVisitId == 0) return 4;

        ContentResolver rs = getActivity().getContentResolver();
        ContentValues locValues = new ContentValues();
        locValues.clear();
        locValues.put("LocName", "Reference Location");
        locValues.put("SourceID", mLocationSource);
        locValues.put("VisitID", mVisitId);
        locValues.put("Latitude", mLatitude);
        locValues.put("Longitude", mLongitude);
        locValues.put("Accuracy", mAccuracy);
        locValues.put("TimeStamp", mLocTime);
        if (mLocId == 0) { // has not been saved to the database yet
            // add the location record
            mUri = rs.insert(mLocationsUri, locValues);
            long newLocID = Long.parseLong(mUri.getLastPathSegment());
            if (newLocID < 1) { // returns -1 on error, e.g. if not valid to save because of missing required field
                if (LDebug.ON) Log.d(LOG_TAG, "new Location record in saveVisitLoc has Id == " +
                        newLocID + "); canceled");
                return 5;
            } else {
                mLocId = newLocID;
                if (LDebug.ON) Log.d(LOG_TAG, "saveVisitLoc; new Location record created, locID = " + mLocId);
                return 1;
            }
        } else { // visit location exists, update
            mUri = ContentUris.withAppendedId(mLocationsUri, mLocId);
            int numUpdated = rs.update(mUri, locValues, null, null);
            if (numUpdated == 0) {
                if (LDebug.ON) Log.d(LOG_TAG, "saveVisitLoc; Location record NOT updated with locID = " + mLocId);
                return 6;
            } else {
                if (LDebug.ON) Log.d(LOG_TAG, "saveVisitLoc; Location record updated with locID = " + mLocId);
                return 2;
            }
        }
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
               if (LDebug.ON) Log.d(LOG_TAG, "Starting 'add new' for Namer from onItemSelect");
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
                   if (LDebug.ON) Log.d(LOG_TAG, "Failed to save record in onFocusChange; mValues: "
                            + mValues.toString());
                } else {
                   if (LDebug.ON) Log.d(LOG_TAG, "Saved record in onFocusChange; mValues: "
                            + mValues.toString());
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
            // can't delete Namer if has Placeholders
            // for now, always remove this menu item
            if (true) menu.removeItem(R.id.vis_hdr_namer_delete);
            // TODO make another way to delete unused Namers
            // TODO maybe through a drawer
            break;
        case R.id.lbl_spp_namer_spinner_cover:
            inflater.inflate(R.menu.context_visit_header_namer_cover, menu);
            break;
        case R.id.txt_visit_scribe:
            inflater.inflate(R.menu.context_visit_header_scribe, menu);
            break;
        case R.id.txt_visit_location:
            inflater.inflate(R.menu.context_visit_header_location, menu);
            // can't restore previous if no previous
            if (!mHasPrevLoc) menu.removeItem(R.id.vis_hdr_loc_restore_prev);
            // can't copy location from other visit if there are none
            if (!mOtherVisLocsAvailable) menu.removeItem(R.id.vis_hdr_loc_other_visit);
            // re-aquire only if already acquired
            if (!mLocIsGood) menu.removeItem(R.id.vis_hdr_loc_reacquire);
            // opt to accept accuracy only if in the process of acquiring
            if (mLocIsGood) menu.removeItem(R.id.vis_hdr_loc_accept);
            // need to request permission only if don't have permission
            if (mHasLocPermission) menu.removeItem(R.id.vis_hdr_loc_permission);
            /*
            other available IDs:
id/vis_hdr_loc_manual
id/vis_hdr_loc_details
id/vis_hdr_loc_help
"Enter manually" should only appear if not able to acquire anything at all, of if permission is turned off
"Details" gives timestamp of location, as well as how acquired; GPS or manually. If incomplete or none, say that.
"Help"
            */
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
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    if (info == null) {
       if (LDebug.ON) Log.d(LOG_TAG, "onContextItemSelected info is null");
    } else {
       if (LDebug.ON) Log.d(LOG_TAG, "onContextItemSelected info: " + info.toString());
    }
    Context c = getActivity();
    UnderConstrDialog notYetDlg = new UnderConstrDialog();
    HelpUnderConstrDialog hlpDlg = new HelpUnderConstrDialog();
    ConfigurableMsgDialog flexHlpDlg = new ConfigurableMsgDialog();
    String helpTitle, helpMessage;
        // get an Analytics event tracker
    Tracker headerContextTracker = ((VNApplication) getActivity().getApplication())
            .getTracker(VNApplication.TrackerName.APP_TRACKER);

    switch (item.getItemId()) {

    case R.id.vis_hdr_visname_help:
       if (LDebug.ON) Log.d(LOG_TAG, "'Visit Name Help' selected");
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
    // drop through to the same Edit dialog for the text view that covers the
    // Namer spinner to catch the first '(add new)' click
    case R.id.vis_hdr_namer_cover_edit:
        if (LDebug.ON) Log.d(LOG_TAG, "'Edit Namer' selected");
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
       if (LDebug.ON) Log.d(LOG_TAG, "'Delete Namer' selected");
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
       if (LDebug.ON) Log.d(LOG_TAG, "'Namer Help' selected");
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
       if (LDebug.ON) Log.d(LOG_TAG, "'Scribe Help' selected");
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
       if (LDebug.ON) Log.d(LOG_TAG, "'Restore Previous' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Restore Previous Location")
                .setValue(1)
                .build());
        // restore previous location, if there is one
        if (mHasPrevLoc) {
            if (mPrevLocation != null) {
                // for now, try to swap current with previous
                Boolean hadCurrentLoc = true;
                if (mCurLocation != null) {
                    Location tmpLoc = new Location("new");
                    tmpLoc.set(mCurLocation);
                } else { // should not have to create it here, but fail-safe
                    hadCurrentLoc = false;
                    mCurLocation = new Location("new");
                }
                mCurLocation.set(mPrevLocation);
                if (hadCurrentLoc) {
                    mPrevLocation.set(mCurLocation);
                }
            finalizeLocation();
            }
        }
        return true;

    case R.id.vis_hdr_loc_reacquire:
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Re-acquire Location")
                .setValue(1)
                .build());
        if (LDebug.ON) Log.d(LOG_TAG, "'Re-acquire' selected");
        // re-acquire location
        // copy current location to previous so can undo
        if (mCurLocation != null) {
            if (mPrevLocation == null) {
                mPrevLocation = new Location(mCurLocation);
            } else {
                mPrevLocation.set(mCurLocation);
            }
            mHasPrevLoc = true;
        }
        mLocIsGood = false;
        mViewVisitLocation.setText("");
        mGotSomeLocation = false;
        // may have to do this if retrieved pre-existing header record
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        return true;

    case R.id.vis_hdr_loc_accept:
       if (LDebug.ON) Log.d(LOG_TAG, "'Accept accuracy' selected");
        if (mLocIsGood) { // message that accuracy is already OK
            headerContextTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Visit Header Event")
                    .setAction("Context Menu")
                    .setLabel("Accept Location Accuracy, already good")
                    .setValue(1)
                    .build());

            helpTitle = c.getResources().getString(R.string.vis_hdr_loc_good_ok_title);
            if (mUserOKdAccuracy) {
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
        mUserOKdAccuracy = true;
        finalizeLocation(); // depends on mCurLocation, tested above
        helpTitle = c.getResources().getString(R.string.vis_hdr_loc_good_ack_title);
        helpMessage = c.getResources().getString(R.string.vis_hdr_loc_good_ack_text_pre)
                + " " + mAccuracy
                + c.getResources().getString(R.string.vis_hdr_loc_good_ack_text_post);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_loc_acc_accept");
        return true;

    case R.id.vis_hdr_loc_other_visit:
        if (LDebug.ON) Log.d(LOG_TAG, "'Location from other visit' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Location from other visit")
                .setValue(1)
                .build());
        // should just disappear the menu option if no previous visits yet
        // but this is here for fail safe
        if (!mOtherVisLocsAvailable) {
            Toast.makeText(getActivity(),
                    getActivity().getResources().getString(R.string.new_visit_no_visits_msg),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        {
            Bundle args = new Bundle();
            // send the current visit ID, so dialog can exclude that one from choices
            args.putLong(ARG_VISIT_ID, mVisitId);
            UsePrevVisitLocDialog  prevLocDlg = UsePrevVisitLocDialog.newInstance(args);
            prevLocDlg.show(getActivity().getSupportFragmentManager(), "frg_prev_vis_locs");
        }
        return true;

    case R.id.vis_hdr_loc_manual:
       if (LDebug.ON) Log.d(LOG_TAG, "'Enter manually' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Enter Location Manually")
                .setValue(1)
                .build());
        // enter location manually
        Bundle args = new Bundle();
        args.putString(LocManualEntryDialog.ARG_TOOLBAR_HEADER,
                c.getResources().getString(R.string.loc_manual_header));
        if (mGotSomeLocation) {
            // send args with keys from this class
            args.putDouble(ARG_LOC_LATITUDE, mLatitude);
            args.putDouble(ARG_LOC_LONGITUDE, mLongitude);
            args.putFloat(ARG_LOC_ACCURACY, mAccuracy);
        }
        LocManualEntryDialog locMnlDlg = LocManualEntryDialog.newInstance(args);
        locMnlDlg.show(getFragmentManager(), "frg_loc_manl_entry");
        return true;

    case R.id.vis_hdr_loc_permission:
        if (LDebug.ON) Log.d(LOG_TAG, "'Permission' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Location Permission dialog")
                .setValue(1)
                .build());
        // explain to user that app needs location permission, and run the dialog
        new askUserForLocPermission().execute();
        return true;

    case R.id.vis_hdr_loc_details:
        if (LDebug.ON) Log.d(LOG_TAG, "'Details' selected");
            headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Visit Header Event")
                .setAction("Context Menu")
                .setLabel("Show Location Details")
                .setValue(1)
                .build());
        // show location details
        helpTitle = c.getResources().getString(R.string.vis_hdr_loc_detail_title);
        if (!mLocIsGood) {
            helpMessage = c.getResources().getString(R.string.vis_hdr_loc_detail_notyet);
        } else {
            helpMessage = "" + mLatitude + ", " + mLongitude
                    + "\n" + c.getResources().getString(R.string.loc_vw_acc) + " "
                    + ((mAccuracy == 0.0) ? c.getResources().getString(R.string.loc_vw_not_knw)
                    : mAccuracy + c.getResources().getString(R.string.loc_vw_m))
                    + "\n" + c.getResources().getString(R.string.loc_vw_acq) + " " + mLocTime + ""
                    + "\n" + c.getResources().getString(R.string.loc_vw_from) + " " + mLocProvider;
        }
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_help_detail");
        return true;

    case R.id.vis_hdr_loc_help:
       if (LDebug.ON) Log.d(LOG_TAG, "'Location Help' selected");
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
       if (LDebug.ON) Log.d(LOG_TAG, "'Azimuth Help' selected");
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
       if (LDebug.ON) Log.d(LOG_TAG, "'Notes Help' selected");
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
       if (LDebug.ON) Log.d(LOG_TAG, "Connection failed, will try resolution");
        if (mResolvingError) { // already working on this
            mResolveTryCount++;
           if (LDebug.ON) Log.d(LOG_TAG, "Currently working on failed connection, attempt " + mResolveTryCount);
            return;
        } else  if (connectionResult.hasResolution()) {
           if (LDebug.ON) Log.d(LOG_TAG, "Failed connection has resolution, about to try");
            try {
                mResolvingError = true;
                // Start an Activity that tries to resolve the error
               if (LDebug.ON) Log.d(LOG_TAG, "About to send Intent to resolve failed connection");
                connectionResult.startResolutionForResult(getActivity(), REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                mGoogleApiClient.connect(); // error with resolution intent, try again
            }
        } else {
           if (LDebug.ON) Log.d(LOG_TAG, "Connection, evidently no resolution, about to try to show error dialog");
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(),getActivity(),2000).show();
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
//        	showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
           if (LDebug.ON) Log.d(LOG_TAG, "Connection failed with code " + connectionResult.getErrorCode());
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
                ((VisitHeaderFragment)getTargetFragment()).onDialogDismissed();
            } catch (Exception e) {
               if (LDebug.ON) Log.d(LOG_TAG, "onDismiss error: " + e);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            try { // is this what occasionally crashes?, e.g. on pause while error dialog is displayed
                super.onSaveInstanceState(outState);
            } catch (Exception e) {
               if (LDebug.ON) Log.d(LOG_TAG, "onSaveInstanceState error: " + e);
            }
        }
    }
    // Runs when a GoogleApiClient object successfully connects.
    @Override
    public void onConnected(Bundle connectionHint) {
        if (!mLocIsGood) {
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                mHasLocPermission = true;
                // in case it turns out that no fix is possible, try to get some sort of
                // previous location, no matter how poor
                if (mHasPrevLoc) { // skip this if we already have a previous location stored
                    // mPrevLocation holds the last good location, if requested to re-acquire, but
                    // then re-acquire fails
                } else {
                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                            mGoogleApiClient);
                    if (mLastLocation != null) {
                        mHasPrevLoc = true;
                        if (mPrevLocation == null) {
                            mPrevLocation = new Location (mLastLocation);
                        } else {
// null pointer exception appears to be fixed
                            mPrevLocation.set(mLastLocation);
                        }
                    }
                }
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                        mLocationRequest, this);
            } else { // don't yet have this permission
                mHasLocPermission = false;
                // decide whether to tap the user
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    new askUserForLocPermission().execute();
                    //
                } else { // send off the request for location permission
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            VNPermissions.REQUEST_ACCESS_FINE_LOCATION);
                    // response comes through onRequestPermissionsResult callback
                }
            }
        }
    }

    private class askUserForLocPermission extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            ConfigurableMsgDialog locReqDlg = ConfigurableMsgDialog.newInstance(
                    getActivity().getResources().getString(R.string.vis_hdr_loc_req_title),
                    getActivity().getResources().getString(R.string.vis_hdr_loc_req_msg));
            locReqDlg.show(getFragmentManager(), "frg_loc_req");
            return null;
        }

        protected void onPostExecute(Void result) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    VNPermissions.REQUEST_ACCESS_FINE_LOCATION);
            // response comes through onRequestPermissionsResult callback
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case VNPermissions.REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mHasLocPermission = true;
                    // permission was granted, yay! Do the
                    // location-related task you need to do.

                } else {
                    mHasLocPermission = false;
                    // permission denied, boo! Disable the
                    // functionality that depends on location permission.
                }
                return;
            }
            // other 'case' lines to check for other permissions this app might request
        }
    }

    // Called by Google Play services if the connection to GoogleApiClient drops because of an error.
    public void onDisconnected() {
       if (LDebug.ON) Log.d(LOG_TAG, "Disconnected");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
       if (LDebug.ON) Log.d(LOG_TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    // Builds a GoogleApiClient.
    protected synchronized void buildGoogleApiClient() {
        if (servicesAvailable()) {
            //  Uses the addApi() method to request the LocationServices API.
            // documented under FusedLocationProviderApi
            if (mGoogleApiClient == null){
                mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
            }
        }
    }

    private boolean servicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getActivity());
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(getActivity(), result,
                        VNPermissions.PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }

            return false;
        }
        return true;
    }

    @Override
    public void onLocationChanged(Location newLoc) {
        handleLocation(newLoc);
    }

    public void handleLocation(Location loc) {
        mCurLocation = new Location(loc); // hold the latest value
        mAccuracy = loc.getAccuracy();
        if (mAccuracy <= mAccuracyTargetForVisitLoc) {
            finalizeLocation();
        } else {
            long n = mCurLocation.getTime();
            mLocTime = mTimeFormat.format(new Date(n));
            mLatitude = loc.getLatitude();
            mLongitude = loc.getLongitude();
            mLocProvider = loc.getProvider();
            // should always have a numeric accuracy here, but test just in case
            Context c = getActivity();
            String s = "" + mLatitude + ", " + mLongitude
                + ((mAccuracy == 0.0) ? "" : "\n" + c.getResources().getString(R.string.loc_vw_acc)
                    + " " + mAccuracy + c.getResources().getString(R.string.loc_vw_m))
                + "\n" + c.getResources().getString(R.string.loc_vw_targacc)
                    + " " + mAccuracyTargetForVisitLoc + c.getResources().getString(R.string.loc_vw_m)
                + "\n" + c.getResources().getString(R.string.loc_vw_cont_acq) + "";
            mViewVisitLocation.setText(s);
            mGotSomeLocation = true;
        }
    }

    public void finalizeLocation() {
        if (mCurLocation == null) {
            return;
        }
        mLocIsGood = true;
        mLatitude = mCurLocation.getLatitude();
        mLongitude = mCurLocation.getLongitude();
        mAccuracy = mCurLocation.getAccuracy();
        mLocProvider = mCurLocation.getProvider();
        switch (mLocProvider) { // translate these into our format
            case LocationManager.GPS_PROVIDER:
                mLocProvider = "Internal GPS";
                break;
            case LocationManager.NETWORK_PROVIDER:
                mLocProvider = "Network";
                break;
            // let any new ones remain as-is
            // other option
            // mLocProvider = "Manual entry";
        }
        if (mUserOKdAccuracy) {
            mAccSource = "User accepted";
        } else {
            mAccSource = "Automatic";
        }
        // other option
//        mAccSource = "User supplied";

        long n = mCurLocation.getTime();
        mLocTime = mTimeFormat.format(new Date(n));
        if (LDebug.ON) Log.d(LOG_TAG, "Location time: " + mLocTime);
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            // overwrite the message
            Context c = getActivity();
            String s = "" + mLatitude + ", " + mLongitude
                    + ((mAccuracy == 0.0) ? ""
                    : "\n" + c.getResources().getString(R.string.loc_vw_acc) + " "
                    + mAccuracy + c.getResources().getString(R.string.loc_vw_m));
            mViewVisitLocation.setText(s);
            mGotSomeLocation = true;
        }
        // attempt to save the reference location for this visit
        int result = saveVisitLoc();
        if (result <= 2) { // successfully created or updated this location
            if (LDebug.ON) {
                if (result == 1) Log.d(LOG_TAG, "setLocation; Saved new Location");
                if (result == 2) Log.d(LOG_TAG, "setLocation; Updated existing Location");
            }
        } else {
            if (LDebug.ON) Log.d(LOG_TAG, "setLocation; Could not store Location; result: "  + result);
        }
        updateLocalSpecies();
    } // end of finalizeLocation

    public void updateLocalSpecies() {
        try { // if any error, fail silently
            RequestQueue queue = Volley.newRequestQueue(getActivity());
            String stringUrl = "http://maps.googleapis.com/maps/api/geocode/json?latlng="
                    + mLatitude + ","
                    + mLongitude + "&sensor=false";
            if (LDebug.ON) Log.d(LOG_TAG, "stringUrl: " + stringUrl);
            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.GET, stringUrl, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // get State and Country; kind of redundant but works for anywhere in the USA
                            String stState = "", stCountry = "";
    //                        if (LDebug.ON) Log.d(LOG_TAG, "response: " + response.toString());
                            try {
                                JSONArray resultsArray = (JSONArray) response.get("results");
                                for (int i=0; i < resultsArray.length(); i++) {
                                    JSONObject resultItem = resultsArray.getJSONObject(i);
    //                                if (LDebug.ON)
    //                                    Log.d(LOG_TAG, "resultItem " + i + ": " + resultItem.toString());
                                    JSONArray address_componentsArray = (JSONArray) resultItem.get("address_components");
                                    for (int j=0; j < address_componentsArray.length(); j++) {
                                        JSONObject address_componentsItem = address_componentsArray.getJSONObject(j);
    //                                    if (LDebug.ON)
    //                                        Log.d(LOG_TAG, "address_componentItem " + j + ": " + address_componentsItem.toString());
                                        JSONArray typesArray = (JSONArray) address_componentsItem.get("types");
                                        for (int k=0; k < typesArray.length(); k++) {
                                            String typeItem = typesArray.getString(k);
    //                                        if (LDebug.ON)
    //                                            Log.d(LOG_TAG, "typeItem " + k + ": " + typeItem);
                                            if (typeItem.equals("administrative_area_level_1")) {
    //                                            String shortName = address_componentsItem.getString("short_name");
    //                                            if (LDebug.ON)
    //                                                Log.d(LOG_TAG, "short_name is: " + shortName);
                                                stState = address_componentsItem.getString("short_name");
                                            }
                                            if (typeItem.equals("country")) {
    //                                            String shortName = address_componentsItem.getString("short_name");
    //                                            if (LDebug.ON)
    //                                                Log.d(LOG_TAG, "short_name is: " + shortName);
                                                stCountry = address_componentsItem.getString("short_name");
                                            }
                                        }
                                    }
                                }
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                                if (LDebug.ON) Log.d(LOG_TAG, "JSON error: " + ex.toString());
                                //
                            }
                            // adjust Greenland from Google format to NRCS
                            if (stCountry.equals("GL")) {
                                stState = stCountry;
                                stCountry = "DEN";
                            }
                            // adjust for Saint Pierre - Miquelon
                            if (stCountry.equals("SP")) {
                                stState = "SPM";
                                stCountry = "FRA";
                            }
                            // adjust for Puerto Rico and  U.S. Virgin Islands
                            if (stCountry.equals("PR") || stCountry.equals("VI")) {
                                stState = stCountry;
                                stCountry = "USA+";
                            }

                            // TODO: minor outlying islands

                            if (LDebug.ON) Log.d(LOG_TAG, "Country " + stCountry + "; State " + stState);
                            // if we got a State and Country
                            if ((stCountry.length() > 0) && (stState.length() > 0)) {
                                try {
                                    // adjust country format from Google to NRCS
                                    if (stCountry.equals("US")) stCountry = "USA";
                                    if (stCountry.equals("CA")) stCountry = "CAN";
                                    String localSppCrit = "%" + stCountry + " (%" + stState + "%)%"; // e.g. "%USA (%OR%)%"
                                    Boolean updateLocal = false;
                                    if (LDebug.ON) Log.d(LOG_TAG, "Country " + stCountry + "; State " + stState);
                                    // following line sometimes gives null pointer exception
                                    SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                                    SharedPreferences.Editor prefEditor;
        //                                String curLocSppCrit = sharedPref.getString(Prefs.LOCAL_SPP_CRIT, "")
                                    if (!(sharedPref.getString(Prefs.LOCAL_SPP_CRIT, "").equals(localSppCrit))) {
                                        prefEditor = sharedPref.edit();
                                        prefEditor.putString(Prefs.LOCAL_SPP_CRIT, localSppCrit);
                                        prefEditor.commit();
                                        updateLocal = true;
                                    }
                                    if (updateLocal) { // update the database
                                        mViewVisitLocation.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                mSppLocChangeCallback.onSppLocalChanged();
                                            }
                                        }, 50);
                                    }
                                } catch (Exception e) { // fail silently
                                    if (LDebug.ON) Log.d(LOG_TAG, "exception: " + e.getMessage());
                                }
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (LDebug.ON) Log.d(LOG_TAG, "That didn't work!");
                        }
                    });

            // Add the request to the RequestQueue.
            queue.add(jsObjRequest);
        } catch (Exception e) { // fail silently
            if (LDebug.ON) Log.d(LOG_TAG, "exception: " + e.getMessage());
        }
            /*
         finally {
            ;
        }*/
    }

    // if Google Play Services not available, would Location Services be?
    // requestSingleUpdate

    public void setLocation(Bundle args) {
        // used to set the location when e.g entered manually, or reset to previous
        // Provider (can be any string) is the one field required to create a Location
        mLocProvider = args.getString(ARG_LOC_PROVIDER);
        if (mCurLocation == null) {
            mCurLocation = new Location(mLocProvider);
        } else {
            mCurLocation.setProvider(mLocProvider);
        }
        // finalizeLocation will copy lat/lon/acc/time etc to global members
        mCurLocation.setLatitude(args.getDouble(ARG_LOC_LATITUDE));
        mCurLocation.setLongitude(args.getDouble(ARG_LOC_LONGITUDE));
        mCurLocation.setAccuracy(args.getFloat(ARG_LOC_ACCURACY));
        mCurLocation.setTime(System.currentTimeMillis());
        // set other globals directly here
        mAccSource = args.getString(ARG_LOC_ACC_SOURCE);
        finalizeLocation();
    }

    // Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}