package com.upokecenter.android.colorwallpaper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import com.upokecenter.android.util.AppManager;
import com.upokecenter.android.util.BitmapUtility;
import com.upokecenter.util.Reflection;
import com.upokecenter.util.SunriseSunset;

public class ColorWallpaperService extends WallpaperService {

	@Override
	public Engine onCreateEngine() {
		AppManager.initialize(this);
		return new Engine(){
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
			private void drawColorTransitions(SurfaceHolder surface){
				if(!this.isVisible())return;
				//if(true)return;
				Canvas c=null;
				try {
					if(surface!=null)
						c=surface.lockCanvas();
					if(c!=null){
						Rect bitmapRect=new Rect(0,0,scratchBitmap.getWidth(),scratchBitmap.getHeight());
						c.drawBitmap(scratchBitmap,bitmapRect,bitmapRect,null);
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
							if(Color.alpha(color)==0xFF){
								scratchCanvas.drawRect(ct.rect,p);
							}							
						}
					}
				} catch(RuntimeException e){
				} finally {
					if(c!=null && surface!=null){
						try {
							surface.unlockCanvasAndPost(c);
						} catch(IllegalArgumentException e){

						}
					}
				}
			}

			int width=0;
			int height=0;
			SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(AppManager.getApplication());
			Random random=new Random();
			Handler handler=new Handler();
			Runnable doDraw;
			Runnable updateDayState;
			Location currentLocation=null;
			SunriseSunset.DayState dayState;
			SurfaceHolder currentSurface;
			Bitmap scratchBitmap=null;
			Canvas scratchCanvas=null;
			@Override
			public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				super.onSurfaceChanged(holder, format, width, height);
				this.currentSurface=holder;
				this.width=width;
				this.height=height;
				redrawSurface(holder);
			}


			@Override
			public Bundle onCommand(String action, int x, int y, int z, Bundle extras, boolean resultRequested){
				if(action.equals("android.wallpaper.tap")){
					if(prefs.getBoolean("reacttotaps",true)){
						for(int i=0;i<8;i++){
							int background=Color.HSVToColor(new float[]{
									random.nextInt(360),
									((10+random.nextInt(11))/20.0f),
									((10+random.nextInt(11))/20.0f)
							});
							drawColor2(currentSurface,background,x,y);							
						}	        		
						drawColorTransitions(currentSurface);
						updateColorTransitions();
					}
				}
				return null;
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

			private boolean isCloseEnough(Location loc, long nanosTolerance){
				Method method=Reflection.getMethod(loc,"getElapsedRealtimeNanos");
				Method method2=Reflection.getStaticMethod(SystemClock.class,"elapsedRealtimeNanos");
				if(method!=null && method2!=null){
					long locNanos=(Long)Reflection.invoke(loc,method,0);
					long sysNanos=(Long)Reflection.invoke(loc,method2,0);
					return Math.abs(locNanos-sysNanos)<nanosTolerance;
				}
				return Math.abs(new Date().getTime()-loc.getTime())<nanosTolerance/1000000;
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

			LocationListener currentListener=null;

			private void setUpLocation(){
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_COARSE);
				criteria.setCostAllowed(false);
				final LocationManager locationManager =
						(LocationManager) getSystemService(Context.LOCATION_SERVICE);
				final String provider = locationManager.getBestProvider(criteria, true);
				boolean enabled = provider!=null &&
						prefs.getBoolean("uselocation",true) &&
						locationManager.isProviderEnabled(provider);
				if(!enabled){
					currentLocation=null;
				}
				if(currentListener!=null && !enabled){
					locationManager.removeUpdates(currentListener);
					currentListener=null;
				} else if(currentListener==null && enabled){
					currentListener=new LocationListener(){
						@Override public void onLocationChanged(Location loc){
							if(loc!=null){
								currentLocation=loc;
								handler.removeCallbacks(updateDayState);
								handler.postDelayed(updateDayState, 50);
							}
						}
						@Override
						public void onProviderDisabled(String arg0) {
							handler.removeCallbacks(updateDayState);
							handler.postDelayed(updateDayState, 50);
						}
						@Override
						public void onProviderEnabled(String provider) {
							if(currentLocation==null){
								Location loc=locationManager.getLastKnownLocation(provider);
								if(loc!=null){
									if(isCloseEnough(loc,600000*1000000)){
										// we have a fresh location already
										currentLocation=loc;
										handler.removeCallbacks(updateDayState);
										handler.postDelayed(updateDayState, 50);
									}
								}
							}
						}
						@Override
						public void onStatusChanged(String arg0,
								int arg1, Bundle arg2) {
						}
					};
					Location loc=locationManager.getLastKnownLocation(provider);
					if(loc!=null){
						if(isCloseEnough(loc,600000*1000000)){
							// we have a fresh location already
							currentLocation=loc;
						}
					}
					handler.postDelayed(new Runnable(){@Override
						public void run(){
						locationManager.requestLocationUpdates(
								provider,10*60*1000,100,currentListener
								);
					}},loc==null ? 1 : 30*1000); // wait 30 seconds if we have a known location
				}
			}

			@Override public void onDestroy(){
				if(scratchBitmap!=null){
					scratchBitmap.recycle();
					scratchBitmap=null;
				}
			}

			@Override public void onCreate(SurfaceHolder surface){
				this.currentSurface=surface;
				prefs.registerOnSharedPreferenceChangeListener(
						new OnSharedPreferenceChangeListener(){
							@Override
							public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
								if(key=="uselocation"){
									setUpLocation();
								}
							}
						});
				setUpLocation();
				updateDayState=new Runnable(){
					@Override
					public void run(){
						if(isVisible()){
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
						handler.removeCallbacks(updateDayState);
						handler.postDelayed(updateDayState, 2000);
					}
				};
				updateDayState.run();
				doDraw=new Runnable(){
					@Override
					public void run(){
						if(isVisible()){
							String speed=prefs.getString("drawspeed","medium");
							int boxcount=8;
							if(speed.equals("slow"))boxcount=2;
							else if(speed.equals("fast"))boxcount=16;
							for(int i=0;i<boxcount;i++){
								drawColor(currentSurface,Color.HSVToColor(new float[]{
										getCurrentHue()-10f+random.nextInt(20),
										((10+random.nextInt(11))/20.0f),
										Math.min(1.0f,((getValueOffset()+random.nextInt(21))/40.0f))
								}));							
							}
							drawColorTransitions(currentSurface);
							updateColorTransitions();
						}
						handler.removeCallbacks(doDraw);
						handler.postDelayed(doDraw, 50);
					}
				};
			}
			int DIPCONVERT=30;
			private void drawColor2(SurfaceHolder surface, int color, int x, int y){
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
						Math.min(width,x1+x2),
						Math.min(height,y1+y2));
				int frames=(prefs.getBoolean("fadeinboxes",true)) ? 5 : 1;
				addColorTransition(r,color,frames);
			}
			private void drawColor(SurfaceHolder surface, int color){
				if(!this.isVisible())return;
				int widthlevel=1024/DIPCONVERT;
				int heightlevel=768/DIPCONVERT;
				int x1=random.nextInt(width);
				int x2=random.nextInt(widthlevel);
				int y1=random.nextInt(height);
				int y2=random.nextInt(heightlevel);
				Rect r=new Rect(
						Math.max(0,x1-x2),
						Math.max(0,y1-y2),
						Math.min(width,x1+x2),
						Math.min(height,y1+y2));
				int frames=(prefs.getBoolean("fadeinboxes",true)) ? 5 : 1;
				addColorTransition(r,color,frames);
			}

			private void redrawSurface(SurfaceHolder surface){
				//DebugUtility.log("redrawSurface");
				updateDayState.run();
				this.currentSurface=surface;
				int background=Color.HSVToColor(new float[]{
						getCurrentHue(),0.2f,getValueOffset()/40f
				});
				Canvas c=surface.lockCanvas();
				try {
					scratchBitmap=BitmapUtility.redrawBitmap(
							scratchBitmap,width,height,background);
					scratchCanvas=new Canvas(scratchBitmap);
					if(c!=null){
						c.drawBitmap(scratchBitmap,null,
								new RectF(0,0,width,height),null);
					}
				} finally {
					if(c!=null)
						surface.unlockCanvasAndPost(c);
				}
				handler.removeCallbacks(doDraw);
				handler.postDelayed(doDraw, 50);
			}

			@Override public void onSurfaceRedrawNeeded(SurfaceHolder surface){
				redrawSurface(surface);
			}
		};
	}

}
