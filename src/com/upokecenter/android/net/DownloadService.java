package com.upokecenter.android.net;

import java.io.File;
import java.io.IOException;
import java.net.ResponseCache;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.upokecenter.android.util.AppManager;
import com.upokecenter.android.util.StorageUtility;
import com.upokecenter.net.DownloadHelper;
import com.upokecenter.net.IOnFinishedListener;
import com.upokecenter.net.IProcessResponseListener;
import com.upokecenter.util.IStreamObjectSerializer;
import com.upokecenter.util.IndexedObjectList;

public final class DownloadService extends IntentService {
	
	private static final IndexedObjectList<IStreamObjectSerializer<Object>> serobjects=new IndexedObjectList<IStreamObjectSerializer<Object>>();
	private static final IndexedObjectList<IProcessResponseListener<Object>> cbobjects=new IndexedObjectList<IProcessResponseListener<Object>>();
	private static final IndexedObjectList<IOnFinishedListener<Object>> finobjects=new IndexedObjectList<IOnFinishedListener<Object>>();
	
	
	public static int sendObject(IStreamObjectSerializer<Object> o) {
		return serobjects.sendObject(o);
	}
	public static int sendObject(IProcessResponseListener<Object> o) {
		return cbobjects.sendObject(o);
	}
	public static int sendObject(IOnFinishedListener<Object> o) {
		return finobjects.sendObject(o);
	}
	private static Handler handler=null;
	private static Object syncRoot=new Object();
	

	public static final String EXTRA_URL = "com.upokecenter.android.extra.URL";
	public static final String EXTRA_SERIALIZER = "com.upokecenter.android.extra.SERIALIZER";
	public static final String EXTRA_PROCESS_RESPONSE = "com.upokecenter.android.extra.PROCESS_RESPONSE";
	public static final String EXTRA_ON_FINISHED = "com.upokecenter.android.extra.ON_FINISHED";
	
	public DownloadService(){
		super("DownloadService");
	}

	public DownloadService(String name) {
		super(name);
	}
	
	public static void sendRequest(
			Context context, 
			String url,
			IStreamObjectSerializer<Object> serializer, 
			IProcessResponseListener<Object> callback,
			IOnFinishedListener<Object> onFinished
	){
		Intent intent = new Intent(context, DownloadService.class);
		intent.putExtra(DownloadService.EXTRA_URL,url);
		intent.putExtra(DownloadService.EXTRA_SERIALIZER,DownloadService.sendObject(serializer));
		intent.putExtra(DownloadService.EXTRA_PROCESS_RESPONSE,DownloadService.sendObject(callback));
		intent.putExtra(DownloadService.EXTRA_ON_FINISHED,DownloadService.sendObject(onFinished));
		context.startService(intent);		
	}
	
	@Override
	public void onCreate(){
		AppManager.initialize(this);
		synchronized(syncRoot){
			if(handler==null)handler=new Handler(this.getMainLooper());
		}
		super.onCreate();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onHandleIntent(Intent intent) {
		Object ser=serobjects.receiveObject(intent.getIntExtra(EXTRA_SERIALIZER,-1));
		Object cb=cbobjects.receiveObject(intent.getIntExtra(EXTRA_PROCESS_RESPONSE,-1));
		Object fin=finobjects.receiveObject(intent.getIntExtra(EXTRA_ON_FINISHED,-1));
		IStreamObjectSerializer<Object> serobj=(ser==null) ? null : (IStreamObjectSerializer<Object>)ser;
		final IProcessResponseListener<Object> cbobj=(cb==null) ? null : (IProcessResponseListener<Object>)cb;		
		final IOnFinishedListener<Object> finobj=(fin==null) ? null : (IOnFinishedListener<Object>)fin;		
		final String url=intent.getStringExtra(EXTRA_URL);
		File cachedFile=StorageUtility.getCachePath(
				AppManager.getApplication(),null);
		try {
			final Object value=DownloadHelper.downloadUrlWithCache(
					url,
					(ResponseCache.getDefault())!=null ? null : cachedFile,
					(IStreamObjectSerializer<Object>)serobj,
					(IProcessResponseListener<Object>)cbobj);
			handler.post(new Runnable(){
				@Override
				public void run() {
					if(finobj!=null)
						finobj.onFinished(url,value,null);
				}
			});
		} catch (IOException e) {
			if(finobj!=null)
				finobj.onFinished(url,null,e);
			e.printStackTrace();
		}
	}
}
