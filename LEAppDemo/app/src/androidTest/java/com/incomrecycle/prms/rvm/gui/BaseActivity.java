package com.incomrecycle.prms.rvm.gui;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.google.code.microlog4android.Logger;
import com.incomrecycle.common.SysGlobal;
import com.incomrecycle.common.task.TimeoutAction;
import com.incomrecycle.common.task.TimeoutTask;
import com.incomrecycle.common.utils.StringUtils;

public abstract class BaseActivity extends Activity {
	private final static Logger logger = Logger.getLogger("BaseActivity");
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		System.out.println("onDestroy:" + this);
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		System.out.println("onPause:" + this);
	}
	@Override
	protected void onPostResume() {
		// TODO Auto-generated method stub
		super.onPostResume();
		System.out.println("onPostResume:" + this);
	}
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		System.out.println("onRestart:" + this);
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		System.out.println("onResume:" + this);
		GUIGlobal.setTopBaseActivity(getName());
	}
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		System.out.println("onStart:" + this);
		checkOnStart();
	}
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		System.out.println("onStop:" + this);
	}
	private final static class MsgWhat {
		private final static int GUIAction = 0;
		private final static int GUIEvent = 1;
		private final static int GUILanguage = 2;
	}
	private static class GUIActionExecutor {
		private GUIAction guiAction;
		private Object[] paramObjs;
		protected GUIActionExecutor(GUIAction guiAction, Object[] paramObjs) {
			this.guiAction = guiAction;
			this.paramObjs = paramObjs;
		}
	}
	private Handler guiHandler = new Handler() {
		public void handleMessage(Message msg) {
			if(msg.what == MsgWhat.GUIAction) {
				GUIActionExecutor guiActionExecutor = (GUIActionExecutor)msg.obj;
				guiActionExecutor.guiAction.doAction(guiActionExecutor.paramObjs);
			}
			if(msg.what == MsgWhat.GUIEvent) {
				try {
					if(doEventFilter((HashMap)msg.obj)) {
						doEvent((HashMap)msg.obj);
					}
				} catch(Exception e) {
					logger.debug(e);
				}
			}
			if(msg.what == MsgWhat.GUILanguage) {
				updateLanguage();
			}
		}
	};
	public final void executeGUIAction(int delaySeconds, GUIAction guiAction, Object[] paramObjs) {
		if(delaySeconds < 0) {
			executeGUIAction(false,guiAction,paramObjs);
		}
		if(delaySeconds == 0) {
			executeGUIAction(true,guiAction,paramObjs);
		}
		SysGlobal.execute(new Runnable() {
			private int delaySeconds;
			private GUIAction guiAction;
			private Object[] paramObjs;
			public Runnable setDelaySeconds(int delaySeconds, GUIAction guiAction, Object[] paramObjs) {
				this.delaySeconds = delaySeconds;
				this.guiAction = guiAction;
				this.paramObjs = paramObjs;
				return this;
			}
			public void run() {
				try {
					Thread.sleep(delaySeconds * 1000);
					executeGUIAction(false,guiAction,paramObjs);
				} catch(Exception e) {
				}
			}
		}.setDelaySeconds(delaySeconds, guiAction, paramObjs));
	}
	public final void executeGUIAction(boolean isBlocking, GUIAction guiAction, Object[] paramObjs) {
		if(isBlocking) {
			guiAction.doAction(paramObjs);
		} else {
			GUIActionExecutor guiActionExecutor = new GUIActionExecutor(guiAction,paramObjs);
			Message msg = new Message();
			msg.what = MsgWhat.GUIAction;
			msg.obj = guiActionExecutor;
			guiHandler.sendMessage(msg);
		}
	}
	public boolean hasPackage(String packageName) {
		if(StringUtils.isBlank(packageName))
			return false;
		try {
			PackageManager packageManager = getPackageManager();
			ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
			if(applicationInfo != null)
				return true;
		} catch(Exception e) {
		}
		return false;
	}
	public boolean launchPackage(String packageName) {
		if(StringUtils.isBlank(packageName))
			return false;
		try {
			PackageManager packageManager = getPackageManager();
			Intent intent = new Intent();
			intent = packageManager.getLaunchIntentForPackage(packageName);
			startActivity(intent);
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	public BaseActivity() {
		super();
		GUIGlobal.setBaseActivity(getName(), this);
	}
	public BaseActivity getBaseActivity() {
		return this;
	}
	public void checkOnStart() {
		if(!GUIGlobal.isGUIReady()) {
			SysGlobal.execute(new Runnable() {
				@Override
				public void run() {
					GUIGlobal.exit();
				}
			});
		}
	}
	public void finish() {
		super.finish();
		GUIGlobal.setBaseActivity(getName(), null);
		System.out.println("Finish:" + this);
	}
	public void showOrHideKeybordAndResetTime(EditText edtClick, final TimeoutAction timeoutAction){
		//edtClick.setInputType(InputType.TYPE_NULL);
//		edtClick.setOnClickListener(new View.OnClickListener() {
			
//			@Override
//			public void onClick(View v) {
//				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
//				if (imm.isActive()) { 
//					imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS); 
//				}
				TimeoutTask.getTimeoutTask().reset(timeoutAction);
//			}
//		});
		edtClick.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

				TimeoutTask.getTimeoutTask().reset(timeoutAction);
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				TimeoutTask.getTimeoutTask().reset(timeoutAction);
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				TimeoutTask.getTimeoutTask().reset(timeoutAction);
			}
		});
	}
	public String getName() {
		return this.getClass().getSimpleName();
	}
	public final void postEvent(HashMap hsmpEvent) {
		Message msg = new Message();
		msg.what = MsgWhat.GUIEvent;
		msg.obj = hsmpEvent;
		guiHandler.sendMessage(msg);
	}
	public final void doUpdateLanguage() {
		Message msg = new Message();
		msg.what = MsgWhat.GUILanguage;
		guiHandler.sendMessage(msg);
	}
	public boolean doEventFilter(HashMap hsmpEvent) {
		return true;
	}
	public abstract void updateLanguage();
	public abstract void doEvent(HashMap hsmpEvent);
}
