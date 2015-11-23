package com.mail163.email.activity;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * A custom text editor that helps automatically dismiss the activity along with the soft
 * keyboard.
 */
public class SearchEditText extends EditText {
    private boolean mMagnifyingGlassShown = true;
    private Drawable mMagnifyingGlass;

    public SearchEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMagnifyingGlass = getCompoundDrawables()[2];
    }

    /**
     * Conditionally shows a magnifying glass icon on the right side of the text field
     * when the text it empty.
     */
    @Override
    public boolean onPreDraw() {
        boolean emptyText = TextUtils.isEmpty(getText());
        if (mMagnifyingGlassShown != emptyText) {
            mMagnifyingGlassShown = emptyText;
            if (mMagnifyingGlassShown) {
                setCompoundDrawables(null, null, mMagnifyingGlass, null);
            } else {
                setCompoundDrawables(null, null, null, null);
            }
            return false;
        }
        return super.onPreDraw();
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (((ContactsEmail)getContext()).onKeyPreIme(keyCode, event)) {
            return true;
        }
        return super.onKeyPreIme(keyCode, event);
    }
}
