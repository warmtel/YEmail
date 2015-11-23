package com.mail163.email.activity;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.mail163.email.Controller;
import com.mail163.email.Email;
import com.mail163.email.R;
import com.mail163.email.Utility;
import com.mail163.email.Email.Global;
import com.mail163.email.activity.setup.AccountSettings;
import com.mail163.email.mail.AuthenticationFailedException;
import com.mail163.email.mail.CertificateValidationException;
import com.mail163.email.mail.MessagingException;
import com.mail163.email.provider.EmailContent;
import com.mail163.email.provider.EmailContent.Account;
import com.mail163.email.provider.EmailContent.AccountColumns;
import com.mail163.email.provider.EmailContent.Mailbox;
import com.mail163.email.provider.EmailContent.MailboxColumns;
import com.mail163.email.provider.EmailContent.Message;
import com.mail163.email.provider.EmailContent.MessageColumns;
import com.mail163.email.util.Utiles;
import com.mobclick.android.MobclickAgent;

public class MailboxList extends ListActivity implements OnItemClickListener,
		OnClickListener {

	// Intent extras (internal to this activity)
	private static final String EXTRA_ACCOUNT_ID = "com.android.email.activity._ACCOUNT_ID";

	private static final String MAILBOX_SELECTION = MailboxColumns.ACCOUNT_KEY
			+ "=?" + " AND " + MailboxColumns.TYPE + "<"
			+ Mailbox.TYPE_NOT_EMAIL + " AND " + MailboxColumns.FLAG_VISIBLE
			+ "=1";
	private static final String MESSAGE_MAILBOX_ID_SELECTION = MessageColumns.MAILBOX_KEY
			+ "=?";

	private static final String MAILBOX_ID_SELECTION = MailboxColumns.ID
			+ " =?";

	// UI support
	private ListView mListView;
	private ProgressBar mProgressIcon;
	private TextView mErrorBanner;

	private MailboxListAdapter mListAdapter;
	private MailboxListHandler mHandler;
	private ControllerResults mControllerCallback;

	// DB access
	private long mAccountId;
	private LoadMailboxesTask mLoadMailboxesTask;
	private AsyncTask<Void, Void, Object[]> mLoadAccountNameTask;
	private MessageCountTask mMessageCountTask;

	private long mSentMailboxKey = -1;
	private long mDraftMailboxKey = -1;
	private long mTrashMailboxKey = -1;
	private int mCountSent = 0;
	private int mUnreadCountDraft = 0;
	private int mUnreadCountTrash = 0;
	// ===============PopupMenu==============
	private PopupWindow popup;
	private GridView mMenuContentGridView;

	private int mTitleMenuIndex;

	public String[] mOptionTextArray1;
	public int[] mOptionImageArray1;

	// ===============PopupMenu==============
	/**
	 * Open a specific account.
	 * 
	 * @param context
	 * @param accountId
	 *            the account to view
	 */
	public static void actionHandleAccount(Context context, long accountId) {
		Intent intent = new Intent(context, MailboxList.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(EXTRA_ACCOUNT_ID, accountId);
		context.startActivity(intent);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.mailbox_list);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.list_title);

		mHandler = new MailboxListHandler();
		mControllerCallback = new ControllerResults();
		mListView = getListView();
		if (Global.backDrawable == null) {
			Global.backDrawable = Utiles.fetchBackDrawable(this);
			setTheme(R.style.XTheme);
		}
		if (null != Global.backDrawable) {
			if(Global.skinName.contains(Global.whiteSkin)){
				setTheme(R.style.Default);
	        }else{
	        	setTheme(R.style.XTheme);
	        }
			mListView.setBackgroundDrawable(Global.backDrawable);
		}

		mListView.setCacheColorHint(getResources().getColor(
				R.color.background_cachhint));

		mProgressIcon = (ProgressBar) findViewById(R.id.title_progress_icon);
		mErrorBanner = (TextView) findViewById(R.id.connection_error_text);

		mListView.setOnItemClickListener(this);
		mListView.setItemsCanFocus(false);
		registerForContextMenu(mListView);

		mListAdapter = new MailboxListAdapter(this);
		setListAdapter(mListAdapter);

		((Button) findViewById(R.id.account_title_button))
				.setOnClickListener(this);

		mAccountId = getIntent().getLongExtra(EXTRA_ACCOUNT_ID, -1);
		if (mAccountId != -1) {
			mLoadMailboxesTask = new LoadMailboxesTask(mAccountId);
			mLoadMailboxesTask.execute();
		} else {
			finish();
		}

		((TextView) findViewById(R.id.title_left_text))
				.setText(R.string.mailbox_list_title);

		// Go to the database for the account name
		mLoadAccountNameTask = new AsyncTask<Void, Void, Object[]>() {
			@Override
			protected Object[] doInBackground(Void... params) {

				Controller.getInstance(getApplication()).updateMailboxList(
						mAccountId, mControllerCallback);

				String accountName = null;
				Uri uri = ContentUris.withAppendedId(Account.CONTENT_URI,
						mAccountId);
				Cursor c = MailboxList.this.getContentResolver().query(uri,
						new String[] { AccountColumns.DISPLAY_NAME }, null,
						null, null);
				try {
					if (c.moveToFirst()) {
						accountName = c.getString(0);
					}
				} finally {
					c.close();
				}
				int nAccounts = EmailContent.count(MailboxList.this,
						Account.CONTENT_URI, null, null);
				return new Object[] { accountName, nAccounts };
			}

			@Override
			protected void onPostExecute(Object[] result) {
				if (result == null) {
					return;
				}
				final String accountName = (String) result[0];
				// accountName is null if account name can't be retrieved or
				// query exception
				if (accountName == null) {
					// something is wrong with this account
					finish();
				}

				final int nAccounts = (Integer) result[1];
				setTitleAccountName(accountName, nAccounts > 1);
			}

		}.execute();

		initMenuResourse();
		initPopupMenu();
	}

	@Override
	public void onPause() {
		super.onPause();
		 MobclickAgent.onPause(this);
		Controller.getInstance(getApplication()).removeResultCallback(
				mControllerCallback);
	}

	@Override
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
		Controller.getInstance(getApplication()).addResultCallback(
				mControllerCallback);

		// Exit immediately if the accounts list has changed (e.g. externally
		// deleted)
		if (Email.getNotifyUiAccountsChanged()) {
			Welcome.actionStart(this);
			finish();
			return;
		}

		updateMessageCount();

		// TODO: may need to clear notifications here
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		Utility.cancelTaskInterrupt(mLoadMailboxesTask);
		mLoadMailboxesTask = null;
		Utility.cancelTaskInterrupt(mLoadAccountNameTask);
		mLoadAccountNameTask = null;
		Utility.cancelTaskInterrupt(mMessageCountTask);
		mMessageCountTask = null;

		mListAdapter.changeCursor(null);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.account_title_button:
			onAccounts();
			break;
		}
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		if (getTypeByMailboxId(this, id) == Mailbox.TYPE_NEWBOX) {
			onCompose();
		} else {
			onOpenMailbox(id);
		}
	}

	private static int getTypeByMailboxId(Context context, long mailboxId) {
		int type = 0;
		Cursor c = context.getContentResolver().query(Mailbox.CONTENT_URI,
				new String[] { EmailContent.Mailbox.TYPE },
				MAILBOX_ID_SELECTION,
				new String[] { String.valueOf(mailboxId) }, null);
		try {
			if (c.moveToFirst()) {

				return c.getInt(0);
			}
		} finally {
			c.close();
		}
		return type;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!Global.skinName.equals(Global.SKIN_NAME)) {
			menu.add("menu");// 必须创建一项
			return super.onCreateOptionsMenu(menu);
		} else {
			super.onCreateOptionsMenu(menu);
			getMenuInflater().inflate(R.menu.mailbox_list_option, menu);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			onRefresh(-1);
			return true;
		case R.id.accounts:
			onAccounts();
			return true;
		case R.id.compose:
			onCompose();
			return true;
		case R.id.account_settings:
			onEditAccount();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo info) {
		super.onCreateContextMenu(menu, v, info);
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) info;
		Cursor c = (Cursor) mListView.getItemAtPosition(menuInfo.position);
		String folderName = Utility.FolderProperties.getInstance(
				MailboxList.this).getDisplayName(
				Integer.valueOf(c.getString(mListAdapter.COLUMN_TYPE)));
		if (folderName == null) {
			folderName = c.getString(mListAdapter.COLUMN_DISPLAY_NAME);
		}

		menu.setHeaderTitle(folderName);
		getMenuInflater().inflate(R.menu.mailbox_list_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		int type = getTypeByMailboxId(this, info.id);
		switch (item.getItemId()) {
		case R.id.refresh:
			if (type != Mailbox.TYPE_NEWBOX) {
				onRefresh(info.id);
			}
			break;
		case R.id.open:
			if (type == Mailbox.TYPE_NEWBOX) {
				onCompose();
			} else {
				onOpenMailbox(info.id);
			}
			break;
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * Refresh the mailbox list, or a single mailbox
	 * 
	 * @param mailboxId
	 *            -1 for all
	 */
	private void onRefresh(long mailboxId) {
		Controller controller = Controller.getInstance(getApplication());
		mHandler.progress(true);
		if (mailboxId >= 0) {
			controller
					.updateMailbox(mAccountId, mailboxId, mControllerCallback);
		} else {
			controller.updateMailboxList(mAccountId, mControllerCallback);
		}
	}

	private void onAccounts() {
		AccountFolderList.actionShowAccounts(this);
		finish();
	}

	private void onEditAccount() {
		AccountSettings.actionSettings(this, mAccountId);
	}

	private void onOpenMailbox(long mailboxId) {
		MessageList.actionHandleMailbox(this, mailboxId);
	}

	private void onCompose() {
		MessageCompose.actionCompose(this, mAccountId);
	}

	private void setTitleAccountName(String accountName,
			boolean showAccountsButton) {
		TextView accountsButton = (TextView) findViewById(R.id.account_title_button);
		TextView textPlain = (TextView) findViewById(R.id.title_right_text);
		if (showAccountsButton) {
			accountsButton.setVisibility(View.VISIBLE);
			textPlain.setVisibility(View.GONE);
			accountsButton.setText(accountName);
		} else {
			accountsButton.setVisibility(View.GONE);
			textPlain.setVisibility(View.VISIBLE);
			textPlain.setText(accountName);
		}
	}

	/**
	 * Async task for loading the mailboxes for a given account
	 */
	private class LoadMailboxesTask extends AsyncTask<Void, Void, Cursor> {

		private long mAccountKey;

		/**
		 * Special constructor to cache some local info
		 */
		public LoadMailboxesTask(long accountId) {
			mAccountKey = accountId;
		}

		@Override
		protected Cursor doInBackground(Void... params) {
			Cursor c = MailboxList.this.managedQuery(
					EmailContent.Mailbox.CONTENT_URI,
					MailboxList.this.mListAdapter.PROJECTION,
					MAILBOX_SELECTION, new String[] { String
							.valueOf(mAccountKey) }, null);
			// MailboxColumns.TYPE + "," + MailboxColumns.DISPLAY_NAME);
			mDraftMailboxKey = -1;
			mTrashMailboxKey = -1;
			mSentMailboxKey = -1;
			c.moveToPosition(-1);
			while (c.moveToNext()) {
				long mailboxId = c.getInt(mListAdapter.COLUMN_ID);
				switch (c.getInt(mListAdapter.COLUMN_TYPE)) {
				case Mailbox.TYPE_DRAFTS:
					mDraftMailboxKey = mailboxId;
					break;
				case Mailbox.TYPE_TRASH:
					mTrashMailboxKey = mailboxId;
					break;
				case Mailbox.TYPE_SENT:
					mSentMailboxKey = mailboxId;
					break;
				}
			}
			if (isCancelled()) {
				c.close();
				c = null;
			}
			return c;
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			if (cursor == null || cursor.isClosed()) {
				return;
			}
			MailboxList.this.mListAdapter.changeCursor(cursor);
			updateMessageCount();
		}
	}

	private class MessageCountTask extends AsyncTask<Void, Void, int[]> {

		@Override
		protected int[] doInBackground(Void... params) {
			int[] counts = new int[3];
			if (mDraftMailboxKey != -1) {
				counts[0] = EmailContent.count(MailboxList.this,
						Message.CONTENT_URI, MESSAGE_MAILBOX_ID_SELECTION,
						new String[] { String.valueOf(mDraftMailboxKey) });
			} else {
				counts[0] = -1;
			}
			if (mTrashMailboxKey != -1) {
				counts[1] = EmailContent.count(MailboxList.this,
						Message.CONTENT_URI, MESSAGE_MAILBOX_ID_SELECTION,
						new String[] { String.valueOf(mTrashMailboxKey) });
			} else {
				counts[1] = -1;
			}
			if (mSentMailboxKey != -1) {
				counts[2] = EmailContent.count(MailboxList.this,
						Message.CONTENT_URI, MESSAGE_MAILBOX_ID_SELECTION,
						new String[] { String.valueOf(mSentMailboxKey) });
			} else {
				counts[2] = -1;
			}
			return counts;
		}

		@Override
		protected void onPostExecute(int[] counts) {
			boolean countChanged = false;
			if (counts == null) {
				return;
			}
			if (counts[0] != -1) {
				if (mUnreadCountDraft != counts[0]) {
					mUnreadCountDraft = counts[0];
					countChanged = true;
				}
			} else {
				mUnreadCountDraft = 0;
			}
			if (counts[1] != -1) {
				if (mUnreadCountTrash != counts[1]) {
					mUnreadCountTrash = counts[1];
					countChanged = true;
				}
			} else {
				mUnreadCountTrash = 0;
			}
			if (counts[2] != -1) {
				if (mCountSent != counts[2]) {
					mCountSent = counts[2];
					countChanged = true;
				}
			} else {
				mUnreadCountTrash = 0;
			}

			if (countChanged) {
				mListAdapter.notifyDataSetChanged();
			}
		}
	}

	private void updateMessageCount() {
		if (mAccountId == -1 || mListAdapter.getCursor() == null) {
			return;
		}
		if (mMessageCountTask != null
				&& mMessageCountTask.getStatus() != MessageCountTask.Status.FINISHED) {
			mMessageCountTask.cancel(true);
		}
		mMessageCountTask = (MessageCountTask) new MessageCountTask().execute();
	}

	/**
	 * Handler for UI-thread operations (when called from callbacks or any other
	 * threads)
	 */
	class MailboxListHandler extends Handler {
		private static final int MSG_PROGRESS = 1;
		private static final int MSG_ERROR_BANNER = 2;
		private static final int MSG_SEND_SUCCESS = 3;

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
			case MSG_ERROR_BANNER:
				String message = (String) msg.obj;
				boolean isVisible = mErrorBanner.getVisibility() == View.VISIBLE;
				if (message != null) {
					Toast.makeText(MailboxList.this, message,
							Toast.LENGTH_SHORT).show();
					// mErrorBanner.setText(message);
					// if (!isVisible) {
					// mErrorBanner.setVisibility(View.VISIBLE);
					// mErrorBanner.startAnimation(
					// AnimationUtils.loadAnimation(
					// MailboxList.this, R.anim.header_appear));
					// }
				} else {
					if (isVisible) {
						mErrorBanner.setVisibility(View.GONE);
						// mErrorBanner.startAnimation(
						// AnimationUtils.loadAnimation(
						// MailboxList.this, R.anim.header_disappear));
					}
				}
				break;
			case MSG_SEND_SUCCESS:
				boolean showSuccess = (msg.arg1 != 0);
				if (showSuccess) {
					Toast
							.makeText(
									MailboxList.this,
									MailboxList.this
											.getString(R.string.message_send_success_toast),
									Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(
							MailboxList.this,
							MailboxList.this
									.getString(R.string.message_sending_toast),
							Toast.LENGTH_SHORT).show();
				}
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

		/**
		 * Called from any thread to show or hide the connection error banner.
		 * 
		 * @param message
		 *            error text or null to hide the box
		 */
		public void showErrorBanner(String message) {
			android.os.Message msg = android.os.Message.obtain();
			msg.what = MSG_ERROR_BANNER;
			msg.obj = message;
			sendMessage(msg);
		}

		public void sendMessageToast(boolean showSuccess) {
			android.os.Message msg = android.os.Message.obtain();
			msg.what = MSG_SEND_SUCCESS;
			msg.arg1 = showSuccess ? 1 : 0;
			sendMessage(msg);
		}
	}

	/**
	 * Callback for async Controller results.
	 */
	private class ControllerResults implements Controller.Result {

		// TODO report errors into UI
		public void updateMailboxListCallback(MessagingException result,
				long accountKey, int progress) {
			if (accountKey == mAccountId) {
				updateBanner(result, progress);
				updateProgress(result, progress);
			}
		}

		// TODO report errors into UI
		public void updateMailboxCallback(MessagingException result,
				long accountKey, long mailboxKey, int progress,
				int numNewMessages) {
			if (result != null || progress == 100) {
				Email.updateMailboxRefreshTime(mailboxKey);
			}
			if (accountKey == mAccountId) {
				updateBanner(result, progress);
				updateProgress(result, progress);
			}
		}

		public void loadMessageForViewCallback(MessagingException result,
				long messageId, int progress) {
		}

		public void loadAttachmentCallback(MessagingException result,
				long messageId, long attachmentId, int progress) {
		}

		public void serviceCheckMailCallback(MessagingException result,
				long accountId, long mailboxId, int progress, long tag) {
		}

		public void sendMailCallback(MessagingException result, long accountId,
				long messageId, int progress) {

			if (progress == 100) {
				mHandler.sendMessageToast(true);
			}
			if (accountId == mAccountId) {
				updateBanner(result, progress);
				updateProgress(result, progress);
			}
		}

		private void updateProgress(MessagingException result, int progress) {
			if (result != null || progress == 100) {
				mHandler.progress(false);

			} else if (progress == 0) {
				mHandler.progress(true);

			}
		}

		/**
		 * Show or hide the connection error banner, and convert the various
		 * MessagingException variants into localizable text. There is
		 * hysteresis in the show/hide logic: Once shown, the banner will remain
		 * visible until some progress is made on the connection. The goal is to
		 * keep it from flickering during retries in a bad connection state.
		 * 
		 * @param result
		 * @param progress
		 */
		private void updateBanner(MessagingException result, int progress) {
			if (result != null) {
				int id = R.string.status_network_error;
				if (result instanceof AuthenticationFailedException) {
					id = R.string.account_setup_failed_dlg_auth_message;
				} else if (result instanceof CertificateValidationException) {
					id = R.string.account_setup_failed_dlg_certificate_message;
				} else {
					switch (result.getExceptionType()) {
					case MessagingException.IOERROR:
						id = R.string.account_setup_failed_ioerror;
						break;
					case MessagingException.TLS_REQUIRED:
						id = R.string.account_setup_failed_tls_required;
						break;
					case MessagingException.AUTH_REQUIRED:
						id = R.string.account_setup_failed_auth_required;
						break;
					case MessagingException.GENERAL_SECURITY:
						id = R.string.account_setup_failed_security;
						break;
					}
				}
				mHandler.showErrorBanner(getString(id));
			} else if (progress > 0) {
				mHandler.showErrorBanner(null);
			}
		}
	}

	/**
	 * The adapter for displaying mailboxes.
	 */
	/* package */class MailboxListAdapter extends CursorAdapter {

		public final String[] PROJECTION = new String[] { MailboxColumns.ID,
				MailboxColumns.DISPLAY_NAME, MailboxColumns.UNREAD_COUNT,
				MailboxColumns.TYPE };
		public final int COLUMN_ID = 0;
		public final int COLUMN_DISPLAY_NAME = 1;
		public final int COLUMN_UNREAD_COUNT = 2;
		public final int COLUMN_TYPE = 3;

		Context mContext;
		private LayoutInflater mInflater;

		public MailboxListAdapter(Context context) {
			super(context, null);
			mContext = context;
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			int type = cursor.getInt(COLUMN_TYPE);
			String text = Utility.FolderProperties.getInstance(context)
					.getDisplayName(type);

			if (text == null) {
				text = cursor.getString(COLUMN_DISPLAY_NAME);
			}
			TextView nameView = (TextView) view.findViewById(R.id.mailbox_name);
			if (text != null) {
				nameView.setText(text);
			}

			// TODO get/track live folder status
			text = null;
			TextView statusView = (TextView) view
					.findViewById(R.id.mailbox_status);
			if (text != null) {
				statusView.setText(text);
				statusView.setVisibility(View.VISIBLE);
			} else {
				statusView.setVisibility(View.GONE);
			}
			View chipView = view.findViewById(R.id.chip);
			chipView.setBackgroundResource(Email
					.getAccountColorResourceId(mAccountId));
			// TODO do we use a different count for special mailboxes (total
			// count vs. unread)
			int count = -1;
			switch (type) {
			case Mailbox.TYPE_SENT:
				count = mCountSent;
				text = String.valueOf(count);
				break;
			case Mailbox.TYPE_DRAFTS:
				count = mUnreadCountDraft;
				text = String.valueOf(count);
				break;
			case Mailbox.TYPE_TRASH:
				count = mUnreadCountTrash;
				text = String.valueOf(count);
				break;
			default:
				text = cursor.getString(COLUMN_UNREAD_COUNT);
				if (text != null) {
					count = Integer.valueOf(text);
				}
				break;
			}
			TextView unreadCountView = (TextView) view
					.findViewById(R.id.new_message_count);
			TextView allCountView = (TextView) view
					.findViewById(R.id.all_message_count);
			// If the unread count is zero, not to show countView.
			if (count > 0) {
				nameView.setTypeface(Typeface.DEFAULT_BOLD);
				switch (type) {
				case Mailbox.TYPE_DRAFTS:
				case Mailbox.TYPE_OUTBOX:
				case Mailbox.TYPE_SENT:
					unreadCountView.setVisibility(View.GONE);
					allCountView.setVisibility(View.VISIBLE);
					allCountView.setText(text);
					break;
				case Mailbox.TYPE_TRASH:
					unreadCountView.setVisibility(View.GONE);
					allCountView.setVisibility(View.VISIBLE);
					allCountView.setText(text);
					break;
				default:
					allCountView.setVisibility(View.GONE);
					unreadCountView.setVisibility(View.VISIBLE);
					unreadCountView.setText(text);
					break;
				}
			} else {
				nameView.setTypeface(Typeface.DEFAULT);
				allCountView.setVisibility(View.GONE);
				unreadCountView.setVisibility(View.GONE);
			}

			ImageView folderIcon = (ImageView) view
					.findViewById(R.id.folder_icon);
			folderIcon.setImageDrawable(Utility.FolderProperties.getInstance(
					context).getIconIds(type));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return mInflater.inflate(R.layout.mailbox_list_item, parent, false);
		}
	}

	// =================PopMenu=============
	private void initPopupMenu() {
		LinearLayout mPopupMenuLayout = new LinearLayout(this);
		mPopupMenuLayout.setOrientation(LinearLayout.VERTICAL);
		mMenuContentGridView = new GridView(this);
		mMenuContentGridView.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		mMenuContentGridView.setSelector(R.drawable.toolbar_menu_item);
		mMenuContentGridView.setNumColumns(mOptionTextArray1.length); // the number of child Option
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
						onAccounts();
					}
					if (childOptionIndex == 1) {
						onEditAccount();
					}
					if (childOptionIndex == 2) {
						onRefresh(-1);
					}
					if (childOptionIndex == 3) {
						onCompose();
					}
					break;

				}
				popup.dismiss();
			}
		});
		mPopupMenuLayout.addView(mMenuContentGridView);

		popup = new PopupWindow(mPopupMenuLayout, LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT);
		popup.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.menu_bg));// 设置menu菜单背景
		popup.setFocusable(true);// menu菜单获得焦点 如果没有获得焦点menu菜单中的控件事件无法响应
		popup.update();

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

	public void initMenuResourse() {
		mOptionTextArray1 = new String[] { 
				getString(R.string.accounts_action),
				getString(R.string.account_settings_action),
				getString(R.string.refresh_action),
				getString(R.string.compose_action)
				 };
		mOptionImageArray1 = new int[] { 
				R.drawable.ic_menu_addaccount,
				R.drawable.ic_menu_settings_icon,
				R.drawable.ic_menu_reply,
				R.drawable.ic_menu_signature };


	}
	// =================PopMenu=============
}
