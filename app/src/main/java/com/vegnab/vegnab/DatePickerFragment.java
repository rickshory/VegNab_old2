package com.vegnab.vegnab;

import java.util.Calendar;

import com.vegnab.vegnab.database.VNContract.LDebug;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

public class DatePickerFragment extends DialogFragment 
        implements DatePickerDialog.OnDateSetListener {
    EditText txt;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Toast.makeText(this.getActivity(),
                "Year/Month/Day: " + year + "-" + (month + 1) + "-" + day ,
                Toast.LENGTH_SHORT).show();

        txt = (EditText) this.getActivity().findViewById(R.id.txt_date_from);
       if (LDebug.ON) Log.d("DatePicker", "about to test 'txt == null'");
        if (txt == null) {
           if (LDebug.ON) Log.d("DatePicker", "EditText is null ");
        } else {
           if (LDebug.ON) Log.d("DatePicker", "EditText not null ");
        }
//		Log.d("DatePicker", "verifity EditText: " + txt.toString());
//		String s = "2000";
//		txt.setText(s);
//		txt.setText(year + "-" + (month + 1) + "-" + day);
    }
}
