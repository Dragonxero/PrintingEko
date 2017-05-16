package common.queue;

public class FILOQueue extends AbstractQueue
{
	private QueueElement m_root = null;
	private int m_count = 0;

	public FILOQueue()
	{
	}

	public void push(Object obj)
	{
		QueueElement tQueueElement = QueueElement.create();
		tQueueElement.m_obj = obj;
		synchronized(this)
		{
			tQueueElement.m_next = m_root;
			m_root = tQueueElement;
			m_count ++;
			notifyAll();
		}
	}
	public int getSize()
	{
		return m_count;
	}
	public void reset() {
		QueueElement tQueueElement = null;
		synchronized(this)
		{
			while(m_root != null)
			{
				tQueueElement = m_root;
				m_root = m_root.m_next;

				tQueueElement.m_obj = null;
				tQueueElement.m_next = null;
				m_count --;
				if(tQueueElement != null)
					QueueElement.release(tQueueElement);
			}
		}
	}
	public Object pop(long timeoutMilliseconds)
	{
		QueueElement tQueueElement = null;
		Object obj = null;
		long lEnd = timeoutMilliseconds + System.currentTimeMillis();
		synchronized(this)
		{
			if(timeoutMilliseconds == 0 && m_root == null)
				return null;
			while(m_root == null)
			{
				try
				{
					if(timeoutMilliseconds < 0) {
						wait();
					} else {
						long lCurrent = System.currentTimeMillis();
						if(lEnd <= lCurrent) {
							return null;
						}
						wait(lEnd - lCurrent);
					}
				}
				catch(Exception e)
				{
					return null;
				}
			}
			tQueueElement = m_root;
			m_root = m_root.m_next;

			obj = tQueueElement.m_obj;
			tQueueElement.m_obj = null;
			tQueueElement.m_next = null;
			m_count --;
		}
		if(tQueueElement != null)
			QueueElement.release(tQueueElement);
		return obj;
	}

	public void push(long timeout, Object obj) {
		push(obj);
	}
}
