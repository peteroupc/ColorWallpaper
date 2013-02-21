package com.upokecenter.android.ui;

import com.upokecenter.android.util.AppManager;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class ContinuousValuePreference extends AlertDialogPreference {

	int minValue,maxValue;
	String label;
	int seekBarID,textViewID;
	IDialogUpdater updater = null;

	public ContinuousValuePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		AppManager.initialize(context);
		TypedArray t=context.getTheme().obtainStyledAttributes(attrs,
				AppManager.getStyleableResourceGroup("ContinuousValuePreference"),0,0);
		try {
			int minval=t.getInt(AppManager.getStyleableResource("ContinuousValuePreference_minValue"),0);
			int maxval=t.getInt(AppManager.getStyleableResource("ContinuousValuePreference_maxValue"),0);
			minValue=Math.min(minval,maxval);
			maxValue=Math.max(minval,maxval);
			seekBarID=AppManager.getIdResource("seekBar1");
			textViewID=AppManager.getIdResource("textView1");
			label=t.getString(AppManager.getStyleableResource("ContinuousValuePreference_label"));
			//DebugUtility.log("values=%d %d [%s]",minValue,maxValue,label);
			if(label==null)label="%d";
		} finally {
			t.recycle();
		}
	}
	
	private int valueToProgress(int value){
		float valueAsProgress=(maxValue==minValue) ? 0 : ((value)-minValue)*1.0f/(maxValue-minValue);
		//DebugUtility.log("valueToProgress %d->%d",value,Math.round(valueAsProgress*10000f));
		if(valueAsProgress<0)return 0;
		if(valueAsProgress>1)return 10000;
		return (int)(valueAsProgress*10000f);
	}
	
	private int progressToValue(int progress){
		int ret=minValue+Math.round((maxValue-minValue)*(progress/10000f));
		//DebugUtility.log("progressToValue %d->%d",progress,ret);
		if(progress<0)return minValue;
		if(progress>10000)return maxValue;
		return ret;
	}
	

	@Override
	protected IDialogUpdater getDialogUpdater() {
		if(updater==null)
			updater=new IDialogUpdater(){
			@Override
			public void setValue(Dialog dialog, Object value) {
				if(dialog==null)return;
				SeekBar seekBar=(SeekBar)dialog.findViewById(seekBarID);
				if(seekBar==null)return;
				seekBar.setProgress(valueToProgress((Integer)value));
			}
	
			@Override
			public Object getValue(Dialog dialog) {
				if(dialog==null)return 0;
				SeekBar seekBar=(SeekBar)dialog.findViewById(seekBarID);
				if(seekBar==null)return 0;
				return progressToValue(seekBar.getProgress());
			}
	
	
			@Override
			public void prepareDialog(Dialog dialog) {
				if(dialog==null)return;
				final Dialog d=dialog;
				SeekBar seekBar=(SeekBar)dialog.findViewById(seekBarID);
				if(seekBar==null)return;
				TextView text=(TextView)d.findViewById(textViewID);
				if(text==null)return;
				seekBar.setMax(10000);
				text.setText(String.format(label,progressToValue(seekBar.getProgress())));
				seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
					@Override
					public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
						TextView text=(TextView)d.findViewById(textViewID);
						text.setText(String.format(label,progressToValue(arg0.getProgress())));
					}
	
					@Override public void onStartTrackingTouch(SeekBar seekBar) {}
					@Override public void onStopTrackingTouch(SeekBar seekBar) {}
				});
			}
	
			@Override
			public Class<?> getType() {
				return Integer.TYPE;
			}

			@Override
			public boolean isValid(Object[] value) {
				return ((Integer)value[0]>=minValue && (Integer)value[0]<=maxValue);
			}
		};
		return updater;
	}

}