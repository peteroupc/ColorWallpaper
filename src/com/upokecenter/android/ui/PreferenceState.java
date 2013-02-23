package com.upokecenter.android.ui;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference.BaseSavedState;


final class PreferenceState extends BaseSavedState {
    Bundle bundle;
    Class<?> clazz;
    public PreferenceState(Parcel p) {
        super(p);
        this.bundle = p.readBundle();
        this.clazz = (Class<?>)p.readSerializable();
    }

    public PreferenceState(Parcelable state, Class<?> clazz) {
        super(state);
		this.clazz=clazz;
        this.bundle=new Bundle();
    }
    
    public Bundle getBundle(){
    	return bundle;
    }
    
    
    
    @Override
	public String toString() {
		return "PreferenceState [bundle=" + bundle + ", clazz=" + clazz + "]";
	}

	public static boolean isPreferenceState(Parcelable state, Class<?> clazz){
    	if(state!=null && (state instanceof PreferenceState)){
    		Class<?> otherClass=((PreferenceState)state).clazz;
    		//DebugUtility.log("thisclass=%s otherclass=%s",clazz,otherClass);
    		if(otherClass!=null && otherClass.equals(clazz))
    			return true;
    	}
    	return false;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeBundle(bundle);
        dest.writeSerializable(clazz);
    }

    public static final Parcelable.Creator<PreferenceState> CREATOR = new Parcelable.Creator<PreferenceState>() {
        @Override
		public PreferenceState[] newArray(int size) {
            return new PreferenceState[size];
        }
        @Override
		public PreferenceState createFromParcel(Parcel parcel) {
            return new PreferenceState(parcel);
        }
    };

}
