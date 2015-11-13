package com.vegnab.vegnab;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import com.vegnab.vegnab.database.VNContract.LDebug;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DonateFragment extends Fragment implements OnClickListener {

    private static final String LOG_TAG = DonateFragment.class.getSimpleName();
    final static String ARG_JSON_STRING = "jsonString";

    OnButtonListener mButtonCallback; // declare the interface
    // declare that the container Activity must implement this interface
    public interface OnButtonListener {
        // methods that must be implemented in the container Activity
        void onDonateButtonClicked(Bundle args);
    }

    public interface OnIAPDoneListener {
        void onINAppPurchaseComplete(DonateFragment donateFragment);
    }
    OnIAPDoneListener mIAPDoneListener;

    public static DonateFragment newInstance(Bundle args) {
        DonateFragment f = new DonateFragment();
        f.setArguments(args);
        return f;
    }

    JSONArray mProdArray;
    private Spinner mDonationSpinner;
    private Button mBtnDonate;
    private TextView mTxtPlsWaitMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get a Tracker (should auto-report)
        ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
        setHasOptionsMenu(true);
        if (savedInstanceState == null) {

        } else {
            // If the bundle containing the product array were stored during onSaveInstanceState, we
            // would come here. But, in this version, the array is not changed, and so we can
            // use the original one created in newInstance. Don't need to do anything here.
        }
        Bundle args = getArguments();
        if (args == null) {
            // do something if no args sent
           if (LDebug.ON) Log.d(LOG_TAG, "In onCreate, received args = null");
        } else {
            String jsonStr = args.getString(ARG_JSON_STRING);
            if (jsonStr == null) {
                // do something if empty product list sent
               if (LDebug.ON) Log.d(LOG_TAG, "In onCreate, received jsonStr = null");
            } else {
                try {
                    mProdArray = new JSONArray(jsonStr);
                   if (LDebug.ON) Log.d(LOG_TAG, "Received JSON product array: " + mProdArray.toString());
                } catch(JSONException ex) {
                    ex.printStackTrace();
                   if (LDebug.ON) Log.d(LOG_TAG, "JSON error retrieving product list");
                    Toast.makeText(getActivity(), "Error retrieving product list", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

/*
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.donate, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
*/
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        FragmentManager fm = getActivity().getSupportFragmentManager();
        switch (item.getItemId()) { // the Activity has first opportunity to handle these
        // any not handled come here to this Fragment
            case R.id.action_donate_info:
                Toast.makeText(getActivity(), "''App Info'' of Visit Header is not implemented yet", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // if the activity was re-created (e.g. from a screen rotate)
        // restore the previous screen, remembered by onSaveInstanceState()
        // This is mostly needed in fixed-pane layouts
        if (savedInstanceState != null) {
            // mDonationOptionSelected = savedInstanceState.getLong(ARG_DONATION, 0);
        } else {
           if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView, savedInstanceState == null");
        }
//        Toast.makeText(getActivity(), "message here", Toast.LENGTH_SHORT).show();

        // inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_donate, container, false);
//        mDonationOptsRadioGp = (RadioGroup) rootView.findViewById(R.id.radio_group_opts_resolve_phs);
        // Prepare the loader
        // maybe use a loader for purchases later, but none now
        //getLoaderManager().initLoader(Loaders.DONATIONS, null, this);
        mDonationSpinner = (Spinner) rootView.findViewById(R.id.donations_spinner);
        List<String> itemsList = new ArrayList<String>();
        if (mProdArray == null) {
            itemsList.add("(no items)");
            // disable other things
        } else {
            try {
                for (int i=0; i < mProdArray.length(); i++) {
                    JSONObject prodItem = mProdArray.getJSONObject(i);
                   if (LDebug.ON) Log.d(LOG_TAG, "Received JSON product item " + i + ": " + prodItem.toString());
                    String price = prodItem.getString("price");
                    String descr = prodItem.getString("descr");
                    itemsList.add(price + " " + descr);
                }
            } catch(JSONException ex) {
                ex.printStackTrace();
               if (LDebug.ON) Log.d(LOG_TAG, "JSON error retrieving product items");
                Toast.makeText(getActivity(), "Error retrieving product items", Toast.LENGTH_LONG).show();
            }
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, itemsList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        mNamerSpinner.setOnItemSelectedListener(this);
        mDonationSpinner.setAdapter(dataAdapter);

        // set click listener for the button in the view
        mBtnDonate = (Button) rootView.findViewById(R.id.donate_go_button);
        mBtnDonate.setOnClickListener(this);
        // if more, loop through all the child items of the ViewGroup rootView and
        // set the onclicklistener for all the Button instances found

        // save a reference to the "Please wait" message
        mTxtPlsWaitMessage = (TextView) rootView.findViewById(R.id.donate_wait);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(getActivity()).reportActivityStart(getActivity());
        // check if arguments are passed to the fragment that will change the layout
        Bundle args = getArguments();
        if (args == null) {
            // do something if no args sent
        } else {
            // do something if args sent
        }
        // fire off loaders that depend on layout being ready to receive results
        // not yet used
//        getLoaderManager().initLoader(Loaders.DONATE, null, this);
        setWaitMessage(false);
    }

    @Override
    public void onResume() {
        super.onResume();
//	    do other setup here if needed
        // re-check this every time the fragment is entered
//        getLoaderManager().restartLoader(Loaders.VISIT_PLACEHOLDERS_ENTERED, null, this);
//        if (!mLocIsGood) {
//            mGoogleApiClient.connect();
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // if namer spinner has been changed
//        if (mNamerSpinner.getId() != mNamerId) {
//            // attempt to save record
//            saveVisitRecord();
//        }
//        if (mGoogleApiClient.isConnected()) {
//            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
//            mGoogleApiClient.disconnect();
//        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // assure the container activity has implemented the callback interface
        try {
            mButtonCallback = (OnButtonListener) getActivity();
           if (LDebug.ON) Log.d(LOG_TAG, "(OnButtonListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement OnButtonListener interface");
        }
        try {
            mIAPDoneListener = (OnIAPDoneListener) getActivity();
           if (LDebug.ON) Log.d(LOG_TAG, "(IAPDoneListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement IAPDoneListener interface");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_JSON_STRING, mProdArray.toString());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

        case R.id.donate_go_button:
            // maybe implement the tracker here
            long i = mDonationSpinner.getSelectedItemId();
            String sku;
           if (LDebug.ON) Log.d(LOG_TAG, "in onClick, got Spinner item Id " + i);
            try {
                JSONObject prodItem = mProdArray.getJSONObject((int)i);
               if (LDebug.ON) Log.d(LOG_TAG, "Retrieved JSON product item " + i + ": " + prodItem.toString());
                // test the following two before proceeding
                Boolean available = prodItem.getBoolean("available");
                Boolean owned = prodItem.getBoolean("owned");
                // for now, get this to test retrieval
                sku = prodItem.getString("sku");
               if (LDebug.ON) Log.d(LOG_TAG, "in onClick, got SKU: " + sku);
                if (!available) {
                    Toast.makeText(getActivity(), "Item is not available", Toast.LENGTH_LONG).show();
                    return;
                }
                if (owned) {
                    Toast.makeText(getActivity(), "Item is already owned, can not purchase again", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch(JSONException ex) {
                ex.printStackTrace();
               if (LDebug.ON) Log.d(LOG_TAG, "JSON error retrieving product item");
                Toast.makeText(getActivity(), "Error retrieving product item", Toast.LENGTH_LONG).show();
                sku = null;
            }

            if (sku == null) {
                return;
            } else {
                Bundle args = new Bundle();
                args.putString(MainVNActivity.SKU_CHOSEN, sku);
               if (LDebug.ON) Log.d(LOG_TAG, "in onClick, about to do 'mButtonCallback.onDonateButtonClicked(args)'");
                // get an Analytics event tracker
                Tracker donationTracker = ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
                // build and send the Analytics even
                // track that the user initiated a donation
                donationTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Purchase Event")
                        .setAction("Initiated")
                        .setLabel("Donation")
                        .setValue(System.currentTimeMillis()) // maybe make this the purchase amount
                        .build());

                mButtonCallback.onDonateButtonClicked(args);
               if (LDebug.ON) Log.d(LOG_TAG, "in onClick, completed 'mButtonCallback.onDonateButtonClicked(args)'");

            }


            //

//            setWaitMessage(true);
            break;
        }
    }
    // Enables or disables the "please wait" message.
    void setWaitMessage(boolean set) {
        mBtnDonate.setVisibility(set ? View.GONE : View.VISIBLE);
        mTxtPlsWaitMessage.setVisibility(set ? View.VISIBLE : View.GONE);
    }
/*
    // Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
*/
}