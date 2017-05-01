package com.vegnab.vegnab.util;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * Created by rshory on 5/1/2017.
 */

public class TextInputFilters {

    public static InputFilter fileNameFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
        {
            if (source.length() < 1) return null;
            char last = source.charAt(source.length() - 1);
            String reservedChars = "?:\"*|/\\<>";
            if(reservedChars.indexOf(last) > -1) return source.subSequence(0, source.length() - 1);
            return null;
        }
    };

    public static InputFilter specialCharactersFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String blockCharacterSet = "~#^|$%*!@/()-'\":;,?{}=!$^';,?×÷<>{}€£¥₩%~`¤♡♥_|《》¡¿°•○●□■◇◆♧♣▲▼▶◀↑↓←→☆★▪:-);-):-D:-(:'(:O 1234567890";
            if (source != null && blockCharacterSet.contains(("" + source))) {
                return "";
            }
            return null;
        }
    };


}
