package com.vegnab.vegnab.compatibility;

import android.os.Environment;

import java.io.File;

/**
 * Created by rshory on 6/30/2015.
 */
public class BaseAlbumDirFactory extends AlbumStorageDirFactory {

    /**
     * For pre-Froyo devices, we must provide the name of the photo directory
     * ourselves. We choose "/dcim/" as it is the widely considered to be the
     * standard storage location for digital camera files.
     */
    private static final String CAMERA_DIR = "/dcim/";

    @Override
    public File getAlbumStorageDir(String albumName) {
        return new File(Environment.getExternalStorageDirectory()
                + CAMERA_DIR + albumName);
    }
}
