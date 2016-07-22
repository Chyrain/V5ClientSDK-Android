package com.v5kf.client;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.v5kf.client.lib.Logger;
import com.v5kf.client.lib.V5ClientAgent;
import com.v5kf.client.lib.V5ClientAgent.ClientLinkType;
import com.v5kf.client.lib.V5ClientAgent.ClientOpenMode;
import com.v5kf.client.lib.V5ClientAgent.ClientServingStatus;
import com.v5kf.client.lib.V5ClientConfig;
import com.v5kf.client.lib.entity.V5Message;
import com.v5kf.client.ui.ClientChatActivity;
import com.v5kf.client.ui.callback.OnChatActivityListener;
import com.v5kf.client.ui.callback.OnURLClickListener;
import com.v5kf.client.ui.callback.UserWillSendMessageListener;

public class WelcomeActivity extends Activity implements OnChatActivityListener {

	private static final String TAG = "WelcomeActivity";
	private Button mChatBtn;
	private Button mChangeUidBtn;
	private EditText mUidEt;
	
	private boolean flag_userBrowseSomething = true; // 浏览某商品标志
	private boolean onceFlag = true; // 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//getSupportActionBar().hide();
		setContentView(R.layout.activity_test);
		
		initView();
		
		V5ClientAgent.clearCache(getApplicationContext());
		
		// V5客服系统客户端配置
        V5ClientConfig config = V5ClientConfig.getInstance(WelcomeActivity.this);
        V5ClientConfig.SOCKET_TIMEOUT = 30000; // 超时30s
        V5ClientConfig.DEBUG = true;
        V5ClientConfig.USE_HTTPS = true; // 使用加密连接，默认true
        config.setShowLog(true); // 显示日志，默认为true
        config.setLogLevel(V5ClientConfig.LOG_LV_DEBUG); // 显示日志级别，默认为全部显示
        
        config.setNickname("android_sdk_chyrain"); // 设置用户昵称
        config.setGender(1); // 设置用户性别: 0-未知  1-男  2-女
        // 设置用户头像URL
		config.setAvatar("https://tcdn21.wn517.com/dev/avatar/nl2fci00fzwpx6m4o2xi.jpg@0o_0l_64w_90q_1pr.jpg"); 
//        config.setUid("android_sdk_chyrain"); // 设置用户ID
        //config.setDeviceToken(""); // 集成第三方推送(腾讯信鸽、百度云推)时设置此参数以在离开会话界面时接收推送消息
//        config.setDeviceToken("new_device_token_for_test_1509");
        Logger.i(TAG, "[onCreate] visitor_id:" + config.getV5VisitorId());
        Logger.i(TAG, "[onCreate] uid:" + config.getUid());
	}

	private void initView() {
		initTitle();
		mChatBtn = (Button) findViewById(R.id.btn_goto_chat);
		mChangeUidBtn = (Button) findViewById(R.id.btn_change_uid);
		mUidEt = (EditText) findViewById(R.id.et_uid);
		
		mChatBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				/* 开启会话界面 */
			    // 可用Bundle传递以下参数
			    Bundle bundle=new Bundle();
			    bundle.putInt("numOfMessagesOnRefresh", 10);	// 下拉刷新数量，默认为10
			    bundle.putInt("numOfMessagesOnOpen", 10);		// 开场显示历史消息数量，默认为0
			    bundle.putBoolean("enableVoice", true);			// 是否允许发送语音
			    bundle.putBoolean("showAvatar", true);			// 是否显示对话双方的头像
			    // 开场白模式，默认为固定开场白，可根据客服启动场景设置开场问题
			    bundle.putInt("clientOpenMode", ClientOpenMode.clientOpenModeDefault.ordinal());
//			    bundle.putString("clientOpenParam", "您好，请问有什么需要帮助的吗？");
			    
			    //Context context = getApplicationContext();
			    //Intent chatIntent = new Intent(context, ClientChatActivity.class);
				//chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				//chatIntent.putExtras(bundle);
				//context.startActivity(chatIntent);
			    // 进入会话界面
			    V5ClientAgent.getInstance().startV5ChatActivityWithBundle(getApplicationContext(), bundle);
			    
				/* 添加聊天界面监听器(非必须，有更多自定义需求方添加) */
			    // 界面生命周期监听[非必须]
			    V5ClientAgent.getInstance().setChatActivityListener(WelcomeActivity.this);
			    // 消息发送监听[非必须]
			    V5ClientAgent.getInstance().setUserWillSendMessageListener(new UserWillSendMessageListener() {
					
					@Override
					public V5Message onUserWillSendMessage(V5Message message) {
						// TODO 可在此处添加消息参数(JSONObject键值对均为字符串)，采集信息透传到坐席端
						if (flag_userBrowseSomething) {
							JSONObject customContent = new JSONObject();
							try {
								customContent.put("用户名", mUidEt.getText().toString());
								customContent.put("用户级别", "VIP");
								customContent.put("用户积分", "300");
								customContent.put("浏览商品", "衬衣");
							} catch (JSONException e) {
								e.printStackTrace();
							}
							message.setCustom_content(customContent);
							
							flag_userBrowseSomething = false;
						} else {
//							JSONObject customContent = new JSONObject();
//							try {
//								customContent.put("手机", "Calaxy S7 Edge G935-FD");
//								customContent.put("操作系统", "Android 6.0.1");
//							} catch (JSONException e) {
//								e.printStackTrace();
//							}
//							message.setCustom_content(customContent);
						}
						return message; // 注：必须将消息对象以返回值返回
					}
				});
			    
			    // 点击链接监听
			    V5ClientAgent.getInstance().setURLClickListener(new OnURLClickListener() {

					@Override
					public boolean onURLClick(Context context,
							ClientLinkType type, String url) {
						// TODO Auto-generated method stub
						Logger.i(TAG, "onURLClick:" + url);
						return false;
					}
				});
			}
		});
		
		mChangeUidBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String uid = mUidEt.getText().toString();
				if (TextUtils.isEmpty(uid)) {
					Toast.makeText(WelcomeActivity.this, "uid不能为空", Toast.LENGTH_SHORT).show();
					return;
				}
				/* 切换用户 */
				V5ClientConfig config = V5ClientConfig.getInstance(WelcomeActivity.this);
				config.shouldUpdateUserInfo();
				config.setNickname(uid); // 设置用户昵称
		        config.setGender(1); // 设置用户性别: 0-未知  1-男  2-女
				config.setAvatar("http://static.v5kf.com/images/qrcode_v5_app.png"); 
		        config.setUid(uid); // 设置用户ID
			    config.setDeviceToken("new_device_token_for_test_1509");
		        Logger.d(TAG, "new visitor_id:" + config.getV5VisitorId());
			}
		});
	}

	private void initTitle() {
		findViewById(R.id.header_ib_imagebutton).setVisibility(View.GONE);
		TextView titleTv = (TextView) findViewById(R.id.header_htv_subtitle);
		titleTv.setText(R.string.v5_chat_title);
	}

	@Override
	public void onChatActivityCreate(ClientChatActivity activity) {
		// TODO Auto-generated method stub
		Logger.d(TAG, "<onChatActivityCreate>");
	}

	@Override
	public void onChatActivityStart(ClientChatActivity activity) {
		// TODO Auto-generated method stub
		Logger.d(TAG, "<onChatActivityStart>");
	}

	@Override
	public void onChatActivityStop(ClientChatActivity activity) {
		// TODO Auto-generated method stub
		Logger.d(TAG, "<onChatActivityStop>");
	}

	@Override
	public void onChatActivityDestroy(ClientChatActivity activity) {
		// TODO Auto-generated method stub
		Logger.d(TAG, "<onChatActivityDestroy>");
	}

	@Override
	public void onChatActivityConnect(ClientChatActivity activity) {
		// TODO Auto-generated method stub
		Logger.d(TAG, "<onChatActivityConnect>");
		
		if (onceFlag) {
			onceFlag = false;
			
			// 找指定客服
			//V5ClientAgent.getInstance().transferHumanService(1, 114052);
			
//			// 发送图文测试
//			V5ArticlesMessage articleMsg = new V5ArticlesMessage();
//			V5ArticleBean article = new V5ArticleBean(
//					"V5KF", 
//					"http://rs.v5kf.com/upload/10000/14568171024.png", 
//					"http://www.v5kf.com/public/weixin/page.html?site_id=10000&id=218833&uid=3657455033351629359", 
//					"V5KF是围绕核心技术“V5智能机器人”研发的高品质在线客服系统。可以运用到各种领域，目前的主要产品有：微信智能云平台、网页智能客服系统...");
//			ArrayList<V5ArticleBean> articlesList = new ArrayList<V5ArticleBean>();
//			articlesList.add(article);
//			articleMsg.setArticles(articlesList);
//			V5ClientAgent.getInstance().sendMessage(articleMsg, null);
		}
	}

	@Override
	public void onChatActivityReceiveMessage(ClientChatActivity activity, V5Message message) {
		// TODO Auto-generated method stub
		Logger.d(TAG, "<onChatActivityReceiveMessage> " + message.getDefaultContent(this));
	}
	
	@Override
	public void onChatActivityServingStatusChange(ClientChatActivity activity,
			ClientServingStatus status) {
		// TODO Auto-generated method stub
		switch (status) {
		case clientServingStatusRobot:
		case clientServingStatusQueue:
			activity.setChatTitle("机器人服务中");
			break;
		case clientServingStatusWorker:
			activity.setChatTitle(V5ClientConfig.getInstance(getApplicationContext()).getWorkerName() + "为您服务");
			break;
		case clientServingStatusInTrust:
			activity.setChatTitle("机器人托管中");
			break;
		}
	}
}
