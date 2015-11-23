package com.mail163.email.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mail163.email.AccountBackupRestore;
import com.mail163.email.Controller;
import com.mail163.email.Email;
import com.mail163.email.Email.Global;
import com.mail163.email.Logs;
import com.mail163.email.NoticeBean;
import com.mail163.email.R;
import com.mail163.email.SecurityPolicy;
import com.mail163.email.SoftUpdateBean;
import com.mail163.email.Utility;
import com.mail163.email.activity.setup.AccountSettings;
import com.mail163.email.activity.setup.AccountSetupBasics;
import com.mail163.email.bean.ShareContentBean;
import com.mail163.email.mail.MessagingException;
import com.mail163.email.mail.Store;
import com.mail163.email.net.SoftUpdateSaxFeedParser;
import com.mail163.email.net.SystemNoticeSaxFeedParser;
import com.mail163.email.provider.EmailContent;
import com.mail163.email.provider.EmailContent.Account;
import com.mail163.email.provider.EmailContent.Mailbox;
import com.mail163.email.provider.EmailContent.MailboxColumns;
import com.mail163.email.provider.EmailContent.Message;
import com.mail163.email.provider.EmailContent.MessageColumns;
import com.mail163.email.service.MailService;
import com.mail163.email.service.MusicService;
import com.mail163.email.service.UpdateService;
import com.mail163.email.util.Configs;
import com.mail163.email.util.PhoneUtil;
import com.mail163.email.util.Utiles;
import com.mobclick.android.MobclickAgent;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXTextObject;
import com.tencent.mm.sdk.openapi.WXWebpageObject;
import com.tencent.mm.sdk.platformtools.Util;

public class AccountFolderList extends ListActivity implements
		OnItemClickListener {
	private static final int DIALOG_REMOVE_ACCOUNT = 1;
	private static final int DIALOG_SOFT_UPDATE = 2;
	private static final int DIALOG_SOFT_UPDATE_NETERROR = 3;
	private static final int DIALOG_BACK = 4;
	private static final int DIALOG_SHARE_APP = 5;
	private static final String SKINSET = "com.mail163.email.activity.SKINSET";
	/* 是否重新加载皮肤 */
	private static boolean skinSet = false;
	/**
	 * Key codes used to open a debug settings screen.
	 */
	private static final int[] SECRET_KEY_CODES = { KeyEvent.KEYCODE_D,
			KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_U,
			KeyEvent.KEYCODE_G };
	private int mSecretKeyCodeIndex = 0;

	private static final String ICICLE_SELECTED_ACCOUNT = "com.android.email.selectedAccount";
	private EmailContent.Account mSelectedContextAccount;
	private ProgressDialog mUpdateSoftDialog;
	private ListView mListView;
	private ProgressBar mProgressIcon;

	private AccountsAdapter mListAdapter;

	private SoftUpdate mSoftUpdateTask;
	private AutoSoftUpdate mAutoSoftUpdateTask;
	private LoadAccountsTask mLoadAccountsTask;
	private DeleteAccountTask mDeleteAccountTask;
	private NoticeTask mNoticeTask;

	private MessageListHandler mHandler;
	private ControllerResults mControllerCallback;

	/**
	 * Reduced mailbox projection used by AccountsAdapter
	 */
	public final static int MAILBOX_COLUMN_ID = 0;
	public final static int MAILBOX_DISPLAY_NAME = 1;
	public final static int MAILBOX_ACCOUNT_KEY = 2;
	public final static int MAILBOX_TYPE = 3;
	public final static int MAILBOX_UNREAD_COUNT = 4;
	public final static int MAILBOX_FLAG_VISIBLE = 5;
	public final static int MAILBOX_FLAGS = 6;

	public final static String[] MAILBOX_PROJECTION = new String[] {
			EmailContent.RECORD_ID, MailboxColumns.DISPLAY_NAME,
			MailboxColumns.ACCOUNT_KEY, MailboxColumns.TYPE,
			MailboxColumns.UNREAD_COUNT, MailboxColumns.FLAG_VISIBLE,
			MailboxColumns.FLAGS };

	private static final String FAVORITE_COUNT_SELECTION = MessageColumns.FLAG_FAVORITE
			+ "= 1";

	private static final String MAILBOX_TYPE_SELECTION = MailboxColumns.TYPE
			+ " =?";

	private static final String MAILBOX_ID_SELECTION = MessageColumns.MAILBOX_KEY
			+ " =?";

	private static final String[] MAILBOX_SUM_OF_UNREAD_COUNT_PROJECTION = new String[] { "sum("
			+ MailboxColumns.UNREAD_COUNT + ")" };

	private static final String MAILBOX_INBOX_SELECTION = MailboxColumns.ACCOUNT_KEY
			+ " =?"
			+ " AND "
			+ MailboxColumns.TYPE
			+ " = "
			+ Mailbox.TYPE_INBOX;

	private static final int MAILBOX_UNREAD_COUNT_COLUMN_UNREAD_COUNT = 0;
	private static final String[] MAILBOX_UNREAD_COUNT_PROJECTION = new String[] { MailboxColumns.UNREAD_COUNT };

	// ===============PopupMenu==============
	private PopupWindow popup;
	private GridView mMenuContentGridView;
	private TextView mTitleOption1, mTitleOption2, mTitleOption3;
	private int mTitleMenuIndex;

	public String[] mOptionTitle;

	public String[] mOptionTextArray1;
	public int[] mOptionImageArray1;

	public String[] mOptionTextArraySet;
	public int[] mOptionImageArrayset;

	public String[] mOptionTextArrayTool;
	public int[] mOptionImageArrayTool;
	
	private GridView mTitleGridView;
	private MenuTitleAdapter mTitleAdapter;
	// ===============PopupMenu==============
	
	Email mAplication;
	/**
	 * Start the Accounts list activity. Uses the CLEAR_TOP flag which means
	 * that other stacked activities may be killed in order to get back to
	 * Accounts.
	 */
	public static void actionShowAccounts(Context context) {
		Intent i = new Intent(context, AccountFolderList.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(i);
	}

	public static void actionShowAccounts(Context context, boolean mSkinSet) {
		Intent i = new Intent(context, AccountFolderList.class);
		skinSet = mSkinSet;
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(i);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.account_folder_list);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.accountfolder_title);

		// 通过WXAPIFactory工厂，获取IWXAPI的实例
		mAplication = (Email)getApplicationContext();
		mAplication.api = WXAPIFactory.createWXAPI(this, Configs.APP_ID, false);
		mAplication.api.registerApp(Configs.APP_ID);
		
		mHandler = new MessageListHandler();
		mControllerCallback = new ControllerResults();
		mProgressIcon = (ProgressBar) findViewById(R.id.title_progress_icon);

		mListView = getListView();

		mListView.setItemsCanFocus(false);
		mListView.setOnItemClickListener(this);
		mListView.setLongClickable(true);
		registerForContextMenu(mListView);

		if (icicle != null && icicle.containsKey(ICICLE_SELECTED_ACCOUNT)) {
			mSelectedContextAccount = (Account) icicle.getParcelable(ICICLE_SELECTED_ACCOUNT);
		}

		((TextView) findViewById(R.id.title_left_text)).setText(R.string.app_name);

		initMenuResourse();
		initPopupMenu();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mSelectedContextAccount != null) {
			outState.putParcelable(ICICLE_SELECTED_ACCOUNT,mSelectedContextAccount);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
		skinSet = false;
		Controller.getInstance(getApplication()).removeResultCallback(mControllerCallback);
	}

	@Override
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
		
		if (Global.backDrawable == null || skinSet) {
			Global.backDrawable = Utiles.fetchBackDrawable(this);
			Global.skinName = Utiles.fetchSkinName(this);
			if (Global.backDrawable == null) {
				mListView.setBackgroundColor(getResources().getColor(R.color.background_cachhint));
				setTheme(R.style.XTheme);
				Global.skinName = "";
			}
		}
		if (null != Global.backDrawable) {
			if(Global.skinName.contains(Global.whiteSkin)){
				setTheme(R.style.Default);
	        }else{
	        	setTheme(R.style.XTheme);
	        }
			mListView.setBackgroundDrawable(Global.backDrawable);
		}
		
		NotificationManager notifMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notifMgr.cancel(1);

		Controller.getInstance(getApplication()).addResultCallback(mControllerCallback);

		// Exit immediately if the accounts list has changed (e.g. externally
		// deleted)
		if (Email.getNotifyUiAccountsChanged()) {
			Welcome.actionStart(this);
			finish();
			return;
		}

		updateAccounts();
		// TODO: What updates do we need to auto-trigger, now that we have
		// mailboxes in view?
		if (Global.shortCutFlag && Global.set_shortcut == 0) { // 0表示快捷方式开关打开
			Global.shortCutFlag = false;
			if (!isCreateShortcut()) {
				// createShortcutAlertDialog();
				addShortcut();
			}
		}
		if (Global.localVersion <= 0) {
			Utiles.getVersion(this);
			Utiles.fetchSetting(this);
		}

		if (Global.softUpdateFlag && Global.set_softUpdate == 0) {// 0表示打开自动更新
			Global.softUpdateFlag = false;
			mAutoSoftUpdateTask = new AutoSoftUpdate();
			mAutoSoftUpdateTask.execute(Email.softUri);
		}

		if (Global.notice_Flag && Global.set_music_background_value == 0) { // 只在每次启动程序时做有无通知检测
			Global.notice_Flag = false;
			playBackgroundMusic();
			// mNoticeTask = new NoticeTask();
			// mNoticeTask.execute(Email.systemNotice);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			popup.dismiss();
			mTitleGridView.setAdapter(mTitleAdapter);//解决横竖屏切换出错
		} else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			popup.dismiss();
			mTitleGridView.setAdapter(mTitleAdapter);
		}
	}

	public void playBackgroundMusic() {
		SharedPreferences sharedata = getSharedPreferences(
				Email.STORESETMESSAGE, 0);
		String musicDir = sharedata.getString(Global.set_music_dir, "");
		if (!musicDir.equals("")) {
			MusicService.resUrl = musicDir;
			sendPlayService(1);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Utility.cancelTaskInterrupt(mLoadAccountsTask);
		mLoadAccountsTask = null;

		Utility.cancelTaskInterrupt(mSoftUpdateTask);
		mSoftUpdateTask = null;

		Utility.cancelTaskInterrupt(mAutoSoftUpdateTask);
		mAutoSoftUpdateTask = null;
		// TODO: We shouldn't call cancel() for DeleteAccountTask. If the task
		// hasn't
		// started, this will mark it as "don't run", but we always want it to
		// finish.
		// (But don't just remove this cancel() call.
		// DeleteAccountTask.onPostExecute() checks if
		// it's been canceled to decided whether to update the UI.)
		Utility.cancelTask(mDeleteAccountTask, false); // Don't interrupt if
		// it's running.
		mDeleteAccountTask = null;

		Utility.cancelTaskInterrupt(mNoticeTask);
		mNoticeTask = null;

		if (mListAdapter != null) {
			mListAdapter.changeCursor(null);
		}
	}
 
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (mListAdapter.isMailbox(position)) {
			MessageList.actionHandleMailbox(this, id,Global.activityType);
		} else if (mListAdapter.isAccount(position)) {
			MessageList.actionHandleAccount(this, id, Mailbox.TYPE_INBOX);
		}
	}

	private static int getUnreadCountByMailboxType(Context context, int type) {
		int count = 0;
		Cursor c = context.getContentResolver().query(Mailbox.CONTENT_URI,
				MAILBOX_SUM_OF_UNREAD_COUNT_PROJECTION, MAILBOX_TYPE_SELECTION,
				new String[] { String.valueOf(type) }, null);

		try {
			if (c.moveToFirst()) {
				return c.getInt(0);
			}
		} finally {
			c.close();
		}
		return count;
	}

	private static int getCountByMailboxType(Context context, int type) {
		int count = 0;
		Cursor c = context.getContentResolver().query(Mailbox.CONTENT_URI,
				EmailContent.ID_PROJECTION, MAILBOX_TYPE_SELECTION,
				new String[] { String.valueOf(type) }, null);

		try {
			c.moveToPosition(-1);
			while (c.moveToNext()) {
				count += EmailContent.count(context, Message.CONTENT_URI,
						MAILBOX_ID_SELECTION, new String[] { String.valueOf(c
								.getLong(EmailContent.ID_PROJECTION_COLUMN)) });
			}
		} finally {
			c.close();
		}
		return count;
	}

	/**
	 * Build the group and child cursors that support the summary views (aka
	 * "at a glance").
	 * 
	 * This is a placeholder implementation with significant problems that need
	 * to be addressed:
	 * 
	 * TODO: We should only show summary mailboxes if they are non-empty. So
	 * there needs to be a more dynamic child-cursor here, probably listening
	 * for update notifications on a number of other internally-held queries
	 * such as count-of-inbox, count-of-unread, etc.
	 * 
	 * TODO: This simple list is incomplete. For example, we probably want
	 * drafts, outbox, and (maybe) sent (again, these would be displayed only
	 * when non-empty).
	 * 
	 * TODO: We need a way to count total unread in all inboxes (probably with
	 * some provider help)
	 * 
	 * TODO: We need a way to count total # messages in all other summary boxes
	 * (probably with some provider help).
	 * 
	 * TODO use narrower account projection (see LoadAccountsTask)
	 */
	private MatrixCursor getSummaryChildCursor() {
		MatrixCursor childCursor = new MatrixCursor(MAILBOX_PROJECTION);
		int count;
		RowBuilder row;
		// TYPE_INBOX_UNREAD
		count = getUnreadCountByMailboxType(this, Mailbox.TYPE_INBOX);
		row = childCursor.newRow();
		row.add(Long.valueOf(Mailbox.QUERY_ALL_UNREAD)); // MAILBOX_COLUMN_ID =
		// 0;
		row.add(getString(R.string.account_folder_list_summary_uninbox)); // MAILBOX_DISPLAY_NAME
		row.add(null); // MAILBOX_ACCOUNT_KEY = 2;
		row.add(Integer.valueOf(Mailbox.TYPE_INBOX)); // MAILBOX_TYPE = 3;
		row.add(Integer.valueOf(count)); // MAILBOX_UNREAD_COUNT = 4;

		// TYPE_INBOX
		count = getCountByMailboxType(this, Mailbox.TYPE_INBOX);
		row = childCursor.newRow();
		row.add(Long.valueOf(Mailbox.QUERY_ALL_INBOXES)); // MAILBOX_COLUMN_ID =
		// 0;
		row.add(getString(R.string.account_folder_list_summary_inbox)); // MAILBOX_DISPLAY_NAME
		row.add(null); // MAILBOX_ACCOUNT_KEY = 2;
		row.add(Integer.valueOf(Mailbox.TYPE_INBOX)); // MAILBOX_TYPE = 3;
		row.add(Integer.valueOf(count)); // MAILBOX_UNREAD_COUNT = 4;

		// TYPE_MAIL (FAVORITES)
		count = EmailContent.count(this, Message.CONTENT_URI,
				FAVORITE_COUNT_SELECTION, null);
		if (count > 0) {
			row = childCursor.newRow();
			row.add(Long.valueOf(Mailbox.QUERY_ALL_FAVORITES)); // MAILBOX_COLUMN_ID
			// = 0;
			// MAILBOX_DISPLAY_NAME
			row.add(getString(R.string.account_folder_list_summary_starred));
			row.add(null); // MAILBOX_ACCOUNT_KEY = 2;
			row.add(Integer.valueOf(Mailbox.TYPE_MAIL)); // MAILBOX_TYPE = 3;
			row.add(Integer.valueOf(count)); // MAILBOX_UNREAD_COUNT = 4;
		}
		// TYPE_DRAFTS
		// count = getCountByMailboxType(this, Mailbox.TYPE_DRAFTS);
		// if (count > 0) {
		// row = childCursor.newRow();
		// row.add(Long.valueOf(Mailbox.QUERY_ALL_DRAFTS)); // MAILBOX_COLUMN_ID
		// = 0;
		// row.add(getString(R.string.account_folder_list_summary_drafts));//
		// MAILBOX_DISPLAY_NAME
		// row.add(null); // MAILBOX_ACCOUNT_KEY = 2;
		// row.add(Integer.valueOf(Mailbox.TYPE_DRAFTS)); // MAILBOX_TYPE = 3;
		// row.add(Integer.valueOf(count)); // MAILBOX_UNREAD_COUNT = 4;
		// }
		// TYPE_OUTBOX
		count = getCountByMailboxType(this, Mailbox.TYPE_OUTBOX);
		if (count > 0) {
			row = childCursor.newRow();
			row.add(Long.valueOf(Mailbox.QUERY_ALL_OUTBOX)); // MAILBOX_COLUMN_ID
			// = 0;
			row.add(getString(R.string.account_folder_list_summary_outbox));// MAILBOX_DISPLAY_NAME
			row.add(null); // MAILBOX_ACCOUNT_KEY = 2;
			row.add(Integer.valueOf(Mailbox.TYPE_OUTBOX)); // MAILBOX_TYPE = 3;
			row.add(Integer.valueOf(count)); // MAILBOX_UNREAD_COUNT = 4;
		}
		return childCursor;
	}

	/**
	 * Async task to handle the accounts query outside of the UI thread
	 */
	private class LoadAccountsTask extends AsyncTask<Void, Void, Object[]> {
		@Override
		protected Object[] doInBackground(Void... params) {
			Cursor c1 = null;
			Cursor c2 = null;
			Long defaultAccount = null;
			if (!isCancelled()) {
				// Create the summaries cursor
				c1 = getSummaryChildCursor();
			}

			if (!isCancelled()) {
				// TODO use a custom projection and don't have to sample all of
				// these columns
				c2 = getContentResolver().query(
						EmailContent.Account.CONTENT_URI,
						EmailContent.Account.CONTENT_PROJECTION, null, null,
						null);
			}

			if (!isCancelled()) {
				defaultAccount = Account
						.getDefaultAccountId(AccountFolderList.this);
			}

			if (isCancelled()) {
				if (c1 != null)
					c1.close();
				if (c2 != null)
					c2.close();
				return null;
			}
			return new Object[] { c1, c2, defaultAccount };
		}

		@Override
		protected void onPostExecute(Object[] params) {
			if (isCancelled() || params == null) {
				if (params != null) {
					Cursor c1 = (Cursor) params[0];
					if (c1 != null) {
						c1.close();
					}
					Cursor c2 = (Cursor) params[1];
					if (c2 != null) {
						c2.close();
					}
				}
				return;
			}
			// Before writing a new list adapter into the listview, we need to
			// shut down the old one (if any).
			ListAdapter oldAdapter = mListView.getAdapter();
			if (oldAdapter != null && oldAdapter instanceof CursorAdapter) {
				((CursorAdapter) oldAdapter).changeCursor(null);
			}
			// Now create a new list adapter and install it
			mListAdapter = AccountsAdapter.getInstance((Cursor) params[0],
					(Cursor) params[1], AccountFolderList.this,
					(Long) params[2]);
			mListView.setAdapter(mListAdapter);
		}
	}

	private class DeleteAccountTask extends AsyncTask<Void, Void, Void> {
		private final long mAccountId;
		private final String mAccountUri;

		public DeleteAccountTask(long accountId, String accountUri) {
			mAccountId = accountId;
			mAccountUri = accountUri;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				// Delete Remote store at first.
				Store.getInstance(mAccountUri, getApplication(), null).delete();
				// Remove the Store instance from cache.
				Store.removeInstance(mAccountUri);
				Uri uri = ContentUris.withAppendedId(
						EmailContent.Account.CONTENT_URI, mAccountId);
				AccountFolderList.this.getContentResolver().delete(uri, null,
						null);
				// Update the backup (side copy) of the accounts
				AccountBackupRestore.backupAccounts(AccountFolderList.this);
				// Release or relax device administration, if relevant
				SecurityPolicy.getInstance(AccountFolderList.this)
						.reducePolicies();
			} catch (Exception e) {
				// Ignore
			}
			Email.setServicesEnabled(AccountFolderList.this);
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
			if (!isCancelled()) {
				updateAccounts();
			}
		}
	}

	private void updateAccounts() {
		Utility.cancelTaskInterrupt(mLoadAccountsTask);
		mLoadAccountsTask = (LoadAccountsTask) new LoadAccountsTask().execute();
	}

	private void onAddNewAccount() {
		AccountSetupBasics.actionNewAccount(this);
	}

	private void onEditAccount(long accountId) {
		AccountSettings.actionSettings(this, accountId);
	}

	/**
	 * Refresh one or all accounts
	 * 
	 * @param accountId
	 *            A specific id to refresh folders only, or -1 to refresh
	 *            everything
	 */
	private void onRefresh(long accountId) {
		if (accountId == -1) {
			// TODO implement a suitable "Refresh all accounts" / "check mail"
			// comment in Controller
			// TODO this is temp
			Toast.makeText(this,
					getString(R.string.account_folder_list_refresh_toast),
					Toast.LENGTH_LONG).show();
		} else {
			mHandler.progress(true);
			Controller.getInstance(getApplication()).updateMailboxList(
					accountId, mControllerCallback);
		}
	}

	private void onCompose(long accountId) {
		if (accountId == -1) {
			accountId = Account.getDefaultAccountId(this);
		}
		if (accountId != -1) {
			MessageCompose.actionCompose(this, accountId);
		} else {
			onAddNewAccount();
		}
	}

	private void onDeleteAccount(long accountId) {
		mSelectedContextAccount = Account.restoreAccountWithId(this, accountId);
		showDialog(DIALOG_REMOVE_ACCOUNT);
	}

	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_REMOVE_ACCOUNT:
			return createRemoveAccountDialog();
		case DIALOG_SOFT_UPDATE:
			return updateSoftDialog();
		case DIALOG_BACK:
			return createBackDialog();
		case DIALOG_SHARE_APP:
			return CreatePopShareDialog();
		}
		return super.onCreateDialog(id);
	}
    private Dialog CreatePopShareDialog(){
    
    	return	new AlertDialog.Builder(this).setTitle(
				getString(R.string.share_app))
				.setIcon(android.R.drawable.ic_dialog_info)
				.setItems(new String[]{getString(R.string.short_message_share),getString(R.string.mailbox_list_title),"分享到微信朋友","分享至微信朋友圈"},
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if(which == 0){
									shareShortMessageApp();
								}else if(which == 1){
									shareEmailApp();
								}
								else if(which == 2){
									shareContent(Configs.SHARE_WX_PY_ID);
								}
								else if(which == 3){
									shareContent(Configs.SHARE_WX_PYQ_ID);
								}
								dialog.dismiss();
							}
						}).setNegativeButton(getString(R.string.cancel),
						null).create();
	
    }
    public void shareContent(int shareId){
    	if(!isInstallClientWeixin()){
    		return;
    	}
    	ShareContentBean shareContentBean = new ShareContentBean();
		shareContentBean.setContent(getString(R.string.about_cotent));// 分享内容=内容+短地址
		shareContentBean.setId("1");// 用于分享完成回调参数
		shareContentBean.setUrl("http://www.warmtel.com");// 短地址
		shareContentBean.setTitle(getString(R.string.app_name));
		shareWeiXin(shareId, shareContentBean);
		
    }
	// 退出程序前恢复数据默认值
	public void resetData() {
		Global.shortCutFlag = true;
		Global.notice_Flag = true;
		Global.softUpdateFlag = true;
		Utility.FolderProperties.getInstance(this).resetInstance();
	}

	private Dialog createBackDialog() {
//	    View layout = getLayoutInflater().inflate(R.layout.dialog_exit, null);
//	    final CheckBox noPrompt = (CheckBox)layout.findViewById(R.id.no_prompt);
//	    noPrompt.setText(R.string.contacts_no_notify);
		return new AlertDialog.Builder(this).setIcon(R.drawable.icon)
				.setTitle(R.string.app_name)
				.setMessage(getString(R.string.exit_yes_msg))
//				.setView(layout)
				.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								resetData();
								sendPlayService(0);// stop music background
//								if(noPrompt.isChecked()){
//									saveExitMessage(noPrompt.isChecked());
//								}
								finish();
							}
					    })
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								
							}
						})
				.create();
	}
	public boolean getExitMessageBoolan(){
		SharedPreferences sharedata = getSharedPreferences(Email.STORESETMESSAGE, 0);
		return sharedata.getBoolean(Global.set_exit_noprompt, false);
    }
    public void saveExitMessage(boolean value){
    	Editor sharedata = getSharedPreferences(Email.STORESETMESSAGE, 0).edit();
    	sharedata.putBoolean(Global.set_exit_noprompt,value);
    	sharedata.commit();
    }
	public void sendPlayService(int op) {
		Bundle bundle = new Bundle();
		bundle.putInt("op", op);
		Intent intent = new Intent(Global.ACTION_MUSIC_SERVICE_BROADCASTK);
		intent.putExtras(bundle);
		sendBroadcast(intent);
	}

	private Dialog updateSoftDialog() {
		mUpdateSoftDialog = new ProgressDialog(this);
		mUpdateSoftDialog.setTitle(R.string.soft_update_title);

		return mUpdateSoftDialog;
	}

	private Dialog createRemoveAccountDialog() {
		return new AlertDialog.Builder(this).setIcon(
				android.R.drawable.ic_dialog_alert).setTitle(
				R.string.account_delete_dlg_title).setMessage(
				getString(R.string.account_delete_dlg_instructions_fmt,
						mSelectedContextAccount.getDisplayName()))
				.setPositiveButton(R.string.okay_action,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dismissDialog(DIALOG_REMOVE_ACCOUNT);
								// Clear notifications, which may become stale
								// here
								NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
								notificationManager
										.cancel(MailService.NOTIFICATION_ID_NEW_MESSAGES);
								int numAccounts = EmailContent.count(
										AccountFolderList.this,
										Account.CONTENT_URI, null, null);
								mListAdapter
										.addOnDeletingAccount(mSelectedContextAccount.mId);

								mDeleteAccountTask = (DeleteAccountTask) new DeleteAccountTask(
										mSelectedContextAccount.mId,
										mSelectedContextAccount
												.getStoreUri(AccountFolderList.this))
										.execute();
								if (numAccounts == 1) {
									AccountSetupBasics
											.actionNewAccount(AccountFolderList.this);
									finish();
								}
							}
						}).setNegativeButton(R.string.cancel_action,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dismissDialog(DIALOG_REMOVE_ACCOUNT);
							}
						}).create();
	}

	/**
	 * Update a cached dialog with current values (e.g. account name)
	 */
	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_REMOVE_ACCOUNT:
			AlertDialog alert = (AlertDialog) dialog;
			alert.setMessage(getString(
					R.string.account_delete_dlg_instructions_fmt,
					mSelectedContextAccount.getDisplayName()));
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();

		if (mListAdapter.isMailbox(menuInfo.position)) {
			Cursor c = (Cursor) mListView.getItemAtPosition(menuInfo.position);
			long id = c.getLong(MAILBOX_COLUMN_ID);
			switch (item.getItemId()) {
			case R.id.open_folder:
				MessageList.actionHandleMailbox(this, id,Global.activityType);
				break;
			case R.id.check_mail:
				onRefresh(-1);
				break;
			}
			return false;
		} else if (mListAdapter.isAccount(menuInfo.position)) {
			Cursor c = (Cursor) mListView.getItemAtPosition(menuInfo.position);
			long accountId = c.getLong(Account.CONTENT_ID_COLUMN);
			switch (item.getItemId()) {
			case R.id.open_folder:
				MailboxList.actionHandleAccount(this, accountId);
				break;
			case R.id.compose:
				onCompose(accountId);
				break;
			case R.id.refresh_account:
				onRefresh(accountId);
				break;
			case R.id.edit_account:
				onEditAccount(accountId);
				break;
			case R.id.delete_account:
				onDeleteAccount(accountId);
				break;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_new_account:
			onAddNewAccount();
			break;
		case R.id.check_mail:
			onRefresh(-1);
			break;
		case R.id.compose:
			onCompose(-1);
			break;
		case R.id.soft_update:
			mSoftUpdateTask = new SoftUpdate();
			mSoftUpdateTask.execute(Email.softUri);
			break;
		case R.id.soft_about:
			createAboutSoftAlertDialog();
			break;
		case R.id.user_idea:
			userIdea();
			break;
		case R.id.search:
			onSearchRequested();
			break;
		case R.id.storage:
			StorageMessage.actionStorageMessage(this);
			break;
		case R.id.system_set:
			SystemSetActivity.actionSystemSet(this);
			break;
		case R.id.skin_manage:
			SkinManageTab.actionView(this, null);
			break;
		case R.id.music_backgroud:
			MusicList.actionMusicList(this);
			break;
		case R.id.share_app:
			showDialog(DIALOG_SHARE_APP);
			break;
			
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public boolean onSearchRequested() {
		startSearch("", false, null, false);
		return true;
	}

	public void userIdea() {
		// StringBuffer mailMessage = new StringBuffer();
		// mailMessage.append("\n");
		// mailMessage.append("android sdk :");
		// mailMessage.append(PhoneUtil.getSdkVersion());
		// mailMessage.append("\n");
		// mailMessage.append("mobile type :");
		// mailMessage.append(PhoneUtil.getModel());
		// mailMessage.append("\n");
		// mailMessage.append(getString(R.string.my_idea));
		Intent it = new Intent(Intent.ACTION_SEND);
		String[] tos = { getString(R.string.yimail) };
		it.putExtra(Intent.EXTRA_EMAIL, tos);
		it.putExtra(Intent.EXTRA_TEXT, getString(R.string.my_idea));
		it.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.user_idear));
		it.setType("message/rfc822");
		it.addCategory("android.intent.category.YIMAIL");
		startActivity(Intent.createChooser(it, ""));
	}
    public void shareEmailApp(){
    	File file = new File(getAppFileDir()); //附件文件地址
    	Intent intent = new Intent(Intent.ACTION_SEND);
    	intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app)); //
    	intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file)); //添加附件，附件为file对象
    	intent.setType("application/octet-stream"); //其他的均使用流当做二进制数据来发送
    	intent.addCategory("android.intent.category.YIMAIL");
    	startActivity(intent); //调用系统的mail客户端进行发送。
    	
    }
    public void shareShortMessageApp(){
    	Intent it = new Intent(Intent.ACTION_VIEW);   
    	it.putExtra("sms_body", getString(R.string.short_message_share_content));   
    	it.setType("vnd.android-dir/mms-sms");   
    	startActivity(it);  
    }
    public String getAppFileDir() {
		PackageManager pckMan = getPackageManager();
		List<PackageInfo> packs = pckMan.getInstalledPackages(0);
		int count = packs.size();
		String name;
		String fileDir = "";
		for (int i = 0; i < count; i++) {
			PackageInfo p = packs.get(i); 
			ApplicationInfo appInfo = p.applicationInfo;
			if((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0){
				
				name = appInfo.loadLabel(pckMan).toString(); 
				if(name.equals(getString(R.string.app_name))){
					fileDir = appInfo.sourceDir;
					break;
				}
			}
		}
		return fileDir;
    }
    
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!Global.skinName.equals(Global.SKIN_NAME)) {
			menu.add("menu");// 必须创建一项
			return super.onCreateOptionsMenu(menu);
		} else {
			super.onCreateOptionsMenu(menu);
			getMenuInflater().inflate(R.menu.account_folder_list_option, menu);
			return true;
		}

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (!Global.skinName.equals(Global.SKIN_NAME)) {
			menu.add("menu");// 必须创建一项
			return super.onCreateOptionsMenu(menu);
		} else {
			menu.clear();
			super.onCreateOptionsMenu(menu);
			getMenuInflater().inflate(R.menu.account_folder_list_option, menu);
			return true;
		}

	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (!Global.skinName.equals(Global.SKIN_NAME)) {
			if (popup != null) {
				if (popup.isShowing())
					popup.dismiss();
				else {
					popup.showAtLocation(mListView, Gravity.BOTTOM, 0, 0);
				}
			}
			return false;// 返回为true 则显示系统menu
		} else {
			return true;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo info) {
		super.onCreateContextMenu(menu, v, info);
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) info;
		if (mListAdapter.isMailbox(menuInfo.position)) {
			Cursor c = (Cursor) mListView.getItemAtPosition(menuInfo.position);
			String displayName = c
					.getString(Account.CONTENT_DISPLAY_NAME_COLUMN);
			menu.setHeaderTitle(displayName);
			getMenuInflater().inflate(
					R.menu.account_folder_list_smart_folder_context, menu);
		} else if (mListAdapter.isAccount(menuInfo.position)) {
			Cursor c = (Cursor) mListView.getItemAtPosition(menuInfo.position);
			String accountName = c
					.getString(Account.CONTENT_DISPLAY_NAME_COLUMN);
			menu.setHeaderTitle(accountName);
			getMenuInflater().inflate(R.menu.account_folder_list_context, menu);
		}
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {  
            exit();  
//            showDialog(DIALOG_BACK);
            return true;  
        } else {  
            return super.onKeyDown(keyCode, event);  
        }  
    }
	
	// @Override
	// public boolean onKeyDown(int keyCode, KeyEvent event) {
	// if (event.getKeyCode() == SECRET_KEY_CODES[mSecretKeyCodeIndex]) {
	// mSecretKeyCodeIndex++;
	// if (mSecretKeyCodeIndex == SECRET_KEY_CODES.length) {
	// mSecretKeyCodeIndex = 0;
	// startActivity(new Intent(this, Debug.class));
	// }
	// } else {
	// mSecretKeyCodeIndex = 0;
	// }
	// return super.onKeyDown(keyCode, event);
	// }

	/**
	 * Handler for UI-thread operations (when called from callbacks or any other
	 * threads)
	 */
	private class MessageListHandler extends Handler {
		private static final int MSG_PROGRESS = 1;
		private static final int MSG_NET_ERROR_PROGRESS = 2;
		private static final int MSG_NOTICE = 3;
		private static final int MSG_SEND_SUCCESS = 4;
		private static final int MSG_BACK_EXIT = 5;
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_PROGRESS:
				boolean showProgress = (msg.arg1 != 0);
				if (showProgress) {
					mProgressIcon.setVisibility(View.VISIBLE);
				} else {
					mProgressIcon.setVisibility(View.GONE);
				}
				break;
			case MSG_NET_ERROR_PROGRESS:
				Toast.makeText(
						AccountFolderList.this,
						AccountFolderList.this
								.getString(R.string.upgrade_accounts_error),
						Toast.LENGTH_SHORT).show();
				break;
			case MSG_SEND_SUCCESS:
				boolean showSuccess = (msg.arg1 != 0);
				if (showSuccess) {
					Toast
							.makeText(
									AccountFolderList.this,
									AccountFolderList.this
											.getString(R.string.message_send_success_toast),
									Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(
							AccountFolderList.this,
							AccountFolderList.this
									.getString(R.string.message_sending_toast),
							Toast.LENGTH_SHORT).show();
				}
				break;
			case MSG_BACK_EXIT:
                isExit = false;
                break;
                
			case MSG_NOTICE:
				String message = (String) msg.obj;
				if (message == null) {
					return;
				}
				Utiles.showSystemNotification(AccountFolderList.this, message);
				break;
			default:
				super.handleMessage(msg);
			}
		}

		/**
		 * Call from any thread to start/stop progress indicator(s)
		 * 
		 * @param progress
		 *            true to start, false to stop
		 */
		public void progress(boolean progress) {
			android.os.Message msg = android.os.Message.obtain();
			msg.what = MSG_PROGRESS;
			msg.arg1 = progress ? 1 : 0;
			sendMessage(msg);
		}

		public void net_Error_progress() {
			android.os.Message msg = android.os.Message.obtain();
			msg.what = MSG_NET_ERROR_PROGRESS;
			sendMessage(msg);
		}

		public void doSytemNotice(String message) {
			android.os.Message msg = android.os.Message.obtain();
			msg.what = MSG_NOTICE;
			msg.obj = message;
			sendMessage(msg);
		}

		public void sendMessageToast(boolean showSuccess) {
			android.os.Message msg = android.os.Message.obtain();
			msg.what = MSG_SEND_SUCCESS;
			msg.arg1 = showSuccess ? 1 : 0;
			sendMessage(msg);
		}
		
		public void backTwoExit() {
            android.os.Message msg = obtainMessage();
            msg.what = MSG_BACK_EXIT;
            sendMessageDelayed(msg, 2000);
        }
	}

	/**
	 * Callback for async Controller results.
	 */
	private class ControllerResults implements Controller.Result {
		public void updateMailboxListCallback(MessagingException result,
				long accountKey, int progress) {
			updateProgress(result, progress);
		}

		public void updateMailboxCallback(MessagingException result,
				long accountKey, long mailboxKey, int progress,
				int numNewMessages) {
			if (result != null || progress == 100) {
				Email.updateMailboxRefreshTime(mailboxKey);
			}
			if (progress == 100) {
				updateAccounts();
			}
			updateProgress(result, progress);
		}

		public void loadMessageForViewCallback(MessagingException result,
				long messageId, int progress) {
		}

		public void loadAttachmentCallback(MessagingException result,
				long messageId, long attachmentId, int progress) {
		}

		public void serviceCheckMailCallback(MessagingException result,
				long accountId, long mailboxId, int progress, long tag) {
			updateProgress(result, progress);
		}

		public void sendMailCallback(MessagingException result, long accountId,
				long messageId, int progress) {
			if (progress == 100) {
				mHandler.sendMessageToast(true);
				updateAccounts();
			}
		}

		private void updateProgress(MessagingException result, int progress) {
			if (result != null || progress == 100) {
				mHandler.progress(false);
			} else if (progress == 0) {
				mHandler.progress(true);
			}
		}
	}

	/* package */static class AccountsAdapter extends CursorAdapter {

		private final Context mContext;
		private final LayoutInflater mInflater;
		private final int mMailboxesCount;
		private final int mSeparatorPosition;
		private final long mDefaultAccountId;
		private final ArrayList<Long> mOnDeletingAccounts = new ArrayList<Long>();

		public static AccountsAdapter getInstance(Cursor mailboxesCursor,
				Cursor accountsCursor, Context context, long defaultAccountId) {
			Cursor[] cursors = new Cursor[] { mailboxesCursor, accountsCursor };
			Cursor mc = new MergeCursor(cursors);
			return new AccountsAdapter(mc, context, mailboxesCursor.getCount(),
					defaultAccountId);
		}

		public AccountsAdapter(Cursor c, Context context, int mailboxesCount,
				long defaultAccountId) {
			super(context, c, true);
			mContext = context;
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mMailboxesCount = mailboxesCount;
			mSeparatorPosition = mailboxesCount;
			mDefaultAccountId = defaultAccountId;
		}

		public boolean isMailbox(int position) {
			return position < mMailboxesCount;
		}

		public boolean isAccount(int position) {
			return position >= mMailboxesCount;
		}

		public void addOnDeletingAccount(long accountId) {
			mOnDeletingAccounts.add(accountId);
		}

		public boolean isOnDeletingAccountView(long accountId) {
			return mOnDeletingAccounts.contains(accountId);
		}

		/**
		 * This is used as a callback from the list items, for clicks in the
		 * folder "button"
		 * 
		 * @param itemView
		 *            the item in which the click occurred
		 */
		public void onClickFolder(AccountFolderListItem itemView) {
			MailboxList.actionHandleAccount(mContext, itemView.mAccountId);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// Logs.v(Logs.LOG_TAG, "bindView");
			if (cursor.getPosition() < mMailboxesCount) {
				bindMailboxItem(view, context, cursor, false);
			} else {
				bindAccountItem(view, context, cursor, false);
			}
		}

		private void bindMailboxItem(View view, Context context, Cursor cursor,
				boolean isLastChild) {

			// Reset the view (in case it was recycled) and prepare for binding
			AccountFolderListItem itemView = (AccountFolderListItem) view;
			itemView.bindViewInit(this, false);

			// Invisible (not "gone") to maintain spacing
			view.findViewById(R.id.chip).setVisibility(View.INVISIBLE);

			String text = cursor.getString(MAILBOX_DISPLAY_NAME);
			if (text != null) {
				TextView nameView = (TextView) view.findViewById(R.id.name);
				nameView.setText(text);
			}

			// TODO get/track live folder status
			text = null;
			TextView statusView = (TextView) view.findViewById(R.id.status);
			if (text != null) {
				statusView.setText(text);
				statusView.setVisibility(View.VISIBLE);
			} else {
				statusView.setVisibility(View.GONE);
			}

			int count = -1;
			text = cursor.getString(MAILBOX_UNREAD_COUNT);
			if (text != null) {
				count = Integer.valueOf(text);
			}
			TextView unreadCountView = (TextView) view
					.findViewById(R.id.new_message_count);
			TextView allCountView = (TextView) view
					.findViewById(R.id.all_message_count);
			int id = cursor.getInt(MAILBOX_COLUMN_ID);
			// If the unread count is zero, not to show countView.
			if (count > 0) {
				if (id == Mailbox.QUERY_ALL_FAVORITES
						|| id == Mailbox.QUERY_ALL_DRAFTS
						|| id == Mailbox.QUERY_ALL_OUTBOX
						|| id == Mailbox.QUERY_ALL_INBOXES) {
					unreadCountView.setVisibility(View.GONE);
					allCountView.setVisibility(View.VISIBLE);
					allCountView.setText(text);
				} else {
					allCountView.setVisibility(View.GONE);
					unreadCountView.setVisibility(View.VISIBLE);
					unreadCountView.setText(text);
				}
			} else {
				allCountView.setVisibility(View.GONE);
				unreadCountView.setVisibility(View.GONE);
			}

			view.findViewById(R.id.folder_button).setVisibility(View.GONE);
			view.findViewById(R.id.folder_separator).setVisibility(View.GONE);
			view.findViewById(R.id.default_sender).setVisibility(View.GONE);
			view.findViewById(R.id.folder_icon).setVisibility(View.VISIBLE);
			((ImageView) view.findViewById(R.id.folder_icon))
					.setImageDrawable(Utility.FolderProperties.getInstance(
							context).getSummaryMailboxIconIds(id));
		}

		private void bindAccountItem(View view, Context context, Cursor cursor,
				boolean isExpanded) {
			// Logs.v(Logs.LOG_TAG, "bindAccountItem");
			// Reset the view (in case it was recycled) and prepare for binding
			AccountFolderListItem itemView = (AccountFolderListItem) view;
			itemView.bindViewInit(this, true);
			itemView.mAccountId = cursor.getLong(Account.CONTENT_ID_COLUMN);

			long accountId = cursor.getLong(Account.CONTENT_ID_COLUMN);
			View chipView = view.findViewById(R.id.chip);
			chipView.setBackgroundResource(Email
					.getAccountColorResourceId(accountId));
			chipView.setVisibility(View.VISIBLE);

			String text = cursor.getString(Account.CONTENT_DISPLAY_NAME_COLUMN);
			if (text != null) {
				TextView descriptionView = (TextView) view
						.findViewById(R.id.name);
				descriptionView.setText(text);
			}

			text = cursor.getString(Account.CONTENT_EMAIL_ADDRESS_COLUMN);
			if (text != null) {
				TextView emailView = (TextView) view.findViewById(R.id.status);
				emailView.setText(text);
				emailView.setVisibility(View.VISIBLE);
			}

			int unreadMessageCount = 0;
			Cursor c = context.getContentResolver().query(Mailbox.CONTENT_URI,
					MAILBOX_UNREAD_COUNT_PROJECTION, MAILBOX_INBOX_SELECTION,
					new String[] { String.valueOf(accountId) }, null);

			try {
				if (c.moveToFirst()) {
					String count = c
							.getString(MAILBOX_UNREAD_COUNT_COLUMN_UNREAD_COUNT);
					if (count != null) {
						unreadMessageCount = Integer.valueOf(count);
					}
				}
			} finally {
				c.close();
			}

			view.findViewById(R.id.all_message_count).setVisibility(View.GONE);
			TextView unreadCountView = (TextView) view
					.findViewById(R.id.new_message_count);
			if (unreadMessageCount > 0) {
				unreadCountView.setText(String.valueOf(unreadMessageCount));
				unreadCountView.setVisibility(View.VISIBLE);
			} else {
				unreadCountView.setVisibility(View.GONE);
			}

			view.findViewById(R.id.folder_icon).setVisibility(View.GONE);
			view.findViewById(R.id.folder_button).setVisibility(View.VISIBLE);
			view.findViewById(R.id.folder_separator)
					.setVisibility(View.VISIBLE);
			if (accountId == mDefaultAccountId) {
				view.findViewById(R.id.default_sender).setVisibility(
						View.VISIBLE);
			} else {
				view.findViewById(R.id.default_sender).setVisibility(View.GONE);
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			// Logs.v(Logs.LOG_TAG, "newView");
			return mInflater.inflate(R.layout.account_folder_list_item, parent,
					false);
		}

		/*
		 * The following series of overrides insert the "Accounts" separator
		 */

		/**
		 * Prevents the separator view from recycling into the other views
		 */
		@Override
		public int getItemViewType(int position) {
			if (position == mSeparatorPosition) {
				return IGNORE_ITEM_VIEW_TYPE;
			}
			return super.getItemViewType(position);
		}

		/**
		 * Injects the separator view when required, and fudges the cursor for
		 * other views
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// The base class's getView() checks for mDataValid at the
			// beginning, but we don't have
			// to do that, because if the cursor is invalid getCount() returns
			// 0, in which case this
			// method wouldn't get called.

			// Handle the separator here - create & bind
			if (position == mSeparatorPosition) {
				TextView view;
				view = (TextView) mInflater.inflate(R.layout.list_separator,
						parent, false);
				view.setText(R.string.account_folder_list_separator_accounts);
				return view;
			}
			// Logs.v(Logs.LOG_TAG,
			// "mSeparatorPosition :"+mSeparatorPosition+", position >> :"+position+", getRealPosition(position) >> :"+getRealPosition(position));
			return super
					.getView(getRealPosition(position), convertView, parent);
		}

		/**
		 * Forces navigation to skip over the separator
		 */
		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		/**
		 * Forces navigation to skip over the separator
		 */
		@Override
		public boolean isEnabled(int position) {
			if (position == mSeparatorPosition) {
				return false;
			} else if (isAccount(position)) {
				Long id = ((MergeCursor) getItem(position))
						.getLong(Account.CONTENT_ID_COLUMN);
				return !isOnDeletingAccountView(id);
			} else {
				return true;
			}
		}

		/**
		 * Adjusts list count to include separator
		 */
		@Override
		public int getCount() {
			int count = super.getCount();
			if (count > 0 && (mSeparatorPosition != ListView.INVALID_POSITION)) {
				// Increment for separator, if we have anything to show.
				count += 1;
			}
			// Logs.v(Logs.LOG_TAG, "count >> :"+count);
			return count;
		}

		/**
		 * Converts list position to cursor position
		 */
		private int getRealPosition(int pos) {
			if (mSeparatorPosition == ListView.INVALID_POSITION) {
				// No separator, identity map
				return pos;
			} else if (pos <= mSeparatorPosition) {
				// Before or at the separator, identity map
				return pos;
			} else {
				// After the separator, remove 1 from the pos to get the real
				// underlying pos
				return pos - 1;
			}
		}

		/**
		 * Returns the item using external position numbering (no separator)
		 */
		@Override
		public Object getItem(int pos) {
			return super.getItem(getRealPosition(pos));
		}

		/**
		 * Returns the item id using external position numbering (no separator)
		 */
		@Override
		public long getItemId(int pos) {
			// Logs.v(Logs.LOG_TAG, "getItemId pos >> :"+pos);
			return super.getItemId(getRealPosition(pos));
		}
	}

	private class NoticeTask extends AsyncTask<String, Void, String> {
		NoticeBean mNoticeBean;

		@Override
		protected String doInBackground(String... params) {
			String feedUrl = params[0];
			// Logs.d(Logs.LOG_TAG, "NoticeTask feedUrl " + feedUrl);
			SystemNoticeSaxFeedParser mSystemNoticeSaxFeedParser = new SystemNoticeSaxFeedParser(
					feedUrl, getApplication());
			try {
				mNoticeBean = mSystemNoticeSaxFeedParser.parse();
			} catch (Exception e) {
				Logs.e(Logs.LOG_TAG, "NoticeTask error " + e.getMessage());
				e.printStackTrace();
				return null;
			}
			return mNoticeBean.getControl();
		}

		@Override
		protected void onPostExecute(String result) {
			if (result == null) {
				return;
			}
			if (result.equals(Global.noticeOpen)) {
				// Logs.d(Logs.LOG_TAG, "NoticeTask mNoticeBean.getVersions() "
				// + mNoticeBean.getVersions() + " ,"
				// + Global.notice_version);
				if (mNoticeBean.getVersions() > Global.notice_version) {
					Editor sharedata = getSharedPreferences(
							Email.STORESETMESSAGE, 0).edit();
					sharedata.putLong(Global.notice_version_name, mNoticeBean
							.getVersions());
					sharedata.commit();

					mHandler.doSytemNotice(mNoticeBean.getMessage());
				}

			}
		}
	}

	private class AutoSoftUpdate extends AsyncTask<String, Void, Integer> {
		@Override
		protected Integer doInBackground(String... params) {
			String feedUrl = params[0];
			SoftUpdateSaxFeedParser baseFeedParsersnew = new SoftUpdateSaxFeedParser(
					feedUrl, getApplication());
			try {
				SoftUpdateBean softUpdateBean = baseFeedParsersnew.parse();
				Global.serverVersion = Integer.parseInt(softUpdateBean
						.getServerVersion());
				Global.apkSize = softUpdateBean.getApkSize();
				Global.addAccountCount = softUpdateBean.getAddAccountCount();
				Global.smsMobile = softUpdateBean.getSmsMail();
				Global.sendMessage = softUpdateBean.getSendmessage();
				Global.softUpdateUri = softUpdateBean.getDownloadUrl();
				Global.softUpdateContent = softUpdateBean.getUpdateContent();
			} catch (Exception e) {
				return null;
			}
			return 1;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == null) {
				return;
			}
			if (result == 1) {
				if (Global.localVersion < Global.serverVersion) {
					// 发现新版本，提示用户更新
					Intent intent = new Intent(Global.ACTION_SOFTUDPAE_RECEIVER);
					sendBroadcast(intent);
				}

			}

		}
	}

	private class SoftUpdate extends AsyncTask<String, Void, Integer> {
		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_SOFT_UPDATE);
		}

		@Override
		protected Integer doInBackground(String... params) {
			String feedUrl = params[0];
			SoftUpdateSaxFeedParser baseFeedParsersnew = new SoftUpdateSaxFeedParser(
					feedUrl, getApplication());
			try {
				SoftUpdateBean softUpdateBean = baseFeedParsersnew.parse();
				Global.serverVersion = Integer.parseInt(softUpdateBean
						.getServerVersion());
				Global.apkSize = softUpdateBean.getApkSize();
				Global.addAccountCount = softUpdateBean.getAddAccountCount();
				Global.smsMobile = softUpdateBean.getSmsMail();
				Global.softUpdateUri = softUpdateBean.getDownloadUrl();
				Global.softUpdateContent = softUpdateBean.getUpdateContent();

			} catch (Exception e) {
				Logs.e(Logs.LOG_TAG, "update error " + e.getMessage());
				return null;
			}
			return 1;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == null) {
				mUpdateSoftDialog.dismiss();
				mHandler.net_Error_progress();
				return;
			}
			if (mUpdateSoftDialog != null) {
				mUpdateSoftDialog.dismiss();
			}
			checkVersion();

		}
	}

	/**
	 * 检查更新版本
	 */
	public void checkVersion() {
		if (Global.localVersion < Global.serverVersion) {
			// 发现新版本，提示用户更新
			createSoftUpdateVersionDialog();
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(R.string.soft_update_title).setMessage(
					R.string.soft_update_no).setPositiveButton(
					R.string.confirm, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			alert.create().show();
		}
	}

	private void createSoftUpdateVersionDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.soft_update_title).setMessage(
				R.string.soft_update_message).setPositiveButton(
				R.string.confirm, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// 开启更新服务UpdateService
						// 这里为了把update更好模块化，可以传一些updateService依赖的值
						// 如布局ID，资源ID，动态获取的标题,这里以app_name为例
						Intent updateIntent = new Intent(
								AccountFolderList.this, UpdateService.class);
						startService(updateIntent);
					}
				}).setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		alert.create().show();

	}

	private void createAboutSoftAlertDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setIcon(R.drawable.icon).setTitle(R.string.about).setMessage(
				String.format(getString(R.string.about_cotent),
						Global.localVersionName)).setPositiveButton(
				R.string.confirm, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		alert.create().show();

	}

	/**
	 * 为程序创建桌面快捷方式
	 */
	private void addShortcut() {
		Intent shortcut = new Intent(
				"com.android.launcher.action.INSTALL_SHORTCUT");

		// 快捷方式的名称
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				getString(R.string.app_name));
		shortcut.putExtra("duplicate", false); // 不允许重复创建

		// 指定当前的Activity为快捷方式启动的对象: 如 com.everest.video.VideoPlayer
		// 注意: ComponentName的第二个参数必须加上点号(.)，否则快捷方式无法启动相应程序
		ComponentName comp = new ComponentName(getPackageName(),
				".activity.Welcome");
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(
				Intent.ACTION_MAIN).setComponent(comp));

		// 快捷方式的图标
		ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(
				this, R.drawable.icon);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
		sendBroadcast(shortcut);
	}

	/**
	 * 判断快捷方式是否存在
	 */
	private boolean isCreateShortcut() {
		String AUTHORITY = "com.android.launcher2.settings";
		boolean isInstallShortcut = false;
		if (PhoneUtil.getModel().contains("XT800")) {
			// XT800\XT806
			AUTHORITY = "com.android.launcher.settings";
		} else if (PhoneUtil.getModel().contains("I909")) {
			// I909
			AUTHORITY = "com.sec.android.app.twlauncher.settings";
		}

		final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
				+ "/favorites?notify=true");

		Cursor c = managedQuery(CONTENT_URI, new String[] { "title",
				"iconResource" }, "title=?",
				new String[] { getString(R.string.app_name) }, null);// XXX表示应用名称。
		if (c != null && c.getCount() > 0) {
			isInstallShortcut = true;

		}
		if (c != null) {
			c.close();
		}
		return isInstallShortcut;
	}

	// =================PopMenu=============
	private void initPopupMenu() {
		LinearLayout mPopupMenuLayout = new LinearLayout(this);
		mPopupMenuLayout.setOrientation(LinearLayout.VERTICAL);
		// option title
		mTitleGridView = new GridView(this);
		mTitleGridView.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		mTitleGridView.setSelector(R.color.background_cachhint);
		mTitleGridView.setNumColumns(mOptionTitle.length);// the number of
		// option title.
		mTitleGridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		mTitleGridView.setVerticalSpacing(1);
		mTitleGridView.setHorizontalSpacing(1);
		mTitleGridView.setGravity(Gravity.CENTER);
		mTitleAdapter = new MenuTitleAdapter(this,
				mOptionTitle, 16, 0xFFFFFFFF);
		mTitleGridView.setAdapter(mTitleAdapter);

		mTitleGridView.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				onChangeItem(arg1, arg2);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		mTitleGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				onChangeItem(arg1, arg2);
			}
		});

		// child Option
		mMenuContentGridView = new GridView(this);
		mMenuContentGridView.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		mMenuContentGridView.setSelector(R.drawable.toolbar_menu_item);
		mMenuContentGridView.setNumColumns(mOptionTextArray1.length); // the
																		// number
																		// of
																		// child
																		// Option
		mMenuContentGridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		mMenuContentGridView.setVerticalSpacing(10);
		mMenuContentGridView.setHorizontalSpacing(10);
		mMenuContentGridView.setPadding(10, 10, 10, 10);
		mMenuContentGridView.setGravity(Gravity.CENTER);
		mMenuContentGridView.setAdapter(getMenuAdapter(mOptionTextArray1,
				mOptionImageArray1));
		mMenuContentGridView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1,
					int childOptionIndex, long arg3) {
				switch (mTitleMenuIndex) {
				case 0:// 常用
					if (childOptionIndex == 0) {
						onAddNewAccount();
					}
					if (childOptionIndex == 1) {
						onCompose(-1);
					}
					if (childOptionIndex == 2) {
						onRefresh(-1);
					}
					if (childOptionIndex == 3) {
						userIdea();
					}
					break;
				case 1:// 设置
					if (childOptionIndex == 0) {
						SystemSetActivity.actionSystemSet(AccountFolderList.this);
					}
					if (childOptionIndex == 1) {
						StorageMessage
								.actionStorageMessage(AccountFolderList.this);
					}
					if (childOptionIndex == 2) {
						createAboutSoftAlertDialog();
					}
					if (childOptionIndex == 3) {
						showDialog(DIALOG_SHARE_APP);
					}
					break;
				case 2:// 工具
					if (childOptionIndex == 0) {
						MusicList.actionMusicList(AccountFolderList.this);
					}
					if (childOptionIndex == 1) {
						SkinManageTab.actionView(AccountFolderList.this, null);
					}
					if (childOptionIndex == 2) {
						mSoftUpdateTask = new SoftUpdate();
						mSoftUpdateTask.execute(Email.softUri);
					}
					if (childOptionIndex == 3) {
						
						onSearchRequested();
					}
					break;
				}
				popup.dismiss();
			}
		});
		mPopupMenuLayout.addView(mTitleGridView);
		mPopupMenuLayout.addView(mMenuContentGridView);

		popup = new PopupWindow(mPopupMenuLayout, LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT);
		popup.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.menu_bg));// 设置menu菜单背景
		popup.setFocusable(true);// menu菜单获得焦点 如果没有获得焦点menu菜单中的控件事件无法响应
		popup.update();
		// 设置默认项
		mTitleOption1 = (TextView) mTitleGridView.getItemAtPosition(0);
		mTitleOption1.setBackgroundColor(0x00);
	}

	private SimpleAdapter getMenuAdapter(String[] menuNameArray,
			int[] imageResourceArray) {
		ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
		for (int i = 0; i < menuNameArray.length; i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("itemImage", imageResourceArray[i]);
			map.put("itemText", menuNameArray[i]);
			data.add(map);
		}
		SimpleAdapter simperAdapter = new SimpleAdapter(this, data,
				R.layout.item_menu, new String[] { "itemImage", "itemText" },
				new int[] { R.id.item_image, R.id.item_text });
		return simperAdapter;
	}

	private void onChangeItem(View mView, int mIndex) {
		mTitleMenuIndex = mIndex;
		switch (mTitleMenuIndex) {
		case 0:
			mTitleOption1 = (TextView) mView;
			mTitleOption1.setBackgroundColor(0x00);
			if (mTitleOption2 != null)
				mTitleOption2
						.setBackgroundResource(R.drawable.toolbar_menu_release);
			if (mTitleOption3 != null)
				mTitleOption3
						.setBackgroundResource(R.drawable.toolbar_menu_release);
			mMenuContentGridView.setNumColumns(mOptionTextArray1.length);
			mMenuContentGridView.setAdapter(getMenuAdapter(mOptionTextArray1,
					mOptionImageArray1));
			break;
		case 1:
			mTitleOption2 = (TextView) mView;
			mTitleOption2.setBackgroundColor(0x00);
			if (mTitleOption1 != null)
				mTitleOption1
						.setBackgroundResource(R.drawable.toolbar_menu_release);
			if (mTitleOption3 != null)
				mTitleOption3
						.setBackgroundResource(R.drawable.toolbar_menu_release);
			mMenuContentGridView.setNumColumns(mOptionTextArraySet.length);
			mMenuContentGridView.setAdapter(getMenuAdapter(mOptionTextArraySet,
					mOptionImageArrayset));
			break;
		case 2:
			mTitleOption3 = (TextView) mView;
			mTitleOption3.setBackgroundColor(0x00);
			if (mTitleOption2 != null)
				mTitleOption2
						.setBackgroundResource(R.drawable.toolbar_menu_release);
			if (mTitleOption1 != null)
				mTitleOption1
						.setBackgroundResource(R.drawable.toolbar_menu_release);
			mMenuContentGridView.setNumColumns(mOptionTextArrayTool.length);
			mMenuContentGridView.setAdapter(getMenuAdapter(
					mOptionTextArrayTool, mOptionImageArrayTool));
			break;
		}
	}

	public class MenuTitleAdapter extends BaseAdapter {
		private Context mContext;
		private int fontColor;
		private TextView[] title;

		public MenuTitleAdapter(Context context, String[] titles, int fontSize,
				int color) {
			this.mContext = context;
			this.fontColor = color;
			this.title = new TextView[titles.length];
			for (int i = 0; i < titles.length; i++) {
				title[i] = new TextView(mContext);
				title[i].setText(titles[i]);
				title[i].setTextSize(fontSize);
				title[i].setTextColor(fontColor);
				title[i].setGravity(Gravity.CENTER);
				title[i].setPadding(10, 10, 10, 10);
				title[i].setBackgroundResource(R.drawable.toolbar_menu_release);
			}
		}

		public int getCount() {
			return title.length;
		}

		public Object getItem(int position) {
			return title[position];
		}

		public long getItemId(int position) {
			return title[position].getId();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			if (convertView == null) {
				v = title[position];
			} else {
				v = convertView;
			}
			return v;
		}

	}

	public void initMenuResourse() {
		mOptionTitle = new String[] { getString(R.string.popmenu_common),
				getString(R.string.popmenu_set),
				getString(R.string.popmenu_other) };
		mOptionTextArray1 = new String[] { getString(R.string.add_account_action),
				getString(R.string.compose_action),
				getString(R.string.refresh_action),
				getString(R.string.user_idear) };
		mOptionImageArray1 = new int[] { R.drawable.ic_menu_addaccount,
				R.drawable.ic_menu_signature, R.drawable.ic_menu_reply,
				R.drawable.ic_menu_settings_icon };

		mOptionTextArraySet = new String[] {
				getString(R.string.system_set_title),
				getString(R.string.storage_result), getString(R.string.about),getString(R.string.share_app) };
		mOptionImageArrayset = new int[] { R.drawable.ic_menu_select_all,
				R.drawable.ic_menu_save_draft,R.drawable.ic_menu_help, R.drawable.ic_menu_email_deselect_mail };

		mOptionTextArrayTool = new String[] {
				getString(R.string.music_backgroud_lable),
				getString(R.string.skin_manage_menu),
				getString(R.string.soft_update) ,
				getString(R.string.search_action)};
		mOptionImageArrayTool = new int[] { 
				R.drawable.ic_menu_music, R.drawable.ic_menu_skin,
				R.drawable.ic_menu_forward_mail,
				R.drawable.ic_menu_query};

	}
	// =================PopMenu=============
	
	public boolean isExit;

    public void exit() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(getApplicationContext(),getString(R.string.common_exit_back_msg),Toast.LENGTH_SHORT).show();
            mHandler.backTwoExit();
        } else {         
            finish();
        }
    }
    
    public boolean isInstallClientWeixin(){
    	String packageName = "com.tencent.mm";
		PackageInfo pi = null;
		try {
			pi = getPackageManager().getPackageInfo(packageName, 0);
		} catch (Throwable t) {
		    Toast.makeText(this, getString(R.string.media_label_share_weixin_no_install), Toast.LENGTH_SHORT).show();
		    t.printStackTrace();
		    return false;
		}

		if (pi == null) {
			Toast.makeText(this, getString(R.string.media_label_share_weixin_no_install), Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
    }
    
    
    /**
     * 需要注意的是，SendMessageToWX.Req的scene成员，如果scene填WXSceneSession，那么消息会发送至微信的会话内。
     * 如果scene填WXSceneTimeline（微信4.2以上支持，如果需要检查微信版本支持API的情况，
     * 可调用IWXAPI的getWXAppSupportAPI方法
     * ,0x21020001及以上支持发送朋友圈），那么消息会发送至朋友圈。scene默认值为WXSceneSession。
     * 
     * @param share_id
     * @param info
     */
    public void shareWeiXin(int share_id, ShareContentBean info) {
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        if (info.getUrl() != null && !info.getUrl().equals("")) {
            WXWebpageObject webpage = new WXWebpageObject();
            webpage.webpageUrl = info.getUrl();

            WXMediaMessage msg = new WXMediaMessage(webpage);
            msg.title = info.getTitle();
            msg.description = info.getContent();
//            if (info.getPic() != null && !info.getPic().equals("")) {
//                Bitmap thumb = BitmapUtil.getCompressImage(mFileCache.getFile(Config.SHARE_FILE_NAME).getAbsolutePath());
//                msg.thumbData = Util.bmpToByteArray(thumb, true);
//            }
            Bitmap thumb = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
            msg.thumbData = Util.bmpToByteArray(thumb, true);
            req.transaction = buildTransaction("webpage");
            req.message = msg;
        } else {
            WXTextObject textObj = new WXTextObject();
            textObj.text = info.getContent();

            WXMediaMessage msg = new WXMediaMessage();
            msg.mediaObject = textObj;
            msg.description = info.getContent();

            req.transaction = buildTransaction("text");
            req.message = msg;
        }

        req.scene = share_id == Configs.SHARE_WX_PY_ID ? SendMessageToWX.Req.WXSceneSession
                : SendMessageToWX.Req.WXSceneTimeline;// 判断发送至朋友圈
        mAplication.api.sendReq(req);
       
    }
    public String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
    
    
}
