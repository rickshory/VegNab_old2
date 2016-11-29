package com.vegnab.vegnab.util;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * Created by rshory on 11/29/2016.
 */

public class ValidateNumber implements InputFilter {
    private float min, max;

    public ValidateNumber(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public ValidateNumber(String min, String max) {
        this.min = Float.parseFloat(min);
        this.max = Float.parseFloat(max);
    }
    
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            // Remove the string out of destination that is to be replaced
            String newVal = dest.toString().substring(0, dstart)
                    + dest.toString().substring(dend, dest.toString().length());
            // Add the new string in
            newVal = newVal.substring(0, dstart) + source.toString()
                    + newVal.substring(dstart, newVal.length());
            float input = Float.parseFloat(dest.toString() + source.toString());
            if (isInRange(min, max, input))
                return null;
        } catch (NumberFormatException nfe) { }
        return "";
    }
    private boolean isInRange(float low, float high, float test) {
        return high > low ? test >= low && test <= high : test >= high && test <= low;
    }

}
