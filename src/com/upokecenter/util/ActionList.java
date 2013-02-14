package com.upokecenter.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for holding tasks that can be referred to by integer index.
 * Useful for communication between two classes when neither class has a
 * direct reference to the other, such as between two Activities or between
 * an Activity and a Preference in Android.
 * 
 */
public final class ActionList<T> {

	private List<IAction<T>> actions;
	private Object syncRoot=new Object();
	
	public ActionList(){
		actions=new ArrayList<IAction<T>>();
	}
	
	public boolean triggerActionOnce(int actionID, T... parameters){
		IAction<T> action=null;
		if(actionID<0)return false;
		synchronized(syncRoot){
			if(actionID>=actions.size())
				return false;
			action=actions.get(actionID);
			actions.set(actionID,null);
		}
		if(action==null)return false;
		action.action(parameters);
		return true;
	}
	
	public int registerAction(IAction<T> action){
		synchronized(syncRoot){
			for(int i=0;i<actions.size();i++){
				if(actions.get(i)==null){
					actions.set(i,action);
					return i;
				}
			}
			int ret=actions.size();
			actions.add(action);
			return ret;
		}
	}
}
