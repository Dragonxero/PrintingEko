package common.utils;

import android.os.StrictMode;

import common.SysGlobal;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

public class SocketUtils {
	private final static String LOCAL_IP = "127.0.0.1";
	public static String getIpAddress() {
		try {
			Enumeration<NetworkInterface> enumNI = NetworkInterface.getNetworkInterfaces();
			while(enumNI.hasMoreElements()) {
				NetworkInterface ni = enumNI.nextElement();
				List<InterfaceAddress> listInterfaceAddress = ni.getInterfaceAddresses();
				for(int i=0;i<listInterfaceAddress.size();i++) {
					InetAddress inetAddress = listInterfaceAddress.get(i).getAddress();
					if(inetAddress instanceof Inet4Address) {
						String info = inetAddress.getHostAddress();
						if(info.startsWith(LOCAL_IP)) {
							continue;
						}
						return info;
					}
				}
			}
		} catch(Exception e) {
		}
		return LOCAL_IP;
	}
	public static Socket close(Socket socket) {
		if(socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	public static Socket createSocket(String ip, int port, long timeoutMilliseconds) throws UnknownHostException, IOException {
		SocketInfo socketInfo = new SocketInfo();
		socketInfo.ip = ip;
		socketInfo.port = port;
		if(timeoutMilliseconds < 0)
			timeoutMilliseconds = 0;
		long lStartTime = System.currentTimeMillis();
		long lEndTime = lStartTime + timeoutMilliseconds;
		synchronized(socketInfo) {
			SysGlobal.execute((new SocketClientThread(socketInfo)));
			while(!socketInfo.isFinished) {
				try {
					if(timeoutMilliseconds <= 0)
						socketInfo.wait();
					else {
						long lTime = System.currentTimeMillis();
						if(lTime >= lEndTime) {
							socketInfo.isDone = true;
							break;
						}
						socketInfo.wait(lEndTime - lTime);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		if(socketInfo.ioException != null)
			throw socketInfo.ioException;
		if(socketInfo.unknownHostException != null)
			throw socketInfo.unknownHostException;
		if(!socketInfo.isFinished) {
			throw new UnknownHostException("TIMEOUT");
		}
		return socketInfo.socket;
	}
	private static class SocketClientThread implements Runnable {
		private SocketInfo socketInfo;
		public SocketClientThread(SocketInfo socketInfo) {
			this.socketInfo = socketInfo;
		}
		public void run() {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
			.detectDiskReads()
			.detectDiskWrites()
			.detectNetwork()
			.detectAll()
			.penaltyLog()
			.permitAll()
			.build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
			.detectLeakedSqlLiteObjects()
			.detectLeakedClosableObjects()
			.penaltyLog()
			.penaltyDeath()
			.build());

			try {
				socketInfo.socket = new Socket(socketInfo.ip,socketInfo.port);
			} catch (UnknownHostException e) {
				socketInfo.unknownHostException = e;
			} catch (IOException e) {
				socketInfo.ioException = e;
			}

			synchronized(socketInfo) {
				if(socketInfo.isDone) {
					if(socketInfo.socket != null) {
						socketInfo.socket = close(socketInfo.socket);
					}
				} else {
					socketInfo.isFinished = true;
					socketInfo.notify();
				}
			}
		}
	}
	private static class SocketInfo {
		private Socket socket = null;
		private String ip;
		private int port;
		private boolean isFinished = false;
		private boolean isDone = false;
		private UnknownHostException unknownHostException = null;
		private IOException ioException = null;
	}
}
