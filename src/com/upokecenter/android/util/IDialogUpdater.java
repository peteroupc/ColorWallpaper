package com.upokecenter.android.util;


import android.app.Dialog;


public interface IDialogUpdater {
	public void prepareDialog(Dialog dialog);
	public void setValue(Dialog Object, Object value);
	public Object getValue(Dialog dialog);
	public Class<?> getType();
}
