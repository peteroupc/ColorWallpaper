package com.upokecenter.android.net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.upokecenter.android.util.AppManager;
import com.upokecenter.util.IFileObjectSerializer;
import com.upokecenter.util.Reflection;

public final class CacheHelper {

	private CacheHelper(){}

	private static class NullHeaders implements IHttpHeaders {

		@Override
		public String getRequestMethod() {
			return "GET";
		}

		@Override
		public String getHeaderField(String name) {
			return null;
		}

		@Override
		public String getHeaderField(int name) {
			return null;
		}

		@Override
		public String getHeaderFieldKey(int name) {
			return null;
		}

		@Override
		public int getResponseCode() {
			return 200;
		}

		@Override
		public long getHeaderFieldDate(String field, long defaultValue) {
			return 0;
		}

		@Override
		public Map<String, List<String>> getHeaderFields() {
			return new HashMap<String, List<String>>();
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
	
	private static void inputStreamToFile(InputStream stream, File file)
	throws IOException {
		FileOutputStream output=null;
		try {
			output=new FileOutputStream(file);
			byte[] buffer=new byte[8192];
			while(true){
				int count=stream.read(buffer,0,buffer.length);
				if(count<0)break;
				output.write(buffer,0,count);
			}
		} finally {
			if(output!=null)output.close();
		}
	}

	public static InputStream getCachedInputStream(
			final String urlString,
			String filename,
			final long maxAgeMillis
			){
		try {
			new URL(urlString);
		} catch (MalformedURLException e1) {
			throw new IllegalArgumentException(e1);
		}
		final File cachedFile=CacheHelper.getCachePath(
				AppManager.getApplication(),filename);
		if(cachedFile.exists()){
			long timeDiff=Math.abs(cachedFile.lastModified()-(new Date().getTime()));
			if(timeDiff>maxAgeMillis){
				// Too old, download again
				cachedFile.delete();
			} else {
				try {
					return new FileInputStream(cachedFile);
				} catch (IOException e) {
					// if we get an exception here, we download again
				}
			}
		}
		try {
			return new DownloadHelper().downloadUrlSynchronous(urlString,
					new IDownloadHandler<InputStream>(){
				@Override
				public InputStream processResponse(URL url,
						InputStream stream, IHttpHeaders headers)
								throws IOException {
					new File(cachedFile.getParent()).mkdirs();
					inputStreamToFile(stream,cachedFile);
					return new FileInputStream(cachedFile);
				}

				@Override
				public void onFinished(URL url, InputStream value,
						IOException exception, int progress,
						int total) {
				}
			});
		} catch (IOException e) {
			return null;
		}
	}


	public static <T> void getCachedData(
			final String urlString,
			String filename,
			final long maxAgeMillis,
			final IFileObjectSerializer<T> serializer,
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
		final AtomicReference<IOException> lastException=new AtomicReference<IOException>(null);
		new AsyncTask<File,Void,T>(){
			@Override
			protected T doInBackground(File... file) {
				if(file[0].exists()){
					long timeDiff=Math.abs(file[0].lastModified()-(new Date().getTime()));
					if(timeDiff>maxAgeMillis){
						// Too old, download again
						cachedFile.delete();
					} else {
						try {
							if(serializer==null){
								T value=null;
								InputStream newStream=null;
								try {
									newStream=new BufferedInputStream(new FileInputStream(cachedFile));
									value=callback.processResponse(url,newStream,new NullHeaders());
								} finally {
									if(newStream!=null)newStream.close();
								}
								return value;
							} else {
								return serializer.readObjectFromFile(file[0]);
							}
						} catch (IOException e) {
							// if we get an exception here, we download again
						}
					}
				}
				try {
					T value=new DownloadHelper().downloadUrlSynchronous(urlString,
							new IDownloadHandler<T>(){

						@Override
						public T processResponse(URL url,
								InputStream stream, IHttpHeaders headers)
										throws IOException {
							new File(cachedFile.getParent()).mkdirs();
							if(serializer==null){
								// we cache the raw data and read from disk
								inputStreamToFile(stream,cachedFile);
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
								if(value!=null){
									try {
										serializer.writeObjectToFile(value,cachedFile);
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

	public static File getCachePath(Context ctx, String name){
		// getExternalCacheDir added in API level 8 (Froyo)
		File cacheroot=(File)Reflection.invokeByName(ctx,"getExternalCacheDir",null);
		String mounted=Environment.MEDIA_MOUNTED;
		if(cacheroot==null){
			cacheroot=new File(Environment.getExternalStorageDirectory(),"Android");
			cacheroot=new File(cacheroot,"data");
			cacheroot=new File(cacheroot,ctx.getApplicationInfo().packageName);
			cacheroot=new File(cacheroot,"cache");			
		}
		// isExternalStorageRemovable added in API level 9 (Gingerbread)
		boolean removable=(Boolean)Reflection.invokeStaticByName(Environment.class,
				"isExternalStorageRemovable",true);
		if(removable && (!mounted.equals(Environment.getExternalStorageState()))){
			cacheroot=null;
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
