/**
 * 
 */
package com.vegnab.vegnab.database;

import android.provider.BaseColumns;

/**
 * @author rshory
 *
 */
public final class VNContract {
	// empty constructor to prevent accidental instantiation
	public VNContract() {}
	// inner class to define preferences
	public static abstract class Prefs {
		public static final String DEFAULT_PROJECT_ID = "Default_Project_Id";
		public static final String DEFAULT_PROJECT_NAME = "Default_Project_Name";
		public static final String DEFAULT_PLOTTYPE_ID = "Default_PlotType_Id";
		public static final String DEFAULT_PLOTTYPE_NAME = "Default_PlotType_Name";
		public static final String DEFAULT_NAMER_ID = "Default_Namer_Id";
		public static final String DEFAULT_NAMER_NAME = "Default_Namer_Name";
		public static final String CURRENT_VISIT_ID = "Current_Visit_Id";
		public static final String CURRENT_VISIT_NAME = "Current_Visit_Name";
		public static final String TARGET_ACCURACY_OF_VISIT_LOCATIONS = "Target_Accuracy_VisitLocs";
		public static final String TARGET_ACCURACY_OF_MAPPED_LOCATIONS = "Target_Accuracy_MappedLocs";
		public static final String UNIQUE_DEVICE_ID = "Unique_Device_Id";
		public static final String DEVICE_ID_SOURCE = "Device_Id_Source";
		public static final String SPECIES_LIST_DESCRIPTION = "Species_List_Description";
        public static final String LOCAL_SPECIES_LIST_DESCRIPTION = "LocalSpecies_List_Description";
        public static final String SPECIES_LIST_DOWNLOADED = "Species_List_Downloaded";
		public static final String VERIFY_VEG_ITEMS_PRESENCE = "VerifyVegItemsPresence";
		
	}
	// inner class to define loader IDs
	// putting them all together here helps avoid conflicts in various fragments
	public static abstract class Loaders {
		public static final int TEST_SQL = 0; // test loading from raw SQL
		// in New Visit
		public static final int PROJECTS = 1; // Loader Id for Projects
		public static final int PLOTTYPES = 2; // Loader Id for Plot Types
		public static final int PREV_VISITS = 3; // Loader ID for previous Visits
		public static final int VALID_DEL_PROJECTS = 4; // Loader Id for the list of Projects that are valid to delete
		// in Edit Project
		public static final int EXISTING_PROJCODES = 11; // to disallow duplicates
		public static final int PROJECT_TO_EDIT = 12; //
		// in Visit Header
		public static final int VISIT_TO_EDIT = 21; // the current Visit
		public static final int EXISTING_VISITS = 22; // Visits other than the current, to check duplicates
		public static final int NAMERS = 23; // all Namers, to choose from
		public static final int LOCATIONS = 24;
		public static final int VISIT_REF_LOCATION = 25; // the reference Location for this Visit
		// in Edit Namer
		public static final int NAMER_TO_EDIT = 31;
		public static final int EXISTING_NAMERS = 32; // Namers other than the current, to check duplicates
		// in Delete Namer
		public static final int VALID_DEL_NAMERS = 41; // Namers that are valid to delete
		// in Date Entry Container
		public static final int CURRENT_SUBPLOTS = 51; // Subplots for the current visit
		// in Veg Subplot
		public static final int CURRENT_SUBPLOT = 61; // Header info for the current subplot
		public static final int CURRENT_SUBPLOT_SPP = 71; // Veg species for the current subplot
		public static final int BASE_SUBPLOT = 1000; // Header info for the current subplot; base number, instances will increment
		public static final int BASE_SUBPLOT_SPP = 2000; // Veg species for the current subplot; base number, instances will increment
		// in Species Select
		public static final int SPP_MATCHES = 71; // Species that match the search string
		public static final int CODES_ALREADY_ON_SUBPLOT = 72; // To warn about duplicates
		// in Main Activity
		// in Edit VegItem
		public static final int VEGITEM_TO_EDIT = 81; // The current veg item
		public static final int VEG_ITEM_CONFIDENCE_LEVELS = 82; //
		public static final int VEG_ITEM_SUBPLOT = 83; // parameters needed to correctly show dialog
		public static final int VEG_ITEM_VISIT = 84; // parameters needed to process dialog
		
		
	}

	// inner class to define Tags
	// putting them all together here helps use across fragments
	public static abstract class Tags {
		public static final String SPINNER_FIRST_USE = "FirstTime"; // flag to catch and ignore erroneous first firing
		public static final String NEW_VISIT = "NewVisit";
		public static final String VISIT_HEADER = "VisitHeader";
		public static final String TEST_WEBVIEW = "TestWebview";
		public static final String WEBVIEW_TUTORIAL = "WebviewTutorial";
		public static final String WEBVIEW_PLOT_TYPES = "WebviewPlotTypes";
		public static final String WEBVIEW_REGIONAL_LISTS = "WebviewSppLists";
		public static final String DATA_SCREENS_CONTAINER = "DataScreensContainer";
		public static final String VEG_SUBPLOT = "VegSubplot";
		public static final String SELECT_SPECIES = "SelectSpecies";
		
	}
	// inner class to define Veg Item record sources
	// putting them all together here allows consistent usage throughout
	public static abstract class VegcodeSources {
		public static final int REGIONAL_LIST = 0; // standard NRCS codes from the entire regional list
		public static final int PREVIOUSLY_FOUND = 1; // species previously entered, but still only standard NRCS
		public static final int PLACE_HOLDERS = 2; // user-created codes for species not known at the time
		public static final int SPECIAL_CODES = 3; // e.g. "no veg" (no vegetation on this subplot)
	}
	// inner class to define Validation levels
	// putting them all together here allows consistent usage throughout
	public static abstract class Validation {
		public static final int SILENT = 0; // usually no notice
		public static final int QUIET = 1; // usually a Toast
		public static final int CRITICAL = 2; // usually a message dialog
	}
	
	// inner classes to define tables
	public static abstract class Project implements BaseColumns {
		public static final String TABLE_NAME = "Projects";
		public static final String COLUMN_NAME_PROJCODE = "ProjCode";
		public static final int COLUMN_SIZE_PROJCODE = 10;
		public static final String COLUMN_NAME_DESCRIPTION = "DESCRIPTION";
		public static final String COLUMN_NAME_CONTEXT = "Context";
		public static final String COLUMN_NAME_CAVEATS = "Caveats";
		public static final String COLUMN_NAME_CONTACTPERSON = "ContactPerson";
		public static final String COLUMN_NAME_STARTDATE = "StartDate";
		public static final String COLUMN_NAME_ENDDATE = "EndDate";
		public static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
				+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,"
				+ COLUMN_NAME_PROJCODE + " VARCHAR(" + COLUMN_SIZE_PROJCODE + ") NOT NULL UNIQUE,"
				+ COLUMN_NAME_DESCRIPTION + " TEXT,"
				+ COLUMN_NAME_CONTEXT + " TEXT,"
				+ COLUMN_NAME_CAVEATS + " TEXT,"
				+ COLUMN_NAME_CONTACTPERSON + " TEXT,"
				+ COLUMN_NAME_STARTDATE + " DATE DEFAULT (DATETIME('now')),"
				+ COLUMN_NAME_ENDDATE + " DATE"
				+ ");";
		public static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
		public static final String SQL_ASSURE_CONTENTS = "";
		

		
	}
}
