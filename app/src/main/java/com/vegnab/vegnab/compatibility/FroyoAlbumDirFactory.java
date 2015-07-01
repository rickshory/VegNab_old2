package com.vegnab.vegnab.compatibility;

import android.os.Environment;

import java.io.File;

/**
 * Created by rshory on 6/30/2015.
 */
public class FroyoAlbumDirFactory extends AlbumStorageDirFactory {

    @Override
    public File getAlbumStorageDir(String albumName) {
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
    }
}
