package com.vegnab.vegnab.util;

/**
 * Created by rshory on 5/2/2017.
 */

public class InputFilterPlaceholderCode extends InputFilterRegex {

    private static final String PLACEHOLDER_CODE_CHARACTERS_ALLOWED_REGEX = "[ a-zA-Z0-9]+";

    public InputFilterPlaceholderCode() {
        super(PLACEHOLDER_CODE_CHARACTERS_ALLOWED_REGEX);
    }
}
