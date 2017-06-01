package com.incomrecycle.prms.rvm.gui.init;

import java.util.HashMap;

import com.incomrecycle.common.SysGlobal;
import com.incomrecycle.common.init.InitInterface;
import com.incomrecycle.prms.rvm.gui.CommonServiceHelper;
import com.incomrecycle.prms.rvm.gui.GUIGlobal;
import com.incomrecycle.prms.rvm.gui.event.GUIActivityEventListener;
import com.incomrecycle.prms.rvm.gui.event.GUIApplicationEventListener;
import com.incomrecycle.prms.rvm.gui.event.GUIEventThread;
import com.incomrecycle.prms.rvmgui.RVMApplication;

public class GUIInit implements InitInterface {

	@Override
	public void Init(HashMap hsmp) {
		CommonServiceHelper.init("localhost", 8888);
		for(int i=0;i<10;i++) {
			if(CommonServiceHelper.isEnable())
				break;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		GUIGlobal.getEventMgr().add(new GUIApplicationEventListener(RVMApplication.getApplication()));
		GUIGlobal.getEventMgr().add(new GUIActivityEventListener());
		GUIEventThread guiEventThread = new GUIEventThread();
		SysGlobal.execute(guiEventThread);
	}
}
