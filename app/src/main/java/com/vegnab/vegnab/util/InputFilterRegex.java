package com.vegnab.vegnab.util;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rshory on 5/2/2017.
 */

public class InputFilterRegex implements InputFilter {

    private Pattern mPattern;
    private static final String CLASS_NAME = InputFilterRegex.class.getSimpleName();

    /**
     * Convenience constructor, builds Pattern object from a String
     * @param pattern Regex string to build pattern from.
     */
    public InputFilterRegex(String pattern) {
        this(Pattern.compile(pattern));
    }

    public InputFilterRegex(Pattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException(CLASS_NAME + " requires a regex.");
        }

        mPattern = pattern;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {

        Matcher matcher = mPattern.matcher(source);
        if (!matcher.matches()) {
            return "";
        }

        return null;
    }
}
