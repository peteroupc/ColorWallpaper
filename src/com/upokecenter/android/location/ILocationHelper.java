package com.upokecenter.android.location;

public interface ILocationHelper {

  public void removeAllLocationListeners();

  public void removeLocationListener(
      ISimpleLocationListener simpleListener);

  public void addLocationListener(
      ISimpleLocationListener simpleListener);

  public boolean isLocationEnabled();

  public void setLocationEnabled(boolean enabled);

  public void setUpdateFrequency(int minTime, int minDistance);

  public void setFineAccuracy(boolean fine);

}
