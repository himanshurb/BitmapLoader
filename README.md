BitmapLoader
============
Uses the standard android bitmap downloading instructions.

Features:

1. Implements LruCache (1/8th of runtime memory) to cache bitmaps.

2. Spans threads asynchronously off the UI thread, to load images, using single thread pool.

3. Manages bitmap memory by recycling bitmaps.

4. Best use with AbsListView (ListView, GridView etc).

5. Check sample app for usage and/or refer to insturctions below.

----------------------------------------------------------------------------------------------------------------    

Usage: 

Extend Application or init BitmapDownloader in application memory.

	public class BitmapApplication extends Application {

		public static BitmapDownloader BITMAP;
		public static Context CONTEXT;
		
		@Override
		public void onCreate() {
			super.onCreate();
			CONTEXT = getApplicationContext();
			BITMAP = new BitmapDownloader(CONTEXT);
		}
	}

In AndroidManifest.xml

	<application
	  android:name="<package path to Application file>"
	  android:largeHeap="true"
	  android:icon="@drawable/ic_launcher"
	  android:label="@string/app_name"
	  android:theme="@style/AppTheme" >
  
Anywhere in code

	BitmapDownloader.downloadImage(imageview, imageUrl, BITMAP_DESIRED_WIDTH, BITMAP_DESIRED_HEIGHT, true/false);
    
----------------------------------------------------------------------------------------------------------------    

Refer to these links:

http://developer.android.com/training/displaying-bitmaps/process-bitmap.html

http://developer.android.com/training/displaying-bitmaps/manage-memory.html

http://developer.android.com/reference/android/util/LruCache.html





