package com.vegnab.vegnab.compatibility;

import java.io.File;
import com.vegnab.vegnab.util;

/**
 * Created by rshory on 6/30/2015.
 */
public abstract class AlbumStorageDirFactory {

    /**
     * Returns a File object that points to the folder that will store
     * the album's pictures.
     */
    public abstract File getAlbumStorageDir(String albumName);

    /**
     * A static factory method that returns a new AlbumStorageDirFactory
     * instance based on the current device's SDK version.
     */
    public static AlbumStorageDirFactory newInstance() {
        // Note: the CompatibilityUtil class is implemented
        // and discussed in a previous post, entitled
        // "Ensuring Compatibility with a Utility Class".
        if (CompatabilityUtil.isFroyo()) {
            return new FroyoAlbumDirFactory();
        } else {
            return new BaseAlbumDirFactory();
        }
    }
}
