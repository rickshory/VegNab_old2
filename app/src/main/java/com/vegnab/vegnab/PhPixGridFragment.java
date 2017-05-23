package com.vegnab.vegnab;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.LDebug;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhPixGridFragment extends Fragment implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = PhPixGridFragment.class.getSimpleName();
    final static String ARG_PLACEHOLDER_ID = "phId";
    final static String ARG_PLACEHOLDER_CODE = "phCode";
    final static String ARG_PLACEHOLDER_DESCRIPTION = "phDescr";
    final static String ARG_PLACEHOLDER_NAMER = "phNamer";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    long mPlaceholderId = 0;
    String mPlaceholderCode, mPlaceholderDescr, mPlaceholderNamer;
    private TextView mViewPlaceholderGridHeader;
    private GridView mPhPixGridView;
    private PhPixGridAdapter mPhPixGridAdapter;
    private PhPixGridArrayAdapter mPhPixGridArrayAdapter;
    SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    Cursor mPixMatchCursor;

    private Bitmap mImageBitmap;
    private String mCurrentPhotoPath;
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    public static PhPixGridFragment newInstance(Bundle args) {
        PhPixGridFragment f = new PhPixGridFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // if the activity was re-created (e.g. from a screen rotate)
        // restore the previous parameters remembered by onSaveInstanceState()
        // This is mostly needed in fixed-pane layouts
        if (savedInstanceState != null) { // restore parameters
           if (LDebug.ON) Log.d(LOG_TAG, "In onCreateView, about to retrieve mPlaceholderId: " + mPlaceholderId);
            mPlaceholderId = savedInstanceState.getLong(ARG_PLACEHOLDER_ID, 0);
            mPlaceholderCode = savedInstanceState.getString(ARG_PLACEHOLDER_CODE);
            mPlaceholderDescr = savedInstanceState.getString(ARG_PLACEHOLDER_DESCRIPTION);
            mPlaceholderNamer = savedInstanceState.getString(ARG_PLACEHOLDER_NAMER);
        } else { // default parameters

        }
        // inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_ph_pix_grid, container, false);
        mViewPlaceholderGridHeader = (TextView) rootView.findViewById(R.id.phPixGridTitleText);
        // set click listener for the buttons in the view
        Button p = (Button) rootView.findViewById(R.id.placeholder_take_picture_button);
        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            p.setOnClickListener(this);
        } else {
            p.setVisibility(View.GONE);
        }
        // get the folder based on Namer and Placeholder
        File pixDir = getAlbumDir();
        if (LDebug.ON) Log.d(LOG_TAG, "getAlbumDir: " + pixDir.toString());
        if (pixDir.isDirectory()) {
            if (LDebug.ON) Log.d(LOG_TAG, "pixDir.isDirectory");
            File[] allFiles = pixDir.listFiles();
            if (LDebug.ON) Log.d(LOG_TAG, "allFiles.length: " + allFiles.length);
            Arrays.sort(allFiles, new Comparator<File>() {
                public int compare(File f1, File f2) {
//                return Long.compare(f1.lastModified(), f2.lastModified()); // API 19
                    return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                }
            });
            ArrayList<String> pixFilePaths = new ArrayList<>();
            int pos = 0;

            for (File file : allFiles) {
                if (LDebug.ON) Log.d(LOG_TAG, "file: " + file.toString());
                if (!file.isDirectory()) {
                    if (LDebug.ON) Log.d(LOG_TAG, "is not Directory: " + file.toString());
                    String ext = getMimeTypeFromFile(file);
                    if (LDebug.ON) Log.d(LOG_TAG, ext + " for " + file.getAbsolutePath());
                    if (ext.equals("jpeg")) {
                        pixFilePaths.add(pos, file.getAbsolutePath());
                        pos++;
                    }
                } else {
                    if (LDebug.ON) Log.d(LOG_TAG, "isDirectory: " + file.toString());
                }
            }
            if (LDebug.ON) Log.d(LOG_TAG, "pixFilePaths: " + pixFilePaths.toString());
        }

        mPhPixGridView = (GridView) rootView.findViewById(R.id.phPixGridView);
//        mPhPixGridView.setOnClickListener(this);
        //mPhPixGridAdapter = new PhPixGridAdapter(this, R.layout.grid_item_layout, getData());
        mPhPixGridAdapter = new PhPixGridAdapter(getActivity(), R.layout.grid_ph_pix, null, 0);
        mPhPixGridView.setAdapter(mPhPixGridAdapter);
        return rootView;

    }
    private String getMimeTypeFromFile(File file) {
        String extension = null;
        Uri uri = null;
        try {
            uri = Uri.fromFile(file);
        } catch (Exception e) {
            if (LDebug.ON) Log.d(LOG_TAG, "getMimeTypeFromFile; Exception: " + e.toString());
            return extension;
        }
        //Check uri format to avoid null
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(
                    getActivity().getApplicationContext().getContentResolver().getType(uri));
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters.
            //This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
        }
        return extension;
    }

    @Override
    public void onStart() {
        super.onStart();
        // during startup, check if arguments are passed to the fragment
        // this is where to do this because the layout has been applied
        // to the fragment
        Bundle args = getArguments();
        if (args != null) {
            if (mPlaceholderId == 0) {
                // On return, this method can re-run before
                // SaveInstanceState and so retain arguments originally passed when created,
                // such as mPlaceholderId=0.
                // Do not allow that zero to overwrite a new (nonzero) mPlaceholderId, or
                // it will flag to create a second copy of the same placeholder.
                mPlaceholderId = args.getLong(ARG_PLACEHOLDER_ID, 0);
//                mPlaceholderCode = args.getString(ARG_PLACEHOLDER_CODE);
            }

            // also use for special arguments like screen layout
            // start loader to get header parameters
            getLoaderManager().initLoader(Loaders.PLACEHOLDER_OF_PIX, null, this);
            // start loader to populate grid
            getLoaderManager().initLoader(Loaders.PLACEHOLDER_PIX, null, this);
//            mTxtNote.setText(args.getString(ARG_NOTE_ID));
        }

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPhPixGridView.getWindowToken(), 0);


        /*        mPhPixGridView.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mPhPixGridView.getWindowToken(), 0);
            }
        }, 50);*/
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save members needed to re-create the fragment
        outState.putLong(ARG_PLACEHOLDER_ID, mPlaceholderId);
        outState.putString(ARG_PLACEHOLDER_CODE, mPlaceholderCode);
        outState.putString(ARG_PLACEHOLDER_DESCRIPTION, mPlaceholderDescr);
        outState.putString(ARG_PLACEHOLDER_NAMER, mPlaceholderNamer);
    }

    @Override
    public void onClick(View v) {
        Bundle args= new Bundle();
        int numUpdated;
        Context c = getActivity();
        ConfigurableMsgDialog flexHlpDlg = new ConfigurableMsgDialog();
        String helpTitle, helpMessage;
        // get an Analytics event tracker
        Tracker placeholderButtonTracker = ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);

        switch (v.getId()) {

            case R.id.placeholder_take_picture_button:
               if (LDebug.ON) Log.d(LOG_TAG, "in onClick, placeholder_take_picture_button");
                placeholderButtonTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Placeholder Pictures Grid Event")
                        .setAction("Button click")
                        .setLabel("Take picture button")
                        .setValue(mPlaceholderId)
                        .build());
                if (mPlaceholderId == 0) { // record not defined yet; this should not happen
                    helpTitle = c.getResources().getString(R.string.placeholder_validate_generic_title);
                    helpMessage = c.getResources().getString(R.string.ph_pix_grid_pic_button_err);
                    flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
                    flexHlpDlg.show(getFragmentManager(), "frg_ph_pix_no_ph_code");
                } else {
                    dispatchTakePictureIntent();
                }

                break;

            default:
                Toast.makeText(this.getActivity(), "Something else clicked.", Toast.LENGTH_SHORT).show();
                break;

        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhPixGridView.setOnItemClickListener(mPixGrid_ItemClickListener);
    }

    //Item click listener for pictures grid
    final AdapterView.OnItemClickListener mPixGrid_ItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Context c = getActivity();
//            Toast.makeText(getActivity(), "Item Clicked: " + position + ", id=" + id, Toast.LENGTH_SHORT).show();
            mPixMatchCursor.moveToPosition(position);
            String path = mPixMatchCursor.getString(mPixMatchCursor.getColumnIndexOrThrow("PhotoPath"));
//            Toast.makeText(getActivity(), "" + path, Toast.LENGTH_SHORT).show();
            Uri uri = getImageContentUri(c, path);
//            Toast.makeText(getActivity(), "" + uri.toString(), Toast.LENGTH_SHORT).show();
            if (uri == null) {
                Toast.makeText(c, c.getResources().getString(R.string.ph_pix_grid_pic_no_file),
                        Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/*");
                startActivity(intent);
            }
        }
    };

    public static Uri getImageContentUri(Context context, String filePath) {
        Uri uri = null;
//        File imageFile = new File(filePath);
        try {
            uri = Uri.fromFile(new File(filePath));
        } catch (Exception e) {
            if (LDebug.ON) Log.d(LOG_TAG, "getImageContentUri; Exception: " + e.toString());
        }
/*
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID },
                MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                uri = context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }
        }
        try {
            cursor.close();
        } catch (Exception e) {
            // ignore
        }
*/
        return uri;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File f = null;
        try {
            f = setUpPhotoFile();
            mCurrentPhotoPath = f.getAbsolutePath();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        } catch (IOException e) {
            e.printStackTrace();
            f = null;
            mCurrentPhotoPath = null;
        }
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            if (mCurrentPhotoPath != null) {
                galleryAddPic();
                makeFileVisible();
                storePicturePathInDB();
                mCurrentPhotoPath = null;
            }
//            Bundle extras = data.getExtras();
//            Bitmap imageBitmap = (Bitmap) extras.get("data");
//            mTestImageView.setImageBitmap(imageBitmap);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        String[] params = null;
        switch (id) {
            case Loaders.PLACEHOLDER_OF_PIX:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT PlaceHolders.PlaceHolderCode, PlaceHolders.Description, Namers.NamerName " +
                        "FROM PlaceHolders LEFT JOIN Namers ON PlaceHolders.NamerID = Namers._id " +
                        "WHERE PlaceHolders._id = ?;";
                params = new String[] { "" + mPlaceholderId };
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, params, null);
                break;

            case Loaders.PLACEHOLDER_PIX:
                baseUri = ContentProvider_VegNab.SQL_URI;
                select = "SELECT PlaceHolderPix._id, PlaceHolderPix.PhotoPath, PlaceHolderPix.PhotoNotes " +
                        "FROM PlaceHolderPix " +
                        "WHERE PlaceHolderPix.PlaceHolderID = ? " +
                        "ORDER BY PlaceHolderPix.PhotoTimeStamp DESC;";
                params = new String[] { "" + mPlaceholderId };
               if (LDebug.ON) Log.d(LOG_TAG, "onCreateLoader, PLACEHOLDER_PIX, just before CursorLoader");
                cl = new CursorLoader(getActivity(), baseUri,
                        null, select, params, null);
               if (LDebug.ON) Log.d(LOG_TAG, "onCreateLoader, PLACEHOLDER_PIX, just after CursorLoader");
                break;
        }
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        // there will be various loaders, switch them out here
//        mRowCt = c.getCount();
        switch (loader.getId()) {

            case Loaders.PLACEHOLDER_OF_PIX:
               if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, PLACEHOLDER_OF_PIX, records: " + c.getCount());
                if (c.moveToFirst()) {
                    mPlaceholderCode = c.getString(c.getColumnIndexOrThrow("PlaceHolderCode"));
                    mPlaceholderDescr = c.getString(c.getColumnIndexOrThrow("Description"));
                    mPlaceholderNamer = c.getString(c.getColumnIndexOrThrow("NamerName"));
                    mViewPlaceholderGridHeader.setText(mPlaceholderCode + ": " + mPlaceholderDescr);
                } else { // no record to edit yet, set up new record
//                    mViewPlaceholderCode.setText(mPlaceholderCode);
                }
                break;

            case Loaders.PLACEHOLDER_PIX:
//               if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, PLACEHOLDER_PIX, just before swapCursor");
                mPhPixGridAdapter.swapCursor(c);
//               if (LDebug.ON) Log.d(LOG_TAG, "onLoadFinished, PLACEHOLDER_PIX, just before copy cursor");
                mPixMatchCursor = c;
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed. Need to make sure it is no longer is use.
        switch (loader.getId()) {

            case Loaders.PLACEHOLDER_OF_PIX:
               if (LDebug.ON) Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDER_OF_PIX.");
    //			don't need to do anything here, no cursor adapter
                break;

            case Loaders.PLACEHOLDER_PIX:
               if (LDebug.ON) Log.d(LOG_TAG, "onLoaderReset, PLACEHOLDER_PIX.");
    //			don't need to do anything here, no cursor adapter
                break;
        }
    }

    // Photo album for this Placeholder
    private String getAlbumName() {
        // PUBLIC_DB_FOLDER is e.g. "VegNab" or "VegNabAlphaTest"; same as for copies of the DB
//        return BuildConfig.PUBLIC_DB_FOLDER + "/" + mPlaceholderNamer.replace("[^a-zA-Z0-9-]", "_");
        return BuildConfig.PUBLIC_DB_FOLDER + "/" + mPlaceholderNamer + "/" + mPlaceholderCode;
    }

    private File getAlbumDir() {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            //min SDK version is Honeycomb 3.0, API level 11, so can depend on following
            storageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), getAlbumName());
//            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());
            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                       if (LDebug.ON) Log.d(LOG_TAG, "Could not create folder: " + getAlbumName());
                        return null;
                    }
                }
            }
        } else {
           if (LDebug.ON) Log.d(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }
        return storageDir;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = mPlaceholderCode.replace("[^a-zA-Z0-9-]", "_") + timeStamp + "_";
        String imageFileName = timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    private File setUpPhotoFile() throws IOException {
        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();
        return f;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        getActivity().sendBroadcast(mediaScanIntent);
    }

    private void makeFileVisible() {
        // must do following or file is not visible externally
        File f = new File(mCurrentPhotoPath);
        MediaScannerConnection.scanFile(getActivity().getApplicationContext(),
                new String[]{f.getAbsolutePath()}, null, null);
    }

    private void storePicturePathInDB() {
       if (LDebug.ON) Log.d(LOG_TAG, "savePlaceHolderPix; creating new record with mPlaceholderId = " + mPlaceholderId);
        ContentResolver rs = getActivity().getContentResolver();
        ContentValues values = new ContentValues();
        values.put("PlaceHolderID", mPlaceholderId);
        values.put("PhotoPath", mCurrentPhotoPath);
        values.put("PhotoTimeStamp", mTimeFormat.format(new Date()));
/* "PhotoNotes" VARCHAR(255),
"PhotoURL" VARCHAR(255),*/
        Uri phPixUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "placeholderpix");
        Uri phUri = rs.insert(phPixUri, values);
       if (LDebug.ON) Log.d(LOG_TAG, "new record in storePicturePathInDB; returned URI: " + phUri.toString());
        long newRecId = Long.parseLong(phUri.getLastPathSegment());
        long placeholderPixId = newRecId;
        if (newRecId < 1) { // returns -1 on error, e.g. if not valid to save because of missing required field
           if (LDebug.ON) Log.d(LOG_TAG, "new record in savePlaceHolderPix has Id == " + newRecId + "); canceled");
//            return 0;
        } else {
            getLoaderManager().restartLoader(Loaders.PLACEHOLDER_PIX, null, this);
//            Uri phNewUri = ContentUris.withAppendedId(phUri, newRecId);
//           if (LDebug.ON) Log.d(LOG_TAG, "new record in savePlaceHolderPix; URI re-parsed: " + phNewUri.toString());
//            long numUpdated = 1;
        }
    }

    private void getImageFiles(File pixDir) {

    }

    List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                inFiles.add(file);
            }
        }
        return inFiles;
    }
}
