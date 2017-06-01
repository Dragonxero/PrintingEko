package com.incomrecycle.prms.rvm.gui.event;

import java.util.HashMap;

import com.google.code.microlog4android.Logger;
import com.incomrecycle.common.json.JSONUtils;
import com.incomrecycle.prms.rvm.gui.CommonServiceHelper;
import com.incomrecycle.prms.rvm.gui.CommonServiceHelper.GUICommonService;
import com.incomrecycle.prms.rvm.gui.GUIGlobal;

public class GUIEventThread implements Runnable {
	private final static Logger logger = Logger.getLogger("GUIEvent");
	public void run() {
		while(true) {
			if(!CommonServiceHelper.isEnable()) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
				continue;
			}
			GUICommonService guiCommonService = CommonServiceHelper.getGUICommonService();
			try {
				HashMap<String,Object> hsmpEvent = guiCommonService.execute("GUIEventCommonService", null, null);
				logger.debug("\n" + JSONUtils.toJSON(hsmpEvent));
				GUIGlobal.getEventMgr().addEvent(hsmpEvent);
			} catch(Exception e) {
				GUIGlobal.exit();
			}
		}
	}
}
