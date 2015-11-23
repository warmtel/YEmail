package com.mail163.email.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.mail163.email.R;
import com.mail163.email.activity.Welcome;

public class YEmailWidget extends AppWidgetProvider {
	public RemoteViews views;

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		Log.v("TAG", "onDeleted ");
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		Log.v("TAG", "onDisabled ");
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		Log.v("TAG", "onEnabled ");
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		Log.v("TAG", "onReceive ");
		if (views == null) {
			views = new RemoteViews(context.getPackageName(),R.layout.yimail_widget);
		}
		
		AppWidgetManager appWidgetManger = AppWidgetManager.getInstance(context);
		int[] appIds = appWidgetManger.getAppWidgetIds(new ComponentName(context, YEmailWidget.class));
		appWidgetManger.updateAppWidget(appIds, views);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Log.v("TAG", "onUpdate ");
		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			views = new RemoteViews(context.getPackageName(),R.layout.yimail_widget);
			Intent intent = new Intent(context, Welcome.class);
	        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

	        views.setOnClickPendingIntent(R.id.ywidget_imageview, pendingIntent);

			// 告诉 AppWidgetManager 在当前App Widget上 执行一次更新
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
	
}
