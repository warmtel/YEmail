package com.mail163.email.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mail163.email.Email;
import com.mail163.email.R;
import com.mail163.email.Email.Global;

public class SystemSetActivity extends Activity implements OnClickListener {
	private TextView set_download_textview, settings_download_count,
			store_area, store_min_size, stream_max_size_tv;
	private RelativeLayout set_download_select, set_mail_count,
			store_area_layout, store_min_size_layout, netdata_layout;
	private RelativeLayout background_music_layout, shortcut_layout,
			soft_update_layout, securtiy_imsi_layout,set_password_layout;
	private TextView background_music_value, shortcut_value, soft_update_value,
			securtiy_imsi_value,set_password_value;
	private com.mail163.email.activity.MyCheckBox mScretSend;
	
	CharSequence[] textArray;
	CharSequence[] mail_counttextArray;
	CharSequence[] visiblelimitValues;
	int downLoadCountindex;
	CharSequence[] storeAreaArray;
	CharSequence[] storeMinAreaArray;
	CharSequence[] storeMaxAreaArray;
	CharSequence[] openCloseArray;

	public static void actionSystemSet(Context context) {
		Intent i = new Intent(context, SystemSetActivity.class);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(Global.skinName.contains(Global.whiteSkin)){
			setTheme(R.style.Default);
        }else{
        	setTheme(R.style.XTheme);
        }
		super.onCreate(savedInstanceState);
		setTitle(R.string.system_set_title);
		setContentView(R.layout.system_set_manage);

		findView();
		initTextView();
	}

	public void findView() {
		set_download_textview = (TextView) findViewById(R.id.set_download_textview);
		settings_download_count = (TextView) findViewById(R.id.settings_download_count);
		store_area = (TextView) findViewById(R.id.store_area_title);
		store_min_size = (TextView) findViewById(R.id.store_min_size);
		stream_max_size_tv = (TextView) findViewById(R.id.stream_max_size_tv);

		set_download_select = (RelativeLayout) findViewById(R.id.set_download_select);
		set_download_select.setOnClickListener(this);
		set_mail_count = (RelativeLayout) findViewById(R.id.set_mail_count);
		set_mail_count.setOnClickListener(this);

		mScretSend = (com.mail163.email.activity.MyCheckBox) findViewById(R.id.settings_secretsend_me);
		mScretSend.setChecked(Global.set_scret_send);

		store_area_layout = (RelativeLayout) findViewById(R.id.store_area_layout);
		store_area_layout.setOnClickListener(this);

		store_min_size_layout = (RelativeLayout) findViewById(R.id.store_min_size_layout);
		store_min_size_layout.setOnClickListener(this);

		netdata_layout = (RelativeLayout) findViewById(R.id.netdata_layout);
		netdata_layout.setOnClickListener(this);

		background_music_layout = (RelativeLayout) findViewById(R.id.background_music_layout);
		background_music_layout.setOnClickListener(this);
		background_music_value = (TextView) findViewById(R.id.background_music_value);

		shortcut_layout = (RelativeLayout) findViewById(R.id.shortcut_layout);
		shortcut_layout.setOnClickListener(this);
		shortcut_value = (TextView) findViewById(R.id.shortcut_value);

		soft_update_layout = (RelativeLayout) findViewById(R.id.soft_update_layout);
		soft_update_layout.setOnClickListener(this);
		soft_update_value = (TextView) findViewById(R.id.soft_update_value);

		securtiy_imsi_layout = (RelativeLayout) findViewById(R.id.securtiy_imsi_layout);
		securtiy_imsi_layout.setOnClickListener(this);
		securtiy_imsi_value = (TextView) findViewById(R.id.securtiy_imsi_value);
		
		
		set_password_layout = (RelativeLayout) findViewById(R.id.set_password_layout);
		set_password_layout.setOnClickListener(this);
		set_password_value = (TextView) findViewById(R.id.set_password_value);

	}

	public void initTextView() {
		textArray = getResources().getTextArray(
				R.array.account_settings_down_entries);
		mail_counttextArray = getResources().getTextArray(
				R.array.account_settings_down_count_entries);
		visiblelimitValues = getResources().getTextArray(
				R.array.account_settings_down_count_values);
		downLoadCountindex = getArrayIndex(visiblelimitValues, String
				.valueOf(Global.set_download_count));
		storeMinAreaArray = getResources().getTextArray(
				R.array.store_size_array);
		openCloseArray = getResources().getTextArray(R.array.soft_update_array);

		storeAreaArray = getResources().getTextArray(R.array.store_area_array);
		storeMaxAreaArray = getResources().getTextArray(
				R.array.store_size_array);
		set_download_textview.setText(textArray[Global.set_download_select]);
		settings_download_count
				.setText(mail_counttextArray[downLoadCountindex]);
		store_area.setText(storeAreaArray[Global.set_store]);
		store_min_size.setText(storeMinAreaArray[Global.set_sdSize]);
		stream_max_size_tv.setText(storeMaxAreaArray[Global.set_streamSize]);

		background_music_value.setText(openCloseArray[Global.set_music_background_value]);
		shortcut_value.setText(openCloseArray[Global.set_shortcut]);
		soft_update_value.setText(openCloseArray[Global.set_softUpdate]);
		securtiy_imsi_value.setText(openCloseArray[Global.set_security]);
		
		
		if(Global.set_security_password.equals("")){
			set_password_value.setText(getString(R.string.set_setting_no));
		}else{
			set_password_value.setText(getString(R.string.set_setting_ok));
			
		}
		
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.set_download_select:
			new AlertDialog.Builder(this).setTitle(
					getString(R.string.account_settings_download_select))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setSingleChoiceItems(textArray,
							Global.set_download_select,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Global.set_download_select = which;
									set_download_textview
											.setText(textArray[Global.set_download_select]);
									dialog.dismiss();
								}
							}).setNegativeButton(getString(R.string.cancel),
							null).show();
			break;
		case R.id.set_mail_count:
			new AlertDialog.Builder(this).setTitle(
					getString(R.string.account_settings_mail_down_count_label))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setSingleChoiceItems(mail_counttextArray,
							downLoadCountindex,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									String count = (String) visiblelimitValues[which];
									Global.set_download_count = Integer
											.parseInt(count);
									settings_download_count
											.setText(mail_counttextArray[which]);
									dialog.dismiss();
								}
							}).setNegativeButton(getString(R.string.cancel),
							null).show();
			break;
		case R.id.store_area_layout:
			new AlertDialog.Builder(this).setTitle(
					getString(R.string.store_area))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setSingleChoiceItems(storeAreaArray, Global.set_store,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Global.set_store = which;
									store_area
											.setText(storeAreaArray[Global.set_store]);
									dialog.dismiss();
								}
							}).setNegativeButton(getString(R.string.cancel),
							null).show();
			break;
		case R.id.store_min_size_layout:
			new AlertDialog.Builder(this).setTitle(
					getString(R.string.store_min_size))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setSingleChoiceItems(storeMinAreaArray, Global.set_sdSize,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Global.set_sdSize = which;
									store_min_size
											.setText(storeMinAreaArray[Global.set_sdSize]);
									dialog.dismiss();
								}
							}).setNegativeButton(getString(R.string.cancel),
							null).show();
			break;
		case R.id.netdata_layout:
			new AlertDialog.Builder(this).setTitle(
					getString(R.string.stream_max_size))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setSingleChoiceItems(storeMaxAreaArray,
							Global.set_streamSize,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Global.set_streamSize = which;
									stream_max_size_tv
											.setText(storeMaxAreaArray[Global.set_streamSize]);
									dialog.dismiss();
								}
							}).setNegativeButton(getString(R.string.cancel),
							null).show();
			break;

		case R.id.background_music_layout:

			new AlertDialog.Builder(this).setTitle(
					getString(R.string.music_backgroud_open_close))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setSingleChoiceItems(openCloseArray,
							Global.set_music_background_value,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Global.set_music_background_value = which;
									background_music_value
											.setText(openCloseArray[Global.set_music_background_value]);
									dialog.dismiss();
								}
							}).setNegativeButton(getString(R.string.cancel),
							null).show();
			break;
		case R.id.shortcut_layout:

			new AlertDialog.Builder(this).setTitle(
					getString(R.string.shortcut_set_open))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setSingleChoiceItems(openCloseArray, Global.set_shortcut,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Global.set_shortcut = which;
									shortcut_value
											.setText(openCloseArray[Global.set_shortcut]);
									dialog.dismiss();
								}
							}).setNegativeButton(getString(R.string.cancel),
							null).show();
			break;
		case R.id.soft_update_layout:
			new AlertDialog.Builder(this).setTitle(
					getString(R.string.soft_update_open))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setSingleChoiceItems(openCloseArray,
							Global.set_softUpdate,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Global.set_softUpdate = which;
									soft_update_value
											.setText(openCloseArray[Global.set_softUpdate]);
									dialog.dismiss();
								}
							}).setNegativeButton(getString(R.string.cancel),
							null).show();
			break;

		case R.id.securtiy_imsi_layout:
			new AlertDialog.Builder(this).setTitle(
					getString(R.string.securtiy_imsi_open))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setSingleChoiceItems(openCloseArray, Global.set_security,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Global.set_security = which;
									securtiy_imsi_value
											.setText(openCloseArray[Global.set_security]);
									dialog.dismiss();
								}
							}).setNegativeButton(getString(R.string.cancel),
							null).show();
			break;
		case R.id.set_password_layout:
			LayoutInflater factory = LayoutInflater.from(this);
			final EditText textEntryView = (EditText)factory.inflate(R.layout.dialog_password_set, null);
	        textEntryView.setText(Global.set_security_password);
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.set_password_layout_tilte))
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage(getString(R.string.set_password_content))
					.setView(textEntryView)
					.setPositiveButton(getString(R.string.popmenu_set),new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								Global.set_security_password = textEntryView.getText().toString();
								if(Global.set_security_password.equals("")){
									set_password_value.setText(getString(R.string.set_setting_no));
								}else{
									set_password_value.setText(getString(R.string.set_setting_ok));
									
								}
								dialog.dismiss();
							}
						})
					.setNeutralButton(getString(R.string.clear_set),new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
		                    	Global.set_security_password = "";
		                    	if(Global.set_security_password.equals("")){
		                			set_password_value.setText(getString(R.string.set_setting_no));
		                		}else{
		                			set_password_value.setText(getString(R.string.set_setting_ok));
		                			
		                		}
		                    	dialog.dismiss();
		                    }
	                })
				    .setNegativeButton(getString(R.string.cancel),
				    		new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                    	
	                    	dialog.dismiss();
	                    }
	                 })
	                .show();
			
			break;
		}

	}

	public int getArrayIndex(CharSequence[] textArray, String value) {
		int index = 0;
		for (int i = 0; i < textArray.length; i++) {
			if (textArray[i].equals(value)) {
				index = i;
				break;
			}
		}
		return index;
	}
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            saveSettings();
        }
        return super.onKeyDown(keyCode, event);
    }
	private void saveSettings() {
		Editor sharedata = getSharedPreferences(Email.STORESETMESSAGE, 0)
				.edit();
		sharedata.putInt(Global.set_store_name, Global.set_store);
		sharedata.putInt(Global.set_sd_name, Global.set_sdSize);
		sharedata.putInt(Global.set_stream_name, Global.set_streamSize);
		sharedata.putInt(Global.set_softUpdate_name, Global.set_softUpdate);
		sharedata.putInt(Global.set_shortcut_name, Global.set_shortcut);
		sharedata.putInt(Global.set_security_name, Global.set_security);

		sharedata.putInt(Global.set_download_select_name,
				Global.set_download_select);

		sharedata.putInt(Global.set_download_count_name, Global.set_download_count);
		Email.VISIBLE_LIMIT_DEFAULT = Global.set_download_count;
		Email.VISIBLE_LIMIT_INCREMENT = Email.VISIBLE_LIMIT_DEFAULT;

		Global.set_scret_send = mScretSend.isChecked();
		sharedata.putBoolean(Global.set_scret_send_name, Global.set_scret_send);

		sharedata.putInt(Global.set_music_background,
				Global.set_music_background_value);

		sharedata.putString(Global.set_security_password_name, Global.set_security_password);
		sharedata.commit();
	}
}
