package com.upokecenter.android.ui;

import java.lang.ref.WeakReference;

import com.upokecenter.android.util.AppManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.Toast;

public class AlertDialogPreference extends Preference {

	IDialogUpdater updater=null;

	protected IDialogUpdater getDialogUpdater(){
		if(updater==null)
			updater=new IDialogUpdater(){
			@Override
			public void setValue(Dialog dialog, Object value) {
			}
			@Override
			public Object getValue(Dialog dialog) {
				return 0;
			}
			@Override
			public void prepareDialog(Dialog dialog) {
			}
			@Override
			public Class<?> getType() {
				return Void.TYPE;
			}
			@Override
			public boolean isValid(Object[] value) {
				return true;
			}
		};
		return updater;
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable state = super.onSaveInstanceState();
		PreferenceState ret = new PreferenceState(state,this.getClass());
		Bundle b=ret.getBundle();
		Dialog dialog=alertDialog==null ? null : alertDialog.get();
		Class<?> type=getDialogUpdater().getType();
		Object value=getDialogUpdater().getValue(dialog);
		if(type.equals(Integer.TYPE))
			b.putInt("value",(Integer)value);
		else if(type.equals(Long.TYPE))
			b.putLong("value",(Long)value);
		else if(type.equals(Float.TYPE))
			b.putFloat("value",(Float)value);
		else if(type.equals(String.class))
			b.putString("value",(String)value);
		else if(!type.equals(Void.TYPE))
			throw new IllegalStateException();
		boolean showing=dialog==null ? false : dialog.isShowing();
		ret.getBundle().putBoolean("showing",showing);
		state=ret;
		// dismiss the dialog here to avoid leaking it
		if(showing){dialog.dismiss(); alertDialog=null;}
		return state;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		boolean isState=PreferenceState.isPreferenceState(state,this.getClass());
		//DebugUtility.log("isState=%s state=%s",isState,
		//	isState ? ((PreferenceState)state).getSuperState() : state);
		super.onRestoreInstanceState(isState ? ((PreferenceState)state).getSuperState() : state);
		if(isState){
			Bundle b=((PreferenceState)state).getBundle();
			boolean showing=b.getBoolean("showing");
			if(showing){
				showDialog();
			}
			Class<?> type=getDialogUpdater().getType();
			Object value=null;
			if(type.equals(Integer.TYPE))
				value=b.getInt("value");
			else if(type.equals(Long.TYPE))
				value=b.getLong("value");
			else if(type.equals(Float.TYPE))
				value=b.getFloat("value");
			else if(type.equals(String.class))
				value=b.getString("value");
			else if(!type.equals(Void.TYPE))
				throw new IllegalStateException();
			getDialogUpdater().setValue(alertDialog==null ? null : alertDialog.get(),value);
		}
	}


	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		Class<?> type=getDialogUpdater().getType();
		if(type.equals(Integer.TYPE) || type.equals(Void.TYPE))
			return a.getInteger(index,0);
		else if(type.equals(Long.TYPE)){
			String s=a.getString(index);
			return (long)Long.parseLong((s!=null) ? s : ""); // Unfortunately there's no getLong in TypedArray
		}
		else if(type.equals(Float.TYPE))
			return a.getFloat(index,0f);
		else if(type.equals(String.class))
			return a.getString(index);
		else
			throw new IllegalStateException();
	}

	private String defaultFormat(){
		Class<?> type=getDialogUpdater().getType();
		if(type.equals(Integer.TYPE) || type.equals(Long.TYPE) || type.equals(Float.TYPE))
			return "%d";
		else
			return "%s";
	}

	private void persist(Object value){
		Class<?> type=getDialogUpdater().getType();
		if(type.equals(Integer.TYPE))
			persistInt((Integer)value);
		else if(type.equals(Long.TYPE))
			persistLong((Long)value);
		else if(type.equals(Float.TYPE))
			persistFloat((Float)value);
		else if(type.equals(String.class))
			persistString((String)value);
		else if(!type.equals(Void.TYPE))
			throw new IllegalStateException();
	}

	private Object getPersisted(Object value){
		Class<?> type=getDialogUpdater().getType();
		if(type.equals(Integer.TYPE) || type.equals(Void.TYPE))
			return getPersistedInt(value==null ? 0 : (Integer)value);
		else if(type.equals(Long.TYPE))
			return getPersistedLong(value==null ? 0 : (Long)value);
		else if(type.equals(Float.TYPE))
			return getPersistedFloat(value==null ? 0 : (Float)value);
		else if(type.equals(String.class))
			return getPersistedString(value==null ? "" : (String)value);
		else
			throw new IllegalStateException();
	}

	private String settingSummary = null;
	private int layoutResource = 0;
	private int dialogTitle = 0;
	private WeakReference<AlertDialog> alertDialog = null;
	private int negativeButton = 0;
	private int positiveButton = 0;
	private int dialogMessage = 0;

	public AlertDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		negativeButton=attrs.getAttributeResourceValue(
				"http://schemas.android.com/apk/res/android","negativeButtonText",0);
		String strNeg=attrs.getAttributeValue(
				"http://schemas.android.com/apk/res/android","negativeButtonText");
		positiveButton=attrs.getAttributeResourceValue(
				"http://schemas.android.com/apk/res/android","positiveButtonText",0);
		String strPos=attrs.getAttributeValue(
				"http://schemas.android.com/apk/res/android","positiveButtonText");
		if(negativeButton==0 && strNeg==null)
			negativeButton=android.R.string.cancel;
		if(positiveButton==0 && strPos==null)
			positiveButton=android.R.string.ok;
		int summaryId=attrs.getAttributeResourceValue(
				"http://schemas.android.com/apk/res/android","summary",0);
		layoutResource=attrs.getAttributeResourceValue(
				"http://schemas.android.com/apk/res/android","dialogLayout",0);
		dialogTitle=attrs.getAttributeResourceValue(
				"http://schemas.android.com/apk/res/android","dialogTitle",0);
		dialogMessage=attrs.getAttributeResourceValue(
				"http://schemas.android.com/apk/res/android","dialogMessage",0);
		if(summaryId!=0){
			settingSummary=context.getResources().getString(summaryId);
		}
	}


	private void showDialog() {
		AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
		if(dialogMessage!=0 && "string".equals(getContext().getResources().getResourceTypeName(dialogMessage)))
			builder=builder.setMessage(dialogMessage);
		if(dialogTitle!=0)
			builder=builder.setTitle(dialogTitle);
		if(positiveButton!=0){
			builder=builder.setPositiveButton(positiveButton,new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					Object newValue=getDialogUpdater().getValue(alertDialog==null ? null : alertDialog.get());
					Object[] validatedValue=new Object[]{newValue};
					if(!getDialogUpdater().isValid(validatedValue)){
						Toast.makeText(getContext(),AppManager.getStringResourceValue("textnotvalid","Not valid"),Toast.LENGTH_SHORT).show();
						showDialog();
					} else {
						if(callChangeListener(validatedValue[0])){
							persist(validatedValue[0]);
							if(!getDialogUpdater().getType().equals(Void.TYPE))
								setSummary(String.format(settingSummary==null ? defaultFormat() : settingSummary,
										getPersisted(null)));
						}
						alertDialog=null;
					}
				}});
		}
		if(negativeButton!=0){
			builder=builder.setNegativeButton(negativeButton,new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					alertDialog=null;
				}});
		}
		if(getContext() instanceof Activity && layoutResource!=0){
			builder=builder.setView(((Activity)getContext()).getLayoutInflater()
					.inflate(layoutResource,null));
		}
		AlertDialog dialog=builder.show();
		getDialogUpdater().prepareDialog(dialog);
		getDialogUpdater().setValue(dialog,this.getPersisted(null));
		alertDialog=new WeakReference<AlertDialog>(dialog);
	}

	@Override
	protected void onClick() {
		super.onClick();
		showDialog();
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if (restorePersistedValue) {
			getDialogUpdater().setValue(alertDialog==null ? null : alertDialog.get(),
					this.getPersisted(defaultValue));
			if(!getDialogUpdater().getType().equals(Void.TYPE))
				setSummary(String.format(settingSummary==null ? defaultFormat() : settingSummary,this.getPersisted(defaultValue)));
		} else {
			persist(defaultValue);
			getDialogUpdater().setValue(alertDialog==null ? null : alertDialog.get(),
					defaultValue);
			if(!getDialogUpdater().getType().equals(Void.TYPE))
				setSummary(String.format(settingSummary==null ? defaultFormat() : settingSummary,this.getPersisted(defaultValue)));
		}
	}

}