package com.mail163.email.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpConnection;

public class HttpConectorThread extends Thread {
	private String httpUrl = "";
	private String fileUrl = "";
	private HttpConnection httpConn = null;
	private URLConnection uc = null;
	private InputStream is = null;
	private OutputStream os = null;
	private ByteArrayOutputStream baos = null;

	public boolean isStop = false;
	public long fileLength;
	public long startPos = 0; // 起始位置
	public long endPos; // 结束位置

	public HttpConectorThread(String httpUrl, String fileUrl) {
		this.httpUrl = httpUrl;
		this.fileUrl = fileUrl;
		isStop = true;
	}

	public synchronized void run() {
		try {
			if(httpUrl == null && httpUrl.equals("")){
				return;
			}
			URL url = new URL (httpUrl);
	        uc = url.openConnection();

			is = (InputStream)uc.getInputStream();
			os = openFileConnection(fileUrl);

			byte[] b = new byte[1024];
			endPos = uc.getContentLength();

			if (startPos < endPos) {
				int nRead = (int) (endPos - startPos);
				if (nRead > 1024) {
					nRead = 1024;
				}
				while (isStop && (nRead = is.read(b, 0, nRead)) > 0 && startPos < endPos) {
					write(os, b, 0, nRead);
					startPos = startPos + nRead;
					nRead = (int) (endPos - startPos);
					if (nRead > 1024) {
						nRead = 1024;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				closeAll();
			} catch (IOException ue) {
				e.printStackTrace();
			}
		} finally {
			try {
				closeAll();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 将网络字节数据缓冲至字节数组中一次性写入文件
	 */
	public void httpcontent() {
		try {
			is = uc.getInputStream();;
			baos = new ByteArrayOutputStream();

			int ch = 0;
			while ((ch = is.read()) != -1) {
				baos.write(ch);
			}
			byte[] byteData = baos.toByteArray();
			os = openFileConnection(fileUrl);
			os.write(byteData);
		} catch (IOException e) {
			e.printStackTrace();
			try {
				closeAll();
			} catch (IOException ue) {
				e.printStackTrace();
			}
		} finally {
			try {
				closeAll();
			} catch (IOException ue) {
				ue.printStackTrace();
			}
		}
	}

	private void closeAll() throws IOException {
		if (baos != null) {
			baos.close();
		}
		if (is != null) {
			is.close();
		}
		if (httpConn != null) {
			httpConn.close();
		}
	}

	public synchronized void write(OutputStream os, byte[] b, int nStart,
			int nLen) {
		try {
			os.write(b, nStart, nLen);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public OutputStream openFileConnection(String fileUrl) throws IOException {
		String dir = "/sdcard/yimail/skin";
		File filedir = new File(dir);
		File file = new File(fileUrl);
		if (!filedir.exists()) {
			filedir.mkdirs();
		}
		if (!file.exists()) {
			file.createNewFile();
		}
		OutputStream os = new FileOutputStream(file);

		return os;
	}

}
