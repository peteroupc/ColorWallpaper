package com.upokecenter.android.net;

import java.net.URL;

public interface IDownloadEventHandler<T> extends IDownloadHandler<T> {
	public void onConnecting(URL url);
	public void onConnected(URL url);
}
