package com.v5kf.client.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.BitmapDescriptorFactory;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.mapsdk.raster.model.Marker;
import com.tencent.mapsdk.raster.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.Overlay;
import com.tencent.tencentmap.mapsdk.map.Projection;
import com.tencent.tencentmap.mapsdk.map.TencentMap.OnMapClickListener;
import com.tencent.tencentmap.mapsdk.map.TencentMap.OnMarkerDraggedListener;
import com.v5kf.client.lib.Logger;
import com.v5kf.client.lib.V5ClientAgent;
import com.v5kf.client.ui.utils.LocationBean;
import com.v5kf.client.ui.utils.UIUtil;

public class LocationMapActivity extends Activity implements TencentLocationListener, 
		OnMarkerDraggedListener {
	private static final String TAG = "LocationMapActivity";
	public static final int RESULT_CODE_GET_LOCATION = 1;
	public static final int RESULT_CODE_NULL = 0;
	private static int MAP_SHOW = 1;
	private static int MAP_SEND = 2;
	
	/* Title Action Bar */
	private LinearLayout mLeftLayout;
	private Button mRightSendBtn;
	private TextView mTitleTv;

	private TextView mLocationDescTv;
	
	private MapView mMapView;
	private TencentLocation mLocation;
	private TencentLocationRequest mLocationRequest;
	private TencentLocationManager mLocationManager;
	private Marker mMarkerFix;
	private LocationBean mLocationBean;	
	private double mX;
	private double mY;
	private int mMapType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//getSupportActionBar().hide(); // 隐藏ActionBar
		setContentView(UIUtil.getIdByName(this, "layout", "v5_activity_location_map"));
		handleIntent();
		findView();
		initView();
		mMapView.onCreate(savedInstanceState);
		initLocation();
	}
	
	private void handleIntent() {
		Intent intent = getIntent();
		mX = intent.getDoubleExtra("x", 0);
		mY = intent.getDoubleExtra("y", 0);
		if (mX == 0 && mY == 0) {
			mMapType = MAP_SEND;
		} else {
			mMapType = MAP_SHOW;
		}
	}

	private void initLocation() {
		if (mMapType == MAP_SHOW) { // 显示标注和名称
			LatLng ll = new LatLng(mX, mY);
			mMapView.getMap().setZoom(15);
			mMapView.getMap().animateTo(ll);
			mMarkerFix = mMapView.getMap().addMarker(new MarkerOptions()
				.position(ll)
				.title("")
				.anchor(0.5f, 1.0f)
				.icon(BitmapDescriptorFactory
				        .defaultMarker())
				        .draggable(false));
			mMapView.invalidate();
			UIUtil.getLocationTitle(this, mX, mY, mLocationDescTv, mMarkerFix);
		} else {
			if (null == mLocationRequest) {
				mLocationRequest = TencentLocationRequest.create();
			}
			if (null == mLocationManager) {
				mLocationManager = TencentLocationManager.getInstance(this);
			}
			mLocationRequest.setInterval(5000);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		V5ClientAgent.getInstance().onStart();
	}

	@Override
    protected void onDestroy() {
		mMapView.onDestroy();
        super.onDestroy();
    }
 
    @Override
    protected void onPause() {
    	mMapView.onPause();
        super.onPause();
        if (mMapType == MAP_SEND) {
        	stopLocation();
        }
    }
 
    @Override
    protected void onResume() {
    	mMapView.onResume();
        super.onResume();
        if (mMapType == MAP_SEND) {
        	new Thread(new Runnable() {
				@Override
				public void run() {
					startLocation();
				}
			}).run();
        }
        Logger.d(TAG, "[onResume] run done");
    }
 
    @Override
    protected void onStop() {
    	mMapView.onStop();
        super.onStop();
        V5ClientAgent.getInstance().onStop();
    }

	private void findView() {
		mLeftLayout = (LinearLayout) findViewById(UIUtil.getIdByName(this, "id", "header_layout_leftview_container"));
		mLocationDescTv = (TextView) findViewById(UIUtil.getIdByName(this, "id", "id_location_desc_tv"));
		mRightSendBtn = (Button) findViewById(UIUtil.getIdByName(this, "id", "btn_send"));
		mTitleTv = (TextView) findViewById(UIUtil.getIdByName(this, "id", "header_htv_subtitle"));
	}

	private void initMapView() {
		mMapView = (MapView) findViewById(UIUtil.getIdByName(this, "id", "mapviewOverlay"));
		mMapView.getMap().setZoom(9);
		
		if (mMapType == MAP_SEND) {
			mMapView.getMap().setOnMapClickListener(new OnMapClickListener() {
				
				@Override
				public void onMapClick(LatLng arg0) {
					if (mMarkerFix != null) {
						mMarkerFix.remove();
					}
					mMarkerFix = mMapView.getMap().addMarker(new MarkerOptions()
						.position(arg0)
						.title("")
						.anchor(0.5f, 1.0f)
						.icon(BitmapDescriptorFactory
						        .defaultMarker())
						        .draggable(true));
					UIUtil.getLocationTitle(LocationMapActivity.this, arg0.getLatitude(), arg0.getLongitude(), mLocationDescTv, mMarkerFix);
					if (mLocationBean == null) {
						mLocationBean = new LocationBean();
					}
					mLocationBean.setLatitude(arg0.getLatitude());
					mLocationBean.setLongitude(arg0.getLongitude());
				}
			});
			
			mMapView.getMap().setOnMarkerDraggedListener(new OnMarkerDraggedListener() {
				
				@Override
				public void onMarkerDragStart(Marker arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onMarkerDragEnd(Marker arg0) {
					UIUtil.getLocationTitle(
							LocationMapActivity.this,
							arg0.getPosition().getLatitude(), 
							arg0.getPosition().getLongitude(), 
							mLocationDescTv, 
							arg0);
					if (mLocationBean == null) {
						mLocationBean = new LocationBean();
					}
					mLocationBean.setLatitude(arg0.getPosition().getLatitude());
					mLocationBean.setLongitude(arg0.getPosition().getLongitude());
				}
				
				@Override
				public void onMarkerDrag(Marker arg0) {
					// TODO Auto-generated method stub
					
				}
			});
		}
	}

	private void initView() {
		initMapView();
		initTitleBar();
	}
	
	private void initTitleBar() {
		mTitleTv.setText(UIUtil.getIdByName(this, "string", "v5_location"));
		mLeftLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mMapType == MAP_SEND) {
					finishLocationMapActivity(false);
				} else {
					finish();
				}
			}
		});
		if (mMapType == MAP_SEND) {
			mRightSendBtn.setVisibility(View.VISIBLE);
			findViewById(UIUtil.getIdByName(this, "id", "header_right_btn")).setVisibility(View.GONE);
			mRightSendBtn.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					finishLocationMapActivity(true);
				}
			});
		} else {
			mRightSendBtn.setVisibility(View.GONE);
			findViewById(UIUtil.getIdByName(this, "id", "header_right_btn")).setVisibility(View.GONE);
		}
	}

	protected void finishLocationMapActivity(boolean isSend) {
		if (isSend) {
			Intent data = null;
			if (null != mLocationBean) {
				data = new Intent();
				mLocationBean.setAddress(mLocationDescTv.getText().toString());
				Bundle bundle = new Bundle();
				bundle.putSerializable("location", mLocationBean);
				data.putExtras(bundle);
			}
			setResult(RESULT_CODE_GET_LOCATION, data);
		} else {
			setResult(RESULT_CODE_NULL);
		}
		finish();
	}

	public void onMyLocationClick(View v) {
		if (mMapType == MAP_SEND) {
			Logger.d("LocationMapActivity", "[onMyLocationClick] requestLocationUpdates");
			mLocationManager.requestLocationUpdates(mLocationRequest, this);
			if (mLocation != null) {
//				mMapView.getMap().setZoom(15);
				mMapView.getMap().animateTo(getLatLng(mLocation));
			}
		} else {
			mMapView.getMap().animateTo(new LatLng(mX, mY));
		}
	}
	
	// ====== util methods	
	private static LatLng getLatLng(TencentLocation location) {
		LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
		return ll;
	}
	
	// ====== location callback
	private void startLocation() {
		Logger.d("LocationMapActivity", "[startLocation]");
		mLocationManager.requestLocationUpdates(mLocationRequest, this);
	}
	
	private void stopLocation() {
		Logger.d("LocationMapActivity", "[stopLocation]");
		mLocationManager.removeUpdates(this);
	}

	@Override
	public void onLocationChanged(TencentLocation location, int error, String reason) {
		// TODO Auto-generated method stub
		Logger.d("LocationMapActivity", "[onLocationChanged] error:" + error + " reason:" + reason);
		if (TencentLocation.ERROR_OK == error) { // 定位成功
			mLocationRequest.setInterval(0);
			mLocationManager.removeUpdates(this);
	        
			mLocation = location;
			if (mLocationBean == null) {
				mLocationBean = new LocationBean();
			}
			mLocationBean.setAccuracy(mLocation.getAccuracy());
			mLocationBean.setAddress(mLocation.getAddress());
			mLocationBean.setAltitude(mLocation.getAltitude());
			mLocationBean.setLatitude(mLocation.getLatitude());
			mLocationBean.setLongitude(mLocation.getLongitude());
			mLocationBean.setName(mLocation.getName());
			
			mLocationDescTv.setText(location.getAddress());
			
			if (location != null) {
				mMapView.getMap().setZoom(15);
				mMapView.getMap().animateTo(getLatLng(location));
			}
			
			// 更新 location 图层
			if (mMarkerFix != null) {
				mMarkerFix.remove();
			}
			mMarkerFix = mMapView.getMap().addMarker(new MarkerOptions()
				.position(getLatLng(location))
				.title(location.getAddress())
				.anchor(0.5f, 1.0f)
				.icon(BitmapDescriptorFactory
				        .defaultMarker())
				        .draggable(true));
//			mMarkerFix.showInfoWindow();
			mMapView.invalidate();
	    } else { // 定位失败
	    	Logger.e("LocationMapActivity", "[onLocationChanged] error:" + error);
	    }
	}

	@Override
	public void onStatusUpdate(String name, int status, String desc) {
		// TODO Auto-generated method stub
		
	}
	
	class LocationOverlay extends Overlay {

		LatLng llPoint;
		Bitmap bmpMarker;
		float fAccuracy = 0f;

		public LocationOverlay(Bitmap mMarker) {
			bmpMarker = mMarker;
		}


		public void setGeoCoords(LatLng point) {
			llPoint = new LatLng(point.getLatitude(),
					point.getLongitude());
		}

		public void setAccuracy(float fAccur) {
			fAccuracy = fAccur;
		}

		@Override
		public void draw(Canvas canvas, MapView mapView) {
			if (llPoint == null) {
				return;
			}
			Projection mapProjection = mapView.getProjection();
			Paint paint = new Paint();
			Point ptMap = mapProjection.toScreenLocation(llPoint);
			paint.setColor(Color.BLUE);
			paint.setAlpha(8);
			paint.setAntiAlias(true);

			float fRadius = mapProjection.metersToEquatorPixels(fAccuracy);
			canvas.drawCircle(ptMap.x, ptMap.y, fRadius, paint);
			paint.setStyle(Style.STROKE);
			paint.setAlpha(200);
			canvas.drawCircle(ptMap.x, ptMap.y, fRadius, paint);

			if (bmpMarker != null) {
				paint.setAlpha(255);
				canvas.drawBitmap(bmpMarker, ptMap.x - bmpMarker.getWidth() / 2,
						ptMap.y - bmpMarker.getHeight() / 2, paint);
			}

			super.draw(canvas, mapView);
		}
	}

	@Override
	public void onMarkerDrag(Marker arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onMarkerDragEnd(Marker arg0) {
		// TODO Auto-generated method stub
//		LatLng ll = arg0.getPosition();
//		mLocation = 
	}

	@Override
	public void onMarkerDragStart(Marker arg0) {
		// TODO Auto-generated method stub
		
	}
}
