package com.incomrecycle.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.ResourceBundle;

public class PropUtils {
	private final static Object objSync = new Object();
	public static Properties loadFile(String propFile) {
		if(StringUtils.isBlank(propFile))
			return loadFile((File)null);
		return loadFile(new File(propFile));
	}
	public static Properties loadFile(File file) {
		return loadFile(file,null);
	}
	public static Properties loadFile(File file, String charSetName) {
		Properties prop = new Properties();
		if(file == null)
			return prop;
		synchronized(objSync) {
			FileInputStream is = null;
			try {
				if(file.isFile()) {
					is = new FileInputStream(file);
					return loadInputStream(is,charSetName);
				}
			} catch(Exception e) {
			} finally {
				IOUtils.close(is);
			}
		}
		return prop;
	}
	public static Properties loadInputStream(InputStream is, String charSetName) {
		if(StringUtils.isBlank(charSetName))
			return loadInputStream(is);
		byte[] data = IOUtils.read(is);
		Properties prop = new Properties();
		if(data != null) {
			try {
				String s = new String(data,charSetName);
				String[] ss = StringUtils.replace(s, "\r", "\n").split("\n");
				for(int i=0;i<ss.length;i++) {
					String sm = ss[i].trim();
					if(sm.length() == 0)
						continue;
					if(sm.startsWith("#"))
						continue;
					int div = ss[i].indexOf("=");
					if(div == -1)
						continue;
					String sh = ss[i].substring(0,div).trim();
					if(sh.length() == 0)
						continue;
					String sv = StringUtils.replace(StringUtils.replace(StringUtils.replace(StringUtils.replace(StringUtils.replace(StringUtils.replace(StringUtils.replace(StringUtils.replace(ss[i].substring(div + 1),"\\=","="),"\\.","."),"\\:",":"),"\\'","'"),"\\\"","\""),"\\r","\r"),"\\n","\n"),"\\\\","\\");
					prop.setProperty(sh, sv);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return prop;
	}
	public static Properties loadInputStream(InputStream is) {
		Properties prop = new Properties();
		if(is == null)
			return prop;
		synchronized(objSync) {
			try {
				prop.load(is);
			} catch(Exception e) {
			} finally {
			}
		}
		return prop;
	}
	public static Properties loadResource(String resource) {
		Properties prop = new Properties();
		if(StringUtils.isBlank(resource))
			return prop;
		synchronized(objSync) {
			ResourceBundle rb = ResourceBundle.getBundle(resource);
			Iterator<String> iter = rb.keySet().iterator();
			while(iter.hasNext()) {
				String name = iter.next();
				String value = rb.getString(name);
				prop.setProperty(name, value);
			}
		}
		return prop;
	}
	public static boolean update(String propFile, Properties prop) {
		return update(new File(propFile), prop);
	}
	public static boolean update(File file, Properties prop) {
		Properties externalProp = new Properties();
		synchronized(objSync) {
			try {
				FileInputStream is = null;
				try {
					if(file.isFile()) {
						is = new FileInputStream(file);
						externalProp.load(is);
					}
				} catch(Exception e) {
				} finally {
					try {
						if(is != null)
							is.close();
					}catch(Exception e) {
					}
				}
				boolean hasChanged = false;
				Iterator<String> iterName = prop.stringPropertyNames().iterator();
				while(iterName.hasNext()) {
					String key = iterName.next();
					String newVal = StringUtils.trimToEmpty(prop.getProperty(key));
					String oldVal = StringUtils.trimToEmpty(externalProp.getProperty(key));
					if(!newVal.equals(oldVal)) {
						hasChanged = true;
						break;
					}
				}
				if(hasChanged) {
					externalProp.putAll(prop);
					FileOutputStream fos = new FileOutputStream(file,false);
					externalProp.store(fos, null);
					fos.flush();
					fos.close();
				}
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public static boolean save(File file, Properties prop) {
		synchronized(objSync) {
			try {
				FileOutputStream fos = new FileOutputStream(file,false);
				prop.store(fos, null);
				fos.flush();
				fos.close();
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
}
