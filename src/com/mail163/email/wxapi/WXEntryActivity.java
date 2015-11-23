package com.mail163.email.wxapi;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.mail163.email.Email;
import com.mail163.email.R;
import com.mail163.email.util.Configs;
import com.tencent.mm.sdk.openapi.BaseReq;
import com.tencent.mm.sdk.openapi.BaseResp;
import com.tencent.mm.sdk.openapi.ConstantsAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
    public Context mContext;
    Email mAplication;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAplication = (Email)getApplicationContext();
        if(mAplication.api == null){
    		mAplication.api = WXAPIFactory.createWXAPI(this, Configs.APP_ID, false);
    		mAplication.api.registerApp(Configs.APP_ID);
        }
        mAplication.api.handleIntent(getIntent(), this);
        mContext = this;
    }

    public void onReq(BaseReq req) {
        switch (req.getType()) {
        case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
            // goToGetMsg();
            break;
        case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
            // goToShowMsg((ShowMessageFromWX.Req) req);
            break;
        default:
            break;
        }
    }

    public void onResp(BaseResp resp) {
        switch (resp.errCode) {
        case BaseResp.ErrCode.ERR_OK:
            Toast.makeText(this, "分享成功!", Toast.LENGTH_SHORT).show();
            this.finish();
            break;
        case BaseResp.ErrCode.ERR_USER_CANCEL:
        	Toast.makeText(this, "取消分享!", Toast.LENGTH_SHORT).show();
        	this.finish();
        	break;
        case BaseResp.ErrCode.ERR_AUTH_DENIED:
            break;
        default:
            break;
        }
    }

}
