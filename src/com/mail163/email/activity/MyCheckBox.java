package com.mail163.email.activity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.CompoundButton;

public class MyCheckBox extends CompoundButton {
	private Drawable mButtonDrawable;

	public MyCheckBox(Context paramContext) {
		this(paramContext, null);
	}

	public MyCheckBox(Context paramContext, AttributeSet paramAttributeSet) {
		this(paramContext, paramAttributeSet, 16842860);
	}

	public MyCheckBox(Context paramContext, AttributeSet paramAttributeSet,
			int paramInt) {
		super(paramContext, paramAttributeSet, paramInt);
	}

	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (this.mButtonDrawable == null)
			return;
		int[] arrayOfInt = getDrawableState();
		this.mButtonDrawable.setState(arrayOfInt);
		invalidate();
	}

	 public int getCompoundPaddingLeft() {
	    return 0;
	 }

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		final Drawable buttonDrawable = mButtonDrawable;
		if (buttonDrawable != null) {
			final int verticalGravity = getGravity()
					& Gravity.VERTICAL_GRAVITY_MASK;
			final int width = buttonDrawable.getIntrinsicWidth();
			final int height = buttonDrawable.getIntrinsicHeight();
			int x = 0;
			int y = 0;
			switch (verticalGravity) {
			case Gravity.BOTTOM:
				y = getHeight() - height;
				break;
			case Gravity.CENTER_VERTICAL:
				y = (getHeight() - height) / 2;
				break;
			}

			x = getWidth() - width;
		
			buttonDrawable.setBounds(x, y, x + width, y + height);
			buttonDrawable.draw(canvas);
		}
	}

	public void setButtonDrawable(Drawable d) {
		if (d != null) {
			if (mButtonDrawable != null) {
				mButtonDrawable.setCallback(null);
				unscheduleDrawable(mButtonDrawable);
			}
			d.setCallback(this);
			d.setState(getDrawableState());
			d.setVisible(getVisibility() == VISIBLE, false);
			mButtonDrawable = d;
			mButtonDrawable.setState(null);
			setMinHeight(mButtonDrawable.getIntrinsicHeight());
		}

		refreshDrawableState();
	}

	protected boolean verifyDrawable(Drawable who) {
		return super.verifyDrawable(who) || who == mButtonDrawable;
	}
}