package com.mail163.email.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.mail163.email.R;
import com.mail163.email.Email.Global;

public class ContactsDialog extends Activity implements OnClickListener {
	private Button mConfirm;
	private Button mCancel;
	private TextView mContent;
	private CheckBox contactsMoren;

	public static void actionContactsDialog(Activity activity,int requestCode) {
		Intent i = new Intent(activity, ContactsDialog.class);
		activity.startActivityForResult(i, requestCode);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.dialog_contacts);
		setTitle(getString(R.string.contacts_add));
		setTheme(R.style.TitleStyle);
		mConfirm = (Button) findViewById(R.id.confirm);
		mCancel = (Button) findViewById(R.id.cancel);
		mContent = (TextView) findViewById(R.id.contacts_content);
		contactsMoren = (CheckBox) findViewById(R.id.contacts_moren);
		mContent.setText(getString(R.string.contacts_add_content));

		mConfirm.setOnClickListener(this);
		mCancel.setOnClickListener(this);

		contactsMoren.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				Global.contacts_add_notify = arg1;
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.confirm:
			Global.contacts_add_flag = true;
			setResult(RESULT_OK, getIntent());
			finish();
			break;
		case R.id.cancel:
			Global.contacts_add_flag = false;
			setResult(RESULT_OK, getIntent());
			finish();
			break;
		}

	}

}
