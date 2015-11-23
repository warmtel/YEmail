/**
 * @author: zhous
 * @version: v1.0
 * @time: 2011-5-5
 */
package com.mail163.email.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.mail163.email.Logs;
import com.mail163.email.Email.Global;
import com.mail163.email.service.MailService;

public class SMSReceiver extends BroadcastReceiver {
	public static final String TAG = "SMSReceiver";
	private static final String DIVIDE = "#";
    private static final String CLRHEAD = "//android:";
    private static final String CLRBODY = "clr";
	/* pushmail 号码 */
	public static final String number = Global.smsMobile;
	/* 声明静态字符串，并使用android.provider.Telephony.SMS_RECEIVED 作为Action为短信的依据 */
	private static final String mACTION = "android.provider.Telephony.SMS_RECEIVED";

	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent.getAction().equals(mACTION)) {
 
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				Object[] obj = (Object[]) bundle.get("pdus");

				SmsMessage[] message = new SmsMessage[obj.length];
				for (int i = 0; i < obj.length; i++) {
					message[i] = SmsMessage.createFromPdu((byte[]) obj[i]);
				}
				for (SmsMessage currentMessage : message) {
					String sender = currentMessage
							.getDisplayOriginatingAddress();
					if (!sender.equals(number)) {
//						Logs.v(TAG, "sender :" + sender+" number :"+number);
						 return;
					}
					String msgBody = currentMessage.getDisplayMessageBody();

					if (isPushMailSMS(msgBody)) {
						abortBroadcast();// 屏蔽手机系统接受短信广播
					} else {
						return;
					}
					MailService.actionClearAccount(context);
				}
			}
		}
	}

	//BODY:CLRHEAD#CLRBODY#NULL
	public boolean isPushMailSMS(String msgBody) {
//		Logs.v(TAG, "msgBody :" + msgBody);
		String[] contents = split(msgBody, DIVIDE);
		if (contents == null) {
			return false;
		}
		if (contents[0].startsWith(CLRHEAD)
				&& contents[1].startsWith(CLRBODY)) {
			return true;
		} else {
			return false;
		}
	}

	private String[] split(String str, String divide) {
		String[] contents = null;
		if (str != null) {
			int length = str.length();
			if (length > 0)
				contents = str.split(divide);
		}
		return contents;

	}
}