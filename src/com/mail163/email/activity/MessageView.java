package com.mail163.email.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.QuickContact;
import android.provider.ContactsContract.StatusUpdates;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomButtonsController;

import com.mail163.email.Controller;
import com.mail163.email.Email;
import com.mail163.email.Email.Global;
import com.mail163.email.Logs;
import com.mail163.email.R;
import com.mail163.email.Utility;
import com.mail163.email.mail.Address;
import com.mail163.email.mail.MeetingInfo;
import com.mail163.email.mail.MessagingException;
import com.mail163.email.mail.PackedString;
import com.mail163.email.mail.internet.EmailHtmlUtil;
import com.mail163.email.mail.internet.MimeUtility;
import com.mail163.email.provider.AttachmentProvider;
import com.mail163.email.provider.EmailContent;
import com.mail163.email.provider.EmailContent.Attachment;
import com.mail163.email.provider.EmailContent.Body;
import com.mail163.email.provider.EmailContent.BodyColumns;
import com.mail163.email.provider.EmailContent.Mailbox;
import com.mail163.email.provider.EmailContent.Message;
import com.mail163.email.service.EmailServiceConstants;
import com.mail163.email.util.Utiles;
import com.mobclick.android.MobclickAgent;

public class MessageView extends Activity implements OnClickListener, OnTouchListener {
    private static final String EXTRA_MESSAGE_ID = "com.android.email.MessageView_message_id";
    private static final String EXTRA_MAILBOX_ID = "com.android.email.MessageView_mailbox_id";
    /* package */static final String EXTRA_DISABLE_REPLY = "com.android.email.MessageView_disable_reply";
    private static final String EXTRA_SAVE_ATTACHMENT = "saveAttachment";
    private static final int REQUEST_CODE_SAVE_ATTACHMENT = 1;
    // for saveInstanceState()
    private static final String STATE_MESSAGE_ID = "messageId";

    // Regex that matches start of img tag. '<(?i)img\s+'.
    private static final Pattern IMG_TAG_START_REGEX = Pattern.compile("<(?i)img\\s+");
    // Regex that matches Web URL protocol part as case insensitive.
    private static final Pattern WEB_URL_PROTOCOL = Pattern.compile("(?i)http|https://");

    // Support for LoadBodyTask
    private static final String[] BODY_CONTENT_PROJECTION = new String[] { Body.RECORD_ID, BodyColumns.MESSAGE_KEY,
            BodyColumns.HTML_CONTENT, BodyColumns.TEXT_CONTENT };

    private static final String[] PRESENCE_STATUS_PROJECTION = new String[] { Contacts.CONTACT_PRESENCE };

    private static final int BODY_CONTENT_COLUMN_RECORD_ID = 0;
    private static final int BODY_CONTENT_COLUMN_MESSAGE_KEY = 1;
    private static final int BODY_CONTENT_COLUMN_HTML_CONTENT = 2;
    private static final int BODY_CONTENT_COLUMN_TEXT_CONTENT = 3;

    private static final int DIALOG_DELETE_MAIL = 1;

    private TextView mSubjectView;
    private TextView mFromView;
    private TextView mDateView;
    private TextView mTimeView;
    private TextView mToView;
    private TextView mCcView;
    private TextView mSizeView;
    private View mCcContainerView;
    private WebView mMessageContentView;
    private LinearLayout mAttachments;
    private ImageView mAttachmentIcon;
    private ImageView mFavoriteIcon;
    private View mShowPicturesSection;
    private View mInviteSection;
    private ImageView mSenderPresenceView;
    private ProgressDialog mProgressDialog;
    private View mScrollView;

    // calendar meeting invite answers
    private TextView mMeetingYes;
    private TextView mMeetingMaybe;
    private TextView mMeetingNo;
    private int mPreviousMeetingResponse = -1;

    private long mAccountId;
    private long mMessageId;
    private long mMailboxId;
    private Message mMessage;
    private long mWaitForLoadMessageId;

    private LoadMessageTask mLoadMessageTask;
    private LoadBodyTask mLoadBodyTask;
    private LoadAttachmentsTask mLoadAttachmentsTask;
    private PresenceCheckTask mPresenceCheckTask;

    private long mLoadAttachmentId; // the attachment being saved/viewed
    private boolean mLoadAttachmentSave; // if true, saving - if false, viewing
    private String mLoadAttachmentName; // the display name

    private java.text.DateFormat mDateFormat;
    private java.text.DateFormat mTimeFormat;

    private Drawable mFavoriteIconOn;
    private Drawable mFavoriteIconOff;

    private MessageViewHandler mHandler;
    private Controller mController;
    private ControllerResults mControllerCallback;

    private LinearLayout mHeaderLayoutZone;
    private View mMoveToNewer;
    private View mMoveToOlder;
    private View mCopyAll;
    private View mZoomIn;
    private View mZoomOut;

    private LoadMessageListTask mLoadMessageListTask;
    private Cursor mMessageListCursor;
    private ContentObserver mCursorObserver;

    // contains the HTML body. Is used by LoadAttachmentTask to display inline
    // images.
    // is null most of the time, is used transiently to pass info to
    // LoadAttachementTask
    private String mHtmlTextRaw;

    // contains the HTML content as set in WebView.
    private String mHtmlTextWebView;

    // this is true when reply & forward are disabled, such as messages in the
    // trash
    private boolean mDisableReplyAndForward;

    private Drawable backDrawable;
    private ClassBeBindedToJS classBeBindedToJS = new ClassBeBindedToJS();

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
    public String skinName;

    // ===============PopupMenu==============
    private class MessageViewHandler extends Handler {
        private static final int MSG_PROGRESS = 1;
        private static final int MSG_ATTACHMENT_PROGRESS = 2;
        private static final int MSG_LOAD_CONTENT_URI = 3;
        private static final int MSG_SET_ATTACHMENTS_ENABLED = 4;
        private static final int MSG_LOAD_BODY_ERROR = 5;
        private static final int MSG_NETWORK_ERROR = 6;
        private static final int MSG_FETCHING_ATTACHMENT = 10;
        private static final int MSG_VIEW_ATTACHMENT_ERROR = 12;
        private static final int MSG_UPDATE_ATTACHMENT_ICON = 18;
        private static final int MSG_FINISH_LOAD_ATTACHMENT = 19;
        private static final int MSG_SEND_SUCCESS = 20;

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case MSG_PROGRESS:
                setProgressBarIndeterminateVisibility(msg.arg1 != 0);
                break;
            case MSG_ATTACHMENT_PROGRESS:
                boolean progress = (msg.arg1 != 0);
                if (progress) {
                    mProgressDialog.setMessage(getString(R.string.message_view_fetching_attachment_progress,
                            mLoadAttachmentName));
                    mProgressDialog.show();
                } else {
                    mProgressDialog.dismiss();
                }
                setProgressBarIndeterminateVisibility(progress);
                break;
            case MSG_LOAD_CONTENT_URI:
                String uriString = (String) msg.obj;
                if (mMessageContentView != null) {
                    mMessageContentView.loadUrl(uriString);
                }
                break;
            case MSG_SET_ATTACHMENTS_ENABLED:
                for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
                    AttachmentInfo attachment = (AttachmentInfo) mAttachments.getChildAt(i).getTag();
                    attachment.viewButton.setEnabled(msg.arg1 == 1);
                    attachment.downloadButton.setEnabled(msg.arg1 == 1);
                }
                break;
            case MSG_LOAD_BODY_ERROR:
                Toast.makeText(MessageView.this, R.string.error_loading_message_body, Toast.LENGTH_LONG).show();
                break;
            case MSG_NETWORK_ERROR:
                Toast.makeText(MessageView.this, R.string.status_network_error, Toast.LENGTH_LONG).show();
                break;
            case MSG_FETCHING_ATTACHMENT:
                Toast.makeText(MessageView.this, getString(R.string.message_view_fetching_attachment_toast),
                        Toast.LENGTH_SHORT).show();
                break;
            case MSG_VIEW_ATTACHMENT_ERROR:
                Toast.makeText(MessageView.this, getString(R.string.message_view_display_attachment_toast),
                        Toast.LENGTH_SHORT).show();
                break;
            case MSG_UPDATE_ATTACHMENT_ICON:
                ((AttachmentInfo) mAttachments.getChildAt(msg.arg1).getTag()).iconView.setImageBitmap((Bitmap) msg.obj);
                break;
            case MSG_FINISH_LOAD_ATTACHMENT:
                long attachmentId = (Long) msg.obj;
                doFinishLoadAttachment(attachmentId);
                break;
            case MSG_SEND_SUCCESS:
                boolean showSuccess = (msg.arg1 != 0);
                if (showSuccess) {
                    Toast.makeText(MessageView.this, MessageView.this.getString(R.string.message_send_success_toast),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MessageView.this, MessageView.this.getString(R.string.message_sending_toast),
                            Toast.LENGTH_SHORT).show();
                }
            default:
                super.handleMessage(msg);
            }
        }

        public void attachmentProgress(boolean progress) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_ATTACHMENT_PROGRESS);
            msg.arg1 = progress ? 1 : 0;
            sendMessage(msg);
        }

        public void progress(boolean progress) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_PROGRESS);
            msg.arg1 = progress ? 1 : 0;
            sendMessage(msg);
        }

        public void loadContentUri(String uriString) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_LOAD_CONTENT_URI);
            msg.obj = uriString;
            sendMessage(msg);
        }

        public void setAttachmentsEnabled(boolean enabled) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_SET_ATTACHMENTS_ENABLED);
            msg.arg1 = enabled ? 1 : 0;
            sendMessage(msg);
        }

        public void loadBodyError() {
            sendEmptyMessage(MSG_LOAD_BODY_ERROR);
        }

        public void networkError() {
            sendEmptyMessage(MSG_NETWORK_ERROR);
        }

        public void fetchingAttachment() {
            sendEmptyMessage(MSG_FETCHING_ATTACHMENT);
        }

        public void attachmentViewError() {
            sendEmptyMessage(MSG_VIEW_ATTACHMENT_ERROR);
        }

        public void updateAttachmentIcon(int pos, Bitmap icon) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_UPDATE_ATTACHMENT_ICON);
            msg.arg1 = pos;
            msg.obj = icon;
            sendMessage(msg);
        }

        public void finishLoadAttachment(long attachmentId) {
            android.os.Message msg = android.os.Message.obtain(this, MSG_FINISH_LOAD_ATTACHMENT);
            msg.obj = Long.valueOf(attachmentId);
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
     * Encapsulates known information about a single attachment.
     */
    private static class AttachmentInfo {
        public String name;
        public String contentType;
        public long size;
        public long attachmentId;
        public Button viewButton;
        public Button downloadButton;
        public ImageView iconView;
    }

    /**
     * View a specific message found in the Email provider.
     * 
     * @param messageId
     *            the message to view.
     * @param mailboxId
     *            identifies the sequence of messages used for newer/older
     *            navigation.
     * @param disableReplyAndForward
     *            set if reply/forward do not make sense for this message (e.g.
     *            messages in Trash).
     */
    public static void actionView(Context context, long messageId, long mailboxId, boolean disableReplyAndForward) {
        if (messageId < 0) {
            throw new IllegalArgumentException("MessageView invalid messageId " + messageId);
        }
        Intent i = new Intent(context, MessageView.class);
        i.putExtra(EXTRA_MESSAGE_ID, messageId);
        i.putExtra(EXTRA_MAILBOX_ID, mailboxId);
        i.putExtra(EXTRA_DISABLE_REPLY, disableReplyAndForward);
        context.startActivity(i);
    }

    public static void actionView(Context context, long messageId, long mailboxId) {
        actionView(context, messageId, mailboxId, false);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.message_view);

        mHandler = new MessageViewHandler();
        mControllerCallback = new ControllerResults();

        mSubjectView = (TextView) findViewById(R.id.subject);
        mFromView = (TextView) findViewById(R.id.from);
        mToView = (TextView) findViewById(R.id.to);
        mCcView = (TextView) findViewById(R.id.cc);
        mSizeView = (TextView) findViewById(R.id.size);
        mCcContainerView = findViewById(R.id.cc_container);
        mDateView = (TextView) findViewById(R.id.date);
        mTimeView = (TextView) findViewById(R.id.time);
        mMessageContentView = (WebView) findViewById(R.id.message_content);
        mAttachments = (LinearLayout) findViewById(R.id.attachments);
        mAttachmentIcon = (ImageView) findViewById(R.id.attachment);
        mFavoriteIcon = (ImageView) findViewById(R.id.favorite);
        mShowPicturesSection = findViewById(R.id.show_pictures_section);
        mInviteSection = findViewById(R.id.invite_section);
        mSenderPresenceView = (ImageView) findViewById(R.id.presence);
        mMoveToNewer = findViewById(R.id.moveToNewer);
        mMoveToOlder = findViewById(R.id.moveToOlder);
        mCopyAll = findViewById(R.id.copy_all);
        mZoomOut = findViewById(R.id.plus_content);
        mZoomIn = findViewById(R.id.minus_content);
        mScrollView = findViewById(R.id.scrollview);

        mZoomOut.setOnClickListener(this);
        mZoomIn.setOnClickListener(this);
        mMoveToNewer.setOnClickListener(this);
        mMoveToOlder.setOnClickListener(this);
        mCopyAll.setOnClickListener(this);
        mFromView.setOnClickListener(this);
        mSenderPresenceView.setOnClickListener(this);
        mFavoriteIcon.setOnClickListener(this);
        findViewById(R.id.reply).setOnClickListener(this);
        findViewById(R.id.reply_all).setOnClickListener(this);
        findViewById(R.id.delete).setOnClickListener(this);
        findViewById(R.id.show_pictures).setOnClickListener(this);

        mMeetingYes = (TextView) findViewById(R.id.accept);
        mMeetingMaybe = (TextView) findViewById(R.id.maybe);
        mMeetingNo = (TextView) findViewById(R.id.decline);

        mMeetingYes.setOnClickListener(this);
        mMeetingMaybe.setOnClickListener(this);
        mMeetingNo.setOnClickListener(this);
        findViewById(R.id.invite_link).setOnClickListener(this);

        if (backDrawable == null) {
            backDrawable = fetchBackDrawable();
            skinName = Utiles.fetchSkinName(this);
        }
        if (null != backDrawable) {
            mMessageContentView.setBackgroundDrawable(backDrawable);
        }
        
//        mMessageContentView.setHorizontalScrollBarEnabled(true);
//        mMessageContentView.setVerticalScrollBarEnabled(true);
        
//        mMessageContentView.getSettings().setDefaultZoom(ZoomDensity.CLOSE);//默认缩放模式 是 ZoomDensity.MEDIUM
//        mMessageContentView.setInitialScale(100);
        mMessageContentView.getSettings().setBlockNetworkLoads(true);
        mMessageContentView.getSettings().setSupportZoom(true);
        mMessageContentView.getSettings().setBuiltInZoomControls(true);// 手势放大缩小
//        mMessageContentView.getSettings().setUseWideViewPort(true);// 设置此属性，可任意比例缩放。
        
//        mMessageContentView.getSettings().setDisplayZoomControls(false);//不显示默认放大缩小按钮 3.0以上支持
        setZoomControlGone(mMessageContentView);
        
        mMessageContentView.setWebViewClient(new CustomWebViewClient());

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        mDateFormat = android.text.format.DateFormat.getDateFormat(this); // short format
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(this); // 12/24 date format

        mFavoriteIconOn = getResources().getDrawable(R.drawable.btn_star_big_buttonless_on);
        mFavoriteIconOff = getResources().getDrawable(R.drawable.btn_star_big_buttonless_off);

        initFromIntent();
        if (icicle != null) {
            mMessageId = icicle.getLong(STATE_MESSAGE_ID, mMessageId);
        }

        mController = Controller.getInstance(getApplication());

        // This observer is used to watch for external changes to the message list
        mCursorObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                // get a new message list cursor, but only if we already had one
                // (otherwise it's "too soon" and other pathways will cause it
                // to be loaded)
                if (mLoadMessageListTask == null && mMessageListCursor != null) {
                    mLoadMessageListTask = new LoadMessageListTask(mMailboxId);
                    mLoadMessageListTask.execute();
                }
            }
        };

        messageChanged();
        mHeaderLayoutZone = (LinearLayout) findViewById(R.id.header_layout_zone);
        mHeaderLayoutZone.setOnTouchListener(this);

        // setWebViewCopy();
        initMenuResourse();
        initPopupMenu();

    }
    /**
     * 不显示默认放大缩小按钮 适配3.0以下支持
     * @param view
     */
    public void setZoomControlGone(View view) {
        Class<?> classType;
        Field field;
        try {
            classType = WebView.class;
            field = classType.getDeclaredField("mZoomButtonsController");
            field.setAccessible(true);
            ZoomButtonsController mZoomButtonsController = new ZoomButtonsController(view);
            mZoomButtonsController.getZoomControls().setVisibility(View.GONE);
            try {
                field.set(view, mZoomButtonsController);
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private Drawable fetchBackDrawable() {
        String targetDir = Environment.getDataDirectory() + "/data/" + getPackageName() + "/skin/";
        String listViewBack = targetDir + "back2.png";
        Drawable drawable = null;
        File bgFile = new File(listViewBack);
        try {
            drawable = BitmapDrawable.createFromPath(bgFile.getCanonicalPath());
        } catch (IOException e) {
            Logs.e(Logs.LOG_TAG, "error :" + e.getMessage());
            e.printStackTrace();
        }
        return drawable;
    }

    /* package */void initFromIntent() {
        Intent intent = getIntent();
        mMessageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
        mMailboxId = intent.getLongExtra(EXTRA_MAILBOX_ID, -1);
        mDisableReplyAndForward = intent.getBooleanExtra(EXTRA_DISABLE_REPLY, false);
        if (mDisableReplyAndForward) {
            findViewById(R.id.reply).setEnabled(false);
            findViewById(R.id.reply_all).setEnabled(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mMessageId != -1) {
            state.putLong(STATE_MESSAGE_ID, mMessageId);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        mWaitForLoadMessageId = -1;
        mController.addResultCallback(mControllerCallback);

        // Exit immediately if the accounts list has changed (e.g. externally
        // deleted)
        if (Email.getNotifyUiAccountsChanged()) {
            Welcome.actionStart(this);
            finish();
            return;
        }

        if (mMessage != null) {
            startPresenceCheck();

            // get a new message list cursor, but only if mailbox is set
            // (otherwise it's "too soon" and other pathways will cause it to be
            // loaded)
            if (mLoadMessageListTask == null && mMailboxId != -1) {
                mLoadMessageListTask = new LoadMessageListTask(mMailboxId);
                mLoadMessageListTask.execute();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        mController.removeResultCallback(mControllerCallback);
        closeMessageListCursor();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        onTouchEvent(event);
        return true;
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//
//        // 获得触摸的坐标
//        float x = event.getX();
//        float y = event.getY();
//        switch (event.getAction()) {
//        // 触摸屏幕时刻
//        case MotionEvent.ACTION_DOWN:
//
//            break;
//
//        // 触摸并移动时刻
//        case MotionEvent.ACTION_MOVE:
//            break;
//
//        // 终止触摸时刻
//        case MotionEvent.ACTION_UP:
//            break;
//        }
//        return super.onTouchEvent(event);
//    }

    private void closeMessageListCursor() {
        if (mMessageListCursor != null) {
            mMessageListCursor.unregisterContentObserver(mCursorObserver);
            mMessageListCursor.close();
            mMessageListCursor = null;
        }
    }

    private void cancelAllTasks() {
        Utility.cancelTaskInterrupt(mLoadMessageTask);
        mLoadMessageTask = null;
        Utility.cancelTaskInterrupt(mLoadBodyTask);
        mLoadBodyTask = null;
        Utility.cancelTaskInterrupt(mLoadAttachmentsTask);
        mLoadAttachmentsTask = null;
        Utility.cancelTaskInterrupt(mLoadMessageListTask);
        mLoadMessageListTask = null;
        Utility.cancelTaskInterrupt(mPresenceCheckTask);
        mPresenceCheckTask = null;
    }

    /**
     * We override onDestroy to make sure that the WebView gets explicitly
     * destroyed. Otherwise it can leak native references.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelAllTasks();
        // This is synchronized because the listener accesses
        // mMessageContentView from its thread
        // synchronized (this) {
        // mMessageContentView.destroy();
        // mMessageContentView = null;
        // }
        // the cursor was closed in onPause()
        if (mMessageContentView != null) {
            mMessageContentView.getSettings().setBuiltInZoomControls(true);
            mMessageContentView.setVisibility(View.GONE);
            long timeout = ViewConfiguration.getZoomControlsTimeout();
            new Timer().schedule(new TimerTask() {

                @Override
                public void run() {
                    synchronized (this) {
                        mMessageContentView.destroy();
                        mMessageContentView = null;
                    }
                }
            }, timeout); // 注意，这个timeout就是那两个sb的按钮的消失的时间，你可以给他加上一，二
        }
    }

    private void onDelete() {
        if (mMessage != null) {
            // the delete triggers mCursorObserver
            // first move to older/newer before the actual delete
            long messageIdToDelete = mMessageId;
            boolean moved = moveToOlder() || moveToNewer();
            mController.deleteMessage(messageIdToDelete, mMessage.mAccountKey);
            Toast.makeText(this, getResources().getQuantityString(R.plurals.message_deleted_toast, 1),
                    Toast.LENGTH_SHORT).show();
            if (!moved) {
                // this generates a benign warning "Duplicate finish request"
                // because
                // repositionMessageListCursor() will fail to reposition and do
                // its own finish()
                finish();
            }
        }
    }

    /**
     * Overrides for various WebView behaviors.
     */
    private class CustomWebViewClient extends WebViewClient {
        /**
         * This is intended to mirror the operation of the original (see
         * android.webkit.CallbackProxy) with one addition of intent flags
         * "FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET". This improves behavior when
         * sublaunching other apps via embedded URI's.
         * 
         * We also use this hook to catch "mailto:" links and handle them
         * locally.
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // hijack mailto: uri's and handle locally
           if (url != null && url.toLowerCase().startsWith("mailto:")) {
                return MessageCompose.actionCompose(MessageView.this, url, mAccountId);
            }

            // Handle most uri's via intent launch
            boolean result = false;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            try {
                startActivity(intent);
                result = true;
            } catch (ActivityNotFoundException ex) {
                // If no application can handle the URL, assume that the
                // caller can handle it.
            }
            return result;
        }
    }

    /**
     * Handle clicks on sender, which shows {@link QuickContact} or prompts to
     * add the sender as a contact.
     */
    private void onClickSender() {
        // Bail early if message or sender not present
        if (mMessage == null)
            return;

        final Address senderEmail = Address.unpackFirst(mMessage.mFrom);
        if (senderEmail == null)
            return;

        // First perform lookup query to find existing contact
        final ContentResolver resolver = getContentResolver();
        final String address = senderEmail.getAddress();
        final Uri dataUri = Uri.withAppendedPath(CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(address));
        final Uri lookupUri = ContactsContract.Data.getContactLookupUri(resolver, dataUri);

        if (lookupUri != null) {
            // Found matching contact, trigger QuickContact
            QuickContact.showQuickContact(this, mSenderPresenceView, lookupUri, QuickContact.MODE_LARGE, null);
        } else {
            // No matching contact, ask user to create one
            final Uri mailUri = Uri.fromParts("mailto", address, null);
            final Intent intent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, mailUri);

            // Pass along full E-mail string for possible create dialog
            intent.putExtra(ContactsContract.Intents.EXTRA_CREATE_DESCRIPTION, senderEmail.toString());

            // Only provide personal name hint if we have one
            final String senderPersonal = senderEmail.getPersonal();
            if (!TextUtils.isEmpty(senderPersonal)) {
                intent.putExtra(ContactsContract.Intents.Insert.NAME, senderPersonal);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

            startActivity(intent);
        }
    }

    /**
     * Toggle favorite status and write back to provider
     */
    private void onClickFavorite() {
        if (mMessage != null) {
            // Update UI
            boolean newFavorite = !mMessage.mFlagFavorite;
            mFavoriteIcon.setImageDrawable(newFavorite ? mFavoriteIconOn : mFavoriteIconOff);

            // Update provider
            mMessage.mFlagFavorite = newFavorite;
            mController.setMessageFavorite(mMessageId, newFavorite);
        }
    }

    private void onReply() {
        if (mMessage != null) {
            MessageCompose.actionReply(this, mMessage.mId, false);
            finish();
        }
    }

    private void onReplyAll() {
        if (mMessage != null) {
            MessageCompose.actionReply(this, mMessage.mId, true);
            finish();
        }
    }

    private void onForward() {
        if (mMessage != null) {
            MessageCompose.actionForward(this, mMessage.mId);
            finish();
        }
    }

    private boolean moveToOlder() {
        // Guard with !isLast() because Cursor.moveToNext() returns false even
        // as it moves
        // from last to after-last.
        if (mMessageListCursor != null && !mMessageListCursor.isLast() && mMessageListCursor.moveToNext()) {
            mMessageId = mMessageListCursor.getLong(0);
            messageChanged();
            return true;
        }
        return false;
    }

    private boolean moveToNewer() {
        // Guard with !isFirst() because Cursor.moveToPrev() returns false even
        // as it moves
        // from first to before-first.
        if (mMessageListCursor != null && !mMessageListCursor.isFirst() && mMessageListCursor.moveToPrevious()) {
            mMessageId = mMessageListCursor.getLong(0);
            messageChanged();
            return true;
        }
        return false;
    }

    private void onMarkAsRead(boolean isRead) {
        if (mMessage != null && mMessage.mFlagRead != isRead) {
            mMessage.mFlagRead = isRead;
            mController.setMessageRead(mMessageId, isRead);
        }
    }

    /**
     * Creates a unique file in the given directory by appending a hyphen and a
     * number to the given filename.
     * 
     * @param directory
     * @param filename
     * @return a new File object, or null if one could not be created
     */
    /* package */static File createUniqueFile(File directory, String filename) {
        File file = new File(directory, filename);
        if (!file.exists()) {
            return file;
        }
        // Get the extension of the file, if any.
        int index = filename.lastIndexOf('.');
        String format;
        if (index != -1) {
            String name = filename.substring(0, index);
            String extension = filename.substring(index);
            format = name + "-%d" + extension;
        } else {
            format = filename + "-%d";
        }
        for (int i = 2; i < Integer.MAX_VALUE; i++) {
            file = new File(directory, String.format(format, i));
            if (!file.exists()) {
                return file;
            }
        }
        return null;
    }

    /**
     * Send a service message indicating that a meeting invite button has been
     * clicked.
     */
    private void onRespond(int response, int toastResId) {
        // do not send twice in a row the same response
        if (mPreviousMeetingResponse != response) {
            mController.sendMeetingResponse(mMessageId, response, mControllerCallback);
            mPreviousMeetingResponse = response;
        }
        Toast.makeText(this, toastResId, Toast.LENGTH_SHORT).show();
        if (!moveToOlder()) {
            finish(); // if this is the last message, move up to message-list.
        }
    }

    private void onDownloadAttachment(AttachmentInfo attachment) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            /*
             * Abort early if there's no place to save the attachment. We don't
             * want to spend the time downloading it and then abort.
             */
            Toast.makeText(this, getString(R.string.message_view_status_attachment_not_saved), Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        mLoadAttachmentId = attachment.attachmentId;
        mLoadAttachmentSave = true;
        mLoadAttachmentName = attachment.name;

        mController.loadAttachment(attachment.attachmentId, mMessageId, mMessage.mMailboxKey, mAccountId,
                mControllerCallback);
    }

    private void onViewAttachment(AttachmentInfo attachment) {
        mLoadAttachmentId = attachment.attachmentId;
        mLoadAttachmentSave = false;
        mLoadAttachmentName = attachment.name;

        mController.loadAttachment(attachment.attachmentId, mMessageId, mMessage.mMailboxKey, mAccountId,
                mControllerCallback);
    }

    private void onShowPictures() {
        if (mMessage != null) {
            if (mMessageContentView != null) {
                mMessageContentView.getSettings().setBlockNetworkLoads(false);
                if (mHtmlTextWebView != null) {
                    mMessageContentView.loadDataWithBaseURL("email://", mHtmlTextWebView, "text/html", "utf-8", null);
                }
            }
            mShowPicturesSection.setVisibility(View.GONE);
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.from:
        case R.id.presence:
            onClickSender();
            break;
        case R.id.favorite:
            onClickFavorite();
            break;
        case R.id.reply:
            onReply();
            break;
        case R.id.reply_all:
            onReplyAll();
            break;
        case R.id.delete:
            // onDelete();
            showDialog(DIALOG_DELETE_MAIL);
            break;
        case R.id.moveToOlder:
            moveToOlder();
            break;
        case R.id.moveToNewer:
            moveToNewer();
            break;
        case R.id.copy_all:
            copy();
            break;
        case R.id.plus_content:
            if (!mMessageContentView.zoomIn()) {
                mZoomOut.setVisibility(View.GONE);
            }
            if (mZoomIn.getVisibility() == View.GONE) {
                mZoomIn.setVisibility(View.VISIBLE);
            }
            break;
        case R.id.minus_content:
            if (!mMessageContentView.zoomOut()) {
                mZoomIn.setVisibility(View.GONE);
            }
            if (mZoomOut.getVisibility() == View.GONE) {
                mZoomOut.setVisibility(View.VISIBLE);
            }
            break;
        case R.id.download:
            onDownloadAttachment((AttachmentInfo) view.getTag());
            break;
        case R.id.view:
            onViewAttachment((AttachmentInfo) view.getTag());
            break;
        case R.id.show_pictures:
            onShowPictures();
            break;
        case R.id.accept:
            onRespond(EmailServiceConstants.MEETING_REQUEST_ACCEPTED, R.string.message_view_invite_toast_yes);
            break;
        case R.id.maybe:
            onRespond(EmailServiceConstants.MEETING_REQUEST_TENTATIVE, R.string.message_view_invite_toast_maybe);
            break;
        case R.id.decline:
            onRespond(EmailServiceConstants.MEETING_REQUEST_DECLINED, R.string.message_view_invite_toast_no);
            break;
        case R.id.invite_link:
            String startTime = new PackedString(mMessage.mMeetingInfo).get(MeetingInfo.MEETING_DTSTART);
            if (startTime != null) {
                long epochTimeMillis = Utility.parseEmailDateTimeToMillis(startTime);
                Uri uri = Uri.parse("content://com.android.calendar/time/" + epochTimeMillis);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                intent.putExtra("VIEW", "DAY");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
            } else {
                Email.log("meetingInfo without DTSTART " + mMessage.mMeetingInfo);
            }
            break;
        }
    }

    public void copy() {
        ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        // clip.getText(); // 粘贴
        clip.setText(classBeBindedToJS.getBodyString()); // 复制
        Toast.makeText(MessageView.this, getString(R.string.copy_all), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = handleMenuItem(item.getItemId());
        if (!handled) {
            handled = super.onOptionsItemSelected(item);
        }
        return handled;
    }

    /**
     * This is the core functionality of onOptionsItemSelected() but broken out
     * and exposed for testing purposes (because it's annoying to mock a
     * MenuItem).
     * 
     * @param menuItemId
     *            id that was clicked
     * @return true if handled here
     */
    /* package */boolean handleMenuItem(int menuItemId) {
        switch (menuItemId) {
        case R.id.delete:
            onDelete();
            break;
        case R.id.reply:
            onReply();
            break;
        case R.id.reply_all:
            onReplyAll();
            break;
        case R.id.forward:
            onForward();
            break;
        case R.id.mark_as_unread:
            onMarkAsRead(false);
            finish();
            break;
        default:
            return false;
        }
        return true;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_DELETE_MAIL:
            return createDeleteDialog();
        }
        return super.onCreateDialog(id);
    }

    private Dialog createDeleteDialog() {
        return new AlertDialog.Builder(this).setMessage(R.string.dialog_delete).setTitle(R.string.delete_action)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        onDelete();
                        dialog.cancel();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                }).create();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!skinName.equals(Global.SKIN_NAME)) {
            menu.add("menu");// 必须创建一项
            return super.onCreateOptionsMenu(menu);
        } else {
            super.onCreateOptionsMenu(menu);
            getMenuInflater().inflate(R.menu.message_view_option, menu);
            if (mDisableReplyAndForward) {
                menu.findItem(R.id.forward).setEnabled(false);
                menu.findItem(R.id.reply).setEnabled(false);
                menu.findItem(R.id.reply_all).setEnabled(false);
            }
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
                    popup.showAtLocation(mHeaderLayoutZone, Gravity.BOTTOM, 0, 0);
                }
            }
            return false;// 返回为true 则显示系统menu
        } else {
            return true;
        }
    }

    /**
     * Re-init everything needed for changing message.
     */
    private void messageChanged() {
        if (Email.DEBUG) {
            Email.log("MessageView: messageChanged to id=" + mMessageId);
        }
        cancelAllTasks();
        setTitle("");
        if (mMessageContentView != null) {
            mMessageContentView.scrollTo(0, 0);
            mMessageContentView.loadUrl("file:///android_asset/empty.html");
        }
        mScrollView.scrollTo(0, 0);
        mAttachments.removeAllViews();
        mAttachments.setVisibility(View.GONE);
        mAttachmentIcon.setVisibility(View.GONE);

        // Start an AsyncTask to make a new cursor and load the message
        mLoadMessageTask = new LoadMessageTask(mMessageId, true);
        mLoadMessageTask.execute();
        updateNavigationArrows(mMessageListCursor);
    }

    /**
     * Reposition the older/newer cursor. Finish() the activity if we are no
     * longer in the list. Update the UI arrows as appropriate.
     */
    private void repositionMessageListCursor() {
        // position the cursor on the current message
        mMessageListCursor.moveToPosition(-1);
        while (mMessageListCursor.moveToNext() && mMessageListCursor.getLong(0) != mMessageId) {
        }
        if (mMessageListCursor.isAfterLast()) {
            // overshoot - get out now, the list is no longer valid
            if (mMailboxId != Mailbox.QUERY_ALL_UNREAD) {
                finish();
            }
        }
        updateNavigationArrows(mMessageListCursor);
    }

    /**
     * Update the arrows based on the current position of the older/newer
     * cursor.
     */
    private void updateNavigationArrows(Cursor cursor) {
        if (cursor != null) {
            boolean hasNewer, hasOlder;
            if (cursor.isAfterLast() || cursor.isBeforeFirst()) {
                // The cursor not being on a message means that the current
                // message was not found.
                // While this should not happen, simply disable prev/next arrows
                // in that case.
                hasNewer = hasOlder = false;
            } else {
                hasNewer = !cursor.isFirst();
                hasOlder = !cursor.isLast();
            }
            // mMoveToNewer.setVisibility(hasNewer ? View.VISIBLE :
            // View.INVISIBLE);
            mMoveToNewer.setEnabled(hasNewer);
            // mMoveToOlder.setVisibility(hasOlder ? View.VISIBLE :
            // View.INVISIBLE);
            mMoveToOlder.setEnabled(hasOlder);
        }
    }

    private Bitmap getPreviewIcon(AttachmentInfo attachment) {
        try {
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(
                    AttachmentProvider.getAttachmentThumbnailUri(mAccountId, attachment.attachmentId, 62, 62)));
        } catch (Exception e) {
            Log.d(Email.LOG_TAG, "Attachment preview failed with exception " + e.getMessage());
            return null;
        }
    }

    /*
     * Formats the given size as a String in bytes, kB, MB or GB with a single
     * digit of precision. Ex: 12,315,000 = 12.3 MB
     */
    public static String formatSize(float size) {
        long kb = 1024;
        long mb = (kb * 1024);
        long gb = (mb * 1024);
        if (size < kb) {
            return String.format("%d bytes", (int) size);
        } else if (size < mb) {
            return String.format("%.1f kB", size / kb);
        } else if (size < gb) {
            return String.format("%.1f MB", size / mb);
        } else {
            return String.format("%.1f GB", size / gb);
        }
    }

    private void updateAttachmentThumbnail(long attachmentId) {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            AttachmentInfo attachment = (AttachmentInfo) mAttachments.getChildAt(i).getTag();
            if (attachment.attachmentId == attachmentId) {
                Bitmap previewIcon = getPreviewIcon(attachment);
                if (previewIcon != null) {
                    mHandler.updateAttachmentIcon(i, previewIcon);
                }
                return;
            }
        }
    }

    /**
     * Copy data from a cursor-refreshed attachment into the UI. Called from UI
     * thread.
     * 
     * @param attachment
     *            A single attachment loaded from the provider
     */
    private void addAttachment(Attachment attachment) {

        AttachmentInfo attachmentInfo = new AttachmentInfo();
        attachmentInfo.size = attachment.mSize;
        attachmentInfo.contentType = AttachmentProvider.inferMimeType(attachment.mFileName, attachment.mMimeType);
        attachmentInfo.name = attachment.mFileName;
        attachmentInfo.attachmentId = attachment.mId;

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.message_view_attachment, null);

        TextView attachmentName = (TextView) view.findViewById(R.id.attachment_name);
        TextView attachmentInfoView = (TextView) view.findViewById(R.id.attachment_info);
        ImageView attachmentIcon = (ImageView) view.findViewById(R.id.attachment_icon);
        Button attachmentView = (Button) view.findViewById(R.id.view);
        Button attachmentDownload = (Button) view.findViewById(R.id.download);

        if ((!MimeUtility.mimeTypeMatches(attachmentInfo.contentType, Email.ACCEPTABLE_ATTACHMENT_VIEW_TYPES))
                || (MimeUtility.mimeTypeMatches(attachmentInfo.contentType, Email.UNACCEPTABLE_ATTACHMENT_VIEW_TYPES))) {
            attachmentView.setVisibility(View.GONE);
        }
        if ((!MimeUtility.mimeTypeMatches(attachmentInfo.contentType, Email.ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES))
                || (MimeUtility.mimeTypeMatches(attachmentInfo.contentType,
                        Email.UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES))) {
            attachmentDownload.setVisibility(View.GONE);
        }

        if (attachmentInfo.size > Email.MAX_ATTACHMENT_DOWNLOAD_SIZE) {
            attachmentView.setVisibility(View.GONE);
            attachmentDownload.setVisibility(View.GONE);
        }

        attachmentInfo.viewButton = attachmentView;
        attachmentInfo.downloadButton = attachmentDownload;
        attachmentInfo.iconView = attachmentIcon;

        view.setTag(attachmentInfo);
        attachmentView.setOnClickListener(this);
        attachmentView.setTag(attachmentInfo);
        attachmentDownload.setOnClickListener(this);
        attachmentDownload.setTag(attachmentInfo);

        attachmentName.setText(attachmentInfo.name);
        attachmentInfoView.setText(formatSize(attachmentInfo.size));

        Bitmap previewIcon = getPreviewIcon(attachmentInfo);
        if (previewIcon != null) {
            attachmentIcon.setImageBitmap(previewIcon);
        }

        mAttachments.addView(view);
        mAttachments.setVisibility(View.VISIBLE);
    }

    private class PresenceCheckTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... emails) {
            Cursor cursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI, PRESENCE_STATUS_PROJECTION,
                    CommonDataKinds.Email.DATA + "=?", emails, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int status = cursor.getInt(0);
                        int icon = StatusUpdates.getPresenceIconResourceId(status);
                        return icon;
                    }
                } finally {
                    cursor.close();
                }
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer icon) {
            if (icon == null) {
                return;
            }
            updateSenderPresence(icon);
        }
    }

    /**
     * Launch a thread (because of cross-process DB lookup) to check presence of
     * the sender of the message. When that thread completes, update the UI.
     * 
     * This must only be called when mMessage is null (it will hide presence
     * indications) or when mMessage has already seen its headers loaded.
     * 
     * Note: This is just a polling operation. A more advanced solution would be
     * to keep the cursor open and respond to presence status updates (in the
     * form of content change notifications). However, because presence changes
     * fairly slowly compared to the duration of viewing a single message, a
     * simple poll at message load (and onResume) should be sufficient.
     */
    private void startPresenceCheck() {
        if (mMessage != null) {
            Address sender = Address.unpackFirst(mMessage.mFrom);
            if (sender != null) {
                String email = sender.getAddress();
                if (email != null) {
                    mPresenceCheckTask = new PresenceCheckTask();
                    mPresenceCheckTask.execute(email);
                    return;
                }
            }
        }
        updateSenderPresence(0);
    }

    /**
     * Update the actual UI. Must be called from main thread (or handler)
     * 
     * @param presenceIconId
     *            the presence of the sender, 0 for "unknown"
     */
    private void updateSenderPresence(int presenceIconId) {
        if (presenceIconId == 0) {
            // This is a placeholder used for "unknown" presence, including
            // signed off,
            // no presence relationship.
            presenceIconId = R.drawable.presence_inactive;
        }
        mSenderPresenceView.setImageResource(presenceIconId);
    }

    /**
     * This task finds out the messageId for the previous and next message in
     * the order given by mailboxId as used in MessageList.
     * 
     * It generates the same cursor as the one used in MessageList (but with an
     * id-only projection), scans through it until finds the current messageId,
     * and takes the previous and next ids.
     */
    private class LoadMessageListTask extends AsyncTask<Void, Void, Cursor> {
        private long mLocalMailboxId;

        public LoadMessageListTask(long mailboxId) {
            mLocalMailboxId = mailboxId;
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            String selection;
            if (mLocalMailboxId == Mailbox.QUERY_ALL_SEARCH_INBOX) {
                selection = Utility.buildSearch(getContentResolver(), MessageList.searchCondition.trim(),
                        MessageList.getMailboxIdByType(MessageView.this));
            } else {
                selection = Utility.buildMailboxIdSelection(getContentResolver(), mLocalMailboxId);
            }
            Cursor c = getContentResolver().query(EmailContent.Message.CONTENT_URI, EmailContent.ID_PROJECTION,
                    selection, null, EmailContent.MessageColumns.TIMESTAMP + " DESC");
            if (isCancelled()) {
                c.close();
                c = null;
            }
            return c;
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            // remove the reference to ourselves so another one can be launched
            MessageView.this.mLoadMessageListTask = null;

            if (cursor == null || cursor.isClosed()) {
                return;
            }
            // replace the older cursor if there is one
            closeMessageListCursor();
            mMessageListCursor = cursor;
            mMessageListCursor.registerContentObserver(MessageView.this.mCursorObserver);

            repositionMessageListCursor();

        }
    }

    /**
     * Async task for loading a single message outside of the UI thread Note: To
     * support unit testing, a sentinel messageId of Long.MIN_VALUE prevents
     * loading the message but leaves the activity open.
     */
    private class LoadMessageTask extends AsyncTask<Void, Void, Message> {

        private long mId;
        private boolean mOkToFetch;

        /**
         * Special constructor to cache some local info
         */
        public LoadMessageTask(long messageId, boolean okToFetch) {
            mId = messageId;
            mOkToFetch = okToFetch;
        }

        @Override
        protected Message doInBackground(Void... params) {
            if (mId == Long.MIN_VALUE) {
                return null;
            }
            return Message.restoreMessageWithId(MessageView.this, mId);
        }

        @Override
        protected void onPostExecute(Message message) {
            /*
             * doInBackground() may return null result (due to
             * restoreMessageWithId()) and in that situation we want to
             * Activity.finish().
             * 
             * OTOH we don't want to Activity.finish() for isCancelled() because
             * this would introduce a surprise side-effect to task cancellation:
             * every task cancelation would also result in finish().
             * 
             * Right now LoadMesageTask is cancelled not only from onDestroy(),
             * and it would be a bug to also finish() the activity in that
             * situation.
             */
            if (isCancelled()) {
                return;
            }
            if (message == null) {
                if (mId != Long.MIN_VALUE) {
                    finish();
                }
                return;
            }
            reloadUiFromMessage(message, mOkToFetch);
            startPresenceCheck();
        }
    }

    /**
     * Async task for loading a single message body outside of the UI thread
     */
    private class LoadBodyTask extends AsyncTask<Void, Void, String[]> {

        private long mId;

        /**
         * Special constructor to cache some local info
         */
        public LoadBodyTask(long messageId) {
            mId = messageId;
        }

        @Override
        protected String[] doInBackground(Void... params) {
            try {
                String text = null;
                String html = Body.restoreBodyHtmlWithMessageId(MessageView.this, mId);
                if (html == null) {
                    text = Body.restoreBodyTextWithMessageId(MessageView.this, mId);
                }
                return new String[] { text, html };
            } catch (RuntimeException re) {
                // This catches SQLiteException as well as other RTE's we've
                // seen from the
                // database calls, such as IllegalStateException
                Log.d(Email.LOG_TAG, "Exception while loading message body: " + re.toString());
                mHandler.loadBodyError();
                return new String[] { null, null };
            }
        }

        @Override
        protected void onPostExecute(String[] results) {
            if (results == null) {
                return;
            }
            reloadUiFromBody(results[0], results[1]); // text, html
            onMarkAsRead(true);
        }
    }

    /**
     * Async task for loading attachments
     * 
     * Note: This really should only be called when the message load is complete
     * - or, we should leave open a listener so the attachments can fill in as
     * they are discovered. In either case, this implementation is incomplete,
     * as it will fail to refresh properly if the message is partially loaded at
     * this time.
     */
    private class LoadAttachmentsTask extends AsyncTask<Long, Void, Attachment[]> {
        @Override
        protected Attachment[] doInBackground(Long... messageIds) {
            return Attachment.restoreAttachmentsWithMessageId(MessageView.this, messageIds[0]);
        }

        @Override
        protected void onPostExecute(Attachment[] attachments) {
            if (attachments == null) {
                return;
            }
            boolean htmlChanged = false;
            for (Attachment attachment : attachments) {
                if (mHtmlTextRaw != null && attachment.mContentId != null && attachment.mContentUri != null) {
                    // for html body, replace CID for inline images
                    // Regexp which matches ' src="cid:contentId"'.
                    String contentIdRe = "\\s+(?i)src=\"cid(?-i):\\Q" + attachment.mContentId + "\\E\"";
                    String srcContentUri = " src=\"" + attachment.mContentUri + "\"";
                    mHtmlTextRaw = mHtmlTextRaw.replaceAll(contentIdRe, srcContentUri);
                    htmlChanged = true;
                } else {
                    addAttachment(attachment);
                }
            }
            mHtmlTextWebView = mHtmlTextRaw;
            mHtmlTextRaw = null;
            if (htmlChanged && mMessageContentView != null) {
                mMessageContentView.loadDataWithBaseURL("email://", mHtmlTextWebView, "text/html", "utf-8", null);
            }
        }
    }

    /**
     * Reload the UI from a provider cursor. This must only be called from the
     * UI thread.
     * 
     * @param message
     *            A copy of the message loaded from the database
     * @param okToFetch
     *            If true, and message is not fully loaded, it's OK to fetch
     *            from the network. Use false to prevent looping here.
     * 
     *            TODO: trigger presence check
     */
    private void reloadUiFromMessage(Message message, boolean okToFetch) {
        mMessage = message;
        mAccountId = message.mAccountKey;
        if (mMailboxId == -1) {
            mMailboxId = message.mMailboxKey;
        }
        // only start LoadMessageListTask here if it's the first time
        if (mMessageListCursor == null) {
            mLoadMessageListTask = new LoadMessageListTask(mMailboxId);
            mLoadMessageListTask.execute();
        }

        mSubjectView.setText(message.mSubject);
        mFromView.setText(Address.toFriendly(Address.unpack(message.mFrom)));
        Date date = new Date(message.mTimeStamp);
        mTimeView.setText(mTimeFormat.format(date));
        mDateView.setText(Utility.isDateToday(date) ? null : mDateFormat.format(date));
        mToView.setText(Address.toFriendly(Address.unpack(message.mTo)));
        String friendlyCc = Address.toFriendly(Address.unpack(message.mCc));
        mCcView.setText(friendlyCc);
        mCcContainerView.setVisibility((friendlyCc != null) ? View.VISIBLE : View.GONE);

        mSizeView.setText(Formatter.formatShortFileSize(this, message.mSize));
        mAttachmentIcon.setVisibility(message.mAttachments != null ? View.VISIBLE : View.GONE);
        mFavoriteIcon.setImageDrawable(message.mFlagFavorite ? mFavoriteIconOn : mFavoriteIconOff);
        // Show the message invite section if we're an incoming meeting
        // invitation only
        mInviteSection.setVisibility((message.mFlags & Message.FLAG_INCOMING_MEETING_INVITE) != 0 ? View.VISIBLE
                : View.GONE);

        // Handle partially-loaded email, as follows:
        // 1. Check value of message.mFlagLoaded
        // 2. If != LOADED, ask controller to load it
        // 3. Controller callback (after loaded) should trigger LoadBodyTask &
        // LoadAttachmentsTask
        // 4. Else start the loader tasks right away (message already loaded)
        if (okToFetch && message.mFlagLoaded != Message.FLAG_LOADED_COMPLETE) {
            mWaitForLoadMessageId = message.mId;

            mController.loadMessageForView(message.mId, mControllerCallback);
        } else {
            mWaitForLoadMessageId = -1;
            // Ask for body
            mLoadBodyTask = new LoadBodyTask(message.mId);
            mLoadBodyTask.execute();
        }
    }

    /**
     * Reload the body from the provider cursor. This must only be called from
     * the UI thread.
     * 
     * @param bodyText
     *            text part
     * @param bodyHtml
     *            html part
     * 
     *            TODO deal with html vs text and many other issues
     */
    private void reloadUiFromBody(String bodyText, String bodyHtml) {
        String text = null;
        mHtmlTextRaw = null;
        boolean hasImages = false;

        if (bodyHtml == null) {
            text = bodyText;
            classBeBindedToJS.setBodyString(text);
            /*
             * Convert the plain text to HTML
             */
            StringBuffer sb = new StringBuffer("<html><body>");
            if (text != null) {
                // Escape any inadvertent HTML in the text message
                text = EmailHtmlUtil.escapeCharacterToDisplay(text);
                // Find any embedded URL's and linkify
                Matcher m = Patterns.WEB_URL.matcher(text);
                while (m.find()) {
                    int start = m.start();
                    /*
                     * WEB_URL_PATTERN may match domain part of email address.
                     * To detect this false match, the character just before the
                     * matched string should not be '@'.
                     */
                    if (start == 0 || text.charAt(start - 1) != '@') {
                        String url = m.group();
                        Matcher proto = WEB_URL_PROTOCOL.matcher(url);
                        String link;
                        if (proto.find()) {
                            // This is work around to force URL protocol part be
                            // lower case,
                            // because WebView could follow only lower case
                            // protocol link.
                            link = proto.group().toLowerCase() + url.substring(proto.end());
                        } else {
                            // Patterns.WEB_URL matches URL without protocol
                            // part,
                            // so added default protocol to link.
                            link = "http://" + url;
                        }
                        String href = String.format("<a href=\"%s\">%s</a>", link, url);
                        m.appendReplacement(sb, href);
                    } else {
                        m.appendReplacement(sb, "$0");
                    }
                }
                m.appendTail(sb);
            }
            sb.append("</body></html>");
            text = sb.toString();
        } else {
            text = bodyHtml;
            classBeBindedToJS.setBodyString(getTxtWithoutHTMLElement(text));
            mHtmlTextRaw = bodyHtml;
            hasImages = IMG_TAG_START_REGEX.matcher(text).find();
        }

        mShowPicturesSection.setVisibility(hasImages ? View.VISIBLE : View.GONE);
        if (mMessageContentView != null) {
            mMessageContentView.loadDataWithBaseURL("email://", text, "text/html", "utf-8", null);
        }

        // Ask for attachments after body
        mLoadAttachmentsTask = new LoadAttachmentsTask();
        mLoadAttachmentsTask.execute(mMessage.mId);
    }

    /**
     * Controller results listener. This completely replaces MessagingListener
     */
    private class ControllerResults implements Controller.Result {

        public void loadMessageForViewCallback(MessagingException result, long messageId, int progress) {
            if (messageId != MessageView.this.mMessageId || messageId != MessageView.this.mWaitForLoadMessageId) {
                // We are not waiting for this message to load, so exit quickly
                return;
            }
            if (result == null) {
                switch (progress) {
                case 0:
                    mHandler.progress(true);
                    mHandler.loadContentUri("file:///android_asset/loading.html");
                    break;
                case 100:
                    mWaitForLoadMessageId = -1;
                    mHandler.progress(false);
                    // reload UI and reload everything else too
                    // pass false to LoadMessageTask to prevent looping here
                    cancelAllTasks();
                    mLoadMessageTask = new LoadMessageTask(mMessageId, false);
                    mLoadMessageTask.execute();
                    break;
                default:
                    // do nothing - we don't have a progress bar at this time
                    break;
                }
            } else {
                mWaitForLoadMessageId = -1;
                mHandler.progress(false);
                mHandler.networkError();
                mHandler.loadContentUri("file:///android_asset/empty.html");
            }
        }

        public void loadAttachmentCallback(MessagingException result, long messageId, long attachmentId, int progress) {

            if (messageId == MessageView.this.mMessageId) {
                if (result == null) {
                    switch (progress) {
                    case 0:
                        mHandler.setAttachmentsEnabled(false);
                        mHandler.attachmentProgress(true);
                        mHandler.fetchingAttachment();
                        break;
                    case 100:
                        mHandler.setAttachmentsEnabled(true);
                        mHandler.attachmentProgress(false);
                        updateAttachmentThumbnail(attachmentId);
                        mHandler.finishLoadAttachment(attachmentId);
                        break;
                    default:
                        // do nothing - we don't have a progress bar at this
                        // time
                        break;
                    }
                } else {
                    mHandler.setAttachmentsEnabled(true);
                    mHandler.attachmentProgress(false);
                    mHandler.networkError();
                }
            }
        }

        public void updateMailboxCallback(MessagingException result, long accountId, long mailboxId, int progress,
                int numNewMessages) {
            if (result != null || progress == 100) {
                Email.updateMailboxRefreshTime(mailboxId);
            }
        }

        public void updateMailboxListCallback(MessagingException result, long accountId, int progress) {
        }

        public void serviceCheckMailCallback(MessagingException result, long accountId, long mailboxId, int progress,
                long tag) {
        }

        public void sendMailCallback(MessagingException result, long accountId, long messageId, int progress) {
            if (progress == 100) {
                mHandler.sendMessageToast(true);
            }
        }
    }

    // @Override
    // public void loadMessageForViewBodyAvailable(Account account, String
    // folder,
    // String uid, com.android.email.mail.Message message) {
    // MessageView.this.mOldMessage = message;
    // try {
    // Part part = MimeUtility.findFirstPartByMimeType(mOldMessage,
    // "text/html");
    // if (part == null) {
    // part = MimeUtility.findFirstPartByMimeType(mOldMessage, "text/plain");
    // }
    // if (part != null) {
    // String text = MimeUtility.getTextFromPart(part);
    // if (part.getMimeType().equalsIgnoreCase("text/html")) {
    // text = EmailHtmlUtil.resolveInlineImage(
    // getContentResolver(), mAccount.mId, text, mOldMessage, 0);
    // } else {
    // // And also escape special character, such as "<>&",
    // // to HTML escape sequence.
    // text = EmailHtmlUtil.escapeCharacterToDisplay(text);

    // /*
    // * Linkify the plain text and convert it to HTML by replacing
    // * \r?\n with <br> and adding a html/body wrapper.
    // */
    // StringBuffer sb = new StringBuffer("<html><body>");
    // if (text != null) {
    // Matcher m = Patterns.WEB_URL.matcher(text);
    // while (m.find()) {
    // int start = m.start();
    // /*
    // * WEB_URL_PATTERN may match domain part of email address. To detect
    // * this false match, the character just before the matched string
    // * should not be '@'.
    // */
    // if (start == 0 || text.charAt(start - 1) != '@') {
    // String url = m.group();
    // Matcher proto = WEB_URL_PROTOCOL.matcher(url);
    // String link;
    // if (proto.find()) {
    // // Work around to force URL protocol part be lower case,
    // // since WebView could follow only lower case protocol link.
    // link = proto.group().toLowerCase()
    // + url.substring(proto.end());
    // } else {
    // // Patterns.WEB_URL matches URL without protocol part,
    // // so added default protocol to link.
    // link = "http://" + url;
    // }
    // String href = String.format("<a href=\"%s\">%s</a>", link, url);
    // m.appendReplacement(sb, href);
    // }
    // else {
    // m.appendReplacement(sb, "$0");
    // }
    // }
    // m.appendTail(sb);
    // }
    // sb.append("</body></html>");
    // text = sb.toString();
    // }

    // /*
    // * TODO consider how to get background images and a million other things
    // * that HTML allows.
    // */
    // // Check if text contains img tag.
    // if (IMG_TAG_START_REGEX.matcher(text).find()) {
    // mHandler.showShowPictures(true);
    // }

    // loadMessageContentText(text);
    // }
    // else {
    // loadMessageContentUrl("file:///android_asset/empty.html");
    // }
    // // renderAttachments(mOldMessage, 0);
    // }
    // catch (Exception e) {
    // if (Email.LOGD) {
    // Log.v(Email.LOG_TAG, "loadMessageForViewBodyAvailable", e);
    // }
    // }
    // }

    /**
     * Back in the UI thread, handle the final steps of downloading an
     * attachment (view or save).
     * 
     * @param attachmentId
     *            the attachment that was just downloaded
     */

    private void doFinishLoadAttachment(long attachmentId) {
        // If the result does't line up, just skip it - we handle one at a time.
        if (attachmentId != mLoadAttachmentId) {
            return;
        }
        Attachment attachment = Attachment.restoreAttachmentWithId(MessageView.this, attachmentId);

        Uri attachmentUri = AttachmentProvider.getAttachmentUri(mAccountId, attachment.mId);
        Uri contentUri = AttachmentProvider.resolveAttachmentIdToContentUri(getContentResolver(), attachmentUri);

        if (mLoadAttachmentSave) {
            attachmentTempName = attachment.mFileName;
            attachmentTempUri = contentUri;

            String requestData = "";
            AttachmentBrowser.actionSaveAttachment(MessageView.this, attachment.mFileName, requestData,
                    REQUEST_CODE_SAVE_ATTACHMENT);
        } else {
            try {
                if (attachment.mFileName.endsWith(".amr")) {
                    SoundPlayer.actionSoundPlayer(this, contentUri);
                } else {
                    Logs.d(Logs.LOG_TAG, "contentUri :" + contentUri.toString());

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(contentUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(intent);
                }
            } catch (ActivityNotFoundException e) {
                mHandler.attachmentViewError();
                Logs.e(Logs.LOG_MessageView, "ActivityNotFoundException :");
                // TODO: Add a proper warning message (and lots of upstream
                // cleanup to prevent
                // it from happening) in the next release.
            }
        }
    }

    private String attachmentTempName;
    private Uri attachmentTempUri;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data == null) {
            return;
        }
        if (RESULT_OK == resultCode) {
            switch (requestCode) {
            case REQUEST_CODE_SAVE_ATTACHMENT:
                File saveFoulderPath = new File(data.getStringExtra(EXTRA_SAVE_ATTACHMENT));
                try {
                    File file = new File(saveFoulderPath, attachmentTempName);
                    boolean isReplace = file.exists();
                    InputStream in = null;
                    // 本地写附件需要判断

                    if ("file".equals(attachmentTempUri.getScheme())) {
                        File uriFile = new File(attachmentTempUri.getPath());
                        in = new FileInputStream(uriFile);
                    } else {
                        in = getContentResolver().openInputStream(attachmentTempUri);
                    }

                    OutputStream out = new FileOutputStream(file);
                    IOUtils.copy(in, out);
                    out.flush();
                    out.close();
                    in.close();

                    // new MediaScannerNotifier(MessageView.this, file,
                    // mHandler);
                } catch (IOException ioe) {
                    Toast.makeText(MessageView.this, getString(R.string.message_view_status_attachment_not_saved),
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }

    }

    /**
     * This notifier is created after an attachment completes downloaded. It
     * attaches to the media scanner and waits to handle the completion of the
     * scan. At that point it tries to start an ACTION_VIEW activity for the
     * attachment.
     */
    private static class MediaScannerNotifier implements MediaScannerConnectionClient {
        private Context mContext;
        private MediaScannerConnection mConnection;
        private File mFile;
        private MessageViewHandler mHandler;

        public MediaScannerNotifier(Context context, File file, MessageViewHandler handler) {
            mContext = context;
            mFile = file;
            mHandler = handler;
            mConnection = new MediaScannerConnection(context, this);
            mConnection.connect();
        }

        public void onMediaScannerConnected() {
            mConnection.scanFile(mFile.getAbsolutePath(), null);
        }

        public void onScanCompleted(String path, Uri uri) {
            try {
                if (uri != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    mContext.startActivity(intent);
                }
            } catch (ActivityNotFoundException e) {
                mHandler.attachmentViewError();
                // TODO: Add a proper warning message (and lots of upstream
                // cleanup to prevent
                // it from happening) in the next release.
            } finally {
                mConnection.disconnect();
                mContext = null;
                mHandler = null;
            }
        }
    }

    // =================PopMenu=============
    private void initPopupMenu() {
        LinearLayout mPopupMenuLayout = new LinearLayout(this);
        mPopupMenuLayout.setOrientation(LinearLayout.VERTICAL);
        // option title
        GridView mTitleGridView = new GridView(this);
        mTitleGridView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        mTitleGridView.setSelector(R.color.background_cachhint);
        mTitleGridView.setNumColumns(mOptionTitle.length);// the number of
        // option title.
        mTitleGridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        mTitleGridView.setVerticalSpacing(1);
        mTitleGridView.setHorizontalSpacing(1);
        mTitleGridView.setGravity(Gravity.CENTER);
        MenuTitleAdapter mTitleAdapter = new MenuTitleAdapter(this, mOptionTitle, 16, 0xFFFFFFFF);
        mTitleGridView.setAdapter(mTitleAdapter);

        mTitleGridView.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                onChangeItem(arg1, arg2);
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mTitleGridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                onChangeItem(arg1, arg2);
            }
        });

        // child Option
        mMenuContentGridView = new GridView(this);
        mMenuContentGridView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
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
        mMenuContentGridView.setAdapter(getMenuAdapter(mOptionTextArray1, mOptionImageArray1));
        mMenuContentGridView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int childOptionIndex, long arg3) {
                switch (mTitleMenuIndex) {
                case 0:// 常用
                    if (childOptionIndex == 0) {
                        onReplyAll();
                    }
                    if (childOptionIndex == 1) {
                        onReply();
                    }
                    if (childOptionIndex == 2) {
                        onForward();
                    }
                    break;
                case 1:// 设置
                    if (childOptionIndex == 0) {
                        onMarkAsRead(false);
                        finish();
                    }
                    if (childOptionIndex == 1) {
                        onDelete();
                    }
                    break;

                }
                popup.dismiss();
            }
        });
        mPopupMenuLayout.addView(mTitleGridView);
        mPopupMenuLayout.addView(mMenuContentGridView);

        popup = new PopupWindow(mPopupMenuLayout, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        popup.setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_bg));// 设置menu菜单背景
        popup.setFocusable(true);// menu菜单获得焦点 如果没有获得焦点menu菜单中的控件事件无法响应
        popup.update();
        // 设置默认项
        mTitleOption1 = (TextView) mTitleGridView.getItemAtPosition(0);
        mTitleOption1.setBackgroundColor(0x00);
    }

    private SimpleAdapter getMenuAdapter(String[] menuNameArray, int[] imageResourceArray) {
        ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
        for (int i = 0; i < menuNameArray.length; i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("itemImage", imageResourceArray[i]);
            map.put("itemText", menuNameArray[i]);
            data.add(map);
        }
        SimpleAdapter simperAdapter = new SimpleAdapter(this, data, R.layout.item_menu, new String[] { "itemImage",
                "itemText" }, new int[] { R.id.item_image, R.id.item_text });
        return simperAdapter;
    }

    private void onChangeItem(View mView, int mIndex) {
        mTitleMenuIndex = mIndex;
        switch (mTitleMenuIndex) {
        case 0:
            mTitleOption1 = (TextView) mView;
            mTitleOption1.setBackgroundColor(0x00);
            if (mTitleOption2 != null)
                mTitleOption2.setBackgroundResource(R.drawable.toolbar_menu_release);
            if (mTitleOption3 != null)
                mTitleOption3.setBackgroundResource(R.drawable.toolbar_menu_release);
            mMenuContentGridView.setNumColumns(mOptionTextArray1.length);
            mMenuContentGridView.setAdapter(getMenuAdapter(mOptionTextArray1, mOptionImageArray1));
            break;
        case 1:
            mTitleOption2 = (TextView) mView;
            mTitleOption2.setBackgroundColor(0x00);
            if (mTitleOption1 != null)
                mTitleOption1.setBackgroundResource(R.drawable.toolbar_menu_release);
            if (mTitleOption3 != null)
                mTitleOption3.setBackgroundResource(R.drawable.toolbar_menu_release);
            mMenuContentGridView.setNumColumns(mOptionTextArraySet.length);
            mMenuContentGridView.setAdapter(getMenuAdapter(mOptionTextArraySet, mOptionImageArrayset));
            break;
        }
    }

    /*
     * copy all content
     */
    // private void setWebViewCopy() {
    // mMessageContentView.getSettings().setJavaScriptEnabled(true);
    //
    // mMessageContentView.addJavascriptInterface(classBeBindedToJS,
    // "classNameBeExposedInJs");
    //
    // mMessageContentView.setWebViewClient(new WebViewClient() {
    // // 页面加载完成后执行
    // @Override
    // public void onPageFinished(WebView webView, String url) {
    // // 首先webView会执行这一段js
    // //
    // document.getElementsByTagName('html')[0].innerHTML。含义即为取得页面中html标记的innerHTML，及网页主要内容；
    // webView
    // .loadUrl("javascript:classNameBeExposedInJs.showHtml(document.body.innerHTML);");
    //
    // }
    //
    // });
    // }
    public class ClassBeBindedToJS {
        public String bodyString = "";

        public String getBodyString() {
            return bodyString;
        }

        public void setBodyString(String bodyString) {
            this.bodyString = bodyString;
        }

        public void showHtml(String html) {
            setBodyString(getTxtWithoutHTMLElement(html));
        }
    };

    /*
     * 过虑HTML元素
     */
    public static String getTxtWithoutHTMLElement(String element) {
        if (null == element || "".equals(element.trim())) {
            return element;
        }

        Pattern pattern = Pattern.compile("<[^<|^>]*>");
        Matcher matcher = pattern.matcher(element);
        StringBuffer txt = new StringBuffer();
        while (matcher.find()) {
            String group = matcher.group();
            if (group.matches("<[//s]*>")) {
                matcher.appendReplacement(txt, group);
            } else {
                matcher.appendReplacement(txt, "");
            }
        }
        matcher.appendTail(txt);
        // repaceEntities(txt,"&","&");
        // repaceEntities(txt,"<","<");
        // repaceEntities(txt,">",">");
        // repaceEntities(txt,""","/"");
        // repaceEntities(txt," ","");

        return txt.toString();
    }

    public class MenuTitleAdapter extends BaseAdapter {
        private Context mContext;
        private int fontColor;
        private TextView[] title;

        public MenuTitleAdapter(Context context, String[] titles, int fontSize, int color) {
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
        mOptionTitle = new String[] { getString(R.string.popmenu_common), getString(R.string.popmenu_other) };
        mOptionTextArray1 = new String[] { getString(R.string.reply_all_action), getString(R.string.reply_action),
                getString(R.string.forward_action) };
        mOptionImageArray1 = new int[] { R.drawable.ic_menu_reply_all, R.drawable.ic_menu_reply,
                R.drawable.ic_menu_select_all };

        mOptionTextArraySet = new String[] { getString(R.string.mark_as_unread_action),
                getString(R.string.delete_action) };
        mOptionImageArrayset = new int[] { R.drawable.ic_menu_reply, R.drawable.ic_menu_select_all };
    }
    // =================PopMenu=============
}
