package com.incomrecycle.prms.rvm.commonservice;

public interface CommonServiceExecutor {
	public void onCreate(int clientId);
	public void onClose(int clientId);
	public String execute(int clientId, String value);
}
