package com.upokecenter.android.ui;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

public class IntentPreference extends Preference {

  public IntentPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean isEnabled(){
    Intent intent=getIntent();
    Context context=getContext();
    if(intent!=null && context!=null){
      if(context.getPackageManager().queryIntentActivities(intent, 0).size()<=0)
        // Disable if no activities can handle the given intent
        return false;
    }
    return super.isEnabled();
  }
}
