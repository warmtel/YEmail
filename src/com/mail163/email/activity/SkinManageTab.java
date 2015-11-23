package com.mail163.email.activity;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

import com.mail163.email.R;
import com.mail163.email.Email.Global;

public class SkinManageTab extends TabActivity {
	private TabHost tabHost;

	public static void actionView(Context context, Bundle extras) {
		Intent i = new Intent(context, SkinManageTab.class);
		if (extras != null) {
			i.putExtras(extras);
		}
		context.startActivity(i);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(Global.skinName.contains(Global.whiteSkin)){
			setTheme(R.style.Default);
        }else{
        	setTheme(R.style.XTheme);
        }
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.skin_manage_menu));

		tabHost = getTabHost();
		tabHost.setBackgroundColor(0xffffff);
		
		tabHost.addTab(tabHost.newTabSpec(
				getResources().getString(R.string.skin_install)).setIndicator(
				getResources().getString(R.string.skin_install),
				getResources().getDrawable(R.drawable.mi_skin)).setContent(
				new Intent(SkinManageTab.this, SkinInstallList.class)));
		tabHost.addTab(tabHost.newTabSpec(
				getResources().getString(R.string.skin_new)).setIndicator(
				getResources().getString(R.string.skin_new),
				getResources().getDrawable(R.drawable.mi_skin)).setContent(
				new Intent(SkinManageTab.this, SkinNewList.class)));
		
		
	}

}