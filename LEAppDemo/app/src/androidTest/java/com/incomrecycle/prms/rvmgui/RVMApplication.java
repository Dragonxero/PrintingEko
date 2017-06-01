package com.incomrecycle.prms.rvmgui;

import android.app.Application;
import android.os.StrictMode;

public class RVMApplication extends Application {
	private static Application gApplication = null;
	public RVMApplication() {
		super();
		gApplication = this;
	}
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		init();
		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext());
	}
	private void init() {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		.permitAll()
		.build());
	}
	public static Application getApplication() {
		return gApplication;
	}
}
