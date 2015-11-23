/**
 * Copyright 2012 viktor.zhou
 */
package com.mail163.email;

import com.mail163.email.util.Utiles;

import android.content.Context;
import android.test.AndroidTestCase;

public class TestApi extends AndroidTestCase {
    Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
    }

    public void testUtilesMethod() throws Throwable {
        float px1 = Utiles.dip2px(mContext, 113);
        float dp1 = Utiles.px2dip(mContext, 113);
        
        Logs.v(Logs.LOG_MessageView, "px1 >> :"+px1+ " dp1 >> :"+dp1);
    }

}