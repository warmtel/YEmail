package com.mail163.email.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.webkit.URLUtil;

import com.mail163.email.Logs;
import com.mail163.email.net.AndroidHttpClient;

public class FileUtils {  

	/**  
     * LogCat TAG 标识  
     */  
    public static final String TAG = "FileUtils";   
       
    /**  
     * 读取应用程序私有文件.相对路径: /data/data/应用程序�?�?�?�?files/  
     * @param fileName 想要读取的文件名  
     * @param buffer 字节方式读取到缓�?�?�?�? 
     * @param context 关联的应用程序上下文  
     */  
    public static int  readFile(String fileName, byte[] buffer, Context context) {
		FileInputStream fis = null;

		try {
			fis = context.openFileInput(fileName);
			return fis.read(buffer);
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		} 
		return -1;
	}   
       
    /**  
     * 写入应用程序私有文件.相对路径: /data/data/应用程序�?�?�?�?files/  
     * @param fileName 想要写入的文件名,没有则创�?�?�?�? 
     * @param mode 写入模式  
     * @param buffer 要写入的字节数组  
     * @param context 关联的应用程序上下文  
     */  
    public static void writeFile(String fileName, int mode, byte[] buffer,
			Context context) {
		FileOutputStream fos = null;
		try {
			fos = context.openFileOutput(fileName, mode);
			fos.write(buffer);
			fos.flush();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IOException e) { 
			Log.e(TAG, e.getMessage(), e);
		}
	}  
    
    public static HashMap<String, Bitmap> DecompressesToBitmap(String urlPath,
			Context context) throws FileNotFoundException, IOException {
		HashMap<String, Bitmap> fileMap = new HashMap<String, Bitmap>();
		/* InputStream 下载文件 */
		InputStream is = null;
		AndroidHttpClient client = null;
		client = AndroidHttpClient.newInstance(context, "Android client");
		HttpGet request = new HttpGet(urlPath);
		HttpResponse response;
		try{
			response = client.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				is = response.getEntity().getContent();
			}
		}catch (IllegalArgumentException ex) {
			Logs.e(Logs.LOG_TAG, "Arg exception trying to execute request for "
					+ urlPath + " : " + ex);
			request.abort();
			if (client != null) {
				client.close();
				client = null;
			}
			return null;
		} catch (IOException ex) {
			Logs.e(Logs.LOG_TAG, "IOException trying to execute request for " + ex);
			ex.printStackTrace();
			request.abort();
			if (client != null) {
				client.close();
				client = null;
			}
			return null;
		}
		
		if (is == null) {
			if (client != null) {
				client.close();
				client = null;
			}
			throw new RuntimeException("stream is null");
		}
		ZipInputStream in = new ZipInputStream(is);
		ZipEntry entry;
		while ((entry = in.getNextEntry()) != null) {
			String zipName = entry.getName();

			if (zipName.endsWith("/")) {
				new File(zipName).mkdirs();
				continue;
			}

			/* 创建临时文件 */
			File myTempFile = null;
			FileOutputStream out = null;
			String sdcardState = Environment.getExternalStorageState();
			if (sdcardState.equals(Environment.MEDIA_MOUNTED)) {
				try {
					File rootFolder = new File("/sdcard/yimail/skintemp");
					if(!rootFolder.exists()){
						rootFolder.mkdir();
					}
					myTempFile = new File(rootFolder,zipName);
					if(!myTempFile.exists()){
						myTempFile.createNewFile();
					}
					out = new FileOutputStream(myTempFile);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}
			} else {
				out = context.openFileOutput(zipName, 0);
				myTempFile = new File(context.getFilesDir() + File.separator
						+ zipName);
			}
			
			IOUtils.copy(in, out);
			out.close();
			
			Bitmap mbitmap = BitmapFactory.decodeFile(myTempFile
					.getAbsolutePath());
			delFile(myTempFile.getPath());
			fileMap.put(zipName, mbitmap);
		}
		
		File rootFolder = new File("/sdcard/yimail/skintemp");
		if(rootFolder.exists()){
			rootFolder.delete();
		}
		in.close();
		is.close();
		if (client != null) {
			client.close();
			client = null;
		}
		return fileMap;
	}
 
    /**  
     * 读取应用程序静�1�?�?�资源文�?�?�?�? 
     * @param id 资源文件ID  
     * @param buffer 字节方式读取到缓�?�?�?�? 
     * @param context 关联的应用程序上下文  
     */  
    public static void readRawFile(int id, byte[] buffer, Context context) {
		InputStream is = null;
		try {
			is = context.getResources().openRawResource(id);
			is.read(buffer);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		} finally {
			Log.d(TAG, buffer.length + "bytes");
		}
	} 
    
    /* 取得远程文件，并暂存到本�?�?�?�?*/
    public static File getFileData(Context context, String url) throws Exception {
		if (!URLUtil.isNetworkUrl(url)) {
			Log.e(TAG,"error URL");
			return null;
		} 
		
		String pathParam = "";
		if(url != null){
			String[] params = url.split("&");
			for(int i=0;i<params.length;i++){
				if(params[i].indexOf("path=")>=0){
					pathParam = params[i];
					break;
				}
			}
		}
		String fileName = pathParam.substring(pathParam.lastIndexOf("/") + 1);

		/* 取得URL */
		URL myURL = new URL(url);
		/* 创建连接 */
		URLConnection conn = myURL.openConnection();
		conn.setConnectTimeout(15 * 60 * 1000);   
		conn.connect();
		/* InputStream 下载文件 */
		InputStream is = conn.getInputStream();
		if (is == null) {
			throw new RuntimeException("stream is null");
		}
		/* 创建临时文件 */
		File myTempFile = null;
		
		try {
			myTempFile = File.createTempFile(fileName, ".apk");
		} catch (Exception e) {
			Log.e(TAG, "error: " + e.getMessage(), e);
		}
		 
		FileOutputStream fos = null;

		// 针对没有/sdcard临时目录的情�?�?�?�?

		if (myTempFile == null || !myTempFile.exists()) {
			fos = context.openFileOutput(fileName, Activity.MODE_WORLD_READABLE);
			myTempFile = new File(context.getFilesDir() + File.separator + fileName);
		} else {
			fos = new FileOutputStream(myTempFile);
		}

		byte buf[] = new byte[128];
		do {
			int numread = is.read(buf);
			if (numread <= 0) {
				break;
			}
			fos.write(buf, 0, numread);
		} while (true);

		try {
			fos.flush();
			fos.close();
			is.close();
		} catch (Exception ex) {
			Log.e(TAG, "error: " + ex.getMessage(), ex);
		}
		return myTempFile;

	}

	/* 自定义删除文件方�?�?�?�?*/
    public static boolean delFile(String strFileName) {
    	if(strFileName == null){
    		return false;
    	}
		File myFile = new File(strFileName);
		if (myFile.exists()) {
			return myFile.delete();
		}
		return false;
	}

  
}
