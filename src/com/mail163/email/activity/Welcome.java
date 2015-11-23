package com.mail163.email.activity;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import com.mail163.email.AccountBackupRestore;
import com.mail163.email.Email;
import com.mail163.email.R;
import com.mail163.email.Utility;
import com.mail163.email.Email.Global;
import com.mail163.email.activity.setup.AccountSetupBasics;
import com.mail163.email.provider.EmailContent;
import com.mail163.email.util.PhoneUtil;
import com.mail163.email.util.Utiles;
import com.mobclick.android.MobclickAgent;

public class Welcome extends Activity {
	private static final int DIALOG_IMSI = 1;
	private static final int DIALOG_PASSWORD = 2;
	private DeleteApk mDeleteApkTask;

	/**
	 * Launch this activity. Note: It's assumed that this activity is only
	 * called as a means to 'reset' the UI state; Because of this, it is always
	 * launched with FLAG_ACTIVITY_CLEAR_TOP, which will drop any other
	 * activities on the stack (e.g. AccountFolderList or MessageList).
	 */
	public static void actionStart(Activity fromActivity) {
		Intent i = new Intent(fromActivity, Welcome.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		fromActivity.startActivity(i);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		init();
		
		if(!Global.set_security_password.equals("")){
			showDialog(DIALOG_PASSWORD);
		}else{
			onstartCreate();
		}

	}
	@Override
	public void onResume() {
	    super.onResume();
	    MobclickAgent.onResume(this);
	}
	@Override
	public void onPause() {
	    super.onPause();
	    MobclickAgent.onPause(this);
	}
	public void onstartCreate(){
		if (Global.set_security == 0) {//表示IMSI安全开关 ,默认关 ==1
			if (PhoneUtil.getModel().equalsIgnoreCase(Email.EMULATOR)) {
				Toast.makeText(this,
						getString(R.string.installation_mobile_message),
						Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			SharedPreferences sharedata = getSharedPreferences(
					Email.STORESETMESSAGE, 0);
			String imsi = sharedata.getString(Global.Save_IMSI, "null");
			if ("null".equals(imsi)) {
				saveIMSI(PhoneUtil.getImsi(this));
			} else {
				if (!PhoneUtil.getImsi(this).equals(imsi)) {
					showDialog(DIALOG_IMSI);
					return;
				}
			}
		}
	

		Email.setNotifyUiAccountsChanged(false);

		AccountBackupRestore.restoreAccountsIfNeeded(this);

		Cursor c = null;
		try {
			c = getContentResolver().query(EmailContent.Account.CONTENT_URI,
					EmailContent.Account.ID_PROJECTION, null, null, null);
			switch (c.getCount()) {
			case 0:
				AccountSetupBasics.actionNewAccount(this);
				break;
			// case 1:
			// c.moveToFirst();
			// long accountId =
			// c.getLong(EmailContent.Account.CONTENT_ID_COLUMN);
			// MessageList.actionHandleAccount(this, accountId,
			// Mailbox.TYPE_INBOX);
			// break;
			default:
				AccountFolderList.actionShowAccounts(this);
				break;
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}

		// In all cases, do not return to this activity
		finish();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Utility.cancelTaskInterrupt(mDeleteApkTask);
		mDeleteApkTask = null;
	}

	/**
	 * 初始化全局变量 实际工作中这个方法中serverVersion从服务器端获取，最好在启动画面的activity中执行
	 */
	public void init() {
		Utiles.fetchSetting(this);

		mDeleteApkTask = new DeleteApk();
		mDeleteApkTask.execute("");
		Utiles.getVersion(this);
	}

	private class DeleteApk extends AsyncTask<String, Void, Integer> {
		@Override
		protected Integer doInBackground(String... params) {
			File apkFile = getAPKInstallDir();
			if (apkFile != null) {
				apkFile.delete();
			}

			return 1;
		}
	}

	public File getAPKInstallDir() {
		File updateDir;
		File updateFile;
		if (android.os.Environment.MEDIA_MOUNTED.equals(android.os.Environment
				.getExternalStorageState())) {
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

		if (updateFile.exists()) {
			return updateFile;
		}
		return null;
	}

	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_IMSI:
			return createIMSICLEARDialog();
		case DIALOG_PASSWORD:
			return createPasswordDialog();
		}
		
		return super.onCreateDialog(id);
	}

	private Dialog createIMSICLEARDialog() {
		return new AlertDialog.Builder(this).setIcon(R.drawable.icon)
				.setMessage(R.string.imsi_change).setTitle(R.string.app_name)
				.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								Utiles.clearAccount(Welcome.this);
								dismissDialog(DIALOG_IMSI);
								finish();
							}
						}).setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								finish();
							}
						}).create();
	}
	private Dialog createPasswordDialog() {
		LayoutInflater factory = LayoutInflater.from(this);
        final EditText textView = (EditText)factory.inflate(R.layout.dialog_password_set, null);
		return new AlertDialog.Builder(this)
				.setMessage(getString(R.string.welcome_password_input))
				.setView(textView)
				.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								if(Global.set_security_password.equals(textView.getText().toString())){
									dismissDialog(DIALOG_PASSWORD);
									onstartCreate();
								}else{
									dismissDialog(DIALOG_PASSWORD);
									Toast.makeText(Welcome.this, getString(R.string.welcome_password_error), Toast.LENGTH_SHORT).show();
									finish();
								}

							}
						}).setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								finish();
							}
						}).create();
	}
	public void saveIMSI(String imsi) {
		Editor sharedata = getSharedPreferences(Email.STORESETMESSAGE, 0)
				.edit();
		sharedata.putString(Global.Save_IMSI, imsi);
		sharedata.commit();
	}
}
