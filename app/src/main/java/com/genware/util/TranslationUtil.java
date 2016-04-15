package com.genware.util;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Base64;

public class TranslationUtil {
	
	/**  
     * 图片转成string  
     *   
     * @param bitmap  
     * @return  
     */  
    public static String convertIcon2String(Bitmap bitmap)  
    {  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();// outputstream  
        bitmap.compress(CompressFormat.PNG, 100, baos);  
        byte[] appicon = baos.toByteArray();// 转为byte数组  
        return Base64.encodeToString(appicon, Base64.DEFAULT);  
  
    }  
  
    /**  
     * string转成bitmap  
     *   
     * @param st  
     */  
    public static Bitmap convertString2Icon(String st)  
    {  
        // OutputStream out;  
        Bitmap bitmap = null;  
        try  
        {  
            // out = new FileOutputStream("/sdcard/aa.jpg");  
            byte[] bitmapArray;  
            bitmapArray = Base64.decode(st, Base64.DEFAULT);  
            bitmap =  
                    BitmapFactory.decodeByteArray(bitmapArray, 0,  
                            bitmapArray.length);  
            // bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);  
            return bitmap;  
        }  
        catch (Exception e)  
        {  
            return null;  
        }  
    } 

}
