package com.mail163.email.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;

import com.mail163.email.Email.Global;
import com.mail163.email.util.Utiles;

public class SendImageView extends ImageView {
    private final String TAG = "SendImageView";

    public static int TOOL_BAR_HIGH = 0;
    public static WindowManager.LayoutParams params = new WindowManager.LayoutParams();
    private float startX;
    private float startY;
    private float x;
    private float y;
    private WindowManager wm = (WindowManager) getContext().getApplicationContext().getSystemService(
            getContext().WINDOW_SERVICE);

    private Context mContext;
    private Paint paint;

    private int circleWidth = 10;
    private int initX = 113;
    private int initY = 21;
    private int initR = 2;
    private int k = 1;

    private static SendImageView SENDVIEWS = null;

    public static SendImageView getInstance(Context context) {
        Global.send_view_animl_flag = true;
        if (SENDVIEWS == null) {
            SENDVIEWS = new SendImageView(context);
        }

        return SENDVIEWS;
    }

    public void setSendImageViewNull() {
        SENDVIEWS = null;
    }

    public SendImageView(Context context) {
        super(context);
        mContext = context;

        paint = new Paint();
        paint.setColor(Color.WHITE);

        initPx();
        updateUI();
    }
    
    private void initPx(){
        circleWidth = Utiles.dip2px(mContext, circleWidth);
        initX = Utiles.dip2px(mContext, initX);
        initY = Utiles.dip2px(mContext, initY);
        initR = Utiles.dip2px(mContext, initR);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        if (touchX > (getRight() - 15) && touchX < getRight()) {
            mContext.sendBroadcast(new Intent(Global.ACTION_SENDMESSAGE_BROADCASET)); // 结束发送广播
            return true;
        }
        // 触摸点相对于屏幕左上角坐标

        x = event.getRawX();
        y = event.getRawY() - TOOL_BAR_HIGH;
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            startX = event.getX();
            startY = event.getY();
            break;
        case MotionEvent.ACTION_MOVE:
            updatePosition();
            break;
        case MotionEvent.ACTION_UP:
            updatePosition();
            startX = startY = 0;
            break;
        }

        return true;
    }

    // 更新浮动窗口位置参数
    private void updatePosition() {
        // View的当前位置
        params.x = (int) (x - startX);
        params.y = (int) (y - startY);
        try {
            wm.updateViewLayout(this, params);
        } catch (IllegalArgumentException iie) {
            iie.printStackTrace();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < k; i++) {
            canvas.drawCircle(initX + circleWidth * i, initY, initR, paint);
        }
        if (k++ == 4) {
            k = 0;
        }

    }

    private Handler handler = new Handler();
    private Runnable update = new Runnable() {
        public void run() {
            updateUI();
        }
    };

    private void updateUI() {
        postInvalidate();

        if (Global.send_view_animl_flag) {
            handler.postDelayed(update, 700);
        }
    }

}