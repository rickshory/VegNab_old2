package com.vegnab.vegnab.util;

/**
 * Created by rshory on 12/1/2016.
 */

public class NumberValidator {
    String mNumberToCheck = null;
    String mMsgIfMissing = "no value";
    boolean mTreatAsFloat = false;
    String mMsgIfInvalid = "invalid value";
    boolean mCheckRange = false;
    int mMinInt, mMaxInt;
    float mMinFloat, mMaxFloat;
    String mMsgIfOutOfRange = "value out of range";
    boolean mCheckSpecialValue = false;
    int mSpecialInt;
    float mSpecialFloat;
    String mMsgSpecialValue = "value is special";


    public NumberValidator(String stringToValidate, float minAllowed, float maxAllowed,
                           String problemIfEmpty, String problemIfNAN, String problemIfOutOfRange) {

    }
}
