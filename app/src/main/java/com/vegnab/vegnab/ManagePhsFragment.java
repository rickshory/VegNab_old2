package com.vegnab.vegnab;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract;
import com.vegnab.vegnab.database.VNContract.LDebug;
import com.vegnab.vegnab.database.VNContract.Loaders;

import java.util.ArrayList;
import java.util.List;

public class ManagePhsFragment extends ListFragment
        implements AdapterView.OnItemSelectedListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = ManagePhsFragment.class.getSimpleName();

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

/*
    public interface OnPlaceholderRequestListener {
        // methods that must be implemented in the container Activity
        void onRequestGenerateExistingPlaceholders(Bundle args);
        long onRequestGetCountOfExistingPlaceholders();
        boolean onRequestMatchCheckOfExistingPlaceholders(String ph);
    }
    OnPlaceholderRequestListener mPlaceholderRequestListener;
*/
    long mRowCt;
    String mStSearch = "";
    EditText mViewSearchChars;
    CheckBox mViewCkPhsNotIdd;
//	ListView mSppItemsList;
    TextWatcher sppCodeTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            // use this method; test length of string; e.g. 'count' of other methods does not give this length
            //Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "'");
           if (LDebug.ON) Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "', length: " + s.length());
            mStSearch = s.toString();
            refreshPhsList();
/*
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
*/
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
        getLoaderManager().initLoader(Loaders.PHS_NAMERS, null, this);

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
        mPhSortSpinner.setOnItemSelectedListener(this);

        mViewSearchChars = (EditText) rootView.findViewById(R.id.txt_search_phs);
        mViewSearchChars.addTextChangedListener(sppCodeTextWatcher);
//        registerForContextMenu(mViewSearchChars); // enable long-press
        mViewCkPhsNotIdd = (CheckBox) rootView.findViewById(R.id.ck_show_phs_not_idd);
        mViewCkPhsNotIdd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                refreshPhsList();
            }
        });

        mViewForEmptyList = (TextView) rootView.findViewById(android.R.id.empty);

        // use query to return 'MatchTxt', concatenated from code and description; more reading room
        // use same internal layout for items as in species search fragment
        mPhResultsAdapter = new SelSppItemAdapter(getActivity(),
                R.layout.list_spp_search_item, null, 0);
        setListAdapter(mPhResultsAdapter);
        getLoaderManager().initLoader(Loaders.PHS_MATCHES, null, this);

        return rootView;
    }

        @Override
    public void onStart() {
        super.onStart();
        mViewSearchChars.requestFocus();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // assure the container activity has implemented the callback interfaces
        if (context instanceof Activity){
            Activity a = (Activity) context;
            try {
                mEditPlaceholderCallback = (OnEditPlaceholderListener) a;
            } catch (ClassCastException e) {
                throw new ClassCastException (a.toString() + " must implement OnEditPlaceholderListener");
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        refreshPhsList(); // if Placeholders were IDd, show changes
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
/*
            // workaround for spinner firing when first set
            if(((String)parent.getTag()).equalsIgnoreCase(VNContract.Tags.SPINNER_FIRST_USE)) {
                parent.setTag("");
                return;
            }
*/
            refreshPhsList();
        }

        if (parent.getId() == mPhSortSpinner.getId()) {
/*
            // workaround for spinner firing when first set
            if(((String)parent.getTag()).equalsIgnoreCase(VNContract.Tags.SPINNER_FIRST_USE)) {
                parent.setTag("");
                return;
            }
*/
            refreshPhsList();
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
//    	getListView().getItemAtPosition(pos).toString(); // not useful, gets cursor wrapper
        // Edit placeholder
        mPhsCursor.moveToPosition(pos);
        if (LDebug.ON) Log.d(LOG_TAG, "Placeholder list item " + pos + " selected");
        // following should not be necessary, list should only contain Placeholders
        int itemIsPlaceholder = mPhsCursor.getInt(
                mPhsCursor.getColumnIndexOrThrow("IsPlaceholder"));
        if (itemIsPlaceholder != 1) {
            Toast.makeText(getActivity(),
                    getActivity().getResources().getString(R.string.sel_spp_list_ctx_edit_ph_not),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle phArgs = new Bundle();
        phArgs.putLong(EditPlaceholderFragment.ARG_PLACEHOLDER_ID, mPhsCursor.getLong(
                mPhsCursor.getColumnIndexOrThrow("_id")));
        phArgs.putString(EditPlaceholderFragment.ARG_PLACEHOLDER_CODE, mPhsCursor.getString(
                mPhsCursor.getColumnIndexOrThrow("Code")));
        phArgs.putBoolean(EditPlaceholderFragment.ARG_CODE_WAS_SHORTENED, false);
        mEditPlaceholderCallback.onEditPlaceholder(phArgs);
        return;


//        EditSppItemDialog newVegItemDlg = EditSppItemDialog.newInstance(args);

//        newVegItemDlg.show(getFragmentManager(), "frg_new_veg_item");
    }

    // create context menus
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
       ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        switch (v.getId()) {

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

        // get an Analytics event tracker
//        Tracker headerContextTracker = ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);

        switch (item.getItemId()) {

            default:
                return super.onContextItemSelected(item);
       } // end of Switch
    }

    public void refreshPhsList() {
        // use after edit/delete
        if (LDebug.ON) Log.d(LOG_TAG, "in 'refreshPhsList'");
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
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, PHS_MATCHES");
                baseUri = ContentProvider_VegNab.SQL_URI;
                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                long mProjectId = sharedPref.getLong(VNContract.Prefs.DEFAULT_PROJECT_ID, 1);
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, PHS_MATCHES, got ProjectID=" + mProjectId);
                mStSearch = mViewSearchChars.getText().toString();
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, PHS_MATCHES, got mStSearch=" + mStSearch);
                boolean showOnlyNotIDd = mViewCkPhsNotIdd.isChecked();
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, PHS_MATCHES, got showOnlyNotIDd="
                        + (showOnlyNotIDd ? "true" : "false"));
                try { // first time, PhName spinner may not be set up yet
                    Cursor cr = (Cursor) mPhNamerSpinner.getSelectedItem();
                    mNamerId = cr.getLong(cr.getColumnIndexOrThrow("_id"));
                } catch (Exception e) {
                    if (LDebug.ON) Log.d(LOG_TAG, "mNamerId error: " + e.toString());
                    mNamerId = 0; // show Placeholders for all Namers, until resolved
                }
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, PHS_MATCHES, got mNamerId=" + mNamerId);
                String orderBy;
                int pos = mPhSortSpinner.getSelectedItemPosition();
                switch (pos) {
                    case 1: // A to Z
                        orderBy = "Code";
                        break;
                    case 2: // oldest first
                        orderBy = "TimeFirstInput";
                        break;
                    case 3: // Z to A
                        orderBy = "Code DESC";
                        break;
                    // for all the following, drop through to
                    // sort order of newest-first
                    case 0:
                    case Spinner.INVALID_POSITION:
                    default:
                        orderBy = "TimeFirstInput DESC";
                }
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, PHS_MATCHES, got orderBy=" + orderBy);
                List<String> prms = new ArrayList<String>(); // have to build SQL and
                // parameter list dynamically because there is no numeric wildcard for SQLite
                // to use all numeric values, leave the field entirely out of the WHERE clause
                if (mStSearch.trim().length() == 0) { // do not filter results by text
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
                            + (showOnlyNotIDd ? "AND IsIdentified = 0 " : "")
                            + "ORDER BY " + orderBy + ";";
                    prms.add( "" + mProjectId ); // always use Project ID
                    if (mNamerId > 0) prms.add( "" + mNamerId );
/*
                } else if (mStSearch.trim().length() < 3) { // match Placeholders only by code
                    select = "SELECT _id, PlaceHolderCode AS Code, '' AS Genus, '' AS Species, "
                            + "'' AS SubsppVar, Description AS Vernacular, "
                            + "PlaceHolderCode || ': ' || Description "
                            + "|| IFNULL((' = ' || IdSppCode || (IFNULL((': ' || IdSppDescription), ''))), '') "
                            + "AS MatchTxt, "
                            + "1 AS SubListOrder, 1 AS IsPlaceholder, "
                            + "CASE WHEN IFNULL(IdSppCode, 0) = 0 THEN 0 ELSE 1 END AS IsIdentified "
                            + "FROM PlaceHolders "
                            + "WHERE Code Like ? AND ProjID=? AND PlaceHolders.NamerID=? "
                            + (showOnlyNotIDd ? "AND IsIdentified = 0 " : "")
                            + "ORDER BY " + orderBy + ";";
                    params = new String[] {mStSearch + "%", "" + mProjectId, ((mNamerId > 0) ? "" + mNamerId : "%") };
*/
                } else { // match Placeholders by text
                    select = "SELECT _id, PlaceHolderCode AS Code, '' AS Genus, '' AS Species, "
                            + "'' AS SubsppVar, Description AS Vernacular, "
                            + "PlaceHolderCode || ': ' || Description "
                            + "|| IFNULL((' = ' || IdSppCode || (IFNULL((': ' || IdSppDescription), ''))), '') "
                            + "AS MatchTxt, "
                            + "1 AS SubListOrder, 1 AS IsPlaceholder, "
                            + "CASE WHEN IFNULL(IdSppCode, 0) = 0 THEN 0 ELSE 1 END AS IsIdentified "
                            + "FROM PlaceHolders "
                            + "WHERE MatchTxt Like ? AND ProjID=? "
                            + ((mNamerId > 0) ? "AND PlaceHolders.NamerID=? " : "")
                            + (showOnlyNotIDd ? "AND IsIdentified = 0 " : "")
                            + "ORDER BY " + orderBy + ";";
                    prms.add( "%" + mStSearch + "%" );
                    prms.add( "" + mProjectId );
                    if (mNamerId > 0) prms.add( "" + mNamerId );
                }
                params = new String[ prms.size() ];
                prms.toArray( params );
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, PHS_MATCHES, got select=" + select);
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, PHS_MATCHES, params=" + java.util.Arrays.toString(params));
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
                if ((mRowCt == 0) && (mStSearch.trim().length() != 0)) {
                    mViewForEmptyList.setText(
                            getActivity().getResources().getString(R.string.sel_spp_search_msg_no_matches));
                }
                if (LDebug.ON) Log.d(LOG_TAG, "in onLoadFinished, PHS_MATCHES, mRowCt=" + mRowCt);
                break;

            case Loaders.PHS_NAMERS:
                // Swap the new cursor in.
                // The framework will take care of closing the old cursor once we return.
                mPhNamerAdapter.swapCursor(finishedCursor);
                if (mRowCt > 0) {
                    mPhNamerSpinner.setEnabled(true);
                }
                refreshPhsList(); // may not have completed on start because this loader was not finished
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
