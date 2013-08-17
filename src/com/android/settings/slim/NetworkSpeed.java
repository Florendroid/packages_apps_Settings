/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.slim;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.util.Helpers;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import net.margaritov.preference.colorpicker.ColorPickerPreference;
import net.margaritov.preference.colorpicker.ColorPickerView; 



public class NetworkSpeed extends SettingsPreferenceFragment implements OnPreferenceChangeListener {


    private static final String STATUS_BAR_TRAFFIC = "status_bar_traffic"; 
    private static final String STATUS_BAR_TRAFFIC_COLOR = "status_bar_traffic_color";
    private static final String STATUS_BAR_NETWORK_STATS = "status_bar_show_network_stats";
    private static final String STATUS_BAR_NETWORK_STATS_UPDATE = "status_bar_network_status_update";

    private CheckBoxPreference mStatusBarTraffic;
    private ColorPickerPreference mTrafficColorPicker;
    private ListPreference mStatusBarNetStatsUpdate;
    private CheckBoxPreference mStatusBarNetworkStats;


  @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.network_speed);

        PreferenceScreen prefSet = getPreferenceScreen();


	mStatusBarTraffic = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_TRAFFIC); 
	mStatusBarTraffic.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
               Settings.System.STATUS_BAR_TRAFFIC, 1) == 1)); 

 	mTrafficColorPicker = (ColorPickerPreference) findPreference("status_bar_traffic_color");
        mTrafficColorPicker.setOnPreferenceChangeListener(this);
        defaultColor = getResources().getColor(
            com.android.internal.R.color.holo_blue_light);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.STATUS_BAR_TRAFFIC_COLOR, defaultColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mTrafficColorPicker.setSummary(hexColor);
        mTrafficColorPicker.setNewPreviewColor(intColor); 


	mStatusBarNetworkStats = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NETWORK_STATS);
	mStatusBarNetworkStats.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_STATS, 0) == 1));
	mStatusBarNetStatsUpdate = (ListPreference) prefSet.findPreference(STATUS_BAR_NETWORK_STATS_UPDATE);
	long statsUpdate = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_STATS_UPDATE_INTERVAL, 500);
        mStatusBarNetStatsUpdate.setValue(String.valueOf(statsUpdate));
        mStatusBarNetStatsUpdate.setSummary(mStatusBarNetStatsUpdate.getEntry());
        mStatusBarNetStatsUpdate.setOnPreferenceChangeListener(this);

  }


   public boolean onPreferenceChange(Preference preference, Object newValue) {

	if (preference == mTrafficColorPicker) {
          String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
              .valueOf(newValue)));
          preference.setSummary(hex);
          int intHex = ColorPickerPreference.convertToColorInt(hex);
          Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
              Settings.System.STATUS_BAR_TRAFFIC_COLOR, intHex);
          return true;
	}  else if (preference == mStatusBarNetworkStats) {
            value = mStatusBarNetworkStats.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                   Settings.System.STATUS_BAR_NETWORK_STATS, value ? 1 : 0);
            return true;
	}  
	return false;
    }


  public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        boolean value;
	boolean value1;
	
	if (preference == mStatusBarTraffic) {
            value = mStatusBarTraffic.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_TRAFFIC, value ? 1 : 0);
	    if (value) {
		Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS, 0);
		}
            return true;
         } else if (preference == mStatusBarNetworkStats) {
            value1 = mStatusBarNetworkStats.isChecked();
	    if (value1) {
		Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_TRAFFIC, 0);
		}
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS, value1 ? 1 : 0);
            return true;
	 }
        	return super.onPreferenceTreeClick(preferenceScreen, preference);
 
    }
	
