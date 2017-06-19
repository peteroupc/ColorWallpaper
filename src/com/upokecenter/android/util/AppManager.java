package com.upokecenter.android.util;

import java.io.IOException;
import java.lang.reflect.Field;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.upokecenter.util.Reflection;

public final class AppManager {
  private AppManager(){}
  private static Object syncRoot=new Object();
  private static Context application=null;
  private static boolean initialized=false;

  public static int getResource(String type, String name){
    String packageName=getApplication().getApplicationInfo().packageName;
    return getApplication().getResources().getIdentifier(name,type,packageName);
  }
  public static int getStyleableResource(String name){
    String packageName=getApplication().getApplicationInfo().packageName;
    return (Integer)Reflection.getStaticFieldByName(
        Reflection.getClassForName(packageName+".R$styleable"),name,0);
  }
  public static int[] getStyleableResourceGroup(String name){
    String packageName=getApplication().getApplicationInfo().packageName;
    return (int[])Reflection.getStaticFieldByName(
        Reflection.getClassForName(packageName+".R$styleable"),name,new int[0]);
  }
  public static int getStringResource(String name){
    return getResource("string",name);
  }
  public static int getIdResource(String name){
    return getResource("id",name);
  }
  public static int getLayoutResource(String name){
    return getResource("layout",name);
  }

  public static String getStringResourceValue(String name, String defaultValue){
    int resource=getStringResource(name);
    try {
      return (resource==0) ? defaultValue : getApplication().getResources().getString(resource);
    } catch(Resources.NotFoundException e){
      return defaultValue;
    }
  }

  private static boolean isBuildConfigDebug(Context context){
    String packageName=context.getApplicationInfo().packageName;
    boolean ret=(Boolean)Reflection.getStaticFieldByName(
        packageName+".BuildConfig","DEBUG",false);
    return ret;
  }

  public static int getRotation(){
    Context context=getApplication();
    if(context==null)return Surface.ROTATION_0;
    WindowManager wm=((WindowManager)context.getSystemService("window"));
    Display display=wm.getDefaultDisplay();
    if(display==null)return Surface.ROTATION_0;
    Integer retval[]=new Integer[1];
    // getRotation was added in API level 8 (Froyo)
    if(!Reflection.invokeByNameWithTest(display,"getRotation",retval)){
      if(!Reflection.invokeByNameWithTest(display,"getOrientation",retval))
        return Surface.ROTATION_0;
    }
    return retval[0];
  }

  public static Context getApplication(){
    synchronized(syncRoot){
      return application;
    }
  }

  private static boolean isPreferenceXml(Resources resources, int id){
    XmlResourceParser parser=null;
    try {
      parser=resources.getXml(id);
      int evt=parser.getEventType();
      while (evt != XmlPullParser.END_DOCUMENT) {
        if(evt==XmlPullParser.START_TAG){
          if(parser.getName().equals("PreferenceScreen"))
            return true;
          else return false;
        }
        evt=parser.next();
      }
      return false;
    } catch(NotFoundException e){
      return false;
    } catch (XmlPullParserException e) {
      return false;
    } catch (IOException e) {
      return false;
    } finally {
      if(parser!=null) {
        parser.close();
      }
    }
  }

  public static void initialize(Context context){
    if(context==null)return;
    Context c=null;
    synchronized(syncRoot){
      if(!initialized){
        initialized=true;
      } else
        return;
    }
    synchronized(syncRoot){
      application=context.getApplicationContext();
      c=application;
    }
    // Set default values of preferences
    String packageName=c.getApplicationInfo().packageName;
    Class<?> classRxml=Reflection.getClassForName(packageName+".R$xml");
    for(Field field : Reflection.getStaticFieldInfos(classRxml)){
      Object obj=Reflection.getField(null,field,null);
      if(obj!=null && obj instanceof Integer){
        int id=(Integer)obj;
        if(isPreferenceXml(c.getResources(),id)){
          PreferenceManager.setDefaultValues(c,id,false);
        }
      }
    }
    // Enable strict mode
    if (isBuildConfigDebug(c)) {
      // We use reflection because StrictMode was only added in API level 9 (Gingerbread)
      Class<?> classStrictMode=Reflection.getClassForName("android.os.StrictMode");
      Class<?> classThreadPolicyBuilder=Reflection.getClassForName("android.os.StrictMode$ThreadPolicy$Builder");
      Class<?> classVmPolicyBuilder=Reflection.getClassForName("android.os.StrictMode$VmPolicy$Builder");
      if(classStrictMode!=null && classThreadPolicyBuilder!=null && classVmPolicyBuilder!=null){
        Object builder=Reflection.construct(classThreadPolicyBuilder);
        builder=Reflection.invokeByName(builder,"detectAll",builder);
        builder=Reflection.invokeByName(builder,"penaltyLog",builder);
        builder=Reflection.invokeByName(builder,"penaltyFlashScreen",builder);
        Object built=Reflection.invokeByName(builder,"build",null);
        Reflection.invokeStaticByName(classStrictMode,"setThreadPolicy",null,built);
        builder=Reflection.construct(classVmPolicyBuilder);
        builder=Reflection.invokeByName(builder,"detectLeakedClosableObjects",builder);
        builder=Reflection.invokeByName(builder,"detectLeakedRegistrationObjects",builder);
        builder=Reflection.invokeByName(builder,"detectLeakedSqlLiteObjects",builder);
        builder=Reflection.invokeByName(builder,"penaltyLog",builder);
        builder=Reflection.invokeByName(builder,"penaltyDeath",builder);
        built=Reflection.invokeByName(builder,"build",null);
        Reflection.invokeStaticByName(classStrictMode,"setVmPolicy",null,built);
      }
    }
  }
}
