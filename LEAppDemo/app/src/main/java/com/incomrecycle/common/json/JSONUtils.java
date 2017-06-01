package com.incomrecycle.common.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class JSONUtils {
	public static String toJSON(List listParam) {
		if(listParam == null)
			return null;
		return (new JSONArray(listParam)).toString();
	}
	public static String toJSON(HashMap hsmpParam) {
		if(hsmpParam == null)
			return null;
		return (new JSONObject(hsmpParam)).toString();
	}
	public static List toList(JSONArray jsonArray) throws Exception {
		if(jsonArray == null)
			return null;
		List listResult = new ArrayList();
		for(int i=0;i<jsonArray.length();i++) {
			Object obj = jsonArray.get(i);
			if(obj == null) {
				listResult.add(null);
				continue;
			}
			if(obj.equals(JSONObject.NULL)) {
				listResult.add(null);
				continue;
			}
			if(obj instanceof JSONObject) {
				listResult.add(toHashMap((JSONObject)obj));
				continue;
			}
			if(obj instanceof JSONArray) {
				listResult.add(toList((JSONArray)obj));
				continue;
			}
			listResult.add(obj);
		}
		return listResult;
	}
	public static HashMap toHashMap(JSONObject jsonObject) throws Exception {
		if(jsonObject == null)
			return null;
		HashMap hsmpResult = new HashMap();
		Iterator iter = jsonObject.keys();
		while(iter.hasNext()) {
			String key = iter.next().toString();
			Object val = jsonObject.get(key);
			if(val == null) {
				hsmpResult.put(key, null);
				continue;
			}
			if(val.equals(JSONObject.NULL)) {
				hsmpResult.put(key, null);
				continue;
			}
			if(val instanceof JSONArray) {
				hsmpResult.put(key, toList((JSONArray)val));
				continue;
			}
			if(val instanceof JSONObject) {
				hsmpResult.put(key, toHashMap((JSONObject)val));
				continue;
			}
			hsmpResult.put(key, val);
		}
		return hsmpResult;
	}
	public static List toList(String json) throws Exception {
		if(json == null)
			return null;
		return toList(new JSONArray(json));
	}
	public static HashMap toHashMap(String json) throws Exception {
		if(json == null)
			return null;
		return toHashMap(new JSONObject(json));
	}
}
