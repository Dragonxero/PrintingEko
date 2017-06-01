package com.incomrecycle.prms.rvm.commonservice;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.incomrecycle.common.queue.FIFOQueue;
import com.incomrecycle.common.utils.StringUtils;

public class CommonServiceFrame {
	private final static String[][] sendConvert = {
		{"\\","\\\\"},
		{"\n","\\n"},
	};
	private final static String[][] recvConvert = {
		{"\\\\","\\"},
		{"\\n","\n"},
	};
	private int id;
	private String value;
	public int getId() {
		return id;
	}
	public String getValue() {
		return value;
	}
	public static void recv(InputStream is, FIFOQueue fifoQueue) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buff = new byte[2048];
		int readLen = 0;
		while((readLen = is.read(buff)) > 0) {
			int pos = 0;
			while(pos < readLen) {
				int nextPos = -1;
				for(int i=pos;i<readLen;i++) {
					if(buff[i] == '\n') {
						nextPos = i + 1;
						break;
					}
				}
				if(nextPos == -1) {
					baos.write(buff,pos,readLen - pos);
					break;
				} else {
					baos.write(buff,pos,nextPos - pos);
					pos = nextPos;
					byte[] data = baos.toByteArray();
					baos.reset();
					for(int s=0; s<data.length;s++) {
						if(data[s] == '$') {
							int div = -1;
							for(int d = s;d<data.length;d++) {
								if(data[d] == ';') {
									div = d;
									break;
								}
							}
							if(div != -1) {
								try {
									int id = Integer.parseInt(new String(data,s + 1,div - (s + 1)));
									String value = new String(data,div + 1,data.length - 1 - (div + 1),"utf-8");
									CommonServiceFrame commonServiceFrame = new CommonServiceFrame();
									commonServiceFrame.id = id;
									commonServiceFrame.value = StringUtils.convert(value, recvConvert);
									fifoQueue.push(commonServiceFrame);
								} catch(Exception e) {
									continue;
								}
							}
							break;
						}
					}
				}
			}
		}
		System.out.println(readLen);
	}
	public static void sendHeartBeat(OutputStream os) throws Exception {
		os.write('\n');
	}
	public static void send(OutputStream os, int id, String value) throws Exception {
		synchronized(os) {
			String finalValue = StringUtils.convert(value, sendConvert);
			os.write('$');
			os.write(Integer.toString(id).getBytes());
			os.write(';');
			os.write(finalValue.getBytes("utf-8"));
			os.write('\n');
			os.flush();
		}
	}
}
