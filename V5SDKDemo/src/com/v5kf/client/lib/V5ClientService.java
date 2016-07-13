package com.v5kf.client.lib;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.v5kf.client.lib.NetworkManager.NetworkListener;
import com.v5kf.client.lib.V5ClientAgent.ClientServingStatus;
import com.v5kf.client.lib.V5ClientAgent.OnMessageRunnable;
import com.v5kf.client.lib.V5KFException.V5ExceptionStatus;
import com.v5kf.client.lib.entity.V5Message;
import com.v5kf.client.lib.entity.V5MessageDefine;
import com.v5kf.client.lib.websocket.WebSocketClient;
//import android.support.v4.content.LocalBroadcastManager;

public class V5ClientService extends Service implements NetworkListener, WebSocketClient.Listener {

	public static final String TAG = "V5ClientService";
//	private static final String ACTION_ALARM = "com.v5kf.client.alarm";
	public static final String ACTION_SEND = "com.v5kf.client.send"; 
	public static final String ACTION_STOP = "com.v5kf.client.stop"; 

	private ServiceReceiver mMsgReceiver;
//	private ServiceReceiver mAlarmReceiver;
	private NetworkManager mNetReceiver;
	Handler mHandler;
	private String mUrl;
	private static WebSocketClient mClient;
	
	private DBHelper mDBHelper;
	private V5ConfigSP mConfigSP;
	private boolean cacheLocalMsg;
	private long mSessionStart;
	
	private int mReAuthCount = 0;
	
	private boolean _block = false;
	
	protected static boolean isConnected() {
		if (mClient != null && mClient.isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	protected static void reConnect(Context context) {
		Logger.i(TAG, "[reConnect] mClient:" + mClient);
		if (mClient != null) {
			mClient.connect();
		} else {
			Intent i = new Intent(context, V5ClientService.class);
			context.startService(i);
		}
	}

	protected static void stop() {
		if (mClient != null) {
			mClient.close(1000, "Normal close");
			mClient.disconnect();
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		if (Build.VERSION.SDK_INT < 18)
			startForeground(-1213, new Notification());
//		else 
//			startForeground(0, new Notification());
		initService();
	}
	
	private void initService() {
		mHandler = new ServiceHandler(this);
		
		mMsgReceiver = new ServiceReceiver();
		IntentFilter filter = new IntentFilter();
		//filter.addAction(ACTION_ALARM);
		filter.addAction(ACTION_SEND);
//		LocalBroadcastManager.getInstance(this).registerReceiver(mMsgReceiver, filter);
		registerReceiver(mMsgReceiver, filter);
		
//		mAlarmReceiver = new ServiceReceiver();
//		registerReceiver(mAlarmReceiver, new IntentFilter(ACTION_ALARM));
		
		mNetReceiver = new NetworkManager();
		NetworkManager.init(getApplicationContext());
		registerReceiver(
				mNetReceiver, 
				new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

		NetworkManager.addNetworkListener(this);
//		initAlarm();
	}
	
//	/**
//	 * 定时任务初始化
//	 */
//	private void initAlarm() {
//		Intent intent = new Intent(ACTION_ALARM);
//		PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);
//		AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
//		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 20 * 1000, 30 * 1000, pi);
//		//Logger.v(TAG, "[Alarm - start] -> com.v5kf.client.alarm");
//	}
//	
//	/**
//	 * 取消定时任务
//	 */
//	private void cancelAlarm() {
//		AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);  
//        Intent intent = new Intent(ACTION_ALARM);  
//        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);
//        if (pi != null){  
//            am.cancel(pi);
//        }
//	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logger.d(TAG, "[onStartCommand]");
		if (mConfigSP == null) {
			mConfigSP = new V5ConfigSP(this);
		}
		cacheLocalMsg = mConfigSP.readLocalDbFlag();
		if (mDBHelper == null && cacheLocalMsg) {
			mDBHelper = new DBHelper(this);
		}
		mSessionStart = V5ClientAgent.getInstance().getCurrentSessionStart();
		
		/* 开启service前需确保认证授权authorization不为空 */
		connectWebsocket(true);
		return super.onStartCommand(intent, flags, startId);
	}
	
	private synchronized void connectWebsocket() {
		connectWebsocket(false);
	}
	
	private synchronized void connectWebsocket(boolean forceNew) {
		if (_block) {
			Logger.w(TAG, "[connectWebsocket] _block return");
			return;
		}
		if (V5ClientAgent.getInstance().isExit()) {
			Logger.w(TAG, "[connectWebsocket] isExit return");
			return;
		}
		_block = true;
		Logger.d(TAG, "[connectWebsocket] auth:" +  V5ClientConfig.getInstance(this).getAuthorization());
		V5ClientConfig config = V5ClientConfig.getInstance(this);
		if (mClient != null && mClient.isConnected()) { // 已连接
			Logger.i(TAG, "[connectWebsocket] -> mClient != null && mClient.isConnected() = true");
			if (forceNew) {
				Logger.i(TAG, "disconnect and then forceNew client");
//				mClient.disconnect(1, "Stop and new client");
//				return;
				mClient.disconnect();
				mClient = null;
				mUrl = String.format(Locale.CHINA, V5ClientConfig.getWSFormstURL(), config.getAuthorization()); 
				mClient = new WebSocketClient(URI.create(mUrl), this, null);
				mClient.connect();
			} else {
				_block = false;
				return;
			}
		} else if (mClient != null) {
			Logger.d(TAG, "[connectWebsocket] -> mClient != null && mClient.isConnected() = false");
			if (forceNew) {
				Logger.d(TAG, "forceNew client");
				mClient.disconnect();
				mClient = null;
				mUrl = String.format(Locale.CHINA, V5ClientConfig.getWSFormstURL(), config.getAuthorization()); 
				mClient = new WebSocketClient(URI.create(mUrl), this, null);
			}			
			mClient.connect();
			if (mUrl != null) {
				Logger.d(TAG, "url:" + mUrl);
			}
		} else {
			Logger.d(TAG, "[connectWebsocket] -> new WebSocketClient");
			if (config.getAuthorization() == null) {
				//[修改]认证过期或者失效
				if (mReAuthCount < 3) {
					try {
						V5ClientAgent.getInstance().doAccountAuth();
						mReAuthCount++;
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					V5ClientConfig.getInstance(this).shouldUpdateUserInfo();
					// [修改]通知界面是否重试
					V5ClientAgent.getInstance().errorHandle(new V5KFException(V5ExceptionStatus.ExceptionWSAuthFailed, "authorization failed"));
				}
				_block = false;
				return;
			}
			mUrl = String.format(Locale.CHINA, V5ClientConfig.getWSFormstURL(), config.getAuthorization()); 
			mClient = new WebSocketClient(URI.create(mUrl), this, null);
			//Logger.d(TAG, "visitor_id:" + config.getV5VisitorId());
			mClient.connect();
			if (mUrl != null) {
				Logger.d(TAG, "url:" + mUrl);
			}
		}
	}
	
//	private void keepService() {
//		Logger.d(TAG, "[keepService] net:" + NetworkManager.isConnected(this));
//		if (NetworkManager.isConnected(this)) { // 判断是否连接
//			if (mClient == null) {
//				Logger.i(TAG, "[keepService] -> mClient is null -> new client");
//				connectWebsocket();
//			} else if (!mClient.isConnected()) {
//				Logger.i(TAG, "[keepService] -> not connect -> try connect");
//				mClient.connect();
//			} else {
//				Logger.i(TAG, "[keepService] -> connected");
//				mClient.ping();
//			}
//		} else {
//			Logger.i(TAG, "[keepService] -> Network not connect");
//		}
//	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Logger.w(TAG, "V5ClientService -> onDestroy");
		if (mClient != null) {
			// 发送断连帧
			mClient.close(1000, "Normal close");
			mClient.disconnect();
			mClient = null;
		}
//		Logger.d(TAG, "onDestroy -> unregisterReceiver");
//		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMsgReceiver);
		unregisterReceiver(mMsgReceiver);
//		unregisterReceiver(mAlarmReceiver);
		unregisterReceiver(mNetReceiver);
		NetworkManager.removeNetworkListener(this);
//		cancelAlarm();
		
//		if (!V5ClientAgent.getInstance().isExit()) {
//			Intent restartIntent = new Intent(getApplicationContext(), V5ClientService.class);
//			startService(restartIntent);
//		}
	}
	
	
	static class ServiceHandler extends Handler {
		
		WeakReference<V5ClientService> mService;
		
		public ServiceHandler(V5ClientService activity) {
			mService = new WeakReference<V5ClientService>(activity);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (null == mService.get()) {
				Logger.w(TAG, "ServiceHandler has bean GC");
			}
			switch (msg.what) {
			
			default:
				break;
			}
		}
	}
	
	class ServiceReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (null == intent) {
				return;
			}
			Logger.d(TAG, "onReceiver action:" + intent.getAction());
//			if (intent.getAction().equals(ACTION_ALARM)) {
//				// 定时检查
//				//keepService();
//			} else 
			if (intent.getAction().equals(ACTION_SEND)) {
				// 发送
				String msg = intent.getStringExtra("v5_message");
				if (null != msg) {
					sendMessage(msg);
				}
			} else if (intent.getAction().equals(ACTION_STOP)) {
				// 停止服务
				if (mClient != null) {
					mClient.disconnect();
				}
				stopSelf();
				Logger.d(TAG, "onReceiver:" + intent.getAction());
			}
		}
	}


	@Override
	public void onNetworkStatusChange(int netStatus, int oldStatus) {
		Logger.i(TAG, "[onNetworkStatusChange] -> " + netStatus);
		switch (netStatus) {
		case NetworkManager.NETWORK_MOBILE:
		case NetworkManager.NETWORK_WIFI:
			connectWebsocket();
			break;
			
		case NetworkManager.NETWORK_NONE:
			
			break;
		}
	}

	public void sendMessage(String msg) {
//		Logger.d(TAG, ">>>sendMessage<<<");
		if (mClient != null && mClient.isConnected()) {
			mClient.send(msg);
			Logger.i(TAG, ">>>sendMessage<<<:" + msg);
		} else {
			Logger.e(TAG, "[sendMessage] -> not connected try connect");
			connectWebsocket();
			//V5ClientAgent.getInstance().errorHandle(new V5KFException(V5ExceptionStatus.ExceptionNotConnected, "connection closed"));
		}
	}

	@Override
	public void onConnect() { // 连接成功后才可以开始调用消息接口
		Logger.i(TAG, ">>>onConnect<<< URL:" + mUrl);
		mReAuthCount = 0;
		
		V5ClientAgent.getInstance().sendOnLineMessage(); // 发上线消息
		if (V5ClientConfig.AUTO_WORKER_SERVICE) { // 自动转人工客服
			V5ClientAgent.getInstance().switchToArtificialService(null);
		}
		if (V5ClientConfig.UI_SUPPORT) {
			V5ClientAgent.getInstance().updateMessages();
		} else {
			// onConnect回调
			if (null != V5ClientAgent.getInstance().getHandler()) { 
				V5ClientAgent.getInstance().getHandler().post(new Runnable() {
					
					@Override
					public void run() {
						if (V5ClientAgent.getInstance().getMessageListener() != null) {
							V5ClientAgent.getInstance().getMessageListener().onConnect();
						}
					}
				});
			} else if (V5ClientAgent.getInstance().getMessageListener() != null) {
				V5ClientAgent.getInstance().getMessageListener().onConnect();
			}
		}
		
		_block = false;
	}

	@Override
	public void onMessage(String message) {
		Logger.i(TAG, ">>>onMessage<<<:" + message);
		try {
			JSONObject json = new JSONObject(message);
			if (json.optString("o_type").equals("message")) {
				V5Message messageBean = V5MessageManager.getInstance().receiveMessage(json);
				messageBean.setSession_start(mSessionStart);
				Logger.d(TAG, "onMessage messageBean:" + messageBean);
//				if (messageBean.getMsg_id() > 0 && messageBean.getMsg_id() < V5ClientAgent.OPEN_QUES_MAX_ID) {
//					// 开场问题的答案，在Activity里缓存
//					Logger.d(TAG, "onMessage MSG_ID");
//				} else if (null != mDBHelper && cacheLocalMsg) {
//					Logger.d(TAG, "onMessage cacheLocalMsg");
//					mDBHelper.insert(messageBean);
//				}
				
				Logger.d(TAG, "onMessage before messageBean:" + messageBean + " handle:" + V5ClientAgent.getInstance().getHandler() + 
						" messageListener:" + V5ClientAgent.getInstance().getMessageListener());
				if (null != V5ClientAgent.getInstance().getHandler()) { // 回调到Message消息接口
					V5ClientAgent.getInstance().getHandler().post(new OnMessageRunnable(messageBean));
				} else if (V5ClientAgent.getInstance().getMessageListener() != null) {
					V5ClientAgent.getInstance().getMessageListener().onMessage(messageBean);
				}
			} else if (json.optString("o_type").equals("session")) {
				if (json.optString("o_method").equals("get_status")) { // 对话状态信息：机器人或者客服信息
					int status = json.optInt("status");
					if (status == 2) {
						String nickname = json.optString("nickname");
						String photo = json.optString("photo");
						long wid = json.optLong("w_id");
						V5ClientAgent.getInstance().getConfig().setWorkerPhoto(photo);
						V5ClientAgent.getInstance().getConfig().setWorkerName(nickname);
						V5ClientAgent.getInstance().getConfig().setWorkerId(wid);
						V5ClientAgent.getInstance().getConfig().setWorkerType(status);
						// 保存wid->photo到本地
						V5ConfigSP configSP = new V5ConfigSP(getApplicationContext());
						configSP.savePhoto(wid, photo);
					}
					final ClientServingStatus ss = ClientServingStatus.getStatus(status);
					if (null != V5ClientAgent.getInstance().getHandler()) { // 回调到Message消息接口
						V5ClientAgent.getInstance().getHandler().post(new Runnable() {
							
							@Override
							public void run() {
								if (V5ClientAgent.getInstance().getMessageListener() != null) {
									V5ClientAgent.getInstance().getMessageListener().onServingStatusChange(ss);
								}
							}
						});
					} else if (V5ClientAgent.getInstance().getMessageListener() != null) {
						V5ClientAgent.getInstance().getMessageListener().onServingStatusChange(ss);
					}
				} else if (json.optString("o_method").equals("get_messages")) {
					List<V5Message> msgs = new ArrayList<V5Message>();
					JSONArray messages = json.optJSONArray("messages");
					if (null != messages && messages.length() > 0) {
						for (int i = 0; i < messages.length(); i++) {
							JSONObject item = messages.getJSONObject(i);
							V5Message msg = V5MessageManager.getInstance().receiveMessage(item);
							if (msg.getMsg_id() > 0 && msg.getMsg_id() < V5ClientAgent.OPEN_QUES_MAX_ID) {
								// 排除开场问题
							} else {
								msgs.add(0, msg);
							}
							V5Message candidate = null;
							if (msg.getCandidate() != null && msg.getCandidate().size() > 0) {
								candidate = msg.getCandidate().get(0);
								if (candidate.getDirection() == V5MessageDefine.MSG_DIR_FROM_ROBOT) {
									msgs.add(0, candidate);
								}
								msg.setCandidate(null);
							}
						}
					}
					// 插入数据库
					if (msgs.size() > 0) {
						// msgs倒序
						Collections.reverse(msgs);
						if (null != mDBHelper && cacheLocalMsg) {
							/**
							 * 注意插入数据库的顺序，决定了读取消息记录的顺序
							 */
							for (V5Message msg : msgs) {
								mDBHelper.insert(msg);
							}
						}
					}
					
					if (V5ClientAgent.getInstance().mMsgIdCount == 0) {
						V5ClientAgent.getInstance().mMsgIdCount = msgs.size() + 1; //+V5Util.getCurrentLongTime()%100
					}

					// onConnect回调
					if (null != V5ClientAgent.getInstance().getHandler()) { 
						V5ClientAgent.getInstance().getHandler().post(new Runnable() {
							
							@Override
							public void run() {
								if (V5ClientAgent.getInstance().getMessageListener() != null) {
									V5ClientAgent.getInstance().getMessageListener().onConnect();
								}
							}
						});
					} else if (V5ClientAgent.getInstance().getMessageListener() != null) {
						V5ClientAgent.getInstance().getMessageListener().onConnect();
					}
				}
			} else if (json.has("o_error")) {
				int code = json.getInt("o_error");
				if (code != 0) {
					String desc = json.optString("o_errmsg");
					V5ClientAgent.getInstance().errorHandle(new V5KFException(
							V5ExceptionStatus.ExceptionServerResponse, "[" + code + "]" + desc));//认证错误
				}
			} else {
				if (null != V5ClientAgent.getInstance().getHandler()) { // 回调到字符串消息接口
					V5ClientAgent.getInstance().getHandler().post(new OnMessageRunnable(message));
				} else if (V5ClientAgent.getInstance().getMessageListener() != null) {
					V5ClientAgent.getInstance().getMessageListener().onMessage(message);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(byte[] data) {
		Logger.d(TAG, ">>>onMessage[byte]<<<" + data);
	}

	@Override
	public void onDisconnect(int code, String reason) {
		Logger.v(TAG, ">>>onDisconnect<<< [code:" + code + "]: " + reason);
		// 断开重连判断
		if (V5ClientAgent.getInstance().isExit()) {
			// 不重连
			//stopSelf();
			Logger.w(TAG, "[onDisconnect] stop service");
			return;
		} else {
			// 因网络原因重连
			if (NetworkManager.isConnected(this)) {
				switch (code) {
				case -1: // 正常关闭，不重连
					break;
				case 1:
					mClient = null;
					connectWebsocket();
					break;
				case 1000: // 同一uid重复登录
					V5ClientAgent.getInstance().errorHandle(new V5KFException(V5ExceptionStatus.ExceptionConnectRepeat, "connection is cut off by same u_id"));
					break;
				default:
					connectWebsocket();
					V5ClientAgent.getInstance().errorHandle(new V5KFException(V5ExceptionStatus.ExceptionConnectionError, "[" + code + "]" + reason));
					break;
				}
			} else {
				V5ClientAgent.getInstance().errorHandle(new V5KFException(V5ExceptionStatus.ExceptionNoNetwork, "no network"));
			}
		}
		
		_block = false;
	}

	@Override
	public void onError(Exception error) {
		if (mClient == null) {
			Logger.e(TAG, "[onError] mClient == null");
			return;
		}
		Logger.e(TAG, ">>>onError<<<status code:" + mClient.getStatusCode() + " " + error.getMessage());
		if (mClient != null && mClient.isConnected()) {
			mClient.disconnect();
		}
		
		if (V5ClientAgent.getInstance().isExit()) {
			// 不重连
			stopSelf();
		} else {
			if (!NetworkManager.isConnected(this)) {
				V5ClientAgent.getInstance().errorHandle(new V5KFException(V5ExceptionStatus.ExceptionNoNetwork, "no network"));
			} else {
				if (mClient.getStatusCode() == 406 || mClient.getStatusCode() == 404) {
					Logger.d(TAG, "onError mReAuthCount:" + mReAuthCount);
					if (mReAuthCount < 3) {
						try {
							V5ClientAgent.getInstance().doAccountAuth();
							mReAuthCount++;
						} catch (JSONException e) {
							e.printStackTrace();
						}
					} else {
						V5ClientConfig.getInstance(this).shouldUpdateUserInfo();
						V5ClientAgent.getInstance().errorHandle(new V5KFException(V5ExceptionStatus.ExceptionWSAuthFailed, "authorization failed"));
					}
				} else if (error.getMessage().toLowerCase(Locale.getDefault()).contains("timed out") ||
						error.getMessage().toLowerCase(Locale.getDefault()).contains("timeout") ||
						error.getMessage().toLowerCase(Locale.getDefault()).contains("time out")) { // 条件不充足
//					if (mClient.getStatusCode() == 101) { // 已连接再出错
//						V5ClientAgent.getInstance().errorHandle(new V5KFException(V5ExceptionStatus.ExceptionNoNetwork, "no network"));
//					} else {
						V5ClientAgent.getInstance().errorHandle(new V5KFException(
								V5ExceptionStatus.ExceptionSocketTimeout, "[" + mClient.getStatusCode() + "]" + error.getMessage()));
//					}
				} else {
					//connectWebsocket(); //[修改]取消自动重连(死循环),在Activity中处理
					V5ClientAgent.getInstance().errorHandle(new V5KFException(
							V5ExceptionStatus.ExceptionConnectionError, "[" + mClient.getStatusCode() + "]" + error.getMessage()));
				}
			}
		}
		
		_block = false;
	}
}