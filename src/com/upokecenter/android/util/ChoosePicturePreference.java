package com.upokecenter.android.util;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;

import com.upokecenter.util.IBoundAction;


public class ChoosePicturePreference extends Preference {

	int callback=-1;

		@Override
		protected Parcelable onSaveInstanceState() {
			Parcelable state = super.onSaveInstanceState();
			PreferenceState ret = new PreferenceState(state,this.getClass());
			Bundle b=ret.getBundle();
			b.putInt("callback",callback);
			state=ret;
			return state;
		}

		@Override
		protected void onRestoreInstanceState(Parcelable state) {
			boolean isState=PreferenceState.isPreferenceState(state,this.getClass());
			super.onRestoreInstanceState(isState ? ((PreferenceState)state).getSuperState() : state);
			if(isState){
				Bundle b=((PreferenceState)state).getBundle();
				this.callback=b.getInt("callback");
				GetContentActivity.getCallbacks().rebindAction(this.callback,this);
			}
		}


		@Override
		protected Object onGetDefaultValue(TypedArray a, int index) {
			return a.getString(index);
		}


		private void persist(Object value){
				persistString((String)value);
		}

		private Object getPersisted(Object value){
				return getPersistedString(value==null ? "" : (String)value);
		}

		private String settingSummary = null;
		private String dialogTitle = null;

		public ChoosePicturePreference(Context context, AttributeSet attrs) {
			super(context, attrs);
			int summaryId=attrs.getAttributeResourceValue(
					"http://schemas.android.com/apk/res/android","summary",0);
			if(summaryId!=0){
				settingSummary=context.getResources().getString(summaryId);
			}
			int dialogTitleId=attrs.getAttributeResourceValue(
					"http://schemas.android.com/apk/res/android","dialogTitle",0);
			if(dialogTitleId!=0){
				dialogTitle=context.getResources().getString(dialogTitleId);
			}
		}

		private void showDialog() {
			Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
			intent.setClass(getContext(),GetContentActivity.class);
			this.callback=GetContentActivity.getCallbacks().registerAction(
					this,new IBoundAction<String>(){
				@Override
				public void action(Object obj, String... parameters) {
					ChoosePicturePreference pref=(ChoosePicturePreference)obj;
					if(parameters[0]!=null && pref.callChangeListener(parameters[0])){
						pref.persist(parameters[0]);
						pref.setSummary(String.format(
								pref.settingSummary==null ? "" : pref.settingSummary,
								pref.getPersisted(null)));
					}
				}
			});
			if(dialogTitle!=null){
				intent.putExtra(Intent.EXTRA_TITLE,dialogTitle);				
			}
			intent.putExtra("com.upokecenter.android.extra.CALLBACK",this.callback);
			getContext().startActivity(intent);
		}

		@Override
		protected void onClick() {
			super.onClick();
			showDialog();
		}

		@Override
		protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
			if (restorePersistedValue) {
				setSummary(String.format(settingSummary==null ? "" : settingSummary,this.getPersisted(defaultValue)));
			} else {
				persist(defaultValue);
				setSummary(String.format(settingSummary==null ? "" : settingSummary,this.getPersisted(defaultValue)));
			}
		}

	}
