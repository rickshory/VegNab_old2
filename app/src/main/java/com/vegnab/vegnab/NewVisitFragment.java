package com.vegnab.vegnab;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract;
import com.vegnab.vegnab.database.VNContract.LDebug;
import com.vegnab.vegnab.database.VNContract.Prefs;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class NewVisitFragment extends ListFragment implements OnClickListener,
        android.widget.AdapterView.OnItemSelectedListener,
        LoaderManager.LoaderCallbacks<Cursor>{


    private static final String LOG_TAG = NewVisitFragment.class.getSimpleName();
    long mProjectId, mPlotTypeId;
    int mRowCt = 0, mCtHiddenVisits = 0;
    final static String ARG_SUBPLOT = "subplot";
//    final static String ARG_VISIT_ID = "visitId";
//    final static String ARG_VISIT_NAME = "visitName";
//    final static String ARG_EXPORT_FILENAME = "exportFileName";
    int mCurrentSubplot = -1;
    Spinner mProjSpinner, mPlotTypeSpinner;
    SimpleCursorAdapter mProjAdapter, mPlotTypeAdapter, mVisitListAdapter;
    Cursor mVisitCursor, mHiddenVisitsCursor;
    ContentValues mValues = new ContentValues();
    // declare that the container Activity must implement this interface
    public interface OnButtonListener {
        // methods that must be implemented in the container Activity
        public void onNewVisitGoButtonClicked();
    }
    OnButtonListener mButtonCallback; // declare the interface
    public interface OnVisitClickListener {
        // methods that must be implemented in the container Activity
        public void onExistingVisitListClicked(long visitId);
    }
    OnVisitClickListener mListClickCallback;
    public interface ExportVisitListener {
        void onExportVisitRequest(Bundle paramsBundle);
    }
//    ExportVisitListener mExpVisListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get a Tracker (should auto-report)
        ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
//        try {
//            mExpVisListener = (ExportVisitListener) getActivity();
//           if (LDebug.ON) Log.d(LOG_TAG, "set up (ExportVisitListener) getActivity()");
//        } catch (ClassCastException e) {
//            throw new ClassCastException("Main Activity must implement ExportVisitListener interface");
//        }
        setHasOptionsMenu(true);
        // start this loader that does not use the UI
        getLoaderManager().initLoader(VNContract.Loaders.HIDDEN_VISITS, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.new_visit, menu);
                /*  // will mCtHiddenVisits be retrieved in time?, or will it always ==0 & therefore never show?
        if (mCtHiddenVisits == 0) {
        menu.removeItem(R.id.action_unhide_visits);
            }
            */
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
//        // any not handled come here to this Fragment
//            // items specific to New Visit menu
//            // action_edit_proj: currently handled by Activity, change to Context menu of Projects spinner
//            // action_new_proj: currently handled by Activity, change to 'new item' in Projects spinner
//            // action_del_proj: currently handled by Activity, change to Context menu of Projects spinner
//            // action_new_plottype: not implemented yet, msg handled by Activity
//            // action_get_species: handled by Activity
//            // action_export_db: handled by Activity
//            // action_unhide_visits: handled here

            case R.id.action_unhide_visits:
                if (mCtHiddenVisits == 0) {
                    Toast.makeText(getActivity(),
                            getActivity().getResources().getString(R.string.new_visit_unhide_visit_none),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Bundle args = new Bundle();
                    // don't put anything in the bundle yet, following line only shows format
                    //args.putLong(UnHideVisitDialog.ARG_VISIT_ID_TO_UNHIDE, mCtHiddenVisits);
                    // maybe pass the cursor?
                    UnHideVisitDialog  unHideVisDlg = UnHideVisitDialog.newInstance(args);
                    unHideVisDlg.show(getActivity().getSupportFragmentManager(), "frg_unhide_vis");
                }
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
            mCurrentSubplot = savedInstanceState.getInt(ARG_SUBPLOT);
        }
        // inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_new_visit, container, false);
//		Button s = (Button) rootView.findViewById(R.id.sign_in_button);
//		s.setOnClickListener(this);
        // set click listener for the "Start" button in the view
        Button b = (Button) rootView.findViewById(R.id.new_visit_go_button);
        b.setOnClickListener(this);
        // if more, loop through all the child items of the ViewGroup rootView and
        // set the onclicklistener for all the Button instances found
        // Create an empty adapter we will use to display the list of Projects
        mProjSpinner = (Spinner) rootView.findViewById(R.id.sel_project_spinner);
        mProjSpinner.setEnabled(false); // will enable when data ready
        mProjAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, null,
                new String[] {"ProjCode"},
                new int[] {android.R.id.text1}, 0);

        mProjAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mProjSpinner.setAdapter(mProjAdapter);
        mProjSpinner.setOnItemSelectedListener(this);
        // Prepare the loader. Either re-connect with an existing one or start a new one
        getLoaderManager().initLoader(VNContract.Loaders.PROJECTS, null, this);
        // If there in no Loader yet, this will call
        // Loader<Cursor> onCreateLoader and pass it a first parameter of Loaders.PROJECTS
        mPlotTypeSpinner = (Spinner) rootView.findViewById(R.id.sel_plot_type_spinner);
        mPlotTypeSpinner.setEnabled(false); // will enable when data ready
        mPlotTypeAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, null,
                new String[] {"PlotTypeDescr"},
                new int[] {android.R.id.text1}, 0);
        mPlotTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPlotTypeSpinner.setAdapter(mPlotTypeAdapter);
        mPlotTypeSpinner.setOnItemSelectedListener(this);
        getLoaderManager().initLoader(VNContract.Loaders.PLOTTYPES, null, this);

        mVisitListAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null,
                new String[] {"VisitName"},
                new int[] {android.R.id.text1}, 0);
        setListAdapter(mVisitListAdapter);
        getLoaderManager().initLoader(VNContract.Loaders.PREV_VISITS, null, this);

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
            // set up subplot based on arguments passed in
            updateSubplotViews(args.getInt(ARG_SUBPLOT));
        } else if (mCurrentSubplot != -1) {
            // set up subplot based on saved instance state defined in onCreateView
            updateSubplotViews(mCurrentSubplot);
        } else {
            updateSubplotViews(-1); // figure out what to do for default state
        }
        // set up the list to receive long-press
        registerForContextMenu(getListView());

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
        // OnVisitClickListener mListClickCallback;
        try {
            mListClickCallback = (OnVisitClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException (activity.toString() + " must implement OnVisitClickListener");
        }
    }

    public void updateSubplotViews(int subplotNum) {
        // don't do anything yet
        // figure out how to deal with default of -1
        mCurrentSubplot = subplotNum;
    }

    public void saveDefaultProjectId(long id) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putLong(Prefs.DEFAULT_PROJECT_ID, id);
        prefEditor.commit();
    }

    public void saveDefaultPlotTypeId(long id) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putLong(Prefs.DEFAULT_PLOTTYPE_ID, id);
        // get the plot type text description
        String strSel;
        try { // on first app load, this cursor may not be set up yet
            Cursor cur = (Cursor)mPlotTypeAdapter.getCursor();
            cur.moveToPosition(mPlotTypeSpinner.getSelectedItemPosition());
            strSel = cur.getString(cur.getColumnIndex("PlotTypeDescr"));
        }  catch (Exception e) {
            strSel = "Species List"; // use the default
        }
        prefEditor.putString(Prefs.DEFAULT_PLOTTYPE_NAME, strSel);
        prefEditor.commit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save the current subplot arguments in case we need to re-create the fragment
        outState.putInt(ARG_SUBPLOT, mCurrentSubplot);
    }

    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
//        Toast.makeText(this.getActivity(), "Clicked position " + pos + ", id " + id, Toast.LENGTH_SHORT).show();
        mListClickCallback.onExistingVisitListClicked(id);
    }

    // create context menus
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        switch (v.getId()) {
//        case R.id.txt_search_chars:
//            inflater.inflate(R.menu.context_sel_spp_search_chars, menu);
//            if (mStSearch.trim().length() == 0) {
//                // can't add placeholder if no text yet to use
//                menu.removeItem(R.id.sel_spp_search_add_placeholder);
//            }
//            if (mPlaceholderCodesForThisNamer.size() == 0) {
//                // if no placeholders, don't show option to pick from them
//                menu.removeItem(R.id.sel_spp_search_pick_placeholder);
//            }
//            break;
            case android.R.id.list:
                inflater.inflate(R.menu.context_new_visit_list_items, menu);
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
        Bundle nvArgs = new Bundle();
        int itemIsPlaceholder;


        // get an Analytics event tracker
        Tracker headerContextTracker = ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);

        switch (item.getItemId()) {

            case R.id.new_visit_list_item_export:
               if (LDebug.ON) Log.d(LOG_TAG, "New Visit item 'Export Visit' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("New Visit Event")
                        .setAction("Context Menu")
                        .setLabel("Export Visit")
                        .setValue(1)
                        .build());
//                notYetDlg.show(getFragmentManager(), null);
                // Call a method in the main activity to export the visit information
                if (info == null) {
                    Toast.makeText(getActivity(),
                            c.getResources().getString(R.string.new_vis_ctx_export_no_info),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                Bundle expArgs = new Bundle();
                expArgs.putLong(MainVNActivity.ARG_VISIT_TO_EXPORT_ID, info.id);
                Cursor cur = (Cursor)mVisitListAdapter.getCursor();
                cur.moveToPosition(info.position);
                String visName = cur.getString(cur.getColumnIndex("VisitName"));
                expArgs.putString(MainVNActivity.ARG_VISIT_TO_EXPORT_NAME, visName);
                // generate a unique filename
                // ultimately user will get to choose/edit in Confirm dialog
                String appName = getActivity().getResources().getString(R.string.app_name);
                SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                String exportFileName = appName + " " + ((visName == "" ? "" : visName + " "))
                        + fileNameFormat.format(new Date());
                expArgs.putString(MainVNActivity.ARG_VISIT_TO_EXPORT_FILENAME, exportFileName);
                // put any other parameters in, such as
                // format of output, whether to resolve Placeholders, etc.
//                mExpVisListener.onExportVisitRequest(expArgs);
                ExportVisitDialog expVisDlg = ExportVisitDialog.newInstance(expArgs);
                expVisDlg.show(getFragmentManager(), "frg_new_exp_visit");
                return true;

            case R.id.new_visit_list_item_hide:
               if (LDebug.ON) Log.d(LOG_TAG, "New Visit item 'Hide Visit' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("New Visit Event")
                        .setAction("Context Menu")
                        .setLabel("List Item Hide Visit")
                        .setValue(1)
                        .build());
                // Hide visit
                if (info == null) {
                   if (LDebug.ON) Log.d(LOG_TAG, "info == null 'Hide Visit' exiting");
                    return true;
                }
// adapt this if need to test for conditions
//                mVisitCursor.moveToPosition(info.position);
//                itemIsPlaceholder = mSppMatchCursor.getInt(
//                        mSppMatchCursor.getColumnIndexOrThrow("IsPlaceholder"));
//                if (itemIsPlaceholder == 1) {
//                    Toast.makeText(getActivity(),
//                            c.getResources().getString(R.string.sel_spp_list_ctx_forget_not_spp),
//                            Toast.LENGTH_SHORT).show();
//                    return true;
//                }
                setVisitVisibility(info.id, false);
                return true;

            case R.id.new_visit_list_item_help:
               if (LDebug.ON) Log.d(LOG_TAG, "New Visit item 'Help' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("New Visit Event")
                        .setAction("Context Menu")
                        .setLabel("List Item Help")
                        .setValue(1)
                        .build());
                // Search Characters help
                helpTitle = c.getResources().getString(R.string.msg_help);
                helpMessage = c.getResources().getString(R.string.msg_help_apology);
                flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
                flexHlpDlg.show(getFragmentManager(), "frg_new_visit_help");
                return true;

            default:
                return super.onContextItemSelected(item);
       } // end of Switch
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.new_visit_go_button:
            // test of using the Content Provider for direct SQL
            getLoaderManager().initLoader(VNContract.Loaders.TEST_SQL, null, this);

/*			Toast.makeText(this.getActivity(), 
                    "Selected Project position: " + mProjSpinner.getSelectedItemPosition()
                    + ", Id: " + mProjSpinner.getSelectedItemId() ,
                    Toast.LENGTH_LONG).show();

            Toast.makeText(this.getActivity(),
                    "Selected PlotType position: " + mPlotTypeSpinner.getSelectedItemPosition()
                    + ", Id: " + mPlotTypeSpinner.getSelectedItemId() ,
                    Toast.LENGTH_LONG).show();
*/			
            if (mProjSpinner.getSelectedItemPosition() == -1) {
                Toast.makeText(this.getActivity(),
                        "" + getResources().getString(R.string.missing_project),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (mPlotTypeSpinner.getSelectedItemPosition() == -1) {
                Toast.makeText(this.getActivity(),
                        "" + getResources().getString(R.string.missing_plottype),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            mButtonCallback.onNewVisitGoButtonClicked();
            break;
        }
    }

    // define the columns we will retrieve from the Projects table
    static final String[] PROJECTS_PROJCODES = new String[] {
        "_id", "ProjCode",
    };

    public void setVisitVisibility(long visRecId, boolean showVisit) {
        if (visRecId == 0) return; // default bailout
        Context c = getActivity();
        if (showVisit) {
           if (LDebug.ON) Log.d(LOG_TAG, "About to show Visit, record id=" + visRecId);
        } else {
           if (LDebug.ON) Log.d(LOG_TAG, "About to hide Visit, record id=" + visRecId);
        }
        Uri uri, sUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "visits");
        uri = ContentUris.withAppendedId(sUri, visRecId);
        mValues.clear();
        mValues.put("ShowOnMobile", (showVisit ? 1 : 0));
        ContentResolver rs = c.getContentResolver();
        int numUpdated = rs.update(uri, mValues, null, null);
       if (LDebug.ON) Log.d(LOG_TAG, "Updated visit to ShowOnMobile=" + showVisit + "; numUpdated: " + numUpdated);
        String msg = (showVisit ? c.getResources().getString(R.string.new_visit_show_visit_done) :
                c.getResources().getString(R.string.new_visit_hide_visit_done));
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
        refreshVisitsList();
    }

    public void refreshVisitsList() {
        // when the referred Loader callback returns, will update the list of Visits
        getLoaderManager().restartLoader(VNContract.Loaders.PREV_VISITS, null, this);
        getLoaderManager().restartLoader(VNContract.Loaders.HIDDEN_VISITS, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        switch (id) {

            case VNContract.Loaders.TEST_SQL:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT StartDate FROM Projects WHERE _id = 1;";
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, null, null);
                break;

            case VNContract.Loaders.PROJECTS:
                // First, create the base URI
                // could test here, based on e.g. filters
                baseUri = ContentProvider_VegNab.CONTENT_URI;
                // Now create and return a CursorLoader that will take care of
                // creating a Cursor for the dataset being displayed
                // select is the WHERE clause
                select = "(IsDeleted = 0)";
                cl = new CursorLoader(getActivity(), Uri.parse(baseUri + "/projects"),
                        PROJECTS_PROJCODES, select, null, null);
                break;

            case VNContract.Loaders.PLOTTYPES:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT _id, PlotTypeDescr FROM PlotTypes;";
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, null, null);
                break;

            case VNContract.Loaders.PREV_VISITS:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT _id, VisitName, VisitDate FROM Visits "
                        + "WHERE ShowOnMobile = 1 AND IsDeleted = 0 "
                        + "ORDER BY LastChanged DESC;";
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, null, null);
                break;

            case VNContract.Loaders.HIDDEN_VISITS:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT _id, VisitName, VisitDate FROM Visits "
                        + "WHERE ShowOnMobile = 0 AND IsDeleted = 0 "
                        + "ORDER BY VisitDate DESC;";
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

            case VNContract.Loaders.TEST_SQL:
               if (LDebug.ON) Log.d(LOG_TAG, "Loaders.TEST_SQL returned cursor ");
                finishedCursor.moveToFirst();
                String d = finishedCursor.getString(0);
               if (LDebug.ON) Log.d(LOG_TAG, "Loaders.TEST_SQL value returned: " + d);
    /*			Toast.makeText(this.getActivity(),
                        "Date: " + d,
                        Toast.LENGTH_LONG).show();
    */
                break;

            case VNContract.Loaders.PROJECTS:
                // Swap the new cursor in.
                // The framework will take care of closing the old cursor once we return.
                mProjAdapter.swapCursor(finishedCursor);
                if (mRowCt > 0) {
                    mProjSpinner.setEnabled(true);
                    // get default Project from app Preferences, to set spinner
                    // this must wait till the spinner is populated
                    SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                    // database comes pre-loaded with one Project record that has _id = 1
                    // default ProjCode = "MyProject', but may be renamed
                    mProjectId = sharedPref.getLong(Prefs.DEFAULT_PROJECT_ID, 1);
                    if (!sharedPref.contains(Prefs.DEFAULT_PROJECT_ID)) {
                        // this will only happen once, when the app is first installed
    /*					Toast.makeText(this.getActivity(),
                                "Prefs key '" + PREF_DEFAULT_PROJECT_ID + "' does not exist yet.",
                                Toast.LENGTH_LONG).show();
    */
                       if (LDebug.ON) Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PROJECT_ID + "' does not exist yet.");
                        // update the create time in the database from when the DB file was created to 'now'
                        String sql = "UPDATE Projects SET StartDate = DATETIME('now') WHERE _id = 1;";
                        ContentResolver resolver = getActivity().getContentResolver();
                        // use raw SQL, to make use of SQLite internal "DATETIME('now')"
                        Uri uri = ContentProvider_VegNab.SQL_URI;
                        int numUpdated = resolver.update(uri, null, sql, null);
                        saveDefaultProjectId(mProjectId);
    /*					Toast.makeText(this.getActivity(),
                                "Prefs key '" + PREF_DEFAULT_PROJECT_ID + "' set for the first time.",
                                Toast.LENGTH_LONG).show();
    */
                       if (LDebug.ON) Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PROJECT_ID + "' set for the first time.");
                    } else {
    /*					Toast.makeText(this.getActivity(),
                                "Prefs key '" + PREF_DEFAULT_PROJECT_ID + "' = " + mProjectId,
                                Toast.LENGTH_LONG).show();
    */
                       if (LDebug.ON) Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PROJECT_ID + "' = " + mProjectId);
                    }
                    // set the default Project to show in its spinner
                    // for a generalized fn, try: mProjSpinner.getAdapter().getCount()
                    for (int i=0; i<mRowCt; i++) {
                       if (LDebug.ON) Log.d(LOG_TAG, "Setting mProjSpinner default; testing index " + i);
                        if (mProjSpinner.getItemIdAtPosition(i) == mProjectId) {
                           if (LDebug.ON) Log.d(LOG_TAG, "Setting mProjSpinner default; found matching index " + i);
                            mProjSpinner.setSelection(i);
                            break;
                        }
                    }
                } else {
                    mProjSpinner.setEnabled(false);
                }
                break;

            case VNContract.Loaders.PLOTTYPES:
                // Swap the new cursor in.
                // The framework will take care of closing the old cursor once we return.
                mPlotTypeAdapter.swapCursor(finishedCursor);
                if (mRowCt > 0) {
                    mPlotTypeSpinner.setEnabled(true);
                    // get default Plot Type from app Preferences, to set spinner
                    // this must wait till the spinner is populated
                    SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                    // database comes pre-loaded with one Plot Type record that has _id = 1
                    // default PlotTypeDescr = "Species List'
                    mPlotTypeId = sharedPref.getLong(Prefs.DEFAULT_PLOTTYPE_ID, 1);
                    if (!sharedPref.contains(Prefs.DEFAULT_PLOTTYPE_ID)) {
                        // this will only happen once, when the app is first installed
    /*					Toast.makeText(this.getActivity(),
                                "Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' does not exist yet.",
                                Toast.LENGTH_LONG).show();
    */
                       if (LDebug.ON) Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' does not exist yet.");
                        saveDefaultPlotTypeId(mPlotTypeId);
    /*					Toast.makeText(this.getActivity(),
                                "Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' set for the first time.",
                                Toast.LENGTH_LONG).show();
    */
                       if (LDebug.ON) Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' set for the first time.");
                    } else { // default plot type already exisits
    /*					Toast.makeText(this.getActivity(),
                                "Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' = " + mPlotTypeId,
                                Toast.LENGTH_LONG).show();
    */
                       if (LDebug.ON) Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' = " + mPlotTypeId);
                    }
                    // set the default Plot Type to show in its spinner
                    // for a generalized fn, try: mySpinner.getAdapter().getCount()
                    for (int i=0; i<mRowCt; i++) {
                       if (LDebug.ON) Log.d(LOG_TAG, "Setting mPlotTypeSpinner default; testing index " + i);
                        if (mPlotTypeSpinner.getItemIdAtPosition(i) == mPlotTypeId) {
                           if (LDebug.ON) Log.d(LOG_TAG, "Setting mPlotTypeSpinner default; found matching index " + i);
                            mPlotTypeSpinner.setSelection(i);
                            break;
                        }
                    }
                } else {
                    mPlotTypeSpinner.setEnabled(false);
                }
                break;

            case VNContract.Loaders.PREV_VISITS:
                mVisitCursor = finishedCursor; // save a reference
                mVisitListAdapter.swapCursor(finishedCursor);
                break;

            case VNContract.Loaders.HIDDEN_VISITS:
                mHiddenVisitsCursor = finishedCursor; // save a reference
                mCtHiddenVisits = mRowCt;
//            mVisitListAdapter.swapCursor(finishedCursor);
                break;

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed. Need to make sure it is no longer is use.
        switch (loader.getId()) {
            case VNContract.Loaders.PROJECTS:
                mProjAdapter.swapCursor(null);
                break;
            case VNContract.Loaders.PLOTTYPES:
                mPlotTypeAdapter.swapCursor(null);
                break;
            case VNContract.Loaders.PREV_VISITS:
                mVisitListAdapter.swapCursor(null);
                break;
            case VNContract.Loaders.HIDDEN_VISITS:
                break; // nothing to do with this one
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
        //Cursor cur = (Cursor)mProjAdapter.getItem(position);
        //String strSel = cur.getString(cur.getColumnIndex("ProjCode"));
        //Log.d(LOG_TAG, strSel);
        // if spinner is filled by Content Provider, can't get text by:
        //String strSel = parent.getItemAtPosition(position).toString();
        // that returns something like below, which there is no way to get text out of:
        // "android.content.ContentResolver$CursorWrapperInner@42041b40"

        // sort out the spinners
        // can't use switch because not constants
        if (parent.getId() == mProjSpinner.getId()) {
            mProjectId = id;
            // save in app Preferences as the default Project
            saveDefaultProjectId(mProjectId);
/*			
            Toast.makeText(parent.getContext(),
                    "Selected Project position: " + position
                    + ", Id: " + id,
                    Toast.LENGTH_LONG).show();
            Cursor cur = (Cursor)mProjAdapter.getItem(position);
            String strSel = cur.getString(cur.getColumnIndex("ProjCode"));
            Toast.makeText(parent.getContext(), "Project selected: " + strSel, Toast.LENGTH_LONG).show();
*/
        }
        if (parent.getId() == mPlotTypeSpinner.getId()) {
            mPlotTypeId = id;
            // save in app Preferences as the default Plot Type
            saveDefaultPlotTypeId(mPlotTypeId);
/*			
            Toast.makeText(parent.getContext(),
                    "Selected Plot Type position: " + position
                    + ", Id: " + id,
                    Toast.LENGTH_LONG).show();
            Cursor cur = (Cursor)mProjAdapter.getItem(position);
            String strSel = cur.getString(cur.getColumnIndex("PlotTypeDescr"));
            Toast.makeText(parent.getContext(), "Plot Type selected: " + strSel, Toast.LENGTH_LONG).show();
*/
        }

        // write code for any other spinner(s) here
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }
/*
    // no Override
    public static void onBackPressed() {
       if (LDebug.ON) Log.d("NewVist", "In NewVisitFragment, caught 'onBackPressed'");
    return;
    }
*/	
    public void showDatePickerDialog(View v) {
       if (LDebug.ON) Log.d("NewVisit", "Event caught in NewVisitFragment");
    }

}
