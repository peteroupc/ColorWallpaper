package com.upokecenter.android.ui;

import java.net.URISyntaxException;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.text.method.SingleLineTransformationMethod;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.upokecenter.android.util.AppManager;

public class UriPreference extends AlertDialogPreference {

  IDialogUpdater updater = null;

  // We use "java.net.URI" to distinguish the URI
  // class from Android's own URI class, "android.net.Uri".
  private static java.net.URI convertToUri(String s){
    int colon=s.indexOf(":");
    int slash=s.indexOf("/");
    java.net.URI uri=null;
    try {
      if(slash!=0 && (colon<0 || slash<0 || colon>slash)){
        uri=new java.net.URI("http://"+s);
      } else {
        uri=new java.net.URI(s);
      }
      if(uri.getScheme()==null)return null;
      uri=new java.net.URI(
          uri.getScheme().toLowerCase(Locale.US),
          uri.getSchemeSpecificPart(),
          uri.getFragment());
      String scheme=uri.getScheme().toLowerCase(Locale.US);
      if("data".equals(scheme) || "content".equals(scheme))
        return null;
      return uri;
    } catch(URISyntaxException e){
      return null;
    }
  }

  public UriPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public static IDialogUpdater staticGetDialogUpdater() {
    return new IDialogUpdater(){
      @Override
      public void setValue(Dialog dialog, Object value) {
        if(dialog==null)return;
        EditText editText=(EditText)dialog.findViewById(AppManager.getIdResource("edittext"));
        if(editText==null)return;
        String oldText=editText.getText().toString();
        if(!oldText.equals(value)){
          editText.setText((String)value);
          editText.setSelection(editText.getText().length());
        }
      }

      @Override
      public Object getValue(Dialog dialog) {
        if(dialog==null)return "";
        EditText editText=(EditText)dialog.findViewById(AppManager.getIdResource("edittext"));
        if(editText==null)return "";
        return editText.getText().toString();
      }

      @Override
      public void prepareDialog(final Dialog dialog) {
        if(dialog==null)return;
        EditText editText=(EditText)dialog.findViewById(AppManager.getIdResource("edittext"));
        if(editText==null)return;
        //DebugUtility.log("onaddedit %s [dialog=%s][%s][length=%s]",
        //  editText,dialog,getPersistedString(""),editText.getText().length());
        //DebugUtility.log("[length=%s]",editText.getText().length());
        editText.setTransformationMethod(SingleLineTransformationMethod.getInstance());
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener(){
          @Override
          public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
            ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
          }
        });
        // This must be last, in order to move the text to the end
        editText.setSelection(editText.getText().length());
      }

      @Override
      public Class<?> getType() {
        return String.class;
      }

      @Override
      public boolean isValid(Object[] value) {
        java.net.URI uri=convertToUri(value[0].toString());
        if(uri==null)
          return false;
        else {
          value[0]=uri.toString();
          return true;
        }
      }
    };
  }

  @Override
  protected IDialogUpdater getDialogUpdater() {
    if(updater==null) {
      updater=staticGetDialogUpdater();
    }
    return updater;
  }
}
