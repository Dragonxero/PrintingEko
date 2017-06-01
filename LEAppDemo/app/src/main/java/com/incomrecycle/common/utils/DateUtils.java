package com.incomrecycle.common.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
	public static String formatDatetime(Date tDate) {
		return formatDatetime(tDate, "yyyy-MM-dd HH:mm:ss");
	}
	public static String formatDatetime(Date tDate, String format) {
		if(tDate == null)
			return null;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.format(tDate);
	}
	public	static	String	formatDatetime(String p_sString) throws Exception
	{
		return	formatDatetime(parseDatetime(p_sString));
	}

	public	static	String	formatDate(String p_sString) throws Exception
	{
		return	formatDatetime(parseDatetime(p_sString), "yyyy-MM-dd");
	}

	public	static	boolean	isDateTime(String p_sTimeString)
	{
		try
		{
			if(parseDatetime(p_sTimeString) != null)
				return	true;
		}
		catch(Exception e)
		{
		}
		return	false;
	}

	public static String formatDatetime(int year, int month, int day, int hour, int minute, int second)
	{
		StringBuffer sb = new StringBuffer();
		sb.append("" + year);
		if(month > 9)
			sb.append("" + month);
		else
			sb.append("0" + month);
		if(day > 9)
			sb.append("" + day);
		else
			sb.append("0" + day);
		if(hour > 9)
			sb.append("" + hour);
		else
			sb.append("0" + hour);
		if(minute > 9)
			sb.append("" + minute);
		else
			sb.append("0" + minute);
		if(second > 9)
			sb.append("" + second);
		else
			sb.append("0" + second);
		return sb.toString();
	}

	public static Date parseDate( String p_sString ) throws Exception
	{
		if(p_sString == null || p_sString.trim().length() == 0)
			return	null;
		Calendar	tCalendar = Calendar.getInstance();
		tCalendar.setTime(parseDatetime(p_sString));

		tCalendar.set(Calendar.HOUR_OF_DAY,0);
		tCalendar.set(Calendar.MINUTE,0);
		tCalendar.set(Calendar.SECOND,0);
		tCalendar.set(Calendar.MILLISECOND, 0);

		return tCalendar.getTime();
	}
	public static String weekOfYear(Date date) {
		Calendar t = Calendar.getInstance();
		t.setTime(date);
		int w = t.get(Calendar.WEEK_OF_YEAR);
		if(w <= 9)
			return "0" + w;
		else
			return "" + w;
	}
	public static boolean isLegal(int iYear,int iMonth,int iDay,int iHour,int iMinute,int iSecond)
	{
		if(iYear < 1900 || iYear > 9999)
			return false;
		if(iMonth < 1 || iMonth > 12)
			return false;
		int iDays[] = {31,28,31,30,31,30,31,31,30,31,30,31};
		if((iYear % 4) == 0)
			iDays[1] = 29;
		if((iYear % 100) == 0)
			iDays[1] = 28;
		if((iYear % 400) == 0)
			iDays[1] = 29;
		if(iDay < 1 || iDay > iDays[iMonth - 1])
			return false;
		if(iHour < 0 || iHour > 59)
			return false;
		if(iMinute < 0 || iMinute > 59)
			return false;
		if(iSecond < 0 || iSecond > 59)
			return false;
		return	true;
	}
	public	static	Date	parseTime(String	strTime) throws Exception
	{
		if(strTime == null || strTime.trim().length() == 0)
			return	null;
		strTime = strTime.trim();
		if(strTime.length() == 0)
			return null;
		String formatted_time = null;
		while(true) {
			if(strTime.length() == 4 && strTime.matches("\\d{4}")) {
				//HHNN
				formatted_time = strTime + "00";
				break;
			}
			if(strTime.length() == 6 && strTime.matches("\\d{6}")) {
				//HHNNSS
				formatted_time = strTime;
				break;
			}
			if(strTime.length() >= 3 && strTime.matches("\\d{1,2}:\\d{1,2}")) {
				//H:N
				formatted_time = strTime + ":0";
				break;
			}
			if(strTime.length() >= 5 && strTime.matches("\\d{1,2}:\\d{1,2}:\\d{1,2}")) {
				//H:N:S
				formatted_time = strTime;
				break;
			}
			break;
		}
		if(formatted_time == null) {
			throw	new Exception("DateFormatError:" + strTime);
		} else {
			int	iTime[] = new	int[]{00,00,00};
			int	iMax[] = new int[]{23,59,59};

			String	sTime[] = null;
			if(formatted_time.indexOf(':') != -1) {
				sTime = formatted_time.split(":");
			} else {
				sTime = new String[3];
				sTime[0] = formatted_time.substring(0,2);
				sTime[1] = formatted_time.substring(2,4);
				sTime[2] = formatted_time.substring(4);
			}
			for(int i=0;i<3;i++) {
				iTime[i] = Integer.parseInt(sTime[i]);
				if(iTime[i] > iMax[i]) {
					throw	new Exception("DateFormatError:" + strTime);
				}
			}
			Calendar	tCalendar = Calendar.getInstance();
			tCalendar.set(Calendar.MILLISECOND, 0);
			tCalendar.set(1970,1 - 1,1,iTime[0],iTime[1],iTime[2]);
			return	tCalendar.getTime();
		}
	}
	public	static	Date	parseDatetime(String p_sTimeString) throws Exception
	{
		if(p_sTimeString == null || p_sTimeString.trim().length() == 0)
			return	null;
		p_sTimeString = p_sTimeString.trim();
		if(p_sTimeString.length() == 0)
			return null;
		String formatted_time = null;
		while(true) {
			if(p_sTimeString.length() == 8 && p_sTimeString.matches("\\d{8}")) {
				//YYYYMMDD
				formatted_time = p_sTimeString + "000000";
				break;
			}
			if(p_sTimeString.length() == 12 && p_sTimeString.matches("\\d{12}")) {
				//YYYYMMDDHHNN
				formatted_time = p_sTimeString + "00";
				break;
			}
			if(p_sTimeString.length() > 12 && p_sTimeString.matches("\\d{8}\\s*\\d{4}")) {
				//YYYYMMDD HHNN
				formatted_time = p_sTimeString.replaceAll("[\\s]", "") + "00";
				break;
			}
			if(p_sTimeString.length() == 14 && p_sTimeString.matches("\\d{14}")) {
				//YYYYMMDDHHNNSS
				formatted_time = p_sTimeString;
				break;
			}
			if(p_sTimeString.length() > 14 && p_sTimeString.matches("\\d{8}\\s*\\d{6}")) {
				//YYYYMMDD HHNNSS
				formatted_time = p_sTimeString.replaceAll("[\\s]", "");
				break;
			}
			if(p_sTimeString.length() >= 8 && p_sTimeString.matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
				//YYYY/M/D
				formatted_time = p_sTimeString.replaceAll("/", "-") + "-0-0-0";
				break;
			}
			if(p_sTimeString.length() >=8 && p_sTimeString.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
				//YYYY-M-D
				formatted_time = p_sTimeString + "-0-0-0";
				break;
			}
			if(p_sTimeString.length() >= 12 && p_sTimeString.matches("\\d{4}/\\d{1,2}/\\d{1,2}\\s+\\d{1,2}:\\d{1,2}")) {
				//YYYY/M/D H:N
				formatted_time = p_sTimeString.replaceAll("([/|:])|(\\s+)", "-") + "-0";
				break;
			}
			if(p_sTimeString.length() >= 12 && p_sTimeString.matches("\\d{4}-\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}")) {
				//YYYY-M-D H:N
				formatted_time = p_sTimeString.replaceAll("([:])|(\\s+)", "-") + "-0";
				break;
			}
			if(p_sTimeString.length() >= 14 && p_sTimeString.matches("\\d{4}/\\d{1,2}/\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}")) {
				//YYYY/M/D H:N:S
				formatted_time = p_sTimeString.replaceAll("([:])|(\\s+)", "-");
				break;
			}
			if(p_sTimeString.length() >= 14 && p_sTimeString.matches("\\d{4}-\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}")) {
				//YYYY-M-D H:N:S
				formatted_time = p_sTimeString.replaceAll("([:])|(\\s+)", "-");
				break;
			}
			break;
		}
		if(formatted_time == null) {
			throw	new Exception("DateFormatError:" + p_sTimeString);
		} else {
			String	sTime[] = null;
			if(formatted_time.indexOf('-') != -1) {
				sTime = formatted_time.split("-");
			} else {
				sTime = new String[6];
				sTime[0] = formatted_time.substring(0,4);
				sTime[1] = formatted_time.substring(4,6);
				sTime[2] = formatted_time.substring(6,8);
				sTime[3] = formatted_time.substring(8,10);
				sTime[4] = formatted_time.substring(10,12);
				sTime[5] = formatted_time.substring(12);
			}
			int		iTime[] = {1970,01,01,00,00,00};
			
			for(int i=0;i<iTime.length;i++)
			{
				if(sTime[i] != null)
				{
					iTime[i] = Integer.parseInt(sTime[i].trim());
				}
			}
			if(isLegal(iTime[0],iTime[1],iTime[2],iTime[3],iTime[4],iTime[5]) == false)
				throw	new Exception("DateFormatError:" + p_sTimeString);
			Calendar	tCalendar = Calendar.getInstance();
			tCalendar.set(iTime[0],iTime[1] - 1,iTime[2],iTime[3],iTime[4],iTime[5]);
			tCalendar.set(Calendar.MILLISECOND, 0);
			return	tCalendar.getTime();
		}
	}
}
