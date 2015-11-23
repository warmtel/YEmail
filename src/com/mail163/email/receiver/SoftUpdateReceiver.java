package com.mail163.email.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.WindowManager;

import com.mail163.email.Email;
import com.mail163.email.Email.Global;
import com.mail163.email.activity.DialogsActivity;
import com.mail163.email.activity.SendImageView;

public class SoftUpdateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();  
		if (Global.ACTION_SOFTUDPAE_RECEIVER.equals(action)) {
			startDialog(context);
		}
		if (Global.ACTION_INSTALL_SOFT.equals(action)) {
			if (!Email.downFlag) {
				return;
			}
			openFile(context, intent.getData());

		}
		if(action.equals(Global.ACTION_SENDMESSAGE_BROADCASET)){
			SendImageView SENDVIEWS = SendImageView.getInstance(context);
			SENDVIEWS.setSendImageViewNull();
			Global.send_view_animl_flag = false;
        	WindowManager wm = (WindowManager)context.getApplicationContext().getSystemService(context.WINDOW_SERVICE);
        	try{
        		wm.removeView(SENDVIEWS);
        	}catch(IllegalArgumentException iie){
        		iie.printStackTrace();
        	}
        }
	}

	public void startDialog(Context context) {
		Intent mIntent = new Intent();
		mIntent.setClass(context, DialogsActivity.class);
		mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(mIntent);
	}

	private void openFile(Context context, Uri uri) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(uri, "application/vnd.android.package-archive");
		context.startActivity(intent);
	}
}
