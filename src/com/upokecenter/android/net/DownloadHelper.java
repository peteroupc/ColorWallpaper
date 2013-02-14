package com.upokecenter.android.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;

import com.upokecenter.android.util.AppManager;

public class DownloadHelper {

	Context application;
	int readTimeout=10000;
	int connectTimeout=15000;

	ArrayList<ConnectionReceiver> connListeners=new ArrayList<ConnectionReceiver>();

	private static class ConnectionReceiver {
		WeakReference<Context> context;
		BroadcastReceiver receiver;
		IConnectionListener listener;
		public ConnectionReceiver(Context context,
				BroadcastReceiver receiver, IConnectionListener listener) {
			super();
			this.context = new WeakReference<Context>(context);
			this.receiver = receiver;
			this.listener = listener;
		}
		public boolean matches(Context context, IConnectionListener listener){
			return this.context.get().equals(context) &&
					this.listener==listener;
		}

	}

	public DownloadHelper(){
	}

	public static int getConnectedNetworkType(){
		ConnectivityManager mgr=(ConnectivityManager)AppManager.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo network=mgr.getActiveNetworkInfo();
		if(network==null)return 0;
		if(network.isConnected()){
			return network.getType();
		}
		if(mgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()){
			return ConnectivityManager.TYPE_WIFI;
		}
		return 0;
	}
	public void removeAllConnectionListeners(){
		for(int i=0;i<connListeners.size();i++){
			Context ctx=connListeners.get(i).context.get();
			if(ctx!=null)ctx.unregisterReceiver(connListeners.get(i).receiver);
		}
	}

	public void removeConnectionListener(Context ctx, IConnectionListener listener){
		for(int i=0;i<connListeners.size();i++){
			if(connListeners.get(i).matches(ctx,listener)){
				connListeners.remove(i);
				break;
			}
		}
	}

	public void addConnectionListener(
			Context context,
			final IConnectionListener listener){
		BroadcastReceiver receiver=new BroadcastReceiver(){
			@Override
			public void onReceive(Context ctx, Intent arg1) {
				listener.onConnectionChanged(ctx,getConnectedNetworkType());
			}
		};
		context.registerReceiver(receiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		connListeners.add(new ConnectionReceiver(context,receiver,listener));
		listener.onConnectionChanged(context,getConnectedNetworkType());
	}

	private static class HttpHeaders implements IHttpHeaders {

		HttpURLConnection connection;
		public HttpHeaders(HttpURLConnection connection){
			this.connection=connection;
		}

		@Override
		public String getHeaderField(String name) {
			return connection.getHeaderField(name);
		}

		@Override
		public String getHeaderField(int name) {
			return connection.getHeaderField(name);
		}

		@Override
		public String getHeaderFieldKey(int name) {
			return connection.getHeaderFieldKey(name);
		}

		@Override
		public int getResponseCode() {
			try {
				return connection.getResponseCode();
			} catch (IOException e) {
				return -1;
			}
		}

		@Override
		public long getHeaderFieldDate(String field, long defaultValue) {
			return connection.getHeaderFieldDate(field,defaultValue);
		}

		@Override
		public Map<String, List<String>> getHeaderFields() {
			return connection.getHeaderFields();
		}

		@Override
		public String getRequestMethod() {
			return connection.getRequestMethod();
		}

	}


	private enum DownloadEvent {
		Connecting,
		Connected,
		Finished
	}

	/**
	 * Downloads a URL. Should be called on the UI thread.
	 * @param url
	 * @param callback
	 */
	public <T> void downloadUrl(
			String url, 
			final IDownloadHandler<T> callback
			){
		downloadUrls(Arrays.asList(new String[]{url}),callback);
	}

	public <T> T downloadUrlSynchronous(
			String urlString, 
			final IDownloadHandler<T> callback
			) throws IOException{
		if(urlString==null)throw new NullPointerException();
		if(callback==null)throw new NullPointerException();
		final boolean isEventHandler=(callback instanceof IDownloadEventHandler);
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.FROYO){
			// See documentation for HttpURLConnection for why this
			// is necessary
			System.setProperty("http.keepAlive","false");
		}
		final int readTimeout=this.readTimeout;
		final int connectTimeout=this.connectTimeout;
		new AtomicInteger(0);
		URL url=null;
		try {
			url=new URL(urlString);
		} catch (MalformedURLException e1) {
			throw new IllegalArgumentException(e1);
		}
		InputStream stream=null;
		int network=getConnectedNetworkType();
		if(network==0){
			throw new NoConnectionException();
		}
		try {
			if(isEventHandler)
				((IDownloadEventHandler<T>)callback).onConnecting(url);
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setDoInput(true);
			connection.setChunkedStreamingMode(0);
			connection.setReadTimeout(readTimeout);
			connection.setConnectTimeout(connectTimeout);
			connection.setRequestMethod("GET");
			connection.connect();
			stream = new BufferedInputStream(connection.getInputStream());
			if(isEventHandler)
				((IDownloadEventHandler<T>)callback).onConnected(url);
			T ret=callback.processResponse(url,stream,
					new HttpHeaders(connection));
			return ret;
		} finally {
			if(stream!=null){
				try {
					stream.close();
				} catch (IOException e) {}
			}
		}
	}
	/**
	 * Downloads a list of URLs.  Should be called on the UI thread.
	 * @param urls
	 * @param callback
	 */
	public <T> void downloadUrls(
			List<String> urls, 
			final IDownloadHandler<T> callback
			){
		if(urls==null)throw new NullPointerException();
		if(callback==null)throw new NullPointerException();
		final boolean isEventHandler=(callback instanceof IDownloadEventHandler);
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.FROYO){
			// See documentation for HttpURLConnection for why this
			// is necessary
			System.setProperty("http.keepAlive","false");
		}
		// Checking all URLs
		final int count=urls.size();
		final int readTimeout=this.readTimeout;
		final int connectTimeout=this.connectTimeout;
		final AtomicInteger counter=new AtomicInteger(0);
		for(String urlString : urls){
			try {
				new URL(urlString);
			} catch (MalformedURLException e1) {
				throw new IllegalArgumentException(e1);
			}
		}
		// Downloading the URLs
		for(String urlString : urls){
			final URL url;
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e1) {
				continue;
			}
			final AtomicBoolean noerrors=new AtomicBoolean(false);
			new AsyncTask<URL,Object,Object>(){
				@Override
				protected Object doInBackground(URL... url) {
					InputStream stream=null;
					publishProgress(DownloadEvent.Connecting,url[0]);
					int network=getConnectedNetworkType();
					if(network==0){
						publishProgress(DownloadEvent.Finished,url[0],null,
								new NoConnectionException(),
								counter.getAndIncrement(),count);
						return null;
					}
					try {
						HttpURLConnection connection = (HttpURLConnection)url[0].openConnection();
						connection.setDoInput(true);
						connection.setReadTimeout(readTimeout);
						connection.setConnectTimeout(connectTimeout);
						connection.setRequestMethod("GET");
						connection.connect();
						stream = new BufferedInputStream(connection.getInputStream());
						if(isEventHandler)
							publishProgress(DownloadEvent.Connected,url[0]);
						T ret=callback.processResponse(url[0],stream,
								new HttpHeaders(connection));
						publishProgress(DownloadEvent.Finished,url[0],ret,null,
								counter.getAndIncrement(),count);
						noerrors.set(true);
						return null;
					} catch(IOException e){
						//e.printStackTrace();
						publishProgress(DownloadEvent.Finished,url[0],null,e,
								counter.getAndIncrement(),count);
						return null;
					} finally {
						if(stream!=null){
							try {
								stream.close();
							} catch (IOException e) {}
						}
					}
				}

				@SuppressWarnings("unchecked")
				@Override
				protected void onProgressUpdate(Object... progress) {
					DownloadEvent event=(DownloadEvent)progress[0];
					if(isEventHandler){
						if(event.equals(DownloadEvent.Connecting))
							((IDownloadEventHandler<T>)callback).onConnecting((URL)progress[1]);
						if(event.equals(DownloadEvent.Connected))
							((IDownloadEventHandler<T>)callback).onConnected((URL)progress[1]);
					}
					if(event.equals(DownloadEvent.Finished))
						callback.onFinished((URL)progress[1],
								(T)progress[2],
								(IOException)progress[3],
								(Integer)progress[4],
								(Integer)progress[5]
								);
				}

			}.execute(url);
		}
	}
}
