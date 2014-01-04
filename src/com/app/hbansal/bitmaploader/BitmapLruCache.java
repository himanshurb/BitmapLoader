package com.app.hbansal.bitmaploader;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class BitmapLruCache {
	LruCache<String, Bitmap> mLruCache;
	
	final int MAX_MEMORY = (int) (Runtime.getRuntime().maxMemory() / 1024);
	final int cacheSize = MAX_MEMORY / 8;
	
	public BitmapLruCache() {
		mLruCache = new LruCache<String, Bitmap>(cacheSize) {
	        @Override
	        protected int sizeOf(String key, Bitmap bitmap) {
	            //The cache size will be measured in kilobytes rather than number of items.
	            return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
	        }
	        
	        @Override
	        protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
	        	super.entryRemoved(evicted, key, oldValue, newValue);
	        	/*if(evicted) {
        			if(oldValue != null && !oldValue.isRecycled() && !isBitmapInView(key)) {
        				//FabUtils.log("bitmap", "Entry recycled: " + key);
            			oldValue.recycle();
            			removeFromMap(key);
            			
        			} else {
        				//FabUtils.log("bitmap", "Entry removed: " + key);		
        			}
	        	}*/
	        	//System.gc();
	        }
	    };
	}
	
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
	    if (getBitmapFromMemCache(key) == null) {
	    	mLruCache.put(key, bitmap);
	    }
	}

	public Bitmap getBitmapFromMemCache(String key) {
		Bitmap bitmap = mLruCache.get(key);
		if(bitmap != null && !bitmap.isRecycled()) {
			return bitmap;
		}
		return null;
	}
	
	public List<String> recycleMemCachedBitmaps() {
		ArrayList<String> recycledUrls = new ArrayList<String>();
		/*Map<String, Bitmap> lruSnapshot = mLruCache.snapshot();
		Set<String> memCachedUrls = lruSnapshot.keySet();
		Bitmap bitmap;
		for(String url : memCachedUrls) {
			bitmap = lruSnapshot.get(url);
			
			if(bitmap != null && !bitmap.isRecycled()) {
				recycledUrls.add(url);
				bitmap.recycle();
			}
		}*/
		mLruCache.evictAll();
		return recycledUrls;
	}
	
	/*private float getLruSizeInMB() {
		long byteSize = 0;
		Map<String, Bitmap> lruSnapshot = mLruCache.snapshot();
		Set<String> memCachedUrls = lruSnapshot.keySet();
		Bitmap bitmap;
		for(String url : memCachedUrls) {
			bitmap = lruSnapshot.get(url);
			
			if(bitmap != null && !bitmap.isRecycled()) {
				byteSize += (bitmap.getRowBytes() * bitmap.getHeight());
				
			} else {
				mLruCache.remove(url);
			}
		}
		return (byteSize / 1024 / 1024);
	}*/
}	