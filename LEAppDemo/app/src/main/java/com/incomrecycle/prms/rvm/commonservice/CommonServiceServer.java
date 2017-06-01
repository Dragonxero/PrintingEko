package com.incomrecycle.prms.rvm.commonservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import java.net.Socket;
import java.net.ServerSocket;
import android.os.StrictMode;
import android.util.Log;

import com.incomrecycle.common.SysGlobal;
import com.incomrecycle.common.queue.FIFOQueue;
import com.incomrecycle.common.utils.IOUtils;

public class CommonServiceServer {
	private static final String TRACE = "CommonServiceServer";
	private ServerSocket serverSocket = null;
	private CommonServiceExecutor commonServiceExecutor = null;
	public final static CommonServiceServer getServer(int port,CommonServiceExecutor commonServiceExecutor) {
		try {
			setPermission();
			ServerSocket serverSocket = new ServerSocket(port);
			CommonServiceServer commonServiceServer = new CommonServiceServer(serverSocket,commonServiceExecutor);
			return commonServiceServer;
		} catch (Exception e) {
			Log.e(TRACE, "SERVER",e);
		}
		return null;
	}
	private static int clientIdSeq = 0;
	private static List<SocketClient> listSocketClient = new ArrayList<SocketClient>();
	private class SocketClient {
		private int clientId;
		private Socket socket = null;
		private InputStream is = null;
		private OutputStream os = null;
		private FIFOQueue fifoQueue = new FIFOQueue();

		public SocketClient(Socket socket) {
			clientId = clientIdSeq;
			clientIdSeq ++;
			this.socket = socket;
			try {
				socket.setSoTimeout(0);
				is = socket.getInputStream();
				os = socket.getOutputStream();
			} catch(Exception e) {
				Log.e(TRACE, "SERVER",e);
			}
			if(commonServiceExecutor != null) {
				commonServiceExecutor.onCreate(clientId);
			}
			SysGlobal.execute(new Runnable() {
				@Override
				public void run() {
					setPermission();
					try {
						CommonServiceFrame.recv(is, fifoQueue);
					} catch(Exception e) {
						Log.e(TRACE, "SERVER",e);
					}
					close();
				}
			});
			SysGlobal.execute(new Runnable() {
				@Override
				public void run() {
					CommonServiceFrame commonServiceFrame;
					while((commonServiceFrame = (CommonServiceFrame)fifoQueue.pop()) != null) {
						SysGlobal.execute(new RequestExecutor(commonServiceFrame));
					}
				}
			});
		}
		public void close() {
			synchronized(listSocketClient) {
				listSocketClient.remove(this);
			}
			fifoQueue.push(null);
			is = IOUtils.close(is);
			os = IOUtils.close(os);
			if(socket != null) {
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				socket = null;
			}
			if(commonServiceExecutor != null) {
				commonServiceExecutor.onClose(clientId);
			}
			Log.e(TRACE, "SERVER" + ":CLIENT:" + clientId, new Exception());
		}
		private class RequestExecutor implements Runnable {
			CommonServiceFrame commonServiceFrame;
			public RequestExecutor(CommonServiceFrame commonServiceFrame) {
				this.commonServiceFrame = commonServiceFrame;
			}
			@Override
			public void run() {
				if(commonServiceExecutor != null) {
					String result = commonServiceExecutor.execute(clientId, commonServiceFrame.getValue());
					try {
						setPermission();
						synchronized(SocketClient.this) {
							CommonServiceFrame.send(os, commonServiceFrame.getId(), result);
						}
					} catch (Exception e) {
						Log.e(TRACE, "SERVER",e);
						close();
					}
				}
			}
		}
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
	private CommonServiceServer(ServerSocket serverSocket,CommonServiceExecutor commonServiceExecutor) {
		this.commonServiceExecutor = commonServiceExecutor;
		this.serverSocket = serverSocket;
		SysGlobal.execute(new Runnable() {

			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					synchronized(listSocketClient) {
						for(int i=0;i<listSocketClient.size();i++) {
							try {
								CommonServiceFrame.sendHeartBeat(listSocketClient.get(i).os);
							} catch(Exception e) {
								Log.e(TRACE, "SERVER", e);
							}
						}
					}
				}
			}
		});
		SysGlobal.execute(new Runnable() {
			@Override
			public void run() {
				Socket socket = null;
				try {
					while((socket = CommonServiceServer.this.serverSocket.accept()) != null) {
						SocketClient socketClient = new SocketClient(socket);
						synchronized(listSocketClient) {
							listSocketClient.add(socketClient);
						}
					}
				} catch(Exception e) {
				}
				close();
			}
		});
	}
	public void close() {
		if(serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			serverSocket = null;
		}
	}
}
