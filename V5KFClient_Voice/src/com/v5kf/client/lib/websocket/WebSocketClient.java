package com.v5kf.client.lib.websocket;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.BasicNameValuePair;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.v5kf.client.lib.Logger;
import com.v5kf.client.lib.V5ClientConfig;

public class WebSocketClient {
    private static final String TAG = "WebSocketClient";

    private URI                      mURI;
    private Listener                 mListener;
    private Socket                   mSocket;
    private Thread                   mThread;
    private HandlerThread            mHandlerThread;
    private Handler                  mHandler;
    private List<BasicNameValuePair> mExtraHeaders;
    private HybiParser               mParser;
    protected boolean                mConnected;
    private int						 mStatusCode;
    private boolean					 mAlive;
    
    private Timer mConnectionTimer;
    private TimerTask mConnectionTask;

	private final Object mSendLock = new Object();

    private static TrustManager[] sTrustManagers;

    public static void setTrustManagers(TrustManager[] tm) {
        sTrustManagers = tm;
    }

    public WebSocketClient(URI uri, Listener listener, List<BasicNameValuePair> extraHeaders) {
        mURI          = uri;
        mListener		= listener;
        mExtraHeaders	= extraHeaders;
        mConnected		= false;
        mAlive			= true;
        mParser			= new HybiParser(this);

        mHandlerThread = new HandlerThread("websocket-thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public Listener getListener() {
        return mListener;
    }

    public void setAlive() {
    	mAlive = true;
    }
    
    public synchronized void connect() {
    	Logger.w(TAG, "connect start*****************");
    	Logger.w(TAG, "connect() mConnected:" + mConnected + " this:" + this.hashCode());
        if (mThread != null) {
        	Logger.w(TAG, "[connect] mThread is alive:" + mThread.isAlive());
        	return;
//        	if (mConnected) {
//        		return;
//        	} else {
//        		mThread.interrupt();
//        	}
        }

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                	Logger.d(TAG, "[connect] new Thread.run() -> timer");
                	mConnectionTimer = new Timer();
                	mConnectionTask = new TimerTask(){  
                    	public void run() {
                    		Logger.w(TAG, "[ConnectionTask] TimerTask.run()");
                    		if (mConnectionTimer != null) {
                        		mConnectionTimer.cancel();
                        		mConnectionTimer = null;
                        	}
                        	if (mConnectionTask != null) {
                            	mConnectionTask.cancel();
                        		mConnectionTask = null;
                        	}
                    		if (mConnected) {
                            	return;
                            	//mListener.onDisconnect(-7, "SocketTimeoutException");
                            }
                    		if (mThread != null) {
                    			mThread.interrupt();
                    			mThread = null;
                    		}
                        	mListener.onError(new SocketTimeoutException("Connection timed out"));
                    	};
                    };
                	try {
                		mConnectionTimer.schedule(mConnectionTask, V5ClientConfig.SOCKET_TIMEOUT);
                	} catch (Exception ex) {
                		ex.printStackTrace();
                		Logger.e(TAG, "Already in connection...");
                		return;
                	}
                	Logger.w(TAG, "[connect] new Thread.run()");
                    int port = (mURI.getPort() != -1) ? mURI.getPort() : ((mURI.getScheme().equals("wss") || mURI.getScheme().equals("https")) ? 443 : 80);

                    String path = TextUtils.isEmpty(mURI.getPath()) ? "/" : mURI.getPath();
                    if (!TextUtils.isEmpty(mURI.getQuery())) {
                        path += "?" + mURI.getQuery();
                    }

                    String originScheme = mURI.getScheme().equals("wss") ? "https" : "http";
                    URI origin = new URI(originScheme, "//" + mURI.getHost(), null);
                    SocketFactory factory = (mURI.getScheme().equals("wss") || mURI.getScheme().equals("https")) ? getSSLSocketFactory() : SocketFactory.getDefault();
                    // 建立连接前
                    Logger.d(TAG, "[connect] mSocket.connect before");
                    mSocket = factory.createSocket(mURI.getHost(), port);
                    // 建立连接
                    Logger.d(TAG, "[connect] mSocket.connect after");
                    
                    PrintWriter out = new PrintWriter(mSocket.getOutputStream());
                    String secretKey = createSecret();
                    out.print("GET " + path + " HTTP/1.1\r\n");
                    out.print("Upgrade: websocket\r\n");
                    out.print("Connection: Upgrade\r\n");
                    out.print("Host: " + mURI.getHost() + "\r\n");
                    out.print("Origin: " + origin.toString() + "\r\n");
                    out.print("Sec-WebSocket-Key: " + secretKey + "\r\n");
                    out.print("Sec-WebSocket-Version: 13\r\n");
                    if (mExtraHeaders != null) {
                        for (NameValuePair pair : mExtraHeaders) {
                            out.print(String.format("%s: %s\r\n", pair.getName(), pair.getValue()));
                        }
                    }
                    out.print("\r\n");
                    out.flush();

                    HybiParser.HappyDataInputStream stream = new HybiParser.HappyDataInputStream(mSocket.getInputStream());

                    // Read HTTP response status line.
                    StatusLine statusLine = parseStatusLine(readLine(stream));
                    if (statusLine != null) {
                    	mStatusCode = statusLine.getStatusCode();
                    }
                    if (statusLine == null) {
                        throw new HttpException("Received no reply from server.");
                    } else if (statusLine.getStatusCode() != HttpStatus.SC_SWITCHING_PROTOCOLS) {
                        throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
                    }

                    // Read HTTP response headers.
                    String line;
                    while (!TextUtils.isEmpty(line = readLine(stream))) {
                        Header header = parseHeader(line);
                        if (header.getName().equals("Sec-WebSocket-Accept")) {
                            String expected = expectedKey(secretKey);
                            if (expected == null) {
                                throw new Exception("SHA-1 algorithm not found");
                            } else if (!expected.equals(header.getValue())) {
                                throw new Exception("Invalid Sec-WebSocket-Accept, expected: " + expected + ", got: " + header.getValue());
                            }
                        }
                    }
                    Logger.w(TAG, "[connect] mConnectionTimer reset before");
                    if (mConnectionTimer != null) {
                		mConnectionTimer.cancel();
                		mConnectionTimer = null;
                    	if (mConnectionTask != null) {
                        	mConnectionTask.cancel();
                    		mConnectionTask = null;
                    	}
                	} else { // mConnectionTimer == null，说明已超时
                		Logger.w(TAG, "[connect] mConnectionTimer timeout already reset");
                		mConnected = false;
                		return;
                	}
                    mConnected = true;
                    Logger.w(TAG, "[connect] mConnectionTimer reset after");
                    mListener.onConnect();

                    // Now decode websocket frames.
                    mParser.start(stream);

                } catch (EOFException ex) {
                    Logger.e(TAG, "WebSocket EOF! > " + ex.getMessage());
                    if (mConnected) {
                    	mConnected = false;
                    	mListener.onDisconnect(-2, "EOFException");
                    } else {
                    	disconnect();
                    }

                } catch (SSLException ex) {
                    // Connection reset by peer
                	Logger.e(TAG, "Websocket SSL error! > " + ex.getMessage());
                	if (mConnected) {
                    	mConnected = false;
                    	mListener.onDisconnect(-3, "SSLException");
                    } else {
                    	disconnect();
                    }
                } catch (SocketTimeoutException ex) {
                	Logger.e(TAG, "Websocket SocketTimeoutException! > " + ex.getMessage());
                	if (mConnected) {
                    	mConnected = false;
                    	//mListener.onDisconnect(-7, "SocketTimeoutException");
                    } else {
                    	disconnect();
                    }
                	mListener.onError(ex);
                } catch (TimeoutException ex) {
                	Logger.e(TAG, "Websocket TimeoutException! > " + ex.getMessage());
                	if (mConnected) {
                    	mConnected = false;
                    	//mListener.onDisconnect(-7, "TimeoutException");
                    } else {
                    	disconnect();
                    }
                	mListener.onError(ex);
                } catch (Exception ex) {
                	Logger.e(TAG, "<<call onError>> StatusCode:" + mStatusCode);
                    disconnect();
                	mListener.onError(ex);
                } finally {
                	disconnect();
                	if (mConnectionTimer != null) {
                		mConnectionTimer.cancel();
                		mConnectionTimer = null;
                	}
                	if (mConnectionTask != null) {
                    	mConnectionTask.cancel();
                		mConnectionTask = null;
                	}
                    mThread = null;
                    Logger.w(TAG, "connect end*****************");
                }
            }
        });
        mThread.start();
    }
    
    public void disconnect() {
    	disconnect(-1, "Normal disconnect");
    }

    public void disconnect(final int code, final String desc) {
        if (mSocket != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mSocket != null) {
                        try {
                            mSocket.close();
                            if (mListener != null && isConnected()) {
                            	mConnected = false;
                            	mListener.onDisconnect(code, desc);
                            }
                        } catch (IOException ex) {
                            Log.d(TAG, "Error while disconnecting", ex);
                            mListener.onError(ex);
                        }
                        mSocket = null;
                    }
                    mConnected = false;
                }
            });
        }
        mConnected = false;
        if (mThread != null && mThread.isAlive()) {
        	mThread.interrupt();
        	mThread = null;
        }
    }

    public void send(String data) {
        sendFrame(mParser.frame(data));
    }

    public void send(byte[] data) {
        sendFrame(mParser.frame(data));
    }
    
    public void ping() {
    	mParser.ping("");
    	if (mAlive) {    		
    		mAlive = false;
    	} else {
    		Logger.w(TAG, "Ping not alive!");
//    		disconnect(-4, "Not Alive");
    	}
    }

    public void close(int code, String reason) {
    	mParser.close(code, reason);
    }

    public boolean isConnected() {
        return mConnected;
    }
    
    public int getStatusCode() {
		return mStatusCode;
	}

    private StatusLine parseStatusLine(String line) {
        if (TextUtils.isEmpty(line)) {
            return null;
        }
        return BasicLineParser.parseStatusLine(line, new BasicLineParser());
    }

    private Header parseHeader(String line) {
        return BasicLineParser.parseHeader(line, new BasicLineParser());
    }

    // Can't use BufferedReader because it buffers past the HTTP data.
    private String readLine(HybiParser.HappyDataInputStream reader) throws IOException {
        int readChar = reader.read();
        if (readChar == -1) {
            return null;
        }
        StringBuilder string = new StringBuilder("");
        while (readChar != '\n') {
            if (readChar != '\r') {
                string.append((char) readChar);
            }

            readChar = reader.read();
            if (readChar == -1) {
                return null;
            }
        }
        return string.toString();
    }

    private String expectedKey(String secret) {
        //concatenate, SHA1-hash, base64-encode
        try {
            final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            final String secretGUID = secret + GUID;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(secretGUID.getBytes());
            return Base64.encodeToString(digest, Base64.DEFAULT).trim();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String createSecret() {
        byte[] nonce = new byte[16];
        for (int i = 0; i < 16; i++) {
            nonce[i] = (byte) (Math.random() * 256);
        }
        return Base64.encodeToString(nonce, Base64.DEFAULT).trim();
    }

    void sendFrame(final byte[] frame) {
    	if (frame == null) {
    		Logger.w(TAG, "Null frame byte array");
    		if (mListener != null) {
    			mListener.onDisconnect(1, "sendFrame error");
    		}
    		return;
    	}
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mSendLock) {
                    	if (mSocket != null) {
	                        OutputStream outputStream = mSocket.getOutputStream();
	                        outputStream.write(frame);
	                        outputStream.flush();
                    	} else {
                    		Logger.e(TAG, "mSocket Nullpoint error!");
                    		disconnect();
                    	}
                    }
                } catch (IOException e) {
                    mListener.onError(e);
                } catch (NullPointerException e) { // send Ping 异常
                	e.printStackTrace();
                	mListener.onError(e);
                }
            }
        });
    }

    public interface Listener {
        public void onConnect();
        public void onMessage(String message);
        public void onMessage(byte[] data);
        public void onDisconnect(int code, String reason);
        public void onError(Exception error);
    }

    private SSLSocketFactory getSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, sTrustManagers, null);
        return context.getSocketFactory();
    }
}
