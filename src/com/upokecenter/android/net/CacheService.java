package com.upokecenter.android.net;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.upokecenter.android.util.AppManager;
import com.upokecenter.android.util.StorageUtility;
import com.upokecenter.net.LightweightCacheService;
import com.upokecenter.util.Reflection;

public class CacheService extends Service {

	public static final String EXTRA_CACHE_SIZE = "com.upokecenter.android.extra.CACHE_SIZE";
	

	LightweightCacheService service=null;
	
	public long getDefaultCacheSize(){
		ActivityManager mgr=((ActivityManager)getSystemService(Context.ACTIVITY_SERVICE));
		long memory=Runtime.getRuntime().maxMemory();
		long m2=(Long)Reflection.invokeByName(mgr,"getMemoryClass",-1);
		if(m2>=0){
			memory=Math.min(memory,m2*1024L*1024L);
		}
		long defaultCacheSize=Math.max(1L*1024L*1024L,memory/4);
		return defaultCacheSize;
	}
	
	@Override public void onCreate(){
		AppManager.initialize(this);
		service=new LightweightCacheService(StorageUtility.getCachePath(this,null),
				StorageUtility.getPrivateCachePath(this,null));
		service.setCacheSize(getDefaultCacheSize());
	}
	
	private void doOnStart(Intent intent){
		if(intent!=null && intent.hasExtra(EXTRA_CACHE_SIZE)){
			service.setCacheSize(intent.getLongExtra(EXTRA_CACHE_SIZE,getDefaultCacheSize()));
		}		
	}
	
	@Override public void onStart(Intent intent, int startId){
		doOnStart(intent);
	}
	@Override public int onStartCommand(Intent intent, int flags, int startId){
		doOnStart(intent);
		return START_STICKY;
	}
	@Override public void onDestroy(){
		service.close();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}	
}
