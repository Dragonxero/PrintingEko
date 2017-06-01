package com.incomrecycle.prms.rvm.gui.event;

import java.util.HashMap;

import android.app.Application;

import com.incomrecycle.common.event.EventListener;
import com.incomrecycle.common.utils.ShellUtils;
import com.incomrecycle.prms.rvm.gui.BaseActivity;
import com.incomrecycle.prms.rvm.gui.GUIGlobal;

public class GUIApplicationEventListener implements EventListener {
	private Application application;
	public GUIApplicationEventListener(Application application) {
		this.application = application;
	}
	@Override
	public void apply(Object event) {
		//which is Application Event
		BaseActivity baseActivity = GUIGlobal.getBaseActivity("RVMMain");
		if(baseActivity == null)
			return;
		HashMap hsmpEvent = (HashMap)event;
		String TYPE = (String)hsmpEvent.get("TYPE");
		String EVENT = (String)hsmpEvent.get("EVENT");
		if(EVENT == null)
			return;
		if(!"Application".equalsIgnoreCase(TYPE))
			return;
		if(EVENT.equalsIgnoreCase("UPDATE")) {
			baseActivity.postEvent(hsmpEvent);
		}
		if(EVENT.equalsIgnoreCase("CMD")) {
			if("LAUNCH_RECYCLE_GUI".equalsIgnoreCase((String)hsmpEvent.get("CMD"))) {
				ShellUtils.shell("am start com.incomrecycle.prms.rvmgui/com.incomrecycle.prms.rvmgui.RVMMainActivity");
			}
			if("LAUNCH_SETTINGS_GUI".equalsIgnoreCase((String)hsmpEvent.get("CMD"))) {
				ShellUtils.shell("am start com.incomrecycle.prms.rvmgui/com.incomrecycle.prms.rvmgui.RVMSettingsActivity");
			}
		}
	}
}
