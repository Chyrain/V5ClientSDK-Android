package com.v5kf.v5sdkdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.v5kf.client.lib.Logger;
import com.v5kf.client.lib.V5ClientAgent;
import com.v5kf.client.lib.V5ClientAgent.ClientServingStatus;
import com.v5kf.client.lib.V5ClientConfig;
import com.v5kf.client.lib.V5KFException;
import com.v5kf.client.lib.V5MessageManager;
import com.v5kf.client.lib.callback.MessageSendCallback;
import com.v5kf.client.lib.callback.V5InitCallback;
import com.v5kf.client.lib.callback.V5MessageListener;
import com.v5kf.client.lib.entity.V5Message;
import com.v5kf.client.lib.entity.V5TextMessage;
//import android.support.v7.app.AppCompatActivity;


public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	
	
	private TextView mStatusTv;
	private TextView mLogTv;
	private EditText mInputEt;
	private Button mSendBtn;
	private Button mConnectBtn;
	private Button mDisconBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        findView();
        initView();
        
        // 初始化SDK
        V5ClientAgent.init(this, new V5InitCallback() {
			
			@Override
			public void onSuccess(String response) {
				Logger.i(TAG, "V5ClientAgent.init(): " + response);
			}
			
			@Override
			public void onFailure(String response) {
				Logger.e(TAG, "V5ClientAgent.init(): " + response);
			}
		});
    }
    
    /** 生命周期的处理 onStart、onStop **/
    @Override
    protected void onStart() {
    	// TODO Auto-generated method stub
    	super.onStart();
    	V5ClientAgent.getInstance().onStart();
    }
    
    @Override
    protected void onStop() {
    	// TODO Auto-generated method stub
    	super.onStop();
    	V5ClientAgent.getInstance().onStop();
    }
    
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    	V5ClientAgent.getInstance().onDestroy();
    }
    
    private void findView() {
		mStatusTv = (TextView)findViewById(R.id.status_tv);
		mLogTv = (TextView)findViewById(R.id.log_tv);
		mInputEt = (EditText)findViewById(R.id.input_et);
		mSendBtn = (Button)findViewById(R.id.send_btn);
		mConnectBtn = (Button)findViewById(R.id.connect_btn);
		mDisconBtn = (Button)findViewById(R.id.disconnect_btn);
	}

	private void initView() {
		mSendBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!V5ClientAgent.isConnected()) {
					Toast.makeText(getApplicationContext(), "未连接，请先连接", Toast.LENGTH_SHORT).show();
					return;
				}
				
				if (!mInputEt.getText().toString().isEmpty()) {
					// 发送消息
					V5TextMessage message = V5MessageManager.getInstance().obtainTextMessage(mInputEt.getText().toString());
					sendV5Message(message);
				} else {
//					// 发送语音 path为本地语音文件
//					V5VoiceMessage voiceMessage = V5MessageManager.obtainVoiceMessage(path);
//					V5ClientAgent.getInstance().sendMessage(voiceMessage, null);
				}
			}
		});
		
		mConnectBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO 连接
				configV5Client(); // 配置客户端
				V5ClientAgent.getInstance().start(getApplicationContext(), messageListener);
				mStatusTv.setText("状态：正在连接...");
			}
		});
		
		mDisconBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO 断开连接
				V5ClientAgent.getInstance().onDestroy();
				mStatusTv.setText("状态：已断开");
			}
		});
	}
	
	protected void sendV5Message(V5TextMessage message) {
		V5ClientAgent.getInstance().sendMessage(message, new MessageSendCallback() {
			
			@Override
			public void onSuccess(V5Message message) {
				Logger.d(TAG, "V5Message.getState:" + message.getState());
				/**
				 * V5Message.STATE_ARRIVED	// 发送成功
				 * V5Message.STATE_SENDING	// 发送中
				 * V5Message.STATE_FAILURE	// 发送失败
				 * V5Message.STATE_UNKNOW	// 默认无状态
				 */
			}
			
			@Override
			public void onFailure(V5Message message, V5KFException.V5ExceptionStatus statusCode, String desc) {
				Logger.e(TAG, "V5Message.getState:" + message.getState() + " exception(" + statusCode + "):" + desc);
				
			}
		});
	}

	/**
	 * 客户端配置
	 */
	private void configV5Client() {
		// V5客服系统客户端配置
        V5ClientConfig config = V5ClientConfig.getInstance(this);
        V5ClientConfig.USE_HTTPS = true; // 使用加密连接，默认true
        V5ClientConfig.RUN_ON_UI_THREAD = false;
        config.setShowLog(true); // 显示日志，默认为true
        config.setLogLevel(V5ClientConfig.LOG_LV_DEBUG); // 显示日志级别，默认为全部显示
        
        config.setNickname("android_sdk_test"); // 设置用户昵称，不设置会默认生成
        config.setGender(1); // 设置用户性别: 0-未知  1-男  2-女
        // 设置用户头像URL
		config.setAvatar("http://debugimg-10013434.image.myqcloud.com/fe1382d100019cfb572b1934af3d2c04/thumbnail"); 
		//config.setUid("android_sdk_chyrain"); // 设置用户ID，绑定你的用户账号，不设置则系统默认生成
        //config.setDeviceToken(""); // 集成第三方推送(腾讯信鸽、百度云推)时设置此参数以在离开会话界面时接收推送消息
		config.setLocalMessageCacheEnable(false); // 不保存消息
		Logger.d(TAG, "get visitor_id:" + config.getV5VisitorId());
	}


	private V5MessageListener messageListener = new V5MessageListener() {
		
		/**
		 * 客户接入状态改变(人工客服或者机器人服务)
		 */
		@Override
		public void onServingStatusChange(final ClientServingStatus status) {
			// TODO Auto-generated method stub
			mStatusTv.post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					switch (status) {
					case clientServingStatusRobot: // 机器人
					case clientServingStatusQueue: // 排队等待，机器人
						mStatusTv.setText("状态：机器人服务中");
						break;
					case clientServingStatusWorker: // 人工坐席
						mStatusTv.setText("状态：" + V5ClientConfig.getInstance(getApplicationContext()).getWorkerName() + "为您服务");
						break;
					case clientServingStatusInTrust: // 坐席交给机器人托管
						mStatusTv.setText("状态：机器人托管中");
						break;
					}
				}
			});
		}
		
		@Override
		public void onMessage(final V5Message message) {
			// TODO Auto-generated method stub
			Logger.i(TAG, "onMessage:" + message.getDefaultContent(MainActivity.this));
			mStatusTv.post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					mLogTv.setText("收到回复：" + message.getDefaultContent(MainActivity.this));
				}
			});
		}
		
		@Override
		public void onMessage(String json) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onError(final V5KFException error) {
			// TODO Auto-generated method stub
			Logger.e(TAG, "onError " + error.toString());
			mStatusTv.post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					mStatusTv.setText("状态：已断开");
					
					// 错误处理，以及重连
					if (!V5ClientAgent.isConnected()) {
						switch (error.getStatus()) {
						case ExceptionNoNetwork: // 无网络连接，需要检查网络
							
							break;
						case ExceptionConnectionError: // 连接异常（突然断网或者其他未知清空），会自动重连
							//V5ClientAgent.getInstance().reconnect(); // 未自动重连成功则弹出提示框手动重连，限定重连次数，避免循环
							break;
						case ExceptionNotConnected: // 网络断开（发送消息时的反馈|网络断开时）
							
							break;
						case ExceptionNotInitialized: // 尚未初始化SDK
							
							break;
						case ExceptionWSAuthFailed: // 账号认证失败或者过期
							// V5ClientAgent.getInstance().reconnect(); // 未自动重连成功则弹出提示框手动重连，需限定重连次数，避免循环
							break;
						case ExceptionSocketTimeout: // 响应超时
							// 超时时间 V5ClientConfig.SOCKET_TIMEOUT
							break;
						default:
							break;
						}
					}
				}
			});
		}
		
		@Override
		public void onConnect() {
			// TODO Auto-generated method stub
			mStatusTv.post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					mStatusTv.setText("状态：已连接");
				}
			});			
		}
	};
}
