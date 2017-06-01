package com.incomrecycle.prms.rvm.gui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Application;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;

import com.incomrecycle.common.event.EventMgr;
import com.incomrecycle.common.utils.DateUtils;
import com.incomrecycle.prms.rvmgui.RVMApplication;

public class GUIGlobal {
	private final static String TAG = "GUIGlobal";
	private static final EventMgr mgr = new EventMgr();
	private static final HashMap<String,BaseActivity> hsmpBaseActivity = new HashMap<String,BaseActivity>();
	private static final List<String> listTopBaseActivity = new ArrayList<String>();
	private static final HashMap hsmpCurrentSession = new HashMap();
	private static boolean isGUIReady = false;
	public static EventMgr getEventMgr() {
		return mgr;
	}
	public static void setGUIReady(boolean isGUIReady) {
		GUIGlobal.isGUIReady = isGUIReady;
	}
	public static boolean isGUIReady() {
		return isGUIReady;
	}
	public static BaseActivity getCurrentBaseActivity() {
		ActivityManager am = (ActivityManager) RVMApplication.getApplication().getSystemService(Application.ACTIVITY_SERVICE);
		List<RunningTaskInfo> RunningActivityList= am.getRunningTasks(1);
		if(RunningActivityList != null && RunningActivityList.size() > 0){
			ComponentName cn = RunningActivityList.get(0).topActivity;
			synchronized(hsmpBaseActivity) {
				Iterator<BaseActivity> iter = hsmpBaseActivity.values().iterator();
				while(iter.hasNext()) {
					BaseActivity baseActivity = iter.next();
					if(baseActivity == null)
						continue;
					if(baseActivity.getComponentName().equals(cn))
						return baseActivity;
				}
			}
		}
		return null;
	}
	public static void exit() {
		BaseActivity RVMMain = hsmpBaseActivity.get("RVMMain");
		HashMap<String,BaseActivity> hsmpBaseActivity = new HashMap<String,BaseActivity>();
		hsmpBaseActivity.putAll(GUIGlobal.hsmpBaseActivity);
		Iterator<BaseActivity> iter = hsmpBaseActivity.values().iterator();
		while(iter.hasNext()) {
			BaseActivity baseActivity = iter.next();
			if(baseActivity == null)
				continue;
			if(baseActivity == RVMMain)
				continue;
			try {
				baseActivity.finish();
			} catch(Exception e) {
			}
		}
		if(RVMMain != null) {
			try {
				RVMMain.finish();
			} catch(Exception e) {
			}
		}
		Log.w(TAG, "RVM Exit on " + DateUtils.formatDatetime(new Date(), "yyyy-MM-dd HH:mm:ss"));
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(0);
	}
	public static BaseActivity getBaseActivity(String name) {
		if(name == null)
			return null;
		synchronized(hsmpBaseActivity) {
			return hsmpBaseActivity.get(name);
		}
	}
	public static void setBaseActivity(String name,BaseActivity baseActivity) {
		synchronized(hsmpBaseActivity) {
			if(baseActivity == null) {
				hsmpBaseActivity.remove(name);
				synchronized(listTopBaseActivity) {
					if(listTopBaseActivity.contains(name)) {
						listTopBaseActivity.remove(name);
					}
				}
			} else {
				hsmpBaseActivity.put(name, baseActivity);
			}
		}
	}
	public static void setTopBaseActivity(String name) {
		synchronized(listTopBaseActivity) {
			if(listTopBaseActivity.contains(name)) {
				listTopBaseActivity.remove(name);
			}
			listTopBaseActivity.add(name);
		}
	}
	public static String getTopBaseActivity(int n) {
		synchronized(listTopBaseActivity) {
			if(n >= listTopBaseActivity.size()) {
				return null;
			}
			return listTopBaseActivity.get(listTopBaseActivity.size() - (n + 1));
		}
	}
	public static void updateLanguage() {
		synchronized(hsmpBaseActivity) {
			Iterator<String> iter = hsmpBaseActivity.keySet().iterator();
			while(iter.hasNext()) {
				String name = iter.next();
				BaseActivity baseActivity = hsmpBaseActivity.get(name);
				if(baseActivity != null) {
					baseActivity.doUpdateLanguage();
				}
			}
		}
	}
	public static Locale getLocale(Application application) {
		Resources resource = application.getResources();
		Configuration config = resource.getConfiguration();
		if(config.locale == null) {
			return Locale.getDefault();
		} else {
			return config.locale;
		}
	}
	public static void updateLanguage(Application application, Locale locale) {
		Locale.setDefault(locale);
		Resources resource = application.getResources();
		Configuration config = resource.getConfiguration();
		DisplayMetrics metrics = resource.getDisplayMetrics();
		config.locale = locale;
		resource.updateConfiguration(config, metrics);
		updateLanguage();
	}
	public static void setCurrentSession(String name, Object obj) {
		synchronized(hsmpCurrentSession) {
			hsmpCurrentSession.put(name, obj);
		}
	}
	public static Object getCurrentSession(String name) {
		synchronized(hsmpCurrentSession) {
			return hsmpCurrentSession.get(name);
		}
	}
	public static void clearCurrentSession() {
		synchronized(hsmpCurrentSession) {
			hsmpCurrentSession.clear();
		}
	}
}
