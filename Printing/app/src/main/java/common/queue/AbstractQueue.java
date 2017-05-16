package common.queue;


public abstract class AbstractQueue implements IQueue {
	public void terminate() {
		push(null);
	}
	public Object pop() {
		return pop(INFINITY);
	}
	public Object tryPop() {
		return pop(0);
	}
}
