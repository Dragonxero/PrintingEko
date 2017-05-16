package common.task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import common.SysGlobal;
import common.utils.DateUtils;

public class CrontabMgr {
	private static final CrontabMgr crontabThread = new CrontabMgr();
	private List<HashMap<String,Object>> listCronTabItem = new ArrayList<HashMap<String,Object>>();
	private CrontabMgr() {
		SysGlobal.execute(new Runnable() {
			@Override
			public void run() {
				CrontabMgr.this.run();
			}
		});
	}
	public static CrontabMgr getCrontabMgr() {
		return crontabThread;
	}

	private void run() {
		long lLastSeconds = 0;
		while(true) {
			long lSeconds = System.currentTimeMillis() / 1000;
			while((lLastSeconds >= lSeconds) || ((lSeconds % 60) != 0)) {
				try {
					Thread.sleep(1000);
				} catch(Exception e) {
				}
				lSeconds ++;
			}
			lLastSeconds = lSeconds;
			Date tDate = new Date();
			Calendar calendar = Calendar.getInstance();
			int minute = Integer.parseInt(DateUtils.formatDatetime(tDate, "mm"));
			int hour = Integer.parseInt(DateUtils.formatDatetime(tDate, "HH"));
			int monthday = Integer.parseInt(DateUtils.formatDatetime(tDate, "dd"));
			int month = Integer.parseInt(DateUtils.formatDatetime(tDate, "MM"));
			int weekday = calendar.get(Calendar.DAY_OF_WEEK) + 1;
			
			for(int t=0;t<listCronTabItem.size();t++) {
				HashMap<String,Object> cronTabItem = listCronTabItem.get(t);
				if(isCronTabTime(minute,(int[])cronTabItem.get("minutes"))
				&& isCronTabTime(hour,(int[])cronTabItem.get("hours"))
				&& isCronTabTime(monthday,(int[])cronTabItem.get("monthdays"))
				&& isCronTabTime(month,(int[])cronTabItem.get("months"))
				&& isCronTabTime(weekday,(int[])cronTabItem.get("weekdays"))
				) {
					SysGlobal.execute((new TaskExecuteThread((TaskAction)cronTabItem.get("action"))));
				}
			}
		}
	}
	private static class TaskExecuteThread implements Runnable {
		TaskAction taskAction;
		TaskExecuteThread(TaskAction taskAction) {
			this.taskAction = taskAction;
		}
		public void run() {
			taskAction.execute();
		}
	}
	public static boolean isCronTabTime(int now, int[] times) {
		if(times == null)
			return true;
		if(times.length == 0)
			return true;
		for(int i=0;i<times.length;i++) {
			if(times[i] == now)
				return true;
		}
		return false;
	}
	public static int[] parseTime(String s, int min, int max) {
		if(s == null)
			return null;
		if(!s.equals("*")) {
			String[] ms = s.split(",");
			List<String> lms = new ArrayList<String>();
			for(int k=0;k<ms.length;k++) {
				if(ms.length == 0)
					continue;
				int idx = ms[k].indexOf("-");
				if(idx == -1)
					lms.add(ms[k]);
				else {
					int start = Integer.parseInt(ms[k].substring(0,idx));
					int end = Integer.parseInt(ms[k].substring(idx + 1));
					int m = start;
					while(true) {
						lms.add("" + m);
						if(m == end)
							break;
						m ++;
						if(m > max)
							m = min;
					}
				}
			}
			if(lms.size() > 0) {
				int[] rs = new int[lms.size()];
				for(int k=0;k<lms.size();k++) {
					rs[k] = Integer.parseInt(lms.get(k));
				}
				return rs;
			}
		}
		return null;
	}
	/**
	 * 
	 * @param triggerTime
	 *     triggerTime format is "m H D M W", for example:
	 *     * * * * *
	 *     0,15,30,45 * * * *
	 *     0 7-21 * * *
	 * @param taskAction
	 * @return
	 */
	public boolean registerCronTab(String triggerTime, TaskAction taskAction) {
		if(triggerTime == null)
			return false;
		triggerTime = triggerTime.trim();
		if(triggerTime.length() == 0)
			return false;
		if(triggerTime.startsWith("#"))
			return false;
		triggerTime = triggerTime.replaceAll("\t", " ");
		int idx = triggerTime.indexOf(" ");
		if(idx == -1)
			return false;
		String minutes = triggerTime.substring(0,idx);
		triggerTime = triggerTime.substring(idx + 1).trim();
		idx = triggerTime.indexOf(" ");
		if(idx == -1)
			return false;
		String hours = triggerTime.substring(0,idx);
		triggerTime = triggerTime.substring(idx + 1).trim();
		idx = triggerTime.indexOf(" ");
		if(idx == -1)
			return false;
		String monthdays = triggerTime.substring(0,idx);
		triggerTime = triggerTime.substring(idx + 1).trim();
		idx = triggerTime.indexOf(" ");
		if(idx == -1)
			return false;
		String months = triggerTime.substring(0,idx);
		triggerTime = triggerTime.substring(idx + 1).trim();
		String weekdays = triggerTime;

		HashMap<String,Object> hsmpCronTabItem = new HashMap<String,Object>();
		try {
			hsmpCronTabItem.put("minutes",parseTime(minutes,0, 59));
			hsmpCronTabItem.put("hours",parseTime(hours,0, 23));
			hsmpCronTabItem.put("monthdays",parseTime(monthdays,1, 31));
			hsmpCronTabItem.put("months",parseTime(months,1, 12));
			hsmpCronTabItem.put("weekdays",parseTime(weekdays,0,6));
			hsmpCronTabItem.put("action",taskAction);
			listCronTabItem.add(hsmpCronTabItem);
			return true;
		} catch(Exception e) {
		}
		return false;
	}
}
