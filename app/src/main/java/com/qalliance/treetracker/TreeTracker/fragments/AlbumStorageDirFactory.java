package com.qalliance.treetracker.TreeTracker.fragments;

import java.io.File;

abstract class AlbumStorageDirFactory {
	public abstract File getAlbumStorageDir(String albumName);
}
