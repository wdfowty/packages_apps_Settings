/* //device/apps/Settings/src/com/android/settings/Keyguard.java
**
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the LicOBense.
*/

package com.android.settings;

import com.android.settings.ShellInterface;
import com.android.settings.ColorPickerDialog;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.SystemProperties;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.IWindowManager;
import android.net.Uri;
import android.text.format.Formatter;
import android.widget.Toast;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class LeoParts extends PreferenceActivity
    implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "LeoParts";
    private static final String REMOUNT_RO = "mount -o ro,remount -t yaffs2 /dev/block/mtdblock3 /system";
    private static final String REMOUNT_RW = "mount -o rw,remount -t yaffs2 /dev/block/mtdblock3 /system";
    private static String REPO;

    private final Configuration mCurConfig = new Configuration();

    // ROM infos
    private static final String ROM_DEVICE_PREF = "rom_device";
    private static final String ROM_NAME_VERSION_PREF = "rom_name_version";
    private static final String ROM_SYSTEM_BUILD_PREF = "rom_system_build";
    private static final String ROM_BOOTLOADER_RADIO_PREF = "rom_bootloader_radio";
    private static final String ROM_KERNEL_PREF = "rom_kernel";
    private static final String ROM_UPDATE_PREF = "rom_update";
    private Preference mUpdatePref;

    // Quick commands
    private static final String SHUTDOWN_PREF = "shutdown_shutdown";
    private Preference mShutdownPref;
    private static final String REBOOT_PREF = "reboot_reboot";
    private Preference mRebootPref;
    private static final String RECOVERY_PREF = "reboot_recovery";
    private Preference mRecoveryPref;
    private static final String BOOTLOADER_PREF = "reboot_bootloader";
    private Preference mBootloaderPref;
    private static final String REMOUNT_RW_PREF = "remount_rw";
    private Preference mRemountRWPref;
    private static final String REMOUNT_RO_PREF = "remount_ro";
    private Preference mRemountROPref;

    // Tweaks

    // User Interface
    private static final String BATTERY_PERCENT_PREF = "battery_percent";
    private CheckBoxPreference mBatteryPercentPref;
    private static final String HIDE_CLOCK_PREF = "hide_clock";
    private CheckBoxPreference mHideClockPref;

    private static final String UI_BATTERY_PERCENT_COLOR = "battery_status_color_title";
    private Preference mBatteryPercentColorPreference;
    private static final String UI_CLOCK_COLOR = "clock_color";
    private Preference mClockColorPref;
    private static final String UI_DATE_COLOR = "date_color";
    private Preference mDateColorPref;
    private static final String UI_PLMN_LABEL_COLOR = "plmn_label_color";
    private Preference mPlmnLabelColorPref;
    private static final String UI_SPN_LABEL_COLOR = "spn_label_color";
    private Preference mSpnLabelColorPref;
    private static final String UI_NO_NOTIF_COLOR = "no_notifications_color";
    private Preference mNoNotifColorPref;
    private static final String UI_LATEST_NOTIF_COLOR = "latest_notifications_color";
    private Preference mLatestNotifColorPref;
    private static final String UI_ONGOING_NOTIF_COLOR = "ongoing_notifications_color";
    private Preference mOngoingNotifColorPref;
    private static final String UI_CLEAR_LABEL_COLOR = "clear_button_label_color";
    private Preference mClearLabelColorPref;
    private static final String UI_NOTIF_TICKER_COLOR = "new_notifications_ticker_color";
    private Preference mNotifTickerColor;
    private static final String UI_NOTIF_COUNT_COLOR = "notifications_count_color";
    private Preference mNotifCountColor;
    private static final String UI_NOTIF_ITEM_TITLE_COLOR = "notifications_title_color";
    private Preference mNotifItemTitlePref;
    private static final String UI_NOTIF_ITEM_TEXT_COLOR = "notifications_text_color";
    private Preference mNotifItemTextPref;
    private static final String UI_NOTIF_ITEM_TIME_COLOR = "notifications_time_color";
    private Preference mNotifItemTimePref;

    private static final String PULSE_SCREEN_ON_PREF = "pulse_screen_on";
    private CheckBoxPreference mPulseScreenOnPref;
    private static final String TRACKBALL_WAKE_PREF = "trackball_wake";
    private CheckBoxPreference mTrackballWakePref;
    private static final String TRACKBALL_UNLOCK_PREF = "trackball_unlock";
    private CheckBoxPreference mTrackballUnlockPref;
    private static final String ROTATION_90_PREF = "rotation_90";
    private CheckBoxPreference mRotation90Pref;
    private static final String ROTATION_180_PREF = "rotation_180";
    private CheckBoxPreference mRotation180Pref;
    private static final String ROTATION_270_PREF = "rotation_270";
    private CheckBoxPreference mRotation270Pref;

    // Apps & Addons

    // About
    private static final String ABOUT_AUTHOR = "about_author";
    private Preference mAboutAuthor;
    private static final String ABOUT_DONATE = "about_donate";
    private Preference mAboutDonate;
    private static final String ABOUT_SOURCES = "about_sources";
    private Preference mAboutSources;

    // Storage
    private static final String SYSTEM_PART_SIZE = "system_storage_levels";
    private Preference mSystemSize;
    private static final String DATA_PART_SIZE = "data_storage_levels";
    private Preference mDataSize;
    private static final String CACHE_PART_SIZE = "cache_storage_levels";
    private Preference mCacheSize;
    private static final String SDCARDFAT_PART_SIZE = "sdcardfat_storage_levels";
    private Preference mSDCardFATSize;
    private static final String SDCARDEXT_PART_SIZE = "sdcardext_storage_levels";
    private Preference mSDCardEXTSize;
    private static final String REFRESH_PREF = "refresh";
    private Preference mRefresh;
    private static final String OLD_APP2SD_PREF = "app2sd_opt";
    private CheckBoxPreference mOldApp2sdPref;
    private static final String DALVIK2SD_PREF = "dalvik2sd_opt";
    private CheckBoxPreference mDalvik2sdPref;
    private static final String DATA2SD_PREF = "data2sd_opt";
    private CheckBoxPreference mData2sdPref;
    private static final String MEDIA2SD_PREF = "media2sd_opt";
    private CheckBoxPreference mMedia2sdPref;
    private boolean extfsIsMounted = false;

    public ProgressDialog patience = null;
    final Handler mHandler = new Handler();
    private int PATCH = 0;

    private IWindowManager mWindowManager;

    @Override
	public void onCreate(Bundle icicle) {
	super.onCreate(icicle);
	addPreferencesFromResource(R.xml.leo_parts);

	PreferenceScreen prefSet = getPreferenceScreen();
	REPO = getResources().getString(R.string.repo_url);

	/**
	 *  ROM infos
	 */

	setStringSummary(ROM_DEVICE_PREF, Build.MODEL + " by " + Build.MANUFACTURER);
	setStringSummary(ROM_NAME_VERSION_PREF, getRomName() + "  /  " + (isRomBeta() ? getRomVersion() + "-BETA" + getRomBeta() : getRomVersion() )+ "  /  patch" + getRomPatch());
	setStringSummary(ROM_SYSTEM_BUILD_PREF, "Android " + Build.VERSION.RELEASE + "  /  " + Build.ID + " " +
			 (fileExists("/system/framework/framework.odex") ? "" : "de") + "odex  /  " + getFormattedFingerprint());
	setStringSummary(ROM_BOOTLOADER_RADIO_PREF, Build.BOOTLOADER + "  /  " + getSystemValue("gsm.version.baseband"));
	String kernel = getFormattedKernelVersion();
	findPreference(ROM_KERNEL_PREF).setSummary((kernel.equals("2.6.32.9\nandroid-build@apa26") ? "stock " : "") + kernel);
	mUpdatePref = (Preference) prefSet.findPreference(ROM_UPDATE_PREF);
	mUpdatePref.setEnabled(false);

	/**
	 *  Quick commands
	 */

	mShutdownPref = (Preference) prefSet.findPreference(SHUTDOWN_PREF);
	findPreference(SHUTDOWN_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "toolbox reboot -p" };
		    sendshell(commands, false, "Shuting down...");
		    return true;
		}
	    });
	mRebootPref = (Preference) prefSet.findPreference(REBOOT_PREF);
	findPreference(REBOOT_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "reboot" };
		    sendshell(commands, false, "Rebooting...");
		    return true;
		}
	    });
	mRecoveryPref = (Preference) prefSet.findPreference(BOOTLOADER_PREF);
	findPreference(RECOVERY_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "reboot recovery" };
		    sendshell(commands, false, "Rebooting...");
		    return true;
		}
	    });
	mBootloaderPref = (Preference) prefSet.findPreference(RECOVERY_PREF);
	findPreference(BOOTLOADER_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "reboot bootloader" };
		    sendshell(commands, false, "Rebooting...");
		    return true;
		}
	    });
	mRemountRWPref = (Preference) prefSet.findPreference(REMOUNT_RW_PREF);
	findPreference(REMOUNT_RO_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { REMOUNT_RO };
		    sendshell(commands, false, "Remounting...");
		    return true;
		}
	    });
	mRemountROPref = (Preference) prefSet.findPreference(REMOUNT_RO_PREF);
	findPreference(REMOUNT_RW_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { REMOUNT_RW };
		    sendshell(commands, false, "Remounting...");
		    return true;
		}
	    });

	/**
	 *  Tweaks
	 */

	/**
	 *  User Interface
	 */
	mBatteryPercentPref = (CheckBoxPreference) prefSet.findPreference(BATTERY_PERCENT_PREF);
	mBatteryPercentPref.setOnPreferenceChangeListener(this);
	mBatteryPercentPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.BATTERY_PERCENTAGE_STATUS_ICON, 0) == 1);
	mPulseScreenOnPref = (CheckBoxPreference) prefSet.findPreference(PULSE_SCREEN_ON_PREF);
	mPulseScreenOnPref.setOnPreferenceChangeListener(this);
	mPulseScreenOnPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.TRACKBALL_SCREEN_ON, 0) == 1);
	mHideClockPref = (CheckBoxPreference) prefSet.findPreference(HIDE_CLOCK_PREF);
	mHideClockPref.setOnPreferenceChangeListener(this);
	mHideClockPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.SHOW_STATUS_CLOCK, 1) == 0);

	mBatteryPercentColorPreference = prefSet.findPreference(UI_BATTERY_PERCENT_COLOR);
	mClockColorPref = prefSet.findPreference(UI_CLOCK_COLOR);
	mClockColorPref.setEnabled(mHideClockPref.isChecked() ? false : true);
	mDateColorPref = prefSet.findPreference(UI_DATE_COLOR);
	mPlmnLabelColorPref = prefSet.findPreference(UI_PLMN_LABEL_COLOR);
	mSpnLabelColorPref = prefSet.findPreference(UI_SPN_LABEL_COLOR);
	mNotifTickerColor = prefSet.findPreference(UI_NOTIF_TICKER_COLOR);
	mNotifCountColor = prefSet.findPreference(UI_NOTIF_COUNT_COLOR);
	mNoNotifColorPref = prefSet.findPreference(UI_NO_NOTIF_COLOR);
	mClearLabelColorPref = prefSet.findPreference(UI_CLEAR_LABEL_COLOR);
	mOngoingNotifColorPref = prefSet.findPreference(UI_ONGOING_NOTIF_COLOR);
	mLatestNotifColorPref = prefSet.findPreference(UI_LATEST_NOTIF_COLOR);
	mNotifItemTitlePref = prefSet.findPreference(UI_NOTIF_ITEM_TITLE_COLOR);
	mNotifItemTextPref = prefSet.findPreference(UI_NOTIF_ITEM_TEXT_COLOR);
	mNotifItemTimePref = prefSet.findPreference(UI_NOTIF_ITEM_TIME_COLOR);

	mTrackballWakePref = (CheckBoxPreference) prefSet.findPreference(TRACKBALL_WAKE_PREF);
	mTrackballWakePref.setOnPreferenceChangeListener(this);
	mTrackballWakePref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.TRACKBALL_WAKE_SCREEN, 0) == 1);
	mTrackballUnlockPref = (CheckBoxPreference) prefSet.findPreference(TRACKBALL_UNLOCK_PREF);
	mTrackballUnlockPref.setOnPreferenceChangeListener(this);
	mTrackballUnlockPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.TRACKBALL_UNLOCK_SCREEN, 0) == 1);
	mRotation90Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_90_PREF);
	mRotation90Pref.setOnPreferenceChangeListener(this);
	mRotation180Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_180_PREF);
	mRotation180Pref.setOnPreferenceChangeListener(this);
	mRotation270Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_270_PREF);
	mRotation270Pref.setOnPreferenceChangeListener(this);
	int mode = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION_MODE, 5);
	mRotation90Pref.setChecked((mode & 1) != 0);
	mRotation180Pref.setChecked((mode & 2) != 0);
	mRotation270Pref.setChecked((mode & 4) != 0);

	/**
	 *  Apps & Addons
	 */

	/**
	 *  About
	 */

	mAboutAuthor = (Preference) prefSet.findPreference(ABOUT_AUTHOR);
	findPreference(ABOUT_AUTHOR).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String url = "http://forum.xda-developers.com/member.php?u=2398805";
		    Intent i = new Intent(Intent.ACTION_VIEW);
		    i.setData(Uri.parse(url));
		    startActivity(i);
		    return true;
		}
	    });
	mAboutDonate = (Preference) prefSet.findPreference(ABOUT_DONATE);
	findPreference(ABOUT_DONATE).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String url = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=FP4JTHPKJPKS6&lc=FR&item_name=leonnib4&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted";
		    Intent i = new Intent(Intent.ACTION_VIEW);
		    i.setData(Uri.parse(url));
		    startActivity(i);
		    return true;
		}
	    });
	mAboutSources = (Preference) prefSet.findPreference(ABOUT_SOURCES);
	findPreference(ABOUT_SOURCES).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String url = "http://github.com/leonnib4/development_apps_Settings";
		    Intent i = new Intent(Intent.ACTION_VIEW);
		    i.setData(Uri.parse(url));
		    startActivity(i);
		    return true;
		}
	    });

	/**
	 *  Storage
	 */

	mOldApp2sdPref     = (CheckBoxPreference) prefSet.findPreference(OLD_APP2SD_PREF);
	mOldApp2sdPref.setOnPreferenceChangeListener(this);
	mOldApp2sdPref.setEnabled(extfsIsMounted);
	mDalvik2sdPref     = (CheckBoxPreference) prefSet.findPreference(DALVIK2SD_PREF);
	mDalvik2sdPref.setOnPreferenceChangeListener(this);
	mDalvik2sdPref.setEnabled(extfsIsMounted);
	mData2sdPref       = (CheckBoxPreference) prefSet.findPreference(DATA2SD_PREF);
	mData2sdPref.setOnPreferenceChangeListener(this);
	mData2sdPref.setEnabled(extfsIsMounted);
	mMedia2sdPref      = (CheckBoxPreference) prefSet.findPreference(MEDIA2SD_PREF);
	mMedia2sdPref.setOnPreferenceChangeListener(this);
	mMedia2sdPref.setEnabled(extfsIsMounted);

	extfsIsMounted     = fileExists("/dev/block/mmcblk0p2");
	mSystemSize        = (Preference) prefSet.findPreference(SYSTEM_PART_SIZE);
	mDataSize          = (Preference) prefSet.findPreference(DATA_PART_SIZE);
	mCacheSize         = (Preference) prefSet.findPreference(CACHE_PART_SIZE);
	mSDCardFATSize     = (Preference) prefSet.findPreference(SDCARDFAT_PART_SIZE);
	mSDCardEXTSize     = (Preference) prefSet.findPreference(SDCARDEXT_PART_SIZE);
	SetupFSPartSize();

	mRefresh = (Preference) prefSet.findPreference(REFRESH_PREF);
	findPreference(REFRESH_PREF)
	    .setOnPreferenceClickListener(new OnPreferenceClickListener() {
		    public boolean onPreferenceClick(Preference preference) {
			SetupFSPartSize();
			return true;
		    }
		});

	mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
	if (preference == mBatteryPercentPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.BATTERY_PERCENTAGE_STATUS_ICON, mBatteryPercentPref.isChecked() ? 0 : 1);
	    toast("You should reboot for the changes to take effect.");
	}
	else if (preference == mHideClockPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.SHOW_STATUS_CLOCK, mHideClockPref.isChecked() ? 1 : 0);
	    toast("You should reboot for the changes to take effect.");
	}
	else if (preference == mPulseScreenOnPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_SCREEN_ON, mPulseScreenOnPref.isChecked() ? 0 : 1);
	    toast("You should reboot for the changes to take effect.");
	}
	else if (preference == mTrackballWakePref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_WAKE_SCREEN, mTrackballWakePref.isChecked() ? 0 : 1);
	}
	else if (preference == mTrackballUnlockPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_UNLOCK_SCREEN, mTrackballUnlockPref.isChecked() ? 0 : 1);
	}
	else if (preference == mRotation90Pref ||
		 preference == mRotation180Pref ||
		 preference == mRotation270Pref) {
	    int mode = 0;
	    if (mRotation90Pref.isChecked()) mode += 1;
	    if (mRotation180Pref.isChecked()) mode += 2;
	    if (mRotation270Pref.isChecked()) mode += 4;
	    if (preference == mRotation90Pref) mode += 1;
	    if (preference == mRotation180Pref) mode += 2;
	    if (preference == mRotation270Pref) mode += 4;
	    Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION_MODE, mode);
	    Log.i(TAG, "RotationMode=" + mode);
	    toast("You should reboot for the changes to take effect.");
	}

	// always let the preference setting proceed.
	return true;
    }

    @Override
	public void onResume() {
	super.onResume();
    }

    /**
     *  Colors relative
     */

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
	if (preference == mBatteryPercentColorPreference) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mBatteryColorListener, readBatteryColor());
	    cp.show();
	}
	else if (preference == mClockColorPref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mClockFontColorListener, readClockFontColor());
	    cp.show();
	}
	if (preference == mDateColorPref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mDateFontColorListener, readDateFontColor());
	    cp.show();
	}
	else if (preference == mPlmnLabelColorPref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mPlmnLabelColorListener, readPlmnLabelColor());
	    cp.show();
	}
	else if (preference == mSpnLabelColorPref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mSpnLabelColorListener, readSpnLabelColor());
	    cp.show();
	}
	else if (preference == mNotifTickerColor) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mNotifTickerColorListener, readNotifTickerColor());
	    cp.show();
	}
	else if (preference == mNotifCountColor) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mNotifCountColorListener, readNotifCountColor());
	    cp.show();
	}
	else if (preference == mNoNotifColorPref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mNoNotifColorListener, readNoNotifColor());
	    cp.show();
	}
	else if (preference == mClearLabelColorPref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mClearLabelColorListener, readClearLabelColor());
	    cp.show();
	}
	else if (preference == mOngoingNotifColorPref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mOngoingNotifColorListener, readOngoingNotifColor());
	    cp.show();
	}
	else if (preference == mLatestNotifColorPref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mLatestNotifColorListener, readLatestNotifColor());
	    cp.show();
	}
	else if (preference == mNotifItemTitlePref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mNotifItemTitleColorListener, readNotifItemTitleColor());
	    cp.show();
	}
	else if (preference == mNotifItemTextPref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mNotifItemTextColorListener, readNotifItemTextColor());
	    cp.show();
	}
	else if (preference == mNotifItemTimePref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mNotifItemTimeColorListener, readNotifItemTimeColor());
	    cp.show();
	}
	return true;
    }

    ColorPickerDialog.OnColorChangedListener mBatteryColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.BATTERY_PERCENTAGE_STATUS_COLOR, color);
		toast("You should reboot for the changes to take effect.");
	    }
	};

    private int readBatteryColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.BATTERY_PERCENTAGE_STATUS_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -1;
	}
    }

    ColorPickerDialog.OnColorChangedListener mClockFontColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.CLOCK_COLOR, color);
		toast("You should reboot for the changes to take effect.");
	    }
	};

    private int readClockFontColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.CLOCK_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -16777216;
	}
    }

    private int readDateFontColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.DATE_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -16777216;
	}
    }

    ColorPickerDialog.OnColorChangedListener mDateFontColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.DATE_COLOR, color);
	    }
	};

    private int readPlmnLabelColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.PLMN_LABEL_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -16777216;
	}
    }

    ColorPickerDialog.OnColorChangedListener mPlmnLabelColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.PLMN_LABEL_COLOR, color);
	    }
	};

    private int readSpnLabelColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.SPN_LABEL_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -16777216;
	}
    }

    ColorPickerDialog.OnColorChangedListener mSpnLabelColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.SPN_LABEL_COLOR, color);
            }
	};

    private int readNotifTickerColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.NEW_NOTIF_TICKER_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -16777216;
	}
    }

    ColorPickerDialog.OnColorChangedListener mNotifTickerColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NEW_NOTIF_TICKER_COLOR, color);
		toast("You should reboot for the changes to take effect.");
	    }
	};

    private int readNotifCountColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.NOTIF_COUNT_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -1;
	}
    }

    ColorPickerDialog.OnColorChangedListener mNotifCountColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_COUNT_COLOR, color);
		toast("You should reboot for the changes to take effect.");
	    }
	};

    private int readNoNotifColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.NO_NOTIF_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -1;
	}
    }

    ColorPickerDialog.OnColorChangedListener mNoNotifColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NO_NOTIF_COLOR, color);
		toast("You should reboot for the changes to take effect.");
	    }
	};

    private int readClearLabelColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.CLEAR_BUTTON_LABEL_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -16777216;
	}
    }

    ColorPickerDialog.OnColorChangedListener mClearLabelColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.CLEAR_BUTTON_LABEL_COLOR, color);
		toast("You should reboot for the changes to take effect.");
	    }
	};

    private int readOngoingNotifColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.ONGOING_NOTIF_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -1;
	}
    }

    ColorPickerDialog.OnColorChangedListener mOngoingNotifColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.ONGOING_NOTIF_COLOR, color);
		toast("You should reboot for the changes to take effect.");
	    }
	};

    private int readLatestNotifColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.LATEST_NOTIF_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -1;
	}
    }

    ColorPickerDialog.OnColorChangedListener mLatestNotifColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.LATEST_NOTIF_COLOR, color);
		toast("You should reboot for the changes to take effect.");
	    }
	};

    private int readNotifItemTitleColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.NOTIF_ITEM_TITLE_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -16777216;
	}
    }

    ColorPickerDialog.OnColorChangedListener mNotifItemTitleColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TITLE_COLOR, color);
		toast("You should reboot for the changes to take effect.");
	    }
	};

    private int readNotifItemTextColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.NOTIF_ITEM_TEXT_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -16777216;
	}
    }

    ColorPickerDialog.OnColorChangedListener mNotifItemTextColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TEXT_COLOR, color);
		toast("You should reboot for the changes to take effect.");
	    }
	};

    private int readNotifItemTimeColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.NOTIF_ITEM_TIME_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -16777216;
	}
    }

    ColorPickerDialog.OnColorChangedListener mNotifItemTimeColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TIME_COLOR, color);
		toast("You should reboot for the changes to take effect.");
	    }
	};

    /**
     *  Shell interaction
     */

    final Runnable mCommandFinished = new Runnable() {
	    public void run() { patience.cancel(); }
	};

    public boolean sendshell(final String[] commands, final boolean reboot, final String message) {
	if (message != null)
	    patience = ProgressDialog.show(this, "", message, true);
	Thread t = new Thread() {
		public void run() {
		    ShellInterface shell = new ShellInterface(commands);
		    shell.start();
		    while (shell.isAlive())
			{
			    if (message != null)
				patience.setProgress(shell.getStatus());
			    try {
				Thread.sleep(500);
			    }
			    catch (InterruptedException e) {
				e.printStackTrace();
			    }
			}
		    if (message != null)
			mHandler.post(mCommandFinished);
		    if (shell.interrupted())
			popup("Error", "Download or install has finished unexpectedly!");
		    if (reboot == true)
			mHandler.post(mNeedReboot);
		}
	    };
	t.start();
	return true;
    }

    /**
     *  Methods for popups
     */

    public void toast(final CharSequence message) {
	Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
	toast.show();
    }

    public void popup(final String title, final String message) {
	Log.i(TAG, "popup");
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle(title)
	    .setMessage(message)
	    .setCancelable(false)
	    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
			dialog.cancel();
		    }
		});
	AlertDialog alert = builder.create();
	alert.show();
    }

    final Runnable mNeedReboot = new Runnable() {
	    public void run() { needreboot(); }
	};

    public void needreboot() {
	Log.i(TAG, "needreboot");
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setMessage("Reboot is requiered to apply. Would you like to reboot now?")
	    .setCancelable(false)
	    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
			String[] commands = { "reboot" };
			sendshell(commands, false, "Rebooting...");
		    }
		})
	    .setNegativeButton("No", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
			dialog.cancel();
		    }
		});
	AlertDialog alert = builder.create();
	alert.show();
    }

    /**
     *  Methods for storage
     */

    private String ObtainFSPartSize(String PartitionPath) {
	String retstr;
	File extraPath = new File(PartitionPath);
	StatFs extraStat = new StatFs(extraPath.getPath());
	long eBlockSize = extraStat.getBlockSize();
	long eTotalBlocks = extraStat.getBlockCount();
	retstr = Formatter.formatFileSize(this, (eTotalBlocks * eBlockSize) - (extraStat.getAvailableBlocks() * eBlockSize));
	retstr += "  used out of  ";
	retstr += Formatter.formatFileSize(this, eTotalBlocks * eBlockSize);
	return retstr;
    }

    private void SetupFSPartSize() {
	try {
	    mSystemSize.setSummary(ObtainFSPartSize    ("/system"));
	    mDataSize.setSummary(ObtainFSPartSize      ("/data"));
	    mCacheSize.setSummary(ObtainFSPartSize     ("/cache"));
	    mSDCardFATSize.setSummary(ObtainFSPartSize ("/sdcard"));
	    if (extfsIsMounted == true) {
		if (fileExists("/system/sd/"))
		    mSDCardEXTSize.setSummary(ObtainFSPartSize ("/system/sd"));
		else
		    mSDCardEXTSize.setEnabled(false);
	    }
	    else
		mSDCardEXTSize.setEnabled(false);
	} catch (IllegalArgumentException e) {
	    e.printStackTrace();
	}
    }

    /**
     *  Methods for files and formats
     */

    public boolean fileExists(String filename) {
	File f = new File(filename);
	return f.exists();
    }

    public void setStringSummary(String preference, String value) {
	try {
	    findPreference(preference).setSummary(value);
	} catch (RuntimeException e) {
	    findPreference(preference).setSummary(" Unavailable");
	}
    }

    public String getSystemValue(String property) {
	try {
	    return SystemProperties.get(property, " Unavailable");
	} catch (RuntimeException e) {
	    e.printStackTrace();
	}
	return " Unavailable";
    }

    public String getFormattedFingerprint() {
	String[] tab = new String(Build.FINGERPRINT).split("/");
	return new String(tab[4]);
    }

    private String getFormattedKernelVersion() {
	String procVersionStr;
	try {
	    BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
	    try {
		procVersionStr = reader.readLine();
	    } finally {
		reader.close();
	    }
	    final String PROC_VERSION_REGEX =
		"\\w+\\s+" + /* ignore: Linux */
		"\\w+\\s+" + /* ignore: version */
		"([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
		"\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
		"\\(.*?(?:\\(.*?\\)).*?\\)\\s+" + /* ignore: (gcc ..) */
		"([^\\s]+)\\s+" + /* group 3: #26 */
		"(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
		"(.+)"; /* group 4: date */
	    Pattern p = Pattern.compile(PROC_VERSION_REGEX);
	    Matcher m = p.matcher(procVersionStr);
	    if (!m.matches()) {
		Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
		return " Unavailable";
	    } else if (m.groupCount() < 4) {
		Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount() + " groups");
		return " Unavailable";
	    } else {
		return (new StringBuilder(m.group(1).substring(0, m.group(1).indexOf('-')))
			.append("\n")
			.append(m.group(2))
			.toString());
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	    return " Unavailable";
	}
    }

    /**
     *  Methods to parse ROM infos
     */

    public static String removeChar(String s, char c) {
	String r = "";
	for (int i = 0; i < s.length(); i ++)
	    if (s.charAt(i) != c)
		r += s.charAt(i);
	return r;
    }

    public String getRomName() {
	String name = Build.DISPLAY; // N_VpP(-B)?
	name = name.substring(0, name.indexOf('_')); // N
	return name;
    }

    public String getRomVersion() {
	String version = Build.DISPLAY; // N_VpP(-B)?
	version = version.substring(version.indexOf('_') + 1, version.indexOf('p')); // V
	return version;
    }

    public String getRomPatch() {
	String patch = Build.DISPLAY; // N_VpP(-B)?
	if (isRomBeta()) // P-B
	    patch = patch.substring(patch.indexOf('p') + 1, patch.indexOf('-')); // P
	else // P
	    patch = patch.substring(patch.indexOf('p') + 1, patch.length()); // P
	return patch;
    }

    public int getRomBeta() {
	String beta = Build.DISPLAY; // N_VpP(-B)?
	if (isRomBeta()) {
	    beta = beta.substring(beta.indexOf('p'), beta.length());
	    beta = beta.substring(beta.indexOf('-') + 5, beta.indexOf('-') + 6);
	    return Integer.parseInt(beta);
	}
	return 0;
    }

    public boolean isRomBeta() {
	String beta = Build.DISPLAY; // N_VpP(-B)?
	return beta.contains("-BETA");
    }

}
