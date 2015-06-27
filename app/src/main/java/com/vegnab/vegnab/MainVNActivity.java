package com.vegnab.vegnab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.vegnab.vegnab.BuildConfig;
import com.vegnab.vegnab.database.VNContract.Prefs;
import com.vegnab.vegnab.database.VNContract.Tags;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.os.Build;

public class MainVNActivity extends ActionBarActivity 
        implements NewVisitFragment.OnButtonListener,
        NewVisitFragment.OnVisitClickListener,
        VisitHeaderFragment.OnButtonListener,
        VisitHeaderFragment.EditVisitDialogListener,
        VegSubplotFragment.OnButtonListener,
        EditNamerDialog.EditNamerDialogListener,
        ConfirmDelNamerDialog.EditNamerDialogListener,
        SelectSpeciesFragment.OnEditPlaceholderListener,
        EditSppItemDialog.EditSppItemDialogListener,
        EditPlaceholderFragment.OnButtonListener,
        ConfirmDelVegItemDialog.ConfirmDeleteVegItemDialogListener {

    private static final String LOG_TAG = MainVNActivity.class.getSimpleName();
    static String mUniqueDeviceId, mDeviceIdSource;
    long mRowCt, mVisitId = 0, mSubplotTypeId = 0, mProjectId = 0, mNamerId = 0  ;

    final static String ARG_SUBPLOT_TYPE_ID = "subplotTypeId";
    final static String ARG_VISIT_ID = "visitId";

    ViewPager viewPager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get a Tracker (should auto-report)
        ((VNApplication) getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        // set up some default Preferences
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        if (!sharedPref.contains(Prefs.TARGET_ACCURACY_OF_VISIT_LOCATIONS)) {
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putFloat(Prefs.TARGET_ACCURACY_OF_VISIT_LOCATIONS, (float) 7.0);
            prefEditor.commit();
        }
        if (!sharedPref.contains(Prefs.TARGET_ACCURACY_OF_MAPPED_LOCATIONS)) {
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putFloat(Prefs.TARGET_ACCURACY_OF_MAPPED_LOCATIONS, (float) 7.0);
            prefEditor.commit();
        }
        if (!sharedPref.contains(Prefs.UNIQUE_DEVICE_ID)) {
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            getUniqueDeviceId(this); // generate the ID and the source
            prefEditor.putString(Prefs.DEVICE_ID_SOURCE, mDeviceIdSource);
            prefEditor.putString(Prefs.UNIQUE_DEVICE_ID, mUniqueDeviceId);
            prefEditor.commit();
        }

        // Is there a description of what "local" is (e.g. "Iowa")? Initially, no.
        if (!sharedPref.contains(Prefs.LOCAL_SPECIES_LIST_DESCRIPTION)) {
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putString(Prefs.LOCAL_SPECIES_LIST_DESCRIPTION, "");
            prefEditor.commit();
        }

        // Has the regional species list been downloaded? Initially, no.
        if (!sharedPref.contains(Prefs.SPECIES_LIST_DOWNLOADED)) {
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            // improve this, test if table contains any species
            prefEditor.putBoolean(Prefs.SPECIES_LIST_DOWNLOADED, false);
            prefEditor.commit();
        }

        // Have the user verify each species entered as presence/absence? Initially, yes.
        // user will probably turn this one off each session, but turn it on on each restart
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putBoolean(Prefs.VERIFY_VEG_ITEMS_PRESENCE, true);
        prefEditor.commit();

        setContentView(R.layout.activity_vn_main);
//		viewPager = (ViewPager) findViewById(R.id.data_entry_pager);
//		FragmentManager fm = getSupportFragmentManager();
//		viewPager.setAdapter(new dataPagerAdapter(fm));

        /* put conditions to test below
         * such as whether the container even exists in this layout
         * e.g. if (findViewById(R.id.fragment_container) != null)
         * */
        if (true) {
            if (savedInstanceState != null) {
                mVisitId = savedInstanceState.getLong(ARG_VISIT_ID);
                mSubplotTypeId = savedInstanceState.getLong(ARG_SUBPLOT_TYPE_ID, 0);
                // if restoring from a previous state, do not create view
                // could end up with overlapping views
                return;
            }

            // create an instance of New Visit fragment
            NewVisitFragment newVisitFrag = new NewVisitFragment();

            // in case this activity were started with special instructions from an Intent
            // pass the Intent's Extras to the fragment as arguments
            newVisitFrag.setArguments(getIntent().getExtras());

            // the tag is for the fragment now being added
            // it will stay with this fragment when put on the backstack
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.fragment_container, newVisitFrag, Tags.NEW_VISIT);
            transaction.commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Get an Analytics tracker to report app starts & uncaught exceptions etc.
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        //Stop the analytics tracking
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.vn_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment editProjDlg;
        switch (item.getItemId()) { // some of these are from Fragments, but handled here in the Activity
        case R.id.action_app_info:
            Toast.makeText(getApplicationContext(), "''App Info'' is not implemented yet", Toast.LENGTH_SHORT).show();
            return true;
        case R.id.action_edit_proj:
//			EditProjectDialog editProjDlg = new EditProjectDialog();
            /*
            fm.executePendingTransactions(); // assure all are done
            NewVisitFragment newVis = (NewVisitFragment) fm.findFragmentByTag("NewVisitScreen");
            if (newVis == null) {
                Toast.makeText(getApplicationContext(), "Can't get New Visit Screen fragment", Toast.LENGTH_SHORT).show();
                return true;
            }
            // wait, we don't need to regenerate the default project Id, it's stored in Preferences*/
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            long defaultProjId = sharedPref.getLong(Prefs.DEFAULT_PROJECT_ID, 1);
            editProjDlg = EditProjectDialog.newInstance(defaultProjId);
            editProjDlg.show(fm, "frg_edit_proj");
            return true;
        case R.id.action_new_proj:
            editProjDlg = EditProjectDialog.newInstance(0);
            editProjDlg.show(fm, "frg_new_proj");
            return true;
        case R.id.action_del_proj:
            DelProjectDialog delProjDlg = new DelProjectDialog();
            delProjDlg.show(fm, "frg_del_proj");
            return true;
        case R.id.action_new_plottype:
//			Toast.makeText(getApplicationContext(), "''New Plot Type'' is still under construction", Toast.LENGTH_SHORT).show();
            showWebViewScreen(Tags.WEBVIEW_PLOT_TYPES);
            /*		public static final String WEBVIEW_TUTORIAL = "WebviewTutorial";
            public static final String WEBVIEW_PLOT_TYPES = "WebviewPlotTypes";
            public static final String WEBVIEW_REGIONAL_LISTS = "WebviewSppLists";
    */
            return true;
        case R.id.action_get_species:
            goToGetSppScreen();
            return true;

        case R.id.action_export_db:
            exportDB();
            return true;

        case R.id.action_unhide_visits:
            Toast.makeText(getApplicationContext(), "''Un-hide Visits'' is not implemented yet", Toast.LENGTH_SHORT).show();
            return true;

        case R.id.action_settings:
            Toast.makeText(getApplicationContext(), "''Settings'' is not implemented yet", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

/* comment out onBackPressed()
    @Override
    public void onBackPressed() {
        Log.d(LOG_TAG, "Caught 'onBackPressed'");
        FragmentManager fm = getSupportFragmentManager();
        try {
            // try to pop the data screens container fragment
            if (fm.popBackStackImmediate (Tags.DATA_SCREENS_CONTAINER, FragmentManager.POP_BACK_STACK_INCLUSIVE)) {
                Log.d(LOG_TAG, "DATA_SCREENS_CONTAINER fragment popped from backstack");
//				wasRemoved = true;
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "stack pop exception: " + e.getMessage());
        }

//		Fragment currentFragment = this.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
//		if (currentFragment.getTag() == Tags.DATA_SCREENS_CONTAINER) {
//			finish();
//			return;
//		}
        super.onBackPressed();
    return;
    }
*/

    public void onVisitHeaderGoButtonClicked(long visitId) {
        mVisitId = visitId;
        // swap DataEntryContainerFragment in place of existing fragment
        Log.d(LOG_TAG, "About to go to DataEntryContainer");
        FragmentManager fm = getSupportFragmentManager();
        Bundle args = new Bundle();
        Log.d(LOG_TAG, "In onVisitHeaderGoButtonClicked, about to putLong mVisitId=" +  mVisitId);
        args.putLong(DataEntryContainerFragment.VISIT_ID, mVisitId);
        DataEntryContainerFragment dataEntryFrag = DataEntryContainerFragment.newInstance(args);
        FragmentTransaction transaction = fm.beginTransaction();
        // put the present fragment on the backstack so the user can navigate back to it
        // the tag is for the fragment now being added, not the one replaced

        transaction.replace(R.id.fragment_container, dataEntryFrag, Tags.DATA_SCREENS_CONTAINER);
        transaction.addToBackStack(null);
        transaction.commit();
        Log.d(LOG_TAG, "Call to DataEntryContainer complete");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save the current subplot arguments in case we need to re-create the activity
        outState.putLong(ARG_SUBPLOT_TYPE_ID, mSubplotTypeId);
        outState.putLong(ARG_VISIT_ID, mVisitId);
    }

    @Override
    public void onNewVisitGoButtonClicked() {
        // check if SPECIES_LIST_DOWNLOADED
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        Boolean hasSpp = sharedPref.getBoolean(Prefs.SPECIES_LIST_DOWNLOADED, false);
        // get an Analytics event tracker
        Tracker newVisitTracker = ((VNApplication) getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        if (hasSpp) {
            // build and send the Analytics event
            // track that user started a new visit
            Long plotTypeID = sharedPref.getLong(Prefs.DEFAULT_PLOTTYPE_ID, 0);
            String plotTypeName = sharedPref.getString(Prefs.DEFAULT_PLOTTYPE_NAME, "no plot type yet");
            newVisitTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Visit Event")
                    .setAction("New Visit")
                    .setLabel(plotTypeName)
                    .setValue(plotTypeID)
                    .build());
            goToVisitHeaderScreen(0);
        } else {
            newVisitTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Visit Event")
                    .setAction("Download Species")
                    .build());
            String errTitle = this.getResources().getString(R.string.dnld_spp_no_spp_title);
            String errMsg = this.getResources().getString(R.string.dnld_spp_no_spp_msg);
            ConfigurableMsgDialog flexMsgDlg =
                    ConfigurableMsgDialog.newInstance(errTitle,  errMsg);
            flexMsgDlg.show(getSupportFragmentManager(), "frg_dnld_spp");
            goToGetSppScreen();
        }
    }

    public void goToGetSppScreen() {
        // get tracker
        Tracker t = ((VNApplication) getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        // set screen name
        t.setScreenName("VegSubplotScreen");
        // send a screen view
        t.send(new HitBuilders.ScreenViewBuilder().build());
        // continue with work
        DownloadSppFragment frgDnldSpp = new DownloadSppFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // replace the fragment in the fragment container with this new fragment and
        // put the present fragment on the backstack so the user can navigate back to it
        // the tag is for the fragment now being added, not the one replaced
        transaction.replace(R.id.fragment_container, frgDnldSpp, "frg_download_spp");
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void goToVisitHeaderScreen(long visitID) {
        // swap VisitHeaderFragment in place of existing fragment
        Log.d(LOG_TAG, "About to go to VisitHeader");
        Bundle args = new Bundle();
        // visitID = 0 means new visit, not assigned or created yet
        args.putLong(VisitHeaderFragment.ARG_VISIT_ID, visitID);
        args.putInt(VisitHeaderFragment.ARG_SUBPLOT, 0); // start with dummy value, subplot 0
        VisitHeaderFragment visHdrFrag = VisitHeaderFragment.newInstance(args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // put the present fragment on the backstack so the user can navigate back to it
        // the tag is for the fragment now being added, not the one replaced
        transaction.replace(R.id.fragment_container, visHdrFrag, Tags.VISIT_HEADER);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void goToEditPlaceholderScreen(Bundle enteredArgs) {
        // swap EditPlaceholderFragment in place of existing fragment
        Log.d(LOG_TAG, "About to go to EditPlaceholder");
        Bundle args = new Bundle();
        // fn structure, but not functional yet
/*available args	final static String ARG_PLACEHOLDER_ID = "placeholderId";
    final static String ARG_PLACEHOLDER_CODE = "placeholderCode";
    final static String ARG_PLACEHOLDER_DESCRIPTION = "placeholderDescription";
    final static String ARG_PLACEHOLDER_HABITAT = "placeholderHabitat";
    final static String ARG_PLACEHOLDER_LABELNUMBER = "placeholderLabelnumber";
    final static String ARG_PH_PROJID = "phProjId";
    final static String ARG_PH_VISITID = "phVisitId";
    final static String ARG_PH_VISIT_NAME = "phVisitName";
    final static String ARG_PH_LOCID = "phLocId";
    final static String ARG_PH_LOC_TEXT = "phLocText";
    final static String ARG_PH_NAMERID = "phNamerId";
    final static String ARG_PH_NAMER_NAME = "phNamerName";
    final static String ARG_PH_SCRIBE = "phScribe";
    final static String ARG_PLACEHOLDER_TIME = "phTimeStamp";
*/		args.putLong(EditPlaceholderFragment.ARG_PLACEHOLDER_ID, 0); // fix this
        args.putString(EditPlaceholderFragment.ARG_PLACEHOLDER_CODE, ""); // fix this
        EditPlaceholderFragment editPhFrag = EditPlaceholderFragment.newInstance(args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // put the present fragment on the backstack so the user can navigate back to it
        // the tag is for the fragment now being added, not the one replaced
        transaction.replace(R.id.fragment_container, editPhFrag, Tags.EDIT_PLACEHOLDER);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void goToPhPixGridScreen(Bundle enteredArgs) {
        // swap PhPixGridFragment in place of existing fragment
        Log.d(LOG_TAG, "About to go to Placeholder Pictures");
        Bundle args = new Bundle();
        // fn structure, but not functional yet
/*available args	final static String ARG_PLACEHOLDER_ID = "placeholderId";
    final static String ARG_PLACEHOLDER_CODE = "placeholderCode";
    final static String ARG_PLACEHOLDER_DESCRIPTION = "placeholderDescription";
    final static String ARG_PLACEHOLDER_HABITAT = "placeholderHabitat";
    final static String ARG_PLACEHOLDER_LABELNUMBER = "placeholderLabelnumber";
    final static String ARG_PH_PROJID = "phProjId";
    final static String ARG_PH_VISITID = "phVisitId";
    final static String ARG_PH_VISIT_NAME = "phVisitName";
    final static String ARG_PH_LOCID = "phLocId";
    final static String ARG_PH_LOC_TEXT = "phLocText";
    final static String ARG_PH_NAMERID = "phNamerId";
    final static String ARG_PH_NAMER_NAME = "phNamerName";
    final static String ARG_PH_SCRIBE = "phScribe";
    final static String ARG_PLACEHOLDER_TIME = "phTimeStamp";
*/		args.putLong(EditPlaceholderFragment.ARG_PLACEHOLDER_ID, 0); // fix this
        args.putString(EditPlaceholderFragment.ARG_PLACEHOLDER_CODE, ""); // fix this
        PhPixGridFragment phPixGridFrag = PhPixGridFragment.newInstance(args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // put the present fragment on the backstack so the user can navigate back to it
        // the tag is for the fragment now being added, not the one replaced
        transaction.replace(R.id.fragment_container, phPixGridFrag, Tags.PLACEHOLDER_PIX_GRID);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void showWebViewScreen(String screenTag) {
        ConfigurableWebviewFragment webVwFrag = new ConfigurableWebviewFragment();
        Bundle args = new Bundle();
        // screenTag serves both as this fn's switch and the tag name of the fragment instance
        args.putString(ConfigurableWebviewFragment.ARG_TAG_ID, screenTag);
        webVwFrag.setArguments(args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // replace the fragment in the fragment container with this new fragment and
        // put the present fragment on the backstack so the user can navigate back to it
        // the tag is for the fragment now being added, not the one replaced
        transaction.replace(R.id.fragment_container, webVwFrag, screenTag);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onPlaceholderPixButtonClicked(Bundle args) {
        Log.d(LOG_TAG, "In onPlaceholderPixButtonClicked");
    }

    public void onPlaceholderSaveButtonClicked(Bundle args) {
        Log.d(LOG_TAG, "In onPlaceholderSaveButtonClicked");
    }

    public void onPlaceholderCancelButtonClicked(Bundle args) {
        Log.d(LOG_TAG, "In onPlaceholderCancelButtonClicked");
    }

    @Override
    public void onNewItemButtonClicked(int screenToReturnTo, long visitId,
            long subplotId, boolean presenceOnly) {
        // presenceOnly is not used by Species Select, but passed along to Edit Species
        SelectSpeciesFragment selSppFrag = new SelectSpeciesFragment();
        Bundle args = new Bundle();

        // screenTag can serve both as this fn's switch and the tag name of the fragment instance
        // args.putString(SelectSpeciesFragment.ARG_TAG_ID, screenTag);

        // provide Visit and Subplot IDs, so selector can check for duplicate codes
        args.putLong(SelectSpeciesFragment.ARG_VISIT_ID, visitId);
        args.putLong(SelectSpeciesFragment.ARG_SUBPLOT_TYPE_ID, subplotId);
        // presenceOnly not used by SppSelect, but passed along to Edit Spp
        args.putBoolean(SelectSpeciesFragment.ARG_PRESENCE_ONLY_SUBPLOT, presenceOnly);
        args.putLong(SelectSpeciesFragment.ARG_PROJECT_ID, mProjectId);
        args.putLong(SelectSpeciesFragment.ARG_NAMER_ID, mNamerId);

        selSppFrag.setArguments(args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // replace the fragment in the fragment container with this new fragment and
        // put the present fragment on the backstack so the user can navigate back to it
        // the tag is for the fragment now being added, not the one replaced
        transaction.replace(R.id.fragment_container, selSppFrag, Tags.SELECT_SPECIES);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void getUniqueDeviceId(Context context) {
        // this is used to get a unique identifier for the device this app is being run on
        // it is primarily used to warn the user if the Visit has been downloaded onto
        // a different device and the Visit is about to be edited;
        // This simple fn is not entirely robust for various reasons, but it is adequate since
        // it is rare for Visits to be edited, and even more rare to be downloaded before editing
        // this ID may be useful in sorting out field work chaos, to tell where the data came from
        String deviceId;
        try {
            TelephonyManager tMgr = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
            deviceId = tMgr.getDeviceId();
            if (deviceId != null) { // won't have this if device is not a phone, and
                                    //not always reliable to read even if it is a phone
                mDeviceIdSource = "Phone";
                mUniqueDeviceId = deviceId;
                return;
            } else { // try to get the Android ID
                deviceId = android.os.Build.SERIAL; // generated on first boot, so may change on system reset
                // only guaranteed available from API 9 and up
                // since Gingerbread (Android 2.3) android.os.Build.SERIAL must exist on any device that doesn't provide IMEI
                if (deviceId != null) {
                    // some Froyo 2.2 builds give the same serial number "9774d56d682e549c" for
                    // all, but these are rare and dying out (fixed ~December 2010.
                    // 4.2+, different profiles on the same device may give different IDs
                    mDeviceIdSource = "Android serial number";
                    mUniqueDeviceId = deviceId;
                    return;
                } else {
                    // generate a random number
                    mDeviceIdSource = "random UUID";
                    mUniqueDeviceId = UUID.randomUUID().toString();
                    return;
                }
            }
        } catch (Exception e) {
            // generate a random number
            mDeviceIdSource = "random UUID";
            mUniqueDeviceId = UUID.randomUUID().toString();
            return;
        }
    }

//    @Override
    public void onDeleteVegItemConfirm(DialogFragment dialog) {
        Log.d(LOG_TAG, "onDeleteVegItemConfirm(DialogFragment dialog)");
        Bundle args = dialog.getArguments();
        long recIdToDelete = args.getLong(ConfirmDelVegItemDialog.ARG_VI_REC_ID);
        DataEntryContainerFragment dataEntryFrag = (DataEntryContainerFragment)
                getSupportFragmentManager().findFragmentByTag(Tags.DATA_SCREENS_CONTAINER);

        int index = dataEntryFrag.mDataScreenPager.getCurrentItem();
        DataEntryContainerFragment.dataPagerAdapter ad =
                ((DataEntryContainerFragment.dataPagerAdapter)dataEntryFrag.mDataScreenPager.getAdapter());
        Fragment f = ad.getFragment(index);
        // fix this up when AuxData implemented, to make sure the class of fragment
        try {
            VegSubplotFragment vf = (VegSubplotFragment) f;
            vf.deleteVegItem(recIdToDelete);
        } catch (ClassCastException e) {
            // if not the right class of fragment, fail silently
        }
    }

    @Override
    public void onEditNamerComplete(DialogFragment dialog) {
        Log.d(LOG_TAG, "onEditNamerComplete(DialogFragment dialog)");
        VisitHeaderFragment visHdrFragment = (VisitHeaderFragment)
                getSupportFragmentManager().findFragmentByTag(Tags.VISIT_HEADER);
        visHdrFragment.refreshNamerSpinner();
    }

    @Override
    public void onEditVisitComplete(VisitHeaderFragment visitHeaderFragment) {
        Log.d(LOG_TAG, "onEditVisitComplete(VisitHeaderFragment visitHeaderFragment)");
        NewVisitFragment newVisFragment = (NewVisitFragment)
                getSupportFragmentManager().findFragmentByTag(Tags.NEW_VISIT);
        newVisFragment.refreshVisitsList();
    }

    @Override
    public void onExistingVisitListClicked(long visitId) {
        mVisitId = visitId;
        goToVisitHeaderScreen(visitId);
    }

    @Override
    public void onEditVegItemComplete(DialogFragment dialog) {
        Log.d(LOG_TAG, "onEditSppComplete(DialogFragment dialog)");
        DataEntryContainerFragment dataScreensFrag = (DataEntryContainerFragment)
                getSupportFragmentManager().findFragmentByTag(Tags.DATA_SCREENS_CONTAINER);
        if (dataScreensFrag == null) {
            Log.d(LOG_TAG, "dataScreensFrag == null");
        } else {
            Log.d(LOG_TAG, "dataScreensFrag: " + dataScreensFrag.toString());
            try {
                // Sometimes screen-rotates while EditSppItemDialog is displayed destroy some of
                // the following objects, in which case the app would crash with null pointer exceptions
                // In this case, skip the data screen refresh and allow it to happen when
                // fragments are re-instantiated
                int index = dataScreensFrag.mDataScreenPager.getCurrentItem();
    //			int index = dataScreensFrag.mScreenToShow;
                DataEntryContainerFragment.dataPagerAdapter adapter =
                        ((DataEntryContainerFragment.dataPagerAdapter)dataScreensFrag.mDataScreenPager.getAdapter());
                VegSubplotFragment vegSubpFragment = (VegSubplotFragment) adapter.getFragment(index);
                if (vegSubpFragment == null) {
                    Log.d(LOG_TAG, "vegSubpFragment == null");
                } else {
                    Log.d(LOG_TAG, "vegSubpFragment: " + vegSubpFragment.toString());
                    Log.d(LOG_TAG, "About to do 'refreshSppList' for data page " + index);
                    vegSubpFragment.refreshVegList();
                    Log.d(LOG_TAG, "Completed 'refreshSppList' for data page " + index);
    //				dataScreensFrag.mDataScreenPager.setCurrentItem(index);
                }
            } catch (Exception e) {
                Log.d(LOG_TAG, "exception: " + e.getMessage());
            }
        }

//		VegSubplotFragment vegSubpFragment = (VegSubplotFragment) 
//				getSupportFragmentManager().findFragmentByTag(Tags.VEG_SUBPLOT);
//		if (vegSubpFragment == null) {
//			Log.d(LOG_TAG, "vegSubpFragment == null");
//		} else {
//			Log.d(LOG_TAG, "vegSubpFragment: " + vegSubpFragment.toString());
//			vegSubpFragment.refreshSppList();
//		}
        Fragment currentFragment = this.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment.getTag() == Tags.SELECT_SPECIES) {
            super.onBackPressed();
        }
    }

    @Override
    public void onEditPlaceholder(Bundle argsIn) {
        // swap EditPlaceholderFragment in place of existing fragment
        Log.d(LOG_TAG, "About to go to Placeholder");
        Bundle argsOut = new Bundle();
        argsOut.putAll(argsIn); // if the bundle info can be passed right through
        EditPlaceholderFragment phFrag = EditPlaceholderFragment.newInstance(argsOut);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // put the present fragment on the backstack so the user can navigate back to it
        // the tag is for the fragment now being added, not the one replaced
        transaction.replace(R.id.fragment_container, phFrag, Tags.EDIT_PLACEHOLDER);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private static final String DATABASE_NAME = "VegNab.db";
    String saveFolderName = BuildConfig.PUBLIC_DB_FOLDER;

    public File getBackupDatabaseFile() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-", Locale.US);
        String uniqueTime = timeFormat.format(new Date()).toString();
        Log.d(LOG_TAG, "uniqueTime: " + uniqueTime);
        String dbBkupName = uniqueTime + DATABASE_NAME;
        Log.d(LOG_TAG, "dbBkupName: " + dbBkupName);

        File sdCard = Environment.getExternalStorageDirectory();
        // create the folder
        File vnFolder = new File(sdCard.getAbsolutePath() + "/" + saveFolderName);
        vnFolder.mkdirs();
        Log.d(LOG_TAG, "folder created '" + saveFolderName + "'");
        File backupDB = new File(vnFolder, dbBkupName);

        return backupDB;
    }
    public final boolean exportDB() {
        File from = getApplicationContext().getDatabasePath(DATABASE_NAME);
        File to = this.getBackupDatabaseFile();
        ConfigurableMsgDialog flexErrDlg = new ConfigurableMsgDialog();
        try {
            copyFile(from, to);
            Log.d(LOG_TAG, "DB backed up to: " + to.getPath());
            flexErrDlg = ConfigurableMsgDialog.newInstance("DB backed up to: ", to.getPath().toString());
            flexErrDlg.show(getSupportFragmentManager(), "frg_db_copy_ok");
            return true;
        } catch (IOException e) {
            Log.d(LOG_TAG, "Error backuping up database: " + e.getMessage(), e);
            flexErrDlg = ConfigurableMsgDialog.newInstance("Error backing up database: ", e.getMessage().toString());
            flexErrDlg.show(getSupportFragmentManager(), "frg_db_copy_ok");
        }
        return false;
    }

    public void copyFile(File src, File dst) throws IOException {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        FileChannel fromChannel = null, toChannel = null;
        try {
            fromChannel = in.getChannel();
            toChannel = out.getChannel();
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        } finally {
            if (fromChannel != null)
                fromChannel.close();
            if (toChannel != null)
                toChannel.close();
        }
        in.close();
        out.close();
        // must do following or file is not visible externally
        MediaScannerConnection.scanFile(getApplicationContext(), new String[] { dst.getAbsolutePath() }, null, null);
    }

}
