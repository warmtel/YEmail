package com.mail163.email.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import com.mail163.email.Email;
import com.mail163.email.Logs;
import com.mail163.email.R;
import com.mail163.email.Email.Global;
import com.mail163.email.receiver.SoftUpdateReceiver;
import com.mail163.email.util.Utiles;
import com.mail163.email.util.ZipFileUtils;

public class UpdateService extends Service {
	private static final String tag = "UpdateService";
	// 为了防止频繁的通知导致应用吃紧，百分比增加3才通知一次
	private static int downloadRate = 3;

	private File updateDir = null;
	private File updateFile = null;

	private NotificationManager updateNotificationManager = null;
	private Notification updateNotification = null;

	private Intent updateIntent = null;
	private PendingIntent updatePendingIntent = null;

	private UpdateRunnable mUpdateRunnable;
	private Thread updateThread;

	private UpdateListHandler updateHandler;
	private class UpdateListHandler extends Handler {
		private static final int DOWNLOAD_COMPLETED = 0x11;
		private static final int DOWNLOAD_FAILD = 2;

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case DOWNLOAD_COMPLETED:
				Email.downFlag = true;
	
				updateNotification.flags = Notification.FLAG_AUTO_CANCEL;
				updateNotification.setLatestEventInfo(UpdateService.this,
						getString(R.string.download_over), "100%",
						updatePendingIntent);
				updateNotificationManager.notify(0, updateNotification);
				UpdateService.this.stopSelf();
				
//				try {
//					ZipFileUtils.upZipFile(updateFile, updateDir.getPath());
//				} catch (ZipException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				Utiles.delFolder(updateFile.getPath());
				break;
			case DOWNLOAD_FAILD:
				Email.downFlag = true;
	
				updateNotification.flags = Notification.FLAG_AUTO_CANCEL;
				updateNotification.setLatestEventInfo(UpdateService.this,
						getString(R.string.app_name),
						getString(R.string.soft_update_apkerror),
						updatePendingIntent);
				updateNotificationManager.notify(0, updateNotification);
				UpdateService.this.stopSelf();
				break;
			default:
				super.handleMessage(msg);
			}
			
		}

		public void doDownloadSuccess() {
			android.os.Message msg = android.os.Message.obtain();
			msg.what = DOWNLOAD_COMPLETED;
			sendMessage(msg);
		}

		public void doDownloadFail() {
			android.os.Message msg = android.os.Message.obtain();
			msg.what = DOWNLOAD_FAILD;
			sendMessage(msg);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		updateHandler = new UpdateListHandler();
		createFile();
		initNotification();
		startDownLoadThread();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}
	
	public void createFile() {
		try {
			if (android.os.Environment.MEDIA_MOUNTED
					.equals(android.os.Environment.getExternalStorageState())) {
				updateDir = new File(Environment.getExternalStorageDirectory(),
						Global.downloadDir);
				updateFile = new File(updateDir.getPath(), getResources()
						.getString(R.string.app_name)
						+ ".apk");
			} else {
				updateDir = new File(Environment.getDownloadCacheDirectory(),
						Global.downloadDir);
				updateFile = new File(updateDir.getPath(), getResources()
						.getString(R.string.app_name)
						+ ".apk");
			}
			if (!updateDir.exists()) {
				updateDir.mkdirs();
			}
			if (!updateFile.exists()) {
				updateFile.createNewFile();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void startDownLoadThread() {
		// 开启一个新的线程下载，如果使用Service同步下载，会导致ANR问题，Service本身也会阻塞
		mUpdateRunnable = new UpdateRunnable();
		updateThread = new Thread(mUpdateRunnable);
		updateThread.start();
	}

	public void initNotification() {
		updateNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		updateNotification = new Notification();
		// updateNotification.flags = Notification.FLAG_ONGOING_EVENT;
		updateNotification.flags = Notification.FLAG_AUTO_CANCEL;
		// 设置点击通知栏，广播apk包安装
		updateIntent = new Intent(this, SoftUpdateReceiver.class);
		updateIntent.setAction(Global.ACTION_INSTALL_SOFT);
		updateIntent.setData(Uri.fromFile(updateFile));
		
		updatePendingIntent = PendingIntent.getBroadcast(this, 0, updateIntent,0);

		// 设置通知栏显示内容
		updateNotification.icon = R.drawable.ic_list_inbox;
		updateNotification.tickerText = getString(R.string.download_start);
		updateNotification.setLatestEventInfo(this,getString(R.string.app_name), "0%", updatePendingIntent);
		// 发出通知
		updateNotificationManager.notify(0, updateNotification);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public long downloadUpdateFile(String downloadUrl, File saveFile)
			throws Exception {
		int downloadCount = 0;
		int currentSize = 0;
		long totalSize = 0;
		int updateTotalSize = 0;

		HttpURLConnection httpConnection = null;
		InputStream is = null;
		FileOutputStream fos = null;

		try {
			URL url = new URL(downloadUrl);
			
			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection
					.setRequestProperty("User-Agent", "PacificHttpClient");
			if (currentSize > 0) {
				httpConnection.setRequestProperty("RANGE", "bytes="
						+ currentSize + "-");
			}
			httpConnection.setConnectTimeout(10000);
			httpConnection.setReadTimeout(20000);
			updateTotalSize = httpConnection.getContentLength();
			if (httpConnection.getResponseCode() == 404) {
				throw new Exception("fail!");
			}

			is = httpConnection.getInputStream();
			fos = new FileOutputStream(saveFile, false);
			byte buffer[] = new byte[4096];
			int readsize = 0;
			while ((readsize = is.read(buffer)) > 0) {
				fos.write(buffer, 0, readsize);
				totalSize += readsize;

				if ((downloadCount == 0)
						|| (int) (totalSize * 100 / updateTotalSize)
								- downloadRate > downloadCount) {
					downloadCount += downloadRate;

					updateNotification.setLatestEventInfo(UpdateService.this,
							getString(R.string.downloading), (int) totalSize
									* 100 / updateTotalSize + "%",
							updatePendingIntent);
					updateNotificationManager.notify(0, updateNotification);

					Logs.d(Logs.LOG_MessageView, "downloading size  :"
							+ totalSize + ", updateTotalSize :"
							+ updateTotalSize);
				}
			}
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
			if (is != null) {
				is.close();
			}
			if (fos != null) {
				fos.close();
			}
		}
		return totalSize;
	}

	class UpdateRunnable implements Runnable {
		public void run() {
			try {
				long downloadSize = downloadUpdateFile(Global.softUpdateUri,updateFile);
				if (downloadSize > 0) {
					updateHandler.doDownloadSuccess();
				}
			} catch (Exception ex) {
				updateHandler.doDownloadFail();
				ex.printStackTrace();
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(updateThread.isAlive()){
			updateThread.interrupt();
		}
		if (mUpdateRunnable != null) {
			mUpdateRunnable = null;
		}
//		this.stopSelf();
	}

}
