package com.upokecenter.net;

public interface IDownloadEventListener<T> extends IProcessResponseListener<T> {
	public void onConnecting(String url);
	public void onConnected(String url);
}
