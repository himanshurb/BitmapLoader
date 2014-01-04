package com.app.hbansal.bitmaploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.WindowManager;
import android.widget.ImageView;

public class BitmapDownloader {
	static HashMap<WorkerModel, BitmapWorkerTask> taskList;
	static ExecutorService ASYNC_EXECUTOR;
	static FileCache mFileCache;
	static BitmapLruCache mLruCache;
	static final int POOL_SIZE = 128;
	static final int WEB_TIMEOUT = 60000;
	
	static int DEVICE_DENSITY_DPI;
	static int mSelectorColor;
	static SparseArray<ShapeDrawable> rectSelectorDrawableArray;
	static SparseArray<ShapeDrawable> rectPlaceholderDrawableArray;
	
	public BitmapDownloader(Context context) {
		mFileCache = new FileCache(context);
		ASYNC_EXECUTOR = Executors.newSingleThreadExecutor();
		taskList = new HashMap<BitmapDownloader.WorkerModel, BitmapWorkerTask>();
		mLruCache = new BitmapLruCache();
		
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(metrics);
		DEVICE_DENSITY_DPI = metrics.densityDpi;
		
		mSelectorColor = context.getResources().getColor(R.color.selector_black);
		rectSelectorDrawableArray = new SparseArray<ShapeDrawable>();
		rectPlaceholderDrawableArray = new SparseArray<ShapeDrawable>();
	}
	
	public static void downloadImage(ImageView iv, String url, int width, int height, boolean useLruCache) {
		if(taskList.size() >= POOL_SIZE) return;
		recycleOldBitmap(iv);
		
		iv.setImageDrawable(getPlaceHolder(width, height));
		
		iv.setTag(R.id.url, url);
		iv.setTag(R.id.width, width);
		iv.setTag(R.id.height, height);
		iv.setTag(R.id.use_lru_cache, useLruCache ? 1 : 0);
		
		WorkerModel model = new WorkerModel();
		model.iv = iv;
		model.url = url;
		model.width = width;
		model.height = height;
		model.useLruCache = useLruCache ? 1 : 0;
		
		BitmapWorkerTask task = new BitmapWorkerTask(model);
		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(ASYNC_EXECUTOR);
		} else {
			task.execute();
		}
		taskList.put(model, task);
	}
	
	private static class BitmapWorkerTask extends AsyncTask<Void, Void, Bitmap> {
		private WorkerModel mWorkerModel;
		
		public BitmapWorkerTask(WorkerModel model) {
			mWorkerModel = model;
		}
		
		@Override
		protected Bitmap doInBackground(Void... params) {
			Bitmap bitmap = null;
			Object object = mWorkerModel.iv.getTag(R.id.url);
			String url = (object instanceof String) ? (String) object : null;
			
			if(isUrlInView(url, mWorkerModel.url)) {
				bitmap = getBitmap(mWorkerModel);
				if(bitmap == null) return null;
				
				if(isUrlInView(url, mWorkerModel.url)) {
					return bitmap;	
					
				} else {
					bitmap.recycle();
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);
			taskList.remove(mWorkerModel);
			if(bitmap == null) return;
			
			int useLruCache = mWorkerModel.useLruCache;
			
			if(useLruCache == 1) {
				String url = (String) mWorkerModel.iv.getTag(R.id.url);
				mLruCache.addBitmapToMemoryCache(url, bitmap);
			}
			setDrawable(mWorkerModel.iv, bitmap);
		}
		
		private boolean isUrlInView(String tagUrl, String url) {
			if(TextUtils.isEmpty(url) || TextUtils.isEmpty(tagUrl)) return false;
			
			if(tagUrl.equalsIgnoreCase(url)) {
				return true;
				
			} else {
				return false;
			}
		}
	}
	
	private static void setDrawable(ImageView iv, Bitmap bitmap) {
		int width = (Integer) iv.getTag(R.id.width);
		int height = (Integer) iv.getTag(R.id.height);
		StateListDrawable drawable;
		LayerDrawable layerDrawable;
		
		if(width > 0){
			drawable = new StateListDrawable();
			
			@SuppressWarnings("deprecation")
			BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
			bitmapDrawable.setTargetDensity(DEVICE_DENSITY_DPI);
			
			Drawable[] drawableArray = new Drawable[2];
			drawableArray[0] = bitmapDrawable;
			drawableArray[1] = getSelector(width, height);
			
			layerDrawable = new LayerDrawable(drawableArray);
			
			int[] state1 = {};
			int[] state2 = {-android.R.attr.state_pressed, -android.R.attr.state_focused};
			drawable.addState(state2, bitmapDrawable);
			drawable.addState(state1, layerDrawable);
			
			iv.setImageDrawable(drawable);
		}
	}
	
	private static Bitmap getBitmap(WorkerModel model) {
		Bitmap bitmap = null;

		File f = mFileCache.getFile(model.url);
		try {
			bitmap = createScaledBitmap(f, model.width, model.height);
			if(bitmap != null) return bitmap;
			
		} catch(Exception e) {
		}

		try {
			URL imageUrl = new URL(model.url);
			
			HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
			conn.setConnectTimeout(WEB_TIMEOUT);
			conn.setReadTimeout(WEB_TIMEOUT);
			conn.setInstanceFollowRedirects(true);
			
			InputStream is = conn.getInputStream();
			mFileCache.saveFile(is, f);

			bitmap = createScaledBitmap(f, model.width, model.height);
			return bitmap;
			
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	private static Bitmap createScaledBitmap(File file, int newWidth, int newHeight) {
		Bitmap bitmap = decodeFile(file);

		if(bitmap == null) return null;
		
		int imageHeight = bitmap.getHeight();
		int imageWidth = bitmap.getWidth();
		
		if (newWidth <= 0 || ((imageWidth == newWidth) && (imageHeight == newHeight)) || bitmap == null) {
			return bitmap;
			
		} else{
			if(newHeight <= 0) newHeight = (int) (newWidth * imageHeight / imageWidth);
			Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
			bitmap.recycle();
			return scaledBitmap;
		}
	}
	
	private static Bitmap decodeFile(File f) {
		FileInputStream input = null;
		try {
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inPurgeable = true;
			input = new FileInputStream(f);
			return BitmapFactory.decodeStream(input, null, o);
		} catch (FileNotFoundException e) {
		}finally{
			try {
				if(input != null) input.close();
			} catch (IOException e) {
			}
		}
		return null;
	}

	private static class WorkerModel {
		public ImageView iv;
		public String url;
		public int width;
		public int height;
		public int useLruCache;
	}
	
	private static void recycleOldBitmap(ImageView iv) {
		Drawable drawable = iv.getDrawable();
		
		if(drawable != null && drawable instanceof StateListDrawable) {
			StateListDrawable stateDrawable = (StateListDrawable) drawable;
			if(stateDrawable.getCurrent() instanceof BitmapDrawable) {
				BitmapDrawable bitmapDrawable = (BitmapDrawable) stateDrawable.getCurrent();
				if(bitmapDrawable == null) return;
				Bitmap bitmap = bitmapDrawable.getBitmap();
				if(bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
			}
		}
	}
	
	private static ShapeDrawable getPlaceHolder(int width, int height) {
		ShapeDrawable sd = rectPlaceholderDrawableArray.get(width); 
		if(sd == null) {
			sd = new ShapeDrawable(new RectShape());
			sd.setIntrinsicHeight(height);
			sd.setIntrinsicWidth(width);
			sd.getPaint().setColor(Color.WHITE);
			rectPlaceholderDrawableArray.put(width, sd);
		}
		return sd;
	}
	
	private static ShapeDrawable getSelector(int width, int height) {
		ShapeDrawable sd = rectSelectorDrawableArray.get(width); 
		if(sd == null) {
			sd = new ShapeDrawable(new RectShape());
			sd.setIntrinsicHeight(height);
			sd.setIntrinsicWidth(width);
			sd.getPaint().setColor(mSelectorColor);
			rectPlaceholderDrawableArray.put(width, sd);
		}
		return sd;
	}
}