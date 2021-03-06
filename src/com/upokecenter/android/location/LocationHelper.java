package com.upokecenter.android.location;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;

import com.upokecenter.util.DateTimeUtility;
import com.upokecenter.util.Reflection;

public final class LocationHelper implements ILocationHelper {
  private Location lastKnownLocation=null;
  private final long NANOS_PER_MS=1000000L;
  private final long FRESHNESS_DELAY_NANOS=150000L*NANOS_PER_MS;
  private LocationListener currentListener=null;
  private ArrayList<ISimpleLocationListener> listeners=null;
  private String provider=null;
  private boolean enabled=false;
  private boolean started=false;
  private boolean twoProviders=false;
  private boolean userEnabledSetting=true;
  private Context application=null;
  private int minTimeInSeconds=600;
  private boolean fineAccuracy=false;
  private int minDistanceInMeters=100;
  public LocationHelper(Context context){
    listeners=new ArrayList<ISimpleLocationListener>();
    application=context.getApplicationContext();
    currentListener=new LocationListener(){
      @Override public void onLocationChanged(Location loc){
        if(loc!=null){
          if(!twoProviders || lastKnownLocation==null){
            lastKnownLocation=loc;
            for(ISimpleLocationListener lis : listeners){
              lis.onLocation(copyLocation(lastKnownLocation));
            }
          } else {
            if(timeOffsetNanos(loc,lastKnownLocation)>=0 ||
                loc.getAccuracy()<=lastKnownLocation.getAccuracy()){
              lastKnownLocation=loc;
              for(ISimpleLocationListener lis : listeners){
                lis.onLocation(copyLocation(lastKnownLocation));
              }
            }
          }
        }
      }
      @Override
      public void onProviderDisabled(String arg0) {
        doSetLocationEnabled(false);
      }
      @Override
      public void onProviderEnabled(String provider) {
        doSetLocationEnabled(userEnabledSetting);
        findLastKnownLocation();
      }
      @Override
      public void onStatusChanged(String arg0,
          int arg1, Bundle arg2) {
      }
    };
    doSetLocationEnabled(true);
  }

  private LocationManager getLocationManager(){
    return ((LocationManager) application.getSystemService(Context.LOCATION_SERVICE));
  }

  private Location copyLocation(Location loc){
    return (loc==null) ? null : new Location(loc);
  }
  private long timeOffsetNanos(Location loc1, Location loc2){
    Method method=Reflection.getMethod(loc1,"getElapsedRealtimeNanos");
    if(method!=null){
      long a=(Long)Reflection.invoke(loc1,method,0);
      long b=(Long)Reflection.invoke(loc2,method,0);
      return a-b;
    }
    return (loc1.getTime()-loc2.getTime())*NANOS_PER_MS;
  }
  private boolean isCloseEnough(Location loc, long nanosTolerance){
    Method method=Reflection.getMethod(loc,"getElapsedRealtimeNanos");
    Method method2=Reflection.getStaticMethod(SystemClock.class,"elapsedRealtimeNanos");
    if(method!=null && method2!=null){
      long locNanos=(Long)Reflection.invoke(loc,method,0);
      long sysNanos=(Long)Reflection.invoke(loc,method2,0);
      return Math.abs(locNanos-sysNanos)<nanosTolerance;
    }
    return Math.abs(DateTimeUtility.getCurrentDate()-loc.getTime())<nanosTolerance/NANOS_PER_MS;
  }
  private boolean findLastKnownLocation(){
    if(lastKnownLocation==null && enabled){
      LocationManager mgr=getLocationManager();
      Location loc=(provider==null) ? null : mgr.getLastKnownLocation(provider);
      if(loc!=null){
        if(isCloseEnough(loc,FRESHNESS_DELAY_NANOS)){
          lastKnownLocation=loc;
          for(ISimpleLocationListener lis : listeners){
            lis.onLocation(copyLocation(loc));
          }
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void removeAllLocationListeners(){
    listeners.clear();
    if(started){
      LocationManager mgr=getLocationManager();
      mgr.removeUpdates(currentListener);
      started=false;
      twoProviders=false;
    }
  }

  @Override
  public void removeLocationListener(ISimpleLocationListener simpleListener){
    listeners.remove(simpleListener);
    if(listeners.size()==0 && started){
      LocationManager mgr=getLocationManager();
      mgr.removeUpdates(currentListener);
      started=false;
      twoProviders=false;
    }
  }

  @Override public void setFineAccuracy(boolean fine){
    if(fineAccuracy==fine)return;
    fineAccuracy=fine;
    doSetLocationEnabled(userEnabledSetting);
    if(started){
      LocationManager mgr=getLocationManager();
      mgr.removeUpdates(currentListener);
      requestUpdates(mgr);
    }
  }

  @Override
  public void setUpdateFrequency(int minTimeInSeconds, int minDistanceInMeters){
    boolean newValues=(this.minTimeInSeconds!=minTimeInSeconds ||
        this.minDistanceInMeters!=minDistanceInMeters);
    this.minTimeInSeconds=minTimeInSeconds;
    this.minDistanceInMeters=minDistanceInMeters;
    if(started && newValues){
      LocationManager mgr=getLocationManager();
      mgr.removeUpdates(currentListener);
      requestUpdates(mgr);
    }
  }

  private void requestUpdates(LocationManager mgr){
    if(provider!=null){
      mgr.requestLocationUpdates(
          provider,minTimeInSeconds*1000,
          minDistanceInMeters,currentListener
          );
      if(LocationManager.GPS_PROVIDER.equals(provider) &&
          mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
        // If we have the GPS provider, also register the network provider
        twoProviders=true;
        mgr.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,minTimeInSeconds*1000,
            minDistanceInMeters,currentListener
            );
      } else {
        twoProviders=false;
      }
      started=true;
    }
  }

  @Override
  public void addLocationListener(ISimpleLocationListener simpleListener){
    LocationManager mgr=getLocationManager();
    if(!started){
      requestUpdates(mgr);
    }
    doSetLocationEnabled(enabled);
    listeners.add(simpleListener);
    if(enabled){
      if(!findLastKnownLocation()){
        simpleListener.onLocation(copyLocation(lastKnownLocation));
      }
    } else {
      simpleListener.onLocation(null);
    }
  }
  @Override
  public void setLocationEnabled(boolean enabled){
    userEnabledSetting=enabled;
    doSetLocationEnabled(enabled);
  }
  private void doSetLocationEnabled(boolean enabled){
    LocationManager mgr=getLocationManager();
    if(enabled){
      Criteria criteria = new Criteria();
      criteria.setAccuracy((fineAccuracy) ? Criteria.ACCURACY_FINE : Criteria.ACCURACY_COARSE);
      criteria.setCostAllowed(false);
      provider=mgr.getBestProvider(criteria,true);
      enabled=(provider!=null);
    }
    this.enabled=enabled;
    if(currentListener!=null && !this.enabled){
      mgr.removeUpdates(currentListener);
    }
    if(!this.enabled){
      Location oldLocation=lastKnownLocation;
      lastKnownLocation=null;
      if(oldLocation!=null){
        for(ISimpleLocationListener lis : listeners){
          lis.onLocation(null);
        }
      }
    }
  }

  @Override
  public boolean isLocationEnabled() {
    return enabled;
  }
}
