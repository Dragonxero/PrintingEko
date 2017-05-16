package common.task;

import java.util.ArrayList;
import java.util.List;

import common.SysGlobal;

public class TickTaskMgr {
	private static final TickTaskMgr tickTaskThread = new TickTaskMgr();
	private List<TickTask> listTickTask = new ArrayList<TickTask>();
	private TickTaskMgr() {
		SysGlobal.execute(new Runnable() {
			@Override
			public void run() {
				TickTaskMgr.this.run();
			}
		});
	}
	public static TickTaskMgr getTickTaskMgr() {
		return tickTaskThread;
	}
	public void register(TaskAction taskAction, double seconds, boolean isBlocking) {
		synchronized(this) {
			try {
				for(int i=0;i<listTickTask.size();i++) {
					TickTask tickTask = listTickTask.get(i);
					if(tickTask.taskAction.equals(taskAction))
						return;
				}
				TaskAction tickTaskAction = taskAction;
				TickTask tickTask = new TickTask(tickTaskAction, seconds, isBlocking);
				listTickTask.add(tickTask);
			} catch(Exception e) {
			}
		}
	}
	public void unregister(TaskAction taskAction) {
		synchronized(this) {
			try {
				for(int i=0;i<listTickTask.size();i++) {
					TickTask tickTask = listTickTask.get(i);
					if(tickTask.taskAction.equals(taskAction)) {
						listTickTask.remove(i);
						return;
					}
				}
			} catch(Exception e) {
			}
		}
	}
	public void clearRegister() {
		synchronized(this) {
			listTickTask.clear();
		}
	}
	private void run() {
		while(true) {
			try {
				Thread.sleep(100);
				synchronized(this) {
					long lCurrentTime = System.currentTimeMillis();
					for(int i=0;i<listTickTask.size();i++) {
						TickTask tickTask = listTickTask.get(i);
						if(tickTask.isExecuting)
							continue;
						if(tickTask.lastTime == 0 || tickTask.lastTime > lCurrentTime || ((tickTask.lastTime + tickTask.seconds * 1000) < lCurrentTime)) {
							SysGlobal.execute((new TickTaskExecuteTask(tickTask)));
						}
					}
				}
			} catch(Exception e) {
			}
		}
	}
	private static class TickTask {
		TaskAction taskAction;
		double seconds;
		long lastTime;
		boolean isExecuting;
		boolean isBlocking;
		TickTask(TaskAction taskAction,double seconds, boolean isBlocking) {
			this.taskAction = taskAction;
			this.seconds = seconds;
			this.lastTime = 0;
			this.isExecuting = false;
			this.isBlocking = isBlocking;
		}
		void run() {
			isExecuting = true;
			lastTime = System.currentTimeMillis();
			taskAction.execute();
			lastTime = System.currentTimeMillis();
			isExecuting = false;
		}
	}
	private static class TickTaskExecuteTask implements Runnable {
		TickTask tickTask;
		TickTaskExecuteTask(TickTask tickTask) {
			this.tickTask = tickTask;
		}
		public void run() {
			tickTask.run();
		}
	}
}
