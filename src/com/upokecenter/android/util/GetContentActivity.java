package com.upokecenter.android.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.widget.Toast;

import com.upokecenter.util.ActionList;

public class GetContentActivity extends Activity { 
	private static final class CameraIntentAsyncTask extends
			AsyncTask<Void, Void, File> {
		private final GetContentActivity thisActivity;
		private final Intent chooser;

		private CameraIntentAsyncTask(GetContentActivity thisActivity,
				Intent chooser) {
			this.thisActivity = thisActivity;
			this.chooser = chooser;
		}

		@Override
		protected File doInBackground(Void... arg0) {
			return StorageUtility.getCameraFolderUniqueFileName();
		}

		@Override protected void onPostExecute(File param){
			if(param!=null){
				Intent cameraIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);							
				cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(param));
				thisActivity.externalFile=param.toString();
				chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,new Intent[]{cameraIntent});
			}
			thisActivity.startActivityForResult(chooser,0xabcd);
		}
		
		
	}

	private static final class MediaStoreAsyncTask extends
			AsyncTask<Uri, Void, String> {
		private final GetContentActivity thisInstance;

		private MediaStoreAsyncTask(GetContentActivity thisInstance) {
			this.thisInstance = thisInstance;
		}

		@Override
		protected String doInBackground(Uri... params) {
			Cursor cursor=thisInstance.getContentResolver().query(params[0], 
					new String[]{"_data"},null,null,null);
			if(cursor==null)return null;
			try {
				cursor.moveToFirst();
				return cursor.getString(0);
			} finally {
				cursor.close();
			}
		}

		@Override public void onPostExecute(String path){
			if(path==null){
				Toast.makeText(thisInstance,
						AppManager.getStringResourceValue("imagenotsaved",
								"We couldn't get the image."),Toast.LENGTH_SHORT).show();	
			}
			if(thisInstance.callback>=0){
				GetContentActivity.callbacks.triggerActionOnce(thisInstance.callback,path);
			}
			thisInstance.finish();						
		}
	}

	private static final class BitmapWriteAsyncTask extends
			AsyncTask<Bitmap, Void, File> {
		private final GetContentActivity thisInstance;
		private final Bitmap bitmap;

		private BitmapWriteAsyncTask(GetContentActivity thisInstance,
				Bitmap bitmap) {
			this.thisInstance = thisInstance;
			this.bitmap = bitmap;
		}

		@Override
		protected File doInBackground(Bitmap... arg0) {
			File file=StorageUtility.getCameraFolderUniqueFileName();
			if(file==null)return null;
			OutputStream fs;
			new File(file.getParent()).mkdirs();
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

		@Override
		public void onPostExecute(File param){
			Toast.makeText(thisInstance,
					(param!=null) ? AppManager.getStringResourceValue("imagesaved",
							"Camera image saved.") :
								AppManager.getStringResourceValue("imagenotsaved",
										"We couldn't get the image."),
										Toast.LENGTH_SHORT).show();
			if(thisInstance.callback>=0){
				GetContentActivity.callbacks.triggerActionOnce(
						thisInstance.callback,param==null ? null : param.toString());
			}
			thisInstance.finish();
		}
	}

	private Activity getThis(){ return this; }

	private static ActionList<String> callbacks=new ActionList<String>();

	public static ActionList<String> getCallbacks(){
		return callbacks;
	}

	int callback=-1;

	String externalFile=null;



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if(requestCode == 0xabcd){
			if(resultCode==RESULT_OK && intent==null && externalFile!=null){
				if(callback>=0){
					callbacks.triggerActionOnce(callback,externalFile);
				}
				finish();
				return;
			}
			if(intent!=null && intent.getExtras()!=null){
				Parcelable parcel=intent.getExtras().getParcelable("data");
				if(parcel!=null && parcel instanceof Bitmap){
					final Bitmap bitmap=(Bitmap)parcel;
					final GetContentActivity thisInstance=this;
					new BitmapWriteAsyncTask(thisInstance, bitmap).execute(bitmap);
					return;
				}
				if(resultCode==RESULT_OK && parcel==null){
					Toast.makeText(AppManager.getApplication(),
							AppManager.getStringResourceValue("imagenotsaved",
									"We couldn't get the image."),Toast.LENGTH_SHORT).show();
					if(callback>=0){
						callbacks.triggerActionOnce(callback,(String)null);
					}
					finish();
					return;
				}
			} else if(intent!=null && intent.getData()!=null){
				final GetContentActivity thisInstance=this;
				new MediaStoreAsyncTask(thisInstance).execute(intent.getData());
				return;
			}
			if(callback>=0){
				callbacks.triggerActionOnce(callback,(String)null);
			}		
			finish();
		}
	}

	@Override public void onSaveInstanceState(Bundle b){
		b.putInt("callback",callback);
		DebugUtility.log("saving callback %d",callback);
	}

	@Override
	public void onCreate(Bundle b){
		super.onCreate(b);
		if(b!=null){
			callback=b.getInt("callback");
			DebugUtility.log("restoring callback %d",callback);
		}
		if(b==null){
			AppManager.initialize(getThis());
			Intent intent=getThis().getIntent();
			if(Intent.ACTION_GET_CONTENT.equals(intent.getAction())){
				Intent myIntent=new Intent(intent);
				callback=intent.getIntExtra("com.upokecenter.android.extra.CALLBACK",-1);
				myIntent.setType(intent.getType()!=null ? intent.getType() : "image/*");
				final Intent chooser=Intent.createChooser(myIntent, 
						intent.getStringExtra(Intent.EXTRA_TITLE));
				if("image/*".equals(myIntent.getType())){
					// Add camera intent only if we can write to external storage
					// and the image media type is specified
					final GetContentActivity thisActivity=this;
					new CameraIntentAsyncTask(thisActivity, chooser).execute();
				} else {
					startActivityForResult(chooser,0xabcd);					
				}
			}
		}
	}
}
