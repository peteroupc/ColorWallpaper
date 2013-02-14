package com.upokecenter.android.net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;

import com.upokecenter.android.util.AppManager;
import com.upokecenter.android.util.DebugUtility;
import com.upokecenter.util.HeaderParser;
import com.upokecenter.util.IStreamObjectSerializer;
import com.upokecenter.util.Reflection;
import com.upokecenter.util.StreamUtility;

public final class CacheHelper {

	private CacheHelper(){}

	private static class NullHeaders implements IHttpHeaders {

		long date,length;

		public NullHeaders(long length){
			this.date=new Date().getTime();
			this.length=length;
		}
		
		@Override
		public String getRequestMethod() {
			return "GET";
		}

		@Override
		public String getHeaderField(String name) {
			if(name==null)return "HTTP/1.1 200 OK";
			if("date".equals(name.toLowerCase(Locale.US)))
				return HeaderParser.formatDate(date);
			if("length".equals(name.toLowerCase(Locale.US)))
				return Long.toString(length);
			return null;
		}

		@Override
		public String getHeaderField(int name) {
			if(name==0)
				return getHeaderField("date");
			if(name==1)
				return getHeaderField("content-length");
			return null;
		}

		@Override
		public String getHeaderFieldKey(int name) {
			if(name==0)
				return "date";
			if(name==1)
				return "content-length";
			return null;
		}

		@Override
		public int getResponseCode() {
			return 200;
		}

		@Override
		public long getHeaderFieldDate(String field, long defaultValue) {
			if(field!=null && "date".equals(field.toLowerCase(Locale.US)))
				return date;
			return defaultValue;
		}

		@Override
		public Map<String, List<String>> getHeaderFields() {
			Map<String, List<String>> map=new HashMap<String, List<String>>();
			map.put(null,Arrays.asList(new String[]{getHeaderField(null)}));
			map.put("date",Arrays.asList(new String[]{getHeaderField("date")}));
			map.put("content-length",Arrays.asList(new String[]{getHeaderField("content-length")}));
			return Collections.unmodifiableMap(map);
		}

	}

	private static void recursiveListFiles(File file, List<File> files){
		for(File f : file.listFiles()){
			if(f.isDirectory()){
				recursiveListFiles(f,files);
			}
			files.add(f);
		}
	}

	public static void pruneCache(long maximumSize){
		File cache=CacheHelper.getCachePath(AppManager.getApplication(),null);
		if(cache==null || !cache.isDirectory())return;
		while(true){
			long length=0;
			boolean exceeded=false;
			long oldest=Long.MAX_VALUE;
			int count=0;
			List<File> files=new ArrayList<File>();
			recursiveListFiles(cache,files);
			for(File file : files){
				if(file.isFile()){
					length+=file.length();
					if(length>maximumSize){
						exceeded=true;
					}
					oldest=file.lastModified();
					count++;
				}
			}
			if(count<=1||!exceeded)return;
			long threshold=oldest+Math.abs(oldest-new Date().getTime())/2;
			for(File file : files){
				if(file.lastModified()<threshold){
					if(file.isDirectory()){
						file.delete();
					} else {
						length-=file.length();
						file.delete();
						if(length<maximumSize){
							return;
						}
					}
				}
			}
		}
	}
	

	 static boolean enableHttpCache(long sizeInBytes){
		try {
			return enableHttpCacheInternal(sizeInBytes);
		} catch(IOException e){
			DebugUtility.log("Can't enable cache with size %d",sizeInBytes);
			return false;
		}
	}

	private static boolean enableHttpCacheInternal(long sizeInBytes) throws IOException{
		Context ctx=AppManager.getApplication();
		File cacheDir=CacheHelper.getCachePath(ctx,"httpcache");
		if(cacheDir==null)return false;
		cacheDir.mkdirs();
		// HttpResponseCache added in ICS
		Class<?> clazz=Reflection.getClassForName("android.net.http.HttpResponseCache");
		if(clazz==null)return false;
		Object o=null;
		o=Reflection.invokeStaticByName(clazz,"install",null,cacheDir,sizeInBytes);
		return (o!=null);
	}

	
	public static <T> void getCachedData(
			final String urlString,
			String filename,
			final IStreamObjectSerializer<T> serializer,
			final IDownloadHandler<T> callback
			){
		final URL url;
		try {
			url=new URL(urlString);
		} catch (MalformedURLException e1) {
			throw new IllegalArgumentException(e1);
		}
		final File cachedFile=CacheHelper.getCachePath(
				AppManager.getApplication(),filename);
		final File cacheInfoFile=CacheHelper.getCachePath(
				AppManager.getApplication(),filename+".cache");
		final boolean isPrivate=cachedFile.toString().startsWith("/data/");
		final AtomicReference<IOException> lastException=new AtomicReference<IOException>(null);
		new AsyncTask<File,Void,T>(){
			@Override
			protected T doInBackground(File... file) {
				if(file[0].exists()){
					boolean fresh=false;
					IHttpHeaders headers=null;
					if(cacheInfoFile.isFile()){
						try {
							ICacheControl cc=CacheControl.fromFile(cacheInfoFile);
							fresh=(cc==null) ? false : cc.isFresh();
							headers=(cc==null) ? new NullHeaders(file[0].length()) : cc.getHeaders(file[0].length());
						} catch (IOException e) {
							e.printStackTrace();
							fresh=false;
							headers=new NullHeaders(file[0].length());
						}
						DebugUtility.log("freshness: %s",fresh);
					} else {
						long maxAgeMillis=24L*3600L*1000L;
						long timeDiff=Math.abs(file[0].lastModified()-(new Date().getTime()));
						fresh=(timeDiff<=maxAgeMillis);
						headers=new NullHeaders(file[0].length());
					}
					if(!fresh){
						// Too old, download again
						cachedFile.delete();
						cacheInfoFile.delete();
					} else {
						try {
								T value=null;
								InputStream newStream=null;
								try {
									newStream=new BufferedInputStream(new FileInputStream(cachedFile));
									if(serializer!=null)
										value=serializer.readObjectFromStream(newStream);
									else
										value=callback.processResponse(url,newStream,headers);
								} finally {
									if(newStream!=null)newStream.close();
								}
								return value;
						} catch (IOException e) {
							// if we get an exception here, we download again
						}
					}
				}
				try {
					final long requestTime=new Date().getTime();
					T value=new DownloadHelper().downloadUrlSynchronous(urlString,
							new IDownloadHandler<T>(){

						@Override
						public T processResponse(URL url,
								InputStream stream, IHttpHeaders headers)
										throws IOException {
							ICacheControl cc=CacheControl.getCacheControl(headers,requestTime);
							if(serializer==null){
								// we cache the raw data and read from disk
								if(cc==null || cc.getCacheability()<=0 || cc.isNoStore()){
									throw new ProtocolException();
								}
								if(!isPrivate && cc.getCacheability()==1){
									throw new ProtocolException();									
								}
								new File(cachedFile.getParent()).mkdirs();
								StreamUtility.inputStreamToFile(stream,cachedFile);
								CacheControl.toFile(cc,cacheInfoFile);
								InputStream newStream=null;
								T value=null;
								try {
									newStream=new BufferedInputStream(new FileInputStream(cachedFile));
									value=callback.processResponse(url,newStream,headers);
								} finally {
									if(newStream!=null)newStream.close();
								}
								return value;
							} else {
								// we read the stream directly and cache a
								// serialized version
								T value=callback.processResponse(url,stream,headers);
								if(value!=null && cc!=null && 
										(cc.getCacheability()==2 || (isPrivate && cc.getCacheability()==1)) &&
										!cc.isNoTransform() && !cc.isNoStore()){
									new File(cachedFile.getParent()).mkdirs();
									try {
										OutputStream fs=new FileOutputStream(cachedFile);
										try {
											serializer.writeObjectToStream(value,fs);
										} finally {
											if(fs!=null)fs.close();
										}
										CacheControl.toFile(cc,cacheInfoFile);
									} catch(IOException e){
										// ignore, we don't care much if caching fails
									}
								}
								return value;
							}
						}

						@Override
						public void onFinished(URL url, T value,
								IOException exception, int progress,
								int total) {
						}
					});
					lastException.set(null);
					return value;
				} catch (IOException e) {
					lastException.set(e);
					return null;
				}
			}

			@Override
			protected void onPostExecute(T value){
				IOException lastexc=lastException.get();
				if(lastexc!=null){
					callback.onFinished(url,null,lastexc,1,1);
				} else {
					callback.onFinished(url,value,null,1,1);
				}
			}
		}.execute(cachedFile);
	}

	public static File getCachePath(Context ctx){
		return getCachePath(ctx,null);
	}
	
	
	public static File getCachePath(Context ctx, String name){
		File cacheroot=null;
		// Check if we can write to external storage
		if(ctx.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==
				PackageManager.PERMISSION_GRANTED){
			// getExternalCacheDir added in API level 8 (Froyo)
			cacheroot=(File)Reflection.invokeByName(ctx,"getExternalCacheDir",null);
			if(cacheroot==null){
				cacheroot=new File(Environment.getExternalStorageDirectory(),"Android");
				cacheroot=new File(cacheroot,"data");
				cacheroot=new File(cacheroot,ctx.getApplicationInfo().packageName);
				cacheroot=new File(cacheroot,"cache");			
			}
			// isExternalStorageRemovable added in API level 9 (Gingerbread)
			String mounted=Environment.MEDIA_MOUNTED;
			boolean removable=(Boolean)Reflection.invokeStaticByName(Environment.class,
					"isExternalStorageRemovable",true);
			if(removable && (!mounted.equals(Environment.getExternalStorageState()))){
				cacheroot=null;
			}
		}
		if(cacheroot==null){
			cacheroot=ctx.getCacheDir();
		}
		if(cacheroot==null){
			return (name==null || name.length()==0) ? null : new File(name);
		} else {
			return (name==null || name.length()==0) ? cacheroot : new File(cacheroot,name);	   
		}
	}


}
