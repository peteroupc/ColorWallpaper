package com.upokecenter.android.net;

import java.io.File;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import com.upokecenter.android.util.AppManager;
import com.upokecenter.android.util.StorageUtility;
import com.upokecenter.net.IOnFinishedListener;
import com.upokecenter.net.IResponseListener;
import com.upokecenter.net.LightweightDownloadService;
import com.upokecenter.util.IAction;
import com.upokecenter.util.IndexedObjectList;

public final class DownloadService extends Service {

	private static final IndexedObjectList<IResponseListener<Object>> cbobjects=new IndexedObjectList<IResponseListener<Object>>();
	private static final IndexedObjectList<IOnFinishedListener<Object>> finobjects=new IndexedObjectList<IOnFinishedListener<Object>>();


	private static int sendObject(IResponseListener<Object> o) {
		return cbobjects.sendObject(o);
	}
	private static int sendObject(IOnFinishedListener<Object> o) {
		return finobjects.sendObject(o);
	}
	private static Handler handler=null;
	private static Object syncRoot=new Object();
	private static final String EXTRA_URL = "com.upokecenter.android.extra.URL";
	private static final String EXTRA_PROCESS_RESPONSE = "com.upokecenter.android.extra.PROCESS_RESPONSE";
	private static final String EXTRA_ON_FINISHED = "com.upokecenter.android.extra.ON_FINISHED";

	public DownloadService(){
		super();
	}

	public static void sendRequest(
			Context context,
			String url,
			IResponseListener<Object> callback,
			IOnFinishedListener<Object> onFinished
			){
		if(context==null)
			throw new IllegalArgumentException();
		Intent intent = new Intent(context, DownloadService.class);
		intent.putExtra(DownloadService.EXTRA_URL,url);
		intent.putExtra(DownloadService.EXTRA_PROCESS_RESPONSE,DownloadService.sendObject(callback));
		intent.putExtra(DownloadService.EXTRA_ON_FINISHED,DownloadService.sendObject(onFinished));
		context.startService(intent);
	}

	public static void shutdown(Context context){
		context.stopService(new Intent(context, DownloadService.class));
	}

	LightweightDownloadService dservice=null;

	@Override
	public void onCreate(){
		AppManager.initialize(this);
		synchronized(syncRoot){
			if(handler==null) {
				handler=new Handler(getMainLooper());
			}
		}
		File cachePath=StorageUtility.getCachePath(this,null);
		File privateCachePath=StorageUtility.getPrivateCachePath(this,null);
		long defaultSize=StorageUtility.getDefaultCacheSize(this);
		dservice=new LightweightDownloadService(cachePath,privateCachePath,defaultSize);
		dservice.setResultPoster(new IAction<Runnable>(){
			@Override
			public void action(Runnable... parameters) {
				Handler h=null;
				synchronized(syncRoot){
					h=handler;
				}
				h.post(parameters[0]);
			}
		});
		super.onCreate();
	}

	@Override public void onDestroy(){
		dservice.shutdown();
	}

	@SuppressWarnings("unchecked")
	private void startEvent(Intent intent){
		if(intent==null)return;
		final Bundle bundle=new Bundle(intent.getExtras());
		Object cb=cbobjects.receiveObject(bundle.getInt(EXTRA_PROCESS_RESPONSE,-1));
		Object fin=finobjects.receiveObject(bundle.getInt(EXTRA_ON_FINISHED,-1));
		final IResponseListener<Object> cbobj=(cb==null) ? null : (IResponseListener<Object>)cb;
		final IOnFinishedListener<Object> finobj=(fin==null) ? null : (IOnFinishedListener<Object>)fin;
		final String url=bundle.getString(EXTRA_URL);
		dservice.sendRequest(url,cbobj,finobj);
	}

	@Override public int onStartCommand(Intent intent, int flags, int startID){
		startEvent(intent);
		return START_STICKY;
	}

	@Override public void onStart(Intent intent, int startID){
		startEvent(intent);
	}

	@Override
	public IBinder onBind(Intent arg0){
		return null;
	}
}
