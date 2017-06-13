package com.vegnab.vegnab;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract;
import com.vegnab.vegnab.database.VNContract.LDebug;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.Prefs;
import com.vegnab.vegnab.database.VNContract.Validation;
import com.vegnab.vegnab.database.VNContract.VNRegex;
import com.vegnab.vegnab.database.VegNabDbHelper;
import com.vegnab.vegnab.util.InputFilterPlaceholderCode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public class EditPlaceholderFragment extends Fragment implements OnClickListener,
        android.widget.AdapterView.OnItemSelectedListener,
        View.OnFocusChangeListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    public interface EditPlaceholderFragmentListener {
        void onEditPlaceholderComplete(EditPlaceholderFragment editPlaceholderFragment);
    }
    EditPlaceholderFragmentListener mEditPhListener;


    private static final String LOG_TAG = EditPlaceholderFragment.class.getSimpleName();

    private int mValidationLevel = Validation.SILENT;

    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    // explicitly handle all fields; some API versions have bugs that lose cursors on orientation change, etc.
    // zero and null defaults means new or not specified yet
    long mPlaceholderId = 0, mPhProjId = 0, mPhVisitId = 0, mPhLocId = 0, mPhNamerId = 0,
            mIdentNamerId = 0, mIdentRefId = 0, mIdentMethodId = 0, mIdentCFId = 1;
    String mPlaceholderCode = null, mPlaceholderDescription = null, mPlaceholderHabitat = null,
            mPlaceholderLabelNumber = null, mPhIdentSppCode = null, mPhIdentSppDescr = null,
            mPhIdentNotes = null, mPhIdentTimeStamp = null,
            mStSearchHabitat = "", mStSearch = "";
    Boolean mCodeWasShortened = false, mIdPlaceholder = false, mPixHousekeepingDone = false;
    HashSet<String> mExistingPlaceholderCodes = new HashSet<String>();

    private Button mBtnIdent;
    private Spinner mIdentNamerSpinner, mIdentRefSpinner, mIdentMethodSpinner, mIdentCFSpinner;
    private TextView mLblIdentNamerSpinnerCover, mLblIdentRefSpinnerCover, mLblIdentMethodSpinnerCover;
    SimpleCursorAdapter mPhHabitatAdapter, mIdentNamerAdapter, mIdentRefAdapter, mIdentMethodAdapter, mIdentCFAdapter;

    private ViewGroup mViewGroupIdent; // the set of views involved with identify-species
    // viewgroups that pair a spinner with its cover (to receive clicks)
    // in groups to manage which of the pair is in front, without interfering with the whole layout
    private ViewGroup mNamerViewGroup, mRefViewGroup, mMethodViewGroup;

    //ident_method_veiw_group

    Uri mUri;
    Uri mPlaceholdersUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "placeholders");
    ContentValues mValues = new ContentValues();

    private EditText mViewPlaceholderCode, mViewPlaceholderDescription,
            mViewPlaceholderIdentifier, mViewPlaceholderIdentNotes;
    AutoCompleteTextView mAutoCompletePlaceholderHabitat, mSppIdentAutoComplete;

    SelSppIdentAdapter mSppIdentResultsAdapter;
    Cursor mSppIdentCursor;

    SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    // explicitly save/retrieve all these through Bundles, some versions have bugs that lose cursor
    final static String ARG_PLACEHOLDER_ID = "placeholderId";
    final static String ARG_PH_PROJECT_ID = "phProjectId";
    final static String ARG_PH_NAMER_ID = "phNamerId";
    final static String ARG_PLACEHOLDER_CODE = "placeholderCode";
    final static String ARG_CODE_WAS_SHORTENED = "phCodeShortened";
    final static String ARG_PLACEHOLDER_DESCRIPTION = "placeholderDescription";
    final static String ARG_PLACEHOLDER_HABITAT = "placeholderHabitat";
    final static String ARG_PLACEHOLDER_LABELNUMBER = "placeholderLabelnumber";
    final static String BUTTON_KEY = "buttonKey";
    final static String ARG_ID_PLACEHOLDER = "identifyPh";
    final static String ARG_IDENT_NAMER_ID = "identNamerId";
    final static String ARG_IDENT_REF_ID = "identRefId";
    final static String ARG_IDENT_METHOD_ID = "identMethodId";
    final static String ARG_IDENT_CF_ID = "identCFId";
    final static String ARG_IDENT_NOTES = "identNotes";
    final static String ARG_IDENT_TIMESTAMP = "identTimeStamp";

    OnButtonListener mButtonCallback; // declare the interface
    // declare that the container Activity must implement this interface
    public interface OnButtonListener {
        // methods that must be implemented in the container Activity
        void onPlaceholderActionButtonClicked(Bundle args);
    }

    public static EditPlaceholderFragment newInstance(Bundle args) {
        EditPlaceholderFragment f = new EditPlaceholderFragment();
        f.setArguments(args);
        return f;
    }

    //habitatTextWatcher
    TextWatcher habitatTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            // use this method; test length of string; e.g. 'count' of other methods does not give this length
            //Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "'");
           if (LDebug.ON) Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "', length: " + s.length());
            mStSearchHabitat = s.toString();
            getLoaderManager().restartLoader(Loaders.PLACEHOLDER_HABITATS, null, EditPlaceholderFragment.this);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // the 'count' characters beginning at 'start' are about to be replaced by new text with length 'after'
            //Log.d(LOG_TAG, "beforeTextChanged, s: '" + s.toString() + "', start: " + start + ", count: " + count + ", after: " + after);
            //
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // the 'count' characters beginning at 'start' have just replaced old text that had length 'before'
            //Log.d(LOG_TAG, "onTextChanged, s: '" + s.toString() + "', start: " + start + ", before: " + before + ", count: " + count);
        }
    };


    TextWatcher sppIdentTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            // use this method; test length of string; e.g. 'count' of other methods does not give this length
            //Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "'");
           if (LDebug.ON) Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "', length: " + s.length());
            mStSearch = s.toString();
            getLoaderManager().restartLoader(Loaders.PH_IDENT_SPECIES, null, EditPlaceholderFragment.this);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // the 'count' characters beginning at 'start' are about to be replaced by new text with length 'after'
            //Log.d(LOG_TAG, "beforeTextChanged, s: '" + s.toString() + "', start: " + start + ", count: " + count + ", after: " + after);
            //
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // the 'count' characters beginning at 'start' have just replaced old text that had length 'before'
            //Log.d(LOG_TAG, "onTextChanged, s: '" + s.toString() + "', start: " + start + ", before: " + before + ", count: " + count);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get a Tracker (should auto-report)
        ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
//        try {
//            mEditPlaceholderListener = (EditPlaceholderDialogListener) getActivity();
//           if (LDebug.ON) Log.d(LOG_TAG, "(EditPlaceholderDialogListener) getActivity()");
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
        switch (item.getItemId()) { // the Activity has first opportunity to handle these
        // any not handled come here to this Fragment

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
//        mPhProjId = sharedPref.getLong(Prefs.DEFAULT_PROJECT_ID, 0);
//        mPhNamerId = sharedPref.getLong(Prefs.DEFAULT_NAMER_ID, 0);
        mPhVisitId = sharedPref.getLong(Prefs.CURRENT_VISIT_ID, 0);
        // if the activity was re-created (e.g. from a screen rotate)
        // restore the previous screen, remembered by onSaveInstanceState()
        // This is mostly needed in fixed-pane layouts
        if (savedInstanceState != null) {
           if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView, about to retrieve mPlaceholderId: " + mPlaceholderId);
            mPlaceholderId = savedInstanceState.getLong(ARG_PLACEHOLDER_ID, 0);
            mPlaceholderCode = savedInstanceState.getString(ARG_PLACEHOLDER_CODE);
            mCodeWasShortened = savedInstanceState.getBoolean(ARG_CODE_WAS_SHORTENED, false);
            mPhProjId = savedInstanceState.getLong(ARG_PH_PROJECT_ID, 0);
            mPhNamerId = savedInstanceState.getLong(ARG_PH_NAMER_ID, 0);
            mPlaceholderDescription = savedInstanceState.getString(ARG_PLACEHOLDER_DESCRIPTION);
            mPlaceholderHabitat = savedInstanceState.getString(ARG_PLACEHOLDER_HABITAT);
            mPlaceholderLabelNumber = savedInstanceState.getString(ARG_PLACEHOLDER_LABELNUMBER);
            mIdPlaceholder = savedInstanceState.getBoolean(ARG_ID_PLACEHOLDER);
            mIdentNamerId = savedInstanceState.getLong(ARG_IDENT_NAMER_ID, 0);
            mIdentRefId = savedInstanceState.getLong(ARG_IDENT_REF_ID, 0);
            mIdentMethodId = savedInstanceState.getLong(ARG_IDENT_METHOD_ID, 0);
            mIdentCFId = savedInstanceState.getLong(ARG_IDENT_CF_ID, 1);
            mPhIdentTimeStamp = savedInstanceState.getString(ARG_IDENT_TIMESTAMP);

           if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView, retrieved mPlaceholderId: " + mPlaceholderId);
           if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView, retrieved mPlaceholderCode: " + mPlaceholderCode);
           if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView, retrieved mPhVisitId: " + mPhVisitId);
        } else {
           if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView, savedInstanceState == null, mPlaceholderId: " + mPlaceholderId);
        }
        // inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_edit_placeholder, container, false);
        mViewPlaceholderCode = (EditText) rootView.findViewById(R.id.txt_placeholder_code);
        if (LDebug.ON) Log.d(LOG_TAG, "Editing a Placeholder, about to set InputFilterPlaceholderCode");
        // prevents all but alphanumeric chars
        mViewPlaceholderCode.setFilters(new InputFilter[] { new InputFilterPlaceholderCode() });
        mViewPlaceholderCode.setOnFocusChangeListener(this);
        registerForContextMenu(mViewPlaceholderCode); // enable long-press

        mViewPlaceholderDescription = (EditText) rootView.findViewById(R.id.txt_placeholder_description);
        mViewPlaceholderDescription.setOnFocusChangeListener(this);
        registerForContextMenu(mViewPlaceholderDescription); // enable long-press

        mAutoCompletePlaceholderHabitat = (AutoCompleteTextView) rootView.findViewById(R.id.autocomplete_placeholder_habitat);
        mPhHabitatAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_dropdown_item_1line, null,
                new String[] {"Habitat"},
                new int[] {android.R.id.text1}, 0);
        mPhHabitatAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        mAutoCompletePlaceholderHabitat.setAdapter(mPhHabitatAdapter);
        mAutoCompletePlaceholderHabitat.addTextChangedListener(habitatTextWatcher);
        mAutoCompletePlaceholderHabitat.setOnFocusChangeListener(this);
        mAutoCompletePlaceholderHabitat.setThreshold(1); // match from 1st character
        registerForContextMenu(mAutoCompletePlaceholderHabitat); // enable long-press

        mViewPlaceholderIdentifier = (EditText) rootView.findViewById(R.id.txt_placeholder_labelnumber);
        mViewPlaceholderIdentifier.setOnFocusChangeListener(this);
        registerForContextMenu(mViewPlaceholderIdentifier); // enable long-press

        // Prepare the loader. Either re-connect with an existing one or start a new one
        getLoaderManager().initLoader(Loaders.PLACEHOLDER_HABITATS, null, this); // Recall these as options to re-select

        // set click listener for the buttons in the view
        Button p = (Button) rootView.findViewById(R.id.placeholder_pix_button);
        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            p.setOnClickListener(this);
        } else {
            p.setVisibility(View.GONE);
        }

        mBtnIdent = (Button) rootView.findViewById(R.id.ph_identify_button);
        mBtnIdent.setOnClickListener(this);
        // if more, loop through all the child items of the ViewGroup rootView and
        // set the onclicklistener for all the Button instances found

        // set up spinners
        // Namer spinner
        mIdentNamerSpinner = (Spinner) rootView.findViewById(R.id.spn_ph_ident_namer);
        mIdentNamerSpinner.setTag(VNContract.Tags.SPINNER_FIRST_USE); // flag to catch and ignore erroneous first firing
        mIdentNamerSpinner.setEnabled(false); // will enable when data ready
        mIdentNamerAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, null,
                new String[] {"IdNamerName"},
                new int[] {android.R.id.text1}, 0);
        mIdentNamerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mIdentNamerSpinner.setAdapter(mIdentNamerAdapter);
        mIdentNamerSpinner.setOnItemSelectedListener(this);
        registerForContextMenu(mIdentNamerSpinner); // enable long-press
        // also need click, if no items & therefore selection cannot be changed
        // use a TextView on top of the spinner, named "..._spinner_cover"
        mLblIdentNamerSpinnerCover = (TextView) rootView.findViewById(R.id.lbl_ident_namer_spinner_cover);
        mLblIdentNamerSpinnerCover.setOnClickListener(this);
        registerForContextMenu(mLblIdentNamerSpinnerCover); // enable long-press
        // Ref spinner
        mIdentRefSpinner = (Spinner) rootView.findViewById(R.id.spn_ph_ident_ref);
        mIdentRefSpinner.setTag(VNContract.Tags.SPINNER_FIRST_USE); // flag to catch and ignore erroneous first firing
        mIdentRefSpinner.setEnabled(false); // will enable when data ready
        mIdentRefAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, null,
                new String[] {"IdRef"},
                new int[] {android.R.id.text1}, 0);
        mIdentRefAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mIdentRefSpinner.setAdapter(mIdentRefAdapter);
        mIdentRefSpinner.setOnItemSelectedListener(this);
        registerForContextMenu(mIdentRefSpinner); // enable long-press
        // also need click, if no items & therefore selection cannot be changed
        // use a TextView on top of the spinner, named "..._spinner_cover"
        mLblIdentRefSpinnerCover = (TextView) rootView.findViewById(R.id.lbl_ident_ref_spinner_cover);
        mLblIdentRefSpinnerCover.setOnClickListener(this);
        registerForContextMenu(mLblIdentRefSpinnerCover); // enable long-press
        // Method spinner
        mIdentMethodSpinner = (Spinner) rootView.findViewById(R.id.spn_ph_ident_method);
        mIdentMethodSpinner.setTag(VNContract.Tags.SPINNER_FIRST_USE); // flag to catch and ignore erroneous first firing
        mIdentMethodSpinner.setEnabled(false); // will enable when data ready
        mIdentMethodAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, null,
                new String[] {"IdMethod"},
                new int[] {android.R.id.text1}, 0);
        mIdentMethodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mIdentMethodSpinner.setAdapter(mIdentMethodAdapter);
        mIdentMethodSpinner.setOnItemSelectedListener(this);
        registerForContextMenu(mIdentMethodSpinner); // enable long-press
        // also need click, if no items & therefore selection cannot be changed
        // use a TextView on top of the spinner, named "..._spinner_cover"
        mLblIdentMethodSpinnerCover = (TextView) rootView.findViewById(R.id.lbl_ident_method_spinner_cover);
        mLblIdentMethodSpinnerCover.setOnClickListener(this);
        registerForContextMenu(mLblIdentMethodSpinnerCover); // enable long-press
        // CF spinner
        mIdentCFSpinner = (Spinner) rootView.findViewById(R.id.spn_ph_ident_cf);
        mIdentCFSpinner.setTag(VNContract.Tags.SPINNER_FIRST_USE); // flag to catch and ignore erroneous first firing
        mIdentCFSpinner.setEnabled(false); // will enable when data ready
        mIdentCFAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, null,
                new String[] {"IdLevelDescr"},
                new int[] {android.R.id.text1}, 0);
        mIdentCFAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mIdentCFSpinner.setAdapter(mIdentCFAdapter);
        mIdentCFSpinner.setOnItemSelectedListener(this);
        registerForContextMenu(mIdentCFSpinner); // enable long-press

        mSppIdentAutoComplete = (AutoCompleteTextView) rootView.findViewById(R.id.autocomplete_ph_ident_spp);
        mSppIdentResultsAdapter = new SelSppIdentAdapter(getActivity(),
                R.layout.list_spp_search_item, null, 0);

        mSppIdentAutoComplete.setAdapter(mSppIdentResultsAdapter);
        mSppIdentAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                mSppIdentCursor.moveToPosition(position);
                String stItem = mSppIdentCursor.getString(mSppIdentCursor.getColumnIndexOrThrow("MatchTxt"));
                mSppIdentAutoComplete.setText(stItem);
            }
        });

        mSppIdentAutoComplete.setThreshold(1); // see if search from 1st char is too slow
        mSppIdentAutoComplete.addTextChangedListener(sppIdentTextWatcher);
        // try to turn off spell check, for scientific names
        mSppIdentAutoComplete.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        mViewPlaceholderIdentNotes = (EditText) rootView.findViewById(R.id.txt_ph_ident_notes);

        // the views for identify-species, to show or hide as a group
        mViewGroupIdent = (ViewGroup) rootView.findViewById(R.id.ident_veiw_group);
        mNamerViewGroup = (ViewGroup) rootView.findViewById(R.id.ident_namer_veiw_group);
        mRefViewGroup = (ViewGroup) rootView.findViewById(R.id.ident_ref_veiw_group);
        mMethodViewGroup = (ViewGroup) rootView.findViewById(R.id.ident_method_veiw_group);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(getActivity()).reportActivityStart(getActivity());
//        // make sure device has a camera
//        if (!hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
//
//        }
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
                mCodeWasShortened = args.getBoolean(ARG_CODE_WAS_SHORTENED, false);
                mPhProjId = args.getLong(ARG_PH_PROJECT_ID, 0);
                mPhNamerId = args.getLong(ARG_PH_NAMER_ID, 0);
                mViewPlaceholderDescription.requestFocus();
            }
        // also use for special arguments like screen layout
        }
        // fire off loaders that depend on layout being ready to receive results
        getLoaderManager().initLoader(Loaders.PLACEHOLDER_TO_EDIT, null, this);
        getLoaderManager().initLoader(Loaders.PLACEHOLDERS_EXISTING, null, this); // Any existing placeholders
        // loaders for species identification items
        getLoaderManager().initLoader(Loaders.PH_IDENT_NAMERS, null, this);
        getLoaderManager().initLoader(Loaders.PH_IDENT_REFS, null, this);
        getLoaderManager().initLoader(Loaders.PH_IDENT_METHODS, null, this);
        getLoaderManager().initLoader(Loaders.PH_IDENT_CONFIDENCS, null, this);
        getLoaderManager().initLoader(Loaders.PH_IDENT_SPECIES, null, this);

        if (mCodeWasShortened) {
            Context c = getActivity();
            String sTitle = c.getResources().getString(R.string.new_placeholder_was_shortened_title);
            String sIssue = c.getResources().getString(R.string.new_placeholder_was_shortened_msg_pre)
                    + " " + VNContract.VNConstraints.PLACEHOLDER_MAX_LENGTH + " "
                    + c.getResources().getString(R.string.new_placeholder_was_shortened_msg_post);
            ConfigurableMsgDialog flexMsgDlg = new ConfigurableMsgDialog();
            flexMsgDlg = ConfigurableMsgDialog.newInstance(sTitle, sIssue);
            flexMsgDlg.show(getFragmentManager(), "frg_msg_ph_code_shortened");
            mCodeWasShortened = false; // only show once
        }
        //  hide views dealing with identifying the species
        configureIdViews();

        // do folder/DB housekeeping on any pictures
        if (!mPixHousekeepingDone) { // do only once per instance
            if (mPlaceholderId > 0) { // would not be any pictures until Placeholder is created
                // prep vars to use inside thread
                final long phId = mPlaceholderId;
                // Perform housekeeping off the UI thread.
                new Thread() {
                    @Override
                    public void run() {
                        // check what image files are in the Placeholder folder and update the database
                        if (LDebug.ON) Log.d(LOG_TAG, "in pix housekeeping thread");
                        // File pixDir = getAlbumDir();
                        VegNabDbHelper hkDb = new VegNabDbHelper(getContext());
                        Cursor phCs;
                        String sSQL, sNamer = "", sPhCode = "";
                        // get text of Namer and Placeholder from DB, will reflect any spelling changes
                        sSQL = "SELECT Namers.NamerName, Placeholders.PlaceHolderCode "
                            + "FROM Placeholders LEFT JOIN Namers ON Namers._id = Placeholders.NamerID "
                            + "WHERE Placeholders._id = " + phId + ";";
                        phCs = hkDb.getReadableDatabase().rawQuery(sSQL, null);
                        while (phCs.moveToNext()) { // should be just one record
                            sNamer = phCs.getString(phCs.getColumnIndexOrThrow("NamerName"));
                            sPhCode = phCs.getString(phCs.getColumnIndexOrThrow("PlaceHolderCode"));
                        }
                        phCs.close();
                        if (LDebug.ON) Log.d(LOG_TAG, "in pix housekeeping thread: " + sNamer + "/" + sPhCode);
                        if ((sNamer.equals("")) || (sPhCode.equals(""))) {
                            hkDb.close();
                            return;
                        }
                        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                            hkDb.close();
                            return;
                        }
                        File phPixDir = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES),
                                BuildConfig.PUBLIC_DB_FOLDER + File.separator + sNamer
                                        + File.separator + sPhCode);
                        if (!phPixDir.exists()) {
                            hkDb.close();
                            return;
                        }
                        if (!phPixDir.isDirectory()) {
                            hkDb.close();
                            return;
                        }
                        // get paths of all the image files
                        ArrayList<String> sPaths = new ArrayList<>();
                        File[] allFiles = phPixDir.listFiles();
                        if (LDebug.ON) Log.d(LOG_TAG, "allFiles.length: " + allFiles.length);
                        if (allFiles.length == 0) {
                            hkDb.close();
                            return;
                        }
                        for (File file : allFiles) {
                            if (LDebug.ON) Log.d(LOG_TAG, "file: " + file.toString());
                            if (!file.isDirectory()) {
                                if (LDebug.ON) Log.d(LOG_TAG, "is not Directory: " + file.toString());
                                if (file.length() > 0) {
                                    Uri uri = null;
                                    try {
                                        uri = Uri.fromFile(file);
                                    } catch (Exception e) {
                                        if (LDebug.ON) Log.d(LOG_TAG, "Uri.fromFile(file); Exception: " + e.toString());
                                        // ignore
                                    }
                                    if (uri != null) {
                                        if(uri.getScheme().equals(ContentResolver.SCHEME_FILE)) {
                                            String ext = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
                                            if (LDebug.ON) Log.d(LOG_TAG, ext + " for " + file.getAbsolutePath());
                                            if (ext.equals("jpg")) {
                                                sPaths.add(file.getAbsolutePath());
                                            }
                                        }
                                    }
                                } else {
                                    if (LDebug.ON) Log.d(LOG_TAG, "zero length file skipped: " + file.toString());
                                }
                            } else {
                                if (LDebug.ON) Log.d(LOG_TAG, "isDirectory: " + file.toString());
                            }
                        } // end 'for (File file : allFiles) {'
                        if (LDebug.ON) Log.d(LOG_TAG, "sPaths.size(): " + sPaths.size());
                        if (sPaths.size() == 0) {
                            if (LDebug.ON) Log.d(LOG_TAG, "sPaths.size() == 0");
                            // clear recs from DB if there are any
                            // if none, this will have no effect
                            sSQL = "DELETE FROM PlaceHolderPix "
                                    + "WHERE PlaceHolderPix.PlaceHolderID = " + phId + ";";
                            try {
                                hkDb.getWritableDatabase().execSQL(sSQL);
                            } catch (SQLException e) {
                                if (LDebug.ON) Log.d(LOG_TAG, "error in query: " + sSQL);
                            }
                            hkDb.close();
                            return;
                        }

                        //
                        /* checking query, something like
                        SELECT PlaceHolderPix._id, PlaceHolderPix.PlaceHolderID, PlaceHolderPix.PhotoPath, PlaceHolderPix.PhotoTimeStamp
                        FROM PlaceHolderPix WHERE PhotoPath IN (
                        "/storage/emulated/0/Pictures/VegNabAlphaTest/null/null/20170401_135845_1154545246.jpg",
                        "/storage/emulated/0/Pictures/VegNabAlphaTest/Rick Shory/pinn blu/20170406_095335_786385682.jpg");
                        */
                        String sPathsList = "\"" + TextUtils.join("\", \"", sPaths) + "\"";
                        sSQL = "SELECT PlaceHolderPix._id, PlaceHolderPix.PlaceHolderID, "
                                + "PlaceHolderPix.PhotoPath, PlaceHolderPix.PhotoTimeStamp "
                                + "FROM PlaceHolderPix WHERE PlaceHolderPix.PlaceHolderID = " + phId + " "
                                + "AND PhotoPath IN (" + sPathsList + ");";
                        phCs = hkDb.getReadableDatabase().rawQuery(sSQL, null);
                        if (phCs.getCount() == sPaths.size()) {
                            if (LDebug.ON) Log.d(LOG_TAG, "(phCs.getCount() == sPaths.size()): " + sPaths.size());
                            // the files in the folder exactly match up with records in the database; we are done
                            phCs.close();
                            hkDb.close();
                            return;
                        }
                        // otherwise, there is some mismatch
                        // remember any that do match
                        ArrayList<Long> lPixIdsToKeep = new ArrayList<>();
                        while (phCs.moveToNext()) {
                            // remember the one that matched
                            if (LDebug.ON) Log.d(LOG_TAG, "keep pic with rec ID "
                                    + phCs.getLong(phCs.getColumnIndexOrThrow("_id")));
                            lPixIdsToKeep.add(phCs.getLong(phCs.getColumnIndexOrThrow("_id")));
                            // done with this item in sPaths, remove
                            Iterator<String> it = sPaths.iterator();
                            while (it.hasNext()) {
                                if (it.next().equals(phCs.getString(phCs.getColumnIndexOrThrow("PhotoPath")))) {
                                    if (LDebug.ON) Log.d(LOG_TAG, "remove from keep list: "
                                            + phCs.getString(phCs.getColumnIndexOrThrow("PhotoPath")));
                                    it.remove();
                                    break; // unique, so can stop searching
                                }
                            }
                        }
                        phCs.close();
                        if (LDebug.ON) Log.d(LOG_TAG, "keeping " + lPixIdsToKeep.size() + " Ph pix: "
                                + TextUtils.join(", ", lPixIdsToKeep));
                        if (lPixIdsToKeep.size() == 0) { // no matches, keep none for this Placeholder
                            sSQL = "DELETE FROM PlaceHolderPix "
                                + "WHERE PlaceHolderPix.PlaceHolderID = " + phId + ";";
                        } else { // keep the ones that match, remove the others
                            sSQL = "DELETE FROM PlaceHolderPix "
                                + "WHERE PlaceHolderPix.PlaceHolderID = " + phId + " "
                                + "AND PlaceHolderPix._id NOT IN (" + TextUtils.join(", ", lPixIdsToKeep) + ");";
                        }
                        // run the deletion query
                        if (LDebug.ON) Log.d(LOG_TAG, "about to run DELETE query");
                        hkDb.getWritableDatabase().execSQL(sSQL);
                        if (LDebug.ON) Log.d(LOG_TAG, "after DELETE query");

                        // insert all the correct paths
                        SimpleDateFormat tFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                        for (String sAbsPath : sPaths) {
                            File imgFile = new  File(sAbsPath);
                            String sDtTime = tFormat.format(new Date(imgFile.lastModified()));
                            sSQL = "INSERT INTO PlaceHolderPix (PlaceHolderID, PhotoPath, PhotoTimeStamp) "
                                    + "VALUES ( " + phId + ", \"" + sAbsPath + "\", \"" + sDtTime + "\");";
                            // run the append query
                            if (LDebug.ON) Log.d(LOG_TAG, "about to run query: " + sSQL);
                            // try content provider instead
                            hkDb.getWritableDatabase().execSQL(sSQL);
                            if (LDebug.ON) Log.d(LOG_TAG, "after APPEND query");
                            hkDb.close();
                        }

                        }

                    }.start(); // end of new thread

                mPixHousekeepingDone = true;
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // assure the container activity has implemented the callback interfaces
        if (context instanceof Activity){
            Activity a = (Activity) context;
            try {
                mButtonCallback = (OnButtonListener) a;
            } catch (ClassCastException e) {
                throw new ClassCastException (a.toString() + " must implement OnButtonListener");
            }
            try {
                mEditPhListener = (EditPlaceholderFragmentListener) a;
            } catch (ClassCastException e) {
                throw new ClassCastException (a.toString() + " must implement EditPlaceholderFragmentListener");
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save the current subplot arguments in case we need to re-create the fragment
        outState.putLong(ARG_PLACEHOLDER_ID, mPlaceholderId);
        outState.putLong(ARG_PH_PROJECT_ID, mPhProjId);
        outState.putLong(ARG_PH_NAMER_ID, mPhNamerId);
        outState.putString(ARG_PLACEHOLDER_CODE, mPlaceholderCode);
        outState.putBoolean(ARG_CODE_WAS_SHORTENED, mCodeWasShortened);
        outState.putString(ARG_PLACEHOLDER_DESCRIPTION, mPlaceholderDescription);
        outState.putString(ARG_PLACEHOLDER_HABITAT, mPlaceholderHabitat);
        outState.putString(ARG_PLACEHOLDER_LABELNUMBER, mPlaceholderLabelNumber);
        outState.putBoolean(ARG_ID_PLACEHOLDER, mIdPlaceholder);
        outState.putLong(ARG_IDENT_NAMER_ID, mIdentNamerId);
        outState.putLong(ARG_IDENT_REF_ID, mIdentRefId);
        outState.putLong(ARG_IDENT_METHOD_ID, mIdentMethodId);
        outState.putLong(ARG_IDENT_CF_ID, mIdentCFId);
        outState.putString(ARG_IDENT_TIMESTAMP, mPhIdentTimeStamp);
    }

    @Override
    public void onClick(View v) {
        Bundle args= new Bundle();
        int numUpdated;
        Context c = getActivity();
        ConfigurableMsgDialog flexHlpDlg = new ConfigurableMsgDialog();
        String helpTitle, helpMessage;
        // get an Analytics event tracker
        Tracker placeholderButtonTracker = ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);

        switch (v.getId()) {

            case R.id.placeholder_pix_button:
               if (LDebug.ON) Log.d(LOG_TAG, "in onClick, placeholder_pix_button");
                placeholderButtonTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Edit Placeholder Event")
                        .setAction("Button click")
                        .setLabel("Pictures button")
                        .setValue(mPlaceholderId)
                        .build());
                if (mPlaceholderId == 0) { // record not defined yet, try once to save it
                    mValidationLevel = Validation.SILENT;
                    numUpdated = savePlaceholderRecord();
                    if (numUpdated == 0) { // still not complete
                        helpTitle = c.getResources().getString(R.string.placeholder_pix_btn_no_descr_title);
                        helpMessage = c.getResources().getString(R.string.placeholder_pix_btn_no_descr_msg);
                        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
                        flexHlpDlg.show(getFragmentManager(), "frg_ph_pix_not_ready");
                        return;
                    }
                }
                args.putInt(BUTTON_KEY, VNContract.PhActions.GO_TO_PICTURES);
                args.putLong(ARG_PLACEHOLDER_ID, mPlaceholderId);
               if (LDebug.ON) Log.d(LOG_TAG, "in onClick, about to do 'mButtonCallback.onPlaceholderActionButtonClicked(PICTURES)'");
                mButtonCallback.onPlaceholderActionButtonClicked(args);
               if (LDebug.ON) Log.d(LOG_TAG, "in onClick, completed 'mButtonCallback.onPlaceholderActionButtonClicked(PICTURES)'");
                break;

            case R.id.ph_identify_button:
               if (LDebug.ON) Log.d(LOG_TAG, "in onClick, ph_identify_button");
                if (mPlaceholderId == 0) { // record not defined yet
                    mValidationLevel = Validation.SILENT;
                    mIdPlaceholder = false; // assure not in identification mode
                    numUpdated = savePlaceholderRecord(); // save only the main fields, not the ident fields
                    if (numUpdated == 0) { // still not complete
                        // message that Placeholder must be defined first
                        helpTitle = c.getResources().getString(R.string.placeholder_ident_btn_no_descr_title);
                        helpMessage = c.getResources().getString(R.string.placeholder_ident_btn_no_descr_msg);
                        flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
                        flexHlpDlg.show(getFragmentManager(), "frg_ph_ident_not_ready");
                        return;
                    }
                }
                // main fields of record OK, record ID would still be zero
                if (mIdPlaceholder) { // toggle
                    // since mIdPlaceholder == true, attempt to save idents as well as the rest of the record
                    mValidationLevel = Validation.CRITICAL; // give warnings about any invalid fields
                    savePlaceholderRecord();
                    mIdPlaceholder = false; // go out of identification mode
                } else {
                    mValidationLevel = Validation.QUIET; // show only Toasts, rather than pop up dialogs
                    // since mIdPlaceholder == false, save only the main fields, not the ident fields
                    savePlaceholderRecord();
                    mIdPlaceholder = true; // go into ident mode
                }
                configureIdViews(); // hide or show the ident views
                break;

            case R.id.lbl_ident_namer_spinner_cover:
               if (LDebug.ON) Log.d(LOG_TAG, "in onClick, lbl_ident_namer_spinner_cover");
                ConfigurableEditDialog newIdNamerDlg =
                        ConfigurableEditDialog.newInstance(getArgsForNewIdentNamer());
                newIdNamerDlg.show(getFragmentManager(), "frg_new_idnamer_fromCover");
                break;

            case R.id.lbl_ident_ref_spinner_cover:
               if (LDebug.ON) Log.d(LOG_TAG, "in onClick, lbl_ident_ref_spinner_cover");
                ConfigurableEditDialog newIdRefDlg =
                        ConfigurableEditDialog.newInstance(getArgsForNewIdentRef());
                newIdRefDlg.show(getFragmentManager(), "frg_new_idref_fromCover");
                break;

            case R.id.lbl_ident_method_spinner_cover:
               if (LDebug.ON) Log.d(LOG_TAG, "in onClick, lbl_ident_method_spinner_cover");
                ConfigurableEditDialog newIdMethodDlg =
                        ConfigurableEditDialog.newInstance(getArgsForNewIdentMethod());
                newIdMethodDlg.show(getFragmentManager(), "frg_new_idmethod_fromCover");
                break;
        }
    }

    public void saveDefaultItemId(String pref, long id) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putLong(pref, id);
        prefEditor.commit();
    }

    public void refreshIdNamerSpinner() {
        getLoaderManager().restartLoader(Loaders.PH_IDENT_NAMERS, null, this);
    }

    public void refreshIdRefSpinner() {
        getLoaderManager().restartLoader(Loaders.PH_IDENT_REFS, null, this);
    }

    public void refreshIdMethodSpinner() {
        getLoaderManager().restartLoader(Loaders.PH_IDENT_METHODS, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        String[] params;
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

            case Loaders.PLACEHOLDER_HABITATS:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT Min(_id) AS _id, Habitat FROM PlaceHolders "
                    + "WHERE ProjID = ? AND NamerID = ? AND  Habitat LIKE ? "
                    + "GROUP BY Habitat;";
                params = new String[] {"" + mPhProjId, "" + mPhNamerId, mStSearchHabitat + "%"};
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, params, null);
                break;

            case Loaders.PH_IDENT_NAMERS:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT _id, IdNamerName FROM IdNamers "
                        + "UNION SELECT 0, '(add new)' "
                        + "ORDER BY _id;";
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, null, null);
                break;

            case Loaders.PH_IDENT_REFS:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT _id, IdRef FROM IdRefs "
                        + "UNION SELECT 0, '(add new)' "
                        + "ORDER BY _id;";
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, null, null);
                break;

            case Loaders.PH_IDENT_METHODS:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT _id, IdMethod FROM IdMethods "
                        + "UNION SELECT 0, '(add new)' "
                        + "ORDER BY _id;";
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, null, null);
                break;

            case Loaders.PH_IDENT_CONFIDENCS:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT _id, IdLevelDescr FROM IdLevels "
                        + "ORDER BY _id;";
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, null, null);
                break;

            case Loaders.PH_IDENT_SPECIES:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, "
                            + "Code || ': ' || Genus || "
                            + "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
                            + "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
                            + "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
                            + "AS MatchTxt FROM NRCSSpp "
                            + "WHERE Code LIKE ? "
                            + "ORDER BY Code;";
                params = new String[] {mStSearch + "%"};
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, params, null);
                break;

            case Loaders.PLACEHOLDER_USAGE:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT PlaceHolderCode FROM PlaceHolders "
                        + "WHERE ProjID = ? AND NamerID = ? AND _id != ?";
                select = "SELECT VegItems.OrigCode "
                        + "FROM VegItems LEFT JOIN Visits "
                        + "ON VegItems.VisitID = Visits._id "
                        + "WHERE (((VegItems.OrigCode)=?)"
                        + "AND ((Visits.ProjID)=?)"
                        + "AND ((Visits.NamerID)=?));";
                cl = new CursorLoader(getActivity(), baseUri, null, select,
                        new String[] {mPlaceholderCode, "" + mPhProjId, "" + mPhNamerId}, null);
                break;        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        // there will be various loaders, switch them out here
        long rowCt = c.getCount();
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        switch (loader.getId()) {

            case Loaders.PLACEHOLDER_TO_EDIT:
               if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, PLACEHOLDER_TO_EDIT, records: " + c.getCount());
                if (c.moveToFirst()) {
                    mViewPlaceholderCode.setText(c.getString(c.getColumnIndexOrThrow("PlaceHolderCode")));
                    mViewPlaceholderDescription.setText(c.getString(c.getColumnIndexOrThrow("Description")));
                    mAutoCompletePlaceholderHabitat.setText(c.getString(c.getColumnIndexOrThrow("Habitat")));
                    if (!(c.isNull(c.getColumnIndexOrThrow("LabelNum")))) {
                        mViewPlaceholderIdentifier.setText(c.getString(c.getColumnIndexOrThrow("LabelNum")));
                    }

                    if (c.isNull(c.getColumnIndexOrThrow("IdSppCode"))) {
                        // absence of this code means a Placeholder is NOT identified, use defaults
                        setSpinnerSelectionFromDefault(mIdentNamerSpinner);
                        setSpinnerSelectionFromDefault(mIdentRefSpinner);
                        setSpinnerSelectionFromDefault(mIdentMethodSpinner);
                        setSpinnerSelectionFromDefault(mIdentCFSpinner);
                    } else { // "IdSppCode" not null
                        // presence of this code is the essential criteria that a Placeholder is identified
                        // however, the other fields must be present or the ident is not valid
                        mPhIdentSppCode = c.getString(c.getColumnIndexOrThrow("IdSppCode"));
                        if (c.isNull(c.getColumnIndexOrThrow("IdSppDescription"))) {
                            mPhIdentSppDescr = null;
                        } else {
                            mPhIdentSppDescr = c.getString(c.getColumnIndexOrThrow("IdSppDescription"));
                        }
                        if (c.isNull(c.getColumnIndexOrThrow("IdNamerID"))) {
                            mIdentNamerId = 0;
                        } else {
                            mIdentNamerId = c.getLong(c.getColumnIndexOrThrow("IdNamerID"));
                        }
                        if (c.isNull(c.getColumnIndexOrThrow("IdRefID"))) {
                            mIdentRefId = 0;
                        } else {
                            mIdentRefId = c.getLong(c.getColumnIndexOrThrow("IdRefID"));
                        }
                        if (c.isNull(c.getColumnIndexOrThrow("IdMethodID"))) {
                            mIdentMethodId = 0;
                        } else {
                            mIdentMethodId = c.getLong(c.getColumnIndexOrThrow("IdMethodID"));
                        }
                        if (c.isNull(c.getColumnIndexOrThrow("IdLevelID"))) {
                            mIdentCFId = 0;
                        } else {
                            mIdentCFId = c.getLong(c.getColumnIndexOrThrow("IdLevelID"));
                        }
                        if (!(c.isNull(c.getColumnIndexOrThrow("IdNotes")))) {
                            mViewPlaceholderIdentNotes.setText(c.getString(c.getColumnIndexOrThrow("IdNotes")));
                        }
                        if (c.isNull(c.getColumnIndexOrThrow("IdNotes"))) {
                            mPhIdentNotes = null;
                        } else {
                            mPhIdentNotes = c.getString(c.getColumnIndexOrThrow("IdNotes"));
                        }
                         if ((mIdentNamerId == 0) ||
                                 (mIdentRefId == 0) ||
                                 (mIdentMethodId == 0) ||
                                 (mIdentCFId == 0)) {
                             // do not fill in code, will null other fields on next save
                             mSppIdentAutoComplete.setText("");
                             setSpinnerSelectionFromDefault(mIdentNamerSpinner);
                             setSpinnerSelectionFromDefault(mIdentRefSpinner);
                             setSpinnerSelectionFromDefault(mIdentMethodSpinner);
                             setSpinnerSelectionFromDefault(mIdentCFSpinner);
                         } else { // fill in the code, and other fields, from saved values
                             String spp = mPhIdentSppCode;
                             // there will normally be a Description, but not required
                             if (mPhIdentSppDescr != null) {
                                 spp = spp + ": " + mPhIdentSppDescr;
                             }
                             mSppIdentAutoComplete.setText(spp);
                             setSpinnerSelection(mIdentNamerSpinner, mIdentNamerId);
                             mNamerViewGroup.bringChildToFront(mIdentNamerSpinner);
                             setSpinnerSelection(mIdentRefSpinner, mIdentRefId);
                             mRefViewGroup.bringChildToFront(mIdentRefSpinner);
                             setSpinnerSelection(mIdentMethodSpinner, mIdentMethodId);
                             mMethodViewGroup.bringChildToFront(mIdentMethodSpinner);
                             setSpinnerSelection(mIdentCFSpinner, mIdentCFId);
                             if (!(c.isNull(c.getColumnIndexOrThrow("IdNotes")))) {
                                 mViewPlaceholderIdentNotes.setText(c.getString(c.getColumnIndexOrThrow("IdNotes")));
                             }
                         }
                    }
                    // maintain the ident timestamp fields independently of the other ident fields
                    // e.g. "TimeIdFirstEntered" and "TimeIdLastWorkedOn" retain valid timestamps
                    // even if the other ident fields are wiped away as invalid
                    // "TimeIdFirstEntered", if present, flags that there has been an ident
                    // and therefore to leave this field as it is on the next Save
                    if (!(c.isNull(c.getColumnIndexOrThrow("TimeIdFirstEntered")))) {
                        mPhIdentTimeStamp = c.getString(c.getColumnIndexOrThrow("TimeIdFirstEntered"));
                    }
                } else { // no record to edit yet, set up new record
                    mViewPlaceholderCode.setText(mPlaceholderCode);
                    mViewPlaceholderCode.setFocusableInTouchMode(true); // allow editing the code
                    // for now, only allow editing the code here, may allow other places later
                }
                break;

            case Loaders.PLACEHOLDERS_EXISTING:
                mExistingPlaceholderCodes.clear();
                while (c.moveToNext()) {
                   if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, add to HashMap: " + c.getString(c.getColumnIndexOrThrow("PlaceHolderCode")));
                    mExistingPlaceholderCodes.add(c.getString(c.getColumnIndexOrThrow("PlaceHolderCode")));
                }
               if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, number of items in mExistingPlaceholderCodes: " + mExistingPlaceholderCodes.size());
               if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, items in mExistingPlaceholderCodes: " + mExistingPlaceholderCodes.toString());
                break;

            case Loaders.PLACEHOLDER_HABITATS:
//                if (rowCt > 0) {
                mPhHabitatAdapter.setStringConversionColumn(c.getColumnIndexOrThrow("Habitat"));
                mPhHabitatAdapter.swapCursor(c);
                break;

            case Loaders.PH_IDENT_NAMERS:
                // Swap the new cursor in.
                // The framework will take care of closing the old cursor once we return.
                mIdentNamerAdapter.swapCursor(c);
                if (rowCt == 0) {
                    mIdentNamerSpinner.setEnabled(false);
                } else {
                    long identNamerId = sharedPref.getLong(Prefs.DEFAULT_IDENT_NAMER_ID, 0);
                    if (rowCt == 1) { // only the "add new" record
                        mIdentNamerSpinner.setSelection(0);
                        // user sees '(add new)', blank TextView receives click;
                        mNamerViewGroup.bringChildToFront(mLblIdentNamerSpinnerCover);
                    } else {
                        if (sharedPref.contains(Prefs.DEFAULT_IDENT_NAMER_ID)) {
                            setSpinnerSelection(mIdentNamerSpinner, identNamerId);
                            // user can operate the spinner
                            mNamerViewGroup.bringChildToFront(mIdentNamerSpinner);
                        }
                    }
                    mIdentNamerSpinner.setEnabled(true);
                }
                break;

            case Loaders.PH_IDENT_REFS:
                // Swap the new cursor in.
                // The framework will take care of closing the old cursor once we return.
                mIdentRefAdapter.swapCursor(c);
                if (rowCt == 0) {
                    mIdentRefSpinner.setEnabled(false);
                } else {
                    long identRefId = sharedPref.getLong(Prefs.DEFAULT_IDENT_REF_ID, 0);
                    if (rowCt == 1) { // only the "add new" record
                        mIdentRefSpinner.setSelection(0);
                        // user sees '(add new)', blank TextView receives click;
                        mRefViewGroup.bringChildToFront(mLblIdentRefSpinnerCover);
                    } else {
                        if (sharedPref.contains(Prefs.DEFAULT_IDENT_REF_ID)) {
                            setSpinnerSelection(mIdentRefSpinner, identRefId);
                            // user can operate the spinner
                            mRefViewGroup.bringChildToFront(mIdentRefSpinner);
                        }
                    }
                    mIdentRefSpinner.setEnabled(true);
                }
                break;

            case Loaders.PH_IDENT_METHODS:
                // Swap the new cursor in.
                // The framework will take care of closing the old cursor once we return.
                mIdentMethodAdapter.swapCursor(c);
                if (rowCt == 0) {
                    mIdentMethodSpinner.setEnabled(false);
                } else {
                    long identMethodId = sharedPref.getLong(Prefs.DEFAULT_IDENT_METHOD_ID, 0);
                    if (rowCt == 1) { // only the "add new" record
                        mIdentMethodSpinner.setSelection(0);
                        // user sees '(add new)', blank TextView receives click;
                        mMethodViewGroup.bringChildToFront(mLblIdentMethodSpinnerCover);
                    } else {
                        if (sharedPref.contains(Prefs.DEFAULT_IDENT_REF_ID)) {
                            setSpinnerSelection(mIdentMethodSpinner, identMethodId);
                            // user can operate the spinner
                            mMethodViewGroup.bringChildToFront(mIdentMethodSpinner);
                        }
                    }
                    mIdentMethodSpinner.setEnabled(true);
                }
                break;

            case Loaders.PH_IDENT_CONFIDENCS:
                // Swap the new cursor in.
                // The framework will take care of closing the old cursor once we return.
                mIdentCFAdapter.swapCursor(c);
                if (rowCt == 0) { // would not happen unless tables are hacked & items deleted
                    mIdentCFSpinner.setEnabled(false);
                } else {
                    mIdentCFSpinner.setSelection(0); // default is always 'no doubt...'
                    mIdentCFSpinner.setEnabled(true);
                }
                break;

            case Loaders.PH_IDENT_SPECIES:
                // Swap the new cursor in.
                // The framework will take care of closing the old cursor once we return.
//                mIdentSppAdapter.setStringConversionColumn(c.getColumnIndexOrThrow("MatchTxt"));
                if (!c.isClosed()) {
                    mSppIdentCursor = c; // save a global reference
                    mSppIdentResultsAdapter.swapCursor(c);
                }
                break;

            case Loaders.PLACEHOLDER_USAGE:
                if (rowCt == 0) { // if Placeholde code is not yet used anywhere in the data
                    mViewPlaceholderCode.setFocusableInTouchMode(true); // allow editing the code
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed. Need to make sure it is no longer is use.
        switch (loader.getId()) {

            case Loaders.PLACEHOLDER_TO_EDIT:
               if (LDebug.ON) Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDER_TO_EDIT.");
    //			don't need to do anything here, no cursor adapter
                break;

            case Loaders.PLACEHOLDERS_EXISTING:
               if (LDebug.ON) Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDERS_EXISTING.");
    //			don't need to do anything here, no cursor adapter
                break;

            case Loaders.PLACEHOLDER_HABITATS:
                mPhHabitatAdapter.swapCursor(null);
                break;

            case Loaders.PH_IDENT_NAMERS:
                mIdentNamerAdapter.swapCursor(null);
                break;

            case Loaders.PH_IDENT_REFS:
                mIdentRefAdapter.swapCursor(null);
                break;

            case Loaders.PH_IDENT_METHODS:
                mIdentMethodAdapter.swapCursor(null);
                break;

            case Loaders.PH_IDENT_CONFIDENCS:
                mIdentCFAdapter.swapCursor(null);
                break;

            case Loaders.PH_IDENT_SPECIES:
                mSppIdentResultsAdapter.swapCursor(null);
                break;

            case Loaders.PLACEHOLDER_USAGE:
                break;
        }
    }


    public void setSpinnerSelectionFromDefault(Spinner spn) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        ViewGroup vg = null; // a possible viewgroup that pairs a spinner with its cover
        TextView spnCover = null; // a possible cover for the spinner to receive clicks
        long itemId = 0; // if none yet, use _id = 0, generated in query as '(add new)'
        if (spn.getId() == mIdentNamerSpinner.getId()) {
            itemId = sharedPref.getLong(Prefs.DEFAULT_IDENT_NAMER_ID, 0);
            vg = mNamerViewGroup;
            spnCover = mLblIdentNamerSpinnerCover;
        }
        if (spn.getId() == mIdentRefSpinner.getId()) {
            itemId = sharedPref.getLong(Prefs.DEFAULT_IDENT_REF_ID, 0);
            vg = mRefViewGroup;
            spnCover = mLblIdentRefSpinnerCover;
        }
        if (spn.getId() == mIdentMethodSpinner.getId()) {
            itemId = sharedPref.getLong(Prefs.DEFAULT_IDENT_METHOD_ID, 0);
            vg = mMethodViewGroup;
            spnCover = mLblIdentMethodSpinnerCover;
        }
        if (spn.getId() == mIdentCFSpinner.getId()) {
            itemId = 1;
        }
        setSpinnerSelection(spn, itemId);
        if (vg != null) { // if null, no spinner/cover pair and so nothing to do
            if ((itemId == 0) && (spn.getCount() == 1)) {
                // user sees '(add new)', blank TextView receives click;
                vg.bringChildToFront(spnCover);
            } else {
                // user can operate the spinner
                vg.bringChildToFront(spn);
            }
        }

    }

    public void setSpinnerSelection(Spinner spn, long recId) {
        long rowCt = spn.getCount();
        for (int i=0; i<rowCt; i++) {
            if (spn.getItemIdAtPosition(i) == recId) {
                spn.setSelection(i);
            }
        }
    }

    private boolean validatePlaceholder() {
        // validate all items on the screen the user can see
        Context c = getActivity();
        String stringProblem;
        String errTitle = c.getResources().getString(R.string.placeholder_validate_generic_title);
        ConfigurableMsgDialog flexErrDlg = new ConfigurableMsgDialog();
        mValues.clear(); // build up mValues while validating; if returns true all members are good
        mPlaceholderCode = mViewPlaceholderCode.getText().toString().trim();
        if (mPlaceholderCode.length() == 0) {
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
        if (!(mPlaceholderCode.length() >= 3)) {
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

        if (mPlaceholderCode.matches(VNRegex.NRCS_CODE)) {
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

        if (mExistingPlaceholderCodes.contains(mPlaceholderCode)) {
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
        mValues.put("PlaceHolderCode", mPlaceholderCode);

        mPlaceholderDescription = mViewPlaceholderDescription.getText().toString().trim();
        if (mPlaceholderDescription.length() == 0) {
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

        if (!(mPlaceholderDescription.length() >= 3)) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.placeholder_validate_description_short);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_ph_drscr_short");
                    mViewPlaceholderDescription.requestFocus();
                }
            }
            return false;
        }

        mValues.put("Description", mPlaceholderDescription); // Description is OK, store it

        // Habitat is required
        mPlaceholderHabitat = mAutoCompletePlaceholderHabitat.getText().toString().trim();
        if (mPlaceholderHabitat.length() == 0) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.placeholder_validate_habitat_none);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_ph_habitat_none");
                    mAutoCompletePlaceholderHabitat.requestFocus();
                }
            }
            return false;
        }
        if (!(mPlaceholderHabitat.length() >= 3)) {
            if (mValidationLevel > Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.placeholder_validate_habitat_short);
                if (mValidationLevel == Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_ph_habitat_short");
                    mAutoCompletePlaceholderHabitat.requestFocus();
                }
            }
            return false;
        }
        mValues.put("Habitat", mPlaceholderHabitat);

        // LabelNum is optional, put as-is or Null if missing
        mPlaceholderLabelNumber = mViewPlaceholderIdentifier.getText().toString().trim();
        if (mPlaceholderLabelNumber.length() == 0) {
            mValues.putNull("LabelNum");
        } else {
            mValues.put("LabelNum", mPlaceholderLabelNumber);
        }

        // following ident components are only serviced on an Update, record ID != 0,
        // and when in Ident mode
        if ((mIdPlaceholder) && (mPlaceholderId != 0)) {
            // this will always be an Update, not a New Record
            // continue if the species code is like an NRCS code...
            String sppIdent = mSppIdentAutoComplete.getText().toString().trim();
            // if exists, will usually be combined code & sciname, like "JUCO5: Juniperus communis..."
            if (sppIdent.contains(":")) {
                String[] segs = sppIdent.split(":");
                mPhIdentSppCode = segs[0];
                mPhIdentSppDescr = segs[1].trim();
            } else { // allow a raw NRCS code
                mPhIdentSppCode = sppIdent;
            }
            // ... and all the spinners are set
            mIdentNamerId = mIdentNamerSpinner.getSelectedItemId();
            mIdentRefId = mIdentRefSpinner.getSelectedItemId();
            mIdentMethodId = mIdentMethodSpinner.getSelectedItemId();
            mIdentCFId = mIdentCFSpinner.getSelectedItemId();

            if ((!mPhIdentSppCode.matches(VNRegex.NRCS_CODE)) ||
                    (mIdentNamerId == 0) || (mIdentNamerId == AdapterView.INVALID_ROW_ID) ||
                    (mIdentRefId == 0) || (mIdentRefId == AdapterView.INVALID_ROW_ID) ||
                    (mIdentMethodId == 0) || (mIdentMethodId == AdapterView.INVALID_ROW_ID) ||
                    (mIdentCFId == 0) || (mIdentCFId == AdapterView.INVALID_ROW_ID)) {
                // ident is not valid, null out all the ident fields
                mValues.putNull("IdSppCode");
                mValues.putNull("IdSppDescription");
                mValues.putNull("IdNamerID");
                mValues.putNull("IdRefID");
                mValues.putNull("IdMethodID");
                mValues.putNull("IdLevelID");
                mValues.putNull("IdNotes");
                if (mValidationLevel > Validation.SILENT) {
                    // test all the errors, but show the user only the latest one on each try
                    stringProblem = c.getResources().getString(R.string.placeholder_validate_ident_bad_generic);
                    if ((mIdentCFId == 0) || (mIdentCFId == AdapterView.INVALID_ROW_ID)) {
                        stringProblem = c.getResources().getString(R.string.placeholder_validate_ident_bad_level);
                    }
                    if ((mIdentMethodId == 0) || (mIdentMethodId == AdapterView.INVALID_ROW_ID)) {
                        stringProblem = c.getResources().getString(R.string.placeholder_validate_ident_bad_method);
                    }
                    if ((mIdentRefId == 0) || (mIdentRefId == AdapterView.INVALID_ROW_ID)) {
                        stringProblem = c.getResources().getString(R.string.placeholder_validate_ident_bad_ref);
                    }
                    if ((mIdentNamerId == 0) || (mIdentNamerId == AdapterView.INVALID_ROW_ID)) {
                        stringProblem = c.getResources().getString(R.string.placeholder_validate_ident_bad_namer);
                    }
                    if (!mPhIdentSppCode.matches(VNRegex.NRCS_CODE)) {
                        stringProblem = c.getResources().getString(R.string.placeholder_validate_ident_bad_code);
                    }
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
                // do not break; continue and update record with these null fields
            } else { // put the values to update
                mValues.put("IdSppCode", mPhIdentSppCode);
                if (mPhIdentSppDescr == null) {
                    mValues.putNull("IdSppDescription");
                } else {
                    mValues.put("IdSppDescription", mPhIdentSppDescr);
                }
                mValues.put("IdNamerID", mIdentNamerId);
                mValues.put("IdRefID", mIdentRefId);
                mValues.put("IdMethodID", mIdentMethodId);
                mValues.put("IdLevelID", mIdentCFId);
                mPhIdentNotes = mViewPlaceholderIdentNotes.getText().toString().trim();
                if (mPhIdentNotes.length() == 0) {
                    mValues.putNull("IdNotes");
                } else {
                    mValues.put("IdNotes", mPhIdentNotes);
                }
            }
            // maintain the ident timestamp fields independently of the other ident fields
            // e.g. "TimeIdFirstEntered" and "TimeIdLastWorkedOn" retain valid timestamps
            // even if the other ident fields are wiped away as invalid
            // this can give clues to previous work and errors
            // "mPhIdentTimeStamp", globally stored from any previous "TimeIdFirstEntered", flags
            // that there has been an ident and therefore to leave this field as it is
            if (mPhIdentTimeStamp == null) {
                mValues.put("TimeIdFirstEntered", mTimeFormat.format(new Date()));
            }
            // always update this one, even if the rest of the ident is being cleared
            mValues.put("TimeIdLastWorkedOn", mTimeFormat.format(new Date()));
        }
        return true;
    }

    private int savePlaceholderRecord() {
        int numUpdated = 0;
        if (!validatePlaceholder()) {
           if (LDebug.ON) Log.d(LOG_TAG, "Failed validation in savePlaceholderRecord; mValues: " + mValues.toString());
            return numUpdated;
        }
        ContentResolver rs = getActivity().getContentResolver();
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        if (mPlaceholderId == 0) { // new record
           if (LDebug.ON) Log.d(LOG_TAG, "savePlaceholderRecord; creating new record with mPlaceholderId = " + mPlaceholderId);
            // fill in fields the user never sees
            mValues.put("TimeFirstInput", mTimeFormat.format(new Date()));
            mValues.put("TimeLastEdited", mTimeFormat.format(new Date()));
            mValues.put("VisitIdWhereFirstFound", sharedPref.getLong(Prefs.CURRENT_VISIT_ID, 0));
            mValues.put("ProjID", mPhProjId);
            mValues.put("NamerID", mPhNamerId);

            mUri = rs.insert(mPlaceholdersUri, mValues);
           if (LDebug.ON) Log.d(LOG_TAG, "new record in savePlaceholderRecord; returned URI: " + mUri.toString());
            long newRecId = Long.parseLong(mUri.getLastPathSegment());
            if (newRecId < 1) { // returns -1 on error, e.g. if not valid to save because of missing required field
               if (LDebug.ON) Log.d(LOG_TAG, "new record in savePlaceholderRecord has Id == " + newRecId + "); canceled");
                return 0;
            }
            mPlaceholderId = newRecId;
            getLoaderManager().restartLoader(Loaders.PLACEHOLDERS_EXISTING, null, this);

            mUri = ContentUris.withAppendedId(mPlaceholdersUri, mPlaceholderId);
           if (LDebug.ON) Log.d(LOG_TAG, "new record in savePlaceholderRecord; URI re-parsed: " + mUri.toString());
            numUpdated = 1;
            mViewPlaceholderCode.setFocusableInTouchMode(false); // disable editing the code
            try {
                mEditPhListener.onEditPlaceholderComplete(EditPlaceholderFragment.this);
            } catch (Exception e) {
                // ignore; fn is just to refresh the search text if Ph code edited
            }
        } else { // update the existing record
           if (LDebug.ON) Log.d(LOG_TAG, "savePlaceholderRecord; updating existing record with mVisitId = " + mPlaceholderId);
            mValues.put("TimeLastEdited", mTimeFormat.format(new Date())); // update the last-changed time
            mUri = ContentUris.withAppendedId(mPlaceholdersUri, mPlaceholderId);
            numUpdated = rs.update(mUri, mValues, null, null);
           if (LDebug.ON) Log.d(LOG_TAG, "Updated record in savePlaceholderRecord; numUpdated: " + numUpdated);
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

    private void configureIdViews() {

        String btnMsg;
        // hide or show the views that involve identifying a Placeholder
        if (mIdPlaceholder) {
            btnMsg = getActivity().getResources().getString(R.string.edit_placeholder_ident_button_on_msg);
            mViewGroupIdent.setVisibility(View.VISIBLE);
        } else { // default, mIdPlaceholder = false
            btnMsg = getActivity().getResources().getString(R.string.edit_placeholder_ident_button_off_msg);
            mViewGroupIdent.setVisibility(View.GONE);
        }
        mBtnIdent.setText(btnMsg);
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

        // workaround for spinner firing when first set
        if(((String)parent.getTag()).equalsIgnoreCase(VNContract.Tags.SPINNER_FIRST_USE)) {
            parent.setTag("");
            return;
        }

        // sort out the spinners
        // can't use switch because not constants
        if (parent.getId() == mIdentNamerSpinner.getId()) {
            mIdentNamerId = id;
            if (mIdentNamerId == 0) { // picked '(add new)'
               if (LDebug.ON) Log.d(LOG_TAG, "Starting 'add new' for IdentNamer from onItemSelect");
                ConfigurableEditDialog newIdNamerDlg =
                        ConfigurableEditDialog.newInstance(getArgsForNewIdentNamer());
                newIdNamerDlg.show(getFragmentManager(), "frg_new_idnamer_fromSpinner");
            } else {
                saveDefaultItemId(Prefs.DEFAULT_IDENT_NAMER_ID, mIdentNamerId);
            }
            // in either case, reset selection
            setSpinnerSelectionFromDefault(mIdentNamerSpinner);
        }

        if (parent.getId() == mIdentRefSpinner.getId()) {
            mIdentRefId = id;
            if (mIdentRefId == 0) { // picked '(add new)'
               if (LDebug.ON) Log.d(LOG_TAG, "Starting 'add new' for IdentRef from onItemSelect");
                ConfigurableEditDialog newIdRefDlg = ConfigurableEditDialog.newInstance(getArgsForNewIdentRef());
                newIdRefDlg.show(getFragmentManager(), "frg_new_idref_fromSpinner");
            } else {
                saveDefaultItemId(Prefs.DEFAULT_IDENT_REF_ID, mIdentRefId);
            }
            // in either case, reset selection
            setSpinnerSelectionFromDefault(mIdentRefSpinner);
        }

        if (parent.getId() == mIdentMethodSpinner.getId()) {
            mIdentMethodId = id;
            if (mIdentMethodId == 0) { // picked '(add new)'
               if (LDebug.ON) Log.d(LOG_TAG, "Starting 'add new' for IdentMethod from onItemSelect");
                ConfigurableEditDialog newIdMethodDlg = ConfigurableEditDialog.newInstance(getArgsForNewIdentMethod());
                newIdMethodDlg.show(getFragmentManager(), "frg_new_idmethod_fromSpinner");
            } else {
                saveDefaultItemId(Prefs.DEFAULT_IDENT_METHOD_ID, mIdentMethodId);
            }
            // in either case, reset selection
            setSpinnerSelectionFromDefault(mIdentMethodSpinner);
        }
    }

    private Bundle getArgsForNewIdentNamer () {
        Bundle args = new Bundle();
        Context c = getActivity();
        args.putLong(ConfigurableEditDialog.ITEM_REC_ID, 0);
        args.putString(ConfigurableEditDialog.DIALOG_TITLE,
                c.getResources().getString(R.string.edit_placeholder_ident_namer_title_new));
        args.putString(ConfigurableEditDialog.DIALOG_MESSAGE,
                c.getResources().getString(R.string.edit_placeholder_ident_namer_msg_new));
        args.putInt(ConfigurableEditDialog.ITEM_INPUT_TYPE_CODE,
                InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        args.putString(ConfigurableEditDialog.ITEM_ERR_MISSING,
                c.getResources().getString(R.string.edit_placeholder_ident_namer_err_missing));
        args.putString(ConfigurableEditDialog.ITEM_ERR_SHORT,
                c.getResources().getString(R.string.edit_placeholder_ident_namer_err_short));
        args.putString(ConfigurableEditDialog.ITEM_ERR_DUP,
                c.getResources().getString(R.string.edit_placeholder_ident_namer_err_dup));
        args.putString(ConfigurableEditDialog.ITEM_DB_FIELD, "IdNamerName");
        args.putString(ConfigurableEditDialog.ITEM_URI_TARGET, "idnamers");
        return args;
    }

    private Bundle getArgsForNewIdentRef () {
        Bundle args = new Bundle();
        Context c = getActivity();
        args.putLong(ConfigurableEditDialog.ITEM_REC_ID, 0);
        args.putString(ConfigurableEditDialog.DIALOG_TITLE,
                c.getResources().getString(R.string.edit_placeholder_ident_ref_title_new));
        args.putString(ConfigurableEditDialog.DIALOG_MESSAGE,
                c.getResources().getString(R.string.edit_placeholder_ident_ref_msg_new));
        args.putString(ConfigurableEditDialog.ITEM_ERR_MISSING,
                c.getResources().getString(R.string.edit_placeholder_ident_ref_err_missing));
        args.putString(ConfigurableEditDialog.ITEM_ERR_SHORT,
                c.getResources().getString(R.string.edit_placeholder_ident_ref_err_short));
        args.putString(ConfigurableEditDialog.ITEM_ERR_DUP,
                c.getResources().getString(R.string.edit_placeholder_ident_ref_err_dup));
        args.putString(ConfigurableEditDialog.ITEM_DB_FIELD, "IdRef");
        args.putString(ConfigurableEditDialog.ITEM_URI_TARGET, "idrefs");
        return args;
    }


    private Bundle getArgsForNewIdentMethod () {
        Bundle args = new Bundle();
        Context c = getActivity();
        args.putLong(ConfigurableEditDialog.ITEM_REC_ID, 0);
        args.putString(ConfigurableEditDialog.DIALOG_TITLE,
                c.getResources().getString(R.string.edit_placeholder_ident_method_title_new));
        args.putString(ConfigurableEditDialog.DIALOG_MESSAGE,
                c.getResources().getString(R.string.edit_placeholder_ident_method_msg_new));
        args.putString(ConfigurableEditDialog.ITEM_ERR_MISSING,
                c.getResources().getString(R.string.edit_placeholder_ident_method_err_missing));
        args.putString(ConfigurableEditDialog.ITEM_ERR_SHORT,
                c.getResources().getString(R.string.edit_placeholder_ident_method_err_short));
        args.putString(ConfigurableEditDialog.ITEM_ERR_DUP,
                c.getResources().getString(R.string.edit_placeholder_ident_method_err_dup));
        args.putString(ConfigurableEditDialog.ITEM_DB_FIELD, "IdMethod");
        args.putString(ConfigurableEditDialog.ITEM_URI_TARGET, "idmethods");
        return args;
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(!hasFocus) { // something lost focus
            mValues.clear();
            switch (v.getId()) {
            case R.id.txt_placeholder_code:
            case R.id.txt_placeholder_description:
            case R.id.autocomplete_placeholder_habitat:
            case R.id.txt_placeholder_labelnumber:
                mValidationLevel = Validation.SILENT; // save if possible, but do not notify
                int numUpdated = savePlaceholderRecord();
                if (numUpdated == 0) {
                   if (LDebug.ON) Log.d(LOG_TAG, "Failed to save record in onFocusChange; mValues: " + mValues.toString());
                } else {
                   if (LDebug.ON) Log.d(LOG_TAG, "Saved record in onFocusChange; mValues: " + mValues.toString());
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
        case R.id.autocomplete_placeholder_habitat:
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
    Tracker headerContextTracker = ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);

    switch (item.getItemId()) {
    case R.id.placeholder_code_help:
       if (LDebug.ON) Log.d(LOG_TAG, "'Placeholder Code Help' selected");
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
       if (LDebug.ON) Log.d(LOG_TAG, "'Placeholder Description Help' selected");
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
       if (LDebug.ON) Log.d(LOG_TAG, "'Placeholder Habitat Help' selected");
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
       if (LDebug.ON) Log.d(LOG_TAG, "'Placeholder Label Number Help' selected");
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