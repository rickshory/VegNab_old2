package com.vegnab.vegnab;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Loaders;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class VegSubplotFragment extends ListFragment 
		implements OnClickListener,
		LoaderManager.LoaderCallbacks<Cursor> {
	private static final String LOG_TAG = VegSubplotFragment.class.getSimpleName();
	
	public static final String POSITION_KEY = "FragmentPositionKey";
	private int mPosition = -1;
	
	public static final String VISIT_ID = "VisitId";
	private long mVisitId = 0;

	public static final String SUBPLOT_TYPE_ID = "SubplotTypeId";
	private long mSubplotTypeId = -1;

	public static final String PRESENCE_ONLY = "PresenceOnly";
	private boolean mPresenceOnly = true;

	public static final String HAS_NESTED = "HasNested";
	private boolean mHasNested = false;
	
	public static final String VISIT_NAME = "VisitName";
	private String mVisitName = "";

//	private int mSubplotLoaderId, mSppLoaderId;
	private int mSppLoaderId;
	
	OnButtonListener mButtonCallback; // declare the interface
	// declare that the container Activity must implement this interface
	public interface OnButtonListener {
		// methods that must be implemented in the container Activity
		public void onNewItemButtonClicked(int screenToReturnTo, long visitId, long subplotId, boolean presenceOnly);
	}
	VegItemAdapter mVegSubplotSppAdapter;
	ListView mVegItemsList;
	
	static VegSubplotFragment newInstance(Bundle args) {
		VegSubplotFragment f = new VegSubplotFragment();
		f.setArguments(args);
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//	setHasOptionsMenu(true);
		if (savedInstanceState == null) {
			Log.d(LOG_TAG, "onCreate FIRST TIME, position = " + mPosition);
		} else {
			Log.d(LOG_TAG, "onCreate SUBSEQUENT TIME, position = " + mPosition);
		}
// set up any interfaces
//		try {
//        	mEditNamerListener = (EditNamerDialogListener) getActivity();
//        	Log.d(LOG_TAG, "(EditNamerDialogListener) getActivity()");
//        } catch (ClassCastException e) {
//            throw new ClassCastException("Main Activity must implement EditNamerDialogListener interface");
//        }
	}	

/*	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.veg_subplot, menu);
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
//		case R.id.action_app_info:
//			Toast.makeText(getActivity(), "''App Info'' of Visit Header is not implemented yet", Toast.LENGTH_SHORT).show();
//			return true;
		case R.id.action_add_item:
			Toast.makeText(getActivity(), "''Add item'' of Veg Subplot is not implemented yet", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.action_veg_help:
			Toast.makeText(getActivity(), "''Help'' of Veg Subplot is not implemented yet", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.action_mark_no_veg:
			Toast.makeText(getActivity(), "''Mark no-veg'' of Veg Subplot is not implemented yet", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.action_go_to:
			Toast.makeText(getActivity(), "''Go to...'' of Veg Subplot is not implemented yet", Toast.LENGTH_SHORT).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
*/
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		mPosition = getArguments().getInt(POSITION_KEY);
		mVisitId = getArguments().getLong(VISIT_ID);
		mVisitName = getArguments().getString(VISIT_NAME);
		mSubplotTypeId = getArguments().getLong(SUBPLOT_TYPE_ID);
		mPresenceOnly = getArguments().getBoolean(PRESENCE_ONLY);
		
		// inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_veg_subplot, container, false);
		// set up the diagnostics
		TextView textview = (TextView) rootView.findViewById(R.id.txt_test_pager);
		textview.setText("Position=" + mPosition + ", VisitId=" + mVisitId +
				", Visit Name: '" + mVisitName + "'" +
				", SubplotTypeId=" + mSubplotTypeId + ", PresenceOnly=" + (mPresenceOnly ? 1 : 0));
		// for now, hide this; comment out following line to show for diagnostics
		textview.setVisibility(View.GONE);
		// eventually remove it entirely

		// set click listener for the buttons in the view
		rootView.findViewById(R.id.subplotNewItemButton).setOnClickListener(this);
		// if more, loop through all the child items of the ViewGroup rootView and 
		// set the onclicklistener for all the Button instances found
		
		// use query to return 'SppLine', concatenated from code and description; more reading room
		mVegSubplotSppAdapter = new VegItemAdapter(getActivity(),
				R.layout.list_veg_item, null, 0);
		setListAdapter(mVegSubplotSppAdapter);
		mVegItemsList = (ListView) rootView.findViewById(android.R.id.list);
		registerForContextMenu(mVegItemsList);
//		mVegItemsList.setOnItemClickListener(this);

		Log.d(LOG_TAG, "onCreateView, position = " + mPosition);
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(LOG_TAG, "onStart, position = " + mPosition);
//		mSubplotLoaderId = Loaders.BASE_SUBPLOT + (int) mSubplotTypeId;
//		getLoaderManager().initLoader(mSubplotLoaderId, null, this);
		mSppLoaderId = Loaders.BASE_SUBPLOT_SPP + (int) mSubplotTypeId;
		getLoaderManager().initLoader(mSppLoaderId, null, this);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(LOG_TAG, "onAttach, position = " + mPosition);
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
		Log.d(LOG_TAG, "onSaveInstanceState, position = " + mPosition);
		// save the current subplot arguments in case we need to re-create the fragment
		outState.putLong(VISIT_ID, mVisitId);
		outState.putLong(SUBPLOT_TYPE_ID, mSubplotTypeId);
	}

	// create context menus
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	   ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		switch (v.getId()) {
		case android.R.id.list:
			inflater.inflate(R.menu.context_veg_sbpl_list_item, menu);
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

	case R.id.veg_subl_list_item_edit:
		Log.d(LOG_TAG, "Veg item 'Edit' selected");
		headerContextTracker.send(new HitBuilders.EventBuilder()
				.setCategory("Veg Subplot Event")
				.setAction("Context Menu")
				.setLabel("Veg Item Edit")
				.setValue(1)
				.build());
		// Search Characters help
		helpTitle = "Edit";
		helpMessage = "Edit tapped";
		flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
		flexHlpDlg.show(getFragmentManager(), "frg_veg_item_edit");
		return true;

	case R.id.veg_subl_list_item_delete:
		Log.d(LOG_TAG, "Veg item 'Delete' selected");
		headerContextTracker.send(new HitBuilders.EventBuilder()
				.setCategory("Veg Subplot Event")
				.setAction("Context Menu")
				.setLabel("Veg Item Delete")
				.setValue(1)
				.build());
		// Search Characters help
		helpTitle = "Delete";
		helpMessage = "Delete tapped";
		flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
		flexHlpDlg.show(getFragmentManager(), "frg_veg_item_delete");
		return true;

	case R.id.veg_subl_list_item_help:
		Log.d(LOG_TAG, "Veg item 'Help' selected");
		headerContextTracker.send(new HitBuilders.EventBuilder()
				.setCategory("Veg Subplot Event")
				.setAction("Context Menu")
				.setLabel("Veg Item Help")
				.setValue(1)
				.build());
		// Search Characters help
		helpTitle = c.getResources().getString(R.string.veg_subpl_help_list_item_title);
		helpMessage = c.getResources().getString(R.string.veg_subpl_help_list_item_text);
		flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
		flexHlpDlg.show(getFragmentManager(), "frg_veg_item_help");
		return true;

    default:
    	return super.onContextItemSelected(item);
	   }
	}


	@Override
	public void onStop() {
		super.onStop();
		Log.d(LOG_TAG, "onStop, position = " + mPosition);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy, position = " + mPosition);
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.d(LOG_TAG, "onDestroyView, position = " + mPosition);
	}	
	
	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(LOG_TAG, "onDetach, position = " + mPosition);
	}	
	
	@Override
	public void onPause() {
		super.onPause();
		Log.d(LOG_TAG, "onPause, position = " + mPosition);
	}	
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume, position = " + mPosition);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(LOG_TAG, "onActivityCreated, position = " + mPosition);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.subplotNewItemButton:
			mButtonCallback.onNewItemButtonClicked(mPosition, mVisitId, mSubplotTypeId, mPresenceOnly);
			break;
		}
	}
	
	public void refreshSppList() {
		// when the referred Loader callback returns, will refresh the currently used species
		getLoaderManager().restartLoader(mSppLoaderId, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// This is called when a new Loader needs to be created.
		// switch out based on id
		CursorLoader cl = null;
		Uri baseUri;
		String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
//		if (id == mSubplotLoaderId) {
//			// retrieve any needed header info
//			baseUri = ContentProvider_VegNab.SQL_URI;
//			select = "SELECT SubplotDescription, PresenceOnly, HasNested " 
//					+ "FROM SubplotTypes WHERE _id = " + mSubplotTypeId + ";";
//			cl = new CursorLoader(getActivity(), baseUri,
//					null, select, null, null);
//		}
		if (id == mSppLoaderId) {
			baseUri = ContentProvider_VegNab.SQL_URI;
			// get any species entries for this subplot of this visit
			select = "SELECT VegItems._id, VegItems.OrigCode, VegItems.OrigDescr, "
					+ "VegItems.OrigCode || ': ' || VegItems.OrigDescr AS SppLine , "
					+ "VegItems.Height, VegItems.Cover, VegItems.Presence, VegItems.IdLevelID, "
					+ "IdLevels.IdLevelDescr, IdLevels.IdLevelLetterCode "
					+ "FROM VegItems LEFT JOIN IdLevels ON VegItems.IdLevelID = IdLevels._id "
					+ "WHERE (((VegItems.VisitID)=?) AND ((VegItems.SubPlotID)=?)) "
					+ "ORDER BY VegItems.TimeLastChanged DESC;";
			Log.d(LOG_TAG, "onCreateLoader CURRENT_SUBPLOT_SPP, mVisitId=" + mVisitId
					+ ", mSubplotTypeId=" + mSubplotTypeId);
			String[] sppSelectionArgs = { "" + mVisitId, "" + mSubplotTypeId };
			cl = new CursorLoader(getActivity(), baseUri,
					null, select, sppSelectionArgs, null);
		}
		return cl;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		// there will be various loaders, switch them out here
//		mRowCt = finishedCursor.getCount();
		int loaderId = loader.getId();
//		if (loaderId == mSubplotLoaderId) {
//			// fill in any header info
//			// SubplotDescription, PresenceOnly, HasNested
//			int rowCt = c.getCount();
//			Log.d(LOG_TAG, "onLoadFinished CURRENT_SUBPLOT, number of rows returned: " + rowCt);
//			if (c.moveToNext()) {
//				mPresenceOnly = ((c.getInt(c.getColumnIndexOrThrow("PresenceOnly")) == 1) ? true : false);
//			}
//		}
		if (loaderId == mSppLoaderId) {
			Log.d(LOG_TAG, "onLoadFinished CURRENT_SUBPLOT_SPP, number of rows returned: " + c.getCount());
			mVegSubplotSppAdapter.swapCursor(c);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// is about to be closed. Need to make sure it is no longer is use.
		int loaderId = loader.getId();
//		if (loaderId == mSubplotLoaderId) {
//			// no adapter, nothing to do
//		}
		if (loaderId == mSppLoaderId) {
			mVegSubplotSppAdapter.swapCursor(null);
		}
	}
}
