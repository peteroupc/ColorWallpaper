package com.upokecenter.android.colorwallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.upokecenter.android.util.AppManager;
import com.upokecenter.android.util.DialogUtility;
import com.upokecenter.android.util.IChoiceListener;

public class LauncherActivity extends Activity {
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		DialogUtility.clean();
	}
	
	@Override public void onCreate(Bundle b){
		super.onCreate(b);
		AppManager.initialize(this);
        DialogUtility.showChoices(this,R.string.app_name,
				(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) ? 
						R.array.entries_launcher_jellybean : 
							R.array.entries_launcher,
		    new IChoiceListener(){
			public void onChoice(int choice){
				if(choice==0){
					Intent intent=new Intent(Intent.ACTION_MAIN);
					intent.setClass(AppManager.getApplication(),SettingsActivity.class);
					startActivity(intent);
				}
				if(choice==1){
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
