package com.upokecenter.android.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.upokecenter.android.util.DebugUtility;
import com.upokecenter.util.HeaderParser;
import com.upokecenter.util.IStreamObjectSerializer;
import com.upokecenter.util.StreamUtility;



class CacheControl implements ICacheControl {

	@Override
	public String toString() {
		return "CacheControl [cacheability=" + cacheability + ", noStore="
				+ noStore + ", noTransform=" + noTransform
				+ ", mustRevalidate=" + mustRevalidate + ", requestTime="
				+ requestTime + ", responseTime=" + responseTime + ", maxAge="
				+ maxAge + ", date=" + date + ", age=" + age + ", code=" + code
				+ ", headerFields=" + headers + "]";
	}
	private int cacheability=0;
	// Client must not store the response
	// to disk and must remove it from memory
	// as soon as it's finished with it
	private boolean noStore=false;
	// Client must not convert the response
	// to a different format before caching it
	private boolean noTransform=false;
	// Client must re-check the server
	// after the response becomes stale
	private boolean mustRevalidate=false;
	private long requestTime=0;
	private long responseTime=0;
	private long maxAge=0;
	private long date=0;
	private long age=0;
	private int code=0;
	private List<String> headers;

	@Override
	public int getCacheability() {
		return cacheability;
	}
	@Override
	public boolean isNoStore() {
		return noStore;
	}
	@Override
	public boolean isNoTransform() {
		return noTransform;
	}
	@Override
	public boolean isMustRevalidate() {
		return mustRevalidate;
	}

	private long getAge(){
		long now=new Date().getTime();
		long age=Math.max(0,Math.max(now-this.date,this.age));
		age+=(this.responseTime-this.requestTime);
		age+=(now-this.responseTime);
		age=(age>Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)age;
		return age;
	}

	@Override
	public boolean isFresh() {
		if(this.cacheability==0 || this.noStore)return false;
		return (this.maxAge>getAge());
	}
	
	private CacheControl(){
		this.headers=new ArrayList<String>();
	}

	public static ICacheControl getCacheControl(IHttpHeaders headers, long requestTime){
		CacheControl cc=new CacheControl();
		boolean proxyRevalidate=false;
		int sMaxAge=0;
		boolean publicCache=false;
		boolean privateCache=false;
		boolean noCache=false;
		long expires=0;
		boolean hasExpires=false;
		String cacheControl=headers.getHeaderField("cache-control");
		if(cacheControl!=null){
			int index=0;
			int[] intval=new int[1];
			while(index<cacheControl.length()){
				int current=index;
				if((index=HeaderParser.parseToken(cacheControl,current,"private",true))!=current){
					privateCache=true;
				} else if((index=HeaderParser.parseToken(cacheControl,current,"no-cache",true))!=current){
					noCache=true;
					DebugUtility.log("returning early because it saw no-cache");
					return null; // return immediately, this is not cacheable
				} else if((index=HeaderParser.parseToken(
						cacheControl,current,"no-store",false))!=current){
					cc.noStore=true;
					DebugUtility.log("returning early because it saw no-store");
					return null; // return immediately, this is not cacheable or storable
				} else if((index=HeaderParser.parseToken(
						cacheControl,current,"public",false))!=current){
					publicCache=true;
				} else if((index=HeaderParser.parseToken(
						cacheControl,current,"no-transform",false))!=current){
					cc.noTransform=true;
				} else if((index=HeaderParser.parseToken(
						cacheControl,current,"must-revalidate",false))!=current){
					cc.mustRevalidate=true;
				} else if((index=HeaderParser.parseToken(
						cacheControl,current,"proxy-revalidate",false))!=current){
					proxyRevalidate=true;
				} else if((index=HeaderParser.parseTokenWithDelta(
						cacheControl,current,"max-age",intval))!=current){
					cc.maxAge=intval[0];
				} else if((index=HeaderParser.parseTokenWithDelta(
						cacheControl,current,"s-maxage",intval))!=current){
					sMaxAge=intval[0];
				} else {
					index=HeaderParser.skipDirective(cacheControl,current);
				}
			}
			if(!publicCache && !privateCache && !noCache){
				noCache=true;
			}
		} else {
			int code=headers.getResponseCode();
			if((code==200 || code==203 || code==300 || code==301 || code==410) &&
					headers.getHeaderField("authorization")==null){
				publicCache=true;
				privateCache=false;
			} else {
				noCache=true;
			}
		}
		if(headers.getResponseCode()==206)
			noCache=true;
		String pragma=headers.getHeaderField("pragma");
		if(pragma!=null && "no-cache".equals(pragma.toLowerCase(Locale.US))){
			noCache=true;
			DebugUtility.log("returning early because it saw pragma no-cache");
			return null;
		}
		long now=new Date().getTime();
		cc.code=headers.getResponseCode();
		cc.date=now;
		cc.responseTime=now;
		cc.requestTime=requestTime;
		if(proxyRevalidate){
			// Enable must-revalidate for simplicity;
			// proxyRevalidate usually only applies to shared caches
			cc.mustRevalidate=true;
		}
		if(headers.getHeaderField("date")!=null){
			cc.date=headers.getHeaderFieldDate("date",Long.MIN_VALUE);
			if(cc.date==Long.MIN_VALUE)noCache=true;
		} else {
			noCache=true;
		}
		String expiresHeader=headers.getHeaderField("expires");
		if(expiresHeader!=null){
			expires=headers.getHeaderFieldDate("expires",Long.MIN_VALUE);			
			hasExpires=(cc.date!=Long.MIN_VALUE);
		}
		if(headers.getHeaderField("age")!=null){
			try {
				cc.age=Integer.parseInt(headers.getHeaderField("age"));
				if(cc.age<0)cc.age=0;
			} catch(NumberFormatException e){
				cc.age=-1;
			}
		}
		if(cc.maxAge>0 || sMaxAge>0){
			long maxAge=cc.maxAge; // max age in seconds
			if(maxAge==0)maxAge=sMaxAge;
			if(cc.maxAge>0 && sMaxAge>0){
				maxAge=Math.max(cc.maxAge,sMaxAge);
			}
			cc.maxAge=maxAge*1000L; // max-age and s-maxage are in seconds
			hasExpires=false;
		} else if(hasExpires && !noCache){
			long maxAge=expires-cc.date;
			cc.maxAge=(maxAge>Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)maxAge;
		} else if(noCache || cc.noStore){
			cc.maxAge=0;
		} else {
			cc.maxAge=24L*3600L*1000L;
		}
		String reqmethod=headers.getRequestMethod();
		if(reqmethod==null || (
				!reqmethod.toUpperCase(Locale.US).equals("GET") &&
				!reqmethod.toUpperCase(Locale.US).equals("HEAD"))){
			// caching responses other than GET and HEAD responses not supported
			return null;
		}
		cc.cacheability=2;
		if(noCache)cc.cacheability=0;
		else if(privateCache)cc.cacheability=1;
		int i=0;
		cc.headers.add(headers.getHeaderField(null));
		while(true){
			String newValue=headers.getHeaderField(i);
			if(newValue==null)break;
			String key=headers.getHeaderFieldKey(i);
			i++;
			if(key==null){
				DebugUtility.log("null key");
				continue;
			}
			key=key.toLowerCase(Locale.US);
			// to simplify matters, don't include Age header fields;
			// so-called hop-by-hop headers are also not included
			if(!"age".equals(key) &&
					!"connection".equals(key) &&
					!"keep-alive".equals(key) &&
					!"proxy-authenticate".equals(key) &&
					!"proxy-authorization".equals(key) &&
					!"te".equals(key) &&
					!"trailers".equals(key) &&
					!"transfer-encoding".equals(key) &&
					!"upgrade".equals(key)){
				cc.headers.add(key);
				cc.headers.add(newValue);				
			}
		}
		DebugUtility.log("final cc: %s",cc);
		return cc;
	}

	public static ICacheControl fromFile(File f) throws IOException{
		InputStream fs=new FileInputStream(f);
		try {
			return new CacheControlSerializer().readObjectFromStream(fs);
		} finally {
			if(fs!=null)fs.close();
		}
	}

	public static void toFile(ICacheControl o, File file) throws IOException{
		OutputStream fs=new FileOutputStream(file);
		try {
			new CacheControlSerializer().writeObjectToStream((CacheControl)o,fs);
		} finally {
			if(fs!=null)fs.close();
		}
	}

	static class CacheControlSerializer implements IStreamObjectSerializer<CacheControl>{
		@Override
		public CacheControl readObjectFromStream(InputStream stream) throws IOException {
			try {
				JSONObject obj=new JSONObject(StreamUtility.streamToString(stream));
				CacheControl cc=new CacheControl();
				cc.cacheability=obj.getInt("cacheability");
				cc.noStore=obj.getBoolean("noStore");
				cc.noTransform=obj.getBoolean("noTransform");
				cc.mustRevalidate=obj.getBoolean("mustRevalidate");
				cc.requestTime=obj.getLong("requestTime");
				cc.responseTime=obj.getLong("responseTime");
				cc.maxAge=obj.getLong("maxAge");
				cc.date=obj.getLong("date");
				cc.code=obj.getInt("code");
				cc.age=obj.getLong("age");
				cc.headers=new ArrayList<String>();
				JSONArray arr=obj.getJSONArray("headers");
				for(int i=0;i<arr.length();i++){
					String str=arr.getString(i);
					if(str!=null && i%2==1){
						str=str.toLowerCase(Locale.US);
						if("age".equals(str) ||
								"connection".equals(str) ||
								"keep-alive".equals(str) ||
								"proxy-authenticate".equals(str) ||
								"proxy-authorization".equals(str) ||
								"te".equals(str) ||
								"trailers".equals(str) ||
								"transfer-encoding".equals(str) ||
								"upgrade".equals(str)){
							// Skip "age" header field and
							// hop-by-hop header fields
							i++;
							continue;
						}
					}
					cc.headers.add(str);
				}
				return cc;
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		@Override
		public void writeObjectToStream(CacheControl o, OutputStream stream)
				throws IOException {
			JSONObject obj=new JSONObject();
			try {
				obj.put("cacheability",o.cacheability);
				obj.put("noStore",o.noStore);
				obj.put("noTransform",o.noTransform);
				obj.put("mustRevalidate",o.mustRevalidate);
				obj.put("requestTime",o.requestTime);
				obj.put("responseTime",o.responseTime);
				obj.put("maxAge",o.maxAge);
				obj.put("date",o.date);
				obj.put("code",o.code);
				obj.put("age",o.age);
				JSONArray arr=new JSONArray();
				for(String header : o.headers){
					arr.put(header);
				}
				obj.put("headers",arr);
				StreamUtility.stringToStream(obj.toString(),stream);
			} catch (JSONException e) {
				throw (IOException)new IOException().initCause(e);
			}
		}
	}

	@Override
	public IHttpHeaders getHeaders(long length) {
		return new AgedHeaders(this,this.getAge(),length);
	}
	
	private static class AgedHeaders implements IHttpHeaders {

		CacheControl cc=null;
		long age=0;
		List<String> list=new ArrayList<String>();

		public AgedHeaders(CacheControl cc, long age, long length){
			list.add(cc.headers.get(0));
			for(int i=1;i<cc.headers.size();i+=2){
				String key=cc.headers.get(i);
				if(key!=null){
					key=key.toLowerCase(Locale.US);
					if("content-length".equals(key)||"age".equals(key))
						continue;
				}
				list.add(cc.headers.get(i));
				list.add(cc.headers.get(i+1));
			}
			this.age=age/1000; // convert age to seconds
			list.add("age");
			list.add(Long.toString(this.age));
			list.add("content-length");
			list.add(Long.toString(length));
			this.cc=cc;
		}

		@Override
		public String getRequestMethod() {
			return "GET";
		}
		@Override
		public String getHeaderField(String name) {
			if(name==null)return cc.headers.get(0);
			name=name.toLowerCase(Locale.US);
			for(int i=1;i<cc.headers.size();i+=2){
				String key=cc.headers.get(i);
				if(name.equals(key)){
					return cc.headers.get(i+1);
				}
			}
			return null;
		}
		@Override
		public String getHeaderField(int index) {
			index=(index)*2+1+1;
			if(index<0 || index>=cc.headers.size())
				return null;
			return cc.headers.get(index+1);
		}
		@Override
		public String getHeaderFieldKey(int index) {
			index=(index)*2+1;
			if(index<0 || index>=cc.headers.size())
				return null;
			return cc.headers.get(index);
		}
		@Override
		public int getResponseCode() {
			return cc.code;
		}
		@Override
		public long getHeaderFieldDate(String field, long defaultValue) {
			return HeaderParser.parseDate(getHeaderField(field),defaultValue);
		}
		@Override
		public Map<String, List<String>> getHeaderFields() {
			Map<String, List<String>> map=new HashMap<String, List<String>>();
			map.put(null,Arrays.asList(new String[]{cc.headers.get(0)}));
			for(int i=1;i<cc.headers.size();i++){
				String key=cc.headers.get(i);
				List<String> list=map.get(key);
				if(list==null){
					list=new ArrayList<String>();
					map.put(key,list);
				}
				list.add(cc.headers.get(i+1));
			}
			return Collections.unmodifiableMap(map);
		}
	}
}