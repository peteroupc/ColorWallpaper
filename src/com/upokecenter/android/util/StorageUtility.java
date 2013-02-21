package com.upokecenter.android.util;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import com.upokecenter.util.Reflection;

public final class StorageUtility {
	private StorageUtility(){}
	
	public static File getPrivateCachePath(Context ctx){
		return getPrivateCachePath(ctx,null);
	}

	public static File getCachePath(Context ctx){
		return getCachePath(ctx,null);
	}


	public static File getPrivateCachePath(Context ctx, String name){
		File cacheroot=ctx.getCacheDir();
		if(cacheroot==null){
			return (name==null || name.length()==0) ? null : new File(name);
		} else {
			return (name==null || name.length()==0) ? cacheroot : new File(cacheroot,name);	   
		}
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
			if(cacheroot!=null){
				return (name==null || name.length()==0) ? cacheroot : new File(cacheroot,name);	   
			}
		}
		return getPrivateCachePath(ctx,name);
	}

	public static File getCameraFolderUniqueFileName(){
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			File storagedir=new File(Environment.getExternalStorageDirectory(),"DCIM");
			// For the camera app to save images, the directory must
			// exist: it won't make the directory for us.
			// So check if it exists
			File subdir=new File(storagedir,"Camera");
			if(subdir.isDirectory()){
				return getUniqueFileName(subdir);				
			}
			if(storagedir.isDirectory()){
				return getUniqueFileName(storagedir);
			}
			storagedir=new File(Environment.getExternalStorageDirectory(),"Pictures");
			if(storagedir.isDirectory()){
				return getUniqueFileName(storagedir);
			}
			// Use the external storage directory itself as a last resort
			storagedir=Environment.getExternalStorageDirectory();
			if(storagedir.isDirectory()){
				return getUniqueFileName(storagedir);
			}
		}
		return null;
	}

	public static File getUniqueFileName(File storage){
		if(storage==null)return null;
		Calendar calendar=Calendar.getInstance();
		calendar.setTimeInMillis(new Date().getTime());
		int i=0;
		while(true){
			String string=String.format(Locale.US,
					"%04d-%02d-%02d-%d.png",
					calendar.get(Calendar.YEAR),
					calendar.get(Calendar.MONTH)+1,
					calendar.get(Calendar.DAY_OF_MONTH),i
					);
			File file=new File(storage,string);
			if(!file.exists()){
				return file;
			}
			i++;
		}
	}

	public static long getDefaultCacheSize(Context context){
		ActivityManager mgr=((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE));
		long memory=Runtime.getRuntime().maxMemory();
		long m2=(Integer)Reflection.invokeByName(mgr,"getMemoryClass",-1);
		if(m2>=0){
			memory=Math.min(memory,m2*1024L*1024L);
		}
		DebugUtility.log("%d %d",memory,m2);
		long defaultCacheSize=Math.max(1L*1024L*1024L,memory/4);
		return defaultCacheSize;
	}
}
