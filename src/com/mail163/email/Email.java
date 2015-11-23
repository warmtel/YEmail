package com.mail163.email;

import com.mail163.email.activity.AccountShortcutPicker;
import com.mail163.email.activity.Debug;
import com.mail163.email.activity.MessageCompose;
import com.mail163.email.provider.EmailContent;
import com.mail163.email.service.MailService;
import com.tencent.mm.sdk.openapi.IWXAPI;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.File;
import java.util.HashMap;

public class Email extends Application {
    // IWXAPI 是第三方app和微信通信的openapi接口
    public IWXAPI api;
//	public static String softUri = "http://192.168.8.75:89/softupdate/ymail_update.xml";
//	public static String softUri = "http://www.warmtel.com/ymail_update_test.xml";
	public static String softUri = "http://www.warmtel.com/ymail_update.xml";
	public static String systemNotice = "http://www.warmtel.com/system_notice.xml";
	public static String skinDownUri = "http://www.warmtel.com/skinmange/skin_down_page.xml";
	//==============正式发包修改如下变量=============
	public static final String EMULATOR = "sdk";//"sdk|testsdk";
	public static final boolean MOBILEMESSAGEFLAG = true;//true|false;
    public static boolean DEBUG = false;//false|true
    public static boolean DEBUG_LOGS = false;//false|true
    //==============正式发包修改如下变量=============
	/**软件更新下载完成*/
	public static boolean downFlag = false;
	public static class  Global {
		public final static String tempFile = "/yimail_tempres";
		public static String noticeOpen = "open"; //通知打开
		public static String noticeClose = "close"; //通知关闭
		public static String ACTION_SOFTUDPAE_RECEIVER = "com.yimail.action.SOFTUPDATE";
		public static final String ACTION_INSTALL_SOFT = "com.mail163.email.receiver.ACTION_INSTALL_SOFT";
		public static final String ACTION_MUSIC_SERVICE_BROADCASTK = "com.mail163.service.MUSIC_SERVICE_BROADCASTKS";
		public static final String ACTION_MUSIC_SERVICE = "com.mail163.service.MUSIC_SERVICE_SERVICEKS";
		public static final String ACTION_SENDMESSAGE_BROADCASET = "com.mail163.send_success_broadcast";
		//版本信息
		public static String localVersionName;
	    public static int localVersion = 0;
	    public static int serverVersion = 0;
	    public static long apkSize;
	    public static int addAccountCount = 20;
	    public static String smsMobile;
	    public static String sendMessage = "(来自忆邮软件: www.warmtel.com)";
	    public static String downloadDir = "YiMail";
	    public static String whiteSkin = "白";
	    public static String skinName = "";
	    public static String softUpdateUri = "";
	    public static String softUpdateContent = "";
	    public static String rootDir = "/YiMail/";
	    
	    public static int set_store = 0; //0表示存储设置默认为保存至手机存储
	    public static int set_sdcard = 1; //1表示存储设置为保存至SD卡
	    public static int set_mobile = 0; //0表示存储设置为保存至手机存储
	    public static String set_store_name = "store_set";
	    
	    public static int set_sdSize_0 = 0; //表示sdcard存储大小无限制
	    public static int set_sdSize_10 = 1; //表示sdcard存储大小10M
	    public static int set_sdsize_20 = 2; //表示sdcard存储大小20M
	    public static int set_sdsize_30 = 3; //表示sdcard存储大小30M
	    public static int set_sdSize = set_sdSize_0; //表示sdcard存储大小默认30M
	    public static String set_sd_name = "Sd_set";
	   
	    public final static int set_streamSize_0 = 0; //表示stream存储大小无限制
	    public final static int set_streamSize_10 = 1; //表示stream存储大小10M
	    public final static int set_streamsize_20 = 2; //表示stream存储大小20M
	    public final static int set_streamsize_30 = 3; //表示stream存储大小30M
	    public static int set_streamSize = set_streamSize_0; //表示stream存储大小默认30M
	    public static String set_stream_name = "Stream_set";
	    
	    public static int set_download_select_title = 0; //表示取标题
	    public static int set_download_select_body = 1; //表示取标题和正文
	    public static int set_download_select = set_download_select_body; //表示取标题和正文
	    public static String set_download_select_name = "download_select_set"; 
	    
	    public static int set_download_count = 20; //默认每次下载数
	    public static String set_download_count_name = "download_count_set"; 
	    
	    public static String set_sysch_delete_name = "download_sysch_delete_set"; 
	    public static boolean set_sysch_delete = true;//同步删除邮件 
	    
	    public static String set_scret_send_name = "scret_send_set"; 
	    public static boolean set_scret_send = true;//邮件密送设置
	    
	    public static int set_softUpdate = 0; //表示软件升级开关,默认开
	    public static String set_softUpdate_name = "softupdate_set"; 
	    
	    public static int set_shortcut = 1; //表示快捷方式开关 ,默认关
	    public static String set_shortcut_name = "shortcut_set"; 
	    
	    public static int set_security = 1; //表示IMSI安全开关 ,默认关
	    public static String set_security_name = "security_set"; 
	    
	    public static String set_security_password = "";
	    public static String set_security_password_name = "set_security_password_name"; 
	    
	    public static boolean notice_Flag = true; //控制通知在每次登录时检测
	    public static String notice_version_name = "notice_set";
	    public static long notice_version = 0; 
	    
	    public static int set_music_background_value = 0; //表示背景音乐开关,默认开
	    public static String set_music_background = "music_background_set";
	    public static String set_music_dir = "music_background_dir_set"; //music background
	    public static String set_music_name = "music_background_name_set"; //musicName
	    
	    /**控制快捷方式只在第一次进入程序时检查*/
	    public static boolean shortCutFlag = true;
	    /**控制软件更新只在第一次进入程序时检查*/
	    public static boolean softUpdateFlag = true;
	    public static String Save_IMSI = "Save_IMSI_set";
	    public static boolean contacts_add_notify;
	    public static boolean contacts_add_flag;
	    
	    public static String SKIN_NAME = "";//"深蓝海洋";
	    public static boolean send_view_animl_flag = true;
	    
	    public static String set_exit_noprompt = "noprompt_set";
	    
	    public static Drawable backDrawable;
	    
	    public static boolean ISFORWORD;
	    
	    public static String activityType = "accountFolderList";
	    
	}
	public static final String STORESETMESSAGE = "StoreSetMessage";
    public static final String LOG_TAG = "Email";
    public static final String SERVICE_MOBILE_TEST = "5556";
    public static final String SERVICE_MOBILE = "5554";
  

    /**
     * If this is enabled than logging that normally hides sensitive information
     * like passwords will show that information.
     */
    public static boolean DEBUG_SENSITIVE = false;

    /**
     * Set this to 'true' to enable as much Email logging as possible.
     * Do not check-in with it set to 'true'!
     */
    public static final boolean LOGD = false;

    /**
     * The MIME type(s) of attachments we're willing to send via attachments.
     *
     * Any attachments may be added via Intents with Intent.ACTION_SEND or ACTION_SEND_MULTIPLE.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_SEND_INTENT_TYPES = new String[] {
        "*/*",
    };

    /**
     * The MIME type(s) of attachments we're willing to send from the internal UI.
     *
     * NOTE:  At the moment it is not possible to open a chooser with a list of filter types, so
     * the chooser is only opened with the first item in the list.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_SEND_UI_TYPES = new String[] {
//        "image/*",       //图片文件
//        "video/*",       //视频文件
//        "application/*", //音乐文件
//        "text/*",         //文本文件无法使用 txt/*  
    	  "*/*"
    };

    /**
     * The MIME type(s) of attachments we're willing to view.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_VIEW_TYPES = new String[] {
        "*/*",
    };
    public static final String[] ACCEPTABLE_ATTACHMENT_IMAGE_VIEW_TYPES = new String[] {
    	"image/*",       //图片文件
    };
    /**
     * The MIME type(s) of attachments we're not willing to view.
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_VIEW_TYPES = new String[] {
    };

    /**
     * The MIME type(s) of attachments we're willing to download to SD.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[] {
//        "image/*",
    	 "*/*",
    };

    /**
     * The MIME type(s) of attachments we're not willing to download to SD.
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[] {
    };

    /**
     * Specifies how many messages will be shown in a folder by default. This number is set
     * on each new folder and can be incremented with "Load more messages..." by the
     * VISIBLE_LIMIT_INCREMENT
     */
    public static  int VISIBLE_LIMIT_DEFAULT = 20;

    /**
     * Number of additional messages to load when a user selects "Load more messages..."
     */
    public static  int VISIBLE_LIMIT_INCREMENT = 20;

    /**
     * The maximum size of an attachment we're willing to download (either View or Save)
     * Attachments that are base64 encoded (most) will be about 1.375x their actual size
     * so we should probably factor that in. A 5MB attachment will generally be around
     * 6.8MB downloaded but only 5MB saved.
     */
    public static final int MAX_ATTACHMENT_DOWNLOAD_SIZE = (5 * 1024 * 1024);

    /**
     * The maximum size of an attachment we're willing to upload (measured as stored on disk).
     * Attachments that are base64 encoded (most) will be about 1.375x their actual size
     * so we should probably factor that in. A 5MB attachment will generally be around
     * 6.8MB uploaded.
     */
    public static final int MAX_ATTACHMENT_UPLOAD_SIZE = (5 * 1024 * 1024);

    private static HashMap<Long, Long> sMailboxSyncTimes = new HashMap<Long, Long>();
    private static final long UPDATE_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;

    /**
     * This is used to force stacked UI to return to the "welcome" screen any time we change
     * the accounts list (e.g. deleting accounts in the Account Manager preferences.)
     */
    private static boolean sAccountsChangedNotification = false;

    public static final String EXCHANGE_ACCOUNT_MANAGER_TYPE = "com.android.exchange";

    // The color chip resources and the RGB color values in the array below must be kept in sync
    private static final int[] ACCOUNT_COLOR_CHIP_RES_IDS = new int[] {
        R.drawable.appointment_indicator_leftside_1,
        R.drawable.appointment_indicator_leftside_2,
        R.drawable.appointment_indicator_leftside_3,
        R.drawable.appointment_indicator_leftside_4,
        R.drawable.appointment_indicator_leftside_5,
        R.drawable.appointment_indicator_leftside_6,
        R.drawable.appointment_indicator_leftside_7,
        R.drawable.appointment_indicator_leftside_8,
        R.drawable.appointment_indicator_leftside_9,
    };

    private static final int[] ACCOUNT_COLOR_CHIP_RGBS = new int[] {
        0x71aea7,
        0x621919,
        0x18462f,
        0xbf8e52,
        0x001f79,
        0xa8afc2,
        0x6b64c4,
        0x738359,
        0x9d50a4,
    };

    private static File sTempDirectory;

    /* package for testing */ static int getColorIndexFromAccountId(long accountId) {
        // Account id is 1-based, so - 1.
        // Use abs so that it won't possibly return negative.
        return Math.abs((int) (accountId - 1) % ACCOUNT_COLOR_CHIP_RES_IDS.length);
    }

    public static int getAccountColorResourceId(long accountId) {
        return ACCOUNT_COLOR_CHIP_RES_IDS[getColorIndexFromAccountId(accountId)];
    }

    public static int getAccountColor(long accountId) {
        return ACCOUNT_COLOR_CHIP_RGBS[getColorIndexFromAccountId(accountId)];
    }

    public static void setTempDirectory(Context context) {
        sTempDirectory = context.getCacheDir();
    }

    public static File getTempDirectory() {
        if (sTempDirectory == null) {
            throw new RuntimeException(
                    "TempDirectory not set.  " +
                    "If in a unit test, call Email.setTempDirectory(context) in setUp().");
        }
        return sTempDirectory;
    }

    /**
     * Called throughout the application when the number of accounts has changed. This method
     * enables or disables the Compose activity, the boot receiver and the service based on
     * whether any accounts are configured.   Returns true if there are any accounts configured.
     */
    public static boolean setServicesEnabled(Context context) {
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    EmailContent.Account.CONTENT_URI,
                    EmailContent.Account.ID_PROJECTION,
                    null, null, null);
            boolean enable = c.getCount() > 0;
            setServicesEnabled(context, enable);
            return enable;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static void setServicesEnabled(Context context, boolean enabled) {
        PackageManager pm = context.getPackageManager();
        if (!enabled && pm.getComponentEnabledSetting(new ComponentName(context, MailService.class)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            /*
             * If no accounts now exist but the service is still enabled we're about to disable it
             * so we'll reschedule to kill off any existing alarms.
             */
            MailService.actionReschedule(context);
        }
        pm.setComponentEnabledSetting(
                new ComponentName(context, MessageCompose.class),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(
                new ComponentName(context, AccountShortcutPicker.class),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(
                new ComponentName(context, MailService.class),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        if (enabled && pm.getComponentEnabledSetting(new ComponentName(context, MailService.class)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            /*
             * And now if accounts do exist then we've just enabled the service and we want to
             * schedule alarms for the new accounts.
             */
            MailService.actionReschedule(context);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Preferences prefs = Preferences.getPreferences(this);
        DEBUG = prefs.getEnableDebugLogging();
        DEBUG_SENSITIVE = prefs.getEnableSensitiveLogging();
        setTempDirectory(this);

        // Reset all accounts to default visible window
        Controller.getInstance(this).resetVisibleLimits();

        // Enable logging in the EAS service, so it starts up as early as possible.
        Debug.updateLoggingFlags(this);
    }

    /**
     * Internal, utility method for logging.
     * The calls to log() must be guarded with "if (Email.LOGD)" for performance reasons.
     */
    public static void log(String message) {
        Log.d(LOG_TAG, message);
    }

    /**
     * Update the time when the mailbox is refreshed
     * @param mailboxId mailbox which need to be updated
     */
    public static void updateMailboxRefreshTime(long mailboxId) {
        synchronized (sMailboxSyncTimes) {
            sMailboxSyncTimes.put(mailboxId, System.currentTimeMillis());
        }
    }

    /**
     * Check if the mailbox is need to be refreshed
     * @param mailboxId mailbox checked the need of refreshing
     * @return the need of refreshing
     */
    public static boolean mailboxRequiresRefresh(long mailboxId) {
        synchronized (sMailboxSyncTimes) {
            return
                !sMailboxSyncTimes.containsKey(mailboxId)
                || (System.currentTimeMillis() - sMailboxSyncTimes.get(mailboxId)
                        > UPDATE_INTERVAL);
        }
    }

    /**
     * Called by the accounts reconciler to notify that accounts have changed, or by  "Welcome"
     * to clear the flag.
     * @param setFlag true to set the notification flag, false to clear it
     */
    public static synchronized void setNotifyUiAccountsChanged(boolean setFlag) {
        sAccountsChangedNotification = setFlag;
    }

    /**
     * Called from activity onResume() functions to check for an accounts-changed condition, at
     * which point they should finish() and jump to the Welcome activity.
     */
    public static synchronized boolean getNotifyUiAccountsChanged() {
        return sAccountsChangedNotification;
    }

}
