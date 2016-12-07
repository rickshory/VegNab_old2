package com.vegnab.vegnab.util;

/**
 * Created by rshory on 12/6/2016.
 * For database tables that have only 2 fields, [_id] and a text field, this class allows
 *  doing something like n=getID("textString", "fieldName", "tableName")
 *  If the text string is in the table, returns the existing ID,
 *  If it is not yet in the table, adds a new record and returns that ID
 */

public class TableIdManager {
    private String mTextToFind;
    private String mFieldName;
    private String mTableName;
    private long mKey;

    TableIdManager(String tableToUse) {
        mTableName = tableToUse;

    }
}
