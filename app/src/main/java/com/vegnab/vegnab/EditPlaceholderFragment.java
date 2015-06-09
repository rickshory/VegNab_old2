package com.vegnab.vegnab;

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
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.Prefs;
import com.vegnab.vegnab.database.VNContract.Tags;
import com.vegnab.vegnab.database.VNContract.Validation;
import com.vegnab.vegnab.database.VNContract.VNRegex;
import com.vegnab.vegnab.database.VegNabDbHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public class EditPlaceholderFragment extends Fragment implements OnClickListener,
        View.OnFocusChangeListener,
        LoaderManager.LoaderCallbacks<Cursor> {

//    public interface EditPlaceholderDialogListener {
//        public void onEditPlaceholderComplete(EditPlaceholderFragment visitHeaderFragment);
//    }
//    EditPlaceholderDialogListener mEditPlaceholderListener;

    private static final String LOG_TAG = EditPlaceholderFragment.class.getSimpleName();

    private int mValidationLevel = Validation.SILENT;

    private double mLatitude, mLongitude;
    private String mLocTime;
    private Location mCurLocation, mPrevLocation;

    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    // explicitly handle all fields; some API versions have bugs that lose cursors on orientation change, etc.
    // zero and null defaults means new or not specified yet
    long mPlaceholderId = 0, mPhProjId = 0, mPhVisitId = 0, mPhLocId = 0, mPhNamerId = 0;
    String mPlaceholderCode = null, mPlaceholderDescription = null, mPlaceholderHabitat = null,
            mPlaceholderLabelNumber = null, mPhVisitName = null, mPhNamerName = null,
            mPhScribe = null, mPhLocText = null;
    Boolean mCodeWasShortened = false;
    HashSet<String> mExistingPlaceholderCodes = new HashSet<String>();
    HashSet<String> mPreviouslyEnteredHabitats = new HashSet<String>();

    Uri mUri;
    Uri mPlaceholdersUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "placeholders");
    Uri mLocationsUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "locations");
    ContentValues mValues = new ContentValues();

    private EditText mViewPlaceholderCode, mViewPlaceholderDescription,
            mViewPlaceholderHabitat, mViewPlaceholderIdentifier;

//	SimpleCursorAdapter mVisitAdapter, mNamerAdapter;
    SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    int mRowCt;
    // explicitly save/retrieve all these through Bundles, some versions have bugs that lose cursor
    final static String ARG_PLACEHOLDER_ID = "placeholderId";
    final static String ARG_PLACEHOLDER_CODE = "placeholderCode";
    final static String ARG_CODE_WAS_SHORTENED = "phCodeShortened";
    final static String ARG_PLACEHOLDER_DESCRIPTION = "placeholderDescription";
    final static String ARG_PLACEHOLDER_HABITAT = "placeholderHabitat";
    final static String ARG_PLACEHOLDER_LABELNUMBER = "placeholderLabelnumber";
    final static String ARG_PH_PROJID = "phProjId";
    final static String ARG_PH_VISITID = "phVisitId";
    final static String ARG_PH_VISIT_NAME = "phVisitName";
    final static String ARG_PH_LOCID = "phLocId";
    final static String ARG_PH_LOC_TEXT = "phLocText";
    final static String ARG_PH_NAMERID = "phNamerId";
    final static String ARG_PH_NAMER_NAME = "phNamerName";
    final static String ARG_PH_SCRIBE = "phScribe";
    final static String ARG_PLACEHOLDER_TIME = "phTimeStamp";

    OnButtonListener mButtonCallback; // declare the interface
    // declare that the container Activity must implement this interface
    public interface OnButtonListener {
        // methods that must be implemented in the container Activity
        public void onPlaceholderSaveButtonClicked(Bundle args);
    }

    public static EditPlaceholderFragment newInstance(Bundle args) {
        EditPlaceholderFragment f = new EditPlaceholderFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get a Tracker (should auto-report)
        ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
//        try {
//            mEditPlaceholderListener = (EditPlaceholderDialogListener) getActivity();
//            Log.d(LOG_TAG, "(EditPlaceholderDialogListener) getActivity()");
//        } catch (ClassCastException e) {
//            throw new ClassCastException("Main Activity must implement EditPlaceholderDialogListener interface");
//        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.placeholder, menu);
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
        case R.id.action_ph_photo:
            Toast.makeText(getActivity(), "''Take Photo'' of Placeholder is not implemented yet", Toast.LENGTH_SHORT).show();
            return true;

        case R.id.action_ph_details:
            Toast.makeText(getActivity(), "''Details'' of Placeholder is not implemented yet", Toast.LENGTH_SHORT).show();
            return true;

        case R.id.action_ph_help:
            Toast.makeText(getActivity(), "''Help'' of Placeholder is not implemented yet", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // any Placeholder worked on here will always belong to the default project, namer, and visit
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        mPhProjId = sharedPref.getLong(Prefs.DEFAULT_PROJECT_ID, 0);
        mPhNamerId = sharedPref.getLong(Prefs.DEFAULT_NAMER_ID, 0);
        mPhVisitId = sharedPref.getLong(Prefs.CURRENT_VISIT_ID, 0);
        // if the activity was re-created (e.g. from a screen rotate)
        // restore the previous screen, remembered by onSaveInstanceState()
        // This is mostly needed in fixed-pane layouts
        if (savedInstanceState != null) {
            Log.d(LOG_TAG, "In onCreateView, about to retrieve mPlaceholderId: " + mPlaceholderId);
            mPlaceholderId = savedInstanceState.getLong(ARG_PLACEHOLDER_ID, 0);
            mPlaceholderCode = savedInstanceState.getString(ARG_PLACEHOLDER_CODE);
            mCodeWasShortened = savedInstanceState.getBoolean(ARG_CODE_WAS_SHORTENED, false);
            mPlaceholderDescription = savedInstanceState.getString(ARG_PLACEHOLDER_DESCRIPTION);
            mPlaceholderHabitat = savedInstanceState.getString(ARG_PLACEHOLDER_HABITAT);
            mPlaceholderLabelNumber = savedInstanceState.getString(ARG_PLACEHOLDER_LABELNUMBER);
//			mPhProjId = savedInstanceState.getLong(ARG_PH_PROJID, 0);
//			mPhVisitId = savedInstanceState.getLong(ARG_PH_VISITID, 0);
            mPhLocId = savedInstanceState.getLong(ARG_PH_LOCID, 0);
//			mPhNamerId = savedInstanceState.getLong(ARG_PH_NAMERID, 0);
            mPhVisitName = savedInstanceState.getString(ARG_PH_VISIT_NAME);
            mPhLocText = savedInstanceState.getString(ARG_PH_LOC_TEXT);
            mPhNamerName = savedInstanceState.getString(ARG_PH_NAMER_NAME);
            mPhScribe = savedInstanceState.getString(ARG_PH_SCRIBE);

            Log.d(LOG_TAG, "In onCreateView, retrieved mPlaceholderId: " + mPlaceholderId);
            Log.d(LOG_TAG, "In onCreateView, retrieved mPlaceholderCode: " + mPlaceholderCode);
            Log.d(LOG_TAG, "In onCreateView, retrieved mPhVisitId: " + mPhVisitId);
//			mCurLocation = savedInstanceState.getParcelable(ARG_CUR_LOCATION);
        } else {
            Log.d(LOG_TAG, "In onCreateView, savedInstanceState == null, mPlaceholderId: " + mPlaceholderId);
        }
        // inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_edit_placeholder, container, false);
        mViewPlaceholderCode = (EditText) rootView.findViewById(R.id.txt_placeholder_code);
        mViewPlaceholderCode.setOnFocusChangeListener(this);
        registerForContextMenu(mViewPlaceholderCode); // enable long-press

        mViewPlaceholderDescription = (EditText) rootView.findViewById(R.id.txt_placeholder_description);
        mViewPlaceholderDescription.setOnFocusChangeListener(this);
        registerForContextMenu(mViewPlaceholderDescription); // enable long-press

        mViewPlaceholderHabitat = (EditText) rootView.findViewById(R.id.txt_placeholder_habitat);
        mViewPlaceholderHabitat.setOnFocusChangeListener(this);
        registerForContextMenu(mViewPlaceholderHabitat); // enable long-press

        mViewPlaceholderIdentifier = (EditText) rootView.findViewById(R.id.txt_placeholder_labelnumber);
        mViewPlaceholderIdentifier.setOnFocusChangeListener(this);
        registerForContextMenu(mViewPlaceholderIdentifier); // enable long-press

        // Prepare the loader. Either re-connect with an existing one or start a new one
//		getLoaderManager().initLoader(Loaders.PLACEHOLDER_TO_EDIT, null, this); // The current placeholder
        getLoaderManager().initLoader(Loaders.PLACEHOLDERS_EXISTING, null, this); // Any existing placeholders
//		getLoaderManager().initLoader(Loaders.PLACEHOLDER_BACKSTORY, null, this); // project, location, namer, etc., automatically recorded for a placeholder
        getLoaderManager().initLoader(Loaders.PLACEHOLDER_HABITATS, null, this); // Recall these as options to re-select

        // set click listener for the button in the view
        Button s = (Button) rootView.findViewById(R.id.placeholder_save_button);
        s.setOnClickListener(this);
        Button c = (Button) rootView.findViewById(R.id.placeholder_cancel_button);
        c.setOnClickListener(this);
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
            if (mPlaceholderId == 0) {
                // On return, this method can re-run before
                // SaveInstanceState and so retain arguments originally passed when created,
                // such as mPlaceholderId=0.
                // Do not allow that zero to overwrite a new (nonzero) mPlaceholderId, or
                // it will flag to create a second copy of the same placeholder.
                mPlaceholderId = args.getLong(ARG_PLACEHOLDER_ID, 0);
                mPlaceholderCode = args.getString(ARG_PLACEHOLDER_CODE);
            }
        // also use for special arguments like screen layout
        }
        // fire off loaders that depend on layout being ready to receive results
        getLoaderManager().initLoader(Loaders.PLACEHOLDER_TO_EDIT, null, this);
        getLoaderManager().initLoader(Loaders.PLACEHOLDER_BACKSTORY, null, this); // text of other fields
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
        outState.putLong(ARG_PLACEHOLDER_ID, mPlaceholderId);
        outState.putString(ARG_PLACEHOLDER_CODE, mPlaceholderCode);
        outState.putBoolean(ARG_CODE_WAS_SHORTENED, mCodeWasShortened);
        outState.putString(ARG_PLACEHOLDER_DESCRIPTION, mPlaceholderDescription);
        outState.putString(ARG_PLACEHOLDER_HABITAT, mPlaceholderHabitat);
        outState.putString(ARG_PLACEHOLDER_LABELNUMBER, mPlaceholderLabelNumber);
//		outState.putLong(ARG_PH_PROJID, mPhProjId);
//		outState.putLong(ARG_PH_VISITID, mPhVisitId);
        outState.putLong(ARG_PH_LOCID, mPhLocId);
//		outState.putLong(ARG_PH_NAMERID, mPhNamerId);
        outState.putString(ARG_PH_VISIT_NAME, mPhVisitName);
        outState.putString(ARG_PH_LOC_TEXT, mPhLocText);
        outState.putString(ARG_PH_NAMER_NAME, mPhNamerName);
        outState.putString(ARG_PH_SCRIBE, mPhScribe);
    }

    @Override
    public void onClick(View v) {
        Bundle args= new Bundle();
        int numUpdated;
        switch (v.getId()) {

        case R.id.placeholder_save_button:
            // create or update the Placeholder record in the database, if everything is valid
            mValidationLevel = Validation.CRITICAL; // save if possible, and announce anything invalid
            numUpdated = savePlaceholderRecord();
            if (numUpdated == 0) {
                Log.d(LOG_TAG, "Failed to save record in onClick; mValues: " + mValues.toString());
            } else {
                Log.d(LOG_TAG, "Saved record in onClick; mValues: " + mValues.toString());
            }
            if (numUpdated == 0) {
                break;
            }
            Log.d(LOG_TAG, "in onClick, about to do 'mButtonCallback.onVisitHeaderGoButtonClicked()'");
			mButtonCallback.onPlaceholderSaveButtonClicked(args);
            Log.d(LOG_TAG, "in onClick, completed 'mButtonCallback.onVisitHeaderGoButtonClicked()'");
            break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        switch (id) {
        case Loaders.PLACEHOLDER_TO_EDIT:
            Uri onePlaceholderUri = ContentUris.withAppendedId(
                            Uri.withAppendedPath(
                            ContentProvider_VegNab.CONTENT_URI, "placeholders"), mPlaceholderId);
            cl = new CursorLoader(getActivity(), onePlaceholderUri,
                    null, select, null, null);
            break;

        case Loaders.PLACEHOLDERS_EXISTING:
            baseUri = ContentProvider_VegNab.SQL_URI;
            select = "SELECT PlaceHolderCode FROM PlaceHolders "
                + "WHERE ProjID = ? AND NamerID = ? AND _id != ?";
            cl = new CursorLoader(getActivity(), baseUri, null, select,
                    new String[] { "" + mPhProjId, "" + mPhNamerId, "" + mPlaceholderId}, null);
            break;

//		case Loaders.PLACEHOLDER_PROJ_NAMER:
//			baseUri = ContentProvider_VegNab.SQL_URI;
//			select = "SELECT Visits.ProjID, Visits.NamerID "
//					+ "FROM Visits "
//					+ "WHERE Visits._id=?;";
//			cl = new CursorLoader(getActivity(), baseUri,
//					null, select, new String[] { "" + mPhVisitId }, null);
//			break;

        case Loaders.PLACEHOLDER_BACKSTORY:
            baseUri = ContentProvider_VegNab.SQL_URI;
            select = "SELECT Visits._id, Visits.VisitName, Visits.ProjID, Locations._id AS LocID, "
                    + "Locations.Latitude, Locations.Longitude, Locations.Accuracy, "
                    + "Visits.NamerID, Namers.NamerName, Visits.Scribe "
                    + "FROM (Visits LEFT JOIN Namers ON Visits.NamerID = Namers._id) "
                    + "LEFT JOIN Locations ON Visits.RefLocID = Locations._id "
                    + "WHERE (((Visits._id)=?));";
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, new String[] { "" + mPhVisitId }, null);
            break;

        case Loaders.PLACEHOLDER_HABITATS:
            baseUri = ContentProvider_VegNab.SQL_URI;
            select = "SELECT Habitat FROM PlaceHolders GROUP BY Habitat;";
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, null, null);
            break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        // there will be various loaders, switch them out here
        mRowCt = c.getCount();
        switch (loader.getId()) {

        case Loaders.PLACEHOLDER_TO_EDIT:
            Log.d(LOG_TAG, "onLoadFinished, PLACEHOLDER_TO_EDIT, records: " + c.getCount());
            if (c.moveToFirst()) {
//				mPlaceholderId = c.getLong(c.getColumnIndexOrThrow("_id"));
                mViewPlaceholderCode.setText(c.getString(c.getColumnIndexOrThrow("PlaceHolderCode")));
                mViewPlaceholderDescription.setText(c.getString(c.getColumnIndexOrThrow("Description")));
                mViewPlaceholderHabitat.setText(c.getString(c.getColumnIndexOrThrow("Habitat")));
                mViewPlaceholderIdentifier.setText(c.getString(c.getColumnIndexOrThrow("LabelNum")));
//				mPhVisitId = c.getLong(c.getColumnIndexOrThrow("VisitIdWhereFirstFound"));
//				mPhProjId = c.getLong(c.getColumnIndexOrThrow("ProjID"));
//				mPhNamerId = c.getLong(c.getColumnIndexOrThrow("NamerID"));

            }
            // else // fire off another loader?
            // getLoaderManager().initLoader(Loaders.XXX, null, this);
            break;

        case Loaders.PLACEHOLDERS_EXISTING:
            mExistingPlaceholderCodes.clear();
            while (c.moveToNext()) {
                Log.d(LOG_TAG, "onLoadFinished, add to HashMap: " + c.getString(c.getColumnIndexOrThrow("PlaceHolderCode")));
                mExistingPlaceholderCodes.add(c.getString(c.getColumnIndexOrThrow("PlaceHolderCode")));
            }
            Log.d(LOG_TAG, "onLoadFinished, number of items in mExistingPlaceholderCodes: " + mExistingPlaceholderCodes.size());
            Log.d(LOG_TAG, "onLoadFinished, items in mExistingPlaceholderCodes: " + mExistingPlaceholderCodes.toString());
            break;

//		case Loaders.PLACEHOLDER_PROJ_NAMER:
//			Log.d(LOG_TAG, "onLoadFinished, PLACEHOLDER_PROJ_NAMER, records: " + c.getCount());
//			if (c.moveToFirst()) {
//				mPhProjId = c.getLong(c.getColumnIndexOrThrow("ProjID"));
//				mPhNamerId = c.getLong(c.getColumnIndexOrThrow("NamerID"));
//			}
//			break;

        case Loaders.PLACEHOLDER_BACKSTORY:
            Log.d(LOG_TAG, "onLoadFinished, PLACEHOLDER_BACKSTORY, records: " + c.getCount());
            if (c.moveToFirst()) {
                mPhVisitName = c.getString(c.getColumnIndexOrThrow("VisitName"));
                mPhProjId = c.getLong(c.getColumnIndexOrThrow("ProjID"));
                mPhLocId = c.getLong(c.getColumnIndexOrThrow("LocID"));
                mPhLocText = "" + c.getString(c.getColumnIndexOrThrow("Latitude"))
                    + "," + c.getString(c.getColumnIndexOrThrow("Longitude"))
                    + "\naccuracy " + c.getString(c.getColumnIndexOrThrow("Accuracy")) + "m";
                mPhNamerId = c.getLong(c.getColumnIndexOrThrow("NamerID"));
                mPhNamerName = c.getString(c.getColumnIndexOrThrow("NamerName"));
                mPhScribe = c.getString(c.getColumnIndexOrThrow("Scribe"));
            }
            break;

        case Loaders.PLACEHOLDER_HABITATS:
            mPreviouslyEnteredHabitats.clear();
            while (c.moveToNext()) {
                Log.d(LOG_TAG, "onLoadFinished, add to HashMap: " + c.getString(c.getColumnIndexOrThrow("Habitat")));
                mPreviouslyEnteredHabitats.add(c.getString(c.getColumnIndexOrThrow("Habitat")));
            }
            Log.d(LOG_TAG, "onLoadFinished, number of items in mPreviouslyEnteredHabitats: " + mPreviouslyEnteredHabitats.size());
            Log.d(LOG_TAG, "onLoadFinished, items in mPreviouslyEnteredHabitats: " + mPreviouslyEnteredHabitats.toString());
            break;

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed. Need to make sure it is no longer is use.
        switch (loader.getId()) {

        case Loaders.PLACEHOLDER_TO_EDIT:
            Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDER_TO_EDIT.");
//			don't need to do anything here, no cursor adapter
            break;

        case Loaders.PLACEHOLDERS_EXISTING:
            Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDERS_EXISTING.");
//			don't need to do anything here, no cursor adapter
            break;

//		case Loaders.PLACEHOLDER_PROJ_NAMER:
//			Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDER_PROJ_NAMER.");
////			don't need to do anything here, no cursor adapter
//			break;

        case Loaders.PLACEHOLDER_BACKSTORY:
            Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDER_BACKSTORY.");
//			don't need to do anything here, no cursor adapter
            break;

        case Loaders.PLACEHOLDER_HABITATS:
            Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDER_HABITATS.");
//			don't need to do anything here, no cursor adapter
            break;

        }
    }

    private boolean validatePlaceholder() {
        // validate all items on the screen the user can see
        Context c = getActivity();
        String stringProblem;
        String errTitle = c.getResources().getString(R.string.placeholder_validate_generic_title);
        ConfigurableMsgDialog flexErrDlg = new ConfigurableMsgDialog();
        mValues.clear(); // build up mValues while validating; if returns true all members are good
        String stringPlaceholderCode = mViewPlaceholderCode.getText().toString().trim();
        if (stringPlaceholderCode.length() == 0) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.placeholder_validate_code_none);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_ph_code_none");
                    mViewPlaceholderCode.requestFocus();
                }
            }
            return false;
        }
        if (!(stringPlaceholderCode.length() >= 2)) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.placeholder_validate_code_short);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_ph_code_short");
                    mViewPlaceholderCode.requestFocus();
                }
            }
            return false;
        }

        if (stringPlaceholderCode.matches(VNRegex.NRCS_CODE)) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.placeholder_validate_code_bad);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_ph_code_bad");
                    mViewPlaceholderCode.requestFocus();
                }
            }
            return false;
        }

        if (mExistingPlaceholderCodes.contains(stringPlaceholderCode)) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.placeholder_validate_code_dup);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_ph_code_duplicate");
                    mViewPlaceholderCode.requestFocus();
                }
            }
            return false;
        }

        // PlaceHolderCode is OK, store it
        mValues.put("PlaceHolderCode", stringPlaceholderCode);

        String stringPlaceholderDescription = mViewPlaceholderDescription.getText().toString().trim();
        if (stringPlaceholderDescription.length() == 0) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.placeholder_validate_description_none);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_ph_description_none");
                    mViewPlaceholderDescription.requestFocus();
                }
            }
            return false;
        }

        mValues.put("Description", stringPlaceholderDescription); // Description is OK, store it

        // Habitat is optional, put as-is or Null if missing
        String stringHabitat = mViewPlaceholderHabitat.getText().toString().trim();
        if (stringHabitat.length() == 0) {
            mValues.putNull("Habitat");
        } else {
            mValues.put("Habitat", stringHabitat);
        }

        // LabelNum is optional, put as-is or Null if missing
        String stringLabelNum = mViewPlaceholderIdentifier.getText().toString().trim();
        if (stringLabelNum.length() == 0) {
            mValues.putNull("LabelNum");
        } else {
            mValues.put("LabelNum", stringLabelNum);
        }

        return true;
    }

    private int savePlaceholderRecord() {
        int numUpdated = 0;
        if (!validatePlaceholder()) {
            Log.d(LOG_TAG, "Failed validation in savePlaceholderRecord; mValues: " + mValues.toString());
            return numUpdated;
        }
        ContentResolver rs = getActivity().getContentResolver();
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        if (mPlaceholderId == 0) { // new record
            Log.d(LOG_TAG, "savePlaceholderRecord; creating new record with mPlaceholderId = " + mPlaceholderId);
            // fill in fields the user never sees
            mValues.put("TimeFirstInput", mTimeFormat.format(new Date()));
            mValues.put("TimeLastEdited", mTimeFormat.format(new Date()));
            mValues.put("VisitIdWhereFirstFound", sharedPref.getLong(Prefs.CURRENT_VISIT_ID, 0));
            mValues.put("ProjID", sharedPref.getLong(Prefs.DEFAULT_PROJECT_ID, 0));
            mValues.put("NamerID", sharedPref.getLong(Prefs.DEFAULT_NAMER_ID, 0));

            mUri = rs.insert(mPlaceholdersUri, mValues);
            Log.d(LOG_TAG, "new record in savePlaceholderRecord; returned URI: " + mUri.toString());
            long newRecId = Long.parseLong(mUri.getLastPathSegment());
            if (newRecId < 1) { // returns -1 on error, e.g. if not valid to save because of missing required field
                Log.d(LOG_TAG, "new record in savePlaceholderRecord has Id == " + newRecId + "); canceled");
                return 0;
            }
            mPlaceholderId = newRecId;
            getLoaderManager().restartLoader(Loaders.PLACEHOLDERS_EXISTING, null, this);

            mUri = ContentUris.withAppendedId(mPlaceholdersUri, mPlaceholderId);
            Log.d(LOG_TAG, "new record in savePlaceholderRecord; URI re-parsed: " + mUri.toString());
            numUpdated = 1;
        } else { // update the existing record
            Log.d(LOG_TAG, "savePlaceholderRecord; updating existing record with mVisitId = " + mPlaceholderId);
            mValues.put("TimeLastEdited", mTimeFormat.format(new Date())); // update the last-changed time
            mUri = ContentUris.withAppendedId(mPlaceholdersUri, mPlaceholderId);
            numUpdated = rs.update(mUri, mValues, null, null);
            Log.d(LOG_TAG, "Updated record in savePlaceholderRecord; numUpdated: " + numUpdated);
        }
        if (numUpdated > 0) {
            // may not need this
//            try {
//                mEditVisitListener.onEditVisitComplete(EditPlaceholderFragment.this);
//                // sometimes this fails with null pointer exception because fragment is gone
//            } catch (Exception e) {
//                // ignore; fn is just to refresh the screen and that will happen on fragment rebuild
//            }
        }
        return numUpdated;
    }


    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(!hasFocus) { // something lost focus
            mValues.clear();
            switch (v.getId()) {
            case R.id.txt_placeholder_code:
            case R.id.txt_placeholder_description:
            case R.id.txt_placeholder_habitat:
            case R.id.txt_placeholder_labelnumber:
                mValidationLevel = Validation.QUIET; // save if possible, but notify minimally
                int numUpdated = savePlaceholderRecord();
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
        case R.id.txt_placeholder_code:
            inflater.inflate(R.menu.context_placeholder_code, menu);
            break;
        case R.id.txt_placeholder_description:
            inflater.inflate(R.menu.context_placeholder_description, menu);
            break;
        case R.id.txt_placeholder_habitat:
            inflater.inflate(R.menu.context_placeholder_habitat, menu);
            break;
        case R.id.txt_placeholder_labelnumber:
            inflater.inflate(R.menu.context_placeholder_labelnumber, menu);
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
    case R.id.placeholder_code_help:
        Log.d(LOG_TAG, "'Placeholder Code Help' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Edit Placeholder Event")
                .setAction("Context Menu")
                .setLabel("Placeholder Code Help")
                .setValue(1)
                .build());
        // Visit Name help
        helpTitle = c.getResources().getString(R.string.placeholder_help_code_title);
        helpMessage = c.getResources().getString(R.string.placeholder_help_code_text);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_help_placeholder_code");
        return true;
    case R.id.placeholder_description_help:
        Log.d(LOG_TAG, "'Placeholder Description Help' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Edit Placeholder Event")
                .setAction("Context Menu")
                .setLabel("Placeholder Description Help")
                .setValue(1)
                .build());
        helpTitle = c.getResources().getString(R.string.placeholder_help_description_title);
        helpMessage = c.getResources().getString(R.string.placeholder_help_description_text);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_help_placeholder_description");
        return true;

    case R.id.placeholder_habitat_help:
        Log.d(LOG_TAG, "'Placeholder Habitat Help' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Edit Placeholder Event")
                .setAction("Context Menu")
                .setLabel("Placeholder Habitat Help")
                .setValue(1)
                .build());
        helpTitle = c.getResources().getString(R.string.placeholder_help_habitat_title);
        helpMessage = c.getResources().getString(R.string.placeholder_help_habitat_text);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_help_placeholder_habitat");
        return true;

    case R.id.placeholder_labelnumber_help:
        Log.d(LOG_TAG, "'Placeholder Label Number Help' selected");
        headerContextTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Edit Placeholder Event")
                .setAction("Context Menu")
                .setLabel("Placeholder Label Number Help")
                .setValue(1)
                .build());
        helpTitle = c.getResources().getString(R.string.placeholder_help_labelnumber_title);
        helpMessage = c.getResources().getString(R.string.placeholder_help_labelnumber_text);
        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
        flexHlpDlg.show(getFragmentManager(), "frg_help_placeholder_labelnumber");
        return true;

    default:
        return super.onContextItemSelected(item);
       }
    }
}