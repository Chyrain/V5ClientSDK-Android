package com.v5kf.client.lib;

import java.security.NoSuchAlgorithmException;

import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class V5ClientConfig {
	/*
	 * Log level: 1 - 5
	 */
	public static final int LOG_LV_ERROR = 1;
	public static final int LOG_LV_WARN = 2;
	public static final int LOG_LV_INFO = 3;
	public static final int LOG_LV_DEBUG = 4;
	public static final int LOG_LV_VERBOS = 5;
	
	public static boolean RUN_ON_UI_THREAD = true;
	public static boolean UI_SUPPORT = false; // 是否需要UI显示，无UI则不需要getCurrentMessages和getStatus
	
	private static int LOG_LEVEL = LOG_LV_VERBOS; // 日志显示级别
	private static boolean LOG_SHOW = true;	// 是否显示日志
	public static final boolean USE_THUMBNAIL = true; // 使用缩略图
	public static boolean AUTO_RETRY_ONERROR = false; // 连接断开是否自动重试(否则弹出对话框点击重试)
	
	public static int SOCKET_TIMEOUT = 10000; // 超时时间10s
	
	protected static boolean AUTO_WORKER_SERVICE = false;
	protected static boolean NOTIFICATION_SHOW = true;
//	protected static final int NOTIFICATION_ID = 23;
	protected static final String ACTION_NOTIFICATION = "com.v5kf.android.intent.notification";
	protected static final String ACTION_NEW_MESSAGE = "com.v5kf.android.intent.action_message";
	
	
	public static boolean DEBUG = false; // 是否debug模式(连接debug服务端)
	public static boolean USE_HTTPS = true; // 默认使用https访问
	
	/**
	 * 客户端连接地址格式 -> [修改]使用静态函数方式获取
	 */
//	public static final String SDK_INIT_URL = (USE_HTTPS ? "https" : "http") + "://www.v5kf.com/public/appsdk/init";
//	public static final String WS_URL_FMT = "ws://chat.v5kf.com/sitews?token=%s&site=%s&o_id=%s";
//	protected static final String WS_URL_FMT = (USE_HTTPS ? "wss" : "ws") + "://chat.v5kf.com/" + DEBUG + "/appws/v2?auth=%s"; // ws地址
//			"site=%s&account=%s&visitor=%s&device=android&timestamp=%d&nonce=%d&expires=%d&signature=%s&
//	protected static final String CHAT_HOST = "http://chat.v5kf.com";
//	public static final String AUTH_URL = "http://chat.v5kf.com/public/webauth/v9"; // web认证v9
//	protected static final String AUTH_URL = (USE_HTTPS ? "https" : "http") + "://chat.v5kf.com/" + DEBUG + "/appauth/v2"; // app认证v2
//	protected static final String ORIGIN = "http://chat.v5kf.com";
//	protected static final String PIC_AUTH_URL = (USE_HTTPS ? "https" : "http") + "://chat.v5kf.com/" + DEBUG + "/wxyt/app?auth=";
//	protected static final String SDK_MEDIA_AUTH_URL = "https://chat.v5kf.com/" + DEBUG + "/wxyt/app?type=voice&suffix=amr&auth=";
	
	/**
	 * 获取站点配置信息地址格式 -> [修改]使用静态函数方式获取
	 */
//	protected static final String SITEINFO_URL_FMT = (USE_HTTPS ? "https" : "http") + "://www.v5kf.com/public/api_dkf/get_chat_siteinfo?sid=%s";
	
	// 本地化配置文件名称
	protected static final String PREFS_FILE = "v5kf_client";
	
	/* 用户信息 */
	// site、account
	private String uid; // 多用户账号APP必须
	private String nickname;
	private String avatar; 
	private int gender;
	
	/* 坐席信息 */
	private long workerId;
	private String workerName;
	private String workerPhoto;
	private int workerType; // 人工or机器人or机器人托管
	
	/* SDK基础配置信息(AndroidManifest.xml)，需要缓存本地 */
	private String siteId;
	private String siteAccount;
	
	/* 需要缓存本地，下次可直接连接，需要绑定uid，切换用户后需要重新认证 */
	private long timestamp;
//	private long nonce;//[修改]去掉nonce
	private long expires;
	private String authorization; // 此参数用于ws连接以及下面参数的签名
	private String v5VisitorId; /* 一个账号对应一个uid->v5VisitorId */
	/* 推送服务 */
	private String deviceToken; /* 一个App对应一个deviceToken */
	/* 通知 */
	private String notificationTitle;
	
	private Context mContext;
	private static V5ClientConfig mClientConfig = null;
	
	private V5ClientConfig(Context context) {
//		Logger.d("V5ClientConfig", "V5ClientConfig instance");
		this.mContext = context;		
	}
	
	/**
	 * 获得V5ClientConfig单例对象
	 * @param context
	 * @return
	 */
	public static V5ClientConfig getInstance(Context context) {
		if(mClientConfig == null){
	        synchronized (V5ClientConfig.class) {   // 保证了同一时间只能只能有一个对象访问此同步块        
	            if(mClientConfig == null){
	            	mClientConfig = new V5ClientConfig(context);
	        }
	      }
	    }
		return mClientConfig;
	}
	
	/**
	 * 销毁V5ClientConfig单例对象
	 */
	public static synchronized void destroyInstance() {
		if (mClientConfig != null) 
			mClientConfig = null;
	}
	
	/**
	 * 是否允许本地消息缓存
	 * @param enable
	 */
	public void setLocalMessageCacheEnable(boolean enable) {
		V5ConfigSP csp = new V5ConfigSP(mContext);
		csp.saveLocalDbFlag(enable);
	}
	
	public boolean getLocalMessageCacheEnable() {
		V5ConfigSP csp = new V5ConfigSP(mContext);
		return csp.readLocalDbFlag();
	}
	
	/**
	 * 设置默认转人工客服
	 * @param isAutoWorker 是否默认转人工客服，默认为机器人接待
	 */
	public void setDefaultServiceByWorker(boolean isAutoWorker) {
		V5ClientConfig.AUTO_WORKER_SERVICE = isAutoWorker;
	}
	
//	/**
//	 * 获取用户唯一识别码(利用设备ID生成，不超过32个字符)应用唯一
//	 * @param context
//	 * @return
//	 */
//	public String getUuid() {
//		if (null == uuid) {		
//			DeviceUuidFactory duf = new DeviceUuidFactory(mContext);
//			uuid = duf.getUuidString();
//			if (null == uuid) {
//				uuid = V5Util.getRandomString(24);
//			}
//			Logger.d("SiteConfig->getUuid", "uuid:" + uuid);
//		}
//		if (uuid.length() > 32) {
//			try {
//				uuid = hash(uuid);
//				Logger.d("SiteConfig->getUuid(md5)", "uuid:" + uuid);
//			} catch (NoSuchAlgorithmException e) {
//				e.printStackTrace();
//			}
//		}
//		return uuid;
//	}
//	
//	/**
//	 * 设置小于32字节的字符串作为o_id
//	 * @param oid
//	 */
//	public void setUuid(String oid) {
//		if (oid.length() > 32) {
//			Logger.w("V5ClientConfig", "The string in setUuid can't greater than 32 bytes.");
//		}
//		uuid = oid;
//	}
	
	/**
	 * 获取站点号
	 * @param context
	 * @return
	 */
	public String getSiteId() {
		if (siteId != null) {
			return siteId;
		}
		
		ApplicationInfo appInfo = null;
		try {
			appInfo = mContext.getPackageManager()
			        .getApplicationInfo(mContext.getPackageName(),
			        PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		if (appInfo != null) {
			String msg = String.valueOf(appInfo.metaData.getInt("V5_SITE"));
			return msg;
		} else {
			return null;
		}
	}

	protected void setSiteId(String siteId) {
		this.siteId = siteId;
		V5ConfigSP configSP = new V5ConfigSP(mContext);
		configSP.saveSiteId(siteId);
	}

	/**
	 * 获取站点号
	 * @param context
	 * @return
	 */
	public String getSiteAccount() {
		if (siteAccount != null) {
			return siteAccount;
		}
		
		ApplicationInfo appInfo = null;
		try {
			appInfo = mContext.getPackageManager()
					.getApplicationInfo(mContext.getPackageName(),
							PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		if (appInfo != null) {
			String msg = String.valueOf(appInfo.metaData.getString("V5_ACCOUNT"));
			return msg;
		} else {
			return null;
		}
	}

	protected void setSiteAccount(String siteAccount) {
		this.siteAccount = siteAccount;
		V5ConfigSP configSP = new V5ConfigSP(mContext);
		configSP.saveSiteAccount(siteAccount);
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public int getGender() {
		return gender;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}
	
	
	public static int getLogLevel() {
		if (LOG_SHOW) {
			return LOG_LEVEL;
		} else {
			return 0;
		}
	}

	public void setLogLevel(int lOG_LEVEL) {
		LOG_LEVEL = lOG_LEVEL;
	}
	
	public void setShowLog(boolean show) {
		LOG_SHOW = show;
	}
	
	public boolean getShowLog() {
		return LOG_SHOW;
	}

	protected String getNotificationAction() {
		return ACTION_NOTIFICATION + getSiteId();
	}

//	public String getSignature() {
//		if (this.signature != null) {
//			return signature;
//		} else {
//			V5ConfigSP config = new V5ConfigSP(mContext);
//			return config.readSignature(getV5VisitorId());
//		}
//	}
//
//	public void setSignature(String signature) {
//		this.signature = signature;
//		V5ConfigSP config = new V5ConfigSP(mContext);
//		config.saveSignature(getV5VisitorId(), signature);
//	}

	public long getExpires() {
		if (expires != 0) {
			return expires;
		} else {
			V5ConfigSP config = new V5ConfigSP(mContext);
			return config.readExpires();
		}
	}

	protected void setExpires(long expires) {
		this.expires = expires;
		V5ConfigSP config = new V5ConfigSP(mContext);
		config.saveExpires(expires);
	}

//	public long getNonce() {
//		if (nonce != 0) {
//			return nonce;
//		} else {
//			V5ConfigSP config = new V5ConfigSP(mContext);
//			return config.readNonce();
//		}
//	}
//
//	public void setNonce(long nonce) {
//		this.nonce = nonce;
//		V5ConfigSP config = new V5ConfigSP(mContext);
//		config.saveNonce(nonce);
//	}

	public long getTimestamp() {
		if (timestamp != 0) {
			return timestamp;
		} else {
			V5ConfigSP config = new V5ConfigSP(mContext);
			return config.readTimestamp();
		}
	}

	protected void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
		V5ConfigSP config = new V5ConfigSP(mContext);
		config.saveTimestamp(timestamp);
	}

	public String getAuthorization() {
		if (authorization != null) {
			return authorization;
		} else {
			V5ConfigSP config = new V5ConfigSP(mContext);
			return config.readAuthorization(getV5VisitorId());
		}
	}

	protected void setAuthorization(String authorization) {
		this.authorization = authorization;
		V5ConfigSP config = new V5ConfigSP(mContext);
		config.saveAuthorization(getV5VisitorId(), authorization);
	}

	public String getDeviceToken() {
		if (deviceToken == null) {
			V5ConfigSP configSP = new V5ConfigSP(mContext);
			deviceToken = configSP.readDeviceToken();
//			if (deviceToken == null) {
//				try {
//					deviceToken = V5Util.hash(getAppkey());
//				} catch (NoSuchAlgorithmException e) {
//					e.printStackTrace();
//				}
//			}
		}
//		Logger.d("V5ClientConfig", "getDeviceToken=" + deviceToken);
		return deviceToken;
	}

	/**
	 * 必须。消息推送应用识别，不同的设备不同的应用该值需不同
	 * @param deviceToken
	 */
	public void setDeviceToken(String deviceToken) {
//		Logger.d("V5ClientConfig", "setDeviceToken:" + deviceToken);
		if (null == deviceToken || deviceToken.isEmpty()) {
			Logger.e("V5ClientConfig", "DeviceToken is null or empty!");
		} else {
			this.deviceToken = deviceToken;
			V5ConfigSP configSP = new V5ConfigSP(mContext);
			configSP.saveDeviceToken(deviceToken);
		}
	}
	
	public String getNotificationTitle() {
		if (notificationTitle == null) {
			V5ConfigSP configSP = new V5ConfigSP(mContext);
			notificationTitle = configSP.readNotificationTitle();
		}
		return notificationTitle;
	}

	public void setNotificationTitle(String title) {
		if (null == title || title.isEmpty()) {
			return;
		} else {
			this.notificationTitle = title;
			V5ConfigSP configSP = new V5ConfigSP(mContext);
			configSP.saveNotificationTitle(title);
		}
	}

	public String getUid() {
		if (null == uid) {
			V5ConfigSP configSP = new V5ConfigSP(mContext);
			uid = configSP.readUid();
			
//			if (uid == null) {
//				DeviceUuidFactory uuidFactory = new DeviceUuidFactory(mContext);
//				String uuidStr = uuidFactory.getUuidString();
//				Logger.d("V5ClientConfig", "getUid null so setUid:" + uuidStr);
//				setUid(uuidStr);
//			}
		}
		return uid;
	}

	/**
	 * 必须。APP用户ID（支持用户账号切换的APP必须为不同用户账号设置该值）
	 * @param uid
	 */
	public void setUid(String uid) {
		if (null == uid) {
			Logger.e("V5ClientConfig", "Uid is null!");
			return;
		}
		this.uid = uid;
		V5ConfigSP configSP = new V5ConfigSP(mContext);
		String localUid = configSP.readUid();
		if (localUid != null && !localUid.equals(uid)) {
			// 重新设置uid与之前不同时需要清除之前的visitor缓存
			configSP.removeAuthorization(getV5VisitorId());
			configSP.removeVisitorId();
			v5VisitorId = null;
			authorization = null;
		}
		configSP.saveUid(uid);
		Logger.d("V5ClientConfig", "setUid:" + uid);
	}

	/**
	 * 64字节visitor ID(V5系统用户识别ID)
	 * @return
	 */
	public String getV5VisitorId() {
		if (v5VisitorId != null) {
			return v5VisitorId;
		}
		
		V5ConfigSP config = new V5ConfigSP(mContext);
		v5VisitorId = config.readVisitorId();
		if (null != v5VisitorId) {
			return v5VisitorId;
		}
		
		if (uid == null || uid.isEmpty()) {
			try {
				DeviceUuidFactory uuidFactory = new DeviceUuidFactory(mContext);
				uid = uuidFactory.getUuidString();
				if (uid == null) {
					uid = V5Util.getRandomString(48) + getSiteAccount() + getSiteId();
				}
				v5VisitorId = V5Util.hash(uid);
				Logger.i("V5ClientConfig", "VisitorId:" + v5VisitorId + " uid:" + uid);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		} else {
			try {
				v5VisitorId = V5Util.hash(uid + getSiteAccount() + getSiteId());
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		config.saveVisitorId(v5VisitorId);
			
		return v5VisitorId;
	}

	/**
	 * 获得通知id，即为站点编号值
	 * @param context
	 * @return
	 */
	protected static int getNotifyId(Context context) {
		int notifyId = 0;
		try {
			notifyId = Integer.parseInt(V5ClientConfig.getInstance(context).getSiteId());
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return notifyId;
	}
	
	/**
	 * 更新用户信息前调用（setUid前）
	 */
	public void shouldUpdateUserInfo() {
		// 更新用户信息时调用，清除之前的visitor缓存
		V5ConfigSP configSP = new V5ConfigSP(mContext);
		configSP.removeAuthorization(getV5VisitorId());
		configSP.removeVisitorId();
		v5VisitorId = null;
		authorization = null;
	}
	
//	protected static Notification getNotification(Context context, V5Message message) {
//		// 此Builder为android.support.v4.app.NotificationCompat.Builder中的，下同。
//		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
//        
//		// 系统收到通知时，通知栏上面滚动显示的文字。
//		mBuilder.setTicker(message.getDefaultContent(context));
//		V5ClientConfig config = V5ClientConfig.getInstance(context);
//		Intent intent = null;
//		intent = new Intent(config.getNotificationAction());
//		Bundle bundle = new Bundle();
//		bundle.putSerializable("v5_message", message);
//		intent.putExtras(bundle);
//		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		
//		// 点击通知之后需要跳转的页面
//        PendingIntent pIntent = PendingIntent.getActivity(
//        		context, 
//        		0, 
//        		intent, 
//        		PendingIntent.FLAG_UPDATE_CURRENT);
//        mBuilder.setContentIntent(pIntent);
//        
//        // 通知标题
//        String notifTitle = config.getNotificationTitle();
//        if (null == notifTitle) {
//        	notifTitle = context.getString(V5Util.getIdByName(context, "string", "v5_def_title"));
//        }
//        mBuilder.setContentTitle(notifTitle);
//        // 通知内容
//        mBuilder.setContentText(message.getDefaultContent(context));
// 
//        // 显示在通知栏上的小图标
//        mBuilder.setSmallIcon(context.getApplicationInfo().icon);
//        
//        // 设置大图标，即通知条上左侧的图片（如果只设置了小图标，则此处会显示小图标）
//        mBuilder.setLargeIcon(
//    			BitmapFactory.decodeResource(context.getResources(),
//    					context.getApplicationInfo().icon));
// 
//        // 设置为可清除模式
//        mBuilder.setOngoing(false);
//        
//        // 点击自动消失 
//        mBuilder.setAutoCancel(true);
//                
//        // 设置铃声、震动、LED灯提醒-默认 
//        mBuilder.setDefaults(Notification.DEFAULT_ALL);
//        
//        Notification notification = mBuilder.build();
//        Logger.d("V5ClientConfig", "notifyMessage send <<<");
//        return notification;
//	}
	
	protected static NotificationManager getNotificationManager(Context context) {
		return (NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
	}
	
	protected static String getSDKInitURL() {
		return (USE_HTTPS ? "https" : "http") + "://www.v5kf.com/public/appsdk/init";
	}
	
	protected static String getWSFormstURL() {
		return (USE_HTTPS ? "wss" : "ws") + "://chat.v5kf.com/" + (DEBUG ? "debug" : "public") + "/appws/v2?auth=%s"; // ws地址
	}
	
	protected static String getAccountAuthURL() {
		return (USE_HTTPS ? "https" : "http") + "://chat.v5kf.com/" + (DEBUG ? "debug" : "public") + "/appauth/v2"; // app认证v2
	}
	
	protected static String getPictureAuthURL() {
		return (USE_HTTPS ? "https" : "http") + "://chat.v5kf.com/" + (DEBUG ? "debug" : "public") + "/wxyt/app?auth=";
	}
	
	protected static String getSiteinfoFormatURL() {
		return (USE_HTTPS ? "https" : "http") + "://www.v5kf.com/public/api_dkf/get_chat_siteinfo?sid=%s";
	}
	
	protected static String getPictureThumbnailFormatURL() { //APP_PIC_V5_THUMBNAIL_FMT
		return (USE_HTTPS ? "https" : "http") + "://chat.v5kf.com/" + (DEBUG ? "debug" : "public") + "/resource/%s/%s?w=350&h=350&q=60"; // 图片质量0-100
	}
	
	protected static String getResourceFormatURL() { //APP_RESOURCE_V5_FMT
		return (USE_HTTPS ? "https" : "http") + "://chat.v5kf.com/" + (DEBUG ? "debug" : "public") + "/resource/%s/%s";
	}
	
	protected static String getMediaAuthURL() { //SDK_MEDIA_AUTH_URL
		return (USE_HTTPS ? "https" : "http") + "://chat.v5kf.com/" + (DEBUG ? "debug" : "public") + "/wxyt/app?type=voice&suffix=amr&auth=";
	}

	public String getWorkerName() {
		return workerName;
	}

	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}

	public String getWorkerPhoto() {
		return workerPhoto;
	}

	public void setWorkerPhoto(String workerPhoto) {
		this.workerPhoto = workerPhoto;
	}

	public int getWorkerType() {
		return workerType;
	}

	public void setWorkerType(int workerType) {
		this.workerType = workerType;
	}

	public long getWorkerId() {
		return workerId;
	}

	public void setWorkerId(long workerId) {
		this.workerId = workerId;
	}

//	public void setWorkerPhoto(String photo, String nickname) {
//		if (photo == null || nickname == null || 
//				nickname.isEmpty() || photo.isEmpty()) {
//			return;
//		}
//		V5ConfigSP config = new V5ConfigSP(mContext);
//		config.saveString(photo, nickname);
//	}
//	
//	public String getWorkerPhoto(String nickname) {
//		if (nickname == null || nickname.isEmpty()) {
//			return null;
//		}
//		V5ConfigSP config = new V5ConfigSP(mContext);
//		return config.readString(nickname);
//	}
 }
