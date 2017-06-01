package common.task;

import java.util.ArrayList;
import java.util.List;

import common.SysGlobal;

public class TimeoutTask implements TaskAction {
	private static final TimeoutTask timeoutTask = new TimeoutTask();
	private List<TimeoutTaskItem> listTimeoutTaskItem = new ArrayList<TimeoutTaskItem>();
	private TimeoutTask() {
		TickTaskMgr.getTickTaskMgr().register(this, 1, false);
	}
	public static TimeoutTask getTimeoutTask() {
		return timeoutTask;
	}
	public void reset(TimeoutAction timeoutAction) {
		synchronized(this) {
			int idx = getIndex(timeoutAction);
			if(idx != -1) {
				listTimeoutTaskItem.get(idx).isEnabled = true;
				listTimeoutTaskItem.get(idx).forwardSeconds = -1;
			}
		}
	}
	public void setEnabled(TimeoutAction timeoutAction, boolean isEnabled) {
		synchronized(this) {
			int idx = getIndex(timeoutAction);
			if(idx != -1) {
				listTimeoutTaskItem.get(idx).isEnabled = isEnabled;
			}
		}
	}
	public boolean isEnabled(TimeoutAction timeoutAction) {
		synchronized(this) {
			int idx = getIndex(timeoutAction);
			if(idx != -1) {
				return listTimeoutTaskItem.get(idx).isEnabled;
			}
			return false;
		}
	}

	public void addTimeoutAction(TimeoutAction timeoutAction, int timeoutSeconds) {
		addTimeoutAction(timeoutAction,timeoutSeconds,true);
	}
	public void addTimeoutAction(TimeoutAction timeoutAction, int timeoutSeconds, boolean isEnable) {
		if(timeoutAction == null)
			return;
		synchronized(this) {
			int idx = getIndex(timeoutAction);
			if(idx == -1) {
				listTimeoutTaskItem.add(new TimeoutTaskItem(timeoutAction,timeoutSeconds, isEnable));
			}
		}
	}
	public void removeTimeoutAction(TimeoutAction timeoutAction) {
		if(timeoutAction == null)
			return;
		synchronized(this) {
			int idx = getIndex(timeoutAction);
			if(idx != -1) {
				listTimeoutTaskItem.remove(idx);
			}
		}
	}
	private int getIndex(TimeoutAction timeoutAction) {
		if(timeoutAction == null)
			return -1;
		for(int i=0;i<listTimeoutTaskItem.size();i++) {
			if(listTimeoutTaskItem.get(i).timeoutAction == timeoutAction)
				return i;
		}
		return -1;
	}
	@Override
	public void execute() {
		synchronized(this) {
			for(int i=0;i<listTimeoutTaskItem.size();i++) {
				TimeoutTaskItem timeoutTaskItem = listTimeoutTaskItem.get(i);
				if(timeoutTaskItem.isEnabled && timeoutTaskItem.timeoutSeconds > 0) {
					timeoutTaskItem.forwardSeconds ++;
					if(timeoutTaskItem.forwardSeconds >= timeoutTaskItem.timeoutSeconds) {
						timeoutTaskItem.isEnabled = false;
					}
					SysGlobal.execute((new TimeoutTaskExecuteThread(timeoutTaskItem)));
				}
			}
		}
	}
	private static class TimeoutTaskItem {
		TimeoutTaskItem(TimeoutAction timeoutAction, int timeoutSeconds, boolean isEnable) {
			this.timeoutAction = timeoutAction;
			this.timeoutSeconds = timeoutSeconds;
			this.forwardSeconds = -1;
			this.isEnabled = isEnable;
			this.isRunning = false;
		}
		TimeoutAction timeoutAction;
		int timeoutSeconds;
		int forwardSeconds;
		boolean isEnabled;
		boolean isRunning;
		void run() {
			int remainedSeconds = timeoutSeconds - forwardSeconds;
			if(isRunning && remainedSeconds > 0)
				return;
			isRunning = true;
			timeoutAction.apply(forwardSeconds, remainedSeconds);
			isRunning = false;
			
		}
	}
	private static class TimeoutTaskExecuteThread implements Runnable {
		TimeoutTaskItem timeoutTaskItem;
		TimeoutTaskExecuteThread(TimeoutTaskItem timeoutTaskItem) {
			this.timeoutTaskItem = timeoutTaskItem;;
		}
		public void run() {
			timeoutTaskItem.run();
		}
	}
}
