package com.vegnab.vegnab.util;

/**
 * Created by rshory on 5/2/2017.
 */

public class InputFilterSppNamer extends InputFilterRegex {

    private static final String SPP_NAMER_CHARACTERS_ALLOWED_REGEX = "[ a-zA-Z]+";

    public InputFilterSppNamer() {
        super(SPP_NAMER_CHARACTERS_ALLOWED_REGEX);
    }
}
