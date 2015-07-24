package com.vegnab.vegnab;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.internal.widget.AdapterViewCompat.AdapterContextMenuInfo;
import android.text.Editable;
import android.text.InputType;
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
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.Prefs;
import com.vegnab.vegnab.database.VNContract.Validation;
import com.vegnab.vegnab.database.VNContract.VNRegex;

import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.HashSet;
import java.util.Locale;

public class EditPlaceholderFragment extends Fragment implements OnClickListener,
        android.widget.AdapterView.OnItemSelectedListener,
        View.OnFocusChangeListener,
        LoaderManager.LoaderCallbacks<Cursor> {

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
            mStSearchHabitat = "", mStSearch = "",
            mPhVisitName = null, mPhNamerName = null,
            mPhScribe = null, mPhLocText = null;
    Boolean mCodeWasShortened = false, mIdPlaceholder = false;
    HashSet<String> mExistingPlaceholderCodes = new HashSet<String>();

    private Button mBtnIdent;
    private Spinner mIdentNamerSpinner, mIdentRefSpinner, mIdentMethodSpinner, mIdentCFSpinner;
    private TextView mLblIdentNamerSpinnerCover, mLblIdentRefSpinnerCover, mLblIdentMethodSpinnerCover;
    SimpleCursorAdapter mPhHabitatAdapter, mIdentNamerAdapter, mIdentRefAdapter, mIdentMethodAdapter, mIdentCFAdapter;

    private ViewGroup mViewGroupIdent; // the set of views involved with identify-species

    Uri mUri;
    Uri mPlaceholdersUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "placeholders");
    ContentValues mValues = new ContentValues();

    private EditText mViewPlaceholderCode, mViewPlaceholderDescription,
            mViewPlaceholderIdentifier, mPhIdentNotes;
    AutoCompleteTextView mAutoCompletePlaceholderHabitat, mSppIdentAutoComplete;

    SelSppIdentAdapter mSppIdentResultsAdapter;
    Cursor mSppIdentCursor;

    SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    // explicitly save/retrieve all these through Bundles, some versions have bugs that lose cursor
    final static String ARG_PLACEHOLDER_ID = "placeholderId";
    final static String ARG_PLACEHOLDER_CODE = "placeholderCode";
    final static String ARG_CODE_WAS_SHORTENED = "phCodeShortened";
    final static String ARG_PLACEHOLDER_DESCRIPTION = "placeholderDescription";
    final static String ARG_PLACEHOLDER_HABITAT = "placeholderHabitat";
    final static String ARG_PLACEHOLDER_LABELNUMBER = "placeholderLabelnumber";
    final static String ARG_PH_VISIT_NAME = "phVisitName";
    final static String ARG_PH_LOCID = "phLocId";
    final static String ARG_PH_LOC_TEXT = "phLocText";
    final static String ARG_PH_NAMER_NAME = "phNamerName";
    final static String ARG_PH_SCRIBE = "phScribe";
    final static String BUTTON_KEY = "buttonKey";
    final static String ARG_ID_PLACEHOLDER = "identifyPh";
    final static String ARG_IDENT_NAMER_ID = "identNamerId";
    final static String ARG_IDENT_REF_ID = "identRefId";
    final static String ARG_IDENT_METHOD_ID = "identMethodId";
    final static String ARG_IDENT_CF_ID = "identCFId";

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
            Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "', length: " + s.length());
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
            Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "', length: " + s.length());
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
            mPhLocId = savedInstanceState.getLong(ARG_PH_LOCID, 0);
            mPhVisitName = savedInstanceState.getString(ARG_PH_VISIT_NAME);
            mPhLocText = savedInstanceState.getString(ARG_PH_LOC_TEXT);
            mPhNamerName = savedInstanceState.getString(ARG_PH_NAMER_NAME);
            mPhScribe = savedInstanceState.getString(ARG_PH_SCRIBE);
            mIdPlaceholder = savedInstanceState.getBoolean(ARG_ID_PLACEHOLDER);
            mIdentNamerId = savedInstanceState.getLong(ARG_IDENT_NAMER_ID, 0);
            mIdentRefId = savedInstanceState.getLong(ARG_IDENT_REF_ID, 0);
            mIdentMethodId = savedInstanceState.getLong(ARG_IDENT_METHOD_ID, 0);
            mIdentCFId = savedInstanceState.getLong(ARG_IDENT_CF_ID, 1);

            Log.d(LOG_TAG, "In onCreateView, retrieved mPlaceholderId: " + mPlaceholderId);
            Log.d(LOG_TAG, "In onCreateView, retrieved mPlaceholderCode: " + mPlaceholderCode);
            Log.d(LOG_TAG, "In onCreateView, retrieved mPhVisitId: " + mPhVisitId);
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
        mLblIdentRefSpinnerCover = (TextView) rootView.findViewById(R.id.lbl_ident_namer_spinner_cover);
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
        mLblIdentMethodSpinnerCover = (TextView) rootView.findViewById(R.id.lbl_ident_namer_spinner_cover);
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

        mPhIdentNotes = (EditText) rootView.findViewById(R.id.txt_ph_ident_notes);

        // the views for identify-species, to show or hide as a group
        mViewGroupIdent = (ViewGroup) rootView.findViewById(R.id.ident_veiw_group);

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
        //  hide views dealing with identifying the species
        configureIdViews();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // assure the container activity has implemented the callback interfaces
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
        outState.putLong(ARG_PH_LOCID, mPhLocId);
        outState.putString(ARG_PH_VISIT_NAME, mPhVisitName);
        outState.putString(ARG_PH_LOC_TEXT, mPhLocText);
        outState.putString(ARG_PH_NAMER_NAME, mPhNamerName);
        outState.putString(ARG_PH_SCRIBE, mPhScribe);
        outState.putBoolean(ARG_ID_PLACEHOLDER, mIdPlaceholder);
        outState.putLong(ARG_IDENT_NAMER_ID, mIdentNamerId);
        outState.putLong(ARG_IDENT_REF_ID, mIdentRefId);
        outState.putLong(ARG_IDENT_METHOD_ID, mIdentMethodId);
        outState.putLong(ARG_IDENT_CF_ID, mIdentCFId);
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
                Log.d(LOG_TAG, "in onClick, placeholder_pix_button");
                placeholderButtonTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Edit Placeholder Event")
                        .setAction("Button click")
                        .setLabel("Pictures button")
                        .setValue(mPlaceholderId)
                        .build());
                if (mPlaceholderId == 0) { // record not defined yet
                    helpTitle = c.getResources().getString(R.string.placeholder_pix_btn_no_descr_title);
                    helpMessage = c.getResources().getString(R.string.placeholder_pix_btn_no_descr_msg);
                    flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
                    flexHlpDlg.show(getFragmentManager(), "frg_ph_pix_not_ready");
                    return;
                }
                args.putInt(BUTTON_KEY, VNContract.PhActions.GO_TO_PICTURES);
                args.putLong(ARG_PLACEHOLDER_ID, mPlaceholderId);
                Log.d(LOG_TAG, "in onClick, about to do 'mButtonCallback.onPlaceholderActionButtonClicked(PICTURES)'");
                mButtonCallback.onPlaceholderActionButtonClicked(args);
                Log.d(LOG_TAG, "in onClick, completed 'mButtonCallback.onPlaceholderActionButtonClicked(PICTURES)'");
                break;

            case R.id.ph_identify_button:
                Log.d(LOG_TAG, "in onClick, ph_identify_button");
                if (mPlaceholderId == 0) {
                    // message that Placeholder must be defined first
                    mIdPlaceholder = false;
                } else {
                    if (mIdPlaceholder) {
                        mIdPlaceholder = false;
                    } else {
                        mIdPlaceholder = true;
                    }
                }
                configureIdViews();
                break;

            case R.id.lbl_ident_namer_spinner_cover:
                Log.d(LOG_TAG, "in onClick, lbl_ident_namer_spinner_cover");
                ConfigurableEditDialog newIdNamerDlg =
                        ConfigurableEditDialog.newInstance(getArgsForNewIdentNamer());
                newIdNamerDlg.show(getFragmentManager(), "frg_new_idnamer_fromCover");
                break;

            case R.id.lbl_ident_ref_spinner_cover:
                Log.d(LOG_TAG, "in onClick, lbl_ident_ref_spinner_cover");
                ConfigurableEditDialog newIdRefDlg =
                        ConfigurableEditDialog.newInstance(getArgsForNewIdentRef());
                newIdRefDlg.show(getFragmentManager(), "frg_new_idref_fromCover");
                break;

            case R.id.lbl_ident_method_spinner_cover:
                Log.d(LOG_TAG, "in onClick, lbl_ident_method_spinner_cover");
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
                            + "AS MatchTxt FROM RegionalSpeciesList "
                            + "WHERE Code LIKE ? "
                            + "ORDER BY Code;";
                params = new String[] {mStSearch + "%"};
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, params, null);
                break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        // there will be various loaders, switch them out here
        long rowCt = c.getCount();
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        switch (loader.getId()) {

            case Loaders.PLACEHOLDER_TO_EDIT:
                Log.d(LOG_TAG, "onLoadFinished, PLACEHOLDER_TO_EDIT, records: " + c.getCount());
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
                         if ((c.isNull(c.getColumnIndexOrThrow("IdNamerId"))) ||
                                 (c.isNull(c.getColumnIndexOrThrow("IdRefId"))) ||
                                 (c.isNull(c.getColumnIndexOrThrow("IdMethodId"))) ||
                                 (c.isNull(c.getColumnIndexOrThrow("IdLevelId")))) {
                             // do not fill in code, will null other fields on next save
                             mSppIdentAutoComplete.setText("");
                             setSpinnerSelectionFromDefault(mIdentNamerSpinner);
                             setSpinnerSelectionFromDefault(mIdentRefSpinner);
                             setSpinnerSelectionFromDefault(mIdentMethodSpinner);
                             setSpinnerSelectionFromDefault(mIdentCFSpinner);
                         } else { // fill in the code, and other fields, from saved values
                             String spp = c.getString(c.getColumnIndexOrThrow("IdSppCode"));
                             // there will normally be a Description, but not required
                             if (!(c.isNull(c.getColumnIndexOrThrow("IdSppDescription")))) {
                                 spp = spp + ": " + (c.getString(c.getColumnIndexOrThrow("IdSppDescription")));
                             }
                             mSppIdentAutoComplete.setText(spp);
                             setSpinnerSelection(mIdentNamerSpinner, c.getLong(c.getColumnIndexOrThrow("IdNamerId")));
                             mIdentNamerSpinner.bringToFront();
                             setSpinnerSelection(mIdentRefSpinner, c.getLong(c.getColumnIndexOrThrow("IdRefId")));
                             mIdentRefSpinner.bringToFront();
                             setSpinnerSelection(mIdentMethodSpinner, c.getLong(c.getColumnIndexOrThrow("IdMethodId")));
                             mIdentMethodSpinner.bringToFront();
                             setSpinnerSelection(mIdentCFSpinner, c.getLong(c.getColumnIndexOrThrow("IdLevelId")));
                         }
                    }
                } else { // no record to edit yet, set up new record
                    mViewPlaceholderCode.setText(mPlaceholderCode);
                }
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
                        mLblIdentNamerSpinnerCover.bringToFront();
                    } else {
                        if (sharedPref.contains(Prefs.DEFAULT_IDENT_NAMER_ID)) {
                            setSpinnerSelection(mIdentNamerSpinner, identNamerId);
                            // user can operate the spinner
                            mIdentNamerSpinner.bringToFront();
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
                        mLblIdentRefSpinnerCover.bringToFront();
                    } else {
                        if (sharedPref.contains(Prefs.DEFAULT_IDENT_REF_ID)) {
                            setSpinnerSelection(mIdentRefSpinner, identRefId);
                            // user can operate the spinner
                            mIdentRefSpinner.bringToFront();
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
                        mLblIdentMethodSpinnerCover.bringToFront();
                    } else {
                        if (sharedPref.contains(Prefs.DEFAULT_IDENT_REF_ID)) {
                            setSpinnerSelection(mIdentMethodSpinner, identMethodId);
                            // user can operate the spinner
                            mIdentMethodSpinner.bringToFront();
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
                if (rowCt > 0) {
                    mSppIdentCursor = c; // save a global reference
                    mSppIdentResultsAdapter.swapCursor(c);
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
                Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDER_TO_EDIT.");
    //			don't need to do anything here, no cursor adapter
                break;

            case Loaders.PLACEHOLDERS_EXISTING:
                Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDERS_EXISTING.");
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
        }
    }


    public void setSpinnerSelectionFromDefault(Spinner spn) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        TextView spnCover = null; // a possible cover for the spinner to receive clicks
        long itemId = 0; // if none yet, use _id = 0, generated in query as '(add new)'
        if (spn.getId() == mIdentNamerSpinner.getId()) {
            itemId = sharedPref.getLong(Prefs.DEFAULT_IDENT_NAMER_ID, 0);
            spnCover = mLblIdentNamerSpinnerCover;
        }
        if (spn.getId() == mIdentRefSpinner.getId()) {
            itemId = sharedPref.getLong(Prefs.DEFAULT_IDENT_REF_ID, 0);
            spnCover = mLblIdentRefSpinnerCover;
        }
        if (spn.getId() == mIdentMethodSpinner.getId()) {
            itemId = sharedPref.getLong(Prefs.DEFAULT_IDENT_METHOD_ID, 0);
            spnCover = mLblIdentMethodSpinnerCover;
        }
        if (spn.getId() == mIdentCFSpinner.getId()) {
            itemId = 1;
        }
        setSpinnerSelection(spn, itemId);
        if ((itemId == 0) && (spn.getCount() == 1) && (spnCover != null)) {
            // user sees '(add new)', blank TextView receives click;
            spnCover.bringToFront();
        } else {
            // user can operate the spinner
            spn.bringToFront();
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
        String stringHabitat = mAutoCompletePlaceholderHabitat.getText().toString().trim();
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
                Log.d(LOG_TAG, "Starting 'add new' for IdentNamer from onItemSelect");
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
                Log.d(LOG_TAG, "Starting 'add new' for IdentRef from onItemSelect");
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
                Log.d(LOG_TAG, "Starting 'add new' for IdentMethod from onItemSelect");
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