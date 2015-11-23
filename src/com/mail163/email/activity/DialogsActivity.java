package com.mail163.email.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.mail163.email.R;
import com.mail163.email.Email.Global;
import com.mail163.email.service.UpdateService;

public class DialogsActivity  extends Activity implements OnClickListener{
	private Button mConfirm;
	private Button mCancel;
    private TextView mContent;
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.dialog_softupdate);
		setTitle(getString(R.string.soft_update_title));
		
		mConfirm = (Button)findViewById(R.id.confirm);
		mCancel = (Button)findViewById(R.id.cancel);
		mContent = (TextView)findViewById(R.id.update_content);
		
		mContent.setText(getContent());
		
		mConfirm.setOnClickListener(this);
		mCancel.setOnClickListener(this);
		
	}
    public String getContent(){
    	StringBuffer content = new StringBuffer();
    	content.append("当前版本号:");
    	content.append(Global.localVersion);
    	content.append(" , ");
    	content.append("最新版本号:");
//    	content.append(Html.fromHtml("<font color='red'><b>"+Global.serverVersion+"</b></font>"));
    	content.append(Global.serverVersion);
    	content.append("\n");
    	content.append("更新包大小:");
    	content.append(Global.apkSize+"k");
    	content.append("\n");
    	content.append(Global.softUpdateContent);
    	return content.toString();
    }
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.confirm :
			Intent updateIntent = new Intent(this, UpdateService.class);
			updateIntent.putExtra("titleId",R.string.app_name);
			this.startService(updateIntent);
			finish();
			break;
		case R.id.cancel :
			finish();
			break;
		}
		
	}

}
