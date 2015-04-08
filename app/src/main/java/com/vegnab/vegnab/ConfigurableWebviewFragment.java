package com.vegnab.vegnab;

import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.Tags;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class ConfigurableWebviewFragment extends Fragment {
	final static String ARG_TAG_ID = "tagId";
	WebView mWebView;
	String mTag, mUrl; 
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		// if the activity was re-created (e.g. from a screen rotate)
		// restore the previous parameters remembered by onSaveInstanceState()
		// This is mostly needed in fixed-pane layouts
/*		if (savedInstanceState != null) {
			// restore parameters
			}
		} else {
			// default parameters
		} */
		// inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_show_webview, container, false);
		mWebView = (WebView) rootView.findViewById(R.id.webview);
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
			mTag = args.getString(ARG_TAG_ID);
			switch (mTag) {
			case Tags.WEBVIEW_PLOT_TYPES:
				mUrl = "http://www.vegnab.com/plot_types.html";
				break;
			case Tags.WEBVIEW_REGIONAL_LISTS:
				mUrl = "http://www.vegnab.com/spp_lists.html";
				break;
			case Tags.WEBVIEW_TUTORIAL:
				mUrl = "http://www.vegnab.com/tutorials.html";
				break;
			}
		} else {
			mUrl = "http://www.vegnab.com"; // base web page
		}
		mWebView.loadUrl(mUrl);
	}

}
