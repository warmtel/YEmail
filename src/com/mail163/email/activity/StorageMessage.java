package com.mail163.email.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import com.mail163.email.Logs;
import com.mail163.email.R;
import com.mail163.email.Email.Global;
import com.mail163.email.util.Utiles;

public class StorageMessage extends Activity {
	public static final String Tag = "HelpAboutActivity";
	private TextView sdAllStorage;
	private TextView sdAvailableStorage;
	private TextView dataStoreage;
	private TextView dataAvailableStorage;
	private TextView databaseStorage;
	// private TextView accountDataStorage;
	// private TextView accountSdcardStorage;
	private TextView netdata;

	public static void actionStorageMessage(Context context) {
		Intent i = new Intent(context, StorageMessage.class);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(Global.skinName.contains(Global.whiteSkin)){
			setTheme(R.style.Default);
        }else{
        	setTheme(R.style.XTheme);
        }
		super.onCreate(savedInstanceState);
		setTitle(R.string.storage_result);
		setContentView(R.layout.storage_message);
		findView();
		// 存储卡

		sdAllStorage.setText(String.format(
				getString(R.string.sdcard_all_capacity), Utiles
						.getSdcardAllSize(this)));
		sdAvailableStorage.setText(String.format(
				getString(R.string.sdcard_available_capacity), Utiles
						.getSdcardAvailSize(this)));
		// 数据内存
		dataStoreage.setText(String.format(
				getString(R.string.data_all_capacity), Utiles
						.getTotalMemory(this)));
		dataAvailableStorage.setText(String.format(
				getString(R.string.data_available_capacity), Utiles
						.getAvailMemory(this)));

		// 数据库
		databaseStorage.setText(getDateBase());

		// 账户附件
		// accountDataStorage.setText(String.format(
		// getString(R.string.account_data), getAccountDateArea()));
		// accountSdcardStorage.setText(String.format(
		// getString(R.string.account_sdcard), getAccountSdcardArea()));

		// 网络流量
		netdata.setText(String.format(getString(R.string.netdata_capacity),
				Utiles.getNetStream(this)));
	}

	private long dataBaseSize;

	private void findView() {
		sdAllStorage = (TextView) findViewById(R.id.sdcard_capacity);
		sdAvailableStorage = (TextView) findViewById(R.id.sdcard_available);
		dataStoreage = (TextView) findViewById(R.id.data_capacity);
		dataAvailableStorage = (TextView) findViewById(R.id.data_available);
		databaseStorage = (TextView) findViewById(R.id.database_capacity);
		// accountDataStorage = (TextView) findViewById(R.id.account_data_area);
		// accountSdcardStorage = (TextView)
		// findViewById(R.id.account_sdcard_area);
		netdata = (TextView) findViewById(R.id.netdata);
	}

	private String getDateBase() {
		File dataBasePath1 = getDatabasePath("EmailProvider.db");
		long pdbLength = dataBasePath1.length();
		

		File dataBasePath2 = getDatabasePath("EmailProviderBody.db");
		long pbodydbLength = dataBasePath2.length();

		long total = pdbLength + pbodydbLength;

		dataBaseSize = total;
		return Formatter.formatFileSize(getBaseContext(), total);
	}

	private String getAccountDateArea() {
		String dateBaseDir = "//data//data//com.mail163.email//databases//";
		File dataBasePath = new File(dateBaseDir);
		
		StatFs dataBaseStatFs = new StatFs(dataBasePath.getPath());
		long blockSize = dataBaseStatFs.getBlockSize();
		long blocksCount = dataBaseStatFs.getBlockCount();
		long total = blockSize * blocksCount;

		long availableBlocksCount = dataBaseStatFs.getAvailableBlocks();
		long sdAvailable = availableBlocksCount * blockSize;

		long FreeBlocksCount = dataBaseStatFs.getFreeBlocks();
		long sdFree = FreeBlocksCount * blockSize;

		long usedSize = total - sdFree;

		Logs.d(Logs.LOG_MessageView, "usedSize :" + usedSize + " usedSize :"
				+ sdFree + " >> :" + (usedSize - dataBaseSize) + " sdFree :"
				+ sdFree);
		return Formatter.formatFileSize(getBaseContext(),
				(usedSize - dataBaseSize));
	}

	private String getAccountSdcardArea() {
		File dataBasePath = new File(Environment.getExternalStorageDirectory()
				.getPath()
				+ "/yimail/");
		if (!dataBasePath.exists()) {
			dataBasePath.mkdir();
		}
	
		StatFs dataBaseStatFs = new StatFs(dataBasePath.getPath());
		long blockSize = dataBaseStatFs.getBlockSize();
		long blocksCount = dataBaseStatFs.getBlockCount();
		long total = blockSize * blocksCount;

		long availableBlocksCount = dataBaseStatFs.getAvailableBlocks();
		long sdAvailable = availableBlocksCount * blockSize;

		long usedSize = total - sdAvailable;

		return Formatter.formatFileSize(getBaseContext(), usedSize);
	}

}
