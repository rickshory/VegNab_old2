package com.vegnab.vegnab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;
import com.vegnab.vegnab.BuildConfig;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract;
import com.vegnab.vegnab.database.VNContract.Prefs;
import com.vegnab.vegnab.database.VNContract.Tags;
import com.vegnab.vegnab.database.VegNabDbHelper;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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

import static com.vegnab.vegnab.PhPixGridFragment.*;

public class MainVNActivity extends ActionBarActivity 
        implements NewVisitFragment.OnButtonListener,
        NewVisitFragment.OnVisitClickListener,
        VisitHeaderFragment.OnButtonListener,
        VisitHeaderFragment.EditVisitDialogListener,
        VegSubplotFragment.OnButtonListener,
        EditNamerDialog.EditNamerDialogListener,
        ConfirmDelNamerDialog.EditNamerDialogListener,
        ConfigurableEditDialog.ConfigurableEditDialogListener,
        SelectSpeciesFragment.OnEditPlaceholderListener,
        EditSppItemDialog.EditSppItemDialogListener,
        EditPlaceholderFragment.OnButtonListener,
        ConfirmDelVegItemDialog.ConfirmDeleteVegItemDialogListener,
        UnHideVisitDialog.ConfirmUnHideVisitDialogListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        NewVisitFragment.ExportVisitListener {
        // LoaderManager.LoaderCallbacks<Cursor>

    private static final String LOG_TAG = MainVNActivity.class.getSimpleName();
    static String mUniqueDeviceId, mDeviceIdSource;
    long mRowCt, mVisitId = 0, mSubplotTypeId = 0, mProjectId = 0, mNamerId = 0, mVisitIdToExport = 0;
    boolean mConnectionRequested = false;

    String mExportFileName = "";

    final static String ARG_SUBPLOT_TYPE_ID = "subplotTypeId";
    final static String ARG_VISIT_ID = "visitId";
    final static String ARG_CONNECTION_REQUESTED = "connRequested";

    ViewPager viewPager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        // start following loader, does not use UI, but only gets a value to have ready for a menu action
//        getSupportLoaderManager().initLoader(VNContract.Loaders.HIDDEN_VISITS, null, this);
        //Get a Tracker (should auto-report)
        ((VNApplication) getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        // set up some default Preferences
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor;
        if (!sharedPref.contains(Prefs.TARGET_ACCURACY_OF_VISIT_LOCATIONS)) {
            prefEditor = sharedPref.edit();
            prefEditor.putFloat(Prefs.TARGET_ACCURACY_OF_VISIT_LOCATIONS, (float) 7.0);
            prefEditor.commit();
        }
        if (!sharedPref.contains(Prefs.TARGET_ACCURACY_OF_MAPPED_LOCATIONS)) {
            prefEditor = sharedPref.edit();
            prefEditor.putFloat(Prefs.TARGET_ACCURACY_OF_MAPPED_LOCATIONS, (float) 7.0);
            prefEditor.commit();
        }
        if (!sharedPref.contains(Prefs.UNIQUE_DEVICE_ID)) {
            prefEditor = sharedPref.edit();
            getUniqueDeviceId(this); // generate the ID and the source
            prefEditor.putString(Prefs.DEVICE_ID_SOURCE, mDeviceIdSource);
            prefEditor.putString(Prefs.UNIQUE_DEVICE_ID, mUniqueDeviceId);
            prefEditor.commit();
        }

        // Is there a description of what "local" is (e.g. "Iowa")? Initially, no.
        if (!sharedPref.contains(Prefs.LOCAL_SPECIES_LIST_DESCRIPTION)) {
            prefEditor = sharedPref.edit();
            prefEditor.putString(Prefs.LOCAL_SPECIES_LIST_DESCRIPTION, "");
            prefEditor.commit();
        }

        // Has the regional species list been downloaded? Initially, no.
        if (!sharedPref.contains(Prefs.SPECIES_LIST_DOWNLOADED)) {
            prefEditor = sharedPref.edit();
            // improve this, test if table contains any species
            prefEditor.putBoolean(Prefs.SPECIES_LIST_DOWNLOADED, false);
            prefEditor.commit();
        }

        // Have the user verify each species entered as presence/absence? Initially, yes.
        // user will probably turn this one off each session, but turn it on on each restart
        prefEditor = sharedPref.edit();
        prefEditor.putBoolean(Prefs.VERIFY_VEG_ITEMS_PRESENCE, true);
        prefEditor.commit();

        // Set the default ID method to Digital Photograph
        if (!sharedPref.contains(Prefs.DEFAULT_IDENT_METHOD_ID)) {
            prefEditor = sharedPref.edit();
            prefEditor.putLong(Prefs.DEFAULT_IDENT_METHOD_ID, 1);
            prefEditor.commit();
        }

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
                mConnectionRequested = savedInstanceState.getBoolean(ARG_CONNECTION_REQUESTED);
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

        case R.id.action_legal_notices:
            // following is required, to use Drive API
            String legalInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this);
            AlertDialog.Builder legalInfoDialog = new AlertDialog.Builder(this);
            legalInfoDialog.setTitle(this.getResources().getString(R.string.action_legal_notices));
            legalInfoDialog.setMessage(legalInfo);
            legalInfoDialog.show();
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

        // following, moved to NewVisit fragment
//        case R.id.action_unhide_visits: // from New Visit fragment
//            if (mCtHiddenVisits == 0) {
//                Toast.makeText(this,
//                        this.getResources().getString(R.string.new_visit_unhide_visit_none),
//                        Toast.LENGTH_SHORT).show();
//            } else {
////                Toast.makeText(getApplicationContext(), mCtHiddenVisits + " hidden visit(s), but "
////                        + "''Un-hide Visits'' is not implemented yet", Toast.LENGTH_SHORT).show();
//                UnHideVisitDialog unHideVisDlg = new UnHideVisitDialog();
//                unHideVisDlg.show(fm, "frg_unhide_vis");
//            }
//            return true;

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
        outState.putBoolean(ARG_CONNECTION_REQUESTED, mConnectionRequested);
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
        PhPixGridFragment phPixGridFrag = newInstance(args);
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

    public void onPlaceholderActionButtonClicked(Bundle args) {
        Bundle argsOut= new Bundle();
        Log.d(LOG_TAG, "In onPlaceholderActionButtonClicked");
        switch (args.getInt(EditPlaceholderFragment.BUTTON_KEY)) {
            case VNContract.PhActions.GO_TO_PICTURES: // go to the show/take/edit photos screen
                argsOut.putLong(PhPixGridFragment.ARG_PLACEHOLDER_ID,
                        args.getLong(EditPlaceholderFragment.ARG_PLACEHOLDER_ID));
                PhPixGridFragment phGridFrag = PhPixGridFragment.newInstance(argsOut);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                // put the present fragment on the backstack so the user can navigate back to it
                // the tag is for the fragment now being added, not the one replaced
                transaction.replace(R.id.fragment_container, phGridFrag, Tags.PLACEHOLDER_PIX_GRID);
                transaction.addToBackStack(null);
                transaction.commit();
                break;
        }
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

    public void onUnHideVisitConfirm(DialogFragment dialog) {
        Log.d(LOG_TAG, "onUnHideVisitConfirm(DialogFragment dialog)");
        Bundle args = dialog.getArguments();
        long recIdToUnhide = args.getLong(UnHideVisitDialog.ARG_VISIT_ID_TO_UNHIDE);
        NewVisitFragment nvFrag = (NewVisitFragment)
                getSupportFragmentManager().findFragmentByTag(Tags.NEW_VISIT);
        nvFrag.setVisitVisibility(recIdToUnhide, true); // fn refreshes lists
    }

    @Override
    public void onConfigurableEditComplete(DialogFragment dialog) {
        Log.d(LOG_TAG, "onConfigurableEditComplete(DialogFragment dialog)");
        // get parameter(s) from dialog
        Bundle args = dialog.getArguments();
        // switch out task based on where called from
        String UriTarget = args.getString(ConfigurableEditDialog.ITEM_URI_TARGET);
        // make work with Projects too
        VisitHeaderFragment visHdrFragment = (VisitHeaderFragment)
                getSupportFragmentManager().findFragmentByTag(Tags.VISIT_HEADER);
        if (visHdrFragment != null) {
            try {
                if (UriTarget == "namers") {
                    visHdrFragment.refreshNamerSpinner();
                }
            } catch (Exception e) {
                // screen rotates may destroy some objects & cause null pointer exceptions
                // refresh will occur when fragments rebuilt
                Log.d(LOG_TAG, "exception: " + e.getMessage());
            }
        }
        EditPlaceholderFragment editPhFragment = (EditPlaceholderFragment)
                getSupportFragmentManager().findFragmentByTag(Tags.EDIT_PLACEHOLDER);
        if (editPhFragment != null) {
            try {
                if (UriTarget == "idnamers") {
                    editPhFragment.refreshIdNamerSpinner();
                }
                if (UriTarget == "idrefs") {
                    editPhFragment.refreshIdRefSpinner();
                }
                if (UriTarget == "idmethods") {
                    editPhFragment.refreshIdMethodSpinner();
                }
            } catch (Exception e) {
                Log.d(LOG_TAG, "exception: " + e.getMessage());
            }
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
        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{dst.getAbsolutePath()}, null, null);
    }

/*
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
                cl = new CursorLoader(this, baseUri,
                        null, select, null, null);
                break;

        }
        return cl;
    }
*/

/*
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor finishedCursor) {
        // there will be various loaders, switch them out here
        mRowCt = finishedCursor.getCount();
        switch (loader.getId()) {
            case VNContract.Loaders.TEST_SQL:
                Log.d(LOG_TAG, "Loaders.TEST_SQL returned cursor ");
                finishedCursor.moveToFirst();
                String d = finishedCursor.getString(0);
                Log.d(LOG_TAG, "Loaders.TEST_SQL value returned: " + d);

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

                        Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PROJECT_ID + "' does not exist yet.");
                        // update the create time in the database from when the DB file was created to 'now'
                        String sql = "UPDATE Projects SET StartDate = DATETIME('now') WHERE _id = 1;";
                        ContentResolver resolver = this.getContentResolver();
                        // use raw SQL, to make use of SQLite internal "DATETIME('now')"
                        Uri uri = ContentProvider_VegNab.SQL_URI;
                        int numUpdated = resolver.update(uri, null, sql, null);
                        saveDefaultProjectId(mProjectId);

                        Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PROJECT_ID + "' set for the first time.");
                    } else {

                        Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PROJECT_ID + "' = " + mProjectId);
                    }
                    // set the default Project to show in its spinner
                    // for a generalized fn, try: mProjSpinner.getAdapter().getCount()
                    for (int i=0; i<mRowCt; i++) {
                        Log.d(LOG_TAG, "Setting mProjSpinner default; testing index " + i);
                        if (mProjSpinner.getItemIdAtPosition(i) == mProjectId) {
                            Log.d(LOG_TAG, "Setting mProjSpinner default; found matching index " + i);
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
                        Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' does not exist yet.");
                        saveDefaultPlotTypeId(mPlotTypeId);
                        Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' set for the first time.");
                    } else { // default plot type already exisits
                        Log.d(LOG_TAG, "Prefs key '" + Prefs.DEFAULT_PLOTTYPE_ID + "' = " + mPlotTypeId);
                    }
                    // set the default Plot Type to show in its spinner
                    // for a generalized fn, try: mySpinner.getAdapter().getCount()
                    for (int i=0; i<mRowCt; i++) {
                        Log.d(LOG_TAG, "Setting mPlotTypeSpinner default; testing index " + i);
                        if (mPlotTypeSpinner.getItemIdAtPosition(i) == mPlotTypeId) {
                            Log.d(LOG_TAG, "Setting mPlotTypeSpinner default; found matching index " + i);
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
    */
    @Override
    public void onExportVisitRequest(Bundle paramsBundle) {
        mVisitIdToExport = paramsBundle.getLong(NewVisitFragment.ARG_VISIT_ID);
        Log.d(LOG_TAG, "mVisitIdToExport received in 'onExportVisitRequest' = " + mVisitIdToExport);
        // get filename, either default or overridden in Confirm dialog
        mExportFileName = paramsBundle.getString(NewVisitFragment.ARG_EXPORT_FILENAME);
        Log.d(LOG_TAG, "mExportFileName received in 'onExportVisitRequest': " + mExportFileName);
        mConnectionRequested = true;
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        // create new contents resource
        Drive.DriveApi.newDriveContents(getGoogleApiClient())
                .setResultCallback(driveContentsCallback);
        // file is actually created by a callback, search in this code for:
        // ResultCallback<DriveContentsResult> driveContentsCallback
    }

    // Builds a GoogleApiClient.
    protected synchronized void buildGoogleApiClient() {
        if (servicesAvailable()) {
            if (mGoogleApiClient == null) {
                mGoogleApiClient = new GoogleApiClient.Builder(this)
                        .addApi(Drive.API)
                        .addScope(Drive.SCOPE_FILE)
                        .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
            }
        }
    }

    private boolean servicesAvailable() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0).show();
            return false;
        }
    }

    // Google Drive API boilerplate, could be in separate class

    // Request code for auto Google Play Services error resolution.
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    // Next available request code.
    protected static final int NEXT_AVAILABLE_REQUEST_CODE = 2;
    // Google API client.
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onResume() {
        super.onResume();
        if (mConnectionRequested) {
            buildGoogleApiClient();
            mGoogleApiClient.connect();
        }
    }

    // Handles resolution callbacks.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(LOG_TAG, "in 'onActivityResult' resolution callback before any validity testing");
        // we come here if connection first failed, such as if we needed to get the user login
        // the failure sent off the login request as an intent, which then
        // sent back another intent, which arrives here
        if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
            Log.d(LOG_TAG, "in 'onActivityResult' resolution callback (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK)");
            mGoogleApiClient.connect();
        }
    }

    // disconnect Drive service when activity is invisible.
    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    // Called when {@code mGoogleApiClient} is disconnected
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(LOG_TAG, "GoogleApiClient connection suspended");
    }

    // Called when {@code mGoogleApiClient} is trying to connect but failed.
    // Handle {@code result.getResolution()} if there is a resolution is available.
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(LOG_TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(LOG_TAG, "Exception while starting resolution activity", e);
        }
    }

    // Shows a toast message.
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // Getter for the {@code GoogleApiClient}.
    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    // end of Google Drive API boilerplate

    @Override
    public void onConnected(Bundle connectionHint) {
 //       super.onConnected(connectionHint);
        Log.d(LOG_TAG, "GoogleApiClient connected");
    }

    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {
        @Override
        public void onResult(DriveApi.DriveContentsResult result) {
            if (!result.getStatus().isSuccess()) {
                showMessage("Error while trying to create new file contents");
                return;
            }
            final DriveContents driveContents = result.getDriveContents();
            // mExportFileName generated in export request, and copied to a Final string here to be accessible by other thread
            final String fileName = mExportFileName;
            final long visId = mVisitIdToExport;

            // Perform I/O off the UI thread.
            new Thread() {
//                @SuppressLint("NewApi")
                @Override
                public void run() {
            // write content to DriveContents
            OutputStream outputStream = driveContents.getOutputStream();
            Writer writer = new OutputStreamWriter(outputStream);
            try {
                // \n writes only a '0x0a' character to the file (newline)
                // 'normal' text files contain '0x0d' '0x0a' (carriage return and then newline)
                writer.write("This is the output of a Visit's data.\r\n");
                // temporarily comment out the following
//	    				if (visId == 0) {
//	    					writer.write("\nNo data yet for this Visit.\r\n");
//	    				} else {

                if (true) { // for testing
                    writer.write("\r\nVisit ID = " + visId + "\r\n");
                    // test getting data from the database
                    VegNabDbHelper thdDb = new VegNabDbHelper(getApplicationContext());
                    ContentResolver thdRs = getApplicationContext().getContentResolver();
                    Cursor thdCs, thdSb, thdVg;
                    String sSQL;
                    // get the Visit Header information
                    sSQL = "SELECT Visits.VisitName, Visits.VisitDate, Projects.ProjCode, "
                            + "PlotTypes.PlotTypeDescr, Visits.StartTime, Visits.LastChanged, "
                            + "Namers.NamerName, Visits.Scribe, Locations.LocName, "
                            + "Locations.VisitID, Locations.SubplotID, Locations.ListingOrder, "
                            + "Locations.Latitude, Locations.Longitude, Locations.TimeStamp, "
                            + "Locations.Accuracy, Locations.Altitude, LocationSources.LocationSource, "
                            + "Visits.Azimuth, Visits.VisitNotes, Visits.DeviceType, "
                            + "Visits.DeviceID, Visits.DeviceIDSource, Visits.IsComplete, "
                            + "Visits.ShowOnMobile, Visits.Include, Visits.IsDeleted, "
                            + "Visits.NumAdditionalLocations, Visits.AdditionalLocationsType, "
                            + "Visits.AdditionalLocationSelected "
                            + "FROM ((((Visits LEFT JOIN Projects "
                            + "ON Visits.ProjID = Projects._id) "
                            + "LEFT JOIN PlotTypes ON Visits.PlotTypeID = PlotTypes._id) "
                            + "LEFT JOIN Namers ON Visits.NamerID = Namers._id) "
                            + "LEFT JOIN Locations ON Visits.RefLocID = Locations._id) "
                            + "LEFT JOIN LocationSources ON Locations.SourceID = LocationSources._id "
                            + "WHERE Visits._id = " + visId + ";";
                    thdCs = thdDb.getReadableDatabase().rawQuery(sSQL, null);
                    int numCols = thdCs.getColumnCount();
                    while (thdCs.moveToNext()) {
                        for (int i=0; i<numCols; i++) {
                            writer.write(thdCs.getColumnName(i) + "\t");
                            try {
//	    								writer.write(thdCs.getType(i) + "\r\n");
                                writer.write(thdCs.getString(i) + "\r\n");
                            } catch (Exception e) {
                                writer.write("\r\n");
                            }
                        }
//	    						Log.d(LOG_TAG, "wrote a record");
                    }
                    Log.d(LOG_TAG, "cursor done");
                    thdCs.close();
                    Log.d(LOG_TAG, "cursor closed");
                    // get the Subplots for this Visit
                    long sbId;
                    String sbName, spCode, spDescr, spParams;
                    sSQL = "SELECT Visits._id, PlotTypes.PlotTypeDescr, PlotTypes.Code, "
                            + "SubplotTypes.[_id] AS SubplotTypeId, "
                            + "SubplotTypes.SubplotDescription, SubplotTypes.OrderDone, "
                            + "SubplotTypes.PresenceOnly, SubplotTypes.HasNested, "
                            + "SubplotTypes.SubPlotAngle, SubplotTypes.XOffset, SubplotTypes.YOffset, "
                            + "SubplotTypes.SbWidth, SubplotTypes.SbLength, SubplotTypes.SbShape, "
                            + "SubplotTypes.NestParam1, SubplotTypes.NestParam2, "
                            + "SubplotTypes.NestParam3, SubplotTypes.NestParam4 "
                            + "FROM (Visits LEFT JOIN PlotTypes ON Visits.PlotTypeID = PlotTypes._id) "
                            + "LEFT JOIN SubplotTypes ON PlotTypes._id = SubplotTypes.PlotTypeID "
                            + "WHERE (((Visits._id)=" + visId + ")) "
                            + "ORDER BY SubplotTypes.OrderDone;";
                    thdSb = thdDb.getReadableDatabase().rawQuery(sSQL, null);
                    while (thdSb.moveToNext()) {
                        sbName = thdSb.getString(thdSb.getColumnIndexOrThrow("SubplotDescription"));
                        sbId = thdSb.getLong(thdSb.getColumnIndexOrThrow("SubplotTypeId"));
                        writer.write("\r\n" + sbName + "\r\n");
                        // get the data for each subplot
                        sSQL = "SELECT VegItems._id, VegItems.VisitID, VegItems.SubPlotID, "
                                + "VegItems.OrigCode, VegItems.OrigDescr, VegItems.Height, VegItems.Cover, "
                                + "VegItems.Presence, VegItems.IdLevelID, "
                                + "VegItems.TimeCreated, VegItems.TimeLastChanged FROM VegItems "
                                + "WHERE (((VegItems.VisitID)=" + visId + ") "
                                + "AND ((VegItems.SubPlotID)=" + sbId + ")) "
                                + "ORDER BY VegItems.TimeLastChanged DESC;";
                        thdVg = thdDb.getReadableDatabase().rawQuery(sSQL, null);
                        while (thdVg.moveToNext()) {
                            spCode = thdVg.getString(thdVg.getColumnIndexOrThrow("OrigCode"));
                            spDescr = thdVg.getString(thdVg.getColumnIndexOrThrow("OrigDescr"));
                            if (thdVg.isNull(thdVg.getColumnIndexOrThrow("Presence"))) {
                                // we should have Height and Cover
                                spParams = "\t\t" + thdVg.getString(thdVg.getColumnIndexOrThrow("Height")) + "cm, "
                                        + thdVg.getString(thdVg.getColumnIndexOrThrow("Cover")) + "%";
                            } else {
                                // we should have Presence = 1 (true) or 0 (false)
                                spParams = "\t\t"
                                        + ((thdVg.getInt(thdVg.getColumnIndexOrThrow("Presence")) == 0)
                                        ? "Absent" : "Present");
                            }
                            writer.write("\t" + spCode + ": " + spDescr + "\r\n");
                            writer.write(spParams + "\r\n");
                        }
                        thdVg.close();
                    }
                    thdSb.close();
                    thdDb.close();
                    Log.d(LOG_TAG, "database closed");
                }

                writer.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(fileName + ".txt")
                    .setMimeType("text/plain")
                    .build();

            // create file in root folder
            Drive.DriveApi.getRootFolder(getGoogleApiClient())
                    .createFile(getGoogleApiClient(), changeSet, driveContents)
                    .setResultCallback(fileCallback);
                }
            }.start();
        }
    };

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    mConnectionRequested = false; // for testing
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Error while trying to create the file");
                        return;
                    }
                    showMessage("Created a file with content: " + result.getDriveFile().getDriveId());
                }
            };


/*
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed. Need to make sure it is no longer is use.
        switch (loader.getId()) {
            case VNContract.Loaders.HIDDEN_VISITS:
                break; // nothing to do with this one
        }
    }
*/


}
