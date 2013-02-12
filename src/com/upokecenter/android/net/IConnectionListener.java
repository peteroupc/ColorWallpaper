package com.upokecenter.android.net;

import android.content.Context;

public interface IConnectionListener {
	public void onConnectionChanged(Context context, int connectedType);
}