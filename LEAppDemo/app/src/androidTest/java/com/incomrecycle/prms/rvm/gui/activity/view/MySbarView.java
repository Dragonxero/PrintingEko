package com.incomrecycle.prms.rvm.gui.activity.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.incomrecycle.common.SysGlobal;

public class MySbarView extends View {
	private static final String EMPTY = "";
	private MyDrawThread myDrawThread = null;
	private String text = EMPTY;
	private boolean isChanged = true;
	private int xPos = 0;
	private int yPos = 0;
	private int fontSize = 30;
	private String color = null;
	private float currentTextWidth;
	private int step = 1;
	public MySbarView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public MySbarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MySbarView(Context context) {
		super(context);
		init();
	}
	private void init() {
		this.addOnAttachStateChangeListener(new OnAttachStateChangeListener(){
			@Override
			public void onViewAttachedToWindow(View arg0) {
				isEnable = true;
				synchronized(MySbarView.this) {
					if(myDrawThread == null) {
						isEnable = true;
						myDrawThread = new MyDrawThread();
						SysGlobal.execute(myDrawThread);
					}
				}
			}

			@Override
			public void onViewDetachedFromWindow(View arg0) {
				isEnable = false;
			}
			
		});
	}
	public void setText(String text, int fontSize, String color, int step) {
		String prevText = this.text;
		if(prevText == null)
			prevText = EMPTY;
		if(text == null)
			text = EMPTY;
		if(text.equals(prevText))
			return;
		synchronized(this) {
			this.text = text;
			isChanged = true;
		}
		this.color = color;
		this.fontSize = fontSize;
		xPos = this.getWidth();
		this.step = step;
	}
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(color == null)
			return;
		String text = null;
		boolean isChanged = false;
		synchronized(this) {
			text = this.text;
			isChanged = this.isChanged;
			this.isChanged = false;
		}
		if(text == null)
			text = EMPTY;
		if(!isChanged) {
			if(text.length() == 0)
				return;
		}
		Paint paint= new Paint();
		paint.setTextSize(fontSize);
		paint.setAntiAlias(true);
		paint.setColor(Color.parseColor(color));
		if(isChanged) {
			currentTextWidth=paint.measureText(this.text);
			xPos =  this.getWidth();
		}
		//draw Text
		yPos = (this.getHeight() - fontSize)/2;
		canvas.drawText(text, xPos, yPos, paint);
		xPos -= step;
		if(xPos < - currentTextWidth) {
			xPos =  this.getWidth();
			if(onScrollEvent != null) {
				SysGlobal.execute(new Thread() {
					public void run() {
						OnScrollEvent onScrollEvent = MySbarView.this.onScrollEvent;
						if(onScrollEvent != null)
							onScrollEvent.onChange(MySbarView.this);
					}
				});
			}
		}
	}
	private boolean isEnable = true;
	private  class MyDrawThread  implements Runnable
	{
		public void run()
		{
			while(isEnable) {
				try {
					Thread.sleep(50);
					postInvalidate();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			synchronized(MySbarView.this) {
				myDrawThread = null;
			}
		}
	}
	public void setOnScrollEvent(OnScrollEvent onScrollEvent) {
		synchronized(this) {
			this.onScrollEvent = onScrollEvent;
		}
	}
	private OnScrollEvent onScrollEvent = null;
	public static interface OnScrollEvent {
		void onChange(View view);
	}
}
