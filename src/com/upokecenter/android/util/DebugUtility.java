package com.upokecenter.android.util;

import android.util.Log;

public final class DebugUtility {
	private DebugUtility(){}
	
	public static void log(String format, Object... items){
		Log.i("CWS",String.format(format,items));
	}
	public static void log(Object item){
		Log.i("CWS",String.format("%s",item));
	}
}
