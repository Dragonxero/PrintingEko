package com.incomrecycle.prms.rvm.gui.action;

import com.incomrecycle.prms.rvm.gui.CommonServiceHelper;
import com.incomrecycle.prms.rvm.gui.CommonServiceHelper.GUICommonService;
import com.incomrecycle.prms.rvm.gui.GUIAction;

public class GUIActionLaunchMaintainGUI extends GUIAction{

	@Override
	protected void doAction(Object[] paramObjs) {
		GUICommonService guiCommonService = CommonServiceHelper.getGUICommonService();
		try {
    		guiCommonService.execute("GUIRecycleCommonService", "launchMaintainGUI", null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
