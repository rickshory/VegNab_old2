package com.vegnab.vegnab.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.vegnab.vegnab.ConfigurableMsgDialog;
import com.vegnab.vegnab.EditNamerDialog;
import com.vegnab.vegnab.EditPlaceholderFragment;
import com.vegnab.vegnab.EditSppItemDialog;
import com.vegnab.vegnab.HelpUnderConstrDialog;
import com.vegnab.vegnab.MainVNActivity;
import com.vegnab.vegnab.R;
import com.vegnab.vegnab.SelSppItemAdapter;
import com.vegnab.vegnab.UnderConstrDialog;
import com.vegnab.vegnab.VNApplication;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract;
import com.vegnab.vegnab.database.VNContract.LDebug;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.VNConstraints;
import com.vegnab.vegnab.database.VNContract.VNRegex;
import com.vegnab.vegnab.database.VNContract.VegcodeSources;

public class ManagePhsFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = ManagePhsFragment.class.getSimpleName();
    final static String ARG_PROJECT_ID = "projectId";
    final static String ARG_NAMER_ID = "namerId";
    final static String ARG_SEARCH_TEXT = "search_text";

    long mProjectId = 0;
    long mNamerId = 0;
    Cursor mPhsCursor;
    ContentValues mValues = new ContentValues();

    private Spinner mPhNamerSpinner, mPhSortSpinner;
    SimpleCursorAdapter mPhNamerAdapter;
    SelSppItemAdapter mPhResultsAdapter;
    TextView mViewForEmptyList;

    // declare an interface the container Activity must implement
    public interface OnEditPlaceholderListener {
        // methods that must be implemented in the container Activity
        void onEditPlaceholder(Bundle args);
    }
    OnEditPlaceholderListener mEditPlaceholderCallback; // declare the interface

    public interface OnPlaceholderRequestListener {
        // methods that must be implemented in the container Activity
        void onRequestGenerateExistingPlaceholders(Bundle args);
        long onRequestGetCountOfExistingPlaceholders();
        boolean onRequestMatchCheckOfExistingPlaceholders(String ph);
    }
    OnPlaceholderRequestListener mPlaceholderRequestListener;

    long mRowCt;
    String mStSearch = "", mStMatch = "";
    EditText mViewSearchChars;
    CheckBox mCkPhsNotIdd;
//	ListView mSppItemsList;
    TextWatcher sppCodeTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            mCkPhsNotIdd.setChecked(false);
            // use this method; test length of string; e.g. 'count' of other methods does not give this length
            //Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "'");
           if (LDebug.ON) Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "', length: " + s.length());
            mStSearch = s.toString();
            if (mStSearch.trim().length() == 0) {
                mViewForEmptyList.setText(
                        getActivity().getResources().getString(R.string.sel_spp_search_msg_empty_list));
                mPhResultsAdapter.swapCursor(null);
            } else {
                mViewForEmptyList.setText(
                        getActivity().getResources().getString(R.string.sel_spp_search_msg_not_finished));
                mPhResultsAdapter.swapCursor(null);
                getLoaderManager().restartLoader(Loaders.SPP_MATCHES, null, ManagePhsFragment.this);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // the 'count' characters beginning at 'start' are about to be replaced by new text with length 'after'
            //Log.d(LOG_TAG, "beforeTextChanged, s: '" + s.toString() + "', start: " + start + ", count: " + count + ", after: " + after);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // the 'count' characters beginning at 'start' have just replaced old text that had length 'before'
            //Log.d(LOG_TAG, "onTextChanged, s: '" + s.toString() + "', start: " + start + ", before: " + before + ", count: " + count);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // if the activity was re-created (e.g. from a screen rotate)
        // restore the previous screen, remembered by onSaveInstanceState()
        // This is mostly needed in fixed-pane layouts
        if (savedInstanceState != null) {
            // restore search text and any search options
            mStSearch = savedInstanceState.getString(ARG_SEARCH_TEXT);
            mProjectId = savedInstanceState.getLong(ARG_PROJECT_ID);
            mNamerId = savedInstanceState.getLong(ARG_NAMER_ID);
        }
        // inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_manage_phs, container, false);

        mPhNamerSpinner = (Spinner) rootView.findViewById(R.id.sel_namer_spinner);
        mPhNamerSpinner.setTag(VNContract.Tags.SPINNER_FIRST_USE); // flag to catch and ignore erroneous first firing
        mPhNamerSpinner.setEnabled(false); // will enable when data ready
        mPhNamerAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, null,
                new String[] {"NamerName"},
                new int[] {android.R.id.text1}, 0);
        mPhNamerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPhNamerSpinner.setAdapter(mPhNamerAdapter);
        mPhNamerSpinner.setOnItemSelectedListener(this);
        registerForContextMenu(mPhNamerSpinner); // enable long-press
        // Prepare the loader. Either re-connect with an existing one or start a new one
        getLoaderManager().initLoader(Loaders.NAMERS, null, this);

/* may not need cover
        // also need click, if no names & therefore selection cannot be changed
//		mPhNamerSpinner.setOnFocusChangeListener(this); // does not work
        // use a TextView on top of the spinner, named "lbl_spp_namer_spinner_cover"
        mLblNewNamerSpinnerCover = (TextView) rootView.findViewById(R.id.lbl_spp_namer_spinner_cover);
        mLblNewNamerSpinnerCover.setOnClickListener(this);
        registerForContextMenu(mLblNewNamerSpinnerCover); // enable long-press
        // in layout, TextView is in front of Spinner and takes precedence
        // for testing context menu, bring spinner to front so it receives clicks
//		mPhNamerSpinner.bringToFront();
*/

        mPhSortSpinner = (Spinner) rootView.findViewById(R.id.ph_sort_spinner);
        mPhSortSpinner.setTag(VNContract.Tags.SPINNER_FIRST_USE); // flag to catch and ignore erroneous first firing
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> phSortAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.phs_sort_options_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        phSortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mPhSortSpinner.setAdapter(phSortAdapter);

        mViewSearchChars = (EditText) rootView.findViewById(R.id.txt_search_phs);
        mCkPhsNotIdd = (CheckBox) rootView.findViewById(R.id.ck_show_phs_not_idd);

        mViewSearchChars.addTextChangedListener(sppCodeTextWatcher);
        registerForContextMenu(mViewSearchChars); // enable long-press

        mViewForEmptyList = (TextView) rootView.findViewById(android.R.id.empty);

        // use query to return 'MatchTxt', concatenated from code and description; more reading room
        mPhResultsAdapter = new SelSppItemAdapter(getActivity(),
                R.layout.list_spp_search_item, null, 0);
        setListAdapter(mPhResultsAdapter);
        getLoaderManager().initLoader(Loaders.SPP_MATCHES, null, this);

        return rootView;
    }

        @Override
    public void onStart() {
        super.onStart();
        // during startup, check if arguments are passed to the fragment
        // this is where to do this because the layout has been applied
        // to the fragment
        Bundle args = getArguments();

        if (args != null) {
            mStSearch = args.getString(ARG_SEARCH_TEXT, "");
            mProjectId = args.getLong(ARG_PROJECT_ID);
            mNamerId = args.getLong(ARG_NAMER_ID);
        }
        // get the ProjectID and NamerID for this Visit
        getLoaderManager().initLoader(Loaders.VISIT_INFO, null, this);
        // when above is done, will call fetchExistingPlaceholders();

        mViewSearchChars.requestFocus();

        mViewSearchChars.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mViewSearchChars, 0);
            }
        }, 50);

        // set up the list to receive long-press
        registerForContextMenu(getListView());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // assure the container activity has implemented the callback interfaces
        try {
            mEditPlaceholderCallback = (OnEditPlaceholderListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException (activity.toString() + " must implement OnEditPlaceholderListener");
        }
        try {
            mPlaceholderRequestListener = (OnPlaceholderRequestListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException (activity.toString() + " must implement OnPlaceholderRequestListener");
        }
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onResume(){
        if (LDebug.ON) Log.d(LOG_TAG, "in 'onResume'; mPickPlaceholder "
                + (mPickPlaceholder ? "true" : "false") + "; mStSearch: '" + mStSearch + "'" );
        super.onResume();
        mStSearch = mViewSearchChars.getText().toString();
        mPickPlaceholder = mCkPhsNotIdd.isChecked();
        if (LDebug.ON) Log.d(LOG_TAG, "in 'onResume' after super and getText; mPickPlaceholder "
                + (mPickPlaceholder ? "true" : "false") + "; mStSearch: '" + mStSearch + "'" );
        refreshPhsList(); // if Placeholders were IDd, show changes
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save the current search text and any options
        outState.putString(ARG_SEARCH_TEXT, mStSearch);
        outState.putLong(ARG_PROJECT_ID, mProjectId);
        outState.putLong(ARG_NAMER_ID, mNamerId);
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
        if (parent.getId() == mPhNamerSpinner.getId()) {
            // workaround for spinner firing when first set
            if(((String)parent.getTag()).equalsIgnoreCase(VNContract.Tags.SPINNER_FIRST_USE)) {
                parent.setTag("");
                return;
            }
            mNamerId = id;
        }

        if (parent.getId() == mPhSortSpinner.getId()) {
            // workaround for spinner firing when first set
            if(((String)parent.getTag()).equalsIgnoreCase(VNContract.Tags.SPINNER_FIRST_USE)) {
                parent.setTag("");
                return;
            }
        }
        // write code for any other spinner(s) here
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
//        setNamerSpinnerSelectionFromDefaultNamer();
    }

    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
//        Toast.makeText(this.getActivity(), "Clicked position " + pos + ", id " + id, Toast.LENGTH_SHORT).show();
        // check if selected code is in mVegCodesAlreadyOnSubplot
//    	getListView().getItemAtPosition(pos).toString(); // not useful, gets cursor wrapper
        mPhsCursor.moveToPosition(pos);
// available fields: _id, Code, Genus, Species, SubsppVar, Vernacular, MatchTxt, SubListOrder, IsPlaceholder
        String vegCode = mPhsCursor.getString(
                mPhsCursor.getColumnIndexOrThrow("Code"));
       if (LDebug.ON) Log.d(LOG_TAG, "mPhsCursor, pos = " + pos + " SppCode: " + vegCode);
        String vegDescr = mPhsCursor.getString(
                mPhsCursor.getColumnIndexOrThrow("MatchTxt"));
        String vegGenus = mPhsCursor.getString(
                mPhsCursor.getColumnIndexOrThrow("Genus"));
        String vegSpecies = mPhsCursor.getString(
                mPhsCursor.getColumnIndexOrThrow("Species"));
        String vegSubsppVar = mPhsCursor.getString(
                mPhsCursor.getColumnIndexOrThrow("SubsppVar"));
        String vegVernacular = mPhsCursor.getString(
                mPhsCursor.getColumnIndexOrThrow("Vernacular"));
        int vegSubListOrder = mPhsCursor.getInt(
                mPhsCursor.getColumnIndexOrThrow("SubListOrder"));
        int vegIsPlaceholder = mPhsCursor.getInt(
                mPhsCursor.getColumnIndexOrThrow("IsPlaceholder"));

       if (LDebug.ON) Log.d(LOG_TAG, "about to dispatch 'EditSppItemDialog' dialog to create new record");
        Bundle args = new Bundle();
        args.putLong(EditSppItemDialog.VEG_ITEM_REC_ID, 0); // don't need this, default is in class

        if (vegIsPlaceholder == 1) {
            args.putInt(EditSppItemDialog.REC_SOURCE, VegcodeSources.PLACE_HOLDERS);
        } else {
            args.putInt(EditSppItemDialog.REC_SOURCE, VegcodeSources.REGIONAL_LIST);
        }
        args.putLong(EditSppItemDialog.SOURCE_REC_ID, id);

        // streamline this, get directly from cursor
        args.putString(EditSppItemDialog.VEG_CODE, vegCode);
        args.putString(EditSppItemDialog.VEG_DESCR, vegDescr);
        args.putString(EditSppItemDialog.VEG_GENUS, vegGenus);
        args.putString(EditSppItemDialog.VEG_SPECIES, vegSpecies);
        args.putString(EditSppItemDialog.VEG_SUBSPP_VAR, vegSubsppVar);
        args.putString(EditSppItemDialog.VEG_VERNACULAR, vegVernacular);
        args.putInt(EditSppItemDialog.VEG_SUB_LIST_ORDER, vegSubListOrder);
        args.putInt(EditSppItemDialog.VEG_IS_PLACEHOLDER, vegIsPlaceholder);

        EditSppItemDialog newVegItemDlg = EditSppItemDialog.newInstance(args);

        newVegItemDlg.show(getFragmentManager(), "frg_new_veg_item");
    }

    // create context menus
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
       ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        switch (v.getId()) {
        case R.id.txt_search_chars:
            inflater.inflate(R.menu.context_sel_spp_search_chars, menu);
            if (mStSearch.trim().length() == 0) {
                // can't add placeholder if no text yet to use
                menu.removeItem(R.id.sel_spp_search_add_placeholder);
            }
            if (mPlaceholderRequestListener
                    .onRequestGetCountOfExistingPlaceholders() == 0) {
                // if no placeholders, don't show option to pick from them
                menu.removeItem(R.id.sel_spp_search_pick_placeholder);
            }
            break;
		case android.R.id.list:
            inflater.inflate(R.menu.context_sel_spp_list_items, menu);
            // try to remove items not relevant to the selection
            AdapterView.AdapterContextMenuInfo info;
            try {
                // Casts the incoming data object into the type for AdapterView objects.
                info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            } catch (ClassCastException e) {
                if (LDebug.ON) Log.d(LOG_TAG, "bad menuInfo", e); // if the menu object can't be cast
                break;
            }
            mPhsCursor.moveToPosition(info.position);
            int isPlaceHolder = mPhsCursor.getInt(
                    mPhsCursor.getColumnIndexOrThrow("IsPlaceholder"));
            if (isPlaceHolder == 0) {
                // if not a Placeholder, the 'edit Placeholder' option does not apply
                menu.removeItem(R.id.sel_spp_list_item_edit_ph);
            }
            int subListOrder = mPhsCursor.getInt(
                    mPhsCursor.getColumnIndexOrThrow("SubListOrder"));
            if ((isPlaceHolder == 1) || (subListOrder > 2)) {
                // a Placeholder, or a defined species not previously found
                // option to forget its top relevance does not apply
                menu.removeItem(R.id.sel_spp_list_item_forget);
            }
			break;
        }
    }

    // This is executed when the user selects an option
    @Override
    public boolean onContextItemSelected(MenuItem item) {
//    AdapterViewCompat.AdapterContextMenuInfo info = (AdapterViewCompat.AdapterContextMenuInfo) item.getMenuInfo();
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
        Bundle phArgs = new Bundle();
        int itemIsPlaceholder;


        // get an Analytics event tracker
        Tracker headerContextTracker = ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);

        switch (item.getItemId()) {

            case R.id.sel_spp_search_add_placeholder:
               if (LDebug.ON) Log.d(LOG_TAG, "'Create Placeholder' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Species Select Event")
                        .setAction("Context Menu")
                        .setLabel("Create Placeholder")
                        .setValue(1)
                        .build());
                // add or edit Placeholder
                String phCode, wholeContents = mStSearch.trim();
                Boolean wasShortened;
                if (wholeContents.length() <= VNConstraints.PLACEHOLDER_MAX_LENGTH) {
                  phCode = wholeContents;
                    wasShortened = false;
                } else {
                    phCode = wholeContents.substring(0, VNConstraints.PLACEHOLDER_MAX_LENGTH).trim();
                    wasShortened = true;
                }

//                Toast.makeText(this.getActivity(), "Placeholder code '" + phCode + "'", Toast.LENGTH_SHORT).show();
                if (phCode.length() < 3) {
//                    Toast.makeText(this.getActivity(), "Placeholder codes must be at least 3 characters long.", Toast.LENGTH_SHORT).show();
                    Toast.makeText(this.getActivity(), c.getResources().getString(R.string.placeholder_validate_code_short), Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (phCode.matches(VNRegex.NRCS_CODE)) { // see VNContract for details
//                    Toast.makeText(this.getActivity(), "Placeholder can\'t be like an NRCS code.", Toast.LENGTH_SHORT).show();
                    Toast.makeText(this.getActivity(), c.getResources().getString(R.string.placeholder_validate_code_bad), Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (mPlaceholderRequestListener
                        .onRequestMatchCheckOfExistingPlaceholders(phCode)) {
                    Toast.makeText(this.getActivity(),
                            c.getResources().getString(R.string.placeholder_validate_code_dup),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }

                phArgs.putLong(EditPlaceholderFragment.ARG_PLACEHOLDER_ID, 0); // new, may not be needed
                phArgs.putLong(EditPlaceholderFragment.ARG_PH_PROJECT_ID, mProjectId);
                phArgs.putLong(EditPlaceholderFragment.ARG_PH_NAMER_ID, mNamerId);
                phArgs.putString(EditPlaceholderFragment.ARG_PLACEHOLDER_CODE, phCode);
                phArgs.putBoolean(EditPlaceholderFragment.ARG_CODE_WAS_SHORTENED, wasShortened);
                mEditPlaceholderCallback.onEditPlaceholder(phArgs);
                return true;

            case R.id.sel_spp_search_pick_placeholder:
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Species Select Event")
                        .setAction("Context Menu")
                        .setLabel("Pick Placeholder")
                        .setValue(1)
                        .build());
                mPickPlaceholder = true;
                mCkPhsNotIdd.setChecked(true);
                getLoaderManager().restartLoader(Loaders.SPP_MATCHES, null, ManagePhsFragment.this);
                return true;

            case R.id.sel_spp_search_help:
               if (LDebug.ON) Log.d(LOG_TAG, "'Search Chars Help' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Species Select Event")
                        .setAction("Context Menu")
                        .setLabel("Search Chars Help")
                        .setValue(1)
                        .build());
                // Search Characters help
                helpTitle = c.getResources().getString(R.string.sel_spp_help_search_title);
                helpMessage = c.getResources().getString(R.string.sel_spp_help_search_text);
                flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
                flexHlpDlg.show(getFragmentManager(), "frg_help_search_chars");
                return true;

            case R.id.sel_spp_list_item_edit_ph:
               if (LDebug.ON) Log.d(LOG_TAG, "Spp list item 'Edit Placeholder' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Species Select Event")
                        .setAction("Context Menu")
                        .setLabel("List Item Edit Placeholder")
                        .setValue(1)
                        .build());
//                // Search Characters help
//                helpTitle = "Edit";
//                helpMessage = "Edit Placeholder tapped";
//                flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
//                flexHlpDlg.show(getFragmentManager(), "frg_spp_item_edit_ph");

                // Edit placeholder
                if (info == null) {
                    Toast.makeText(getActivity(),
                            c.getResources().getString(R.string.sel_spp_list_ctx_edit_ph_not),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                mPhsCursor.moveToPosition(info.position);
                itemIsPlaceholder = mPhsCursor.getInt(
                        mPhsCursor.getColumnIndexOrThrow("IsPlaceholder"));
                if (itemIsPlaceholder != 1) {
                    Toast.makeText(getActivity(),
                            c.getResources().getString(R.string.sel_spp_list_ctx_edit_ph_not),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                phArgs.putLong(EditPlaceholderFragment.ARG_PLACEHOLDER_ID, info.id);
                phArgs.putString(EditPlaceholderFragment.ARG_PLACEHOLDER_CODE, mPhsCursor.getString(
                        mPhsCursor.getColumnIndexOrThrow("Code")));
                phArgs.putBoolean(EditPlaceholderFragment.ARG_CODE_WAS_SHORTENED, false);
                mEditPlaceholderCallback.onEditPlaceholder(phArgs);
                return true;

            case R.id.sel_spp_list_item_help:
               if (LDebug.ON) Log.d(LOG_TAG, "Spp list item 'Help' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Species Select Event")
                        .setAction("Context Menu")
                        .setLabel("List Item Help")
                        .setValue(1)
                        .build());
                // Search Characters help
                helpTitle = c.getResources().getString(R.string.sel_spp_help_list_item_title);
                helpMessage = c.getResources().getString(R.string.sel_spp_help_list_item_text);
                flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
                flexHlpDlg.show(getFragmentManager(), "frg_spp_item_help");
                return true;

            default:
                return super.onContextItemSelected(item);
       } // end of Switch
    }

    public void refreshPhsList() {
        // use after edit/delete
        getLoaderManager().restartLoader(Loaders.PHS_MATCHES, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        String[] params = null;
        switch (id) {

            case Loaders.PHS_MATCHES:
                baseUri = ContentProvider_VegNab.SQL_URI;
                String orderBy;


                select = "SELECT _id, PlaceHolderCode AS Code, '' AS Genus, '' AS Species, "
                        + "'' AS SubsppVar, Description AS Vernacular, "
                        + "PlaceHolderCode || ': ' || Description || "
                        + "IFNULL((' = ' || IdSppCode || (IFNULL((': ' || IdSppDescription), ''))), '') "
                        + "AS MatchTxt, "
                        + "1 AS SubListOrder, "
                        + "1 AS IsPlaceholder, "
                        + "CASE WHEN IFNULL(IdSppCode, 0) = 0 THEN 0 ELSE 1 END AS IsIdentified "
                        + "FROM PlaceHolders "
                        + "WHERE ProjID=? "
                        + ((mNamerId > 0) ? "AND PlaceHolders.NamerID=? " : "")
                        + "ORDER BY TimeFirstInput DESC;";
                if (mNamerId > 0) {
                    params = new String[] {"" + mProjectId, "" + mNamerId };
                } else {
                    params = new String[] {"" + mProjectId };
                }
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, params, null);
                break;

            case Loaders.PHS_NAMERS:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT _id, NamerName FROM Namers "
                        + "UNION SELECT 0, '(all)' "
                        + "ORDER BY _id;";
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, null, null);
                break;
        }
        return cl;

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor finishedCursor) {
        // there will be various loaders, switch them out here
        mRowCt = finishedCursor.getCount();
        switch (loader.getId()) {

            case Loaders.PHS_MATCHES:
                mPhResultsAdapter.swapCursor(finishedCursor);
                mPhsCursor = finishedCursor;
                if ((mPickPlaceholder) && (mRowCt == 0)) {
                            /*    <string name="sel_spp_search_msg_empty_list">(matches will appear here)</string>
        <string name="sel_spp_search_msg_not_finished">(working)</string>
        <string name="sel_spp_search_msg_no_matches">(no matches)</string>*/
                    Toast.makeText(getActivity(),
                            getActivity().getResources().getString(R.string.sel_spp_pick_placeholder_none),
                            Toast.LENGTH_SHORT).show();
                }
                if ((mRowCt == 0) && (mStSearch.trim().length() != 0)) {
                    mViewForEmptyList.setText(
                            getActivity().getResources().getString(R.string.sel_spp_search_msg_no_matches));
                }
                break;


            case Loaders.PHS_NAMERS:
                // Swap the new cursor in.
                // The framework will take care of closing the old cursor once we return.
                mPhNamerAdapter.swapCursor(finishedCursor);
                if (mRowCt > 0) {
                    mPhNamerSpinner.setEnabled(true);
                }
                break;

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed. Need to make sure it is no longer is use.
        switch (loader.getId()) {

            case Loaders.PHS_MATCHES:
                mPhResultsAdapter.swapCursor(null);
                break;

            case Loaders.NAMERS:
                mPhNamerAdapter.swapCursor(null);
                break;
        }
    }
}
