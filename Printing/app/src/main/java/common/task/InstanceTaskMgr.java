package common.task;

import common.SysGlobal;
import common.queue.FIFOQueue;

public class InstanceTaskMgr {
	private final static InstanceTaskMgr instanceTask = new InstanceTaskMgr();
	private FIFOQueue fifoQueue = new FIFOQueue();
	public InstanceTaskMgr() {
		SysGlobal.execute(new Runnable() {
			@Override
			public void run() {
				InstanceTaskMgr.this.run();
			}
		});
	}
	public static InstanceTaskMgr getInstanceTaskMgr() {
		return instanceTask;
	}
	public void addTask(TaskAction taskAction) {
		fifoQueue.push(taskAction);
	}

	private void run() {
		TaskAction taskAction;
		while((taskAction = (TaskAction)fifoQueue.pop()) != null) {
			taskAction.execute();
		}
	}
}
