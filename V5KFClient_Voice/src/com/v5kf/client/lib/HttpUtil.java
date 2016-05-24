package com.v5kf.client.lib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.v5kf.client.lib.entity.V5Message;
import com.v5kf.client.lib.entity.V5MessageDefine;

public class HttpUtil {
	
	private static final int MAX_PIC_SIZE = 500; // 最大上传图片大小
	private static final int MIN_PIC_SIZE_UNCOMPRESS = 200; // 最低不压缩图片大小
	protected static final String TAG = "HttpUtil";
	
	public enum HttpMethod {
		POST,
		GET
	}	

	public static void post(final String url, final String entity, final HttpResponseHandler handler) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Content-Type", "application/json");
				headers.put("Origin", "http://chat.v5kf.com");
				// 获得上传信息的字节大小以及长度
	 			StringBuffer buffer = new StringBuffer();
	 			if (entity != null) {
	 				buffer.append(entity);
	 			}
	 			byte[] myData = buffer.toString().getBytes();
				httpSync(url, HttpMethod.POST, myData, headers, handler);
			}
		}).start();
	}
	
	public static void get(final String url, final HttpResponseHandler handler) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Content-Type", "application/json");
				headers.put("Origin", "http://chat.v5kf.com");
				httpSync(url, HttpMethod.GET, null, headers, handler);
			}
		}).start();
	}

	public static void getPicService(final String url, final String auth,
			final HttpResponseHandler httpResponseHandler) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Authorization", auth);
				headers.put("Content-Type", "application/json");
				headers.put("Origin", "http://chat.v5kf.com");
				httpSync(url, HttpMethod.GET, null, headers, httpResponseHandler);
			}
		}).start();
	}

//	public static void postLocalImage(final String url, final String filePath,
//			final String authorization, 
//			final HttpResponseHandler httpResponseHandler) {
//		new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				String BOUNDARY = "----" + UUID.randomUUID().toString(); // 边界标识 随机生成
//				final String PREFIX = "--", LINE_END = "\r\n";
//				final String CONTENT_TYPE = "multipart/form-data";
//				Map<String, String> headers = new HashMap<String, String>();
//				File file = new File(filePath);  
//				headers.put("Authorization", authorization);
//                headers.put("Connection", "keep-alive");
//                headers.put("Host", "web.image.myqcloud.com");
//                headers.put("Content-Type", CONTENT_TYPE + ";boundary="
//                    + BOUNDARY);
//                
//                String contentType = "image/jpeg";
//                StringBuffer sb = new StringBuffer();
//                sb.append(PREFIX);
//                sb.append(BOUNDARY);
//                sb.append(LINE_END);                
//                sb.append("Content-Disposition: form-data; name=\"filecontent\"; filename=\"" 
//                		+ file.getName() + "\"" + LINE_END);
//                sb.append("Content-Type: " + contentType + LINE_END);
//                sb.append(LINE_END);
//                
//                byte[] myData = sb.toString().getBytes();
//                
//                // 读取文件转为字节流
//                Logger.d("HttpUtil", "FileSize>>>>>>>:" + V5Util.getFileSize(file));
//                byte[] b = null;
//                if (V5Util.getFileSize(file) / 1000 < MIN_PIC_SIZE_UNCOMPRESS) {
//	                try {
//	                	FileInputStream stream = new FileInputStream(file);
//		                ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
//		                b = new byte[1024];
//		                int n;
//		                while ((n = stream.read(b)) != -1)
//		                    out.write(b, 0, n);
//		                stream.close();
//		                out.close();
//		                b = out.toByteArray();
//		                Logger.d("HttpUtil", "SourceFileSize>>>:" + b.length);
//		            } catch (IOException e) {
//		            	e.printStackTrace();
//		            }
//                } else {
//                	b = V5Util.compressImageToByteArray(V5Util.getCompressBitmap(filePath), MAX_PIC_SIZE);
//                    Logger.d("HttpUtil", "CompressSize>>>：" + b.length);
//                }
//				myData = byteAppend(myData, b);
//                byte[] end_data = (LINE_END + PREFIX + BOUNDARY + PREFIX + LINE_END)
//                        .getBytes();
//                myData = byteAppend(myData, end_data);
////                DataInputStream in = new DataInputStream(new FileInputStream(file));  
////                int bytes = 0;  
////                byte[] myData = new byte[1024];  
////                while ((bytes = in.read(bufferOut)) != -1) {  
////                    out.write(bufferOut, 0, bytes);  
////                }
//				
//				httpSync(url, HttpMethod.POST, myData, headers, httpResponseHandler);
//			}
//		}).start();
//	}
	
	public static void httpSync(String path, HttpMethod method, byte[] myData, Map<String, String> headers, HttpResponseHandler handler) {
		URL url;
		try {
			if (V5ClientConfig.USE_HTTPS) {
				if (path.startsWith("http://")) {
					// http头替换成https头
					StringBuffer str=new StringBuffer(path);
					str.replace(0, 7, "https://"); 
					path = str.toString();
					Logger.w("HttpUtil", path);
				} else if (path.startsWith("https://")) {
					// 无需改动
				} else {
					// 添加https头
					path = "https://" + path;
				}
			}

			Logger.d("HttpUtil", "[httpSync] path:" + path);
			url = new URL(path);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(V5ClientConfig.SOCKET_TIMEOUT);
			urlConnection.setReadTimeout(V5ClientConfig.SOCKET_TIMEOUT);
			urlConnection.setDoInput(true);// 表示从服务器获取数据
			if (method == HttpMethod.POST) {
				urlConnection.setRequestMethod("POST");
				urlConnection.setDoOutput(true);// 表示向服务器写数据
			} else if (method == HttpMethod.GET) {
				urlConnection.setRequestMethod("GET");
			}
			// 设置请求的头  
//			urlConnection.setRequestProperty("Connection", "keep-alive");
//            urlConnection.setRequestProperty("origin", V5ClientConfig.ORIGIN);
//            urlConnection.setRequestProperty("Charset","UTF-8");
//            urlConnection.setRequestProperty("Content-Type",  
//                    "application/json");
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}
            
            if (HttpMethod.POST == method && myData != null) { // POST输出数据
	            urlConnection.setRequestProperty("Content-Length",  
	                    String.valueOf(myData.length));
	            Logger.d("HttpUtil", "Content-Length:" + String.valueOf(myData.length));
				// 获得输出流,向服务器输出数据
				OutputStream outputStream = urlConnection.getOutputStream();
				outputStream.write(myData);
				outputStream.flush();
				outputStream.close();
            }
			int responseCode = urlConnection.getResponseCode();
			if (responseCode == 200) {
				InputStream is = urlConnection.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] data = new byte[1024];
				int len = 0;
				String result = "";
				if (is != null) {
					try {
						while ((len = is.read(data)) != -1) {
							baos.write(data, 0, len);
						}
						// 释放资源  
		                is.close();
		                baos.close();
		                
						result = new String(baos.toByteArray(), "UTF-8");
						if (handler != null) {
							handler.onSuccess(responseCode, result);
							return;
						}
					} catch (IOException e) {
						e.printStackTrace();
						if (handler != null) {
							handler.onFailure(responseCode, e.getMessage());
							return;
						}
					}
				}
			}
			if (handler != null) {
				handler.onFailure(responseCode, "no InputStream be read");
				return;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			if (handler != null) {
				handler.onFailure(-11, e.getMessage());
				return;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			if (handler != null) {
				handler.onFailure(-12, e.getMessage());
				return;
			}
		} catch (ProtocolException e) {
			e.printStackTrace();
			if (handler != null) {
				handler.onFailure(-13, e.getMessage());
				return;
			}
		} catch (SocketTimeoutException e) {
			if (handler != null) {
				handler.onFailure(-10, "<SocketTimeoutException> " + e.getMessage());
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			if (handler != null) {
				handler.onFailure(-14, e.getMessage());
				return;
			}
		}
	}
	
	public static byte[] byteAppend(byte[] byte_1, byte[] byte_2){  
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];  
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);  
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);  
        return byte_3;  
    }
	
	/**
	 * 获取响应字符串(阻塞)
	 * 
	 * @param path
	 *            路径
	 * @param parameters
	 *            参数
	 * @return 响应字符串
	 */
/*	public static void getSync(String path, Map<String, String> parameters, Map<String, String> headers, HttpResponseHandler handler) {
		StringBuffer buffer = new StringBuffer();
		URL url;
		try {
			if (parameters != null && !parameters.isEmpty()) {
				for (Map.Entry<String, String> entry : parameters.entrySet()) {
					// 完成转码操作
					buffer.append(entry.getKey()).append("=")
							.append(URLEncoder.encode(entry.getValue(), "UTF-8")).append("&");
				}
				buffer.deleteCharAt(buffer.length() - 1);
			}
			url = new URL(path);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(30000);
			urlConnection.setReadTimeout(30000);
//			urlConnection.setRequestMethod("GET"); // 默认即GET
			urlConnection.setDoInput(true); // 表示从服务器获取数据
//			urlConnection.setDoOutput(true); // 表示向服务器写数据,POST才要,GET不用
//			// 获得上传信息的字节大小以及长度
//			byte[] mydata = buffer.toString().getBytes();
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}
			// 表示设置请求体的类型是文本类型
			urlConnection.setRequestProperty("origin", V5ClientConfig.ORIGIN);
//			urlConnection.setRequestProperty("Content-Type",
//					"application/json");
//			urlConnection.setRequestProperty("Content-Length", String.valueOf(mydata.length));
//			urlConnection.connect();
//			// 获得输出流,向服务器输出数据
//			OutputStream outputStream = urlConnection.getOutputStream();
//			outputStream.write(mydata, 0, mydata.length);
//			outputStream.close();
			int responseCode = urlConnection.getResponseCode();
			if (responseCode == 200) {
				InputStream is = urlConnection.getInputStream();
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				byte[] data = new byte[1024];
				int len = 0;
				String result = "";
				if (is != null) {
					try {
						while ((len = is.read(data)) != -1) {
							os.write(data, 0, len);
						}
						result = new String(os.toByteArray(), "UTF-8");
						// 释放资源  
		                is.close();  
		                os.close();
						if (handler != null) {
							handler.onSuccess(responseCode, result);
							return;
						}
					} catch (IOException e) {
						e.printStackTrace();
						if (handler != null) {
							handler.onFailure(responseCode, e.getMessage());
							return;
						}
					}
				}
			}
			if (handler != null) {
				handler.onFailure(responseCode, "no InputStream be read");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			if (handler != null) {
				handler.onFailure(0, e.getMessage());
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			if (handler != null) {
				handler.onFailure(0, e.getMessage());
			}
		} catch (ProtocolException e) {
			e.printStackTrace();
			if (handler != null) {
				handler.onFailure(0, e.getMessage());
			}
		} catch (IOException e) {
			e.printStackTrace();
			if (handler != null) {
				handler.onFailure(0, e.getMessage());
			}
		}
	}
*/
	
	
	public static void postLocalImage(final String url, final String filePath,
			final String authorization, 
			final String magicContext,
			final HttpResponseHandler httpResponseHandler) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				String BOUNDARY = "----" + UUID.randomUUID().toString(); // 边界标识 随机生成
				final String PREFIX = "--", LINE_END = "\r\n";
				final String CONTENT_TYPE = "multipart/form-data";
				
				Map<String, String> headers = new HashMap<String, String>();
				File file = new File(filePath);  
				headers.put("Authorization", authorization);
                headers.put("Connection", "keep-alive");
                headers.put("Host", "web.image.myqcloud.com");
                headers.put("Content-Type", CONTENT_TYPE + "; boundary="
                    + BOUNDARY);
                
                String contentType = "image/jpeg";
                String firstBoundary = PREFIX + BOUNDARY + LINE_END;
                String commonBoundary = LINE_END + firstBoundary;
                String lastBoundary = LINE_END + PREFIX + BOUNDARY + PREFIX + LINE_END;
                
                byte[] myData = firstBoundary.getBytes(); // first boundary
                
                StringBuffer fileContent = new StringBuffer();                
                fileContent.append("Content-Disposition: form-data; name=\"FileContent\"; filename=\"" 
                		+ file.getName() + "\"" + LINE_END);
                fileContent.append("Content-Type: " + contentType + LINE_END);
                fileContent.append(LINE_END);
                
                myData = byteAppend(myData, fileContent.toString().getBytes()); // Content-Disposition
                
                // 读取文件转为字节流
                Logger.d(TAG, "FileSize>>>>>>>:" + V5Util.getFileSize(file));
                byte[] b = null;
                if (V5Util.getFileSize(file) / 1000 < MIN_PIC_SIZE_UNCOMPRESS) {
	                try {
	                	FileInputStream stream = new FileInputStream(file);
		                ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		                b = new byte[1024];
		                int n;
		                while ((n = stream.read(b)) != -1)
		                    out.write(b, 0, n);
		                stream.close();
		                out.close();
		                b = out.toByteArray();
		                Logger.d(TAG, "SourceFileSize>>>:" + b.length);
		            } catch (IOException e) {
		            	e.printStackTrace();
		            }
                } else {
                	b = V5Util.compressImageToByteArray(V5Util.getCompressBitmap(filePath), MAX_PIC_SIZE);
                    Logger.d(TAG, "CompressSize>>>：" + b.length);
                }
                Logger.i(TAG, "Image content length>>>：" + b.length);
                String md5 = null;
//				try { // 取消MD5添加
//					md5 = V5Util.md5(b);
//				} catch (NoSuchAlgorithmException e) {
//					e.printStackTrace();
//				}
				myData = byteAppend(myData, b); // image content
				
				if ((magicContext == null || magicContext.isEmpty()) &&
						(md5 == null || md5.isEmpty())) {
	                myData = byteAppend(myData, lastBoundary.getBytes());
				} else {
	                if (magicContext != null && !magicContext.isEmpty()) {
	                	myData = byteAppend(myData, commonBoundary.getBytes());// 间隔 boundary
	                	// 添加MagicContext
	                    StringBuffer mcContent = new StringBuffer();              //form-data; name="field1"
	                    mcContent.append("Content-Disposition: form-data; name=\"MagicContext\"" + LINE_END + LINE_END);
	                    mcContent.append(magicContext);
	                    myData = byteAppend(myData, mcContent.toString().getBytes());
	                }
	                if (md5 != null && !md5.isEmpty()) {
	                	myData = byteAppend(myData, commonBoundary.getBytes());// 间隔 boundary
		                // 添加MD5
		                StringBuffer md5Content = new StringBuffer();
		                md5Content.append("Content-Disposition: form-data; name=\"Md5\"" + LINE_END + LINE_END);
		                md5Content.append(md5);
		                myData = byteAppend(myData, md5Content.toString().getBytes());
	                }
	                myData = byteAppend(myData, lastBoundary.getBytes()); // last boundary
				}
//                DataInputStream in = new DataInputStream(new FileInputStream(file));  
//                int bytes = 0;  
//                byte[] myData = new byte[1024];  
//                while ((bytes = in.read(bufferOut)) != -1) {  
//                    out.write(bufferOut, 0, bytes);  
//                }
				
				httpSync(url, HttpMethod.POST, myData, headers, httpResponseHandler);
			}
		}).start();
	}

	public static void postLocalMedia(final V5Message message, final String filePath, final String url,
			final String authorization, 
			final String magicContext,
			final HttpResponseHandler httpResponseHandler) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				String BOUNDARY = "----" + UUID.randomUUID().toString(); // 边界标识 随机生成
				final String PREFIX = "--", LINE_END = "\r\n";
				final String CONTENT_TYPE = "multipart/form-data";
				
				Map<String, String> headers = new HashMap<String, String>();
				File file = new File(filePath);  
				headers.put("Authorization", authorization);
				headers.put("Connection", "keep-alive");
				headers.put("Content-Type", "multipart/form-data");
				headers.put("Host", "web.file.myqcloud.com");
				headers.put("Content-Type", CONTENT_TYPE + "; boundary="
						+ BOUNDARY);
				
				String firstBoundary = PREFIX + BOUNDARY + LINE_END;
				String commonBoundary = LINE_END + firstBoundary;
				String lastBoundary = LINE_END + PREFIX + BOUNDARY + PREFIX + LINE_END;
				
				/* 添加op->upload */
				byte[] myData = firstBoundary.getBytes(); // first boundary
				StringBuffer opContent = new StringBuffer();              //form-data; name="field1"
				opContent.append("Content-Disposition: form-data; name=\"op\"" + LINE_END + LINE_END);
				opContent.append("upload");
				myData = byteAppend(myData, opContent.toString().getBytes());
				
				/* 添加File data */
				myData = byteAppend(myData, commonBoundary.getBytes());// 间隔 boundary
				StringBuffer fileContent = new StringBuffer();
				String contentType = "image/jpeg";
				if (message.getMessage_type() == V5MessageDefine.MSG_TYPE_IMAGE) {
					contentType = "image/jpeg";
				} else if (message.getMessage_type() == V5MessageDefine.MSG_TYPE_VOICE) {
					contentType = "audio/amr";
				} else {
					// TODO 更多类型
				}
				fileContent.append("Content-Disposition: form-data; name=\"FileContent\"; filename=\"" 
						+ file.getName() + "\"" + LINE_END);
				fileContent.append("Content-Type: " + contentType + LINE_END);
				fileContent.append(LINE_END); 
				myData = byteAppend(myData, fileContent.toString().getBytes()); // Content-Disposition
				// 读取文件转为字节流
				Logger.d(TAG, "FileSize>>>>>>>:" + V5Util.getFileSize(file) + " of:" + file.getAbsolutePath());
				byte[] b = null;
				try {
					FileInputStream stream = new FileInputStream(file);
					ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
					b = new byte[1024];
					int n;
					while ((n = stream.read(b)) != -1)
						out.write(b, 0, n);
					stream.close();
					out.close();
					b = out.toByteArray();
					Logger.d(TAG, "SourceFileSize>>>:" + b.length);
				} catch (IOException e) {
					e.printStackTrace();
				}
//				if (V5Util.getFileSize(file) / 1000 < MIN_PIC_SIZE_UNCOMPRESS) {
//					try {
//						FileInputStream stream = new FileInputStream(file);
//						ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
//						b = new byte[1024];
//						int n;
//						while ((n = stream.read(b)) != -1)
//							out.write(b, 0, n);
//						stream.close();
//						out.close();
//						b = out.toByteArray();
//						Logger.d(TAG, "SourceFileSize>>>:" + b.length);
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				} else {
//					b = V5Util.compressImageToByteArray(V5Util.getCompressBitmap(filePath), MAX_PIC_SIZE);
//					Logger.d(TAG, "CompressSize>>>：" + b.length);
//				}
				if (b != null) {
					Logger.i(TAG, "Media content length>>>：" + b.length);
				} else {
					Logger.e(TAG, "Media data is null !!!");
					if (httpResponseHandler != null) {
						httpResponseHandler.onFailure(-1, "Rcord not permited, Media data is null!");
						return;
					}
					return;
				}
				myData = byteAppend(myData, b); // image content
				
				myData = byteAppend(myData, lastBoundary.getBytes()); // last boundary
//				// 添加sha
//				String sha = null;
////				try { // 取消SHA添加
////					sha = V5Util.sha(b);
////				} catch (NoSuchAlgorithmException e) {
////					e.printStackTrace();
////				}
//				if ((sha == null || sha.isEmpty())) {
//					myData = byteAppend(myData, lastBoundary.getBytes()); // last boundary
//				} else {
//					if (sha != null && !sha.isEmpty()) {
//						myData = byteAppend(myData, commonBoundary.getBytes());// 间隔 boundary
//						// 添加MD5
//						StringBuffer md5Content = new StringBuffer();
//						md5Content.append("Content-Disposition: form-data; name=\"sha\"" + LINE_END + LINE_END);
//						md5Content.append(sha);
//						myData = byteAppend(myData, md5Content.toString().getBytes());
//					}
//					myData = byteAppend(myData, lastBoundary.getBytes()); // last boundary
//				}
				
//                DataInputStream in = new DataInputStream(new FileInputStream(file));  
//                int bytes = 0;  
//                byte[] myData = new byte[1024];  
//                while ((bytes = in.read(bufferOut)) != -1) {  
//                    out.write(bufferOut, 0, bytes);  
//                }
				
				httpSync(url, HttpMethod.POST, myData, headers, httpResponseHandler);
			}
		}).start();
	}
}
