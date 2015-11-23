package com.mail163.email.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.mail163.email.Email;
import com.mail163.email.Logs;
import com.mail163.email.R;
import com.mail163.email.Email.Global;
import com.mail163.email.activity.MusicList;

public class MusicReceiver extends BroadcastReceiver {
	public Context context;
	@Override
	public void onReceive(Context context, Intent intent) {
		this.context = context;
		Intent it = new Intent(Global.ACTION_MUSIC_SERVICE);
		Bundle bundle = intent.getExtras();
		it.putExtras(bundle);
		if (bundle != null) {
			int op = bundle.getInt("op");
			if (op == 0) {
				context.stopService(it);
			
				clearNotify();
				
			} else if (op == 1) {
				context.startService(it);
					showNotify();
			}
		}
	}
	private void showNotify() {
		String musicName = getGroundMusicName();
		NotificationManager mNM = (NotificationManager) context.getSystemService("notification");
		Notification notification = new Notification(R.drawable.play_state, musicName,
				System.currentTimeMillis());
		// notification.flags = Notification.FLAG_ONGOING_EVENT;
		notification.flags = Notification.FLAG_NO_CLEAR;

		Intent intent = new Intent(context, MusicList.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		notification.setLatestEventInfo(context, context.getString(R.string.app_name), musicName, contentIntent);

		mNM.notify(R.string.app_name, notification);
	}
	private void clearNotify(){
		NotificationManager mNM = (NotificationManager) context.getSystemService("notification");
		mNM.cancel(R.string.app_name);
	}
    public String getGroundMusicName(){
    	SharedPreferences sharedata = context.getSharedPreferences(
				Email.STORESETMESSAGE, 0);
		String musicName = sharedata.getString(Global.set_music_name, "");
		return musicName;

    }
}
