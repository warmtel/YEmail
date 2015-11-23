

package com.mail163.email.activity;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.mail163.email.R;

/**
 * This custom View is the list item for the MessageList activity, and serves two purposes:
 * 1.  It's a container to store message metadata (e.g. the ids of the message, mailbox, & account)
 * 2.  It handles internal clicks such as the checkbox or the favorite star
 */
public class ContactsEmailListItem extends RelativeLayout {

	public long mMessageId;
    public boolean mSelected;
    public String mEmail;
    public String mUser;
    private boolean mAllowBatch;
    private ContactsEmail.MessageListAdapter mAdapter;

    private boolean mDownEvent;
    private boolean mCachedViewPositions;
    private int mCheckRight;
    private int mStarLeft;
    // Padding to increase clickable areas on left & right of each list item
    private final static float STAR_PAD = 10.0F;

    public ContactsEmailListItem(Context context) {
        super(context);
    }

    public ContactsEmailListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContactsEmailListItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Called by the adapter at bindView() time
     * 
     * @param adapter the adapter that creates this view
     * @param allowBatch true if multi-select is enabled for this list
     */
    public void bindViewInit(ContactsEmail.MessageListAdapter adapter, boolean allowBatch) {
        mAdapter = adapter;
        mAllowBatch = allowBatch;
        mCachedViewPositions = false;
    }

    /**
     * Overriding this method allows us to "catch" clicks in the checkbox or star
     * and process them accordingly.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        int touchX = (int) event.getX();

        if (!mCachedViewPositions) {
            float paddingScale = getContext().getResources().getDisplayMetrics().density;
            int starPadding = (int) ((STAR_PAD * paddingScale) + 0.5);

            mStarLeft = findViewById(R.id.selected).getLeft() - starPadding;
 
            mCachedViewPositions = true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownEvent = true;
                if ((mAllowBatch && touchX < mCheckRight) || touchX > mStarLeft) {
                    handled = true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mDownEvent = false;
                break;

            case MotionEvent.ACTION_UP:
                if (mDownEvent) {
                    if (touchX > mStarLeft) {
                    	 mSelected = !mSelected;
                         mAdapter.updateSelected(this, mSelected);
                         handled = true;
                       
                    }
                }
                break;
        }

        if (handled) {
            postInvalidate();
        } else {
            handled = super.onTouchEvent(event);
        }

        return handled;
    }
}
