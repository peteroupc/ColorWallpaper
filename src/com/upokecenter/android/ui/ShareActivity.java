package com.upokecenter.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.upokecenter.android.util.AppManager;

//public class ShareFragment extends android.support.v4.app.Fragment { private Activity getThis(){ return this.getActivity(); }
public class ShareActivity extends Activity { private Activity getThis(){ return this; }

private class IntentHolder {
	Intent intent;
	String packageName;
	String label;
	public IntentHolder(String packageName, String label){
		this.packageName=packageName;
		this.label=label;
	}
	@Override public String toString(){
		return label;
	}
}

AlertDialog dialog=null;
boolean useChooser=false;

private boolean advancedShare(Intent sourceIntent){
	Bundle bundle=sourceIntent.getExtras();
	String action=sourceIntent.getAction();
	String title=bundle.getString(Intent.EXTRA_TITLE);
	String message=bundle.getString(Intent.EXTRA_TEXT);
	String subject=bundle.getString(Intent.EXTRA_SUBJECT);
	final Activity thisActivity=getThis();
	Intent share = new Intent(Intent.ACTION_SEND);
	share.setType(sourceIntent.getType());
	share.putExtra(Intent.EXTRA_TEXT,message);
	share.putExtra(Intent.EXTRA_SUBJECT,subject);
	List<Intent> intents = new ArrayList<Intent>();
	List<IntentHolder> resolveInfos=new ArrayList<IntentHolder>();
	for(ResolveInfo intentAct : thisActivity.getPackageManager().queryIntentActivities(share, 0)) {
		resolveInfos.add(new IntentHolder(
				intentAct.activityInfo.packageName,
				intentAct.loadLabel(thisActivity.getPackageManager()).toString()
				));
	}
	// Sort intent list by label
	Collections.sort(resolveInfos,new Comparator<IntentHolder>(){
		@Override
		public int compare(IntentHolder a, IntentHolder b){
			return a.label.compareTo(b.label); // compare the labels
		}
	});
	for(IntentHolder intentAct : resolveInfos) {
		Intent intent = new Intent(action);
		intent.setType("text/plain");
		String packageName=intentAct.packageName;
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_TEXT, message);
		if(useChooser && (Build.VERSION.SDK_INT<Build.VERSION_CODES.ICE_CREAM_SANDWICH)){
			modifyIntent(intent);
		}
		intent.setPackage(packageName);
		intentAct.intent=intent;
		intents.add(intent);
	}
	if(intents.size()<=0){
		// No intents available for sharing
		Toast.makeText(thisActivity.getApplicationContext(),
				AppManager.getStringResourceValue("nowaytoshare","No applications for sharing are installed."),
				Toast.LENGTH_SHORT).show();
		return false;
	} else {
		if(useChooser){
			Intent chooser = Intent.createChooser(intents.remove(intents.size()-1),title);
			chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,
					intents.toArray(new Parcelable[]{}));
			this.startActivityForResult(chooser,0);
			return true;
		} else {
			final List<IntentHolder> ri=resolveInfos;
			AlertDialog.Builder builder=DialogUtility.createBuilder(thisActivity);
			builder=builder.setAdapter(
					new ArrayAdapter<IntentHolder>(thisActivity,
							AppManager.getLayoutResource("textimagelayout"),
							AppManager.getIdResource("text"),
							resolveInfos){
						@Override
						public View getView(int position, View convertView, ViewGroup parent){
							ImageView image;
							if(convertView!=null){
								image=(ImageView)convertView.findViewById(AppManager.getIdResource("icon"));
								if(image!=null)return convertView;
							}
							View view=super.getView(position,convertView,parent);
							image=(ImageView)view.findViewById(AppManager.getIdResource("icon"));
							TextView text=(TextView)view.findViewById(AppManager.getIdResource("text"));
							if(Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB){
								// this is a hack, since otherwise the text will be white on
								// white if the activity's theme is "dark"
								text.setTextColor(Color.BLACK);
							}
							if(image!=null){
								try {
									image.setImageDrawable(getContext().getPackageManager().getActivityIcon(getItem(position).intent));
								} catch (NameNotFoundException e) {}
							}
							return view;
						}
					},new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface di, int position) {
							di.dismiss(); // Dismiss -before- starting the activity
							Intent intent=ri.get(position).intent;
							modifyIntent(intent);
							thisActivity.startActivity(intent);
						}
					}
					);
			builder=builder.setTitle(title);
			dialog=builder.create();
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){
				@Override
				public void onDismiss(DialogInterface arg0) {
					//DebugUtility.log("onDismiss was called");
					doFinish();
				}
			});
			dialog.show();
			return true;
		}
	}
}

boolean savingState=false;
private void doFinish(){
	//DebugUtility.log("doFinish was called");
	if(!savingState){
		//DebugUtility.log("now finishing");
		getThis().finish();
	}
}

private void modifyIntent(Intent intent){
	String packageName=intent.getPackage();
	String intentText=intent.getStringExtra(Intent.EXTRA_TEXT);
	if(packageName!=null){
		if(packageName.equals("com.facebook.katana")){
			// Extract URL from message for Facebook
			Matcher matcher=Pattern.compile("(geo\\:|[a-z\\-]+\\:\\/\\/)\\S+").matcher(intentText);
			if(matcher.find()){
				intentText=matcher.group();
				intent.putExtra(Intent.EXTRA_TEXT, intentText);
			}
		}
	}
}

@Override
public void onSaveInstanceState(Bundle b){
	super.onSaveInstanceState(b);
	savingState=true;
	//DebugUtility.log("saving state");
	if(dialog!=null){
		dialog.dismiss();
		dialog=null;
	}
}

@Override
public void onActivityResult(int requestCode, int resultCode, Intent data){
	super.onActivityResult(requestCode,resultCode,data);
	getThis().finish();
}

@Override
public void onCreate(Bundle b){
	super.onCreate(b);
	useChooser=(Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH);
	if(b==null || !useChooser){
		AppManager.initialize(getThis());
		Intent intent=getThis().getIntent();
		if(Intent.ACTION_SEND.equals(intent.getAction())){
			if(!advancedShare(intent)){
				getThis().finish();
			}
		} else {
			getThis().finish();
		}
	} else {
		// chooser activity is already showing
		getThis().finish();
	}
}
}
