package com.upokecenter.android.net;

interface ICacheControl {
	public int getCacheability();
	public boolean isNoStore();
	public boolean isNoTransform();
	public boolean isMustRevalidate();
	public boolean isFresh();
	public IHttpHeaders getHeaders(long contentLength);
}