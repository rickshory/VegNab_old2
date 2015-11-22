package com.vegnab.vegnab;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.LDebug;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.Prefs;
import com.vegnab.vegnab.database.VNContract.Tags;
import com.vegnab.vegnab.database.VNContract.Validation;
import com.vegnab.vegnab.database.VNContract.VegcodeSources;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class EditSppItemDialog extends DialogFragment implements android.view.View.OnClickListener,
        android.widget.AdapterView.OnItemSelectedListener,
        android.view.View.OnFocusChangeListener, LoaderManager.LoaderCallbacks<Cursor>
        //, android.view.View.OnKeyListener
        {
    private static final String LOG_TAG = EditSppItemDialog.class.getSimpleName();
    public interface EditSppItemDialogListener {
        public void onEditVegItemComplete(DialogFragment dialog);
    }
    EditSppItemDialogListener mEditVegItemListener;

    public static final String VEG_ITEM_REC_ID = "VegItemRecId";
    private long mVegItemRecId = 0; // zero default means new or not specified yet

    public static final String CUR_VISIT_REC_ID = "CurVisitRecId";
    private long mCurVisitRecId = 0;

    public static final String CUR_SUBPLOT_REC_ID = "CurSubplotRecId";
    private long mCurSubplotRecId = -1;

    public static final String REC_SOURCE = "RecSource";
    private int mRecSource;

    public static final String SOURCE_REC_ID = "SourceRecId";
    private long mSourceRecId;

    public static final String PRESENCE_ONLY = "PresenceOnly";
    private boolean mPresenceOnly = true; // default is that this veg item needs only presence/absence

    public static final String VEG_CODE = "VegCode";
    public static final String VEG_DESCR = "VegDescr";
    public static final String VEG_GENUS = "VegGenus";
    public static final String VEG_SPECIES = "VegSpecies";
    public static final String VEG_SUBSPP_VAR = "VegSubsppVar";
    public static final String VEG_VERNACULAR = "VegVernacular";
    public static final String VEG_SUB_LIST_ORDER = "VegSubListOrder";
    public static final String VEG_IS_PLACEHOLDER = "VegIsPlaceholder";
    private String mStrVegCode = null, mStrDescription = null,
            mStrGenus = null, mStrSpecies = null, mStrSubsppVar = null, mStrVernacular = null;
    private int mHeight, mCover, mSublistOrder, mIsPlaceholder;
    private boolean isPresent = true; // assume present; explicit false by user means verified absent
    long mIDConfidence = 1; // default 'no doubt of ID'
    Cursor mCFCursor = null, mDupSppCursor = null;
    boolean mAutoVerifyPresence = false;
    // following flags are used to auto verify
    boolean mOKToAutoAcceptItem = false, mNoDupCodes = false;
    private int mValidationLevel = Validation.SILENT;
    Uri mUri, mVegItemsUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "vegitems");
    ContentValues mValues = new ContentValues();
    private TextView mTxtSpeciesItemLabel, mTxtSppDupLabel, mTxtSppCFLabel, mTxtHeightLabel, mTxtCoverLabel;
    private EditText mEditSpeciesHeight, mEditSpeciesCover;
    private CheckBox mCkSpeciesIsPresent, mCkDontVerifyPresence;
    private Spinner mSpinnerSpeciesConfidence;
    SimpleCursorAdapter mCFSpinnerAdapter;
    private Boolean mBoolRecHasChanged = false;
    SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    static EditSppItemDialog newInstance(Bundle args) {
        EditSppItemDialog f = new EditSppItemDialog();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mEditVegItemListener = (EditSppItemDialogListener) getActivity();
           if (LDebug.ON) Log.d(LOG_TAG, "(EditSppItemDialogListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement EditSppItemDialogListener interface");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_spp_item, root);

        mTxtSpeciesItemLabel = (TextView) view.findViewById(R.id.lbl_spp_item);
        mTxtSppDupLabel = (TextView) view.findViewById(R.id.lbl_spp_dups);
        mTxtHeightLabel = (TextView) view.findViewById(R.id.lbl_spp_height);
        mEditSpeciesHeight = (EditText) view.findViewById(R.id.txt_spp_height);
        mTxtCoverLabel = (TextView) view.findViewById(R.id.lbl_spp_cover);
        mEditSpeciesCover = (EditText) view.findViewById(R.id.txt_spp_cover);
        mCkSpeciesIsPresent = (CheckBox) view.findViewById(R.id.ck_spp_present);
        mCkDontVerifyPresence = (CheckBox) view.findViewById(R.id.ck_spp_present_do_not_ask);
        mTxtSppCFLabel = (TextView) view.findViewById(R.id.lbl_spp_confidence);
        mSpinnerSpeciesConfidence = (Spinner) view.findViewById(R.id.spinner_spp_confidence);
        mSpinnerSpeciesConfidence.setTag(Tags.SPINNER_FIRST_USE); // flag to catch and ignore erroneous first firing
        mSpinnerSpeciesConfidence.setEnabled(false); // will enable when data ready
        mCFSpinnerAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, null,
                new String[] {"IdLevelDescr"},
                new int[] {android.R.id.text1}, 0);
        mCFSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerSpeciesConfidence.setAdapter(mCFSpinnerAdapter);
        mSpinnerSpeciesConfidence.setOnItemSelectedListener(this);

        mEditSpeciesHeight.setOnFocusChangeListener(this);
        mEditSpeciesCover.setOnFocusChangeListener(this);
        mCkSpeciesIsPresent.setOnFocusChangeListener(this);
        mCkDontVerifyPresence.setOnFocusChangeListener(this);

        // enable long-press
        registerForContextMenu(mSpinnerSpeciesConfidence);
        registerForContextMenu(mEditSpeciesHeight);
        registerForContextMenu(mEditSpeciesCover);
        registerForContextMenu(mCkSpeciesIsPresent);
        registerForContextMenu(mCkDontVerifyPresence);

        getDialog().setTitle(R.string.edit_spp_item_title_add); // usually adding, will change to 'edit' if not
        return view;
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onStart() {
        super.onStart();
        // during startup, check if arguments are passed to the fragment
        // this is where to do this because the layout has been applied
        // to the fragment
        // fire off this database request
        getLoaderManager().initLoader(Loaders.VEG_ITEM_CONFIDENCE_LEVELS, null, this);
        Bundle args = getArguments();
        if (args != null) {
            mVegItemRecId = args.getLong(VEG_ITEM_REC_ID);
            mPresenceOnly = args.getBoolean(PRESENCE_ONLY);
            if (mVegItemRecId == 0) { // new record
                mCurVisitRecId = args.getLong(CUR_VISIT_REC_ID);
                mCurSubplotRecId = args.getLong(CUR_SUBPLOT_REC_ID);
                mRecSource = args.getInt(REC_SOURCE);
                mSourceRecId = args.getLong(SOURCE_REC_ID);
                mStrVegCode = args.getString(VEG_CODE);
                mStrDescription = args.getString(VEG_DESCR);
                mStrGenus = args.getString(VEG_GENUS);
                mStrSpecies = args.getString(VEG_SPECIES);
                mStrSubsppVar = args.getString(VEG_SUBSPP_VAR);
                mStrVernacular = args.getString(VEG_VERNACULAR);
                mSublistOrder = args.getInt(VEG_SUB_LIST_ORDER);
                mIsPlaceholder = args.getInt(VEG_IS_PLACEHOLDER);
                mTxtSpeciesItemLabel.setText(mStrDescription);
                setupUI();
            } else {
                // fill the fields with defaults
                mCurVisitRecId = 0;
                mCurSubplotRecId = 0;
                mRecSource = 0;
                mSourceRecId = 0;
                mStrVegCode = "";
                mStrDescription = "";
                mStrGenus = "";
                mStrSpecies = "";
                mStrSubsppVar = "";
                mStrVernacular = "";
                mSublistOrder = 0;
                mIsPlaceholder = 0;
                // request the record to be edited
                getLoaderManager().initLoader(Loaders.VEGITEM_TO_EDIT, null, this);
                // when loader manager returns, will set up UI
            }
            // try this loader here
            getLoaderManager().restartLoader(Loaders.VEG_ITEM_DUP_CODES, null, this);
        }
    }

        void setupUI() {
            // adjust UI depending on whether we want Height/Cover information, or only Presence/Absence
            if (mPresenceOnly) { // hide the Height/Cover views
                mTxtHeightLabel.setVisibility(View.GONE);
                mEditSpeciesHeight.setVisibility(View.GONE);
                mTxtCoverLabel.setVisibility(View.GONE);
                mEditSpeciesCover.setVisibility(View.GONE);
                mCkSpeciesIsPresent.setChecked(isPresent); // set checkmark
            } else { // hide the Presence/Absence views
                mCkSpeciesIsPresent.setVisibility(View.GONE);
                mCkDontVerifyPresence.setVisibility(View.GONE);
            }
            switch (mRecSource) {
                case VegcodeSources.REGIONAL_LIST: // standard NRCS codes from the regional list, first time entered
                case VegcodeSources.PREVIOUSLY_FOUND: // NRCS code, species previously entered
                    // normally named species; may distinguish above two later
                    setCFSpinnerSelection();
                    break; // allow Confidence selector to show for these
//            case VegcodeSources.PLACE_HOLDERS: // user-created codes for species not known at the time
//            case VegcodeSources.SPECIAL_CODES: // e.g. "no veg" (no vegetation on this subplot)
                default: // otherwise hide Confidence selector, has no meaning
                    mTxtSppCFLabel.setVisibility(View.GONE);
                    mSpinnerSpeciesConfidence.setVisibility(View.GONE);
            }

            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            if ((sharedPref.getBoolean(Prefs.VERIFY_VEG_ITEMS_PRESENCE, true)) == false) {
                mOKToAutoAcceptItem = true;
            }
        }
            
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(!hasFocus) { // something lost focus
            mValues.clear();
            switch (v.getId()) {
            case R.id.txt_spp_height:
            case R.id.txt_spp_cover:
            case R.id.ck_spp_present:
                mValidationLevel = Validation.QUIET;
                if (validateVegItemValues()) {
                    int numUpdated = saveVegItemRecord();
                   if (LDebug.ON) Log.d(LOG_TAG, "Saved record in onFocusChange; numUpdated: " + numUpdated);
                }
            }
        }
    }

    @Override
    public void onCancel (DialogInterface dialog) {
        // update the project record in the database, if everything valid
        mValidationLevel = Validation.CRITICAL;
        if (validateVegItemValues()) {
            int numUpdated = saveVegItemRecord();
           if (LDebug.ON) Log.d(LOG_TAG, "Saved record in onCancel; numUpdated: " + numUpdated);
            if (numUpdated > 0) {
                if (mCkDontVerifyPresence.isChecked()) {
                    SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor prefEditor;
                    prefEditor = sharedPref.edit();
                    prefEditor.putBoolean(Prefs.VERIFY_VEG_ITEMS_PRESENCE, false);
                    prefEditor.commit();
                }
                mEditVegItemListener.onEditVegItemComplete(EditSppItemDialog.this);
            }
        }
    }
            
    private void checkAutoAcceptSppItem() {
        if (!mPresenceOnly) return; // can only auto accept presence-only items
        if (!mOKToAutoAcceptItem) return; // only if this is OKd in Preferences
        if (!mNoDupCodes) return; // only if we have checked for duplicate codes and there are none
        if (mVegItemRecId != 0) return; // only applies to new records, id=0
        // try the following line to avoid occasional crashes from (?) race conditions
        mOKToAutoAcceptItem = false; // assure only runs once
        if (saveVegItemRecord() > 0) { // if item is correctly saved
           if (LDebug.ON) Log.d(LOG_TAG, "Saved record in checkAutoAcceptSppItem");
            mEditVegItemListener.onEditVegItemComplete(EditSppItemDialog.this);
            this.dismiss();
        }
    }

    private void getEditedItemDetails() {
        getLoaderManager().initLoader(Loaders.VEGITEM_DETAILS, null, this);
    }

    private int saveVegItemRecord() {
        Context c = getActivity();
        String strSaveDescription;
        mValues.clear();
        if (mPresenceOnly) {
            mValues.put("Presence", (mCkSpeciesIsPresent.isChecked() ? 1 : 0));
        } else {
            mValues.put("Height", mHeight);
            mValues.put("Cover", mCover);
        }
        mValues.put("IdLevelID", mIDConfidence);

/**/
        switch (mRecSource) {
            case VegcodeSources.REGIONAL_LIST: // standard NRCS codes from the regional list, first time entered
            case VegcodeSources.PREVIOUSLY_FOUND: // NRCS code, species previously entered
                // normally named species; may distinguish above two later
                if (mIDConfidence == 3) { // uncertain of genus, build botanical nomenclature
                    strSaveDescription = "CF " + mStrGenus
                            + ((mStrSpecies.length() == 0) ? "" : " " + mStrSpecies)
                            + ((mStrSubsppVar.length() == 0) ? "" : " " + mStrSubsppVar)
                            + ((mStrVernacular.length() == 0) ? "" : ", " + mStrVernacular);
                } else if (mIDConfidence == 2) { // uncertain of species, build botanical nomenclature
                    strSaveDescription = mStrGenus + " CF"
                            + ((mStrSpecies.length() == 0) ? "" : " " + mStrSpecies)
                            + ((mStrSubsppVar.length() == 0) ? "" : " " + mStrSubsppVar)
                            + ((mStrVernacular.length() == 0) ? "" : ", " + mStrVernacular);
                }  else { // usual default, no uncertainty, build botanical nomenclature
                    strSaveDescription = mStrGenus
                            + ((mStrSpecies.length() == 0) ? "" : " " + mStrSpecies)
                            + ((mStrSubsppVar.length() == 0) ? "" : " " + mStrSubsppVar)
                            + ((mStrVernacular.length() == 0) ? "" : ", " + mStrVernacular);
                }
                break;
            case VegcodeSources.PLACE_HOLDERS: // user-created codes for species not known at the time
                // Placeholder, string is stored in Vernacular
                strSaveDescription = mStrVernacular;
                break;
            case VegcodeSources.SPECIAL_CODES: // e.g. "no veg" (no vegetation on this subplot)
            // 'no veg' is the only one so far
                strSaveDescription = "(no veg)";
                break;
            default:
                strSaveDescription = "";
        }

        ContentResolver rs = c.getContentResolver();
        if (mVegItemRecId == -1) {
           if (LDebug.ON) Log.d(LOG_TAG, "entered saveVegItemRecord with (mVegItemRecId == -1); canceled");
            return 0;
        }
        if (mVegItemRecId == 0) { // new record
            // provide the other fields the new record needs
            mValues.put("VisitID", mCurVisitRecId);
            mValues.put("SubPlotID", mCurSubplotRecId);
            mValues.put("SourceID", mRecSource);
            mValues.put("SourceRecID", mSourceRecId);
            mValues.put("OrigCode", mStrVegCode);
            mValues.put("OrigDescr", strSaveDescription);
            mValues.put("TimeCreated", mTimeFormat.format(new Date()));
            mValues.put("TimeLastChanged", mTimeFormat.format(new Date()));

            mUri = rs.insert(mVegItemsUri, mValues);
           if (LDebug.ON) Log.d(LOG_TAG, "new record in saveVegItemRecord; returned URI: " + mUri.toString());
            long newRecId = Long.parseLong(mUri.getLastPathSegment());
            if (newRecId < 1) { // returns -1 on error, e.g. if not valid to save because of missing required field
               if (LDebug.ON) Log.d(LOG_TAG, "new record in saveVegItemRecord has Id == " + newRecId + "); canceled");
                return 0;
            }
            mVegItemRecId = newRecId;
            mUri = ContentUris.withAppendedId(mVegItemsUri, mVegItemRecId);
           if (LDebug.ON) Log.d(LOG_TAG, "new record in saveVegItemRecord; URI re-parsed: " + mUri.toString());
            // save the timestamp, in seconds, when this occurred
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putLong(Prefs.LATEST_VEG_ITEM_SAVE, (new Date().getTime())/1000);
            prefEditor.commit();
            // remember the species, for priority lookup in the future
            if (mRecSource == VegcodeSources.REGIONAL_LIST) { // standard NRCS codes from the regional list, first time entered
                Uri sUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "species");
                mUri = ContentUris.withAppendedId(sUri, mSourceRecId);
                mValues.clear();
                mValues.put("HasBeenFound",1);
                int numUpdated = rs.update(mUri, mValues, null, null);
               if (LDebug.ON) Log.d(LOG_TAG, "Updated species to HasBeenFound=true; numUpdated: " + numUpdated);
            }
            return 1;
        } else {
            mUri = ContentUris.withAppendedId(mVegItemsUri, mVegItemRecId);
            mValues.put("OrigDescr", strSaveDescription);
            mValues.put("TimeLastChanged", mTimeFormat.format(new Date()));
            int numUpdated = rs.update(mUri, mValues, null, null);
           if (LDebug.ON) Log.d(LOG_TAG, "Saved record in saveVegItemRecord; numUpdated: " + numUpdated);
            return numUpdated;
        }

    }

    private boolean validateVegItemValues() {
        // validate all user-accessible items
        Context c = getActivity();
        String stringProblem;
        String errTitle = c.getResources().getString(R.string.vis_hdr_validate_generic_title);
        ConfigurableMsgDialog flexErrDlg = new ConfigurableMsgDialog();
        int Ht, Cv;
        if (mPresenceOnly) { // check and get Presence
            if (mAutoVerifyPresence) {
                isPresent = true;
            } else {
                isPresent = mCkSpeciesIsPresent.isChecked();
            }
            return true;
        } else { // verify numerics Height & Cover
            // validate Height
            String stringHt = mEditSpeciesHeight.getText().toString().trim();
            if (stringHt.length() == 0) {
               if (LDebug.ON) Log.d(LOG_TAG, "Height is length zero");
                if (mValidationLevel > Validation.SILENT) {
                    stringProblem = c.getResources().getString(R.string.edit_spp_item_msg_no_height);
                    if (mValidationLevel == Validation.QUIET) {
                        Toast.makeText(this.getActivity(),
                                stringProblem,
                                Toast.LENGTH_LONG).show();
                    }
                    if (mValidationLevel == Validation.CRITICAL) {
                        flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                        flexErrDlg.show(getFragmentManager(), "frg_err_height_out_of_range");
                        mEditSpeciesHeight.requestFocus();
                    }
                } // end of validation not silent
                return false; // end of Ht length zero
            } else {
                try {
                    Ht = Integer.parseInt(stringHt);
                    if ((Ht < 0) || (Ht > 35000)) { // tallest plants are 300m redwoods
                       if (LDebug.ON) Log.d(LOG_TAG, "Height is out of range");
                        if (mValidationLevel > Validation.SILENT) {
                            stringProblem = c.getResources().getString(R.string.edit_spp_item_validate_height_bad);
                            if (mValidationLevel == Validation.QUIET) {
                                Toast.makeText(this.getActivity(),
                                        stringProblem,
                                        Toast.LENGTH_LONG).show();
                            }
                            if (mValidationLevel == Validation.CRITICAL) {
                                flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                                flexErrDlg.show(getFragmentManager(), "frg_err_height_out_of_range");
                                mEditSpeciesHeight.requestFocus();
                            }
                        } // end of validation not silent
                        return false; // end of Ht out of range
                    }
                    if (Ht == 0) {
                        errTitle = c.getResources().getString(R.string.edit_spp_item_title_pls_note);
                        stringProblem = c.getResources().getString(R.string.edit_spp_item_msg_zero_height);
                        flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                        flexErrDlg.show(getFragmentManager(), "frg_verify_height_zero");
                        Cv = 0;
                       if (LDebug.ON) Log.d(LOG_TAG, "Height is zero, Cover set to zero");
                    }
                } catch(NumberFormatException e) {
                   if (LDebug.ON) Log.d(LOG_TAG, "Height is not a valid number");
                    if (mValidationLevel > Validation.SILENT) {
                        stringProblem = c.getResources().getString(R.string.edit_spp_item_validate_height_bad);
                        if (mValidationLevel == Validation.QUIET) {
                            Toast.makeText(this.getActivity(),
                                    stringProblem,
                                    Toast.LENGTH_LONG).show();
                        }
                        if (mValidationLevel == Validation.CRITICAL) {
                            flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                            flexErrDlg.show(getFragmentManager(), "frg_err_height_out_of_range");
                            mEditSpeciesHeight.requestFocus();
                        }
                    } // end of validation not silent
                    return false; // end of Ht invalid number
                }
            } // end of validate Height

            // validate Cover
            String stringCv = mEditSpeciesCover.getText().toString().trim();
            if (stringCv.length() == 0) {
               if (LDebug.ON) Log.d(LOG_TAG, "Cover is length zero");
                if (mValidationLevel > Validation.SILENT) {
                    stringProblem = c.getResources().getString(R.string.edit_spp_item_msg_no_cover);
                    if (mValidationLevel == Validation.QUIET) {
                        Toast.makeText(this.getActivity(),
                                stringProblem,
                                Toast.LENGTH_LONG).show();
                    }
                    if (mValidationLevel == Validation.CRITICAL) {
                        flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                        flexErrDlg.show(getFragmentManager(), "frg_err_cover_out_of_range");
                        mEditSpeciesCover.requestFocus();
                    }
                } // end of validation not silent
                return false; // end of Cv length zero
            } else {
                try {
                    Cv = Integer.parseInt(stringCv);
                    if ((Cv < 0) || (Cv > 100)) { // percent
                       if (LDebug.ON) Log.d(LOG_TAG, "Cover is out of range");
                        if (mValidationLevel > Validation.SILENT) {
                            stringProblem = c.getResources().getString(R.string.edit_spp_item_validate_cover_bad);
                            if (mValidationLevel == Validation.QUIET) {
                                Toast.makeText(this.getActivity(),
                                        stringProblem,
                                        Toast.LENGTH_LONG).show();
                            }
                            if (mValidationLevel == Validation.CRITICAL) {
                                flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                                flexErrDlg.show(getFragmentManager(), "frg_err_cover_out_of_range");
                                mEditSpeciesCover.requestFocus();
                            }
                        } // end of validation not silent
                        return false; // end of Cv out of range
                    }
                    if (Cv == 0) {
                        errTitle = c.getResources().getString(R.string.edit_spp_item_title_pls_note);
                        stringProblem = c.getResources().getString(R.string.edit_spp_item_msg_zero_cover);
                        flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                        flexErrDlg.show(getFragmentManager(), "frg_verify_cover_zero");
                        Ht = 0;
                    }
                } catch(NumberFormatException e) {
                   if (LDebug.ON) Log.d(LOG_TAG, "Cover is not a valid number");
                    if (mValidationLevel > Validation.SILENT) {
                        stringProblem = c.getResources().getString(R.string.edit_spp_item_validate_cover_bad);
                        if (mValidationLevel == Validation.QUIET) {
                            Toast.makeText(this.getActivity(),
                                    stringProblem,
                                    Toast.LENGTH_LONG).show();
                        }
                        if (mValidationLevel == Validation.CRITICAL) {
                            flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                            flexErrDlg.show(getFragmentManager(), "frg_err_cover_out_of_range");
                            mEditSpeciesCover.requestFocus();
                        }
                    } // end of validation not silent
                    return false; // end of Cv invalid number
                }
            } // end of verify Cover
            mHeight = Ht;
            mCover = Cv;
            mEditSpeciesHeight.setText("" + Ht);
            mEditSpeciesCover.setText("" + Cv);
            return true;
        } // end of verify numeric Height & Cover
    } // end of validation

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        String[] params = null;
        switch (id) {
        case Loaders.VEGITEM_TO_EDIT:
            Uri oneVegItemUri = ContentUris.withAppendedId(
                Uri.withAppendedPath(
                ContentProvider_VegNab.CONTENT_URI, "vegitems"), mVegItemRecId);
            cl = new CursorLoader(getActivity(), oneVegItemUri,
                    null, select, null, null);
            break;

        case Loaders.VEG_ITEM_CONFIDENCE_LEVELS:
            Uri allCFLevelsUri = Uri.withAppendedPath(
                    ContentProvider_VegNab.CONTENT_URI, "idlevels");
            select = "(_id <= 3)"; // don't offer 'not identified' here, only for Placeholders never identified
            // mIDConfidence
            cl = new CursorLoader(getActivity(), allCFLevelsUri,
                    null, select, null, null);
            break;

        case Loaders.VEG_ITEM_DUP_CODES:
            Uri dupSppUri = ContentProvider_VegNab.SQL_URI;
            select = "SELECT OrigCode, IdLevelID FROM VegItems WHERE "
                + "VisitID = ? AND SubPlotID = ? AND OrigCode = ? AND _id != ?;";
            params = new String[] {"" + mCurVisitRecId, "" + mCurSubplotRecId, mStrVegCode, "" + mVegItemRecId };
            cl = new CursorLoader(getActivity(), dupSppUri,
                    null, select, params, null);
            break;

        case Loaders.VEGITEM_DETAILS:
            Uri vegDetailsUri = ContentProvider_VegNab.SQL_URI;
            switch (mRecSource) {

                case VegcodeSources.REGIONAL_LIST:
                    select = "SELECT Genus, Species, SubsppVar, Vernacular "
                            + "FROM NRCSSpp "
                            + "WHERE _id = " + mSourceRecId + ";";
                    break;

                case VegcodeSources.PLACE_HOLDERS:
                    select = "SELECT '' AS Genus, '' AS Species, "
                            + "'' AS SubsppVar, Description AS Vernacular "
                            + "FROM PlaceHolders "
                            + "WHERE _id = " + mSourceRecId + ";";
                    break;
            }
            cl = new CursorLoader(getActivity(), vegDetailsUri,
                    null, select, null, null);
            break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        switch (loader.getId()) {

        case Loaders.VEGITEM_TO_EDIT:
           if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, records: " + c.getCount());
            if (c.moveToFirst()) {
                getDialog().setTitle(R.string.edit_spp_item_title_edit);
                String vegItemLabel = c.getString(c.getColumnIndexOrThrow("OrigCode")) + ": "
                        + c.getString(c.getColumnIndexOrThrow("OrigDescr"));
                mTxtSpeciesItemLabel.setText(vegItemLabel);
                mEditSpeciesHeight.setText(c.getString(c.getColumnIndexOrThrow("Height")));
                mEditSpeciesCover.setText(c.getString(c.getColumnIndexOrThrow("Cover")));
                int presInt = c.getInt(c.getColumnIndexOrThrow("Presence"));
                boolean present = ((presInt == 1) ? true : false);
                mCkSpeciesIsPresent.setChecked(present);
                // get other fields, may not use all
                mCurVisitRecId = c.getLong(c.getColumnIndexOrThrow("VisitID"));
                mCurSubplotRecId = c.getLong(c.getColumnIndexOrThrow("SubPlotID"));
                mRecSource = c.getInt(c.getColumnIndexOrThrow("SourceID"));
                mSourceRecId = c.getLong(c.getColumnIndexOrThrow("SourceRecID"));
                mStrVegCode = c.getString(c.getColumnIndexOrThrow("OrigCode"));
                mStrDescription = c.getString(c.getColumnIndexOrThrow("OrigDescr"));
                mIDConfidence = c.getInt(c.getColumnIndexOrThrow("IdLevelID"));
                // following is integer for DB compatibility
                mIsPlaceholder = ((mRecSource == VegcodeSources.PLACE_HOLDERS) ? 1 : 0);

                // in 50ms, get 4 detail fields of edited item: Genus, Species, SubsppVar, Vernacular
                // have these on hand when exiting the dialog
                mTxtSpeciesItemLabel.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getEditedItemDetails(); // get the 4 detail fields
                    }
                }, 50);

                setupUI();

                // CheckBox mCkSpeciesIsPresent, mCkDontVerifyPresence;
                // set up spinner
            }
            break;

        case Loaders.VEG_ITEM_CONFIDENCE_LEVELS:
            // Swap the new cursor in
            // The framework will take care of closing the old cursor once we return
            mCFCursor = c;
            mCFSpinnerAdapter.swapCursor(mCFCursor);
            if (mCFCursor.getCount() > 0) {
                // setCFSpinnerSelection();
                mSpinnerSpeciesConfidence.setEnabled(true);
            } else {
                mSpinnerSpeciesConfidence.setEnabled(false);
            }
            break;

        case Loaders.VEG_ITEM_DUP_CODES:
            mDupSppCursor = c;
            if (mDupSppCursor.getCount() == 0) {
                mTxtSppDupLabel.setVisibility(View.GONE);
                mNoDupCodes = true;
            }

            // in 50ms, check if we can auto verify
            mCkDontVerifyPresence.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkAutoAcceptSppItem(); // see if we can auto accept this item
                }
            }, 50);

            break;

        case Loaders.VEGITEM_DETAILS:
            if (c.moveToFirst()) {
                mStrGenus = c.getString(c.getColumnIndexOrThrow("Genus"));
                mStrSpecies = c.getString(c.getColumnIndexOrThrow("Species"));
                mStrSubsppVar = c.getString(c.getColumnIndexOrThrow("SubsppVar"));
                mStrVernacular = c.getString(c.getColumnIndexOrThrow("Vernacular"));

                /*
                mSublistOrder = 0;
                */
            }
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
        case Loaders.VEGITEM_TO_EDIT:
            // nothing to do here since no adapter
            break;
        case Loaders.VEG_ITEM_CONFIDENCE_LEVELS:
            mCFSpinnerAdapter.swapCursor(null);
            break;
        case Loaders.VEG_ITEM_DUP_CODES:
            // nothing to do here since no adapter
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
        //Cursor cur = (Cursor)mNamerAdapter.getItem(position);
        //String strSel = cur.getString(cur.getColumnIndex("NamerName"));
        //Log.d(LOG_TAG, strSel);
        // if spinner is filled by Content Provider, can't get text by:
        //String strSel = parent.getItemAtPosition(position).toString();
        // that returns something like below, which there is no way to get text out of:
        // "android.content.ContentResolver$CursorWrapperInner@42041b40"

        // sort out the spinners
        // can't use switch because not constants
        if (parent.getId() == mSpinnerSpeciesConfidence.getId()) {
            // workaround for spinner firing when first set
            if(((String)parent.getTag()).equalsIgnoreCase(Tags.SPINNER_FIRST_USE)) {
                parent.setTag("");
                return;
            }
            mIDConfidence = id;
            return;
        }
        // write code for any other spinner(s) here
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    public void setCFSpinnerSelection() {
        if (mCFCursor == null) {
            return;
        }
        // set the id confidence spinner
        int ct = mCFCursor.getCount();
        for (int i=0; i<ct; i++) {
           if (LDebug.ON) Log.d(LOG_TAG, "Setting mSpinnerSpeciesConfidence; testing index " + i);
            if (mSpinnerSpeciesConfidence.getItemIdAtPosition(i) == mIDConfidence) {
               if (LDebug.ON) Log.d(LOG_TAG, "Setting mSpinnerSpeciesConfidence; found matching index " + i);
                mSpinnerSpeciesConfidence.setSelection(i);
                break;
            }
        }
    }

}
