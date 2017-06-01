package com.incomrecycle.common.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.incomrecycle.common.SysGlobal;

public class DelayTaskMgr {
	private static final DelayTaskMgr delayTask = new DelayTaskMgr();
	private HashMap<TaskAction, Long> hsmpTaskAction = new HashMap<TaskAction, Long>(); 
	private DelayTaskMgr() {
		SysGlobal.execute(new Runnable() {
			@Override
			public void run() {
				DelayTaskMgr.this.run();
			}
		});
	}
	public static DelayTaskMgr getDelayTaskMgr() {
		return delayTask;
	}
	private void run() {
		while(true) {
			List<TaskAction> listTaskAction = new ArrayList<TaskAction>();
			synchronized(hsmpTaskAction) {
				try {
					long lTime = System.currentTimeMillis();
					long lNextTime = 0;
					Iterator<TaskAction> iter = hsmpTaskAction.keySet().iterator();
					while(iter.hasNext()) {
						TaskAction taskAction = iter.next();
						Long atTime = hsmpTaskAction.get(taskAction);
						if(atTime <= lTime) {
							listTaskAction.add(taskAction);
						} else {
							if(lNextTime == 0)
								lNextTime = atTime;
							else if(lNextTime > atTime) {
								lNextTime = atTime;
							}
						}
					}
					if(listTaskAction.size() == 0) {
						if(lNextTime == 0)
							hsmpTaskAction.wait();
						else
							hsmpTaskAction.wait(lNextTime - lTime);
						continue;
					} else {
						for(int i=0;i<listTaskAction.size();i++) {
							TaskAction taskAction = listTaskAction.get(i);
							hsmpTaskAction.remove(taskAction);
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			for(int i=0;i<listTaskAction.size();i++) {
				TaskAction taskAction = listTaskAction.get(i);
				SysGlobal.execute((new Runnable() {
					TaskAction taskAction;
					public Runnable setTaskAction(TaskAction taskAction) {
						this.taskAction = taskAction;
						return this;
					}
					public void run() {
						taskAction.execute();
					}
				}).setTaskAction(taskAction));
			}
		}
	}
	public void addDelayTask(TaskAction taskAction, long milliSeconds) {
		if(milliSeconds <= 0)
			taskAction.execute();
		else {
			synchronized(hsmpTaskAction) {
				hsmpTaskAction.put(taskAction, (Long)(System.currentTimeMillis() + milliSeconds));
				hsmpTaskAction.notify();
			}
		}
	}
	public void removeDelayTask(TaskAction taskAction) {
		synchronized(hsmpTaskAction) {
			if(hsmpTaskAction.get(taskAction) != null) {
				hsmpTaskAction.remove(taskAction);
				hsmpTaskAction.notify();
			}
		}
	}
}
