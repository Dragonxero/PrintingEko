package com.incomrecycle.prms.rvm.gui.event;

import java.util.HashMap;

import com.incomrecycle.common.event.EventListener;
import com.incomrecycle.prms.rvm.gui.BaseActivity;
import com.incomrecycle.prms.rvm.gui.GUIGlobal;

public class GUIActivityEventListener implements EventListener {
	@Override
	public void apply(Object event) {
		BaseActivity baseActivity = GUIGlobal.getCurrentBaseActivity();
		if(baseActivity == null)
			return;
		HashMap hsmpEvent = (HashMap)event;
		String type = (String)hsmpEvent.get("TYPE");
		if("Application".equalsIgnoreCase(type))
			return;
		baseActivity.postEvent(hsmpEvent);
	}
}
