package com.v5kf.client.lib.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import com.v5kf.client.lib.Logger;
import com.v5kf.client.lib.V5Util;

public class ImageLoader {

	private MemoryCache memoryCache;
	private FileCache fileCache;
	private static Map<ImageView, String> imageViews = Collections
			.synchronizedMap(new WeakHashMap<ImageView, String>());
	private ExecutorService executorService;
	private boolean isSrc;	// src 还是 background
	
	private int mDefaultImg; // 加载中显示图片
	private int mFailureImag; // 加载失败显示图片
	
//	private Context mContext;
	private ImageLoaderListener mListener;
	private String mUrl;
	private ImageView mImageView;
	
	public interface ImageLoaderListener {
		public void onSuccess(String url, ImageView imageView);
		public void onFailure(ImageLoader imageLoader, String url, ImageView imageView);
	}

	/**
	 * @param context
	 *            上下文对象
	 * @param flag
	 *            true为source资源，false为background资源
	 */
	public ImageLoader(Context context, boolean srcFlag, int defaultImg, ImageLoaderListener listener) {
		if (mFailureImag == 0) {
			mFailureImag = UIUtil.getIdByName(context, "drawable", "v5_img_src_error");
		}
		memoryCache = new MemoryCache();
		fileCache = new FileCache(context, FileUtil.getImageCachePath(context));
		executorService = Executors.newFixedThreadPool(5);
		isSrc = srcFlag;
		this.mDefaultImg = defaultImg;
//		this.mContext = context;
		this.mListener = listener;
	}
	
	public ImageLoader(Context context, boolean srcFlag, int defaultImg) {
		this(context, srcFlag, defaultImg, null);
	}

	public void DisplayImage(String url, ImageView imageView) {
		if (null == imageView) {
			return;
		}
		mUrl = url;
		mImageView = imageView;
		
		if (null == url || url.isEmpty()) {
			if (isSrc)
				imageView.setImageResource(mDefaultImg);
			else
				imageView.setBackgroundResource(mDefaultImg);
			return;
		}
		// 取消url编码
//		String u1 = url.substring(0, url.lastIndexOf("/") + 1);
//		String u2 = url.substring(url.lastIndexOf("/") + 1);
//		try {
//			u2 = URLEncoder.encode(u2, "UTF-8");
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//		url = u1 + u2;
		imageViews.put(imageView, url);
		Bitmap bitmap = memoryCache.get(url);
		if (bitmap != null) {
			Logger.v("ImageLoader", "From MemoryCache:" + url);
			if (isSrc)
				imageView.setImageBitmap(bitmap);
			else
				imageView.setBackgroundDrawable(new BitmapDrawable(bitmap));
			
			if (mListener != null) {
				mListener.onSuccess(url, imageView);
			}
		} else {
			queuePhoto(url, imageView);
			if (isSrc)
				imageView.setImageResource(mDefaultImg);
			else
				imageView.setBackgroundResource(mDefaultImg);
		}
	}

	private void queuePhoto(String url, ImageView imageView) {
		PhotoToLoad p = new PhotoToLoad(url, imageView);
		executorService.submit(new PhotosLoader(p));
	}

	public Bitmap getBitmap(String url) {
		try {
			File f = fileCache.getFile(url);
			// 从sd卡
			Bitmap b = onDecodeFile(f);
			if (b != null) {
				Logger.v("ImageLoader", "From FileCache:" + url);
				return b;
			} else { // 判断是否本地路径
				Bitmap localBmp = V5Util.ratio(
						url, 
						UIUtil.MAX_PIC_W * 2 / 3, 
						UIUtil.MAX_PIC_H * 2 / 3); // 压缩宽高
				if (localBmp != null) {
					Logger.v("ImageLoader", "From localFile:" + url);
					return localBmp;
				}
			}
			
			// 从网络
			Bitmap bitmap = null;
			Logger.d("ImageLoader", "ImageLoader-->download:" + url);
			HttpUtil.CopyStream(url, f);
			
			// 图片角度矫正
			UIUtil.correctBitmapAngle(f.getAbsolutePath());
			
			bitmap = onDecodeFile(f);
			return bitmap;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public Bitmap onDecodeFile(File f) {
		try {
			return BitmapFactory.decodeStream(new FileInputStream(f));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 解码图像用来减少内存消耗
	 * 
	 * @param f
	 * @return
	 */
	public Bitmap decodeFile(File f) {
		try {
			// 解码图像大小
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);
			// 找到正确的刻度值，它应该是2的幂。
			final int REQUIRED_SIZE = 70;
			int width_tmp = o.outWidth, height_tmp = o.outHeight;
			int scale = 1;
			while (true) {
				if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
			}
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	/**
	 * 任务队列
	 * 
	 * @author Scorpio.Liu
	 * 
	 */
	private class PhotoToLoad {
		public String url;
		public ImageView imageView;

		public PhotoToLoad(String u, ImageView i) {
			url = u;
			imageView = i;
		}
	}

	class PhotosLoader implements Runnable {
		PhotoToLoad photoToLoad;

		PhotosLoader(PhotoToLoad photoToLoad) {
			this.photoToLoad = photoToLoad;
		}

		@Override
		public void run() {
			if (imageViewReused(photoToLoad))
				return;
			Bitmap bmp = getBitmap(photoToLoad.url);
			if (bmp == null) {
				Logger.e("ImageLoader", "getBitmap --> null");
			}
			memoryCache.put(photoToLoad.url, bmp);
			Logger.d("ImageLoader", "memoryCache put:" + photoToLoad.url);
			if (imageViewReused(photoToLoad))
				return;
			BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad);
			Activity a = (Activity) photoToLoad.imageView.getContext();
			a.runOnUiThread(bd);
		}
	}

	boolean imageViewReused(PhotoToLoad photoToLoad) {
		String tag = imageViews.get(photoToLoad.imageView);
		if (tag == null || !tag.equals(photoToLoad.url))
			return true;
		return false;
	}

	/**
	 * 显示位图在UI线程
	 * 
	 * @author Scorpio.Liu
	 * 
	 */
	class BitmapDisplayer implements Runnable {
		Bitmap bitmap;
		PhotoToLoad photoToLoad;

		public BitmapDisplayer(Bitmap b, PhotoToLoad p) {
			bitmap = b;
			photoToLoad = p;
		}

		public void run() {
			if (imageViewReused(photoToLoad))
				return;
			if (bitmap != null) {
				if (isSrc)
					photoToLoad.imageView.setImageBitmap(bitmap);
				else
					photoToLoad.imageView.setBackgroundDrawable(new BitmapDrawable(bitmap));
				
				if (mListener != null) {
					mListener.onSuccess(mUrl, mImageView);
				}
			} else { // 获取图片失败
				if (isSrc)
					photoToLoad.imageView.setImageResource(mFailureImag);
				else
					photoToLoad.imageView.setBackgroundResource(mFailureImag);
				
				if (mListener != null) {
					mListener.onFailure(ImageLoader.this, mUrl, mImageView);
				}
			}
		}
	}

	/**
	 * 在适当的时机清理图片缓存
	 */
	public void clearCache() {
		memoryCache.clear();
		fileCache.clear();
	}

	/**
	 * 在适当的时机清理内存图片缓存
	 */
	public void clearMemoryCache() {
		memoryCache.clear();
	}
	
	/**
	 * 缓存Bitmap图片，id可为url
	 * @param bmp
	 * @param id
	 * @throws IOException
	 */
	public void saveImage(Bitmap bmp, String id) throws IOException {
		memoryCache.put(id, bmp);
		File f = fileCache.getFile(id);
		Bitmap b = onDecodeFile(f);
		if (b != null) {
			Logger.d("ImageLoader", "[saveImage] Already in FileCache:" + id);
			return;
		}
		try {
			FileOutputStream out = new FileOutputStream(f);
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
		    out.flush();
		    out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

}
