package com.upokecenter.android.colorwallpaper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.upokecenter.android.net.CacheHelper;
import com.upokecenter.android.net.IDownloadHandler;
import com.upokecenter.android.net.IHttpHeaders;
import com.upokecenter.android.util.AppManager;
import com.upokecenter.android.util.DebugUtility;
import com.upokecenter.android.util.DialogUtility;
import com.upokecenter.android.util.IChoiceListener;

public class LauncherActivity extends Activity {

	@Override
	public void onDestroy(){
		super.onDestroy();
		DialogUtility.clean();
	}

	public void onCreate2(Bundle b){
		super.onCreate(b);
		AppManager.initialize(this);
		CacheHelper.getCachedData("http://www.upokecenter.com/",
				"upokecenter.html",
				null, 
				new IDownloadHandler<Object>(){

					@Override
					public Object processResponse(URL url, InputStream stream,
							IHttpHeaders headers) throws IOException {
						return null;
					}

					@Override
					public void onFinished(URL url, Object value,
							IOException exception, int progress, int total) {
						DebugUtility.log("finished");
						if(exception!=null)
							exception.printStackTrace();
						finish();
					}
			
		});
	}
	
	public void onCreate(Bundle b){
		super.onCreate(b);
		AppManager.initialize(this);
        DialogUtility.showChoices(this,R.string.app_name,
				(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) ? 
						R.array.entries_launcher_jellybean : 
							R.array.entries_launcher,
		    new IChoiceListener(){
			public void onChoice(int choice){
				if(choice==0){ // Settings
					Intent intent=new Intent(Intent.ACTION_MAIN);
					intent.setClass(AppManager.getApplication(),SettingsActivity.class);
					startActivity(intent);
				}
				if(choice==1){ // Show Live Wallpapers or Set Live Wallpaper
					Intent intent=null;
					if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN){
						intent=new Intent("android.service.wallpaper.CHANGE_LIVE_WALLPAPER");
						intent.putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT",
								new ComponentName(AppManager.getApplication(),ColorWallpaperService.class));
					} else {
						intent=new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);						
					}
					startActivity(intent);
				}
				finish();
			}
		});
	}
}
