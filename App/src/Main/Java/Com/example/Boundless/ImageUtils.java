package com.example.boundless;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class ImageUtils {
    private static final String TAG = "ImageUtils";

    public static String saveImageToInternalStorage(Context context, Bitmap bitmap) {
        String fileName = "trip_" + UUID.randomUUID().toString() + ".jpg";
        File directory = new File(context.getFilesDir(), "trip_images");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }

    public static String saveImageToInternalStorage(Context context, Uri uri) {
        String fileName = "trip_" + UUID.randomUUID().toString() + ".jpg";
        File directory = new File(context.getFilesDir(), "trip_images");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, fileName);
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error saving image from URI", e);
            return null;
        }
    }

    public static Bitmap loadImageFromPath(String path) {
        if (path == null || path.isEmpty()) return null;
        return BitmapFactory.decodeFile(path);
    }
}
