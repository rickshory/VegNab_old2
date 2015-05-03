package com.vegnab.vegnab;

import java.util.HashSet;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.VegcodeSources;

import android.app.Activity;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

public class SelectSpeciesFragment extends ListFragment 
		implements LoaderManager.LoaderCallbacks<Cursor>{
	private static final String LOG_TAG = SelectSpeciesFragment.class.getSimpleName();
	final static String ARG_VISIT_ID = "visId";
	final static String ARG_SUBPLOT_TYPE_ID = "sbpId";
	final static String ARG_PRESENCE_ONLY_SUBPLOT = "presenceOnly";
	final static String ARG_SEARCH_TEXT = "search_text";
	final static String ARG_SQL_TEXT = "sql_text";
	final static String ARG_SEARCH_FULL_LIST = "regional_list";
	final static String ARG_USE_FULLTEXT_SEARCH = "fulltext_search";
	
	long mCurVisitRecId = 0;
	long mCurSubplotTypeRecId = 0;
	boolean mPresenceOnly = true; 
	HashSet<String> mVegCodesAlreadyOnSubplot = new HashSet<String>();
	Cursor mSppMatchCursor;
	
	SimpleCursorAdapter mSppResultsAdapter;
	// declare that the container Activity must implement this interface

	public interface OnSppResultClickListener {
		// methods that must be implemented in the container Activity
		public void onSppMatchListClicked(int sourceId, long recId, String vegCode, String vegDescr,
				String vegGenus, String vegSpecies, String vegSubsppVar, String vegVernacular);
//		public void onSelSppDone();
	}
	OnSppResultClickListener mListClickCallback;
	long mRowCt;
	String mStSearch = "", mStSQL = "SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, " 
			+ "Code || ': ' || Genus AS MatchTxt FROM RegionalSpeciesList " 
			+ "WHERE Code LIKE '';";  // dummy query that gets no records
	// mSearchFullList false = search only species previously found, plus Placeholders
	// mSearchFullList true = search the entire big regional list of species
	// mUseFullText false = search only the codes (species plus Placeholders), matching the start of the code
	// mUseFullText true = search all positions of the concatenated code + description
	Boolean mSearchFullList = false, mUseFullText = false; // defaults
	// add option checkboxes or radio buttons to set the above; or do from menu items
	EditText mViewSearchChars;
	TextWatcher sppCodeTextWatcher = new TextWatcher() {
		@Override
		public void afterTextChanged(Editable s) {
			// use this method; test length of string; e.g. 'count' of other methods does not give this length
			//Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "'");
			Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "', length: " + s.length());
			mStSearch = s.toString();
			if (s.length() == 0) {
				mStSQL = "SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, " 
						+ "Code || ': ' || Genus AS MatchTxt FROM RegionalSpeciesList " 
						+ "WHERE Code LIKE '';";  // dummy query that gets no records
			} else {
				mStSQL = "SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, "
						+ "Code || ': ' || Genus || "
						+ "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
						+ "AS MatchTxt, 1 AS SubListOrder FROM RegionalSpeciesList "
						+ "WHERE Code LIKE '" + mStSearch + "%' AND Local = 1 AND HasBeenFound = 1 "
						+ "UNION SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, "
						+ "Code || ': ' || Genus || "
						+ "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
						+ "AS MatchTxt, 4 AS SubListOrder FROM RegionalSpeciesList "
						+ "WHERE Vernacular LIKE '%" + mStSearch + "%' AND Local = 1 AND HasBeenFound = 1 "
						+ "UNION SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, "
						+ "Code || ': ' || Genus || "
						+ "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
						+ "AS MatchTxt, 5 AS SubListOrder FROM RegionalSpeciesList "
						+ "WHERE Code LIKE '" + mStSearch + "%' AND Local = 1 AND HasBeenFound = 0 "
						+ "UNION SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, "
						+ "Code || ': ' || Genus || "
						+ "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
						+ "AS MatchTxt, 8 AS SubListOrder FROM RegionalSpeciesList "
						+ "WHERE Vernacular LIKE '%" + mStSearch + "%' AND Local = 1 AND HasBeenFound = 0 "
						+ "ORDER BY SubListOrder, Code;"
				;
			}
			getLoaderManager().restartLoader(Loaders.SPP_MATCHES, null, SelectSpeciesFragment.this);
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
	
/*
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.new_visit, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
*/
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		// if the activity was re-created (e.g. from a screen rotate)
		// restore the previous screen, remembered by onSaveInstanceState()
		// This is mostly needed in fixed-pane layouts
		if (savedInstanceState != null) {
			// restore search text and any search options
			mStSearch = savedInstanceState.getString(ARG_SEARCH_TEXT);
			mStSQL = savedInstanceState.getString(ARG_SQL_TEXT);
			mSearchFullList = savedInstanceState.getBoolean(ARG_SEARCH_FULL_LIST);
			mUseFullText = savedInstanceState.getBoolean(ARG_USE_FULLTEXT_SEARCH);
		} /*SELECT _id, Code || ': ' || SppDescr AS MatchTxt FROM SpeciesFound WHERE Code LIKE '' ;*/
		// inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_sel_species, container, false);
		mViewSearchChars = (EditText) rootView.findViewById(R.id.txt_search_chars);
		
		mViewSearchChars.addTextChangedListener(sppCodeTextWatcher);

		// use query to return 'MatchTxt', concatenated from code and description; more reading room
		mSppResultsAdapter = new SimpleCursorAdapter(getActivity(),
				android.R.layout.simple_list_item_1, null,
				new String[] {"MatchTxt"},
				new int[] {android.R.id.text1}, 0);
		setListAdapter(mSppResultsAdapter);
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
			mCurVisitRecId = args.getLong(ARG_VISIT_ID);
			mCurSubplotTypeRecId = args.getLong(ARG_SUBPLOT_TYPE_ID);
			mPresenceOnly = args.getBoolean(ARG_PRESENCE_ONLY_SUBPLOT);
			/*	final static String ARG_VISIT_ID = "visId";
	final static String ARG_SUBPLOT_TYPE_ID = "sbpId";
	final static String ARG_PRESENCE_ONLY_SUBPLOT = "presenceOnly";
	final static String ARG_SEARCH_TEXT = "search_text";
	final static String ARG_SQL_TEXT = "sql_text";
	final static String ARG_SEARCH_FULL_LIST = "regional_list";
	final static String ARG_USE_FULLTEXT_SEARCH = "fulltext_search";
*/
		}
		// get following, to warn about duplicates
		getLoaderManager().initLoader(Loaders.CODES_ALREADY_ON_SUBPLOT, null, this);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// assure the container activity has implemented the callback interface
		try {
			mListClickCallback = (OnSppResultClickListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException (activity.toString() + " must implement OnVisitClickListener");
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// save the current search text and any options
		outState.putString(ARG_SEARCH_TEXT, mStSearch);
		outState.putString(ARG_SQL_TEXT, mStSQL);
		outState.putBoolean(ARG_SEARCH_FULL_LIST, mSearchFullList);
		outState.putBoolean(ARG_USE_FULLTEXT_SEARCH, mUseFullText);
	}
	
    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
//        Toast.makeText(this.getActivity(), "Clicked position " + pos + ", id " + id, Toast.LENGTH_SHORT).show();
    	// check if selected code is in mVegCodesAlreadyOnSubplot
//    	getListView().getItemAtPosition(pos).toString(); // not useful, gets cursor wrapper
    	mSppMatchCursor.moveToPosition(pos);
// available fields: _id, Code, Genus, Species, SubsppVar, Vernacular, MatchTxt
        String vegCode = mSppMatchCursor.getString(
        		mSppMatchCursor.getColumnIndexOrThrow("Code"));
        String vegDescr = mSppMatchCursor.getString(
        		mSppMatchCursor.getColumnIndexOrThrow("MatchTxt"));

        String vegGenus = mSppMatchCursor.getString(
        		mSppMatchCursor.getColumnIndexOrThrow("Genus"));
        String vegSpecies = mSppMatchCursor.getString(
        		mSppMatchCursor.getColumnIndexOrThrow("Species"));
        String vegSubsppVar = mSppMatchCursor.getString(
        		mSppMatchCursor.getColumnIndexOrThrow("SubsppVar"));
        String vegVernacular = mSppMatchCursor.getString(
        		mSppMatchCursor.getColumnIndexOrThrow("Vernacular"));
        
        
        Log.d(LOG_TAG, "mSppMatchCursor, pos = " + pos + " SppCode: " + vegCode);
        if (mVegCodesAlreadyOnSubplot.contains(vegCode)) {
        	// warn user and allow to cancel
        }
        Log.d(LOG_TAG, "about to dispatch 'EditSppItemDialog' dialog to create new record");
        Bundle args = new Bundle();
        args.putLong(EditSppItemDialog.VEG_ITEM_REC_ID, 0); // don't need this, default is in class
        args.putLong(EditSppItemDialog.CUR_VISIT_REC_ID, mCurVisitRecId);
        args.putLong(EditSppItemDialog.CUR_SUBPLOT_REC_ID, mCurSubplotTypeRecId);
        args.putInt(EditSppItemDialog.REC_SOURCE, VegcodeSources.REGIONAL_LIST);
        args.putLong(EditSppItemDialog.SOURCE_REC_ID, id);
        args.putBoolean(EditSppItemDialog.PRESENCE_ONLY, mPresenceOnly);
        // streamline this, get directly from cursor
        args.putString(EditSppItemDialog.VEG_CODE, vegCode);
        args.putString(EditSppItemDialog.VEG_DESCR, vegDescr);
        args.putString(EditSppItemDialog.VEG_GENUS, vegGenus);
        args.putString(EditSppItemDialog.VEG_SPECIES, vegSpecies);
        args.putString(EditSppItemDialog.VEG_SUBSPP_VAR, vegSubsppVar);
        args.putString(EditSppItemDialog.VEG_VERNACULAR, vegVernacular);
        
        EditSppItemDialog newVegItemDlg = EditSppItemDialog.newInstance(args);

        newVegItemDlg.show(getFragmentManager(), "frg_new_veg_item");
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// This is called when a new Loader needs to be created.
		// switch out based on id
		CursorLoader cl = null;
		Uri baseUri;
		String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
		switch (id) {

		case Loaders.SPP_MATCHES:
			baseUri = ContentProvider_VegNab.SQL_URI;
			select = mStSQL;
			cl = new CursorLoader(getActivity(), baseUri,
					null, select, null, null);
			break;		
		
		case Loaders.CODES_ALREADY_ON_SUBPLOT:
			baseUri = ContentProvider_VegNab.SQL_URI;
			select = "SELECT OrigCode FROM VegItems WHERE VisitID = " + mCurVisitRecId 
					+ " AND SubPlotID = " + mCurSubplotTypeRecId + " GROUP BY OrigCode;";
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
		case Loaders.SPP_MATCHES:
			mSppResultsAdapter.swapCursor(finishedCursor);
			mSppMatchCursor = finishedCursor;
			break;
			
		case Loaders.CODES_ALREADY_ON_SUBPLOT:
			mVegCodesAlreadyOnSubplot.clear();
			while (finishedCursor.moveToNext()) {
				mVegCodesAlreadyOnSubplot.add(finishedCursor.getString(
						finishedCursor.getColumnIndexOrThrow("OrigCode")).toString());
			}
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// is about to be closed. Need to make sure it is no longer is use.
		switch (loader.getId()) {
		case Loaders.SPP_MATCHES:
			mSppResultsAdapter.swapCursor(null);
			break;
			
		case Loaders.CODES_ALREADY_ON_SUBPLOT:
			// not an adapter, nothing to do here
			break;
		}
	}

/*
	// no Override
	public static void onBackPressed() {
		Log.d("NewVist", "In NewVisitFragment, caught 'onBackPressed'");
	return;
	}
*/	


}
