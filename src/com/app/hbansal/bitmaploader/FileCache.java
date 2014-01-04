package com.app.hbansal.bitmaploader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.os.Environment;
/**
 *
 */
public class FileCache {
    
    File cacheDir;
    final String ADDR_SDCARD = "/Android/data/com.app.hbansal.bitmaploader/.Bitmaps";
    
    public FileCache(Context context){
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            cacheDir=new File(Environment.getExternalStorageDirectory(), ADDR_SDCARD);
        else
            cacheDir=context.getCacheDir();
        if(!cacheDir.exists())
            cacheDir.mkdirs();
    }
    
    public File getFile(String url){
        String filename=String.valueOf(url.hashCode());
        File f = new File(cacheDir, filename);
        return f;
    }
    
    public void saveFile(InputStream is, File file){
		try {
			OutputStream os = new FileOutputStream(file);
			copyStream(is, os);
			os.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
    }
    
    public void clear(){
        File[] files=cacheDir.listFiles();
        if(files==null)
            return;
        for(File f:files)
            f.delete();
    }
    
    private void copyStream(InputStream is, OutputStream os) {
		final int buffer_size = 1024;
		try {
			byte[] bytes = new byte[buffer_size];
			for (;;) {
				int count = is.read(bytes, 0, buffer_size);
				if (count == -1)
					break;
				os.write(bytes, 0, count);
			}
		} catch (Exception ex) {
		}
	}
}