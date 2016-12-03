package com.vegnab.vegnab.util;

/**
 * Created by rshory on 12/1/2016.
 */

public class NumberValidator {
    public String mNumberToCheck = null;
    public String mMsgIfMissing = "no value";
    public boolean mValueIsMissing = false;
    private boolean mTreatAsFloat = false;
    public boolean mValueInvalid = false;
    public String mMsgIfInvalid = "invalid value";
    private boolean mCheckRange = false;
    public int mMinInt, mMaxInt;
    public float mMinFloat, mMaxFloat;
    public String mMsgIfOutOfRange = "value out of range";
    private boolean mCheckSpecialValue = false;
    public int mSpecialInt;
    public float mSpecialFloat;
    public String mMsgSpecialValue = "value is special";
    public int mValidInt;
    public float mValidFloat;

    public NumberValidator(String stringToValidate) {
        if ((stringToValidate == null) || (stringToValidate.trim().isEmpty())) {
            mValueIsMissing = true;
        } else {
            try {
                mValidInt = Integer.parseInt(stringToValidate.trim());
            } catch (NumberFormatException e) {
                mValueInvalid = true;
            }
        }
    }

    public NumberValidator(String stringToValidate, float minAllowed, float maxAllowed,
                           String problemIfEmpty, String problemIfNAN, String problemIfOutOfRange) {


    }
}
