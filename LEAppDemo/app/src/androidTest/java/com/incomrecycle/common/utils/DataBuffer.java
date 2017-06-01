package com.incomrecycle.common.utils;

public class DataBuffer {
	private final static int BLOCK_SIZE = 1024;
	private DataBlock m_head = null;
	private DataBlock m_tail = null;
	private int m_blocks = 0;

	public DataBuffer() {
	}
	public int length() {
		if(m_blocks == 0)
			return 0;
		return m_blocks * BLOCK_SIZE - m_head.m_offset - m_tail.getFreeSize();
	}
	private void preAppend() {
		if(m_tail == null) {
			m_head = createDataBlock();
			m_tail = m_head;
			m_blocks ++;
		} else {
			if((m_tail.m_offset + m_tail.m_len) == BLOCK_SIZE) {
				m_tail.m_next = createDataBlock();
				m_tail = m_tail.m_next;
				m_blocks ++;
			}
		}
	}
	public void append(byte b) {
		preAppend();
		m_tail.m_buff[m_tail.m_offset + m_tail.m_len] = b;
		m_tail.m_len ++;
	}
	public void append(byte[] buff) {
		append(buff,0,buff.length);
	}
	public void append(byte[] buff, int offset, int len) {
		if(offset < 0 || offset >= buff.length) {
			throw new RuntimeException("Out of index!");
		}
		if(len > (buff.length - offset))
			len = (buff.length - offset);
		while(len > 0) {
			preAppend();
			int max = m_tail.getFreeSize();
			if(max > len)
				max = len;
			System.arraycopy(buff, offset, m_tail.m_buff, m_tail.m_offset + m_tail.m_len, max);
			m_tail.m_len += max;
			offset += max;
			len -= max;
		}
	}
	public byte get(int offset) {
		if(offset < 0 || offset >= length())
			throw new RuntimeException("Out of index!");
		DataBlock tDataBlock = m_head;
		while(offset >= tDataBlock.m_len) {
			offset -= tDataBlock.m_len;
			tDataBlock = tDataBlock.m_next;
		}
		return tDataBlock.m_buff[tDataBlock.m_offset + offset];
	}
	public int copy(byte[] buff, int offset, int len) {
		if(offset == length())
			return 0;
		if(offset < 0 || offset > length())
			throw new RuntimeException("Out of index!");
		int max = length() - offset;
		if(len > max)
			len = max;
		if(max == 0)
			return 0;
		int relLen = len;

		DataBlock tDataBlock = m_head;
		while(offset >= tDataBlock.m_len) {
			offset -= tDataBlock.m_len;
			tDataBlock = tDataBlock.m_next;
		}
		int idx = 0;
		while(len > 0) {
			max = tDataBlock.m_len - offset;
			if(max > len)
				max = len;
			System.arraycopy(tDataBlock.m_buff, tDataBlock.m_offset + offset, buff, idx, max);
			offset = 0;
			len -= max;
			idx += max;
			tDataBlock = tDataBlock.m_next;
		}
		return relLen;
	}
	public byte[] get(int offset, int len) {
		if(offset < 0 || offset >= length())
			throw new RuntimeException("Out of index!");
		int max = length() - offset;
		if(len > max)
			len = max;
		if(max == 0)
			return new byte[0];
		byte[] buff = new byte[len];
		copy(buff,0,len);
		return buff;
	}
	public void remove(int len) {
		int max = length();
		if(len > max)
			len = max;
		while(len > 0) {
			if(len >= m_head.m_len) {
				len -= m_head.m_len;
				DataBlock tDataBlock = m_head;
				m_head = m_head.m_next;
				if(m_head == null)
					m_tail = null;
				releaseDataBlock(tDataBlock);
				m_blocks --;
			} else {
				m_head.m_offset += len;
				m_head.m_len -= len;
				return;
			}
		}
	}
	public void clear() {
		remove(length());
	}

	private static final Object g_ObjLcok = new Object();

	private static DataBlock g_root = null;
	private static DataBlock createDataBlock() {
		DataBlock tDataBlock = null;
		synchronized(g_ObjLcok) {
			if(g_root == null)
				tDataBlock = new DataBlock();
			else {
				tDataBlock = g_root;
				g_root = g_root.m_next;
				tDataBlock.m_len = 0;
				tDataBlock.m_offset = 0;
				tDataBlock.m_next = null;
			}
		}
		return tDataBlock;
	}
	private static void releaseDataBlock(DataBlock tDataBlock) {
		synchronized(g_ObjLcok) {
			tDataBlock.m_next = g_root;
			g_root = tDataBlock;
		}
	}
	private static class DataBlock
	{
		DataBlock m_next = null;

		byte[] m_buff = new byte[BLOCK_SIZE];
		int m_offset = 0;
		int m_len = 0;
		public int getFreeSize() {
			return BLOCK_SIZE - m_offset - m_len;
		}
	}
}
