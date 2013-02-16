package com.upokecenter.net;

import java.io.File;
import java.io.IOException;
import java.net.ResponseCache;
import java.util.Date;

import com.upokecenter.util.Reflection;

public final class LightweightCacheService {

	private boolean enableHttpCache(long sizeInBytes){
		try {
			return enableHttpCacheInternal(sizeInBytes);
		} catch(IOException e){
			return false;
		}
	}

	private boolean enableHttpCacheInternal(long sizeInBytes) throws IOException{
		File cacheDir=this.filePublicPath;
		if(cacheDir==null)return false;
		cacheDir=new File(this.filePublicPath,"httpcache");
		cacheDir.mkdirs();
		// HttpResponseCache added in ICS
		Class<?> clazz=Reflection.getClassForName("android.net.http.HttpResponseCache");
		if(clazz==null && ResponseCache.getDefault()==null){
			ResponseCache legacyCache=DownloadHelper.getLegacyResponseCache(cacheDir);
			ResponseCache.setDefault(legacyCache);
			return (ResponseCache.getDefault()!=null);
		}
		Object o=null;
		o=Reflection.invokeStaticByName(clazz,"install",null,cacheDir,sizeInBytes);
		return (o!=null);
	}

	Thread thread=null;
	long cacheSize=2L*1024L*1024L;
	Object syncRoot=new Object();
	long lastTime=0;
	final long INTERVAL = 1000L * 60L * 5L;		
	File filePublicPath;
	File filePrivatePath;

	private static void nanoSleep(long nanos){
		long lastTime=System.nanoTime();
		do {
			try {
				Thread.sleep(nanos/1000000L,(int)(nanos%1000000L));
				return;
			} catch(InterruptedException e){
				long newTime=System.nanoTime();
				nanos-=Math.abs(newTime-lastTime);
				lastTime=newTime;
			}
		} while(nanos>0);
	}

	public LightweightCacheService(File publicPath, File privatePath){
		this.filePublicPath=publicPath;
		this.filePrivatePath=privatePath;
		thread=new Thread(new Runnable(){
			@Override
			public void run() {
				long size=0;
				lastTime=new Date().getTime();
				synchronized(syncRoot){
					size=cacheSize;
				}
				enableHttpCache(size);
				nanoSleep(5000L*1000000L);
				while(true){
					synchronized(syncRoot){
						size=cacheSize;
					}
					if(ResponseCache.getDefault()==null){
						enableHttpCache(size);
						DownloadHelper.pruneCache(filePublicPath,size);						
						DownloadHelper.pruneCache(filePrivatePath,size);						
					}
					nanoSleep(INTERVAL*1000000L);
				}
			}
		});
		thread.start();
	}
	public void close(){
		thread=null;
	}
	public void setCacheSize(long size){
		synchronized(syncRoot){ 
			cacheSize=Math.max(size,0);
		}			
	}
}