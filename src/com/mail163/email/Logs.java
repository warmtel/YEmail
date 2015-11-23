/**
 * @author: zhous
 * @version: v1.0
 * @time: 2010-10-26
 */
package com.mail163.email;

import android.util.Log;

public class Logs {
	public static final String LOG_TAG = "mail163";
	public static final String LOG_MessageView = "MessageView";
	public static final String LOG_TAG_MAIL_LIST = "MailList";
	public static final String LOG_TAG_MAIL_FOLDER_LIST = "MailFolderList";
	public static final String LOG_TAG_MESSAGE_COMPOSE = "MessageCompose";
	public static final String LOG_TAG_MESSAGE_VIEW = "MessageView";
	
	public static void i(String tag, String msg) {
		if(Email.DEBUG_LOGS){
			Log.i(tag, msg);
		}
	}

	public static void e(String tag, String msg) {
		if(Email.DEBUG_LOGS){
			Log.e(tag, msg);
		}
	}
	public static void d(String tag, String msg) {
		if(Email.DEBUG_LOGS){
			Log.d(tag, msg);
		}
	}
	public static void v(String tag, String msg) {
		if(Email.DEBUG_LOGS){
			Log.v(tag, msg);
		}
	}
	public static void print(String tag, String msg) {
		if(Email.DEBUG_LOGS){
			System.out.println("["+tag+"]  "+msg);
		}
	}
}
