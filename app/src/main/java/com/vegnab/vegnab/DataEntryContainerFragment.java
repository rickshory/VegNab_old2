package com.vegnab.vegnab;

import java.lang.ref.WeakReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Loaders;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DataEntryContainerFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = DataEntryContainerFragment.class.getSimpleName();
    public static final String TAG = DataEntryContainerFragment.class.getName();
    public static final String VISIT_ID = "VisitId";
    static long mVisitId = 0; // new or not specified yet
    public static int mScreenToShow = 0; // default unless changed
    static SparseArray<String> mSubplotNames = new SparseArray<String>();
    static Cursor mSubplotsCursor;
    private JSONObject mSubplotSpec = new JSONObject();
    private JSONArray mPlotSpecs = new JSONArray();

    public ViewPager mDataScreenPager = null;
    public static DataEntryContainerFragment newInstance(Bundle args) {
        DataEntryContainerFragment f = new DataEntryContainerFragment();
        f.setArguments(args);
        return f;
    }

//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
// set up any interfaces
//		try {
//        	mEditNamerListener = (EditNamerDialogListener) getActivity();
//        	Log.d(LOG_TAG, "(EditNamerDialogListener) getActivity()");
//        } catch (ClassCastException e) {
//            throw new ClassCastException("Main Activity must implement EditNamerDialogListener interface");
//        }
//	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "entered 'onCreateView'");
        Log.d(LOG_TAG, "in 'onCreateView' before getArguments, mVisitId = " + mVisitId);
        mVisitId = getArguments().getLong(VISIT_ID);
        Log.d(LOG_TAG, "in 'onCreateView' after getArguments, mVisitId = " + mVisitId);
        View root = inflater.inflate(R.layout.fragment_data_entry_container, container, false);
// assign UI elements
        mDataScreenPager = (ViewPager) root.findViewById(R.id.data_entry_pager);
        Log.d(LOG_TAG, "About to call LoaderManager.initLoader CURRENT_SUBPLOTS");
        getLoaderManager().initLoader(Loaders.CURRENT_SUBPLOTS, null, this);
        Log.d(LOG_TAG, "Called LoaderManager.initLoader CURRENT_SUBPLOTS");
        if (savedInstanceState != null) {
            Log.d(LOG_TAG, "About to do 'getInt(dataPagePosition)', mScreenToShow=" + mScreenToShow);
            mVisitId = savedInstanceState.getLong(VISIT_ID);
            mScreenToShow = savedInstanceState.getInt("dataPagePosition");
            Log.d(LOG_TAG, "Completed 'getInt(dataPagePosition)', mScreenToShow=" + mScreenToShow);
        } else {
            Log.d(LOG_TAG, "savedInstanceState == null; mScreenToShow=" + mScreenToShow);
            // don't yet know why mScreenToShow is retained across re-entry, but explicitly reset it here
//			Log.d(LOG_TAG, "resetting mScreenToShow to 0");
//			mScreenToShow = 0;
        }
        // set Pager Adapter when loader done
        Log.d(LOG_TAG, "about to return from 'onCreateView'");
        return root;
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop, ScreenToShow = " + mScreenToShow);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy, ScreenToShow = " + mScreenToShow);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(LOG_TAG, "onDestroyView, ScreenToShow = " + mScreenToShow);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(LOG_TAG, "onDetach, ScreenToShow = " + mScreenToShow);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause, ScreenToShow = " + mScreenToShow + "; about to set to 'mDataScreenPager.getCurrentItem()'");
        mScreenToShow = mDataScreenPager.getCurrentItem();
        Log.d(LOG_TAG, "onPause, ScreenToShow = " + mScreenToShow);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume, ScreenToShow = " + mScreenToShow);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(LOG_TAG, "onActivityCreated, ScreenToShow = " + mScreenToShow);
    }

    public static class dataPagerAdapter extends FragmentStatePagerAdapter {


        private SparseArray<WeakReference<Fragment>> mFragments = new SparseArray<>();

        public dataPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(LOG_TAG, "called dataPagerAdapter 'getItem' " + position);
            Bundle args = new Bundle();
            args.putInt(VegSubplotFragment.POSITION_KEY, position);
            mSubplotsCursor.moveToPosition(position);
            args.putLong(VegSubplotFragment.VISIT_ID, mSubplotsCursor.getLong(
                    mSubplotsCursor.getColumnIndexOrThrow("VisitId")));
            args.putString(VegSubplotFragment.VISIT_NAME, mSubplotsCursor.getString(
                    mSubplotsCursor.getColumnIndexOrThrow("VisitName")));
            args.putLong(VegSubplotFragment.SUBPLOT_TYPE_ID, mSubplotsCursor.getLong(
                    mSubplotsCursor.getColumnIndexOrThrow("SubplotTypeId")));
            args.putBoolean(VegSubplotFragment.PRESENCE_ONLY, ((mSubplotsCursor.getInt(
                    mSubplotsCursor.getColumnIndexOrThrow("PresenceOnly")) == 0) ? false : true));
            return VegSubplotFragment.newInstance(args);
        }

        @Override
        public int getCount() {
//			Log.d(LOG_TAG, "called dataPagerAdapter 'getCount'");
            return mSubplotsCursor.getCount();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            mSubplotsCursor.moveToPosition(position);
            return mSubplotsCursor.getString(
                    mSubplotsCursor.getColumnIndexOrThrow("SubplotDescription"));
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment f = (Fragment) super.instantiateItem(container, position);
//			Log.d(LOG_TAG, "Tag of fragment instatiated at position " + position + ": " + f.getTag());
//			getSupportFragmentManager().beginTransaction().add(f, "dataScreen" + position).commit();
//			Log.d(LOG_TAG, "After setting tag of fragment at position " + position + ": " + f.getTag());
            mFragments.put(position, new WeakReference<>(f));  // Remember what fragment was in this position
            return f;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            mFragments.remove(position);
        }

        public Fragment getFragment(int position) {
            WeakReference<Fragment> ref = mFragments.get(position);
            Fragment f = ref != null ? ref.get() : null;
            if (f == null) {
                Log.d(LOG_TAG, "fragment for " + position + " is null!");
            }
            return f;
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
        case Loaders.CURRENT_SUBPLOTS:
            baseUri = ContentProvider_VegNab.SQL_URI;
            select = "SELECT Visits._id AS VisitId, Visits.VisitName, SubplotTypes._id AS SubplotTypeId, "
                    + "SubplotTypes.ParentPlotCode, SubplotTypes.SubplotDescription, "
                    + "SubplotTypes.PresenceOnly, SubplotTypes.HasNested, "
                    + "SubplotTypes.NestedInId, SubplotTypes.NestedInName "
                    + "FROM Visits LEFT JOIN SubplotTypes "
                    + "ON Visits.PlotTypeID = SubplotTypes.PlotTypeID "
                    + "WHERE (((Visits._id)=" + mVisitId + ")) "
                    + "ORDER BY SubplotTypes.OrderDone, SubplotTypes._id;";
            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, null, null);
            break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor finishedCursor) {
        // there will be various loaders, switch them out here
//		mRowCt = finishedCursor.getCount();
        switch (loader.getId()) {
        case Loaders.CURRENT_SUBPLOTS:
            // store a reference to the cursor
            mSubplotsCursor = finishedCursor;
            // store the list of subplots
            mPlotSpecs = new JSONArray(); // clear the array
            mSubplotNames.clear();
            Log.d(LOG_TAG, "In 'onLoadFinished', mPlotSpecs=" + mPlotSpecs.toString());
            while (finishedCursor.moveToNext()) {
                mSubplotNames.append(finishedCursor.getInt(finishedCursor.getColumnIndexOrThrow("SubplotTypeId")),
                        finishedCursor.getString(finishedCursor.getColumnIndexOrThrow("SubplotDescription")));
                mSubplotSpec = new JSONObject();
                // for now, only put the Subplot ID number
                try {
                    mSubplotSpec.put("subplotId", finishedCursor.getInt(finishedCursor.getColumnIndexOrThrow("SubplotTypeId")));
                    mSubplotSpec.put("plotTypeCode", finishedCursor.getString(finishedCursor.getColumnIndexOrThrow("ParentPlotCode")));
                    mSubplotSpec.put("sbpDescription", finishedCursor.getString(finishedCursor.getColumnIndexOrThrow("SubplotDescription")));
                    mSubplotSpec.put("presenceOnly", finishedCursor.getInt(finishedCursor.getColumnIndexOrThrow("PresenceOnly")));
                    mSubplotSpec.put("hasNested", finishedCursor.getInt(finishedCursor.getColumnIndexOrThrow("HasNested")));
                    mSubplotSpec.put("nstInId", finishedCursor.getInt(finishedCursor.getColumnIndexOrThrow("NestedInId")));
                    mSubplotSpec.put("nstInName", finishedCursor.getString(finishedCursor.getColumnIndexOrThrow("NestedInName")));
                } catch (JSONException e) {
                    Log.d(LOG_TAG, "In 'onLoadFinished', JSON error: " + e.getMessage());
                }
                // can put in the auxiliary data specs, here or in post-processing
                mPlotSpecs.put(mSubplotSpec);
            }
            // must use ChildFragmentManager
            mDataScreenPager.setAdapter(new dataPagerAdapter(getChildFragmentManager()));
            Log.d(LOG_TAG, "About to do mDataScreenPager.setCurrentItem(mScreenToShow)=" + mScreenToShow);
            mDataScreenPager.setCurrentItem(mScreenToShow);
            Log.d(LOG_TAG, "Just did mDataScreenPager.setCurrentItem(mScreenToShow)=" + mScreenToShow);
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed. Need to make sure it is no longer is use.
        switch (loader.getId()) {
        case Loaders.CURRENT_SUBPLOTS:
            // do we need to do any cleanup here?
            // following line crashes app:
//			mDataScreenPager.setAdapter(null);
            break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // following can crash on screen rotate when EditSppItemDialog is displayed
//		Log.d(LOG_TAG, "In 'onSaveInstanceState', about to save dataPagePosition; mDataScreenPager.getCurrentItem() = " + mDataScreenPager.getCurrentItem());
//		outState.putInt("dataPagePosition", mDataScreenPager.getCurrentItem());
        Log.d(LOG_TAG, "In 'onSaveInstanceState', about to save dataPagePosition; mScreenToShow = " + mScreenToShow);
        outState.putInt("dataPagePosition", mScreenToShow);
        outState.putLong(VISIT_ID, mVisitId);
        super.onSaveInstanceState(outState);
    }
}

