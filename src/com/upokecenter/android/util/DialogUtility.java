package com.upokecenter.android.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;

import com.upokecenter.util.Reflection;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Build;

public final class DialogUtility {
	private DialogUtility(){}
	static HashMap<WeakReference<DialogInterface>,Integer> choices=new HashMap<WeakReference<DialogInterface>,Integer>();
	public static void clean(){
		for(WeakReference<DialogInterface> key : new HashSet<WeakReference<DialogInterface>>(choices.keySet())){
			DialogInterface k=key.get();
			if(k!=null){
				k.dismiss();
			}
		}
		choices.clear();
	}
	

	public static AlertDialog.Builder createBuilder(Context context){
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			// use Device Default Dark theme in ICS and later
			return (AlertDialog.Builder)Reflection.construct(AlertDialog.Builder.class,context,4);
		} else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
			// use Holo Dark theme in Honeycomb
			return (AlertDialog.Builder)Reflection.construct(AlertDialog.Builder.class,context,2);
		} else {
			// This constructor will use the Traditional theme even in Honeycomb and later
			return new AlertDialog.Builder(context);
		}
	}
	private static void setChoice(DialogInterface c, int choice){
		if(c==null)return;
		for(WeakReference<DialogInterface> key : new HashSet<WeakReference<DialogInterface>>(choices.keySet())){
			DialogInterface k=key.get();
			if(c.equals(k)){
				choices.put(key,choice);
				return;
			} else if(k==null){
				choices.remove(key);
			}
		}
		choices.put(new WeakReference<DialogInterface>(c),choice);
	}
	private static int getChoice(DialogInterface c){
		if(c==null)return Integer.MIN_VALUE;
		for(WeakReference<DialogInterface> key : new HashSet<WeakReference<DialogInterface>>(choices.keySet())){
			DialogInterface k=key.get();
			if(c.equals(key.get())){
				return choices.get(key);
			} else if(k==null){
				choices.remove(key);
			}
		}
		return Integer.MIN_VALUE;
	}

	public static <T> void showChoices(Context context, int title, T[] items, IChoiceListener listener){
		showChoices(context,context.getResources().getString(title),items,listener);
	}
	public static void showChoices(Context context, int title, int arrayResource, IChoiceListener listener){
		showChoices(context,context.getResources().getString(title),arrayResource,listener);
	}
		
	private static <T> CharSequence[] toCharSequenceArray(T[] items){
		CharSequence[] ret=new CharSequence[items.length];
		for(int i=0;i<items.length;i++){
			ret[i]=items[i].toString();
		}
		return ret;
	}
	
	
	private static AlertDialog.Builder createBuilder(
			Context context,
			String title,
		   IChoiceListener listener
	){
		AlertDialog.Builder builder=createBuilder(context);
		if(title!=null)builder=builder.setTitle(title);
		builder=builder.setOnCancelListener(
				(listener==null) ? null : new DialogInterface.OnCancelListener(){
					@Override public void onCancel(DialogInterface di){
						setChoice(di,CANCELED);
					}
				});
		return builder;
	}
	
	private static void showBuilder(AlertDialog.Builder builder,
			   final IChoiceListener listener){
		AlertDialog dialog=builder.create();
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){
			@Override public void onDismiss(DialogInterface di){
				int choice=getChoice(di);
				if(choice!=Integer.MIN_VALUE)listener.onChoice(choice);
			}
		});
		setChoice(dialog,Integer.MIN_VALUE);
		dialog.show();
	}
	
	public static void showChoices(
			Context context,
			String title,
			int arrayResource,
			 IChoiceListener listener
			){
		AlertDialog.Builder builder=createBuilder(context,title,listener);
		if(arrayResource!=0){
				builder=builder.setItems(arrayResource,
								new DialogInterface.OnClickListener(){
					@Override public void onClick(DialogInterface di, int o){
						setChoice(di,o);
					}
				});
		}
		showBuilder(builder,listener);
	}

	public static <T> void showChoices(
			Context context,
			String title,
			T[] items,
			 IChoiceListener listener
			){
		if(items==null)throw new NullPointerException();
		AlertDialog.Builder builder=createBuilder(context,title,listener);
		if(items.length>0){
				builder=builder.setItems(
						(items[0] instanceof CharSequence) ? (CharSequence[])items
								: toCharSequenceArray(items),
								new DialogInterface.OnClickListener(){
					@Override public void onClick(DialogInterface di, int o){
						setChoice(di,o);
					}
				});
		}
		showBuilder(builder,listener);
	}
	public static final int POSITIVE=1;
	public static final int NEUTRAL=0;
	public static final int NEGATIVE=-1;
	public static final int CANCELED=-2;

	public static void showMessage(Context context, int title, int message, IChoiceListener listener){
		showMessage(context,title,message,android.R.string.yes, android.R.string.no,0,listener);
	}

	public static void showMessage(Context context, String title, String message, IChoiceListener listener){
		Resources resources=context.getResources();
		showMessage(context,title,message,resources.getString(android.R.string.yes), 
				resources.getString(android.R.string.no),null,listener);
	}	
	
	public static void showMessage(
			Context context,
			String title,
			String message, 
			String positiveButton, 
			String negativeButton,
			String neutralButton,
			final IChoiceListener listener
			){
		AlertDialog.Builder builder=createBuilder(context);
		if(title!=null)builder=builder.setTitle(title);
		if(message!=null)builder=builder.setMessage(message);
		if(positiveButton!=null)builder=builder.setPositiveButton(
				positiveButton,
				(listener==null) ? null : new DialogInterface.OnClickListener(){
					@Override public void onClick(DialogInterface di, int o){
						setChoice(di,POSITIVE);
					}
				});
		if(negativeButton!=null)builder=builder.setNegativeButton(
				negativeButton,
				(listener==null) ? null : new DialogInterface.OnClickListener(){
					@Override public void onClick(DialogInterface di, int o){
						setChoice(di,NEGATIVE);
					}
				});
		if(neutralButton!=null)builder=builder.setNeutralButton(
				neutralButton,
				(listener==null) ? null : new DialogInterface.OnClickListener(){
					@Override public void onClick(DialogInterface di, int o){
						setChoice(di,NEUTRAL);
					}
				});
		builder=builder.setOnCancelListener(
				(listener==null) ? null : new DialogInterface.OnCancelListener(){
					@Override public void onCancel(DialogInterface di){
						setChoice(di,CANCELED);
					}
				});
		AlertDialog dialog=builder.create();
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){
			@Override public void onDismiss(DialogInterface di){
				int choice=getChoice(di);
				if(choice!=Integer.MIN_VALUE)listener.onChoice(choice);
			}
		});
		setChoice(dialog,Integer.MIN_VALUE);
		dialog.show();
	}
	public static void showMessage(
			Context context,
			int title,
			int message, 
			int positiveButton, 
			int negativeButton,
			int neutralButton,
			IChoiceListener listener
			){
		Resources resources=context.getResources();
		showMessage(context,
				(title==0 ? null : resources.getString(title)),
				(message==0 ? null : resources.getString(message)),
				(positiveButton==0 ? null : resources.getString(positiveButton)),
				(negativeButton==0 ? null : resources.getString(negativeButton)),
				(neutralButton==0 ? null : resources.getString(neutralButton)),
				listener
				);
	}

}
