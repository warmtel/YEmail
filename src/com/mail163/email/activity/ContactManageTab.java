package com.mail163.email.activity;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;

import com.mail163.email.Email.Global;
import com.mail163.email.R;

public class ContactManageTab extends TabActivity implements
        OnTabChangeListener {
    public static final int TAB_INDEX_MOBILE = 0;
    public static final int TAB_INDEX_EMAIL = 1;
    private TabHost tabHost;
    public Intent mPrecIntent;

    public static void actionContactManageTabView(Activity activity,
            int requestCode) {
        Intent i = new Intent(activity, ContactManageTab.class);
        activity.startActivityForResult(i, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Global.skinName.contains(Global.whiteSkin)) {
            setTheme(R.style.Default);
        } else {
            setTheme(R.style.XTheme);
        }
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.contacts_people));

        tabHost = getTabHost();
        
        tabHost.addTab(tabHost.newTabSpec(
                getResources().getString(R.string.email_contact)).setIndicator(
                getResources().getString(R.string.email_contact),
				getResources().getDrawable(R.drawable.ic_button_contacts)).setContent(
                new Intent(ContactManageTab.this, ContactsEmailOwner.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)));
        tabHost.addTab(tabHost.newTabSpec(
                getResources().getString(R.string.system_contact)).setIndicator(
                getResources().getString(R.string.system_contact),
				getResources().getDrawable(R.drawable.ic_button_contacts)).setContent(
                new Intent(ContactManageTab.this, ContactsEmail.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)));
    }


    @Override
    public void onTabChanged(String tabId) {

    }

}