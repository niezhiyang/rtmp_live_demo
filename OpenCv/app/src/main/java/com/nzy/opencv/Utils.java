package com.nzy.opencv;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Utils {

    public static void copyAssets(Context context, String path) {
        File model = new File(path);
        File file = new File(context.getFilesDir(), model.getName());
        if (file.exists()) {
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            InputStream is = context.getAssets().open(path);
            int len;
            byte[] b = new byte[2048];
            while ((len = is.read(b)) != -1) {
                fos.write(b, 0, len);
            }
            fos.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
