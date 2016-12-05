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
    public boolean mValueOutOfRange = false;
    public String mMsgIfOutOfRange = "value out of range";
    private boolean mCheckSpecialValue = false;
    public int mSpecialInt;
    public float mSpecialFloat;
    public String mMsgSpecialValue = "value is special";
    public int mValidInt;
    public float mValidFloat;

    public NumberValidator(String stringToValidate) {
        mNumberToCheck = stringToValidate;
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

    public NumberValidator(String stringToValidate, boolean isFloat) {
        mNumberToCheck = stringToValidate;
        mTreatAsFloat = isFloat;
        if ((stringToValidate == null) || (stringToValidate.trim().isEmpty())) {
            mValueIsMissing = true;
        } else {
            if (isFloat) {
                try {
                    mValidFloat = Float.parseFloat(stringToValidate.trim());
                } catch (NumberFormatException e) {
                    mValueInvalid = true;
                }
            } else {
                try {
                    mValidInt = Integer.parseInt(stringToValidate.trim());
                } catch (NumberFormatException e) {
                    mValueInvalid = true;
                }
            }
        }
    }

    public NumberValidator(String stringToValidate, int minAllowed, int maxAllowed) {
        mNumberToCheck = stringToValidate;
        mCheckRange = true;
        if ((stringToValidate == null) || (stringToValidate.trim().isEmpty())) {
            mValueIsMissing = true;
        } else {
            try {
                mValidInt = Integer.parseInt(stringToValidate.trim());
                if ((mValidInt < minAllowed) || (mValidInt > maxAllowed)) {
                    mValueOutOfRange = true;
                }

            } catch (NumberFormatException e) {
                mValueInvalid = true;
            }
        }
    }
    public NumberValidator(String stringToValidate, float minAllowed, float maxAllowed,
                           String problemIfEmpty, String problemIfNAN, String problemIfOutOfRange) {


    }
}
