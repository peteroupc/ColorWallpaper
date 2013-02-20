package com.upokecenter.android.ui;

import java.net.URISyntaxException;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.text.method.SingleLineTransformationMethod;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import com.upokecenter.android.util.DebugUtility;

public class UriPreference
extends EditTextPreference {

	public UriPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	// We use "java.net.URI" to distinguish the URI
	// class from Android's own URI class, "android.net.Uri".
	public java.net.URI convertToUri(String s){
		int colon=s.indexOf(":");
		int slash=s.indexOf("/");
		java.net.URI uri=null;
		try {
			if(colon<0 || slash<0 || colon>slash){
				uri=new java.net.URI("http://"+s);
			} else {
				uri=new java.net.URI(s);
			}
			uri=new java.net.URI(
					uri.getScheme().toLowerCase(Locale.US),
					uri.getSchemeSpecificPart(),
					uri.getFragment());
			return uri;
		} catch(URISyntaxException e){
			return null;
		}
	}
		
	// Move the caret to the end of the text, for a better
	// user experience
	@Override protected void onAddEditTextToDialogView(View view, EditText editText){
		super.onAddEditTextToDialogView(view,editText);
		DebugUtility.log("onaddedit %s [dialog=%s]",editText,getDialog());
		editText.setText(this.getPersistedString(""));
		editText.setTransformationMethod(SingleLineTransformationMethod.getInstance());
		editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
		editText.setOnEditorActionListener(new TextView.OnEditorActionListener(){
			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
				return true;
			}
		});
		// This must be last, in order to move the text to the end
		editText.setSelection(editText.getText().length());
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult){
		// call super method first, before we get the text
		super.onDialogClosed(positiveResult);
		if(positiveResult){
			String uriText=getText();
			java.net.URI uri=convertToUri(uriText.toString());
			if(uri==null){
				uriText="";
			} else {
				uriText=uri.toString();
			}
			setSummary(String.format("%s",uriText));			
		}
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if (restorePersistedValue) {
			setSummary(String.format("%s",this.getPersistedString((String)defaultValue)));
		} else {
			persistString((String)defaultValue);
			setSummary(String.format("%s",this.getPersistedString((String)defaultValue)));
		}
	}
}
