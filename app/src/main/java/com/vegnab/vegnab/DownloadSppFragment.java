package com.vegnab.vegnab;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.vegnab.vegnab.database.DataBaseHelper;
import com.vegnab.vegnab.database.VNContract.Prefs;

import java.io.FileNotFoundException;
import java.io.IOException;

public class DownloadSppFragment extends Fragment
		implements OnClickListener {
	private boolean downloadInProgress;
	private long downloadStartTimeMs, downloadEndTimeMs, downloadElapsedTimeMs = 0;
	private DownloadManager downloadManager;
	private long downloadReference;
	Button mBtnStartDownload, mBtnDisplayDownload, mBtnCheckStatus, mBtnCancelDownload;
	TextView mTxtVwShowSpp;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Get a Tracker (should auto-report)
		((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		// if the activity was re-created (e.g. from a screen rotate)
		// restore the previous screen, remembered by onSaveInstanceState()
		// This is mostly needed in fixed-pane layouts
		if (savedInstanceState != null) {
		}
		// inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_download_spp, container, false);
		//start download button
		mBtnStartDownload = (Button) rootView.findViewById(R.id.startDownload);
		mBtnStartDownload.setOnClickListener(this);
		
		//display all download button
		mBtnDisplayDownload = (Button) rootView.findViewById(R.id.displayDownload);
		mBtnDisplayDownload.setOnClickListener(this);
		
		//check download status button
		mBtnCheckStatus = (Button) rootView.findViewById(R.id.checkStatus);
		mBtnCheckStatus.setOnClickListener(this);
		mBtnCheckStatus.setEnabled(false);
		
		//cancel download button
		mBtnCancelDownload = (Button) rootView.findViewById(R.id.cancelDownload);
		mBtnCancelDownload.setOnClickListener(this);
		mBtnCancelDownload.setEnabled(false);
		
		mTxtVwShowSpp = (TextView) rootView.findViewById(R.id.spp_data);
		
		//set filter to only when download is complete and register broadcast receiver
		IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		getActivity().registerReceiver(downloadReceiver, filter);

		return rootView;
	}
	
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setHasOptionsMenu(true);
//	}
	
//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		inflater.inflate(R.menu.test_download, menu);
//		super.onCreateOptionsMenu(menu, inflater);
//	}	
	
	public void onClick(View v) {
		switch (v.getId()) {
		//start the download process
		case R.id.startDownload:
			downloadInProgress = true;
			downloadStartTimeMs = System.currentTimeMillis();
			// downloadStartTimeMs, downloadEndTimeMs, downloadElapsedTimeMs
			downloadManager = (DownloadManager)getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
			Uri Download_Uri = Uri.parse("http://www.vegnab.com/specieslists/USASpecies.txt");
			DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
				
			//Restrict the types of networks over which this download may proceed.
			request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
			//Set whether this download may proceed over a roaming connection.
			request.setAllowedOverRoaming(true);
			//Set the title of this download, to be displayed in notifications (if enabled).
			request.setTitle("" + getResources().getString(R.string.dnld_spp_title));
			//Set a description of this download, to be displayed in notifications (if enabled)
			request.setDescription("" + getResources().getString(R.string.dnld_spp_descr));
			//Set the local destination for the downloaded file to a path within the application's external files directory
			request.setDestinationInExternalFilesDir(getActivity(),Environment.DIRECTORY_DOWNLOADS,"VegNabSpeciesList.txt");
		
			//Enqueue a new download and save the referenceId
			downloadReference = downloadManager.enqueue(request);
			
			mTxtVwShowSpp.setText("" + getResources().getString(R.string.dnld_spp_pls_wait));
				
			mBtnCheckStatus.setEnabled(true);
			mBtnCancelDownload.setEnabled(true);
			break;
		
		//display all downloads 
		case R.id.displayDownload: 
			Intent intent = new Intent();
			intent.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
			startActivity(intent);
			break;
		
		//check the status of a download 
		case R.id.checkStatus: 
			Query myDownloadQuery = new Query();
			//set the query filter to our previously Enqueued download 
			myDownloadQuery.setFilterById(downloadReference);
			
			//Query the download manager about downloads that have been requested.
			Cursor cursor = downloadManager.query(myDownloadQuery);
			if(cursor.moveToFirst()){
				checkStatus(cursor);
			}
			break;
		
		//cancel the ongoing download 
		case R.id.cancelDownload: 
			downloadManager.remove(downloadReference);
			mBtnCheckStatus.setEnabled(false);
			mTxtVwShowSpp.setText("" + getResources().getString(R.string.dnld_spp_canceled));
			break; 
		
		// More buttons go here (if any) ...
		
		}
	}
	
	private void checkStatus(Cursor cursor){
		//column for status
		int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
		int status = cursor.getInt(columnIndex);
		//column for reason code if the download failed or paused
		int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
		int reason = cursor.getInt(columnReason);
		//get the download filename
		int filenameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
		String filename = cursor.getString(filenameIndex);
	
		String statusText = "";
		String reasonText = "";
		
		switch(status){
		case DownloadManager.STATUS_FAILED:
			statusText = "STATUS_FAILED";
			switch(reason){
			case DownloadManager.ERROR_CANNOT_RESUME:
				reasonText = "ERROR_CANNOT_RESUME";
				break;
			case DownloadManager.ERROR_DEVICE_NOT_FOUND:
				reasonText = "ERROR_DEVICE_NOT_FOUND";
				break;
			case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
				reasonText = "ERROR_FILE_ALREADY_EXISTS";
				break;
			case DownloadManager.ERROR_FILE_ERROR:
				reasonText = "ERROR_FILE_ERROR";
				break;
			case DownloadManager.ERROR_HTTP_DATA_ERROR:
				reasonText = "ERROR_HTTP_DATA_ERROR";
				break;
			case DownloadManager.ERROR_INSUFFICIENT_SPACE:
				reasonText = "ERROR_INSUFFICIENT_SPACE";
				break;
			case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
				reasonText = "ERROR_TOO_MANY_REDIRECTS";
				break;
			case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
				reasonText = "ERROR_UNHANDLED_HTTP_CODE";
				break;
			case DownloadManager.ERROR_UNKNOWN:
				reasonText = "ERROR_UNKNOWN";
				break;
			}
			break;
		case DownloadManager.STATUS_PAUSED:
			statusText = "STATUS_PAUSED";
			switch(reason){
			case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
				reasonText = "PAUSED_QUEUED_FOR_WIFI";
				break;
			case DownloadManager.PAUSED_UNKNOWN:
				reasonText = "PAUSED_UNKNOWN";
				break;
			case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
				reasonText = "PAUSED_WAITING_FOR_NETWORK";
				break;
			case DownloadManager.PAUSED_WAITING_TO_RETRY:
				reasonText = "PAUSED_WAITING_TO_RETRY";
				break;
			}
			break;
		case DownloadManager.STATUS_PENDING:
			statusText = "STATUS_PENDING";
			break;
		case DownloadManager.STATUS_RUNNING:
			statusText = "STATUS_RUNNING";
			break;
		case DownloadManager.STATUS_SUCCESSFUL:
			statusText = "STATUS_SUCCESSFUL";
			reasonText = "Filename:\n" + filename;
			break;
		}
		
		
		Toast toast = Toast.makeText(getActivity(), 
			statusText + "\n" + 
			reasonText, 
			Toast.LENGTH_LONG);
		toast.setGravity(Gravity.TOP, 25, 400);
		toast.show();
	
	}
	
	private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
				
			//check if the broadcast message is for our Enqueued download
			long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
			if(downloadReference == referenceId){
				
				mBtnCancelDownload.setEnabled(false);
		
				ParcelFileDescriptor file;
				//parse the data
				try {
					file = downloadManager.openDownloadedFile(downloadReference);
					DataBaseHelper vnDb = new DataBaseHelper(getActivity());
					String listLabel = vnDb.fillSpeciesTable(file);
					// returns the description of the species list from the first line in the file, e.g. "Oregon"
					SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
					SharedPreferences.Editor prefEditor = sharedPref.edit();
					prefEditor.putString(Prefs.SPECIES_LIST_DESCRIPTION, listLabel);
                    // improve this, test if table now contains any species
                    prefEditor.putBoolean(Prefs.SPECIES_LIST_DOWNLOADED, true);
					prefEditor.commit();
					
					// 'file' here is a pointer, cannot directly read InputStream
/*
					InputStream is = new FileInputStream(file.getFileDescriptor()); // use getFileDescriptor to get InputStream
					// wrap InputStream with an InputStreamReader, which is wrapped by a BufferedReader, "trick" to use readLine() fn
					BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
					StringBuilder lines = new StringBuilder();
					String line, code, descr;
					String[] lineParts;
					long ct = 0;
					while ((line = br.readLine()) != null) {
						// readLine gets the lines one at a time, strips the delimiters
						lineParts = line.split("\t");
						code = lineParts[0].replace('\'', '`');
						descr = lineParts[1].replace('\'', '`');
						lines.append("('").append(code).append("', '").append(descr).append("'), \n\r");
						ct++;
					}
					br.close();
					mTxtVwShowSpp.setText(lines.toString()); // lines are all combined, will parse them into DB in final version
*/					
					mTxtVwShowSpp.setText("" + getResources().getString(R.string.dnld_spp_parsed));
					Toast toast = Toast.makeText(getActivity(),
                        "" + getResources().getString(R.string.dnld_spp_done),
						Toast.LENGTH_LONG);
					toast.setGravity(Gravity.TOP, 25, 400);
					toast.show();
					
					// delete the file when done
					downloadManager.remove(downloadReference);

					if (downloadInProgress) {
						downloadInProgress = false;
						downloadEndTimeMs = System.currentTimeMillis();
						downloadElapsedTimeMs += (downloadEndTimeMs - downloadStartTimeMs);
						// Build and send a timing hit.
						Tracker sppDownloadTracker = ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);
						sppDownloadTracker.send(new HitBuilders.TimingBuilder()
								.setCategory("Downloads")
								.setValue(downloadElapsedTimeMs)
								.setVariable("Complete Species List")
								.setLabel("USA")
								.build());
					}
				
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
}
