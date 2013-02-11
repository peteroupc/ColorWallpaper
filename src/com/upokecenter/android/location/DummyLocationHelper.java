package com.upokecenter.android.location;

import java.util.ArrayList;

import android.content.Context;

public final class DummyLocationHelper implements ILocationHelper {

	private ArrayList<ISimpleLocationListener> listeners=null;
	public DummyLocationHelper(){
		listeners=new ArrayList<ISimpleLocationListener>();		
	}
	public DummyLocationHelper(Context context){
		listeners=new ArrayList<ISimpleLocationListener>();
	}
	
	@Override
	public void removeAllLocationListeners() {
		listeners.clear();
	}

	@Override
	public void removeLocationListener(ISimpleLocationListener simpleListener) {
		listeners.remove(simpleListener);
	}

	@Override
	public void addLocationListener(ISimpleLocationListener simpleListener) {
		listeners.add(simpleListener);
		simpleListener.onLocation(null);
	}

	@Override
	public void setLocationEnabled(boolean enabled) {
	}

	@Override
	public void setUpdateFrequency(int minTime, int minDistance) {
	}

	@Override
	public void setFineAccuracy(boolean fine) {
	}
	@Override
	public boolean isLocationEnabled() {
		return false;
	}

}
