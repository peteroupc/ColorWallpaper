package com.upokecenter.android.colorwallpaper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;

import com.upokecenter.android.location.ILocationHelper;
import com.upokecenter.android.location.ISimpleLocationListener;
import com.upokecenter.android.location.LocationHelper;
import com.upokecenter.android.util.AppManager;
import com.upokecenter.android.util.BitmapUtility;
import com.upokecenter.android.wallpaper.BaseWallpaperService;
import com.upokecenter.util.SunriseSunset;

public class ColorWallpaperService extends BaseWallpaperService {


	@Override
	public Engine onCreateEngine() {
		AppManager.initialize(this);
		return new Engine(){
			ILocationHelper helper=null;
			final int DIPCONVERT=30;
			int width=0;
			int height=0;
			SharedPreferences prefs=null;
			Random random=null;
			Location currentLocation=null;
			SunriseSunset.DayState dayState;
			Bitmap scratchBitmap=null;
			Canvas scratchCanvas=null;
			int onDayStateCount=0;


			class ColorTransition {
				Rect rect;
				int endColor;
				int currentFrame;
				int frameCount;
				public int getColor(){
					int frame=Math.min(currentFrame,frameCount);
					frame=Math.max(0,frame);
					int a=(Color.alpha(endColor)*currentFrame/frameCount);
					return Color.argb(a,Color.red(endColor),
							Color.green(endColor),
							Color.blue(endColor));
				}
			}

			ArrayList<ColorTransition> transitions=new ArrayList<ColorTransition>();
			private void updateColorTransitions(){
				for(ColorTransition ct : transitions){
					ct.currentFrame++;
				}
				for(int i=0;i<transitions.size();i++){
					ColorTransition ct=transitions.get(i);
					if(ct.currentFrame>ct.frameCount){
						transitions.remove(i);
						i--;
					}
				}
			}
			private void addColorTransition(Rect rect, int endColor, int frameCount){
				ColorTransition ct=new ColorTransition();
				ct.rect=rect;
				ct.endColor=endColor;
				ct.frameCount=frameCount;
				ct.currentFrame=0;
				transitions.add(ct);
			}

			@Override
			protected void drawFrame(Canvas c, long e) {
				int curWidth=c.getClipBounds().width();
				int curHeight=c.getClipBounds().height();
				if(curWidth!=width || curHeight!=height){
					width=curWidth;
					height=curHeight;
					int background=Color.HSVToColor(new float[]{
							getCurrentHue(),0.2f,getValueOffset()/40f
					});
					scratchBitmap=BitmapUtility.redrawBitmap(
							scratchBitmap,width,height,background);
					scratchCanvas=new Canvas(scratchBitmap);
					if(c!=null){
						c.drawBitmap(scratchBitmap,null,
								new RectF(0,0,
										scratchBitmap.getWidth(),
										scratchBitmap.getHeight()),null);
					}
				}
				if(scratchBitmap!=null){
					Rect bitmapRect=new Rect(0,0,scratchBitmap.getWidth(),scratchBitmap.getHeight());
					c.drawBitmap(scratchBitmap,bitmapRect,bitmapRect,null);
				}
				for(int i=0;i<transitions.size();i++){
					ColorTransition ct=transitions.get(i);
					Paint p=new Paint();
					int color=ct.getColor();
					p.setColor(color);
					c.drawRect(ct.rect,p);
				}
				for(int i=0;i<transitions.size();i++){
					ColorTransition ct=transitions.get(i);
					Paint p=new Paint();
					int color=ct.getColor();
					p.setColor(color);
					if(Color.alpha(color)==0xFF && scratchCanvas!=null){
						scratchCanvas.drawRect(ct.rect,p);
					}							
				}
				updateColorTransitions();
			}
			
			@Override
			public int getDelay(){
				int fps=prefs.getInt("drawspeedfps",10);
				if(fps!=0){
					return 1000/Math.max(fps,1);
				}
				String speed=prefs.getString("drawspeed","medium");
				if("slow".equals(speed))return 80;
				if("medium".equals(speed))return 40;
				return 20;
			}
			
			@Override
			protected void onFrame() {
				int boxcount=8;
				for(int i=0;i<boxcount;i++){
						drawColor(Color.HSVToColor(new float[]{
								getCurrentHue()-10f+random.nextInt(20),
								((10+random.nextInt(11))/20.0f),
								Math.min(1.0f,((getValueOffset()+random.nextInt(21))/40.0f))
						}));							
				}
				if(onDayStateCount==0){
					updateDayState();
				}
				onDayStateCount+=1;
				onDayStateCount%=(2000/getDelay());
			}

			@Override
			public Bundle onCommand(String action, int x, int y, int z, Bundle extras, boolean resultRequested){
				if(WallpaperManager.COMMAND_TAP.equals(action)){
					if(prefs.getBoolean("reacttotaps",true)){
						for(int i=0;i<8;i++){
							int background=Color.HSVToColor(new float[]{
									random.nextInt(360),
									((10+random.nextInt(11))/20.0f),
									((10+random.nextInt(11))/20.0f)
							});
							drawColor2(background,x,y);							
						}
					}
				}
				return super.onCommand(action,x,y,z,extras,resultRequested);
			}


			private float getCurrentHue(){
				SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(AppManager.getApplication());
				if(prefs.getBoolean("usemonthcycle",true)){
					Calendar cal=Calendar.getInstance();
					long time=new Date().getTime();
					cal.setTimeInMillis(time);
					cal.set(Calendar.HOUR_OF_DAY,0);
					cal.set(Calendar.MINUTE,0);
					cal.set(Calendar.SECOND,0);
					cal.set(Calendar.MILLISECOND,0);
					cal.set(Calendar.DAY_OF_MONTH,1);
					long startOfMonth=cal.getTimeInMillis();
					cal.add(Calendar.MONTH,1);
					long startOfNextMonth=cal.getTimeInMillis();
					float currentHue=(time-startOfMonth)*1.0f/(startOfNextMonth-startOfMonth);
					currentHue*=360.0;
					return currentHue;
				} else {
					return prefs.getInt("colorhue",0);	
				}
			}

			private int getValueOffset(){
				int valueOffset=20;
				if(prefs.getBoolean("usedaycycle",true)){
					if(dayState==SunriseSunset.DayState.Night)
						valueOffset=5;
					if(dayState==SunriseSunset.DayState.NightToDay)
						valueOffset=20;
					if(dayState==SunriseSunset.DayState.DayToNight)
						valueOffset=20;
					if(dayState==SunriseSunset.DayState.Day)
						valueOffset=30;
				}
				return valueOffset;
			}

			@Override public void onDestroy(){
				if(helper!=null){
					helper.removeAllLocationListeners();
				}
				if(scratchBitmap!=null){
					scratchBitmap.recycle();
					scratchBitmap=null;
				}
				super.onDestroy();
			}
			
			private void updateDayState(){
				if(currentLocation!=null){
					dayState=SunriseSunset.getCurrentDayState(
							currentLocation.getLatitude(),
							currentLocation.getLongitude());
				} else {
					Calendar cal=Calendar.getInstance();
					int hour=cal.get(Calendar.HOUR_OF_DAY);
					if(hour<6)dayState=SunriseSunset.DayState.Night;
					else if(hour<7)dayState=SunriseSunset.DayState.NightToDay;
					else if(hour<17)dayState=SunriseSunset.DayState.Day;
					else if(hour<18)dayState=SunriseSunset.DayState.DayToNight;
					else dayState=SunriseSunset.DayState.Night;
				}				
			}

			@Override public void onCreate(SurfaceHolder surface){
				super.onCreate(surface);
				prefs=PreferenceManager.getDefaultSharedPreferences(AppManager.getApplication());
				random=new Random();
				helper=new LocationHelper(AppManager.getApplication());
				helper.setLocationEnabled(prefs.getBoolean("uselocation",false));
				helper.addLocationListener(new ISimpleLocationListener(){
					@Override
					public void onLocation(Location loc) {
						currentLocation=loc;
						updateDayState();
					}
				});
				prefs.registerOnSharedPreferenceChangeListener(
						new OnSharedPreferenceChangeListener(){
							@Override
							public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
								if(key=="uselocation"){
									helper.setLocationEnabled(prefs.getBoolean("uselocation",false));
								}
							}
						});
			}
			private void drawColor2(int color, int x, int y){
				if(!this.isVisible())return;
				int widthlevel=1024/DIPCONVERT;
				int heightlevel=768/DIPCONVERT;
				int x1=x-heightlevel+random.nextInt(widthlevel*2);
				int x2=Math.max(4,random.nextInt(widthlevel));
				int y1=y-heightlevel+random.nextInt(heightlevel*2);
				int y2=Math.max(4,random.nextInt(heightlevel));
				Rect r=new Rect(
						Math.max(0,x1-x2),
						Math.max(0,y1-y2),
						Math.min(this.width,x1+x2),
						Math.min(this.height,y1+y2));
				int frames=(prefs.getBoolean("fadeinboxes",true)) ? 5 : 1;
				addColorTransition(r,color,frames);
			}
			private void drawColor(int color){
				if(!this.isVisible())return;
				int widthlevel=1024/DIPCONVERT;
				int heightlevel=768/DIPCONVERT;
				int x1=random.nextInt(Math.max(1,this.width));
				int x2=random.nextInt(widthlevel);
				int y1=random.nextInt(Math.max(1,this.height));
				int y2=random.nextInt(heightlevel);
				Rect r=new Rect(
						Math.max(0,x1-x2),
						Math.max(0,y1-y2),
						Math.min(this.width,x1+x2),
						Math.min(this.height,y1+y2));
				int frames=(prefs.getBoolean("fadeinboxes",true)) ? 5 : 1;
				addColorTransition(r,color,frames);
			}
		};
	}

}
