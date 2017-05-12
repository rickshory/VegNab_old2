/**
 * 
 */
package com.vegnab.vegnab.database;

import android.graphics.Bitmap;
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
        public static final String VERIFY_VEG_ITEMS_PRESENCE = "VerifyVegItemsPresence";
        public static final String LATEST_VEG_ITEM_SAVE = "LatestVegItemSave";
        public static final String DEFAULT_IDENT_NAMER_ID = "DefaultIdentNamerId";
        public static final String DEFAULT_IDENT_REF_ID = "DefaultIdentRefId";
        public static final String DEFAULT_IDENT_METHOD_ID = "DefaultIdentMethodId";
        public static final String SPECIES_ONCE = "SpeciesOnce"; // allow spp only once per subplot; true/false
        // string for WHERE clause that defines the current local species, e.g. "%USA (%OR%)%"
        public static final String LOCAL_SPP_CRIT = "LocalSpeciesCriteria";
        public static final String USE_LOCAL_SPP = "UseLocalSpp"; // whether to use the 'Local' feature

    }
    // inner class to define loader IDs
    // putting them all together here helps avoid conflicts in various fragments
    public static abstract class Loaders {
        public static final int TEST_SQL = 0; // test loading from raw SQL

        // start value for loaderIdFactory in Application
        public static final int TABLE_MANAGER_LOADERS_STARTVALUE = 8000;

        // in Main Activity
        // All Placeholder codes defined, to check before going to management screen
        public static final int ALL_PH_CODES = 5000;
        // Placeholder codes already defined for a Project/Namer, to check before add/edit
        public static final int EXISTING_PH_CODES = 5010;
        public static final int EXISTING_SPP = 5020; // check whether species list is loaded

        // in New Visit
        public static final int PROJECTS = 1; // Loader Id for Projects
        public static final int PLOTTYPES = 2; // Loader Id for Plot Types
        public static final int PREV_VISITS = 3; // Loader ID for previous Visits
        public static final int VALID_DEL_PROJECTS = 4; // Loader Id for the list of Projects that are valid to delete
        public static final int HIDDEN_VISITS = 5; // visits that are flagged hidden and can be un-hidden
        // in dialog to use location from previous visit
        public static final int PREVIOUS_VISIT_LOCATIONS = 5; // locations from previous visits
        // in Edit Project
        public static final int EXISTING_PROJCODES = 11; // to disallow duplicates
        public static final int PROJECT_TO_EDIT = 12; //
        // in Visit Header
        public static final int VISIT_TO_EDIT = 221; // the current Visit
        public static final int EXISTING_VISITS = 222; // Visits other than the current, to check duplicates
        public static final int NAMERS = 223; // all Namers, to choose from
        public static final int LOCATIONS = 224;
        public static final int VISIT_REF_LOCATION = 225; // the reference Location for this Visit
        public static final int VISIT_PLACEHOLDERS_ENTERED = 226; // any Placeholders entered on this Visit, to allow or deny Namer change
        public static final int UPDATE_LOCAL_SPP = 227; // set the 'Local' flag for species, based on location
        public static final int UPDATE_LOCAL_NAME = 228; // store in Preferences the name of the local area, e.g. 'Oregon' for quick reference
        public static final int EXISTING_LOC_PROVIDERS = 229; // For quick lookup
        public static final int EXISTING_LOC_ACCURACY_SOURCES = 230; // For quick lookup
        public static final int VISITS_HAVING_LOC_AVAILABLE = 231; // Visits, other than the current one, user can copy the location from

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
        public static final int EXISTING_PH_CODES_PRECHECK = 72; // Placeholder codes already defined by this Namer, checked before add/edit
        public static final int VISIT_INFO = 73; // Fields needed from the Visit

        // in Edit VegItem
        public static final int VEGITEM_TO_EDIT = 81; // The current veg item
        public static final int VEG_ITEM_CONFIDENCE_LEVELS = 82; //
        public static final int VEG_ITEM_DUP_CODES = 83; // codes duplicated on the subplot?
        public static final int VEGITEM_DETAILS = 84; // genus, species, etc. fields of item being edited

        // in Fix Spellings
        public static final int SPELL_ITEMS = 141; // Items available to edit the spelling of

        // in Manage Placeholders
        public static final int PHS_MATCHES = 131; // Placeholders that match any search string
        public static final int PHS_NAMERS = 132; // all Namers, to choose from

        // in Edit Placeholder
        public static final int PLACEHOLDER_TO_EDIT = 91; // The current placeholder
        public static final int PLACEHOLDERS_EXISTING = 92; // Placeholder codes already defined by this Namer
        public static final int PLACEHOLDER_HABITATS = 93; // Recall these as options to re-select
        public static final int PH_IDENT_NAMERS = 94; // For spinner
        public static final int PH_IDENT_REFS = 95; // For spinner
        public static final int PH_IDENT_METHODS = 96; // For spinner
        public static final int PH_IDENT_CONFIDENCS = 97; // For spinner
        public static final int PH_IDENT_SPECIES = 98; // For auto complete
        public static final int PLACEHOLDER_USAGE = 99; // Usage of this placeholder in the data, to allow/disallow edit

        // in Placeholder Pictures Grid
        public static final int PLACEHOLDER_OF_PIX = 110; // The current placeholder
        public static final int PLACEHOLDER_PIX = 111; // Pictures for the current placeholder

        // in Configurable Edit Dialog
        public static final int ITEMS = 120; // all Items, to choose from
        public static final int ITEM_TO_EDIT = 121;
        public static final int EXISTING_ITEMS = 122; // Items other than the current, to check duplicates


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
        public static final String MANAGE_PLACEHOLDERS = "ManagePlaceholders";
        public static final String MANAGE_SPELLINGS = "ManageSpellings";
        public static final String CLOUD_STORAGE = "CloudStorage";
        public static final String EDIT_PLACEHOLDER = "EditPlaceholder";
        public static final String PLACEHOLDER_PIX_GRID = "PlaceholderPixGrid";
        public static final String PLACEHOLDER_PIC_DETAIL = "PlaceholderPicDetail";

    }
    // inner class to define Veg Item record sources
    // putting them all together here allows consistent usage throughout
    public static abstract class VegcodeSources {
        public static final int REGIONAL_LIST = 0; // standard NRCS codes from the regional list, first time entered
        public static final int PREVIOUSLY_FOUND = 1; // NRCS code, species previously entered
        public static final int PLACE_HOLDERS = 2; // user-created codes for species not known at the time
        public static final int SPECIAL_CODES = 3; // e.g. "no veg" (no vegetation on this subplot)
    }

    // inner class to define Placeholder screen actions
    // putting them all together here allows consistent usage throughout
    public static abstract class PhActions {
        public static final int GO_TO_PICTURES = 0; // go to the show/take/edit photos screen
    }

    // inner class to define Validation levels
    // putting them all together here allows consistent usage throughout
    public static abstract class Validation {
        public static final int SILENT = 0; // usually no notice
        public static final int QUIET = 1; // usually a Toast
        public static final int CRITICAL = 2; // usually a message dialog
    }

    // inner class to define regular expressions
    // putting them all together here allows consistent usage throughout
    public static abstract class VNRegex {
        public static final String NRCS_CODE = "[a-zA-Z]{3,5}[0-9]*|2[a-zA-Z]{1,4}";
        // defines what an NRCS code can look like, mostly for disallowing Placeholders being like that
        // disallow 3 to 5 letters, alone or followed by numbers
        // disallow any with numerals trailing 3-5 letters, though never saw real codes with more than 2 digits here
        // also disallow codes like "2FDA" (forb dicot annual) some agencies use for general ids
    }

    public static abstract class VNConstraints {
        public static final int PLACEHOLDER_MAX_LENGTH = 10; // maximum allowed length for a Placeholder code
    }

    public static abstract class VNPermissions {
        public static final int REQUEST_ACCESS_FINE_LOCATION = 10; //

        public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000; //
    }

    public static abstract class LDebug {
        public static final boolean ON = true; // true or false to conditionally compile Log statements
    }

    // class for use in grid view
    public class VNGridImageItem {
        private Bitmap image;
        private String title;
        private String path;

        public VNGridImageItem(Bitmap image, String title, String path) {
            super();
            this.image = image;
            this.title = title;
            this.path = path;
        }
        public Bitmap getImage() {
            return image;
        }
        public void setImage(Bitmap image) {
            this.image = image;
        }
        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
        public String getPath() {
            return path;
        }
        public void setPath(String path) {
            this.path = path;
        }
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
