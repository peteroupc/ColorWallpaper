package com.upokecenter.android.ui;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import com.upokecenter.android.util.AppManager;

public class AlertDialogActivity extends Activity {

	WeakReference<AlertDialog> alertDialog;

	private Context getContext(){
		return this;
	}

	private void showDialog() {
		int positiveButton=android.R.string.ok;
		int negativeButton=android.R.string.cancel;
		int layoutResource=AppManager.getLayoutResource("edittextlayout");
		AlertDialog.Builder builder=DialogUtility.createBuilder(getContext());
		if(dialogMessage!=null)
			builder=builder.setMessage(dialogMessage);
		if(dialogTitle!=null)
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
						Intent intent=new Intent(getContext(),
								AlertDialogActivity.class);
						intent.putExtra("result",validatedValue[0].toString());
						setResult(Activity.RESULT_OK,intent);
						finish();
						alertDialog=null;
					}
				}});
		}
		if(negativeButton!=0){
			builder=builder.setNegativeButton(negativeButton,new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					Intent intent=new Intent(getContext(),
							AlertDialogActivity.class);
					setResult(Activity.RESULT_CANCELED,intent);
					finish();
					alertDialog=null;
				}});
		}
		if(getContext() instanceof Activity && layoutResource!=0){
			builder=builder.setView(((Activity)getContext()).getLayoutInflater()
					.inflate(layoutResource,null));
		}
		builder=builder.setOnCancelListener(new DialogInterface.OnCancelListener(){
			@Override public void onCancel(DialogInterface dialog) {
				Intent intent=new Intent(getContext(),
						AlertDialogActivity.class);
				setResult(Activity.RESULT_CANCELED,intent);
				finish();
				alertDialog=null;
			}});
		AlertDialog dialog=builder.show();
		getDialogUpdater().prepareDialog(dialog);
		getDialogUpdater().setValue(dialog,startValue);
		alertDialog=new WeakReference<AlertDialog>(dialog);
	}

	protected IDialogUpdater getDialogUpdater() {
		return UriPreference.staticGetDialogUpdater();
	}

	String dialogMessage=null;
	String dialogTitle=null;
	String startValue=null;
	int layoutResource=0;

	@Override
	protected void onSaveInstanceState(Bundle b){
		super.onSaveInstanceState(b);
		if(alertDialog!=null){
			AlertDialog d=alertDialog.get();
			if(d!=null){
				b.putString("startValue",(String)getDialogUpdater().getValue(d));
				return;
			}
		}
		b.putString("startValue",(String)startValue);
	}
	
	@Override
	protected void onDestroy(){
		if(alertDialog!=null){
			AlertDialog d=alertDialog.get();
			if(d!=null)d.dismiss();
		}
		super.onDestroy();
	}
	
	@Override
	protected void onCreate(Bundle b){
		super.onCreate(b);
		AppManager.initialize(this);
		Intent intent=getIntent();
		dialogMessage=intent.getStringExtra("dialogMessage");
		dialogTitle=intent.getStringExtra("dialogTitle");
		if(b==null){
			startValue=intent.getStringExtra("startValue");			
		} else {
			startValue=b.getString("startValue");
		}
		layoutResource=intent.getIntExtra("layoutResource",0);
		showDialog();
	}

}
