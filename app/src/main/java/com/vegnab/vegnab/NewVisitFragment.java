package com.vegnab.vegnab;

import java.util.List;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VegNabDbHelper;
import com.vegnab.vegnab.database.VNContract.Prefs;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.support.v7.internal.widget.AdapterViewCompat.OnItemSelectedListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
	int mRowCt;
	final static String ARG_SUBPLOT = "subplot";
	int mCurrentSubplot = -1;
	Spinner mProjSpinner, mPlotTypeSpinner;
	SimpleCursorAdapter mProjAdapter, mPlotTypeAdapter, mVisitListAdapter;
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
		// set click listener for the button in the view
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
		getLoaderManager().initLoader(Loaders.PROJECTS, null, this);
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
		getLoaderManager().initLoader(Loaders.PLOTTYPES, null, this);
		
		mVisitListAdapter = new SimpleCursorAdapter(getActivity(),
				android.R.layout.simple_list_item_1, null,
				new String[] {"VisitName"},
				new int[] {android.R.id.text1}, 0);
		setListAdapter(mVisitListAdapter);
		getLoaderManager().initLoader(Loaders.PREV_VISITS, null, this);

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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.new_visit_go_button:
			// test of using the Content Provider for direct SQL
			getLoaderManager().initLoader(Loaders.TEST_SQL, null, this);
			
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

	public void refreshVisitsList() {
		// when the referred Loader callback returns, will update the list of Visits
		getLoaderManager().restartLoader(Loaders.PREV_VISITS, null, this);
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// This is called when a new Loader needs to be created.
		// switch out based on id
		CursorLoader cl = null;
		Uri baseUri;
		String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
		switch (id) {
		case Loaders.TEST_SQL:
			baseUri = ContentProvider_VegNab.SQL_URI;
			select = "SELECT StartDate FROM Projects WHERE _id = 1;";
			cl = new CursorLoader(getActivity(), baseUri,
					null, select, null, null);
			break;
		case Loaders.PROJECTS:
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
		case Loaders.PLOTTYPES:
			baseUri = ContentProvider_VegNab.SQL_URI;
			select = "SELECT _id, PlotTypeDescr FROM PlotTypes;";
			cl = new CursorLoader(getActivity(), baseUri,
					null, select, null, null);
			break;
		case Loaders.PREV_VISITS:
			baseUri = ContentProvider_VegNab.SQL_URI;
			select = "SELECT _id, VisitName, VisitDate FROM Visits " 
					+ "WHERE ShowOnMobile = 1 AND IsDeleted = 0 " 
					+ "ORDER BY LastChanged DESC;";
			cl = new CursorLoader(getActivity(), baseUri,
					null, select, null, null);
			break;

		
//Loaders.		
		}
		return cl;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor finishedCursor) {
		// there will be various loaders, switch them out here
		mRowCt = finishedCursor.getCount();
		switch (loader.getId()) {
		case Loaders.TEST_SQL:
			Log.v(LOG_TAG, "Loaders.TEST_SQL returned cursor ");
			finishedCursor.moveToFirst();
			String d = finishedCursor.getString(0);
			Log.v(LOG_TAG, "Loaders.TEST_SQL value returned: " + d);
/*			Toast.makeText(this.getActivity(),
					"Date: " + d,
					Toast.LENGTH_LONG).show();
*/
			break;
		case Loaders.PROJECTS:
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
					Log.v(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PROJECT_ID + "' does not exist yet.");
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
					Log.v(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PROJECT_ID + "' set for the first time."); 
				} else {
/*					Toast.makeText(this.getActivity(), 
							"Prefs key '" + PREF_DEFAULT_PROJECT_ID + "' = " + mProjectId, 
							Toast.LENGTH_LONG).show();
*/
					Log.v(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PROJECT_ID + "' = " + mProjectId);
				}
				// set the default Project to show in its spinner
				// for a generalized fn, try: mProjSpinner.getAdapter().getCount()
				for (int i=0; i<mRowCt; i++) {
					Log.v(LOG_TAG, "Setting mProjSpinner default; testing index " + i);
					if (mProjSpinner.getItemIdAtPosition(i) == mProjectId) {
						Log.v(LOG_TAG, "Setting mProjSpinner default; found matching index " + i);
						mProjSpinner.setSelection(i);
						break;
					}
				}
			} else {
				mProjSpinner.setEnabled(false);
			}
			break;
		case Loaders.PLOTTYPES:
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
					Log.v(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' does not exist yet.");
					saveDefaultPlotTypeId(mPlotTypeId);
/*					Toast.makeText(this.getActivity(), 
							"Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' set for the first time.", 
							Toast.LENGTH_LONG).show();
*/
					Log.v(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' set for the first time."); 
				} else {
/*					Toast.makeText(this.getActivity(), 
							"Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' = " + mPlotTypeId, 
							Toast.LENGTH_LONG).show();
*/
					Log.v(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PROJECT_ID + "' = " + mPlotTypeId);
				}
				// set the default Plot Type to show in its spinner
				// for a generalized fn, try: mySpinner.getAdapter().getCount()
				for (int i=0; i<mRowCt; i++) {
					Log.v(LOG_TAG, "Setting mPlotTypeSpinner default; testing index " + i);
					if (mPlotTypeSpinner.getItemIdAtPosition(i) == mPlotTypeId) {
						Log.v(LOG_TAG, "Setting mPlotTypeSpinner default; found matching index " + i);
						mPlotTypeSpinner.setSelection(i);
						break;
					}
				}
			} else {
				mPlotTypeSpinner.setEnabled(false);
			}
			break;

		case Loaders.PREV_VISITS:
			mVisitListAdapter.swapCursor(finishedCursor);
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// is about to be closed. Need to make sure it is no longer is use.
		switch (loader.getId()) {
		case Loaders.PROJECTS:
			mProjAdapter.swapCursor(null);
			break;
		case Loaders.PLOTTYPES:
			mPlotTypeAdapter.swapCursor(null);
			break;
		case Loaders.PREV_VISITS:
			mVisitListAdapter.swapCursor(null);
			break;
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
		//Log.v(LOG_TAG, strSel);
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
		Log.v("NewVist", "In NewVisitFragment, caught 'onBackPressed'");
	return;
	}
*/	
	public void showDatePickerDialog(View v) {
		Log.v("NewVisit", "Event caught in NewVisitFragment");
	}

}
