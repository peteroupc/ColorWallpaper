package com.upokecenter.android.ui;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.upokecenter.android.util.AppManager;

public abstract class BaseSettingsActivity extends PreferenceActivity
implements OnSharedPreferenceChangeListener {

	protected abstract int getPreferenceResource();

	private static void updatePreference(SharedPreferences prefs, Preference pref){
		if(pref instanceof ListPreference){
			ListPreference lpref=(ListPreference)pref;
			String currentValue=prefs.getString(pref.getKey(), "");
			pref.setSummary(lpref.getEntries()[lpref.findIndexOfValue(currentValue)]);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle inst) {
		if(inst==null){
			AppManager.initialize(this);
		}
		super.onCreate(inst);
		addPreferencesFromResource(getPreferenceResource());
		SharedPreferences prefs=getPreferenceScreen().getSharedPreferences();
		for(String key : prefs.getAll().keySet()){
			updatePreference(prefs,findPreference(key));
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		updatePreference(prefs,findPreference(key));
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

}