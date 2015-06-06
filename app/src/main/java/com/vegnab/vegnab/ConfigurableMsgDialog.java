package com.vegnab.vegnab;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ConfigurableMsgDialog extends DialogFragment 	
{
    private static final String LOG_TAG = ConfigurableMsgDialog.class.getSimpleName();

    private TextView mTxtMsg;
    String mStringTitle, mStringMessage;

    static ConfigurableMsgDialog newInstance(String title, String message) {
        ConfigurableMsgDialog f = new ConfigurableMsgDialog();
        // supply title and message as arguments
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_msg_configurable, root);
        mTxtMsg = (TextView) view.findViewById(R.id.lbl_msg_text);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // during startup, check if arguments are passed to the fragment
        // this is where to do this because the layout has been applied
        // to the fragment
        Bundle args = getArguments();

        if (args != null) {
            mStringTitle = args.getString("title");
            mStringMessage = args.getString("message");
            getDialog().setTitle(mStringTitle);
            mTxtMsg.setText(mStringMessage);
        }
    }
}
