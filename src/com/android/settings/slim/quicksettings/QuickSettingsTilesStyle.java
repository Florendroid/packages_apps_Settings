/*
 * Copyright (C) 2013 Slimroms
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

package com.android.settings.slim.quicksettings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.ContentResolver;
import android.app.FragmentTransaction;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;
import com.android.settings.slim.RandomColors;
import com.android.settings.util.Helpers;
import com.android.settings.util.ShortcutPickerHelper;
import net.margaritov.preference.colorpicker.ColorPickerPreference;
import net.margaritov.preference.colorpicker.ColorPickerView; 

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class QuickSettingsTilesStyle extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String PREF_TILES_PER_ROW = "tiles_per_row";
    private static final String PREF_TILES_PER_ROW_DUPLICATE_LANDSCAPE = "tiles_per_row_duplicate_landscape";
    private static final String PREF_QUICK_TILES_BG_COLOR = "quick_tiles_bg_color";
    private static final String PREF_QUICK_TILES_BG_PRESSED_COLOR = "quick_tiles_bg_pressed_color";
    private static final String PREF_QUICK_TILES_TEXT_COLOR = "quick_tiles_text_color";
  private static final String PREF_RANDOM_COLORS = "random_colors"; 

    private static final int DEFAULT_QUICK_TILES_BG_COLOR = 0xff161616;
    private static final int DEFAULT_QUICK_TILES_BG_PRESSED_COLOR = 0xff212121;
    private static final int DEFAULT_QUICK_TILES_TEXT_COLOR = 0xffcccccc;

    private static final int MENU_RESET = Menu.FIRST;

    private ListPreference mTilesPerRow;
    private CheckBoxPreference mDuplicateColumnsLandscape;
    private ColorPickerPreference mQuickTilesBgColor;
    private ColorPickerPreference mQuickTilesBgPressedColor;
    private ColorPickerPreference mQuickTilesTextColor;
 Preference mRandomColors;

    private boolean mCheckPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    private PreferenceScreen refreshSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.quicksettings_tiles_style);

        prefs = getPreferenceScreen();

 mRandomColors = (Preference) findPreference(PREF_RANDOM_COLORS); 

       

        mQuickTilesBgPressedColor = (ColorPickerPreference) findPreference(PREF_QUICK_TILES_BG_PRESSED_COLOR);
        mQuickTilesBgPressedColor.setNewPreviewColor(DEFAULT_QUICK_TILES_BG_PRESSED_COLOR);
        mQuickTilesBgPressedColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_BG_PRESSED_COLOR, -2);
        if (intColor == -2) {
            mQuickTilesBgPressedColor.setSummary(getResources().getString(R.string.color_default));
        } else {
            mQuickTilesBgPressedColor.setNewPreviewColor(intColor);
        }

        mQuickTilesTextColor = (ColorPickerPreference) findPreference(PREF_QUICK_TILES_TEXT_COLOR);
        mQuickTilesTextColor.setNewPreviewColor(DEFAULT_QUICK_TILES_TEXT_COLOR);
        mQuickTilesTextColor.setOnPreferenceChangeListener(this);
        int intColor2 = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_TEXT_COLOR, -2);
        if (intColor2 == -2) {
            mQuickTilesTextColor.setSummary(getResources().getString(R.string.color_default));
        } else {
            mQuickTilesTextColor.setNewPreviewColor(intColor2);
        }

        mTilesPerRow = (ListPreference) prefs.findPreference(PREF_TILES_PER_ROW);
        int tilesPerRow = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW, 3);
        mTilesPerRow.setValue(String.valueOf(tilesPerRow));
        mTilesPerRow.setSummary(mTilesPerRow.getEntry());
        mTilesPerRow.setOnPreferenceChangeListener(this);

        mDuplicateColumnsLandscape = (CheckBoxPreference) findPreference(PREF_TILES_PER_ROW_DUPLICATE_LANDSCAPE);
        mDuplicateColumnsLandscape.setChecked(Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW_DUPLICATE_LANDSCAPE, 1) == 1);
        mDuplicateColumnsLandscape.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefs;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.qs_reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.qs_reset);
        alertDialog.setMessage(R.string.qs_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.QUICK_TILES_BG_COLOR, -2);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.QUICK_TILES_BG_PRESSED_COLOR, -2);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.QUICK_TILES_TEXT_COLOR, -2);
                refreshSettings();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mTilesPerRow) {
            int index = mTilesPerRow.findIndexOfValue((String) newValue);
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_PER_ROW,
                    value);
            mTilesPerRow.setSummary(mTilesPerRow.getEntries()[index]);
            return true;
        } else if (preference == mDuplicateColumnsLandscape) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_PER_ROW_DUPLICATE_LANDSCAPE,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mQuickTilesBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_TILES_BG_COLOR,
                    intHex);
            return true;
        } else if (preference == mQuickTilesBgPressedColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_TILES_BG_PRESSED_COLOR,
                    intHex);
            return true;
        } else if (preference == mQuickTilesTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_TILES_TEXT_COLOR,
                    intHex);
            return true;
        

  } else if (preference == mRandomColors) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            RandomColors fragment = new RandomColors();
            ft.addToBackStack("pick_random_colors");
            ft.replace(this.getId(), fragment);
            ft.commit();
            return true;
         } 
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

 @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

 if (preference == mRandomColors) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            RandomColors fragment = new RandomColors();
            ft.addToBackStack("pick_random_colors");
            ft.replace(this.getId(), fragment);
            ft.commit();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}
