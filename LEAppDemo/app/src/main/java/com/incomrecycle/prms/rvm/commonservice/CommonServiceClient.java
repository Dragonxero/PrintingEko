package com.incomrecycle.prms.rvm.commonservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

import java.net.Socket;
import android.os.StrictMode;
import android.util.Log;

import com.incomrecycle.common.queue.FIFOQueue;
import com.incomrecycle.common.utils.IOUtils;
import com.incomrecycle.common.utils.SocketUtils;

public class CommonServiceClient {
	private static final String TRACE = "CommonServiceClient";
	private Socket socket = null;
	private InputStream is = null;
	private OutputStream os = null;
	private int idSeq = 0;
	private FIFOQueue fifoQueue = new FIFOQueue();
	private HashMap<Integer,CommonServiceReq> hsmpCommonServiceReq = new HashMap<Integer,CommonServiceReq>();
	public final static CommonServiceClient getClient(String ip, int port) {
		Socket socket = null;
		try {
			setPermission();
			socket = SocketUtils.createSocket(ip, port, 5000);
			CommonServiceClient commonServiceClient = new CommonServiceClient(socket);
			return commonServiceClient;
		} catch (Exception e) {
			Log.e(TRACE, "CLIENT", e);
			if(socket != null) {
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				socket = null;
			}
		}
		return null;
	}
	private CommonServiceClient(Socket socket) {
		this.socket = socket;
		try {
			socket.setSoTimeout(0);
			is = socket.getInputStream();
			os = socket.getOutputStream();
		} catch(Exception e) {
			Log.e(TRACE, "CLIENT", e);
		}
		(new Thread() {
			@Override
			public void run() {
				try {
					setPermission();
					CommonServiceFrame.recv(is, fifoQueue);
				} catch(Exception e) {
					Log.e(TRACE, "CLIENT", e);
				}
				close();
			}
		}).start();
		(new Thread() {
			@Override
			public void run() {
				CommonServiceFrame commonServiceFrame;
				while((commonServiceFrame = (CommonServiceFrame)fifoQueue.pop()) != null) {
					CommonServiceReq commonServiceReq = null;
					synchronized(hsmpCommonServiceReq) {
						commonServiceReq = hsmpCommonServiceReq.get(commonServiceFrame.getId());
					}
					if(commonServiceReq != null) {
						commonServiceReq.res = commonServiceFrame.getValue();
						synchronized(commonServiceReq) {
							commonServiceReq.notify();
						}
					}
				}
				synchronized(hsmpCommonServiceReq) {
					Iterator<Integer> iter = hsmpCommonServiceReq.keySet().iterator();
					while(iter.hasNext()) {
						Integer id = iter.next();
						if(id != null) {
							CommonServiceReq commonServiceReq = hsmpCommonServiceReq.get(id);
							if(commonServiceReq != null) {
								synchronized(commonServiceReq) {
									commonServiceReq.notify();
								}
							}
						}
					}
					hsmpCommonServiceReq.clear();
				}
			}
		}).start();
	}
	public boolean isOpen() {
		return !(socket == null || is == null || os == null);
	}
	public void close() {
		fifoQueue.push(null);
		is = IOUtils.close(is);
		os = IOUtils.close(os);
		if(socket != null) {
			try {
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			socket = null;
		}
		Log.e(TRACE, "client has been closed");
	}
	public String request(String value) {
		int id = 0;
		synchronized(this) {
			id = idSeq;
			idSeq ++;
		}
		CommonServiceReq commonServiceReq = new CommonServiceReq();
		commonServiceReq.id = id;
		commonServiceReq.req = value;
		synchronized(hsmpCommonServiceReq) {
			hsmpCommonServiceReq.put(commonServiceReq.id, commonServiceReq);
		}
		setPermission();
		synchronized(commonServiceReq) {
			try {
				CommonServiceFrame.send(os, commonServiceReq.id, commonServiceReq.req);
				commonServiceReq.wait();
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TRACE, "CLIENT", e);
			}
		}
		synchronized(hsmpCommonServiceReq) {
			if(hsmpCommonServiceReq.containsKey(commonServiceReq.id)) {
				hsmpCommonServiceReq.remove(commonServiceReq.id);
			}
		}
		return commonServiceReq.res;
	}
	private static void setPermission() {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		.penaltyLog()
		.permitAll()
		.build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
		.detectAll()
		.penaltyLog()
		.build());
	}
	private class CommonServiceReq {
		private int id = 0;
		private String req = null;
		private String res = null;
	}
}
