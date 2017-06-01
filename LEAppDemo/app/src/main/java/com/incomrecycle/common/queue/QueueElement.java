package com.incomrecycle.common.queue;

public class QueueElement
{
	public Object m_obj;
	public QueueElement m_prev;
	public QueueElement m_next;

	private QueueElement()
	{
		m_obj = null;
		m_prev = null;
		m_next = null;
	}

	private static QueueElement m_root = null;
	public static final synchronized QueueElement create()
	{
		if(m_root == null)
		{
			return new QueueElement();
		}
		else
		{
			QueueElement queueElement = m_root;
			m_root = m_root.m_next;
			queueElement.m_next = null;
			return queueElement;
		}
	}
	public static final synchronized void release(QueueElement tQueueElement)
	{
		tQueueElement.m_obj = null;
		tQueueElement.m_prev = null;
		tQueueElement.m_next = m_root;
		m_root = tQueueElement;
	}
}
