
package com.android.settings.aokp.romcontrol;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.util.Helpers;
import com.android.settings.util.ShortcutPickerHelper;
import com.android.settings.aokp.romcontrol.weather.WeatherPrefs;
import com.android.settings.aokp.romcontrol.weather.WeatherRefreshService;
import com.android.settings.aokp.romcontrol.weather.WeatherService;

public class Weather extends SettingsPreferenceFragment implements
        ShortcutPickerHelper.OnPickListener, Preference.OnPreferenceChangeListener {

    public static final String TAG = "Weather";

    CheckBoxPreference mEnableWeather;
    CheckBoxPreference mUseCustomLoc;
    CheckBoxPreference mUseCelcius;
    CheckBoxPreference mTimeStamp;
    ListPreference mStatusBarLocation;
    ListPreference mWeatherSyncInterval;
    EditTextPreference mCustomWeatherLoc;
    ListPreference mWeatherShortClick;
    ListPreference mWeatherLongClick;

    private ShortcutPickerHelper mPicker;
    private Preference mPreference;
    private String mString;
    SharedPreferences prefs;

    private static final int LOC_WARNING = 101;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_weather);
	PreferenceScreen prefSet = getPreferenceScreen();
        prefs = getActivity().getSharedPreferences("weather", Context.MODE_WORLD_WRITEABLE);

        mPicker = new ShortcutPickerHelper(this, this);

        mWeatherSyncInterval = (ListPreference) prefSet.findPreference("refresh_interval");
        mWeatherSyncInterval.setOnPreferenceChangeListener(this);
        mWeatherSyncInterval.setSummary(Integer.toString(WeatherPrefs.getRefreshInterval(mContext))
                + getResources().getString(R.string.weather_refresh_interval_minutes));

        mStatusBarLocation = (ListPreference) prefSet.findPreference("statusbar_location");
        mStatusBarLocation.setOnPreferenceChangeListener(this);
        mStatusBarLocation.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUSBAR_WEATHER_STYLE, 2) + "");

        mCustomWeatherLoc = (EditTextPreference) prefSet.findPreference("custom_location");
        mCustomWeatherLoc.setOnPreferenceChangeListener(this);
        mCustomWeatherLoc
                .setSummary(WeatherPrefs.getCustomLocation(mContext));

        mEnableWeather = (CheckBoxPreference) prefSet.findPreference("enable_weather");
        mEnableWeather.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.USE_WEATHER, 0) == 1);

        mUseCustomLoc = (CheckBoxPreference) prefSet.findPreference(WeatherPrefs.KEY_USE_CUSTOM_LOCATION);
        mUseCustomLoc.setChecked(WeatherPrefs.getUseCustomLocation(mContext));

        mUseCelcius = (CheckBoxPreference) prefSet.findPreference(WeatherPrefs.KEY_USE_CELCIUS);
        mUseCelcius.setChecked(WeatherPrefs.getUseCelcius(mContext));

	mTimeStamp = (CheckBoxPreference) prefSet.findPreference("show_time");
        mTimeStamp.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.SHOW_TIME, 1) == 1);

	mWeatherShortClick = (ListPreference) prefSet.findPreference("weather_shortclick");
        mWeatherShortClick.setOnPreferenceChangeListener(this);
        mWeatherShortClick.setSummary(getProperSummary(mWeatherShortClick));
	mWeatherShortClick.setValue(Settings.System.getString(getContentResolver(),
                Settings.System.WEATHER_PANEL_SHORTCLICK));
	
        mWeatherLongClick = (ListPreference) prefSet.findPreference("weather_longclick");
        mWeatherLongClick.setOnPreferenceChangeListener(this);
        mWeatherLongClick.setSummary(getProperSummary(mWeatherLongClick));
	mWeatherLongClick.setValue(Settings.System.getString(getContentResolver(),
                Settings.System.WEATHER_PANEL_LONGCLICK));
	
	
        setHasOptionsMenu(true);

        if (!Settings.Secure.isLocationProviderEnabled(
                getContentResolver(), LocationManager.NETWORK_PROVIDER)
                && !mUseCustomLoc.isChecked()) {
            showDialog(LOC_WARNING);
        }

    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        LayoutInflater factory = LayoutInflater.from(mContext);

        switch (dialogId) {
            case LOC_WARNING:
                return new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.weather_loc_warning_title))
                        .setMessage(getResources().getString(R.string.weather_loc_warning_msg))
                        .setCancelable(false)
                        .setPositiveButton(
                                getResources().getString(R.string.weather_loc_warning_positive),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Settings.Secure.setLocationProviderEnabled(
                                                getContentResolver(),
                                                LocationManager.NETWORK_PROVIDER, true);
                                    }
                                })
                        .setNegativeButton(
                                getResources().getString(R.string.weather_loc_warning_negative),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                }).create();
        }
        return null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.weather, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.get_weather:
                Intent i = new Intent(getActivity().getApplicationContext(),
                        WeatherRefreshService.class);
                i.setAction(WeatherService.INTENT_WEATHER_REQUEST);
                i.putExtra(WeatherService.INTENT_EXTRA_ISMANUAL, true);
                getActivity().getApplicationContext().startService(i);
                Helpers.msgShort(getActivity().getApplicationContext(),
                        getString(R.string.weather_refreshing));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnableWeather) {
            // _stop_ alarm or start service
            boolean check = ((CheckBoxPreference) preference).isChecked();
            Intent i = new Intent(getActivity().getApplicationContext(),
                    WeatherRefreshService.class);
            i.setAction(WeatherService.INTENT_WEATHER_REQUEST);
            i.putExtra(WeatherService.INTENT_EXTRA_ISMANUAL, true);
            PendingIntent weatherRefreshIntent = PendingIntent.getService(getActivity(), 0, i, 0);
            if (!check) {
                AlarmManager alarms = (AlarmManager) getActivity().getSystemService(
                        Context.ALARM_SERVICE);
                alarms.cancel(weatherRefreshIntent);
            } else {
                getActivity().startService(i);
            }
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.USE_WEATHER,
                    check ? 1 : 0);
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mUseCustomLoc) {
            return WeatherPrefs.setUseCustomLocation(mContext,
                    ((CheckBoxPreference) preference).isChecked());
        } else if (preference == mUseCelcius) {
            return WeatherPrefs.setUseCelcius(mContext,
                    ((CheckBoxPreference) preference).isChecked());
        } else if (preference == mTimeStamp) {
	    boolean check = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SHOW_TIME,
                    check ? 1 : 0);
            Helpers.restartSystemUI();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;

        if (preference == mWeatherSyncInterval) {
            int newVal = Integer.parseInt((String) newValue);
            preference.setSummary(newValue
                    + getResources().getString(R.string.weather_refresh_interval_minutes));

            return WeatherPrefs.setRefreshInterval(mContext, newVal);

        } else if (preference == mCustomWeatherLoc) {

            String newVal = (String) newValue;

            Intent i = new Intent(getActivity().getApplicationContext(),
                    WeatherRefreshService.class);
            getActivity().getApplicationContext().startService(i);
            preference.setSummary(newVal);
            return WeatherPrefs.setCustomLocation(mContext, newVal);

         } else if (preference == mStatusBarLocation) {

             String newVal = (String) newValue;
	     if (Integer.parseInt(newVal) == 0 || Integer.parseInt(newVal) == 1) {
		Intent i = new Intent(getActivity().getApplicationContext(),
                        WeatherRefreshService.class);
                i.setAction(WeatherService.INTENT_WEATHER_REQUEST);
                i.putExtra(WeatherService.INTENT_EXTRA_ISMANUAL, true);
                getActivity().getApplicationContext().startService(i);
		}
	     Settings.System.putInt(getActivity().getContentResolver(),
                     Settings.System.STATUSBAR_WEATHER_STYLE,
                     Integer.parseInt(newVal));
	     Helpers.restartSystemUI();
             return Settings.System.putInt(getActivity().getContentResolver(),
                     Settings.System.STATUSBAR_WEATHER_STYLE,
                     Integer.parseInt(newVal));
	     

        }  else if (preference == mWeatherShortClick) {

            mPreference = preference;
            mString = Settings.System.WEATHER_PANEL_SHORTCLICK;
            if (newValue.equals("**app**")) {
             mPicker.pickShortcut();
            } else {
            Settings.System.putString(getContentResolver(), Settings.System.WEATHER_PANEL_SHORTCLICK, (String) newValue);
            mWeatherShortClick.setSummary(getProperSummary(mWeatherShortClick));
            }
	    return true;

        } else if (preference == mWeatherLongClick) {

            mPreference = preference;
            mString = Settings.System.WEATHER_PANEL_LONGCLICK;
            if (newValue.equals("**app**")) {
             mPicker.pickShortcut();
            } else {
            Settings.System.putString(getContentResolver(), Settings.System.WEATHER_PANEL_LONGCLICK, (String) newValue);
            mWeatherLongClick.setSummary(getProperSummary(mWeatherLongClick));
            }
	    return true;
         }
         return false;
    } 

    @Override
    public void shortcutPicked(String uri, String friendlyName, Bitmap bmp, boolean isApplication) {
          mPreference.setSummary(friendlyName);
          Settings.System.putString(getContentResolver(), mString, (String) uri);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private String getProperSummary(Preference preference) {
        if (preference == mWeatherLongClick) {
            mString = Settings.System.WEATHER_PANEL_LONGCLICK;
        } else if (preference == mWeatherShortClick) {
            mString = Settings.System.WEATHER_PANEL_SHORTCLICK;
        }

        String uri = Settings.System.getString(getActivity().getContentResolver(),mString);
        String empty = "";

        if (uri == null)
            return empty;

        if (uri.startsWith("**")) {
            if (uri.equals("**update**"))
                return getResources().getString(R.string.update);
            else if (uri.equals("**nothing**"))
                return getResources().getString(R.string.nothing);
        } else {
            return mPicker.getFriendlyNameForUri(uri);
        }
        return null;
    }

}
