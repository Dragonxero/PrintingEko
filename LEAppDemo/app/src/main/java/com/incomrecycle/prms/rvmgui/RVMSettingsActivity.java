package com.incomrecycle.prms.rvmgui;

import java.util.HashMap;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.incomrecycle.common.task.TimeoutAction;
import com.incomrecycle.common.task.TimeoutTask;
import com.incomrecycle.prms.rvm.gui.BaseActivity;
import com.incomrecycle.prms.rvm.gui.GUIAction;
import com.incomrecycle.prms.rvmgui.R;

public class RVMSettingsActivity extends BaseActivity {
	@Override
	public void onStop() {
		super.onStop();
		TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, false);
		finish();
	}

	@Override
	public void onStart() {
		super.onStart();
		TimeoutTask.getTimeoutTask().addTimeoutAction(timeoutAction, 30,false);
		TimeoutTask.getTimeoutTask().reset(timeoutAction);
		TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, true);
	}

	@Override
	public void finish() {
		super.finish();
		TimeoutTask.getTimeoutTask().setEnabled(timeoutAction, false);
		TimeoutTask.getTimeoutTask().removeTimeoutAction(timeoutAction);
	}

	private TimeoutAction timeoutAction = new TimeoutAction() {
		@Override
		public void apply(int forwardSeconds, int remainedSeconds) {
			System.out.println("TimeoutAction:" + RVMSettingsActivity.this);
			GUIAction guiAction = new GUIAction() {
				@Override
				protected void doAction(Object[] paramObjs) {
					int remainedSeconds = (Integer) paramObjs[1];
					if (remainedSeconds == 0) {

						finish();
					} else {
						TextView textTime = (TextView) findViewById(R.id.showNumText);
						textTime.setText("" + remainedSeconds);
					}
				}
			};
			executeGUIAction(false, guiAction, new Object[] { forwardSeconds,remainedSeconds });
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().getDecorView().setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
		setContentView(R.layout.activity_channel_main);
		Button backButton = (Button) findViewById(R.id.backButton);
		backButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		TimeoutTask.getTimeoutTask().reset(timeoutAction);
		return super.onTouchEvent(event);
	}

	@Override
	public void updateLanguage() {
		Button backButton = (Button) findViewById(R.id.backButton);
		backButton.setText(R.string.backBtn);

	}
	@Override
	public void doEvent(HashMap hsmpEvent) {
		// TODO Auto-generated method stub

	}
	public TimeoutAction getTimeoutAction() {
		return timeoutAction;
	}
}
