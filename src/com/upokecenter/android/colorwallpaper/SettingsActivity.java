package com.upokecenter.android.colorwallpaper;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.upokecenter.android.ui.BaseSettingsActivity;
import com.upokecenter.android.ui.ShareActivity;
import com.upokecenter.util.Reflection;

public class SettingsActivity extends BaseSettingsActivity {
  @Override
  protected int getPreferenceResource() {
    return R.xml.preferences;
  }

  private Intent shareIntent(boolean actionBar){
    Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.sharesubject));
    sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.sharetext));
    if(!actionBar){
      sendIntent.putExtra(Intent.EXTRA_TITLE, getResources().getString(R.string.sharevia));
      sendIntent.setClass(this,ShareActivity.class);
    }
    sendIntent.setType("text/plain");
    return sendIntent;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem menu){
    if(menu.getItemId()==R.id.share){
      startActivity(shareIntent(false));
    }
    return super.onOptionsItemSelected(menu);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.settingsmenu, menu);
    MenuItem item = menu.findItem(R.id.share);
    Object provider = Reflection.invokeByName(item,"getActionProvider",null);
    if(provider!=null){
      Reflection.invokeByName(provider,"setShareIntent",null,shareIntent(true));
    }
    super.onCreateOptionsMenu(menu);
    return true;
  }
}
