package com.vegnab.vegnab;

import java.util.HashMap;
import java.util.HashSet;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.VegcodeSources;
import com.vegnab.vegnab.database.VNContract.VNRegex;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class SelectSpeciesFragment extends ListFragment 
		implements LoaderManager.LoaderCallbacks<Cursor>{
	private static final String LOG_TAG = SelectSpeciesFragment.class.getSimpleName();
	final static String ARG_VISIT_ID = "visId";
	final static String ARG_SUBPLOT_TYPE_ID = "sbpId";
	final static String ARG_PRESENCE_ONLY_SUBPLOT = "presenceOnly";
	final static String ARG_PROJECT_ID = "projectId";
	final static String ARG_NAMER_ID = "namerId";
	final static String ARG_SEARCH_TEXT = "search_text";
	final static String ARG_SQL_TEXT = "sql_text";
	final static String ARG_SEARCH_FULL_LIST = "regional_list";
	final static String ARG_USE_FULLTEXT_SEARCH = "fulltext_search";
	
	long mCurVisitRecId = 0;
	long mCurSubplotTypeRecId = 0;
	boolean mPresenceOnly = true;
	long mProjectId = 0;
	long mNamerId = 0;
	HashMap<String, Long> mPlaceholderCodesForThisNamer = new HashMap<String, Long>();
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
	// add option checkboxes or radio buttons to set the above; or do from menu items
	EditText mViewSearchChars;
	TextWatcher sppCodeTextWatcher = new TextWatcher() {
		@Override
		public void afterTextChanged(Editable s) {
			// use this method; test length of string; e.g. 'count' of other methods does not give this length
			//Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "'");
			Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "', length: " + s.length());
			mStSearch = s.toString();
			getLoaderManager().restartLoader(Loaders.SPP_MATCHES, null, SelectSpeciesFragment.this);
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
		}
		// inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_sel_species, container, false);
		mViewSearchChars = (EditText) rootView.findViewById(R.id.txt_search_chars);
		
		mViewSearchChars.addTextChangedListener(sppCodeTextWatcher);
		registerForContextMenu(mViewSearchChars); // enable long-press

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
			mProjectId = args.getLong(ARG_PROJECT_ID);
			mNamerId = args.getLong(ARG_NAMER_ID);
			/*	final static String ARG_VISIT_ID = "visId";
	final static String ARG_SUBPLOT_TYPE_ID = "sbpId";
	final static String ARG_PRESENCE_ONLY_SUBPLOT = "presenceOnly";
	final static String ARG_SEARCH_TEXT = "search_text";
	final static String ARG_SQL_TEXT = "sql_text";
	final static String ARG_SEARCH_FULL_LIST = "regional_list";
	final static String ARG_USE_FULLTEXT_SEARCH = "fulltext_search";
*/
		}
		// get following to disallow duplicate Placeholder definitions
		getLoaderManager().initLoader(Loaders.EXISTING_PLACEHOLDER_CODES, null, this);
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

		outState.putLong(ARG_VISIT_ID, mCurVisitRecId);
		outState.putLong(ARG_SUBPLOT_TYPE_ID, mCurSubplotTypeRecId);
		outState.putBoolean(ARG_PRESENCE_ONLY_SUBPLOT, mPresenceOnly);
		outState.putLong(ARG_PROJECT_ID, mProjectId);
		outState.putLong(ARG_NAMER_ID, mNamerId);
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
		Log.d(LOG_TAG, "mSppMatchCursor, pos = " + pos + " SppCode: " + vegCode);
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

	// create context menus
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	   ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		switch (v.getId()) {
		case R.id.txt_search_chars:
			inflater.inflate(R.menu.context_sel_spp_search_chars, menu);
			break;
		}
	}

	// This is executed when the user selects an option
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	AdapterViewCompat.AdapterContextMenuInfo info = (AdapterViewCompat.AdapterContextMenuInfo) item.getMenuInfo();
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

	case R.id.sel_spp_search_add_placeholder:
		Log.d(LOG_TAG, "'Create Placeholder' selected");
		headerContextTracker.send(new HitBuilders.EventBuilder()
				.setCategory("Species Select Event")
				.setAction("Context Menu")
				.setLabel("Create Placeholder")
				.setValue(1)
				.build());
		// add or edit Placeholder
		String phCode = mStSearch.trim();
		Toast.makeText(this.getActivity(), "Placeholder code '" + phCode + "'", Toast.LENGTH_SHORT).show();
		if (phCode.length() < 3) {
			Toast.makeText(this.getActivity(), "Placeholder codes must be at least 3 characters long.", Toast.LENGTH_SHORT).show();
			return true;
		}
		String nrcsCodePattern = "[a-zA-Z]{3,5}[0-9]*|2[a-zA-Z]{1,4}";
		// disallow 3 to 5 letters, alone or followed by numbers
		// disallow any with numerals trailing 3-5 letters, though never saw real codes with more than 2 digits here
		// also disallow codes like "2FDA" (forb dicot annual) some agencies use for general ids
		if (phCode.matches(VNRegex.NRCS_CODE)) {
			Toast.makeText(this.getActivity(), "Placeholder can\'t be like an NRCS code.", Toast.LENGTH_SHORT).show();
			return true;
		}

		if (mPlaceholderCodesForThisNamer.containsKey(phCode)) {
			Toast.makeText(this.getActivity(), "Placeholder code \"" + phCode + "\" is already used.", Toast.LENGTH_SHORT).show();
			return true;
		}

// replace following with Placeholder dialog
//		EditPlaceholderDialog editPhDlg = EditPlaceholderDialog.newInstance(phCode);
//		editPhDlg.show(getFragmentManager(), "frg_edit_placeholder");
// for menu testing, pop us some Help
		helpTitle = "New Placeholder";
		helpMessage = "This will open the Placeholder dialog";
		flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
		flexHlpDlg.show(getFragmentManager(), "frg_help_phld_dlg");
		return true;

	case R.id.sel_spp_search_help:
		Log.d(LOG_TAG, "'Search Chars Help' selected");
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
    default:
    	return super.onContextItemSelected(item);
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

		case Loaders.SPP_MATCHES:
			baseUri = ContentProvider_VegNab.SQL_URI;
			if (mStSearch.trim().length() == 0) {
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
						+ "UNION SELECT _id, PlaceHolderCode AS Code, '' AS Genus, '' AS Species, "
						+ "'' AS SubsppVar, '' AS Vernacular, "
						+ "Description AS MatchTxt, 1 AS SubListOrder "
						+ "FROM PlaceHolders "
						+ "WHERE Code Like '" + mStSearch + "%' "
						+ "AND ProjID=" + mProjectId + " AND PlaceHolders.NamerID=" + mNamerId + " "
						+ "UNION SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, "
						+ "Code || ': ' || Genus || "
						+ "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
						+ "AS MatchTxt, 2 AS SubListOrder FROM RegionalSpeciesList "
						+ "WHERE MatchTxt LIKE '%" + mStSearch + "%' AND Local = 1 AND HasBeenFound = 1 "
						+ "UNION SELECT _id, PlaceHolderCode AS Code, '' AS Genus, '' AS Species, "
						+ "'' AS SubsppVar, '' AS Vernacular, "
						+ "Description AS MatchTxt, 2 AS SubListOrder "
						+ "FROM PlaceHolders "
						+ "WHERE MatchTxt Like '%" + mStSearch + "%' "
						+ "AND ProjID=" + mProjectId + " AND PlaceHolders.NamerID=" + mNamerId + " "
						+ "UNION SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, "
						+ "Code || ': ' || Genus || "
						+ "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
						+ "AS MatchTxt, 3 AS SubListOrder FROM RegionalSpeciesList "
						+ "WHERE Code LIKE '" + mStSearch + "%' AND Local = 1 AND HasBeenFound = 0 "
						+ "UNION SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, "
						+ "Code || ': ' || Genus || "
						+ "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
						+ "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
						+ "AS MatchTxt, 4 AS SubListOrder FROM RegionalSpeciesList "
						+ "WHERE MatchTxt LIKE '%" + mStSearch + "%' AND Local = 1 AND HasBeenFound = 0 "
						+ "ORDER BY SubListOrder, Code;";
			}

			select = mStSQL;
			cl = new CursorLoader(getActivity(), baseUri,
					null, select, null, null);
			break;

			case Loaders.EXISTING_PLACEHOLDER_CODES:
				baseUri = ContentProvider_VegNab.SQL_URI;
				select = "SELECT PlaceHolders._id, PlaceHolders.PlaceHolderCode "
					+ "FROM PlaceHolders LEFT JOIN Visits ON PlaceHolders.VisitIdWhereFirstFound = Visits._id "
					+ "WHERE Visits.ProjID = ? "
					+ "AND Visits.NamerID = ?;";
				cl = new CursorLoader(getActivity(), baseUri, null, select,
						new String[] { "" + mProjectId, "" + mNamerId }, null);
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

		case Loaders.EXISTING_PLACEHOLDER_CODES:
			mPlaceholderCodesForThisNamer.clear();
			while (finishedCursor.moveToNext()) {
/*				String code;
				Log.d(LOG_TAG, "Namer already used code: '" + code + "'");
				code = finishedCursor.getString(
						finishedCursor.getColumnIndexOrThrow("PlaceHolderCode"));
				mVegCodesAlreadyOnSubplot.add(code);
*/
				mPlaceholderCodesForThisNamer.put(finishedCursor.getString(
						finishedCursor.getColumnIndexOrThrow("PlaceHolderCode")),
						finishedCursor.getLong(
						finishedCursor.getColumnIndexOrThrow("_id")));
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
		case Loaders.EXISTING_PLACEHOLDER_CODES:
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
