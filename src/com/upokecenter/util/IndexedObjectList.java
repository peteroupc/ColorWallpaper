package com.upokecenter.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.upokecenter.android.util.DebugUtility;

public final class IndexedObjectList<T> {
	private List<T> strongrefs=new ArrayList<T>();
	private List<WeakReference<T>> weakrefs=new ArrayList<WeakReference<T>>();
	private Object syncRoot=new Object();
	
	public int sendObject(T value){
		if(value==null)return -1; // Special case for null
		synchronized(syncRoot){
			for(int i=0;i<strongrefs.size();i++){
				if(strongrefs.get(i)==null){
					if(weakrefs.get(i)==null ||
					   weakrefs.get(i).get()==null){
						// If the object is garbage collected
						// the index is available for use again
						DebugUtility.log("Adding object %d",i);
						strongrefs.set(i,value);
						weakrefs.set(i,new WeakReference<T>(value));
						return i;
					}
				}
			}
			// Keep a strong and weak reference of
			// the same object
			int ret=strongrefs.size();
			DebugUtility.log("Adding object %d",ret);
			strongrefs.add(value);
			weakrefs.add(new WeakReference<T>(value));
			return ret;
		}
	}
	
	public T receiveObject(int index){
		if(index<0)return null;
		T ret=null;
		synchronized(syncRoot){
			if(index>=strongrefs.size())return null;
			// Remove the strong reference
			ret=strongrefs.get(index);
			if(ret==null)throw new IllegalStateException();
			strongrefs.set(index,null);
		}
		return ret;
	}
}
