package org.greenstand.android.TreeTracker.utilities;

import android.content.Context;
import android.content.ContextWrapper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TreeImage {

    public static File createImageFile(Context context) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = ValueHelper.JPEG_FILE_PREFIX + timeStamp + "_";
        File directory = context.getDir("treeImages", Context.MODE_PRIVATE);
        File imageF = File.createTempFile(imageFileName,ValueHelper.JPEG_FILE_SUFFIX, directory); // NOTE: createTempFile is just a shortcut for a unique name

        return imageF;
    }
}
