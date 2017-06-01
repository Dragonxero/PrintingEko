package com.incomrecycle.prms.rvm.gui;

import java.util.HashMap;

import com.incomrecycle.common.json.JSONUtils;
import com.incomrecycle.common.utils.StringUtils;
import com.incomrecycle.prms.rvm.commonservice.CommonServiceClient;

public class CommonServiceHelper {
	public static CommonServiceClient commonServiceClient = null;
	public static void init(String ip, int port) {
		if(commonServiceClient == null) {
			commonServiceClient = CommonServiceClient.getClient(ip ,port);
		}
	}
	public static void done() {
		if(commonServiceClient != null) {
			commonServiceClient.close();
			commonServiceClient = null;
		}
	}
	public static boolean isEnable() {
		if(commonServiceClient == null)
			return false;
		return true;
	}
	public static GUICommonService getGUICommonService() {
		return guiCommonService;
	}
	private static GUICommonService guiCommonService = new GUICommonService();
	public static class GUICommonService {
		private GUICommonService() {
		}
		public HashMap<String,Object> execute(String svcName, String subSvcName, HashMap<String,Object> hsmpParam) throws Exception {
			if(!isEnable())
				return null;
			HashMap hsmpFrame = new HashMap();
			hsmpFrame.put("SVC_NAME", svcName);
			hsmpFrame.put("SUB_SVC_NAME", subSvcName);
			hsmpFrame.put("JSON", JSONUtils.toJSON(hsmpParam));
			String res = commonServiceClient.request(JSONUtils.toJSON(hsmpFrame));
			if(res == null) {
				throw new Exception("CommonServer Error");
			} else {
				HashMap hsmpResult = JSONUtils.toHashMap(res);
				String exception = (String)hsmpResult.get("EXCEPTION");
				String result = (String)hsmpResult.get("RESULT");
				if(!StringUtils.isBlank(exception))
					throw new Exception(exception);
				else {
					return (HashMap)JSONUtils.toHashMap(result);
				}
			}
		}
	}
}
