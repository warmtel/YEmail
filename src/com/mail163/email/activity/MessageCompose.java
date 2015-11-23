package com.mail163.email.activity;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Rect;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.OpenableColumns;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.mail163.email.Controller;
import com.mail163.email.Email;
import com.mail163.email.EmailAddressAdapter;
import com.mail163.email.EmailAddressValidator;
import com.mail163.email.Logs;
import com.mail163.email.R;
import com.mail163.email.Recorder;
import com.mail163.email.Utility;
import com.mail163.email.Email.Global;
import com.mail163.email.mail.Address;
import com.mail163.email.mail.MessagingException;
import com.mail163.email.mail.internet.EmailHtmlUtil;
import com.mail163.email.mail.internet.MimeUtility;
import com.mail163.email.provider.EmailContent;
import com.mail163.email.provider.EmailContent.Account;
import com.mail163.email.provider.EmailContent.Attachment;
import com.mail163.email.provider.EmailContent.Body;
import com.mail163.email.provider.EmailContent.BodyColumns;
import com.mail163.email.provider.EmailContent.Message;
import com.mail163.email.provider.EmailContent.MessageColumns;
import com.mail163.email.util.ContactsUtils;
import com.mail163.email.util.Utiles;
import com.mobclick.android.MobclickAgent;

public class MessageCompose extends Activity implements OnClickListener,
		OnFocusChangeListener, Recorder.OnStateChangedListener, OnTouchListener {
	private static final String ACTION_REPLY = "com.android.email.intent.action.REPLY";
	private static final String ACTION_REPLY_ALL = "com.android.email.intent.action.REPLY_ALL";
	private static final String ACTION_FORWARD = "com.android.email.intent.action.FORWARD";
	private static final String ACTION_EDIT_DRAFT = "com.android.email.intent.action.EDIT_DRAFT";

	private static final String EXTRA_ACCOUNT_ID = "account_id";
	private static final String EXTRA_MESSAGE_ID = "message_id";
	private static final String STATE_KEY_CC_SHOWN = "com.android.email.activity.MessageCompose.ccShown";
	private static final String STATE_KEY_BCC_SHOWN = "com.android.email.activity.MessageCompose.bccShown";
	private static final String STATE_KEY_QUOTED_TEXT_SHOWN = "com.android.email.activity.MessageCompose.quotedTextShown";
	private static final String STATE_KEY_SOURCE_MESSAGE_PROCED = "com.android.email.activity.MessageCompose.stateKeySourceMessageProced";
	private static final String STATE_KEY_DRAFT_ID = "com.android.email.activity.MessageCompose.draftId";

	private static final String EXTRA_ADD_ATTACHMENT = "addAttachment";
	private static final String EXTRA_ADD_CONTACTS = "addContacts";

	/* Activity request code */
	private static final int REQUEST_CODE_ADD_ATTACHMENT = 1;
	private static final int REQUEST_CODE_ADD_CONTACTS = 2;
	private static final int REQUEST_CODE_TAKE_VIDEO = 3;
	private static final int REQUEST_CODE_ADD_ADRESS_CONTACTS = 9;
	private static final int DIALOG_ADD_CONTACTS = 7;
	private static final int MSG_UPDATE_TITLE = 3;
	private static final int MSG_SKIPPED_ATTACHMENTS = 4;
	private static final int MSG_DISCARDED_DRAFT = 6;
	private static final int MSG_SEND_SUCCESS = 8;

	private static final int ACTIVITY_REQUEST_PICK_ATTACHMENT = 1;

	private static final String[] ATTACHMENT_META_NAME_PROJECTION = { OpenableColumns.DISPLAY_NAME };
	private static final int ATTACHMENT_META_NAME_COLUMN_DISPLAY_NAME = 0;

	private static final String[] ATTACHMENT_META_SIZE_PROJECTION = { OpenableColumns.SIZE };
	private static final int ATTACHMENT_META_SIZE_COLUMN_SIZE = 0;

	private SharedPreferences mSharedPreference;

	// private final String PREFERENCES_NAME = "RecentlyMailContacts";
	// Is set while the draft is saved by a background thread.
	// Is static in order to be shared between the two activity instances
	// on orientation change.
	private static boolean sSaveInProgress = false;
	// lock and condition for sSaveInProgress
	private static final Object sSaveInProgressCondition = new Object();

	private Account mAccount;

	// mDraft has mId > 0 after the first draft save.
	private Message mDraft = new Message();

	// mSource is only set for REPLY, REPLY_ALL and FORWARD, and contains the
	// source message.
	private Message mSource;

	// we use mAction instead of Intent.getAction() because sometimes we need to
	// re-write the action to EDIT_DRAFT.
	private String mAction;

	/**
	 * Indicates that the source message has been processed at least once and
	 * should not be processed on any subsequent loads. This protects us from
	 * adding attachments that have already been added from the restore of the
	 * view state.
	 */
	private boolean mSourceMessageProcessed = false;

	private AddressTextView mToView;
	private AddressTextView mCcView;
	private AddressTextView mBccView;
	private AddressTextView mContactView;
	private LinearLayout mComposeLayout;
	private LinearLayout mCcLayout;
	private LinearLayout mBccLayout;
	private ImageView mFromImgView;
	private ImageButton mToImgView;
	private ImageButton mCcImgView;
	private ImageButton mBccImgView;
	private EditText mSubjectView;
	private EditText mMessageContentView;
	private Button mSendButton;
	private Button mDiscardButton;
	private Button mSaveButton;
	private ImageView mRecordAttachment;
	private LinearLayout mAttachments;
	private View mQuotedTextBar;
	private ImageButton mQuotedTextDelete;
	private WebView mQuotedText;
	private TextView mLeftTitle;
	private TextView mRightTitle;
	private Button mAddAttachButton;
	private Spinner mPrioritySpinner;

	private Controller mController;
	private Listener mListener;
	private boolean mDraftNeedsSaving;
	private boolean mMessageLoaded;
	private AsyncTask mLoadAttachmentsTask;
	private AsyncTask mSaveMessageTask;
	private AsyncTask mLoadMessageTask;
	private AddEmailContactsTask mAddEmailContactsTask;
	private EmailAddressAdapter mAddressAdapterTo;
	private EmailAddressAdapter mAddressAdapterCc;
	private EmailAddressAdapter mAddressAdapterBcc;
	// ===============PopupMenu==============
	private PopupWindow popup;
	private GridView mMenuContentGridView;

	private int mTitleMenuIndex;

	public String[] mOptionTextArray1;
	public int[] mOptionImageArray1;
	public String skinName = "";
	// ===============PopupMenu==============
	private File mSoundFile = null;

	private Recorder mRecorder;
	private Vibrator vibrator;
	private long[] vibrate = { 0, 100 };
	private LayoutInflater mInflater;
	private View mView;
	private LinearLayout mLinearLayoutrcding, mLinearLayoutloading,
			mLinearLayouttooshort;
	public SoundRecorderHandler mSoundHandler = new SoundRecorderHandler();

	class SoundRecorderHandler extends Handler {
		private static final int RECORDING_STATE = 0x10001;
		private static final int RECORDING_STATE_PREPARE = 0x10002;
		private static final int RECORDING_STATE_END = 0x10003;
		private static final int RECORDING_NOSDCARD = 0x10004;

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case RECORDING_NOSDCARD:
				Toast.makeText(MessageCompose.this,
						getString(R.string.sound_play_sdcard),
						Toast.LENGTH_SHORT).show();
				break;
			case RECORDING_STATE:
				mRecordAttachment
						.setBackgroundResource(R.drawable.pop_talk_bottom_press);
				break;
			case RECORDING_STATE_PREPARE:
				mLinearLayoutloading.setVisibility(View.GONE); // 加载动画完成
				mLinearLayoutrcding.setVisibility(View.VISIBLE); // 开始录音
				break;
			case RECORDING_STATE_END:
				mRecordAttachment
						.setBackgroundResource(R.drawable.pop_talk_bottom);
				mLinearLayoutrcding.setVisibility(View.GONE);
				if (mRecorder.sampleLength() < 2) {// 2s
					Toast
							.makeText(MessageCompose.this,
									getString(R.string.sound_record),
									Toast.LENGTH_LONG).show();
					if (mRecorder.sampleFile().exists()) {
						mRecorder.sampleFile().delete();
					}
					return;
				}
				addAttachment(Uri.fromFile(mRecorder.sampleFile()));
				break;

			default:
				super.handleMessage(msg);
			}
		}

		public void mRecordStart() {
			mSoundHandler.sendEmptyMessage(RECORDING_STATE);
		}

		public void mRecordPrepare() {
			mSoundHandler.sendEmptyMessage(RECORDING_STATE_PREPARE);
		}

		public void mRecordEnd() {
			mSoundHandler.sendEmptyMessage(RECORDING_STATE_END);
		}

		public void mRecordNoSdcard() {
			mSoundHandler.sendEmptyMessage(RECORDING_NOSDCARD);
		}

	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_TITLE:
				updateTitle();
				break;
			case MSG_SKIPPED_ATTACHMENTS:
				Toast
						.makeText(
								MessageCompose.this,
								getString(R.string.message_compose_attachments_skipped_toast),
								Toast.LENGTH_LONG).show();
				break;
			case MSG_SEND_SUCCESS:
				showSendWindow();
				// Toast.makeText(
				// MessageCompose.this,
				// MessageCompose.this
				// .getString(R.string.message_sending_toast),
				// Toast.LENGTH_SHORT).show();

			default:
				super.handleMessage(msg);
				break;
			}

		}
	};

	public ImageView mImageView;

	public void showSendWindow() {
		mImageView = SendImageView.getInstance(this);
		mImageView.setBackgroundResource(R.drawable.send_message_toast1);
		Rect frame = new Rect();
		getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
		SendImageView.TOOL_BAR_HIGH = frame.top;

		WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
		WindowManager.LayoutParams params = SendImageView.params;

		params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT | WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
		params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

		params.width = WindowManager.LayoutParams.WRAP_CONTENT;
		params.height = WindowManager.LayoutParams.WRAP_CONTENT;

		params.gravity = Gravity.LEFT | Gravity.TOP;

		Display display = wm.getDefaultDisplay();
		int screenWidth = display.getWidth();
		int screenHeight = display.getHeight();
		// 以屏幕左上角为原点，设置x、y初始值
		params.x = (screenWidth - Utiles.dip2px(this, 177)) / 2;
		params.y = (screenHeight - Utiles.dip2px(this, 31)) / 2;

		wm.addView(mImageView, params);
	}

	/**
	 * Compose a new message using the given account. If account is -1 the
	 * default account will be used.
	 * 
	 * @param context
	 * @param accountId
	 */
	public static void actionCompose(Context context, long accountId) {
		try {
			Intent i = new Intent(context, MessageCompose.class);
			i.putExtra(EXTRA_ACCOUNT_ID, accountId);
			context.startActivity(i);
		} catch (ActivityNotFoundException anfe) {
			// Swallow it - this is usually a race condition, especially under
			// automated test.
			// (The message composer might have been disabled)
			Email.log(anfe.toString());
		}
	}

	/**
	 * Compose a new message using a uri (mailto:) and a given account. If
	 * account is -1 the default account will be used.
	 * 
	 * @param context
	 * @param uriString
	 * @param accountId
	 * @return true if startActivity() succeeded
	 */
	public static boolean actionCompose(Context context, String uriString,
			long accountId) {
		try {
			Intent i = new Intent(context, MessageCompose.class);
			i.setAction(Intent.ACTION_SEND);
			i.setData(Uri.parse(uriString));
			i.putExtra(EXTRA_ACCOUNT_ID, accountId);
			context.startActivity(i);
			return true;
		} catch (ActivityNotFoundException anfe) {
			// Swallow it - this is usually a race condition, especially under
			// automated test.
			// (The message composer might have been disabled)
			Email.log(anfe.toString());
			return false;
		}
	}

	/**
	 * Compose a new message as a reply to the given message. If replyAll is
	 * true the function is reply all instead of simply reply.
	 * 
	 * @param context
	 * @param messageId
	 * @param replyAll
	 */
	public static void actionReply(Context context, long messageId,
			boolean replyAll) {
		startActivityWithMessage(context, replyAll ? ACTION_REPLY_ALL : ACTION_REPLY, messageId);
	}

	/**
	 * Compose a new message as a forward of the given message.
	 * 
	 * @param context
	 * @param messageId
	 */
	public static void actionForward(Context context, long messageId) {
		startActivityWithMessage(context, ACTION_FORWARD, messageId);
	}

	/**
	 * Continue composition of the given message. This action modifies the way
	 * this Activity handles certain actions. Save will attempt to replace the
	 * message in the given folder with the updated version. Discard will delete
	 * the message from the given folder.
	 * 
	 * @param context
	 * @param messageId
	 *            the message id.
	 */
	public static void actionEditDraft(Context context, long messageId) {
		startActivityWithMessage(context, ACTION_EDIT_DRAFT, messageId);
	}

	private static void startActivityWithMessage(Context context,
			String action, long messageId) {
		Intent i = new Intent(context, MessageCompose.class);
		i.putExtra(EXTRA_MESSAGE_ID, messageId);
		i.setAction(action);
		context.startActivity(i);
	}

	private void setAccount(Intent intent) {
		long accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1);
		if (accountId == -1) {
			accountId = Account.getDefaultAccountId(this);
		}
		if (accountId == -1) {
			// There are no accounts set up. This should not have happened.
			// Prompt the
			// user to set up an account as an acceptable bailout.
			AccountFolderList.actionShowAccounts(this);
			finish();
		} else {
			setAccount(Account.restoreAccountWithId(this, accountId));
		}
	}

	private void setAccount(Account account) {
		mAccount = account;
		if (account != null) {
			mRightTitle.setText(account.mDisplayName);
			mAddressAdapterTo.setAccount(account);
			mAddressAdapterCc.setAccount(account);
			mAddressAdapterBcc.setAccount(account);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.message_compose);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.list_title);

		mController = Controller.getInstance(getApplication());
		mListener = new Listener();

		initViews();
		setDraftNeedsSaving(false);

		long draftId = -1;
		if (savedInstanceState != null) {
			// This data gets used in onCreate, so grab it here instead of
			// onRestoreInstanceState
			mSourceMessageProcessed = savedInstanceState.getBoolean(STATE_KEY_SOURCE_MESSAGE_PROCED, false);
			draftId = savedInstanceState.getLong(STATE_KEY_DRAFT_ID, -1);
		}

		Intent intent = getIntent();
		mAction = intent.getAction();

		if (draftId != -1) {
			// this means that we saved the draft earlier,
			// so now we need to disregard the intent action and do
			// EDIT_DRAFT instead.
			mAction = ACTION_EDIT_DRAFT;
			mDraft.mId = draftId;
		}

		// Handle the various intents that launch the message composer
		if (Intent.ACTION_VIEW.equals(mAction)
				|| Intent.ACTION_SENDTO.equals(mAction)
				|| Intent.ACTION_SEND.equals(mAction)
				|| Intent.ACTION_SEND_MULTIPLE.equals(mAction)) {
			setAccount(intent);
			// Use the fields found in the Intent to prefill as much of the
			// message as possible
			initFromIntent(intent);
			setDraftNeedsSaving(true);
			mMessageLoaded = true;
			mSourceMessageProcessed = true;
		} else {
			// Otherwise, handle the internal cases (Message Composer invoked
			// from within app)
			long messageId = draftId != -1 ? draftId : intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
			if (messageId != -1) {
				mLoadMessageTask = new LoadMessageTask().execute(messageId);
			} else {
				setAccount(intent);
				// Since this is a new message, we don't need to call
				// LoadMessageTask.
				// But we DO need to set mMessageLoaded to indicate the message
				// can be sent
				mMessageLoaded = true;
				mSourceMessageProcessed = true;
			}
			setInitialComposeText(null,(mAccount != null) ? mAccount.mSignature : null);
		}

		if (ACTION_REPLY.equals(mAction) || ACTION_REPLY_ALL.equals(mAction)
				|| ACTION_FORWARD.equals(mAction)
				|| ACTION_EDIT_DRAFT.equals(mAction)) {
			/*
			 * If we need to load the message we add ourself as a message
			 * listener here so we can kick it off. Normally we add in onResume
			 * but we don't want to reload the message every time the activity
			 * is resumed. There is no harm in adding twice.
			 */
			// TODO: signal the controller to load the message
		}
		addRecordView();
		initRecord();

		updateTitle();
		initMenuResourse();
		initPopupMenu();
		skinName = Utiles.fetchSkinName(this);
		
		
	}

	// needed for unit tests
	@Override
	public void setIntent(Intent intent) {
		super.setIntent(intent);
		mAction = intent.getAction();
	}

	@Override
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
		mController.addResultCallback(mListener);

		// Exit immediately if the accounts list has changed (e.g. externally
		// deleted)
		if (Email.getNotifyUiAccountsChanged()) {
			Welcome.actionStart(this);
			finish();
			return;
		}
		
	}

	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
		// saveIfNeeded();
		mController.removeResultCallback(mListener);
	}

	/**
	 * We override onDestroy to make sure that the WebView gets explicitly
	 * destroyed. Otherwise it can leak native references.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		mQuotedText.destroy();
		mQuotedText = null;

		Utility.cancelTaskInterrupt(mLoadAttachmentsTask);
		mLoadAttachmentsTask = null;
		Utility.cancelTaskInterrupt(mLoadMessageTask);
		mLoadMessageTask = null;
		// don't cancel mSaveMessageTask, let it do its job to the end.
		mSaveMessageTask = null;

		if (mAddressAdapterTo != null) {
			mAddressAdapterTo.changeCursor(null);
		}
		if (mAddressAdapterCc != null) {
			mAddressAdapterCc.changeCursor(null);
		}
		if (mAddressAdapterBcc != null) {
			mAddressAdapterBcc.changeCursor(null);
		}
		Utility.cancelTaskInterrupt(mAddEmailContactsTask);
		mAddEmailContactsTask = null;
		vibrator.cancel();
	}

	public void addRecordView() {
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mView = mInflater.inflate(R.layout.voice_rcd_hint_window,mComposeLayout, false);

		addContentView(mView, new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));

		mLinearLayoutrcding = (LinearLayout) this.findViewById(R.id.voice_rcd_hint_rcding);
		mLinearLayoutloading = (LinearLayout) this.findViewById(R.id.voice_rcd_hint_loading);
		mLinearLayouttooshort = (LinearLayout) this.findViewById(R.id.voice_rcd_hint_tooshort);
	}

	public void initRecord() {
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		mRecorder = new Recorder();
		mRecorder.setOnStateChangedListener(this);

		// 是否存在SD卡
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			mSoundFile = new File(Environment.getExternalStorageDirectory()
					.getPath()
					+ Global.tempFile);
			if (!mSoundFile.exists()) {
				mSoundFile.mkdir();
			}
		} else {
			File file = getFilesDir();
			mSoundFile = new File(file.getPath() + Global.tempFile);
			if (!mSoundFile.exists()) {
				mSoundFile.mkdir();
			}
			// Toast.makeText(this, "please insert sdcard", Toast.LENGTH_LONG)
			// .show();
			return;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			mSoundHandler.mRecordNoSdcard();
			return super.onTouchEvent(event);
		}
		int touchY = (int) event.getY();
		int mPopBottom = findViewById(R.id.pop_layout).getTop() + 4;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (touchY > mPopBottom) {
				vibrator.vibrate(vibrate, -1);
				mLinearLayoutloading.setVisibility(View.VISIBLE); // 加载动画
				mRecorder.startRecording(mSoundFile,
						MediaRecorder.OutputFormat.DEFAULT, ".amr", this);
			}
			break;
		case MotionEvent.ACTION_UP:
			if (touchY > mPopBottom) {
				vibrator.vibrate(vibrate, -1);
				mRecorder.stop();
			}

			break;
		}
		return super.onTouchEvent(event);
	}

	/**
	 * The framework handles most of the fields, but we need to handle stuff
	 * that we dynamically show and hide: Cc field, Bcc field, Quoted text,
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		long draftId = getOrCreateDraftId();
		if (draftId != -1) {
			outState.putLong(STATE_KEY_DRAFT_ID, draftId);
		}
		outState.putBoolean(STATE_KEY_CC_SHOWN,
				mCcView.getVisibility() == View.VISIBLE);
		outState.putBoolean(STATE_KEY_BCC_SHOWN,
				mBccView.getVisibility() == View.VISIBLE);
		outState.putBoolean(STATE_KEY_QUOTED_TEXT_SHOWN, mQuotedTextBar
				.getVisibility() == View.VISIBLE);
		outState.putBoolean(STATE_KEY_SOURCE_MESSAGE_PROCED,
				mSourceMessageProcessed);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mCcView
				.setVisibility(savedInstanceState
						.getBoolean(STATE_KEY_CC_SHOWN) ? View.VISIBLE
						: View.GONE);
		mBccView.setVisibility(savedInstanceState
				.getBoolean(STATE_KEY_BCC_SHOWN) ? View.VISIBLE : View.GONE);
		mQuotedTextBar.setVisibility(savedInstanceState
				.getBoolean(STATE_KEY_QUOTED_TEXT_SHOWN) ? View.VISIBLE
				: View.GONE);
		mQuotedText.setVisibility(savedInstanceState
				.getBoolean(STATE_KEY_QUOTED_TEXT_SHOWN) ? View.VISIBLE
				: View.GONE);
		setDraftNeedsSaving(false);
	}

	private void setDraftNeedsSaving(boolean needsSaving) {
		mDraftNeedsSaving = needsSaving;
		mSaveButton.setEnabled(needsSaving);
	}

	private void initViews() {
		mComposeLayout = (LinearLayout) findViewById(R.id.composeLinearLayout);
		mToView = (AddressTextView) findViewById(R.id.to);
		mCcView = (AddressTextView) findViewById(R.id.cc);
		mBccView = (AddressTextView) findViewById(R.id.bcc);

		mCcLayout = (LinearLayout) findViewById(R.id.ccLayout);
		mBccLayout = (LinearLayout) findViewById(R.id.bccLayout);
		mAddAttachButton = (Button) findViewById(R.id.add_attachments);
		mPrioritySpinner = (Spinner) findViewById(R.id.compose_priority);
		mToImgView = (ImageButton) findViewById(R.id.toImgView);
		mCcImgView = (ImageButton) findViewById(R.id.ccImgView);
		mBccImgView = (ImageButton) findViewById(R.id.bccImgView);

		mSubjectView = (EditText) findViewById(R.id.subject);
		mMessageContentView = (EditText) findViewById(R.id.message_content);
		mSendButton = (Button) findViewById(R.id.send);
		mDiscardButton = (Button) findViewById(R.id.discard);
		mSaveButton = (Button) findViewById(R.id.save);

		mRecordAttachment = (ImageView) findViewById(R.id.record_attachment);
		mAttachments = (LinearLayout) findViewById(R.id.attachments);
		mQuotedTextBar = findViewById(R.id.quoted_text_bar);
		mQuotedTextDelete = (ImageButton) findViewById(R.id.quoted_text_delete);
		mQuotedText = (WebView) findViewById(R.id.quoted_text);
		mLeftTitle = (TextView) findViewById(R.id.title_left_text);
		mRightTitle = (TextView) findViewById(R.id.title_right_text);

		TextWatcher watcher = new TextWatcher() {
			public void beforeTextChanged(CharSequence s, int start,
					int before, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				setDraftNeedsSaving(true);
			}

			public void afterTextChanged(android.text.Editable s) {
			}
		};

		/**
		 * Implements special address cleanup rules: The first space key entry
		 * following an "@" symbol that is followed by any combination of
		 * letters and symbols, including one+ dots and zero commas, should
		 * insert an extra comma (followed by the space).
		 */
		InputFilter recipientFilter = new InputFilter() {

			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {

				// quick check - did they enter a single space?
				if (end - start != 1 || source.charAt(start) != ' ') {
					return null;
				}

				// determine if the characters before the new space fit the
				// pattern
				// follow backwards and see if we find a comma, dot, or @
				int scanBack = dstart;
				boolean dotFound = false;
				while (scanBack > 0) {
					char c = dest.charAt(--scanBack);
					switch (c) {
					case '.':
						dotFound = true; // one or more dots are req'd
						break;
					case ',':
						return null;
					case '@':
						if (!dotFound) {
							return null;
						}

						// we have found a comma-insert case. now just do it
						// in the least expensive way we can.
						if (source instanceof Spanned) {
							SpannableStringBuilder sb = new SpannableStringBuilder(
									",");
							sb.append(source);
							return sb;
						} else {
							return ", ";
						}
					default:
						// just keep going
					}
				}

				// no termination cases were found, so don't edit the input
				return null;
			}
		};
		InputFilter[] recipientFilters = new InputFilter[] { recipientFilter };

		mToView.addTextChangedListener(watcher);
		mCcView.addTextChangedListener(watcher);
		mBccView.addTextChangedListener(watcher);
		mSubjectView.addTextChangedListener(watcher);
		mMessageContentView.addTextChangedListener(watcher);

		mAddAttachButton.setOnClickListener(this);
		mToImgView.setOnClickListener(this);
		mCcImgView.setOnClickListener(this);
		mBccImgView.setOnClickListener(this);
		// NOTE: assumes no other filters are set
		mToView.setFilters(recipientFilters);
		mCcView.setFilters(recipientFilters);
		mBccView.setFilters(recipientFilters);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.priority_mail_entries,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPrioritySpinner.setAdapter(adapter);
		mPrioritySpinner.setOnItemSelectedListener(priority_SpinnerListener);

		/*
		 * We set this to invisible by default. Other methods will turn it back
		 * on if it's needed.
		 */
		mQuotedTextBar.setVisibility(View.GONE);
		mQuotedText.setVisibility(View.GONE);

		mQuotedText.setClickable(true);
		mQuotedText.setLongClickable(false); // Conflicts with ScrollView,
		// unfortunately
		mQuotedTextDelete.setOnClickListener(this);

		EmailAddressValidator addressValidator = new EmailAddressValidator();

		setupAddressAdapters();
		mToView.setAdapter(mAddressAdapterTo);
		mToView.setTokenizer(new Rfc822Tokenizer());
		mToView.setValidator(addressValidator);

		mCcView.setAdapter(mAddressAdapterCc);
		mCcView.setTokenizer(new Rfc822Tokenizer());
		mCcView.setValidator(addressValidator);

		mBccView.setAdapter(mAddressAdapterBcc);
		mBccView.setTokenizer(new Rfc822Tokenizer());
		mBccView.setValidator(addressValidator);

		mSendButton.setOnClickListener(this);
		mDiscardButton.setOnClickListener(this);
		mSaveButton.setOnClickListener(this);

		mRecordAttachment.setOnTouchListener(this);
		mSubjectView.setOnFocusChangeListener(this);
		mMessageContentView.setOnFocusChangeListener(this);
	}

	private int priority = 0;
	private Spinner.OnItemSelectedListener priority_SpinnerListener = new Spinner.OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			priority = arg2;
		}

		public void onNothingSelected(AdapterView<?> arg0) {

		}
	};

	/**
	 * Set up address auto-completion adapters.
	 */
	@SuppressWarnings("all")
	private void setupAddressAdapters() {
		/* EXCHANGE-REMOVE-SECTION-END */
		mAddressAdapterTo = new EmailAddressAdapter(this);
		mAddressAdapterCc = new EmailAddressAdapter(this);
		mAddressAdapterBcc = new EmailAddressAdapter(this);
		/* EXCHANGE-REMOVE-SECTION-START */
	}

	// TODO: is there any way to unify this with MessageView.LoadMessageTask?
	private class LoadMessageTask extends AsyncTask<Long, Void, Object[]> {
		@Override
		protected Object[] doInBackground(Long... messageIds) {
			synchronized (sSaveInProgressCondition) {
				while (sSaveInProgress) {
					try {
						sSaveInProgressCondition.wait();
					} catch (InterruptedException e) {
						// ignore & retry loop
					}
				}
			}
			Message message = Message.restoreMessageWithId(MessageCompose.this,messageIds[0]);
	
			if (message == null) {
				return new Object[] { null, null };
			}
			long accountId = message.mAccountKey;
			Account account = Account.restoreAccountWithId(MessageCompose.this,accountId);

			try {
//				Body body = Body.restoreBodyWithMessageId(MessageCompose.this,message.mId);
				message.mHtml = Body.restoreBodyHtmlWithMessageId(MessageCompose.this, message.mId);
				message.mText = Body.restoreBodyTextWithMessageId(MessageCompose.this, message.mId);
				boolean isEditDraft = ACTION_EDIT_DRAFT.equals(mAction);
				// the reply fields are only filled/used for Drafts.
				if (isEditDraft) {
					message.mHtmlReply = Body.restoreReplyHtmlWithMessageId(MessageCompose.this, message.mId);
					message.mTextReply = Body.restoreReplyTextWithMessageId(MessageCompose.this, message.mId);
					message.mIntroText = Body.restoreIntroTextWithMessageId(MessageCompose.this, message.mId);
					message.mSourceKey = Body.restoreBodySourceKey(MessageCompose.this, message.mId);
				} else {
					message.mHtmlReply = null;
					message.mTextReply = null;
					message.mIntroText = null;
				}
			} catch (RuntimeException e) {
				Log.d(Email.LOG_TAG, "Exception while loading message body: "+ e);
				return new Object[] { null, null };
			}
			return new Object[] { message, account };
		}

		@Override
		protected void onPostExecute(Object[] messageAndAccount) {
			if (messageAndAccount == null) {
				return;
			}

			final Message message = (Message) messageAndAccount[0];
			final Account account = (Account) messageAndAccount[1];
			if (message == null && account == null) {
				// Something unexpected happened:
				// the message or the body couldn't be loaded by SQLite.
				// Bail out.
				Toast.makeText(MessageCompose.this,R.string.error_loading_message_body, Toast.LENGTH_LONG).show();
				finish();
				return;
			}

			if (ACTION_EDIT_DRAFT.equals(mAction)) {
				mDraft = message;
				mLoadAttachmentsTask = new AsyncTask<Long, Void, Attachment[]>() {
					@Override
					protected Attachment[] doInBackground(Long... messageIds) {
						return Attachment.restoreAttachmentsWithMessageId(
								MessageCompose.this, messageIds[0]);
					}

					@Override
					protected void onPostExecute(Attachment[] attachments) {
						if (attachments == null) {
							return;
						}
						for (Attachment attachment : attachments) {
							addAttachment(attachment);
						}
					}
				}.execute(message.mId);
			} else if (ACTION_REPLY.equals(mAction)
					|| ACTION_REPLY_ALL.equals(mAction)
					|| ACTION_FORWARD.equals(mAction)) {
				mSource = message;
				//=============2012.3.4===2013.7.3========
				
//				if(mSource.mHtml!= null){     
//					Global.ISFORWORD = true;
//					Logs.v(Logs.LOG_MessageView, "message.mIntroText :"+message.mIntroText);
//					
//					Logs.v(Logs.LOG_MessageView, "message.mIntroText :"+message.mIntroText);
//					if(message.mText == null && message.mIntroText != null){
//						 message.mHtml = "<br><div>"+message.mIntroText+"</div><br>"+mSource.mHtml;   
//					}else if(message.mText != null && message.mIntroText == null){
//						 message.mHtml = "<br><div>"+message.mText+"</div><br>"+mSource.mHtml;    
//					}else if(message.mText == null && message.mIntroText == null){
//						 message.mHtml = "<br>"+mSource.mHtml;    
//					}else{
//						message.mHtml = "<br><div>"+message.mText+"</div><br><div>"+message.mIntroText+"</div><br>"+mSource.mHtml;    
//					}
//	
//                    message.mIntroText= message.mHtml;
//                    message.mText = message.mHtml;
//				}
				//=============2012.3.4==2013.7.3=========
			} else if (Email.LOGD) {
				Email.log("Action " + mAction + " has unexpected EXTRA_MESSAGE_ID");
			}

			setAccount(account);
			processSourceMessageGuarded(message, mAccount);
			mMessageLoaded = true;
		}
	}

	private void updateTitle() {
		if (mSubjectView.getText().length() == 0) {
			mLeftTitle.setText(R.string.compose_title);
		} else {
			mLeftTitle.setText(mSubjectView.getText().toString());
		}
	}

	public void onFocusChange(View view, boolean focused) {
		if (!focused) {
			updateTitle();
		} else {
			switch (view.getId()) {
			case R.id.message_content:
				setMessageContentSelection((mAccount != null) ? mAccount.mSignature
						: null);
			}
		}
	}

	private void addAddresses(MultiAutoCompleteTextView view,
			Address[] addresses) {
		if (addresses == null) {
			return;
		}
		for (Address address : addresses) {
			addAddress(view, address.toString());
		}
	}

	private void addAddresses(MultiAutoCompleteTextView view, String[] addresses) {
		if (addresses == null) {
			return;
		}
		for (String oneAddress : addresses) {
			addAddress(view, oneAddress);
		}
	}

	private void addAddress(MultiAutoCompleteTextView view, String address) {
		view.append(address + ", ");
	}

	private String getPackedAddresses(TextView view) {
		Address[] addresses = Address.parse(view.getText().toString().trim());
		// SharedPreferences.Editor editor = mSharedPreference.edit();
		// for (Address address : addresses) {
		// editor.putString(address.toString(), address.toString());
		// }
		// editor.commit();
		return Address.pack(addresses);
	}

	private Address[] getAddresses(TextView view) {
		Address[] addresses = Address.parse(view.getText().toString().trim());
		return addresses;
	}

	/*
	 * Computes a short string indicating the destination of the message based
	 * on To, Cc, Bcc. If only one address appears, returns the friendly form of
	 * that address. Otherwise returns the friendly form of the first address
	 * appended with "and N others".
	 */
	private String makeDisplayName(String packedTo, String packedCc,
			String packedBcc) {
		Address first = null;
		int nRecipients = 0;
		for (String packed : new String[] { packedTo, packedCc, packedBcc }) {
			Address[] addresses = Address.unpack(packed);
			nRecipients += addresses.length;
			if (first == null && addresses.length > 0) {
				first = addresses[0];
			}
		}
		if (nRecipients == 0) {
			return "";
		}
		String friendly = first.toFriendly();
		if (nRecipients == 1) {
			return friendly;
		}
		return this.getString(R.string.message_compose_display_name, friendly,
				nRecipients - 1);
	}

	private ContentValues getUpdateContentValues(Message message) {
		ContentValues values = new ContentValues();
		values.put(MessageColumns.TIMESTAMP, message.mTimeStamp);
		values.put(MessageColumns.FROM_LIST, message.mFrom);
		values.put(MessageColumns.TO_LIST, message.mTo);
		values.put(MessageColumns.CC_LIST, message.mCc);
		values.put(MessageColumns.BCC_LIST, message.mBcc);
		values.put(MessageColumns.SUBJECT, message.mSubject);
		values.put(MessageColumns.DISPLAY_NAME, message.mDisplayName);
		values.put(MessageColumns.FLAG_READ, message.mFlagRead);
		values.put(MessageColumns.FLAG_LOADED, message.mFlagLoaded);
		values.put(MessageColumns.FLAG_ATTACHMENT, message.mFlagAttachment);
		values.put(MessageColumns.FLAGS, message.mFlags);
		values.put(MessageColumns.PRIORITY, message.mPriority);
		return values;
	}

	/**
	 * @param message
	 *            The message to be updated.
	 * @param account
	 *            the account (used to obtain From: address).
	 * @param bodyText
	 *            the body text.
	 */
	private void updateMessage(Message message, Account account,
			boolean hasAttachments) {
		if (message.mMessageId == null || message.mMessageId.length() == 0) {
			message.mMessageId = Utility.generateMessageId();
		}
		message.mTimeStamp = System.currentTimeMillis();
		message.mFrom = new Address(account.getEmailAddress(), account.getSenderName()).pack();
		message.mTo = getPackedAddresses(mToView);
		message.mCc = getPackedAddresses(mCcView);
		message.mBcc = getPackedAddresses(mBccView);
		message.mSubject = mSubjectView.getText().toString();
		message.mPriority = String.valueOf(priority);
		message.mText = mMessageContentView.getText().toString();
		message.mAccountKey = account.mId;
		message.mDisplayName = makeDisplayName(message.mTo, message.mCc,message.mBcc);
		message.mFlagRead = true;
		message.mFlagLoaded = Message.FLAG_LOADED_COMPLETE;
		message.mFlagAttachment = hasAttachments;
		// Use the Intent to set flags saying this message is a reply or a
		// forward and save the
		// unique id of the source message
		if (mSource != null && mQuotedTextBar.getVisibility() == View.VISIBLE) {
			if (ACTION_REPLY.equals(mAction)
					|| ACTION_REPLY_ALL.equals(mAction)
					|| ACTION_FORWARD.equals(mAction)) {
				message.mSourceKey = mSource.mId;
				// Get the body of the source message here
				message.mHtmlReply = mSource.mHtml;
				message.mTextReply = mSource.mText;
			}

			String fromAsString = Address.unpackToString(mSource.mFrom);
			if (ACTION_FORWARD.equals(mAction)) {
				message.mFlags |= Message.FLAG_TYPE_FORWARD;
				String subject = mSource.mSubject;
				String to = Address.unpackToString(mSource.mTo);
				String cc = Address.unpackToString(mSource.mCc);
				message.mIntroText = getString(
						R.string.message_compose_fwd_header_fmt, subject,
						fromAsString, to != null ? to : "", cc != null ? cc
								: "");
			} else {
				message.mFlags |= Message.FLAG_TYPE_REPLY;
				message.mIntroText = getString(
						R.string.message_compose_reply_header_fmt, fromAsString);
			}

		}
	}

	private Attachment[] getAttachmentsFromUI() {
		int count = mAttachments.getChildCount();
		Attachment[] attachments = new Attachment[count];
		for (int i = 0; i < count; ++i) {
			attachments[i] = (Attachment) mAttachments.getChildAt(i).getTag();
		}
		return attachments;
	}

	/*
	 * This method does DB operations in UI thread because the draftId is needed
	 * by onSaveInstanceState() which can't wait for it to be saved in the
	 * background. TODO: This will cause ANRs, so we need to find a better
	 * solution.
	 */
	private long getOrCreateDraftId() {
		synchronized (mDraft) {
			if (mDraft.mId > 0) {
				return mDraft.mId;
			}
			// don't save draft if the source message did not load yet
			if (!mMessageLoaded) {
				return -1;
			}
			final Attachment[] attachments = getAttachmentsFromUI();
			updateMessage(mDraft, mAccount, attachments.length > 0);
			mController.saveToMailbox(mDraft, EmailContent.Mailbox.TYPE_DRAFTS);
			return mDraft.mId;
		}
	}

	/**
	 * Send or save a message: - out of the UI thread - write to Drafts - if
	 * send, invoke Controller.sendMessage() - when operation is complete,
	 * display toast
	 */
	private void sendOrSaveMessage(final boolean send) {
		final Attachment[] attachments = getAttachmentsFromUI();
		if (!mMessageLoaded) {
			// early save, before the message was loaded: do nothing
			return;
		}
		updateMessage(mDraft, mAccount, attachments.length > 0);

		synchronized (sSaveInProgressCondition) {
			sSaveInProgress = true;
		}

		mSaveMessageTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				synchronized (mDraft) {
					if (mDraft.isSaved()) {
						// Update the message
						Uri draftUri = ContentUris.withAppendedId(mDraft.SYNCED_CONTENT_URI, mDraft.mId);

						getContentResolver().update(draftUri,getUpdateContentValues(mDraft), null, null);
						// Update the body
						ContentValues values = new ContentValues();
						values.put(BodyColumns.TEXT_CONTENT, mDraft.mText);
						values.put(BodyColumns.TEXT_REPLY, mDraft.mTextReply);
						values.put(BodyColumns.HTML_REPLY, mDraft.mHtmlReply);
						values.put(BodyColumns.INTRO_TEXT, mDraft.mIntroText);
						values.put(BodyColumns.SOURCE_MESSAGE_KEY,mDraft.mSourceKey);
						Body.updateBodyWithMessageId(MessageCompose.this,mDraft.mId, values);
					} else {
						// mDraft.mId is set upon return of saveToMailbox()
						mController.saveToMailbox(mDraft,EmailContent.Mailbox.TYPE_DRAFTS);
					}
					for (Attachment attachment : attachments) {
						if (!attachment.isSaved()) {
							// this attachment is new so save it to DB.
							attachment.mMessageKey = mDraft.mId;
							attachment.save(MessageCompose.this);
						}
					}

					if (send) {
						mController.sendMessage(mDraft.mId, mDraft.mAccountKey);
					}
					return null;
				}
			}

			@Override
			protected void onPostExecute(Void dummy) {
				synchronized (sSaveInProgressCondition) {
					sSaveInProgress = false;
					sSaveInProgressCondition.notify();
				}
				if (isCancelled()) {
					return;
				}
				// Don't display the toast if the user is just changing the
				// orientation
				if (!send && (getChangingConfigurations() & ActivityInfo.CONFIG_ORIENTATION) == 0) {
					Toast.makeText(MessageCompose.this,R.string.message_saved_toast, Toast.LENGTH_LONG).show();
				}
			}
		}.execute();
	}

	private void saveIfNeeded() {
		if (!mDraftNeedsSaving) {
			return;
		}
		setDraftNeedsSaving(false);
		sendOrSaveMessage(false);
	}

	/**
	 * Checks whether all the email addresses listed in TO, CC, BCC are valid.
	 */
	/* package */boolean isAddressAllValid() {
		for (TextView view : new TextView[] { mToView, mCcView, mBccView }) {
			String addresses = view.getText().toString().trim();
			if (!Address.isAllValid(addresses)) {
				view
						.setError(getString(R.string.message_compose_error_invalid_email));
				return false;
			}
		}
		return true;
	}

	private void onSend() {
	
		if (!isAddressAllValid()) {
			Toast.makeText(this,
					getString(R.string.message_compose_error_invalid_email),
					Toast.LENGTH_LONG).show();
		} else if (getAddresses(mToView).length == 0
				&& getAddresses(mCcView).length == 0
				&& getAddresses(mBccView).length == 0) {
			mToView.setError(getString(R.string.message_compose_error_no_recipients));
			Toast.makeText(this,
					getString(R.string.message_compose_error_no_recipients),
					Toast.LENGTH_LONG).show();
		} else {
			addEmailAdressToContacts();
			sendOrSaveMessage(true);
			setDraftNeedsSaving(false);
			mHandler.sendEmptyMessage(MSG_SEND_SUCCESS);
			if (Global.contacts_add_notify) {
				finish();
			}
		}
	}

	private void addEmailAdressToContacts() {
		String toAddress = mToView.getText().toString().trim();
//============2013.7.4==发送邮件时默认自动添加联系人========
//		if (!Global.contacts_add_notify) {
//			ContactsDialog.actionContactsDialog(MessageCompose.this,REQUEST_CODE_ADD_ADRESS_CONTACTS);
//		} else if (Global.contacts_add_flag) {
//			mAddEmailContactsTask = new AddEmailContactsTask();
//			mAddEmailContactsTask.execute(toAddress);
//		}
//===========2013.7.4==========
		mAddEmailContactsTask = new AddEmailContactsTask();
        mAddEmailContactsTask.execute(toAddress);
	}

	private class AddEmailContactsTask extends AsyncTask<String, Void, Integer> {
		@Override
		protected Integer doInBackground(String... params) {
			String toAddress = params[0];
			ContactsUtils.addEmailContacts(MessageCompose.this, toAddress);
			return 1;
		}

		@Override
		protected void onPostExecute(Integer result) {

		}
	}

	private void onDiscard() {
		if (mDraft.mId > 0) {
			mController.deleteMessage(mDraft.mId, mDraft.mAccountKey);
		}
		Toast.makeText(this, getString(R.string.message_discarded_toast),
				Toast.LENGTH_LONG).show();
		setDraftNeedsSaving(false);
		finish();
	}

	private void onSave() {
		saveIfNeeded();
		finish();
	}

	private void onAddCcBcc() {
		mCcLayout.setVisibility(View.VISIBLE);
		mBccLayout.setVisibility(View.VISIBLE);
	}

	/**
	 * Kick off a picker for whatever kind of MIME types we'll accept and let
	 * Android take over.
	 */
	private void onAddAttachment() {
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.addCategory(Intent.CATEGORY_OPENABLE);
		i.setType(Email.ACCEPTABLE_ATTACHMENT_SEND_UI_TYPES[0]);
		i.putExtra("add", true);
		startActivityForResult(Intent.createChooser(i,
				getString(R.string.choose_attachment_dialog_title)),
				REQUEST_CODE_ADD_ATTACHMENT);

	}

	private Attachment loadAttachmentInfo(Uri uri) {

		long size = -1;
		String name = null;
		ContentResolver contentResolver = getContentResolver();

		// Load name & size independently, because not all providers support
		// both
		Cursor metadataCursor = contentResolver.query(uri,
				ATTACHMENT_META_NAME_PROJECTION, null, null, null);
		if (metadataCursor != null) {
			try {
				if (metadataCursor.moveToFirst()) {
					name = metadataCursor
							.getString(ATTACHMENT_META_NAME_COLUMN_DISPLAY_NAME);
				}
			} finally {
				metadataCursor.close();
			}
		}
		metadataCursor = contentResolver.query(uri,
				ATTACHMENT_META_SIZE_PROJECTION, null, null, null);
		if (metadataCursor != null) {
			try {
				if (metadataCursor.moveToFirst()) {
					size = metadataCursor
							.getLong(ATTACHMENT_META_SIZE_COLUMN_SIZE);
				}
			} finally {
				metadataCursor.close();
			}
		}

		// When the name or size are not provided, we need to generate them
		// locally.
		if (name == null) {
			name = uri.getLastPathSegment();
		}
		if (size < 0) {
			// if the URI is a file: URI, ask file system for its size
			if ("file".equalsIgnoreCase(uri.getScheme())) {
				String path = uri.getPath();
				if (path != null) {
					File file = new File(path);
					size = file.length(); // Returns 0 for file not found

				}
			}

			if (size <= 0) {
				// The size was not measurable; This attachment is not safe to
				// use.
				// Quick hack to force a relevant error into the UI
				// TODO: A proper announcement of the problem
				size = Email.MAX_ATTACHMENT_UPLOAD_SIZE + 1;
			}
		}
		String contentType = contentResolver.getType(uri);
		if (contentType == null) {
			// contentType = "";
			contentType = "application/octet-stream";
		}

		Attachment attachment = new Attachment();
		attachment.mFileName = name;
		attachment.mContentUri = uri.toString();
		attachment.mSize = size;
		attachment.mMimeType = contentType;
		return attachment;
	}

	private void addAttachment(Attachment attachment) {
		// Before attaching the attachment, make sure it meets any other
		// pre-attach criteria
		if (attachment.mSize > Email.MAX_ATTACHMENT_UPLOAD_SIZE) {
			Toast.makeText(this, R.string.message_compose_attachment_size,
					Toast.LENGTH_LONG).show();
			return;
		}

		View view = getLayoutInflater().inflate(
				R.layout.message_compose_attachment, mAttachments, false);
		TextView nameView = (TextView) view.findViewById(R.id.attachment_name);
		ImageButton delete = (ImageButton) view
				.findViewById(R.id.attachment_delete);

		Button mOpenAttachment = (Button) view
				.findViewById(R.id.open_attachment);

		Logs.d(Logs.LOG_TAG, "attachment.mMimeType :" + attachment.mMimeType);
		if (attachment.mFileName.endsWith(".jpg")
				|| attachment.mFileName.endsWith(".amr")
				|| (MimeUtility.mimeTypeMatches(attachment.mMimeType,
						Email.ACCEPTABLE_ATTACHMENT_IMAGE_VIEW_TYPES))) {
			mOpenAttachment.setVisibility(View.VISIBLE);
		} else {
			mOpenAttachment.setVisibility(View.GONE);
		}

		StringBuffer attachmentNameandSize = new StringBuffer(
				attachment.mFileName);
		attachmentNameandSize.append("(");
		attachmentNameandSize.append(Formatter.formatFileSize(this,
				attachment.mSize));
		attachmentNameandSize.append(")");
		nameView.setText(attachmentNameandSize.toString());// attachment.mFileName);
		delete.setOnClickListener(this);
		delete.setTag(view);
		mOpenAttachment.setOnClickListener(this);
		mOpenAttachment.setTag(view);
		view.setTag(attachment);
		mAttachments.addView(view);
	}

	private void addAttachment(Uri uri) {
		addAttachment(loadAttachmentInfo(uri));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data == null) {
			return;
		}
		if (RESULT_OK == resultCode) {
			Bundle bundle = data.getExtras();
			switch (requestCode) {
			case REQUEST_CODE_ADD_ATTACHMENT:
				setDraftNeedsSaving(true);
				addAttachment(data.getData());
				break;
			case REQUEST_CODE_ADD_CONTACTS:
				String phonebookContact = bundle.getString(EXTRA_ADD_CONTACTS);

				String[] mAdressList = phonebookContact.split(",");
				int length = mAdressList.length;
				for (int i = 0; i < length; i++) {
					setContactsTextView(mContactView, mAdressList[i]);
				}
				break;
			case REQUEST_CODE_ADD_ADRESS_CONTACTS:
				if (Global.contacts_add_flag == true) {
					String toAddress = mToView.getText().toString().trim();
					if (Global.contacts_add_flag) {
						mAddEmailContactsTask = new AddEmailContactsTask();
						mAddEmailContactsTask.execute(toAddress);
					}
				}
				finish();
				break;
			}
		}
	}

	/**
	 * 添加本地联系人成功后，刷新联系人编辑框
	 * */
	private void setContactsTextView(
			MultiAutoCompleteTextView contactsTextView, String emailAddress) {
		String lastEmailAddress = contactsTextView.getText().toString().trim();
		if (lastEmailAddress.contains(emailAddress) && !emailAddress.equals("")) {
			// Toast.makeText(MessageCompose.this,
			// String.format(getString(R.string.mc_toast_add_contact_exist),emailAddress)
			// , Toast.LENGTH_SHORT)
			// .show();
		} else {
			if (lastEmailAddress.equals("") || lastEmailAddress.endsWith(",")) {
				contactsTextView.setText(lastEmailAddress + emailAddress);

			} else {
				contactsTextView.setText(lastEmailAddress + "," + emailAddress);
			}
		}
		// 将焦点定位在邮件地址末尾，便于继续添加
		contactsTextView.setSelection(contactsTextView.getText().toString()
				.length());
		contactsTextView.requestFocus();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.toImgView:
			// PhonebookTabActivity.actionAddContacts(MessageCompose.this,REQUEST_CODE_ADD_CONTACTS);
			ContactManageTab.actionContactManageTabView(MessageCompose.this,REQUEST_CODE_ADD_CONTACTS);
			mContactView = mToView;
			break;
		case R.id.ccImgView:
			ContactManageTab.actionContactManageTabView(MessageCompose.this,REQUEST_CODE_ADD_CONTACTS);
			mContactView = mCcView;
			break;
		case R.id.bccImgView:
			ContactManageTab.actionContactManageTabView(MessageCompose.this,REQUEST_CODE_ADD_CONTACTS);
			mContactView = mBccView;
			break;
		case R.id.send:
			onSend();
			break;
		case R.id.save:
			onSave();
			break;
		case R.id.discard:
			onDiscard();
			break;
		case R.id.add_attachments:
			onAddAttachment();
			break;
		case R.id.attachment_delete:
			onDeleteAttachment(view);
			break;
		case R.id.open_attachment:
			onOpenAttachment(view);
			break;
		case R.id.quoted_text_delete:
			mQuotedTextBar.setVisibility(View.GONE);
			mQuotedText.setVisibility(View.GONE);
			mDraft.mIntroText = null;
			mDraft.mTextReply = null;
			mDraft.mHtmlReply = null;
			mDraft.mSourceKey = 0;
			setDraftNeedsSaving(true);
			break;
		}
	}

	private void onOpenAttachment(View openButtonView) {
		View attachmentView = (View) openButtonView.getTag();
		Attachment attachment = (Attachment) attachmentView.getTag();
		Uri mUri = Uri.parse(attachment.mContentUri);

		try {
			if (attachment.mFileName.endsWith(".amr")) {
				SoundPlayer.actionSoundPlayer(this, mUri);
			} else {
				startActivity(getImageFileIntent(mUri));
			}
		} catch (ActivityNotFoundException e) {
			Toast.makeText(MessageCompose.this,
					getString(R.string.message_view_display_attachment_toast),
					Toast.LENGTH_SHORT).show();
			Logs.e(Logs.LOG_TAG, "ActivityNotFoundException :");
		}
	}

	public static Intent getImageFileIntent(Uri mUri) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(mUri, "image/*");
		return intent;
	}

	private void onDeleteAttachment(View delButtonView) {
		/*
		 * The view is the delete button, and we have previously set the tag of
		 * the delete button to the view that owns it. We don't use parent
		 * because the view is very complex and could change in the future.
		 */
		View attachmentView = (View) delButtonView.getTag();
		Attachment attachment = (Attachment) attachmentView.getTag();
		mAttachments.removeView(attachmentView);

		Uri mUri = Uri.parse(attachment.mContentUri);
		if ("file".equalsIgnoreCase(mUri.getScheme())) {
			String path = mUri.getPath();
			if (path.contains(Global.tempFile)) {
				// Logs.d(Logs.LOG_TAG, "attachment.path :"+path);
				if (path != null) {
					File file = new File(path);
					file.delete();
				}
			}
		}

		if (attachment.isSaved()) {
			// The following async task for deleting attachments:
			// - can be started multiple times in parallel (to delete multiple
			// attachments).
			// - need not be interrupted on activity exit, instead should run to
			// completion.
			new AsyncTask<Long, Void, Void>() {
				@Override
				protected Void doInBackground(Long... attachmentIds) {
					mController.deleteAttachment(attachmentIds[0]);
					return null;
				}
			}.execute(attachment.mId);
		}
		setDraftNeedsSaving(true);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				MessageCompose.this);
		switch (id) {
		case DIALOG_ADD_CONTACTS:
			builder.setTitle(R.string.app_name);
			builder.setMessage(R.string.dialog_delete);
			builder.setMultiChoiceItems(1, new boolean[] { true, false }, null);
			builder.setPositiveButton(R.string.confirm,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							dialog.cancel();

						}
					});
			builder.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.cancel();
						}
					});
			break;

		}
		AlertDialog alert = builder.create();
		return alert;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.send:
			onSend();
			break;
		case R.id.save:
			onSave();
			break;
		case R.id.discard:
			onDiscard();
			break;
		case R.id.add_cc_bcc:
			onAddCcBcc();
			break;
		case R.id.add_attachment:
			onAddAttachment();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!skinName.equals(Global.SKIN_NAME)) {
			menu.add("menu");// 必须创建一项
			return super.onCreateOptionsMenu(menu);
		} else {
			super.onCreateOptionsMenu(menu);
			getMenuInflater().inflate(R.menu.message_compose_option, menu);
			return true;
		}
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (!skinName.equals(Global.SKIN_NAME)) {
			if (popup != null) {
				if (popup.isShowing())
					popup.dismiss();
				else {
					popup.showAtLocation(mComposeLayout, Gravity.BOTTOM, 0, 0);
				}
			}
			return false;// 返回为true 则显示系统menu
		} else {
			return true;
		}
	}

	/**
	 * Returns true if all attachments were able to be attached, otherwise
	 * returns false.
	 */
	// private boolean loadAttachments(Part part, int depth) throws
	// MessagingException {
	// if (part.getBody() instanceof Multipart) {
	// Multipart mp = (Multipart) part.getBody();
	// boolean ret = true;
	// for (int i = 0, count = mp.getCount(); i < count; i++) {
	// if (!loadAttachments(mp.getBodyPart(i), depth + 1)) {
	// ret = false;
	// }
	// }
	// return ret;
	// } else {
	// String contentType = MimeUtility.unfoldAndDecode(part.getContentType());
	// String name = MimeUtility.getHeaderParameter(contentType, "name");
	// if (name != null) {
	// Body body = part.getBody();
	// if (body != null && body instanceof LocalAttachmentBody) {
	// final Uri uri = ((LocalAttachmentBody) body).getContentUri();
	// mHandler.post(new Runnable() {
	// public void run() {
	// addAttachment(uri);
	// }
	// });
	// }
	// else {
	// return false;
	// }
	// }
	// return true;
	// }
	// }
	/**
	 * Set a message body and a signature when the Activity is launched.
	 * 
	 * @param text
	 *            the message body
	 */
	/* package */void setInitialComposeText(CharSequence text, String signature) {
		int textLength = 0;
		if (text != null) {
			mMessageContentView.append(text);
			textLength = text.length();
		}
		if (!TextUtils.isEmpty(signature)) {
			if (textLength == 0 || text.charAt(textLength - 1) != '\n') {
				mMessageContentView.append("\n");
			}
			mMessageContentView.append(signature);
		}
	}

	/**
	 * Fill all the widgets with the content found in the Intent Extra, if any.
	 * 
	 * Note that we don't actually check the intent action (typically VIEW,
	 * SENDTO, or SEND). There is enough overlap in the definitions that it
	 * makes more sense to simply check for all available data and use as much
	 * of it as possible.
	 * 
	 * With one exception: EXTRA_STREAM is defined as only valid for
	 * ACTION_SEND.
	 * 
	 * @param intent
	 *            the launch intent
	 */
	/* package */void initFromIntent(Intent intent) {

		// First, add values stored in top-level extras

		String[] extraStrings = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
		if (extraStrings != null) {
			addAddresses(mToView, extraStrings);
		}
		extraStrings = intent.getStringArrayExtra(Intent.EXTRA_CC);
		if (extraStrings != null) {
			addAddresses(mCcView, extraStrings);
		}
		extraStrings = intent.getStringArrayExtra(Intent.EXTRA_BCC);
		if (extraStrings != null) {
			addAddresses(mBccView, extraStrings);
		}
		String extraString = intent.getStringExtra(Intent.EXTRA_SUBJECT);
		if (extraString != null) {
			mSubjectView.setText(extraString);
		}

		// Next, if we were invoked with a URI, try to interpret it
		// We'll take two courses here. If it's mailto:, there is a specific set
		// of rules
		// that define various optional fields. However, for any other scheme,
		// we'll simply
		// take the entire scheme-specific part and interpret it as a possible
		// list of addresses.

		final Uri dataUri = intent.getData();
		if (dataUri != null) {
			if ("mailto".equals(dataUri.getScheme())) {
				initializeFromMailTo(dataUri.toString());
			} else {
				String toText = dataUri.getSchemeSpecificPart();
				if (toText != null) {
					addAddresses(mToView, toText.split(","));
				}
			}
		}

		// Next, fill in the plaintext (note, this will override mailto:?body=)

		CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
		if (text != null) {
			setInitialComposeText(text, null);
		}

		// Next, convert EXTRA_STREAM into an attachment

		if (Intent.ACTION_SEND.equals(mAction)
				&& intent.hasExtra(Intent.EXTRA_STREAM)) {
			String type = intent.getType();
			Uri stream = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			if (stream != null && type != null) {
				if (MimeUtility.mimeTypeMatches(type,
						Email.ACCEPTABLE_ATTACHMENT_SEND_INTENT_TYPES)) {
					addAttachment(stream);
				}
			}
		}

		if (Intent.ACTION_SEND_MULTIPLE.equals(mAction)
				&& intent.hasExtra(Intent.EXTRA_STREAM)) {
			ArrayList<Parcelable> list = intent
					.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (list != null) {
				for (Parcelable parcelable : list) {
					Uri uri = (Uri) parcelable;
					if (uri != null) {
						Attachment attachment = loadAttachmentInfo(uri);
						if (MimeUtility.mimeTypeMatches(attachment.mMimeType,
								Email.ACCEPTABLE_ATTACHMENT_SEND_INTENT_TYPES)) {
							addAttachment(attachment);
						}
					}
				}
			}
		}

		// Finally - expose fields that were filled in but are normally hidden,
		// and set focus

		if (mCcView.length() > 0) {
			mCcView.setVisibility(View.VISIBLE);
		}
		if (mBccView.length() > 0) {
			mBccView.setVisibility(View.VISIBLE);
		}
		setNewMessageFocus();
		setDraftNeedsSaving(false);
	}

	/**
	 * When we are launched with an intent that includes a mailto: URI, we can
	 * actually gather quite a few of our message fields from it.
	 * 
	 * @mailToString the href (which must start with "mailto:").
	 */
	private void initializeFromMailTo(String mailToString) {

		// Chop up everything between mailto: and ? to find recipients
		int index = mailToString.indexOf("?");
		int length = "mailto".length() + 1;
		String to;
		try {
			// Extract the recipient after mailto:
			if (index == -1) {
				to = decode(mailToString.substring(length));
			} else {
				to = decode(mailToString.substring(length, index));
			}
			addAddresses(mToView, to.split(" ,"));
		} catch (UnsupportedEncodingException e) {
			Log.e(Email.LOG_TAG, e.getMessage() + " while decoding '"
					+ mailToString + "'");
		}

		// Extract the other parameters

		// We need to disguise this string as a URI in order to parse it
		Uri uri = Uri.parse("foo://" + mailToString);

		List<String> cc = uri.getQueryParameters("cc");
		addAddresses(mCcView, cc.toArray(new String[cc.size()]));

		List<String> otherTo = uri.getQueryParameters("to");
		addAddresses(mCcView, otherTo.toArray(new String[otherTo.size()]));

		List<String> bcc = uri.getQueryParameters("bcc");
		addAddresses(mBccView, bcc.toArray(new String[bcc.size()]));

		List<String> subject = uri.getQueryParameters("subject");
		if (subject.size() > 0) {
			mSubjectView.setText(subject.get(0));
		}

		List<String> body = uri.getQueryParameters("body");
		if (body.size() > 0) {
			setInitialComposeText(body.get(0),
					(mAccount != null) ? mAccount.mSignature : null);
		}
	}

	private String decode(String s) throws UnsupportedEncodingException {
		return URLDecoder.decode(s, "UTF-8");
	}

	// used by processSourceMessage()
	private void displayQuotedText(String textBody, String htmlBody) {
		/*
		 * Use plain-text body if available, otherwise use HTML body. This
		 * matches the desired behavior for IMAP/POP where we only send
		 * plain-text, and for EAS which sends HTML and has no plain-text body.
		 */
		boolean plainTextFlag = textBody != null;
		String text = plainTextFlag ? textBody : htmlBody;
		if (text != null) {
			text = plainTextFlag ? EmailHtmlUtil.escapeCharacterToDisplay(text): text;
			// TODO: re-enable EmailHtmlUtil.resolveInlineImage() for HTML
			// EmailHtmlUtil.resolveInlineImage(getContentResolver(), mAccount,
			// text, message, 0);
			mQuotedTextBar.setVisibility(View.VISIBLE);
			if (mQuotedText != null) {
				mQuotedText.setVisibility(View.VISIBLE);
				mQuotedText.loadDataWithBaseURL("email://", text, "text/html","utf-8", null);
			}
		}
	}

	/**
	 * Given a packed address String, the address of our sending account, a
	 * view, and a list of addressees already added to other addressing views,
	 * adds unique addressees that don't match our address to the passed in view
	 */
	private boolean safeAddAddresses(String addrs, String ourAddress,
			MultiAutoCompleteTextView view, ArrayList<Address> addrList) {
		boolean added = false;
		for (Address address : Address.unpack(addrs)) {
			// Don't send to ourselves or already-included addresses
			if (!address.getAddress().equalsIgnoreCase(ourAddress)
					&& !addrList.contains(address)) {
				addrList.add(address);
				addAddress(view, address.toString());
				added = true;
			}
		}
		return added;
	}

	/**
	 * Set up the to and cc views properly for the "reply" and "replyAll" cases.
	 * What's important is that we not 1) send to ourselves, and 2) duplicate
	 * addressees.
	 * 
	 * @param message
	 *            the message we're replying to
	 * @param account
	 *            the account we're sending from
	 * @param toView
	 *            the "To" view
	 * @param ccView
	 *            the "Cc" view
	 * @param replyAll
	 *            whether this is a replyAll (vs a reply)
	 */
	/* package */void setupAddressViews(Message message, Account account,
			MultiAutoCompleteTextView toView, MultiAutoCompleteTextView ccView,
			boolean replyAll) {
		/*
		 * If a reply-to was included with the message use that, otherwise use
		 * the from or sender address.
		 */
		Address[] replyToAddresses = Address.unpack(message.mReplyTo);
		if (replyToAddresses.length == 0) {
			replyToAddresses = Address.unpack(message.mFrom);
		}
		addAddresses(mToView, replyToAddresses);

		if (replyAll) {
			// Keep a running list of addresses we're sending to
			ArrayList<Address> allAddresses = new ArrayList<Address>();
			String ourAddress = account.mEmailAddress;

			for (Address address : replyToAddresses) {
				allAddresses.add(address);
			}

			safeAddAddresses(message.mTo, ourAddress, mToView, allAddresses);
			if (safeAddAddresses(message.mCc, ourAddress, mCcView, allAddresses)) {
				mCcView.setVisibility(View.VISIBLE);
			}
		}
	}

	void processSourceMessageGuarded(Message message, Account account) {
		// Make sure we only do this once (otherwise we'll duplicate addresses!)
		if (!mSourceMessageProcessed) {
			processSourceMessage(message, account);
			mSourceMessageProcessed = true;
		}

		/*
		 * The quoted text is displayed in a WebView whose content is not
		 * automatically saved/restored by onRestoreInstanceState(), so we need
		 * toalways restore it here, regardless of the value of
		 * mSourceMessageProcessed. This only concerns EDIT_DRAFT because after
		 * a configuration change we're always in EDIT_DRAFT.
		 */
		if (ACTION_EDIT_DRAFT.equals(mAction)) {
			displayQuotedText(message.mTextReply, message.mHtmlReply);
		}
	}

	/**
	 * Pull out the parts of the now loaded source message and apply them to the
	 * new message depending on the type of message being composed.
	 * 
	 * @param message
	 */
	/* package */
	void processSourceMessage(Message message, Account account) {
		setDraftNeedsSaving(true);
		final String subject = message.mSubject;
		if (ACTION_REPLY.equals(mAction) || ACTION_REPLY_ALL.equals(mAction)) {
			setupAddressViews(message, account, mToView, mCcView,ACTION_REPLY_ALL.equals(mAction));
			if (subject != null && !subject.toLowerCase().startsWith("re:")) {
				mSubjectView.setText("Re: " + subject);
			} else {
				mSubjectView.setText(subject);
			}
			displayQuotedText(message.mText, message.mHtml);
			setInitialComposeText(null, (account != null) ? account.mSignature: null);
		} else if (ACTION_FORWARD.equals(mAction)) {
			mSubjectView.setText(subject != null
					&& !subject.toLowerCase().startsWith("fwd:") ? "Fwd: "
					+ subject : subject);
			displayQuotedText(message.mText, message.mHtml);
			setInitialComposeText(null, (account != null) ? account.mSignature
					: null);

			// TODO: re-enable loadAttachments below
			if (!loadAttachments(message, 0)) {
				mHandler.sendEmptyMessage(MSG_SKIPPED_ATTACHMENTS);
			}
		} else if (ACTION_EDIT_DRAFT.equals(mAction)) {
			mSubjectView.setText(subject);

			mPrioritySpinner.setSelection(Integer
					.parseInt(message.mPriority == null ? "0"
							: message.mPriority));
			addAddresses(mToView, Address.unpack(message.mTo));
			Address[] cc = Address.unpack(message.mCc);
			if (cc.length > 0) {
				addAddresses(mCcView, cc);
				mCcView.setVisibility(View.VISIBLE);
			}
			Address[] bcc = Address.unpack(message.mBcc);
			if (bcc.length > 0) {
				addAddresses(mBccView, bcc);
				mBccView.setVisibility(View.VISIBLE);
			}

			mMessageContentView.setText(message.mText);
			// TODO: re-enable loadAttachments
			// loadAttachments(message, 0);
			setDraftNeedsSaving(false);
		}
		setNewMessageFocus();
	}

	/**
	 * Returns true if all attachments were able to be attached, otherwise
	 * returns false.
	 */
	private boolean loadAttachments(Message message, int depth) {
		boolean flag = false;
		try {
			Attachment[] attachments = Attachment
					.restoreAttachmentsWithMessageId(this, message.mId);
			for (final Attachment attachment : attachments) {
				if (attachment.mContentUri == null) {
					flag = true;
				} else {
					mHandler.post(new Runnable() {
						public void run() {
							Uri aUri = Uri.parse(attachment.mContentUri);
							addAttachment(aUri);
						}
					});
				}

			}
			if (flag == true) {
				return false;
			} else {
				return true;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}


	/**
	 * Set a cursor to the end of a body except a signature
	 */
	/* package */void setMessageContentSelection(String signature) {
		// when selecting the message content, explicitly move IP to the end of
		// the message,
		// so you can quickly resume typing into a draft
		int selection = mMessageContentView.length();
		if (!TextUtils.isEmpty(signature)) {
			int signatureLength = signature.length();
			int estimatedSelection = selection - signatureLength;
			if (estimatedSelection >= 0) {
				CharSequence text = mMessageContentView.getText();
				int i = 0;
				while (i < signatureLength
						&& text.charAt(estimatedSelection + i) == signature
								.charAt(i)) {
					++i;
				}
				if (i == signatureLength) {
					selection = estimatedSelection;
					while (selection > 0 && text.charAt(selection - 1) == '\n') {
						--selection;
					}
				}
			}
		}
		mMessageContentView.setSelection(selection, selection);
	}

	/**
	 * In order to accelerate typing, position the cursor in the first empty
	 * field, or at the end of the body composition field if none are empty.
	 * Typically, this will play out as follows: Reply / Reply All - put cursor
	 * in the empty message body Forward - put cursor in the empty To field Edit
	 * Draft - put cursor in whatever field still needs entry
	 */
	private void setNewMessageFocus() {
		if (mToView.length() == 0) {
			mToView.requestFocus();
		} else if (mSubjectView.length() == 0) {
			mSubjectView.requestFocus();
		} else {
			mMessageContentView.requestFocus();
			setMessageContentSelection((mAccount != null) ? mAccount.mSignature
					: null);
		}
	}

	private class Listener implements Controller.Result {
		public void updateMailboxListCallback(MessagingException result,
				long accountId, int progress) {
		}

		public void updateMailboxCallback(MessagingException result,
				long accountId, long mailboxId, int progress, int numNewMessages) {
			if (result != null || progress == 100) {
				Email.updateMailboxRefreshTime(mailboxId);
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
						onAddCcBcc();
					}
					if (childOptionIndex == 1) {
						onSend();
					}
					if (childOptionIndex == 2) {
						onSave();
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
				getString(R.string.add_cc_bcc_action),
				getString(R.string.send_action),
				getString(R.string.save_draft_action) };
		mOptionImageArray1 = new int[] { R.drawable.ic_menu_reply,
				R.drawable.ic_menu_signature, R.drawable.ic_menu_save_draft };

	}

	// =================PopMenu=============

	@Override
	public void onError(int error) {

	}

	@Override
	public void onStateChanged(int state) {
		if (state == Recorder.RECORDING_STATE) {
			mSoundHandler.mRecordStart();
		} else if (state == Recorder.RECORDING_STATE_PREPARE) {
			mSoundHandler.mRecordPrepare();
		} else {
			mSoundHandler.mRecordEnd();
		}

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
}
