package com.v5kf.client.ui;

import java.io.File;
import java.io.FileOutputStream;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.v5kf.client.lib.Logger;
import com.v5kf.client.lib.V5ClientAgent;
import com.v5kf.client.lib.V5Util;
import com.v5kf.client.ui.utils.FileUtil;
import com.v5kf.client.ui.utils.ImageLoader;
import com.v5kf.client.ui.utils.ImageLoader.ImageLoaderListener;
import com.v5kf.client.ui.utils.UIUtil;
import com.v5kf.client.ui.widget.AlertDialog;
import com.v5kf.client.ui.widget.PhotoView;
import com.v5kf.client.ui.widget.PhotoViewAttacher.OnPhotoTapListener;
import com.v5kf.client.ui.widget.PhotoViewAttacher.OnViewTapListener;

public class ShowImageActivity extends Activity implements ImageLoaderListener {

	private String mUrl;
	private PhotoView mPhotoIv;
	private ProgressBar mProgress;
	private ImageLoader mImgLoader;
	
	private Handler mHandler;
	private String mFileName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//getSupportActionBar().hide(); // 隐藏ActionBar
		setContentView(UIUtil.getIdByName(this, "layout", "v5_activity_show_image"));
		handleIntent();
		initView();
		initHandler();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		V5ClientAgent.getInstance().onStart();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		V5ClientAgent.getInstance().onStop();
	}
	
	private void initHandler() {
		mHandler = new Handler() {
			
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == 1) {
					Toast.makeText(ShowImageActivity.this, 
							String.format(
									getString(UIUtil.getIdByName(
											ShowImageActivity.this, 
											"string", 
											"v5_on_image_saveed")), 
									mFileName), 
							Toast.LENGTH_SHORT)
							.show();
				}
			}
		};
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void initView() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().setStatusBarColor(0x00000000);
		}
		
		initTitleBar();
		mPhotoIv = (PhotoView) findViewById(UIUtil.getIdByName(this, "id", "id_image"));
		mProgress = (ProgressBar) findViewById(UIUtil.getIdByName(this, "id", "id_loading_progressbar"));
		if (mProgress != null) {
			mProgress.setVisibility(View.VISIBLE);
		}
		
		mPhotoIv.setOnPhotoTapListener(new OnPhotoTapListener() {
			
			@Override
			public void onPhotoTap(View view, float x, float y) {
				Logger.d("ShowImageActivity", "[onPhotoTap]");
				finish();
//				int v = getWindow().getAttributes().flags;  
//		        // 全屏 66816 - 非全屏 65792
//				if(v != WindowManager.LayoutParams.FLAG_FULLSCREEN) { //非全屏
//		        	getWindow().setFlags(
//	                    WindowManager.LayoutParams.FLAG_FULLSCREEN,  
//	                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
//		        } else { //取消全屏
//		            getWindow().clearFlags(  
//		            	WindowManager.LayoutParams.FLAG_FULLSCREEN);  
//		        }
			}
		});
		
		mPhotoIv.setOnViewTapListener(new OnViewTapListener() {
			
			@Override
			public void onViewTap(View view, float x, float y) {
				Logger.d("ShowImageActivity", "[onViewTap]");
				finish();
			}
		});
		
		mPhotoIv.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				Logger.d("ShowImageActivity", "[onLongClick]");
				new AlertDialog(ShowImageActivity.this).builder()
				.setTitle(UIUtil.getIdByName(getApplicationContext(), "string", "v5_tips"))
				.setMsg(UIUtil.getIdByName(ShowImageActivity.this, "string", "v5_save_image"))
				.setCancelable(false)
				.setPositiveButton(0, new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						saveImage();
					}
				})
				.setNegativeButton(0, null)
				.show();
				return true;
			}
		});
		
		mImgLoader = new ImageLoader(this, true, UIUtil.getIdByName(this, "drawable", "v5_img_src_loading"), this);
		mImgLoader.DisplayImage(mUrl, mPhotoIv);
	}

	private void initTitleBar() {
		TextView titleTv = (TextView)findViewById(UIUtil.getIdByName(this, "id", "header_htv_subtitle"));
		titleTv.setText(UIUtil.getIdByName(this, "string", "v5_image_viewer"));
		findViewById(UIUtil.getIdByName(this, "id", "header_layout_leftview_container")).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		Button rightBtn = (Button) findViewById(UIUtil.getIdByName(this, "id", "header_right_btn"));
		rightBtn.setBackgroundResource(UIUtil.getIdByName(this, "drawable", "v5_action_bar_save"));
		rightBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				saveImage();
			}
		});
	}
	
	protected void saveImage() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				Bitmap saveBmp = null;
				if (mImgLoader != null) {
					saveBmp = mImgLoader.getBitmap(mUrl);
				}
				if (saveBmp == null) {
					mPhotoIv.setDrawingCacheEnabled(true);
					saveBmp = Bitmap.createBitmap(mPhotoIv.getDrawingCache());
					mPhotoIv.setDrawingCacheEnabled(false);
				}
				if (saveBmp != null) {
					mFileName = saveBitmap2File(saveBmp);
					mHandler.obtainMessage(1).sendToTarget();
				}
			}
		}).start();
	}

	public String saveBitmap2File(Bitmap bitmap) {
        String filepath = null;
        // 图片存储路径
        String savePath = FileUtil.getImageSavePath(this);
        // 保存Bitmap
        try {
            File path = new File(savePath);
            // 文件
            String fileName = FileUtil.getImageName();
            filepath = savePath + "/" + fileName;
            File file = new File(filepath);
            if (!path.exists()) {
            	path.mkdirs();
            }
            if (!file.exists()) {
            	file.createNewFile();
            }
            FileOutputStream fos = null;
            fos = new FileOutputStream(file);
            if (null != fos) {
            	bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            	fos.flush();
            	fos.close();
            }
            Logger.d("ShowImageActivity", "SaveFileSize>>>:" + V5Util.getFileSize(new File(filepath)));
            
            // 通知系统刷新相册，否则点击相册后，找不到该文件，除非mount SD卡
            Uri localUri = Uri.fromFile(file);
            Intent localIntent = new Intent(
            		Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri);
            sendBroadcast(localIntent);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return filepath;
	}
	
	private void handleIntent() {
		Intent intent = getIntent();
		mUrl = intent.getStringExtra("pic_url");
		if (null == mUrl || mUrl.isEmpty()) {
			Logger.w("ShowImageActivity", "Got null pic_url.");
			finish();
			return;
		}
	}

	@Override
	public void onSuccess(String url, ImageView imageView) {
		if (mProgress != null) {
			mProgress.setVisibility(View.GONE);
		}
	}

	@Override
	public void onFailure(ImageLoader imageLoader, String url,
			ImageView imageView) {
		if (mProgress != null) {
			mProgress.setVisibility(View.GONE);
		}
	}
}
