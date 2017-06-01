package common.queue;

public interface IQueue
{
	long INFINITY = -1;

	void push(Object obj);
	void push(long timeout, Object obj);
	int getSize();
	void reset();
	void terminate();
	Object pop();
	Object pop(long timeoutMilliseconds);
	Object tryPop();
}
