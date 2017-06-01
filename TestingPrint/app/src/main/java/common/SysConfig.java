package common;

import java.util.Properties;

import common.utils.PropUtils;
import common.utils.StringUtils;

public class SysConfig {
	private static final Properties prop = new Properties();
	public static void init(String resource) {
		synchronized(SysConfig.prop) {
			if(SysConfig.prop.size() == 0) {
				if(resource == null || "".equals(resource)) {
					resource = "config";
				}
				SysConfig.prop.putAll(PropUtils.loadResource(resource));
				if(!StringUtils.isBlank(prop.getProperty("CONFIG.CUSTOMERIZED"))) {
					SysConfig.prop.putAll(PropUtils.loadResource(prop.getProperty("CONFIG.CUSTOMERIZED")));
				}
			}
		}
	}
	public static void set(Properties prop) {
		init(null);
		synchronized(SysConfig.prop) {
			SysConfig.prop.putAll(prop);
		}
	}
	public static void set(String name, String value) {
		init(null);
		prop.setProperty(name, value);
	}
	public static String get(String name) {
		init(null);
		return prop.getProperty(name);
	}
}
