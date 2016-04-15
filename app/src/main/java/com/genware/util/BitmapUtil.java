package com.genware.util;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapUtil {
	
	public static Bitmap convertToBitmap(String path) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        // 设置为ture只获取图片大小
        opts.inJustDecodeBounds = true;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // 返回为空
        BitmapFactory.decodeFile(path, opts);
        int width = opts.outWidth;
        int height = opts.outHeight;
        float scaleWidth = 0.f, scaleHeight = 0.f;
        if (width > 200 || height > 200) {
            // 缩放
            scaleWidth = ((float) width) / 200;
            scaleHeight = ((float) height) / 200;
        }
        opts.inJustDecodeBounds = false;
        float scale = Math.max(scaleWidth, scaleHeight);
        opts.inSampleSize = (int)scale;
        WeakReference<Bitmap> weak = new WeakReference<Bitmap>(BitmapFactory.decodeFile(path, opts));
        return Bitmap.createScaledBitmap(weak.get(), 200, 200, true);
    }

}
