package com.mail163.email.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import com.mail163.email.Logs;

public class MessageScrollView extends ScrollView implements View.OnTouchListener {

    private Handler handler;
    private View view;
    private OnScrollListener onScrollListener;
    private static float ANIMATION_DISTANCE = 30;

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        onScrollListener.onAutoScroll(l, t, oldl, oldt);
    }

    public MessageScrollView(Context context) {
        super(context);
    }

    public MessageScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    // �������ܵĸ߶�
    public int computeVerticalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    public int computeVerticalScrollOffset() {
        return super.computeVerticalScrollOffset();
    }

    private void init() {
        this.setOnTouchListener(this);
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                super.handleMessage(msg);
                switch (msg.what) {
                case 1:
                    if (onScrollListener != null) {
                        onScrollListener.onHideBottomBar();
                    }
                    break;
                case 2:
                    if (onScrollListener != null) {
                        onScrollListener.onShowBottomBar();
                    }
                    break;
                default:
                    break;
                }
            }
        };

    }

    /**
     * ��òο���View����Ҫ��Ϊ�˻������MeasuredHeight��Ȼ��͹�������ScrollY+getHeight���Ƚϡ�
     */
    public void getView() {
        this.view = getChildAt(0);
        if (view != null) {
            init();
        }
    }

    /**
     * ����ӿ�
     * 
     * @author admin
     */
    public interface OnScrollListener {
        void onHideBottomBar();

        void onShowBottomBar();

        void onScroll();

        void onAutoScroll(int l, int t, int oldl, int oldt);
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
        getView();
    }

    private float startY = 0;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_MOVE:
            if (startY == 0) {
                startY = event.getY();
            }
            if (event.getY() - startY > ANIMATION_DISTANCE) {
                // ��ʾ
                if (view != null && onScrollListener != null) {
                    handler.sendMessageDelayed(handler.obtainMessage(2), 200);
                    startY = event.getY();
                }
            } else if (startY - event.getY() > ANIMATION_DISTANCE) {
                // ����
                if (view != null && onScrollListener != null) {
                    handler.sendMessageDelayed(handler.obtainMessage(1), 200);
                    startY = event.getY();
                }
            }
            break;
        case MotionEvent.ACTION_UP:
            startY = 0;
            break;
        }
        return super.onTouchEvent(event);
    }

}
