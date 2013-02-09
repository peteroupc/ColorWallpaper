package com.upokecenter.android.colorwallpaper;

import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.upokecenter.android.util.AlertDialogPreference;
import com.upokecenter.android.util.IDialogUpdater;

public class HuePreference extends AlertDialogPreference {
	
	public HuePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}


	IDialogUpdater updater=null;

	@Override protected IDialogUpdater getDialogUpdater(){
		if(updater==null)
			updater=new IDialogUpdater(){
			@Override
			public void setValue(Dialog dialog, Object value) {
				if(dialog==null)return;
				SeekBar seekBar=(SeekBar)dialog.findViewById(R.id.seekBar1);
				if(seekBar==null)return;
				seekBar.setProgress((Integer)value);
			}

			@Override
			public Object getValue(Dialog dialog) {
				if(dialog==null)return 0;
				SeekBar seekBar=(SeekBar)dialog.findViewById(R.id.seekBar1);
				if(seekBar==null)return 0;
				return seekBar.getProgress();
			}


			@Override
			public void prepareDialog(Dialog dialog) {
				if(dialog==null)return;
				final Dialog d=dialog;
				SeekBar seekBar=(SeekBar)dialog.findViewById(R.id.seekBar1);
				if(seekBar==null)return;
				TextView text=(TextView)d.findViewById(R.id.textView1);
				if(text==null)return;
				String seq=text.getResources().getString(R.string.xdegrees).toString();
				text.setText(String.format(seq,seekBar.getProgress()));
				seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
					@Override
					public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
						TextView text=(TextView)d.findViewById(R.id.textView1);
						String seq=text.getResources().getString(R.string.xdegrees).toString();
						text.setText(String.format(seq,arg0.getProgress()));
					}

					@Override public void onStartTrackingTouch(SeekBar seekBar) {}
					@Override public void onStopTrackingTouch(SeekBar seekBar) {}
				});
			}

			@Override
			public Class<?> getType() {
				return Integer.TYPE;
			}
		};
		return updater;
	}
}
