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
import android.text.InputType;
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
import java.util.HashMap;
import java.util.List;

public class FixSpellingsFragment extends ListFragment
        implements AdapterView.OnItemSelectedListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = FixSpellingsFragment.class.getSimpleName();

    long mProjectId = 0;
    long mItemId = 0;
    Cursor mSpellSourceCursor, mSpellItemsCursor;
    ContentValues mValues = new ContentValues();

    private Spinner mSpellSourceSpinner;
    SimpleCursorAdapter mSpellItemsAdapter;
    TextView mViewForEmptyList;

/* may not need another interface
    // declare an interface the container Activity must implement
    public interface OnEditPlaceholderListener {
        // methods that must be implemented in the container Activity
        void onEditPlaceholder(Bundle args);
    }
    OnEditPlaceholderListener mEditPlaceholderCallback; // declare the interface
*/
    long mRowCt;
    String mItemToEdit;

    final static String ARG_ITEM_TO_EDIT = "itemToEdit";
    final static String ARG_TABLE_NAME = "tableName";
    final static String ARG_TABLE_URI = "tableUri";
    final static String ARG_FIELD_NAME = "fieldName";
    final static String ARG_RECORD_ID = "recID";
    final static String ARG_INPUT_TYPE = "textFormat";
    final static String ARG_LENGTH_MIN = "minTextLength";
    final static String ARG_LENGTH_MAX = "maxTextLength";
    final static String ARG_EXISTING_VALUES = "existingValues";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_fix_spellings, container, false);
        mSpellSourceSpinner = (Spinner) rootView.findViewById(R.id.fix_spellings_spinner);
        mSpellSourceSpinner.setTag(VNContract.Tags.SPINNER_FIRST_USE); // flag to catch and ignore erroneous first firing
//        mSpellSourceSpinner.setEnabled(false); // will enable when data ready
//        mSpellSourceAdapter = new SimpleCursorAdapter(getActivity(),
//                android.R.layout.simple_spinner_item, null,
///                new String[] {"NamerName"},
//                new int[] {android.R.id.text1}, 0);
//        mSpellSourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        mSpellSourceSpinner.setAdapter(mSpellSourceAdapter);
//        mSpellSourceSpinner.setOnItemSelectedListener(this);
//        registerForContextMenu(mSpellSourceSpinner); // enable long-press
//        // Prepare the loader. Either re-connect with an existing one or start a new one
//        getLoaderManager().initLoader(Loaders.SPELL_ITEMS, null, this);

//        mPhSortSpinner = (Spinner) rootView.findViewById(R.id.ph_sort_spinner);
//        mPhSortSpinner.setTag(VNContract.Tags.SPINNER_FIRST_USE); // flag to catch and ignore erroneous first firing
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> spellTablesAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.spellings_tables_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        spellTablesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpellSourceSpinner.setAdapter(spellTablesAdapter);
        mSpellSourceSpinner.setOnItemSelectedListener(this);

        mViewForEmptyList = (TextView) rootView.findViewById(android.R.id.empty);

        mSpellItemsAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_2, null,
                new String[] {"SpellItem", "UsageNote"},
                new int[] {android.R.id.text1, android.R.id.text2}, 0);
        setListAdapter(mSpellItemsAdapter);
        getLoaderManager().initLoader(Loaders.PHS_MATCHES, null, this);

        return rootView;
    }

        @Override
    public void onStart() {
        super.onStart();
//        mViewSearchChars.requestFocus();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
/* may not need an interface
        // assure the container activity has implemented the callback interfaces
        try {
            mEditPlaceholderCallback = (OnEditPlaceholderListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException (activity.toString() + " must implement OnEditPlaceholderListener");
        }
*/
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        refreshItemsList(); // if Placeholders were IDd, show changes
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
        if (parent.getId() == mSpellSourceSpinner.getId()) {
            // workaround for spinner firing when first set
            if(((String)parent.getTag()).equalsIgnoreCase(VNContract.Tags.SPINNER_FIRST_USE)) {
                parent.setTag("");
                return;
            }
            refreshItemsList();
        }
        // write code for any other spinner(s) here
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
//        setSpinnerSelectionFromDefault();
    }

    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
        if (LDebug.ON) Log.d(LOG_TAG, "List item clicked, item " + pos);
//        Toast.makeText(this.getActivity(), "Clicked position " + pos + ", id " + id, Toast.LENGTH_SHORT).show();
//    	getListView().getItemAtPosition(pos).toString(); // not useful, gets cursor wrapper
        // Edit item
        if (LDebug.ON) Log.d(LOG_TAG, "List item " + pos + " selected");
        Bundle args = new Bundle();
        Context c = getActivity();
        args.putString(EditSpellingDialog.ARG_TOOLBAR_HEADER,
                c.getResources().getString(R.string.edit_spellings_toolbar_title));
        Cursor cr = mSpellItemsAdapter.getCursor();
        cr.moveToPosition(pos);
        args.putString(ARG_ITEM_TO_EDIT,
                cr.getString(cr.getColumnIndexOrThrow("SpellItem")));
        args.putLong(ARG_RECORD_ID,
                cr.getLong(cr.getColumnIndexOrThrow("_id")));
        HashMap<Long, String> existingItems = new HashMap<Long, String>();
        cr.moveToFirst();
        while (cr.moveToNext()) {
            existingItems.put(cr.getLong(cr.getColumnIndexOrThrow("_id")),
                    cr.getString(cr.getColumnIndexOrThrow("SpellItem")));
        }
        args.putSerializable(ARG_EXISTING_VALUES, existingItems);
        int src = mSpellSourceSpinner.getSelectedItemPosition();
        switch (src) {
            case 0: // Species Namers
                args.putString(ARG_TABLE_NAME, "Namers");
                args.putString(ARG_TABLE_URI, "namers");
                args.putString(ARG_FIELD_NAME, "NamerName");
                args.putInt(ARG_INPUT_TYPE, InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                args.putInt(ARG_LENGTH_MIN, 2);
                args.putInt(ARG_LENGTH_MAX, 16);
                break;
            case 1: // Projects
                args.putString(ARG_TABLE_NAME, "Projects");
                args.putString(ARG_TABLE_URI, "projects");
                args.putString(ARG_FIELD_NAME, "ProjCode");
                args.putInt(ARG_LENGTH_MIN, 2);
                args.putInt(ARG_LENGTH_MAX, 10);
                break;
            case 2: // ID Namers
                args.putString(ARG_TABLE_NAME, "IdNamers");
                args.putString(ARG_TABLE_URI, "idnamers");
                args.putString(ARG_FIELD_NAME, "IdNamerName");
                args.putInt(ARG_INPUT_TYPE, InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                args.putInt(ARG_LENGTH_MIN, 2);
                args.putInt(ARG_LENGTH_MAX, 30);
                break;
            case 3: // ID References
                args.putString(ARG_TABLE_NAME, "IdRefs");
                args.putString(ARG_TABLE_URI, "idrefs");
                args.putString(ARG_FIELD_NAME, "IdRef");
                args.putInt(ARG_LENGTH_MIN, 2);
                args.putInt(ARG_LENGTH_MAX, 255);
                break;
            case 4: // ID Methods
                args.putString(ARG_TABLE_NAME, "IdMethods");
                args.putString(ARG_TABLE_URI, "idmethods");
                args.putString(ARG_FIELD_NAME, "IdMethod");
                args.putInt(ARG_LENGTH_MIN, 2);
                args.putInt(ARG_LENGTH_MAX, 255);
                break;

// do anything with these?
//        case Spinner.INVALID_POSITION:
//        default:
//        break;
        } // end of case that selects which table

        EditSpellingDialog edSplDlg = EditSpellingDialog.newInstance(args);
        edSplDlg.show(getFragmentManager(), "frg_edit_spelling");
        return;
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
            // add only the item "Delete" if relevant to the selection

        /* or use something like this to allow deleting or not
        int itemIsPlaceholder = mSpellItemsCursor.getInt(
                mSpellItemsCursor.getColumnIndexOrThrow("IsPlaceholder"));
        if (itemIsPlaceholder != 1) {
            Toast.makeText(getActivity(),
                    getActivity().getResources().getString(R.string.sel_spp_list_ctx_edit_ph_not),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        */            AdapterView.AdapterContextMenuInfo info;
            try {
                // Casts the incoming data object into the type for AdapterView objects.
                info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            } catch (ClassCastException e) {
                if (LDebug.ON) Log.d(LOG_TAG, "bad menuInfo", e); // if the menu object can't be cast
                break;
            }
/*
            mSpellItemsCursor.moveToPosition(info.position);
            int isPlaceHolder = mSpellItemsCursor.getInt(
                    mSpellItemsCursor.getColumnIndexOrThrow("IsPlaceholder"));
            if (isPlaceHolder == 0) {
                // if not a Placeholder, the 'edit Placeholder' option does not apply
                menu.removeItem(R.id.sel_spp_list_item_edit_ph);
            }
*/
			break;
        }
    }

    // This is executed when the user selects a context menu option
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
            // confirm that the item can be deleted, and verify with the user

            default:
                return super.onContextItemSelected(item);
       } // end of Switch
    }

    public void refreshItemsList() {
        // use after edit/delete
        if (LDebug.ON) Log.d(LOG_TAG, "in 'refreshItemsList'");
        getLoaderManager().restartLoader(Loaders.SPELL_ITEMS, null, this);
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

            case Loaders.SPELL_ITEMS:
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, SPELL_ITEMS");
                baseUri = ContentProvider_VegNab.SQL_URI;
                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                long mProjectId = sharedPref.getLong(VNContract.Prefs.DEFAULT_PROJECT_ID, 1);
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, SPELL_ITEMS, got ProjectID=" + mProjectId);
                List<String> prms = new ArrayList<String>(); // build parameter list dynamically
                int pos = mSpellSourceSpinner.getSelectedItemPosition();
                /*
                Items appear in the list as their text, of, course; but the second line gives
                some information so the user knows e.g. if the item can be deleted.
                For Namers: e.g. "Ellie Expert"
                Second line, e.g. "8 Visits, 14 Placeholders"
                If there is only one Project, all these are of course in that Project.
                If there is more than one Project, but all these are in that
                Project, then nothing else appears.
                If there are multiple Projects, and some Items are in the current Project, but
                some are also in other Projects, the second line appears e.g.
                "8 Visits, 14 Placeholders (9 Visits, 16 Placeholders in all Projects)"
                If there are none, this should make it obvious the Item is unused, and can be deleted.
                "0 Visits, 0 Placeholders"
                Unless the Item is used on Projects other than the current one.
                "0 Visits, 0 Placeholders (3 Visits, 5 Placeholders in all Projects)"
                The SQL derives a count of total usages in all Projects. If this is zero, the
                item can be deleted.
                The SQL compares the usages count for the current Project and for all
                Projects. If these are the same, the short form appears, otherwise the long form.
                */
                switch (pos) {
                    case 0: // Species Namers
                        select = "SELECT _id, NamerName AS SpellItem, ("
                                + " (SELECT count(_id)  FROM Visits"
                                + " WHERE Visits.NamerID = Namers._id AND Visits.ProjID = ?)"
                                + " + (SELECT count(_id)  FROM Placeholders"
                                + " WHERE Placeholders.NamerID = Namers._id AND Placeholders.ProjID = ?)"
                                + " ) UsageCount, ("
                                + " (SELECT count(_id) || "
                                + "CASE WHEN count(_id) = 1 THEN ' Visit' ELSE ' Visits' END "
                                + " FROM Visits"
                                + " WHERE Visits.NamerID = Namers._id)"
                                + " || ', ' || (SELECT count(_id) || "
                                + "CASE WHEN count(_id) = 1 THEN ' Placeholder' ELSE ' Placeholders' END"
                                + " FROM Placeholders"
                                + " WHERE Placeholders.NamerID = Namers._id)"
                                + "|| CASE WHEN (((SELECT count(_id)  FROM Visits"
                                + " WHERE Visits.NamerID = Namers._id AND Visits.ProjID = ?)"
                                + " + (SELECT count(_id) FROM Placeholders"
                                + " WHERE Placeholders.NamerID = Namers._id AND Placeholders.ProjID = ?)"
                                + ") == ((SELECT count(_id)  FROM Visits"
                                + " WHERE Visits.NamerID = Namers._id)"
                                + " + (SELECT count(_id) FROM Placeholders"
                                + " WHERE Placeholders.NamerID = Namers._id))) THEN '' ELSE ("
                                + " ' (' || (SELECT count(_id) || "
                                + "CASE WHEN count(_id) = 1 THEN ' Visit' ELSE ' Visits' END "
                                + " FROM Visits WHERE Visits.NamerID = Namers._id)"
                                + "|| ', ' || (SELECT count(_id) || "
                                + "CASE WHEN count(_id) = 1 THEN ' Placeholder' ELSE ' Placeholders' END"
                                + " FROM Placeholders WHERE Placeholders.NamerID = Namers._id)"
                                + "|| ' on all Projects)' ) END"
                                + " ) UsageNote FROM Namers;";
                        prms.add( "" + mProjectId );
                        prms.add( "" + mProjectId );
                        prms.add( "" + mProjectId );
                        prms.add( "" + mProjectId );
                        break;
                    case 1: // Projects
                        select = "SELECT _id, ProjCode AS SpellItem, ("
                                + "(SELECT count(_id) "
                                + "FROM Visits "
                                + "WHERE Visits.ProjID = Projects._id)"
                                + ") UsageCount, ("
                                + "(SELECT count(_id) || "
                                + "CASE WHEN count(_id) = 1 THEN ' Visit' ELSE ' Visits' END "
                                + "FROM Visits "
                                + "WHERE Visits.ProjID = Projects._id)"
                                + ") UsageNote  FROM Projects;";
                        break;

/* for testing, comment out the others
                    case 2: // ID Namers

                        break;
                    case 3: // ID References

                        break;
                    // for all the following, drop through
                    case 4: // ID Methods

                        break;
*/
                    case Spinner.INVALID_POSITION:
                    default:
                        // dummy query that gets no records
                        select = "SELECT _id, NamerName AS SpellItem, "
                            + "0 AS UsageCount, '' AS UsageNote "
                            + "FROM Namers WHERE Namers._id = 0;";
                        break;
                } // end of case that selects which table

                if (prms.size() == 0) {
                    params = null;
                } else {
                    params = new String[ prms.size() ];
                    prms.toArray( params );
                }
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, SPELL_ITEMS, got select=" + select);
                if (LDebug.ON) Log.d(LOG_TAG, "in onCreateLoader, SPELL_ITEMS, params=" + java.util.Arrays.toString(params));
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, params, null);
                break;

            // if any other Loaders, their code would go here
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor finishedCursor) {
        // there will be various loaders, switch them out here
        mRowCt = finishedCursor.getCount();
        switch (loader.getId()) {

            case Loaders.SPELL_ITEMS:
                // Swap the new cursor in.
                // The framework will take care of closing the old cursor once we return.
                mSpellItemsAdapter.swapCursor(finishedCursor);
                if (mRowCt == 0) {
//                    mViewForEmptyList.setText(
//                            getActivity().getResources().getString(R.string.sel_spp_search_msg_no_matches));
                }
//                refreshItemsList(); // may not have completed on start because this loader was not finished
                if (LDebug.ON) Log.d(LOG_TAG, "in onLoadFinished, SPELL_ITEMS, mRowCt=" + mRowCt);
                break;
            // code for any other loaders would go here
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed. Need to make sure it is no longer is use.
        switch (loader.getId()) {

            case Loaders.SPELL_ITEMS:
                mSpellItemsAdapter.swapCursor(null);
                break;
            // code for any other loaders would go here
        }
    }
}
