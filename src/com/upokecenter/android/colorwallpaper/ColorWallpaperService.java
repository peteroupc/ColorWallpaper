package com.upokecenter.android.colorwallpaper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Xml;
import android.view.SurfaceHolder;

import com.upokecenter.android.location.ILocationHelper;
import com.upokecenter.android.location.ISimpleLocationListener;
import com.upokecenter.android.location.LocationHelper;
import com.upokecenter.android.net.DownloadService;
import com.upokecenter.android.util.AppManager;
import com.upokecenter.android.util.BitmapUtility;
import com.upokecenter.android.wallpaper.BaseWallpaperService;
import com.upokecenter.net.IHttpHeaders;
import com.upokecenter.net.IOnFinishedListener;
import com.upokecenter.net.IProcessResponseListener;
import com.upokecenter.util.SunriseSunset;
import com.upokecenter.util.XmlHelper;


@TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
public class ColorWallpaperService extends BaseWallpaperService {

	public static Bitmap cacheImageFile(String file, 
			int desiredWidth, int desiredHeight) throws IOException {
		BitmapFactory.Options o=new BitmapFactory.Options();
		o.inJustDecodeBounds=true;
		BitmapFactory.decodeFile(file.toString(),o);
		if(o.outWidth>desiredWidth || o.outHeight>desiredHeight){
			float sampleX=o.outWidth*1.0f/desiredWidth;
			float sampleY=o.outHeight*1.0f/desiredHeight;
			o.inSampleSize=Math.round(Math.min(sampleX,sampleY));
		}
		o.inJustDecodeBounds=false;
		Bitmap bitmap=BitmapFactory.decodeFile(file.toString(),o);
		return bitmap;
	}


	public Bitmap cacheImage(InputStream stream, int desiredWidth, int desiredHeight) throws IOException {
		stream.mark(0x10000);
		BitmapFactory.Options o=new BitmapFactory.Options();
		o.inJustDecodeBounds=true;
		BitmapFactory.decodeStream(stream,null,o);
		stream.reset();
		if(o.outWidth>desiredWidth || o.outHeight>desiredHeight){
			float sampleX=o.outWidth*1.0f/desiredWidth;
			float sampleY=o.outHeight*1.0f/desiredHeight;
			o.inSampleSize=Math.round(Math.min(sampleX,sampleY));
		}
		o.inJustDecodeBounds=false;
		Bitmap bitmap=BitmapFactory.decodeStream(stream,null,o);
		return bitmap;
	}

	public static List<String> parseRssFeedImages(InputStream stream) throws IOException {
		XmlPullParser parser=Xml.newPullParser();
		try {
			List<String> ret=new ArrayList<String>();
			parser.setInput(stream,null);
			while(XmlHelper.moveToElement(parser,null,"item")){
				int depth=XmlHelper.GetDepth(parser);
				while(XmlHelper.findChildToDepth(parser,depth,null,null)){
					if(XmlHelper.IsElement(parser,"http://search.yahoo.com/mrss/","content")){
						String urlname=parser.getAttributeValue(null,"url");
						ret.add(urlname);
					}
				}
			}
			return ret;
		} catch (XmlPullParserException e) {
			throw (IOException)new IOException().initCause(e);
		}
	}
	@TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
	@Override
	public Engine onCreateEngine() {
		AppManager.initialize(this);
		return new Engine(){
			ILocationHelper locationHelper=null;
			int width=0;
			int height=0;
			Preferences prefs=null;
			Random random=null;
			Location currentLocation=null;
			SunriseSunset.DayState dayState;
			Bitmap scratchBitmap=null;
			Canvas scratchCanvas=null;
			int onDayStateCount=0;
			class Preferences {
				public boolean usedaycycle;
				public boolean uselocation;
				public boolean usemonthcycle;
				public int colorhue;
				public boolean reacttotaps;
				public boolean fadeinboxes;
				public boolean usemodelbg;
				public String picture;
				public int drawspeedfps;
				public int boxsize;
				public void setPreferences(SharedPreferences prefs){
					this.usedaycycle=prefs.getBoolean("usedaycycle",true);
					this.uselocation=prefs.getBoolean("uselocation",false);
					this.usemonthcycle=prefs.getBoolean("usemonthcycle",true);
					this.colorhue=prefs.getInt("colorhue",0);
					this.boxsize=prefs.getInt("boxsize",30);
					this.reacttotaps=prefs.getBoolean("reacttotaps",true);
					this.fadeinboxes=prefs.getBoolean("fadeinboxes",true);
					this.usemodelbg=prefs.getBoolean("usemodelbg",true);
					this.picture=prefs.getString("picture","");
					this.drawspeedfps=prefs.getInt("drawspeedfps",10);
				}
			}

			/*
			 * NOTE: The OnSharedPreferenceChangeListener must be
			 * an instance variable, not a local variable, because
			 * the SharedPreferences object stores listeners as weak
			 * references when it registers them, so when they are
			 * local variables, they become eligible for garbage 
			 * collection once the listeners leave the
			 * scope of the method.
			 */
			OnSharedPreferenceChangeListener listener=null;

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
				int fps=prefs.drawspeedfps;
				if(fps!=0){
					return 1000/Math.max(fps,1);
				}
				return 20;
			}

			@Override
			protected void onFrame() {
				int boxcount=8;
				float hue=getCurrentHue();
				int value=getValueOffset();
				for(int i=0;i<boxcount;i++){
					drawColor(Color.HSVToColor(new float[]{
							hue-10f+random.nextInt(20),
							((10+random.nextInt(11))/20.0f),
							Math.min(1.0f,((value+random.nextInt(21))/40.0f))
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
					if(prefs.reacttotaps){
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
				if(prefs.usemonthcycle){
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
					return prefs.colorhue;	
				}
			}

			private int getValueOffset(){
				int valueOffset=20;
				if(prefs.usedaycycle){
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
				if(locationHelper!=null){
					locationHelper.removeAllLocationListeners();
					locationHelper=null;
				}
				if(scratchBitmap!=null){
					scratchBitmap.recycle();
					scratchBitmap=null;
				}
				if(modelBitmap!=null){
					modelBitmap.recycle();
					modelBitmap=null;
				}
				DownloadService.shutdown(ColorWallpaperService.this);
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

			Bitmap modelBitmap=null;

			private void loadPictureBitmap(){
				if(!prefs.usemodelbg || prefs.picture==null){
					if(modelBitmap!=null){
						modelBitmap.recycle();
						modelBitmap=null;
					}
					return;
				}
				String pic=prefs.picture;
				if(pic.startsWith("file://")){
					Uri uri=Uri.parse(pic);
					pic=uri.getPath();
				}
				if(pic.startsWith("http://") ||
						pic.startsWith("https://")){
					DownloadService.sendRequest(AppManager.getApplication(),pic,
							new IProcessResponseListener<Object>(){

						@Override
						public Object processResponse(String url,
								InputStream stream, IHttpHeaders headers)
										throws IOException {
							return cacheImage(stream,200,200);
						}

					},new IOnFinishedListener<Object>(){

						@Override
						public void onFinished(String url, Object value,
								IOException exception) {
							if(modelBitmap!=null)
								modelBitmap.recycle();
							modelBitmap=(value==null) ? null : (Bitmap)value;							
						}

					});
				} else {
					new AsyncTask<String,Void,Bitmap>(){

						@Override
						protected Bitmap doInBackground(String... arg0) {
							String arg=arg0[0];
							if(arg==null)return null;
							if(!new File(arg).isFile())
								return null;
							try {
								Bitmap b=cacheImageFile(arg,200,200);
								return b;
							} catch (IOException e) {
								return null;
							}

						}

						@Override
						protected void onPostExecute(Bitmap b){
							if(modelBitmap!=null)
								modelBitmap.recycle();
							modelBitmap=b;
						}
					}.execute(pic);
				}
			}

			@TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
			@Override public void onCreate(SurfaceHolder surface){
				super.onCreate(surface);
				SharedPreferences p=PreferenceManager.getDefaultSharedPreferences(
						AppManager.getApplication());
				prefs=new Preferences();
				prefs.setPreferences(p);
				random=new Random();
				locationHelper=new LocationHelper(AppManager.getApplication());
				locationHelper.setLocationEnabled(prefs.uselocation);
				locationHelper.addLocationListener(new ISimpleLocationListener(){
					@Override
					public void onLocation(Location loc) {
						currentLocation=loc;
						updateDayState();
					}
				});
				listener=new OnSharedPreferenceChangeListener(){
					@Override
					public void onSharedPreferenceChanged(SharedPreferences p, String key) {
						prefs.setPreferences(p);
						if(key.equals("uselocation")){
							locationHelper.setLocationEnabled(prefs.uselocation);
						}
						if(key.equals("picture") || key.equals("usemodelbg")){
							loadPictureBitmap();
						}
					}
				};
				p.registerOnSharedPreferenceChangeListener(listener);
				loadPictureBitmap();
			}
			@TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
			private void drawColor2(int color, int x, int y){
				if(!this.isVisible())return;
				int widthlevel=prefs.boxsize;
				int heightlevel=prefs.boxsize;
				int x1=x-heightlevel+random.nextInt(widthlevel*2);
				int x2=Math.max(4,random.nextInt(widthlevel));
				int y1=y-heightlevel+random.nextInt(heightlevel*2);
				int y2=Math.max(4,random.nextInt(heightlevel));
				Rect r=new Rect(
						Math.max(0,x1-x2),
						Math.max(0,y1-y2),
						Math.min(this.width,x1+x2),
						Math.min(this.height,y1+y2));
				int frames=(prefs.fadeinboxes) ? 5 : 1;
				addColorTransition(r,color,frames);
			}
			@TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
			private void drawColor(int color){
				if(!this.isVisible())return;
				int widthlevel=prefs.boxsize;
				int heightlevel=prefs.boxsize;
				int x1=random.nextInt(Math.max(1,this.width));
				int x2=random.nextInt(widthlevel);
				int y1=random.nextInt(Math.max(1,this.height));
				int y2=random.nextInt(heightlevel);
				Rect r=new Rect(
						Math.max(0,x1-x2),
						Math.max(0,y1-y2),
						Math.min(this.width,x1+x2),
						Math.min(this.height,y1+y2));
				if(modelBitmap!=null){
					int w=modelBitmap.getWidth();
					int h=modelBitmap.getHeight();
					float xmid=r.left+(r.right-r.left)/2;
					float ymid=r.top+(r.bottom-r.top)/2;
					int bitmapx=Math.round(xmid*w*1.0f/Math.max(1,this.width));
					int bitmapy=Math.round(ymid*h*1.0f/Math.max(1,this.height));
					if(bitmapx>=w)bitmapx=w-1;
					if(bitmapy>=h)bitmapy=h-1;
					if(bitmapx<0)bitmapx=0;
					if(bitmapy<0)bitmapy=0;
					color=modelBitmap.getPixel(bitmapx,bitmapy);
					float[] hsv=new float[3];
					Color.colorToHSV(color, hsv);
					hsv[0]-=10f;
					hsv[0]+=random.nextInt(20);
					hsv[1]-=0.05f;
					hsv[1]+=(random.nextInt(100)*0.10f/100f);
					hsv[2]-=0.05f;
					hsv[2]+=(random.nextInt(100)*0.10f/100f);
					hsv[2]=hsv[2]*(1.0f-((30-getValueOffset())*0.02f));
					hsv[1]=Math.max(0,Math.min(1,hsv[1]));
					hsv[2]=Math.max(0,Math.min(1,hsv[2]));
					color=Color.HSVToColor(hsv);
				}
				int frames=(prefs.fadeinboxes) ? 5 : 1;
				addColorTransition(r,color,frames);
			}
		};
	}

}
