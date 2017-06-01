package common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ShellUtils {
	public static String shell(String cmd) {
		List<String> listCmd = new ArrayList<String>();
		if(cmd != null) {
			cmd = cmd.trim();
			if(cmd.length() > 0)
				listCmd.add(cmd);
		}
		return shell(listCmd);
	}
	public static String shell(List<String> listCmd) {
		try {
			Process su;
			su = Runtime.getRuntime().exec("su");
			OutputStream os = su.getOutputStream();
			if(listCmd != null) {
				for(int i=0;i<listCmd.size();i++) {
					os.write((listCmd.get(i) + "\n").getBytes());
				}
			}
			os.write(("exit\n").getBytes());
			try
			{
				InputStream in = su.getInputStream();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				byte[] bDataBuff = new byte[8192];
				while(true)
				{
					int iReaded = in.read(bDataBuff);
					if(iReaded <= 0)
						break;
					out.write(bDataBuff,0,iReaded);
				}
				in.close();
				return new String(out.toByteArray());
			}
			catch(Exception e)
			{
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static String execScript(File file) {
		if(file == null)
			return null;
		if(!file.isFile())
			return null;
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			String script = new String(IOUtils.read(fis));
			fis.close();
			return execScript(script);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static String execScript(String script) {
		if(script == null)
			return null;
		ByteArrayInputStream bais = new ByteArrayInputStream(script.getBytes());
		DataInputStream dis = new DataInputStream(bais);
		String str = null;
		List<String> listCmd = new ArrayList<String>();
		try {
			while((str = dis.readLine()) != null) {
				listCmd.add(str);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			dis.close();
			bais.close();
		} catch(Exception e) {
		}
		return shell(listCmd);
	}
	public static boolean kill(String activity) {
		int div = activity.indexOf("/");
		String pkgname = null;
		if(div != -1)
			pkgname = activity.substring(0,activity.indexOf("/"));
		else
			pkgname = activity;
		String text = shell("ps | busybox grep ' " + pkgname + "$' | busybox grep -v grep");;
		if(text != null) {
			String[] lines = text.split("\n");
			for(int l=0;l<lines.length;l++) {
				lines[l] = lines[l].trim();
				if(lines[l].endsWith(pkgname) && lines[l].indexOf(" grep ") == -1) {
					int idx = lines[l].indexOf(" ");
					if(idx != -1) {
						String substr = lines[l].substring(idx).trim();
						idx = substr.indexOf(" ");
						String pid = substr.substring(0,idx).trim();
						shell("kill -9 " + pid);
						return !isRunning(activity);
					}
					return true;
				}
			}
		}
		return true;
	}
	public static boolean isRunning(String activity) {
		int div = activity.indexOf("/");
		String pkgname = null;
		if(div != -1)
			pkgname = activity.substring(0,activity.indexOf("/"));
		else
			pkgname = activity;
		String text = shell("ps | busybox grep ' " + pkgname + "$' | busybox grep -v grep");
		if(text != null) {
			String[] lines = text.split("\n");
			for(int l=0;l<lines.length;l++) {
				lines[l] = lines[l].trim();
				if(lines[l].endsWith(pkgname) && lines[l].indexOf(" grep ") == -1) {
					return true;
				}
			}
		}
		return false;
	}
	public static void run(String activity) {
		shell("am start " + activity);
	}
	public static boolean install(String apk) {
		File file = new File(apk);
		if(file.isFile()) {
			shell("pm install -r " + file.getAbsolutePath());
			return true;
		}
		return false;
	}
	public static void delete(String apk) {
		File file = new File(apk);
		if(file.isFile()) {
			shell("rm " + file.getAbsolutePath());
		}
	}
}
