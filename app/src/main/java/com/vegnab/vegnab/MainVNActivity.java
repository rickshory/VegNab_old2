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
import java.util.concurrent.ConcurrentHashMap;

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
import com.vegnab.vegnab.util.inappbilling.IabHelper;
import com.vegnab.vegnab.util.inappbilling.IabResult;
import com.vegnab.vegnab.util.inappbilling.Inventory;
import com.vegnab.vegnab.util.inappbilling.Purchase;

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
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;
import android.os.Build;

import static com.vegnab.vegnab.PhPixGridFragment.*;

public class MainVNActivity extends ActionBarActivity 
        implements DonateFragment.OnButtonListener,
        DonateFragment.OnIAPDoneListener,
        NewVisitFragment.OnButtonListener,
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
//        NewVisitFragment.ExportVisitListener,
        ExportVisitDialog.ExportVisitListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        SelectSpeciesFragment.OnPlaceholderRequestListener
        {
        //

    private static final String LOG_TAG = MainVNActivity.class.getSimpleName();
    static String mUniqueDeviceId, mDeviceIdSource;
    long mRowCt, mVisitId = 0, mSubplotTypeId = 0, mProjectId = 0, mNamerId = 0, mVisitIdToExport = 0;
    boolean mConnectionRequested = false;
    long mPhProjID = 0, mPhNameId =0;
    ConcurrentHashMap<String, Long> mExistingPhCodes = new ConcurrentHashMap<String, Long>();

    String mExportFileName = "";
    boolean mResolvePlaceholders = true;

    final static String ARG_SUBPLOT_TYPE_ID = "subplotTypeId";
    final static String ARG_VISIT_ID = "visitId";
    final static String ARG_CONNECTION_REQUESTED = "connRequested";

    final static String ARG_VISIT_TO_EXPORT_ID = "visToExportId";
    final static String ARG_VISIT_TO_EXPORT_NAME = "visToExportName";
    final static String ARG_VISIT_TO_EXPORT_FILENAME = "visToExportFileName";
    final static String ARG_RESOLVE_PLACEHOLDERS = "resolvePlaceholders";

    final static String ARG_PH_PROJ_ID = "phProjId";
    final static String ARG_PH_NAMER_ID = "phNamerId";
    final static String ARG_PH_EXISTING_SET = "phExistingSet";

    // fake product IDs for testing, provided by Google
    private final String productID_testPurchased = "android.test.purchased";
    private final String productID_testCanceled = "android.test.canceled";
    private final String productID_testRefunded = "android.test.refunded";
    private final String productID_testUnavailable = "android.test.item_unavailable";

    // SKUs for products
    // testing in-app billing with donations
    static final String SKU_DONATE_USD_001_00 = "donateUSD001_00";
    static final String SKU_DONATE_USD_003_00 = "donateUSD003_00";
    static final String SKU_DONATE_USD_010_00 = "donateUSD010_00";
    static final String SKU_DONATE_USD_030_00 = "donateUSD030_00";

    // (arbitrary) request code for donation purchase flow
    static final int RC_REQUEST = 10003;

    // the In-App billing helper object
    IabHelper mHelper;

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
        // set up in-app billing
        // following resource is in it's own file, listed in .gitignore
        String base64EncodedPublicKey = getString(R.string.app_license);
        Log.d(LOG_TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        // enable debug logging (for production application, set this to false).
        mHelper.enableDebugLogging(true);
        // Start setup. This is asynchronous.
        // The specified listener will be called once setup completes.
        Log.d(LOG_TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(LOG_TAG, "Setup finished.");
                if (!result.isSuccess()) {
                    // a problem.
                    //Toast.makeText(this,
//                        this.getResources().getString(R.string.in_app_bill_error) + result,
//                        Toast.LENGTH_SHORT).show();
                    return;
                }
                // disposed?
                if (mHelper == null)
                    return;

                // always a good idea to query inventory
                // even if products were supposed to be consumed there might have been some glitch
                Log.d(LOG_TAG, "Setup done. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);

            }
        });

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
/*
            @Override
            protected void onActivityResult(int requestCode, int resultCode, Intent data) {
                Log.d(LOG_TAG, "onActivityResult(" + requestCode + "," + resultCode + ", " + data);
                if (mHelper == null)
                    return;
// Pass on the activity result to the helper for handling
                if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
// not handled, so handle it ourselves (here's where you'd
// perform any handling of activity results not related to in-app
// billing...
                    super.onActivityResult(requestCode, resultCode, data);
                } else {
                    Log.d(LOG_TAG, "onActivityResult handled by IABUtil.");
                }
            }
*/
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

    // activity being destroyed, dispose of the IAB helper
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        Log.d(LOG_TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
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

        case R.id.action_donate:
            goToDonateScreen();
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
        outState.putSerializable(ARG_PH_EXISTING_SET, mExistingPhCodes);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(LOG_TAG, "In 'onRestoreInstanceState'");
        mSubplotTypeId = savedInstanceState.getLong(ARG_SUBPLOT_TYPE_ID);
        mVisitId = savedInstanceState.getLong(ARG_VISIT_ID);
        mConnectionRequested = savedInstanceState.getBoolean(ARG_CONNECTION_REQUESTED);
        mExistingPhCodes = (ConcurrentHashMap<String, Long>) savedInstanceState.getSerializable(ARG_PH_EXISTING_SET);

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

    public void goToDonateScreen() {
        // get tracker
        Tracker t = ((VNApplication) getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        // set screen name
        t.setScreenName("DonateScreen");
        // send a screen view
        t.send(new HitBuilders.ScreenViewBuilder().build());
        // continue with work
        DonateFragment frgDonate = new DonateFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // replace the fragment in the fragment container with this new fragment and
        // put the present fragment on the backstack so the user can navigate back to it
        // the tag is for the fragment now being added, not the one replaced
        transaction.replace(R.id.fragment_container, frgDonate, "frg_donate");
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

    public void onRequestGenerateExistingPlaceholders(Bundle args) {
        Log.d(LOG_TAG, "In onRequestGenerateExistingPlaceholders");
        mPhProjID = args.getLong(ARG_PH_PROJ_ID, 0);
        mPhNameId = args.getLong(ARG_PH_NAMER_ID, 0);
        getSupportLoaderManager().restartLoader(VNContract.Loaders.EXISTING_PH_CODES, null, this);
    }

    public long onRequestGetCountOfExistingPlaceholders () {
        return (long) mExistingPhCodes.size();
    }

    public boolean onRequestMatchCheckOfExistingPlaceholders (String ph) {
        return (mExistingPhCodes.containsKey(ph));
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

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(LOG_TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(LOG_TAG, "Query inventory was successful.");

            // has this user used the Google test code "android.test.purchased"
            Purchase testPurchase = inventory.getPurchase(productID_testPurchased);
            if ((testPurchase != null) && (verifyDeveloperPayload(testPurchase))) {
                Log.d(LOG_TAG, "user has purchased the Google test purchase, about to consume it");
                mHelper.consumeAsync(inventory.getPurchase(productID_testPurchased), mConsumeFinishedListener);
                Log.d(LOG_TAG, "consumeAsync of Google test purchase sent");
                return;
            }

//    /
//      Check for items we own. Notice that for each purchase, we check
//      the developer payload to see if it's correct! See
//      verifyDeveloperPayload().
//     /

//            // Do we have the premium upgrade?
//            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
//            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
//            Log.d(LOG_TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));
//
//            // Do we have the infinite gas plan?
//            Purchase infiniteGasPurchase = inventory.getPurchase(SKU_INFINITE_GAS);
//            mSubscribedToInfiniteGas = (infiniteGasPurchase != null &&
//                    verifyDeveloperPayload(infiniteGasPurchase));
//            Log.d(LOG_TAG, "User " + (mSubscribedToInfiniteGas ? "HAS" : "DOES NOT HAVE")
//                    + " infinite gas subscription.");
//            if (mSubscribedToInfiniteGas) mTank = TANK_MAX;
//
//            // Check for gas delivery -- if we own gas, we should fill up the tank immediately
//            Purchase gasPurchase = inventory.getPurchase(SKU_GAS);
//            if (gasPurchase != null && verifyDeveloperPayload(gasPurchase)) {
//                Log.d(LOG_TAG, "We have gas. Consuming it.");
//                mHelper.consumeAsync(inventory.getPurchase(SKU_GAS), mConsumeFinishedListener);
//                return;
//            }

//            updateUi();
//            setWaitScreen(false);
//            Log.d(LOG_TAG, "Initial inventory query finished; enabling main UI.");
            // probably set a flag here that inventory is checked
        }
    };

        @Override
        public void onDonateButtonClicked(Bundle args) {
            // can declare a tracker here
/*                if (readyToDoThis()) {
                putTogetherTheBundle();
                goToVisitHeaderScreen (Bundle args);
            } else {
                doTheOtherThingFirst();
            }
*/
            // set the value to be donated
            // call onDonate
            // for first test, pass same empty bundle; ignored
            onDonate(args);
        }


    public void onDonate(Bundle args) {
        Log.d(LOG_TAG, "in onDonate; launching purchase flow");
//        setWaitScreen(true);
//        mHelper.consumeAsync(inventory.getPurchase(productID_testPurchased),
//                mConsumeFinishedListener);
        // TODO: for security, generate a payload here for verification.
        // For testing use an empty string, but in production would generate this.
        // See comments in onverifyDeveloperPayload() for more info.
        String payload = "";
//        // for testing, make the donation one dollar; make it a variable later
//        mHelper.launchPurchaseFlow(this, SKU_DONATE_USD_001_00, RC_REQUEST,
//                mPurchaseFinishedListener, payload);
        // for first test, use reserved testing code
//        mHelper.launchPurchaseFlow(this, productID_testPurchased, RC_REQUEST,
//                mPurchaseFinishedListener, payload);

        mHelper.launchPurchaseFlow(this, productID_testCanceled, RC_REQUEST,
                mPurchaseFinishedListener, payload);
    }
            
    // Verifies the developer payload of a purchase.
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        // doesn't do anything yet, always returns true
         // TODO: verify that the developer payload of the purchase is correct. It will be
         // the same one that you sent when initiating the purchase.
         //
         // WARNING: Locally generating a random string when starting a purchase and
         // verifying it here might seem like a good approach, but this will fail in the
         // case where the user purchases an item on one device and then uses your app on
         // a different device, because on the other device you will not have access to the
         // random string you originally generated.
         //
         // So a good developer payload has these characteristics:
         //
         // 1. If two different users purchase an item, the payload is different between them,
         //    so that one user's purchase can't be replayed to another user.
         //
         // 2. The payload must be such that you can verify it even when the app wasn't the
         //    one who initiated the purchase flow (so that items purchased by the user on
         //    one device work on other devices owned by the user).
         //
         // Using your own server to store and verify developer payloads across app
         // installations is recommended.
         ///

    return true;
}

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(LOG_TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                return;
            }

            Log.d(LOG_TAG, "Purchase successful.");

//            if (purchase.getSku().equals(SKU_DONATE_USD_001_00)) {
            if (purchase.getSku().equals(productID_testPurchased)) {
                // bought a donation, so consume it.
//                Log.d(LOG_TAG, "Purchase is $1 donation. Starting consumption.");
                Log.d(LOG_TAG, "Purchase is test.purchased. Starting consumption.");
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);
            }
/*
            else if (purchase.getSku().equals(SKU_PREMIUM)) {
                // bought the premium upgrade!
                Log.d(LOG_TAG, "Purchase is premium upgrade. Congratulating user.");
                alert("Thank you for upgrading to premium!");
                mIsPremium = true;
                updateUi();
                setWaitScreen(false);
            }
            else if (purchase.getSku().equals(SKU_INFINITE_GAS)) {
                // bought the infinite gas subscription
                Log.d(LOG_TAG, "Infinite gas subscription purchased.");
                alert("Thank you for subscribing to infinite gas!");
                mSubscribedToInfiniteGas = true;
                mTank = TANK_MAX;
                updateUi();
                setWaitScreen(false);
            }
*/
        }
    };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(LOG_TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
//            if (purchase.getSku() == productID_testPurchased) {
//
//            }
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                Log.d(LOG_TAG, "Consumption successful. Accounting.");
                // maybe record this in the database?
//                Log.d(LOG_TAG, "Consumption successful. Provisioning.");
//                mTank = mTank == TANK_MAX ? TANK_MAX : mTank + 1;
//                saveData();
//                alert("You filled 1/4 tank. Your tank is now " + String.valueOf(mTank) + "/4 full!");
                alert("Thank you for your donation!");
            }
            else {
                complain("Error while consuming: " + result);
            }
//            updateUi();
//            setWaitScreen(false);
            Log.d(LOG_TAG, "End consumption flow.");
        }
    };

    @Override
    public void onINAppPurchaseComplete(DonateFragment donateFragment) {
//                putTogetherTheBundle();
//                goToWhateverScreen (Bundle args);
        // tie up any open dialogs or fragments
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        switch (id) {
            case VNContract.Loaders.EXISTING_PH_CODES:
               baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT PlaceHolders._id, PlaceHolders.PlaceHolderCode "
                    + "FROM PlaceHolders "
                    + "WHERE PlaceHolders.ProjID = ? "
                    + "AND PlaceHolders.NamerID = ?;";
                cl = new CursorLoader(this, baseUri, null, select,
                        new String[] { "" + mPhProjID, "" + mPhNameId }, null);
                break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor finishedCursor) {
        mRowCt = finishedCursor.getCount();
        // there will be various loaders, switch them out here
        switch (loader.getId()) {
            case VNContract.Loaders.EXISTING_PH_CODES:
                mExistingPhCodes.clear();
                while (finishedCursor.moveToNext()) {
                    mExistingPhCodes.put(finishedCursor.getString(
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
            case VNContract.Loaders.EXISTING_PH_CODES:
                break; // nothing to do with this one
        }
    }

    @Override
    public void onExportVisitRequest(Bundle paramsBundle) {
        mVisitIdToExport = paramsBundle.getLong(ARG_VISIT_TO_EXPORT_ID);
        Log.d(LOG_TAG, "mVisitIdToExport received in 'onExportVisitRequest' = " + mVisitIdToExport);
        // ARG_VISIT_TO_EXPORT_NAME); // currently unused
        // get filename, either default or overridden in Confirm dialog
        mExportFileName = paramsBundle.getString(ARG_VISIT_TO_EXPORT_FILENAME);
        Log.d(LOG_TAG, "mExportFileName received in 'onExportVisitRequest': " + mExportFileName);
        mResolvePlaceholders = paramsBundle.getBoolean(ARG_RESOLVE_PLACEHOLDERS, true);
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
        Log.d(LOG_TAG, "onActivityResult(" + requestCode + "," + resultCode + ", " + data);
        // first, test for in-app billing result
        if ((mHelper != null) && (mHelper.handleActivityResult(requestCode, resultCode, data))) {
            Log.d(LOG_TAG, "onActivityResult handled by IABUtil.");
        } else {
            // handle activity results not related to in-app billing
            // we come here if connection first failed, such as if we needed to get the user login
            // the failure sent off the login request as an intent, which then
            // sent back another intent, which arrives here
            if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
                Log.d(LOG_TAG, "in 'onActivityResult' resolution callback (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK)");
                mGoogleApiClient.connect();
            }
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
            // global members generated in export request, and copied to Final vars here to be accessible by other thread
            final long visId = mVisitIdToExport;
            final String fileName = mExportFileName;
            final boolean resolvePh = mResolvePlaceholders;


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
                    long pjId =0, nmId=0;
                    // get the Visit Header information
                    // first get some numeric fields used in later queries
                    sSQL = "SELECT Visits.ProjID, Visits.NamerID "
                            + "FROM Visits WHERE Visits._id = " + visId + ";";
                    thdCs = thdDb.getReadableDatabase().rawQuery(sSQL, null);
                    while (thdCs.moveToNext()) { // should be just one record
                        pjId = thdCs.getLong(thdCs.getColumnIndexOrThrow("ProjID"));
                        nmId = thdCs.getLong(thdCs.getColumnIndexOrThrow("NamerID"));
                    }
                    thdCs.close();
                    // now get the fields to display
                    sSQL = "SELECT Visits.VisitName, Visits.VisitDate, Projects.ProjCode, "
                            + "PlotTypes.PlotTypeDescr, Visits.StartTime, Visits.LastChanged, "
                            + "Namers.NamerName, Visits.Scribe, Locations.LocName, "
                            + "Locations.VisitID, Locations.SubplotID, Locations.ListingOrder, "
                            + "'' || Locations.Latitude AS Latitude, "
                            + "'' || Locations.Longitude AS Longitude, "
                            + "Locations.TimeStamp, "
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
                        if (resolvePh) {
                            sSQL = "SELECT VegItems._id, VegItems.VisitID, VegItems.SubPlotID, "
                                    + "COALESCE(PlaceHolders.IdSppCode, VegItems.OrigCode) AS Code, "
                                    + "COALESCE(PlaceHolders.IdSppDescription, VegItems.OrigDescr) AS Descr, "
                                    + "VegItems.Height, VegItems.Cover, VegItems.Presence, "
                                    + "MAX(IFNULL(VegItems.IdLevelID,0), IFNULL(PlaceHolders.IdLevelID,0)) AS IdLev, "
                                    + "VegItems.TimeCreated, VegItems.TimeLastChanged "
                                    + "FROM VegItems LEFT JOIN PlaceHolders "
                                    + "ON VegItems.OrigCode = PlaceHolders.PlaceHolderCode "
                                    + "WHERE (((VegItems.VisitID)=" + visId + ") "
                                    + "AND ((VegItems.SubPlotID)=" + sbId + ") "
                                    + "AND ((PlaceHolders.ProjID) Is Null Or (PlaceHolders.ProjID)=" + pjId + ") "
                                    + "AND ((PlaceHolders.NamerID) Is Null Or (PlaceHolders.NamerID)=" + nmId + ")) "
                                    + "ORDER BY VegItems.TimeLastChanged;";
                        } else {
                            sSQL = "SELECT VegItems._id, VegItems.VisitID, VegItems.SubPlotID, "
                                    + "VegItems.OrigCode AS Code, VegItems.OrigDescr AS Descr, VegItems.Height, VegItems.Cover, "
                                    + "VegItems.Presence, VegItems.IdLevelID AS IdLev, "
                                    + "VegItems.TimeCreated, VegItems.TimeLastChanged FROM VegItems "
                                    + "WHERE (((VegItems.VisitID)=" + visId + ") "
                                    + "AND ((VegItems.SubPlotID)=" + sbId + ")) "
                                    + "ORDER BY VegItems.TimeLastChanged;";
                        }
                        thdVg = thdDb.getReadableDatabase().rawQuery(sSQL, null);
                        while (thdVg.moveToNext()) {
                            spCode = thdVg.getString(thdVg.getColumnIndexOrThrow("Code"));
                            spDescr = thdVg.getString(thdVg.getColumnIndexOrThrow("Descr"));
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

    // Enables or disables the "please wait" screen for purchases
    void setWaitScreen(boolean set) {
        // implement this later
//        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
//        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    }

    void complain(String message) {
        Log.d(LOG_TAG, "Error: " + message);
        alert("Error: " + message);
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(LOG_TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }
}
