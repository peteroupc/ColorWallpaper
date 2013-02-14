package com.upokecenter.android.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Locale;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.widget.Toast;

import com.upokecenter.util.ActionList;

public class GetContentActivity extends Activity { 
	private Activity getThis(){ return this; }

	private static ActionList<String> callbacks=new ActionList<String>();

	public static ActionList<String> getCallbacks(){
		return callbacks;
	}

	int callback=-1;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == 0xabcd){
			if(data!=null && data.getExtras()!=null){
				Parcelable parcel=data.getExtras().getParcelable("data");
				if(this.checkCallingOrSelfPermission(
						Manifest.permission.WRITE_EXTERNAL_STORAGE)==
						PackageManager.PERMISSION_GRANTED){
					if(parcel!=null && parcel instanceof Bitmap){
						final Bitmap bitmap=(Bitmap)parcel;
						if(Environment.getExternalStorageState().equals(
								Environment.MEDIA_MOUNTED)){
							// A bitmap is stored
							File storagedir=new File(Environment.getExternalStorageDirectory(),"DCIM");
							storagedir=new File(storagedir,"Camera");
							final File storage=storagedir;
							new AsyncTask<Bitmap,Void,File>(){

								@Override
								protected File doInBackground(Bitmap... arg0) {
									Calendar calendar=Calendar.getInstance();
									int i=0;
									storage.mkdirs();
									while(true){
										String string=String.format(Locale.US,
												"%04d-%02d-%02d--%02d-%02d-%02d-%d.png",
												calendar.get(Calendar.YEAR),
												calendar.get(Calendar.MONTH)+1,
												calendar.get(Calendar.DAY_OF_MONTH),
												calendar.get(Calendar.HOUR_OF_DAY),
												calendar.get(Calendar.MINUTE),
												calendar.get(Calendar.SECOND),i
												);
										File file=new File(storage,string);
										if(!file.exists()){
											OutputStream fs;
											try {
												fs = new FileOutputStream(file);
												try {
													return bitmap.compress(Bitmap.CompressFormat.PNG,0,fs) ? file : null;
												} finally {
													if(fs!=null)
														try { fs.close(); } catch (IOException e) {}
												}
											} catch (FileNotFoundException e) {
												return null;
											}
										}
										i++;
									}
								}
								@Override
								public void onPostExecute(File param){
									if(param!=null){
										Toast.makeText(GetContentActivity.this,
												AppManager.getStringResourceValue("imagesaved",
														"Camera image saved."),Toast.LENGTH_SHORT).show();
									} else {
										Toast.makeText(GetContentActivity.this,
												AppManager.getStringResourceValue("imagenotsaved",
														"Camera image couldn't be saved."),Toast.LENGTH_SHORT).show();
									}
									if(callback>=0){
										callbacks.triggerActionOnce(callback,param==null ? null : param.toString());
									}
									finish();
								}
							}.execute(bitmap);
							return;
						}
					}
					if(resultCode==RESULT_OK){
						Toast.makeText(GetContentActivity.this,
								AppManager.getStringResourceValue("imagenotsaved",
										"Camera image couldn't be saved."),Toast.LENGTH_SHORT).show();
						if(callback>=0){
							callbacks.triggerActionOnce(callback,(String)null);
						}
						finish();
						return;
					}
				}
			} else if(data!=null && data.getData()!=null){
				new AsyncTask<Uri,Void,String>(){
					@Override
					protected String doInBackground(Uri... params) {
						Cursor cursor=getContentResolver().query(params[0], 
								new String[]{"_data"},null,null,null);
						try {
							cursor.moveToFirst();
							return cursor.getString(0);
						} finally {
							cursor.close();
						}
					}
					@Override public void onPostExecute(String path){
						if(callback>=0){
							callbacks.triggerActionOnce(callback,path);
						}
						finish();						
					}
				}.execute(data.getData());
				return;
			}
			if(callback>=0){
				callbacks.triggerActionOnce(callback,(String)null);
			}		
			finish();
		}
	}

	@Override
	public void onCreate(Bundle b){
		super.onCreate(b);
		if(b==null){
			AppManager.initialize(getThis());
			Intent intent=getThis().getIntent();
			if(Intent.ACTION_GET_CONTENT.equals(intent.getAction())){
				Intent myIntent=new Intent(intent);
				callback=intent.getIntExtra("com.upokecenter.android.extra.CALLBACK",-1);
				myIntent.setType(intent.getType()!=null ? intent.getType() : "image/*");
				Intent chooser=Intent.createChooser(myIntent, 
						intent.getStringExtra(Intent.EXTRA_TITLE));
				if(this.checkCallingOrSelfPermission(
						Manifest.permission.WRITE_EXTERNAL_STORAGE)==
						PackageManager.PERMISSION_GRANTED &&
						Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) &&
						"image/*".equals(myIntent.getType())){
					// Add camera intent only if we can write to external storage
					// and the image media type is specified
					chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,new Intent[]{ 
							new Intent(MediaStore.ACTION_IMAGE_CAPTURE)});
				}
				startActivityForResult(chooser,0xabcd);
			}
		}
	}
}
