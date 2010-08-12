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
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

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
import java.net.URI;
import java.net.URISyntaxException;

public class LeoParts extends PreferenceActivity
    implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "LeoParts";
    private static final String REMOUNT_RO = "mount -o ro,remount -t yaffs2 /dev/block/mtdblock3 /system";
    private static final String REMOUNT_RW = "mount -o rw,remount -t yaffs2 /dev/block/mtdblock3 /system";
    private static final String SYS_PROP_MOD_VERSION = "ro.modversion";
    private static final String SYS_PROP_MOD_PATCH = "ro.modpatch";
    private static final String VERSION_FILE = "version-beta";
    private static String REPO_ROM;
    private static String REPO_ADDONS;
    private static String REPO_PATCH;

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
    private static final String APP2SD_PREF = "app2sd";
    private ListPreference mApp2sdPref;
    private static final String PULSE_SCREEN_ON_PREF = "pulse_screen_on";
    private CheckBoxPreference mPulseScreenOnPref;
    private static final String TRACKBALL_WAKE_PREF = "trackball_wake";
    private CheckBoxPreference mTrackballWakePref;
    private static final String TRACKBALL_UNLOCK_PREF = "trackball_unlock";
    private CheckBoxPreference mTrackballUnlockPref;
    private static final String TRACKBALL_HANG_PREF = "trackball_hang";
    private CheckBoxPreference mTrackballHangPref;
    private static final String UI_SOUNDS_PREF = "ui_sounds";
    private CheckBoxPreference mUiSoundsPref;
    private static final String FIX_PERMS_PREF = "fix_perms";
    private Preference mFixPermsPref;
    private static final String FIX_MARKET_PREF = "fix_market";
    private Preference mFixMarketPref;

    // User Interface
    private static final String BATTERY_PERCENT_PREF = "battery_percent";
    private CheckBoxPreference mBatteryPercentPref;
    private static final String H_ICON_PREF = "h_icon";
    private CheckBoxPreference mHIconPref;
    private static final String HIDE_CLOCK_PREF = "hide_clock";
    private CheckBoxPreference mHideClockPref;
    private static final String SHOW_STATUS_DBM = "show_status_dbm";
    private CheckBoxPreference mShowDbmPref;
    private static final String AM_PM_PREF = "am_pm";
    private CheckBoxPreference mAmPmPref;

    private static final String UI_BATTERY_PERCENT_COLOR = "battery_status_color_title";
    private Preference mBatteryPercentColorPreference;
    private static final String UI_CLOCK_COLOR = "clock_color";
    private Preference mClockColorPref;
    private static final String UI_DBM_COLOR = "dbm_color";
    private Preference mDbmColorPref;
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

    private static final String LOCKSCREEN_MUSIC_CONTROLS = "lockscreen_music_controls";
    private CheckBoxPreference mMusicControlPref;
    private static final String LOCKSCREEN_ALWAYS_MUSIC_CONTROLS = "lockscreen_always_music_controls";
    private CheckBoxPreference mAlwaysMusicControlPref;

    private static final String ROTATION_90_PREF = "rotation_90";
    private CheckBoxPreference mRotation90Pref;
    private static final String ROTATION_180_PREF = "rotation_180";
    private CheckBoxPreference mRotation180Pref;
    private static final String ROTATION_270_PREF = "rotation_270";
    private CheckBoxPreference mRotation270Pref;

    private static final String RENDER_EFFECT_PREF = "pref_render_effect";
    private ListPreference mRenderEffectPref;
    private static final String POWER_PROMPT_PREF = "power_prompt";
    private CheckBoxPreference mPowerPromptPref;

    // Apps & Addons
    private static final String CALCULATOR_PREF = "calculator";
    private CheckBoxPreference mCalculatorPref;
    private static final String CAR_HOME_PREF = "car_home";
    private CheckBoxPreference mCarHomePref;
    private static final String EMAIL_PREF = "email";
    private CheckBoxPreference mEmailPref;
    private static final String FACEBOOK_PREF = "facebook";
    private CheckBoxPreference mFacebookPref;
    private static final String GOOGLE_TALK_PREF = "google_talk";
    private CheckBoxPreference mGoogleTalkPref;
    private static final String GOOGLE_VOICE_PREF = "google_voice";
    private CheckBoxPreference mGoogleVoicePref;
    private static final String STK_PREF = "stk";
    private CheckBoxPreference mStkPref;
    private static final String TWITTER_PREF = "twitter";
    private CheckBoxPreference mTwitterPref;
    private static final String YOUTUBE_PREF = "youtube";
    private CheckBoxPreference mYouTubePref;

    private static final String FILEMANAGER_PREF = "filemanager";
    private CheckBoxPreference mFileManagerPref;
    private static final String TERMINAL_PREF = "terminal";
    private CheckBoxPreference mTerminalPref;
    private static final String METAMORPH_PREF = "metamorph";
    private Preference mMetamorphPref;
    private static final String TRACKBALL_ALERT_PREF = "trackball_alert";
    private Preference mTrackballAlertPref;
    private static final String PLAYER_PREF = "player";
    private Preference mPlayerPref;
    private static final String BARCODE_PREF = "barcode_scanner";
    private Preference mBarcodePref;
    private static final String HANDYCALC_PREF = "handycalc";
    private Preference mHandyCalcPref;

    private static final String BOOTANIM_PREF = "bootanim";
    private ListPreference mBootanimPref;
    private static final String HTC_IME_PREF = "htc_ime";
    private CheckBoxPreference mHtcImePref;
    private static final String CPU_LED_PREF = "cpu_led";
    private CheckBoxPreference mCpuLedPref;

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
    private String UPDATE;

    private IWindowManager mWindowManager;

    @Override
	public void onCreate(Bundle icicle) {
	super.onCreate(icicle);
	addPreferencesFromResource(R.xml.leo_parts);

	PreferenceScreen prefSet = getPreferenceScreen();
	REPO_ROM = getResources().getString(R.string.repo_rom_url);
	REPO_ADDONS = getResources().getString(R.string.repo_addons_url);
	REPO_PATCH = getResources().getString(R.string.repo_patch_url);

	// root check
	if (!fileExists("/system/bin/su")      && !fileExists("/system/xbin/su"))
	    popup(getResources().getString(R.string.warning ),
		  getResources().getString(R.string.warning_root));
	// busybox check
	if (!fileExists("/system/bin/busybox") && !fileExists("/system/xbin/busybox"))
	    popup(getResources().getString(R.string.warning),
		  getResources().getString(R.string.warning_busybox));
	// a2sd check
	if (fileExists("/system/sd") == true) {
	    Log.i(TAG, "a2sd: ext partition found");
	    if (fileExists("/dev/block/mmcblk0p2") == true) {
		Log.i(TAG, "a2sd: ext partition mouted");
		extfsIsMounted = true;
	    } else {
		Log.i(TAG, "a2sd: ext partition not mounted");
	    }
	} else {
	    Log.i(TAG, "a2sd: ext partition not found");
	}
	// request root access and ensure the dir exists
	String[] commands = { "busybox mkdirp -p /data/local/tmp" };
	sendshell(commands, false, null);

	/**
	 *  ROM infos
	 */

	setStringSummary(ROM_DEVICE_PREF, Build.MODEL + " by " + Build.BRAND + "/" + Build.MANUFACTURER);
	setStringSummary(ROM_NAME_VERSION_PREF, getRomName() + "  /  " + (isRomBeta() ? getRomVersion() + "-BETA" + getRomBeta() : getRomVersion() )+ "  /  patch" + getRomPatch());
	setStringSummary(ROM_SYSTEM_BUILD_PREF, "Android " + Build.VERSION.RELEASE + "  /  " + Build.ID + " " +
			 (fileExists("/system/framework/framework.odex") ? "" : "de") + "odex  /  " + getFormattedFingerprint());
	setStringSummary(ROM_BOOTLOADER_RADIO_PREF, Build.BOOTLOADER + "  /  " + getSystemValue("gsm.version.baseband"));
	String kernel = getFormattedKernelVersion();
	findPreference(ROM_KERNEL_PREF).setSummary((kernel.equals("2.6.32.9\nandroid-build@apa26") ? "stock " : "") + kernel);
	mUpdatePref = (Preference) prefSet.findPreference(ROM_UPDATE_PREF);
	findPreference(ROM_UPDATE_PREF)
	    .setOnPreferenceClickListener(new OnPreferenceClickListener() {
		    public boolean onPreferenceClick(Preference preference) {
			patience = ProgressDialog.show(LeoParts.this, "", getResources().getString(R.string.check_latest_update), true);
			Thread t = new Thread() {
				public void run() {
				    String[] commands = {
					"busybox wget -q " + REPO_ROM + VERSION_FILE + " -O /data/local/tmp/version"
				    };
				    ShellInterface shell = new ShellInterface(commands);
				    shell.start();
				    while (shell.isAlive())
					{
					    try {
						Thread.sleep(500);
					    }
					    catch (InterruptedException e) {
					    }
					}
				    if (shell.interrupted())
					popup(getResources().getString(R.string.error), getResources().getString(R.string.download_install_error));
				    else
					mHandler.post(mBuildDownloaded);
				}
			    };
			t.start();
			return true;
		    }
		});

	/**
	 *  Quick commands
	 */

	mShutdownPref = (Preference) prefSet.findPreference(SHUTDOWN_PREF);
	findPreference(SHUTDOWN_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "toolbox reboot -p" };
		    sendshell(commands, false, getResources().getString(R.string.shuting_down));
		    return true;
		}
	    });
	mRebootPref = (Preference) prefSet.findPreference(REBOOT_PREF);
	findPreference(REBOOT_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "reboot" };
		    sendshell(commands, false, getResources().getString(R.string.rebooting));
		    return true;
		}
	    });
	mRecoveryPref = (Preference) prefSet.findPreference(BOOTLOADER_PREF);
	findPreference(RECOVERY_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "reboot recovery" };
		    sendshell(commands, false, getResources().getString(R.string.rebooting));
		    return true;
		}
	    });
	mBootloaderPref = (Preference) prefSet.findPreference(RECOVERY_PREF);
	findPreference(BOOTLOADER_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "reboot bootloader" };
		    sendshell(commands, false, getResources().getString(R.string.rebooting));
		    return true;
		}
	    });
	mRemountRWPref = (Preference) prefSet.findPreference(REMOUNT_RW_PREF);
	findPreference(REMOUNT_RO_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { REMOUNT_RO };
		    sendshell(commands, false, getResources().getString(R.string.remounting));
		    return true;
		}
	    });
	mRemountROPref = (Preference) prefSet.findPreference(REMOUNT_RO_PREF);
	findPreference(REMOUNT_RW_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { REMOUNT_RW };
		    sendshell(commands, false, getResources().getString(R.string.remounting));
		    return true;
		}
	    });

	/**
	 *  Tweaks
	 */

	mApp2sdPref = (ListPreference) prefSet.findPreference(APP2SD_PREF);
	mApp2sdPref.setOnPreferenceChangeListener(this);
	mPulseScreenOnPref = (CheckBoxPreference) prefSet.findPreference(PULSE_SCREEN_ON_PREF);
	mPulseScreenOnPref.setOnPreferenceChangeListener(this);
	mPulseScreenOnPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.TRACKBALL_SCREEN_ON, 0) == 1);
	mTrackballWakePref = (CheckBoxPreference) prefSet.findPreference(TRACKBALL_WAKE_PREF);
	mTrackballWakePref.setOnPreferenceChangeListener(this);
	mTrackballWakePref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.TRACKBALL_WAKE_SCREEN, 0) == 1);
	mTrackballUnlockPref = (CheckBoxPreference) prefSet.findPreference(TRACKBALL_UNLOCK_PREF);
	mTrackballUnlockPref.setOnPreferenceChangeListener(this);
	mTrackballUnlockPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.TRACKBALL_UNLOCK_SCREEN, 0) == 1);
	mTrackballHangPref = (CheckBoxPreference) prefSet.findPreference(TRACKBALL_HANG_PREF);
	mTrackballHangPref.setOnPreferenceChangeListener(this);
	mTrackballHangPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.TRACKBALL_HANG_UP, 0) == 1);
	mUiSoundsPref = (CheckBoxPreference) prefSet.findPreference(UI_SOUNDS_PREF);
	mUiSoundsPref.setOnPreferenceChangeListener(this);
	mUiSoundsPref.setEnabled(fileExists("/system/xbin/nouisounds"));
	mFixPermsPref = (Preference) prefSet.findPreference(FIX_PERMS_PREF);
	mFixPermsPref.setEnabled(fileExists("/system/xbin/fix_permissions"));
	findPreference(FIX_PERMS_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "fix_permissions" };
		    sendshell(commands, false, getResources().getString(R.string.fixing_permissions));
		    return true;
		}
	    });
	mFixMarketPref = (Preference) prefSet.findPreference(FIX_MARKET_PREF);
	mFixMarketPref.setEnabled(fileExists("/data/data/com.android.vending/shared_prefs/vending_preferences.xml"));
	findPreference(FIX_MARKET_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "sed -i 's/false/true/' /data/data/com.android.vending/shared_prefs/vending_preferences.xml" };
		    sendshell(commands, false, getResources().getString(R.string.fixing_market));
		    return true;
		}
	    });

	/**
	 *  User Interface
	 */

	mBatteryPercentPref = (CheckBoxPreference) prefSet.findPreference(BATTERY_PERCENT_PREF);
	mBatteryPercentPref.setOnPreferenceChangeListener(this);
	mBatteryPercentPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.BATTERY_PERCENTAGE_STATUS_ICON, 0) == 1);
	mHIconPref = (CheckBoxPreference) prefSet.findPreference(H_ICON_PREF);
	mHIconPref.setOnPreferenceChangeListener(this);
	mHIconPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.SHOW_H_ICON, 0) == 1);
	mHideClockPref = (CheckBoxPreference) prefSet.findPreference(HIDE_CLOCK_PREF);
	mHideClockPref.setOnPreferenceChangeListener(this);
	mHideClockPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.SHOW_STATUS_CLOCK, 1) == 0);
	mShowDbmPref = (CheckBoxPreference) prefSet.findPreference(SHOW_STATUS_DBM);
	mShowDbmPref.setOnPreferenceChangeListener(this);
	mShowDbmPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.SHOW_STATUS_DBM, 0) == 1);
	mAmPmPref = (CheckBoxPreference) prefSet.findPreference(AM_PM_PREF);
	mAmPmPref.setOnPreferenceChangeListener(this);
	mAmPmPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.SHOW_TWELVE_HOUR_CLOCK_PERIOD, 1) == 0);

	mMusicControlPref = (CheckBoxPreference) prefSet.findPreference(LOCKSCREEN_MUSIC_CONTROLS);
	mMusicControlPref.setOnPreferenceChangeListener(this);
	mMusicControlPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_MUSIC_CONTROLS, 0) == 1);
	mAlwaysMusicControlPref = (CheckBoxPreference) prefSet.findPreference(LOCKSCREEN_ALWAYS_MUSIC_CONTROLS);
	mAlwaysMusicControlPref.setOnPreferenceChangeListener(this);
	mAlwaysMusicControlPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_ALWAYS_MUSIC_CONTROLS, 0) == 1);

	mBatteryPercentColorPreference = prefSet.findPreference(UI_BATTERY_PERCENT_COLOR);
	mClockColorPref = prefSet.findPreference(UI_CLOCK_COLOR);
	mClockColorPref.setEnabled(mHideClockPref.isChecked() ? false : true);
	mDbmColorPref = prefSet.findPreference(UI_DBM_COLOR);
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

	mRenderEffectPref = (ListPreference) prefSet.findPreference(RENDER_EFFECT_PREF);
	mRenderEffectPref.setOnPreferenceChangeListener(this);
	updateFlingerOptions();
	mPowerPromptPref = (CheckBoxPreference) prefSet.findPreference(POWER_PROMPT_PREF);
	mPowerPromptPref.setOnPreferenceChangeListener(this);
	mPowerPromptPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.POWER_DIALOG_PROMPT, 1) == 1);

	/**
	 *  Apps & Addons
	 */

	mCalculatorPref = (CheckBoxPreference) prefSet.findPreference(CALCULATOR_PREF);
	mCalculatorPref.setOnPreferenceChangeListener(this);
	mCalculatorPref.setChecked(fileExists("/system/app/Calculator.apk"));
	mCalculatorPref.setEnabled(fileExists("/system/app/Calculator.apk"));
	mCarHomePref = (CheckBoxPreference) prefSet.findPreference(CAR_HOME_PREF);
	mCarHomePref.setOnPreferenceChangeListener(this);
	mCarHomePref.setChecked(fileExists("/system/app/CarHomeGoogle.apk") && fileExists("/system/app/CarHomeLauncher.apk"));
	mCarHomePref.setEnabled(fileExists("/system/app/CarHomeGoogle.apk") && fileExists("/system/app/CarHomeLauncher.apk"));
	mEmailPref = (CheckBoxPreference) prefSet.findPreference(EMAIL_PREF);
	mEmailPref.setOnPreferenceChangeListener(this);
	mEmailPref.setChecked(fileExists("/system/app/Email.apk"));
	mEmailPref.setEnabled(fileExists("/system/app/Email.apk"));
	mFacebookPref = (CheckBoxPreference) prefSet.findPreference(FACEBOOK_PREF);
	mFacebookPref.setOnPreferenceChangeListener(this);
	mFacebookPref.setChecked(fileExists("/system/app/Facebook.apk"));
	mFacebookPref.setEnabled(fileExists("/system/app/Facebook.apk"));
	mGoogleTalkPref = (CheckBoxPreference) prefSet.findPreference(GOOGLE_TALK_PREF);
	mGoogleTalkPref.setOnPreferenceChangeListener(this);
	mGoogleTalkPref.setChecked(fileExists("/system/app/Talk.apk"));
	mGoogleTalkPref.setEnabled(fileExists("/system/app/Talk.apk"));
	mGoogleVoicePref = (CheckBoxPreference) prefSet.findPreference(GOOGLE_VOICE_PREF);
	mGoogleVoicePref.setOnPreferenceChangeListener(this);
	mGoogleVoicePref.setChecked(fileExists("/system/app/googlevoice.apk"));
	mGoogleVoicePref.setEnabled(fileExists("/system/app/googlevoice.apk"));
	mTwitterPref = (CheckBoxPreference) prefSet.findPreference(TWITTER_PREF);
	mTwitterPref.setOnPreferenceChangeListener(this);
	mTwitterPref.setChecked(fileExists("/system/app/Twitter.apk"));
	mTwitterPref.setEnabled(fileExists("/system/app/Twitter.apk"));
	mStkPref = (CheckBoxPreference) prefSet.findPreference(STK_PREF);
	mStkPref.setOnPreferenceChangeListener(this);
	mStkPref.setChecked(fileExists("/system/app/Stk.apk"));
	mStkPref.setEnabled(fileExists("/system/app/Stk.apk"));
	mYouTubePref = (CheckBoxPreference) prefSet.findPreference(YOUTUBE_PREF);
	mYouTubePref.setOnPreferenceChangeListener(this);
	mYouTubePref.setChecked(fileExists("/system/app/YouTube.apk"));
	mYouTubePref.setEnabled(fileExists("/system/app/YouTube.apk"));

	mFileManagerPref = (CheckBoxPreference) prefSet.findPreference(FILEMANAGER_PREF);
	mFileManagerPref.setOnPreferenceChangeListener(this);
	mFileManagerPref.setChecked(fileExists("/system/app/FileManager.apk"));
	mFileManagerPref.setEnabled(mFileManagerPref.isChecked());
	mTerminalPref = (CheckBoxPreference) prefSet.findPreference(TERMINAL_PREF);
	mTerminalPref.setOnPreferenceChangeListener(this);
	mTerminalPref.setChecked(fileExists("/system/app/Terminal.apk"));
	mTerminalPref.setEnabled(mTerminalPref.isChecked());
	mMetamorphPref = (Preference) prefSet.findPreference(METAMORPH_PREF);
	findPreference(METAMORPH_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    Uri marketUri = Uri.parse("market://search?q=metamorph");
		    Intent intent = new Intent (Intent.ACTION_VIEW, marketUri);
		    startActivity(intent);
		    return true;
		}
	    });
	mTrackballAlertPref = (Preference) prefSet.findPreference(TRACKBALL_ALERT_PREF);
	findPreference(TRACKBALL_ALERT_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    Uri marketUri = Uri.parse("market://search?q=trackball%20alert");
		    Intent intent = new Intent (Intent.ACTION_VIEW, marketUri);
		    startActivity(intent);
		    return true;
		}
	    });
	mPlayerPref = (Preference) prefSet.findPreference(PLAYER_PREF);
	findPreference(PLAYER_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    Uri marketUri = Uri.parse("market://details?id=org.freecoder.android.cmplayer.v7");
		    Intent intent = new Intent (Intent.ACTION_VIEW, marketUri);
		    startActivity(intent);
		    return true;
		}
	    });
	mBarcodePref = (Preference) prefSet.findPreference(BARCODE_PREF);
	findPreference(BARCODE_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
		    Intent intent = new Intent (Intent.ACTION_VIEW, marketUri);
		    startActivity(intent);
		    return true;
		}
	    });
	mHandyCalcPref = (Preference) prefSet.findPreference(HANDYCALC_PREF);
	findPreference(HANDYCALC_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    Uri marketUri = Uri.parse("market://details?id=org.mmin.handycalc");
		    Intent intent = new Intent (Intent.ACTION_VIEW, marketUri);
		    startActivity(intent);
		    return true;
		}
	    });

	mBootanimPref = (ListPreference) prefSet.findPreference(BOOTANIM_PREF);
	mBootanimPref.setOnPreferenceChangeListener(this);
	mHtcImePref = (CheckBoxPreference) prefSet.findPreference(HTC_IME_PREF);
	mHtcImePref.setOnPreferenceChangeListener(this);
	mCpuLedPref = (CheckBoxPreference) prefSet.findPreference(CPU_LED_PREF);
	mCpuLedPref.setOnPreferenceChangeListener(this);

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
		    String url = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=FP4JTHPKJPKS6&lc=FR" +
			"&item_name=leonnib4&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted";
		    Intent i = new Intent(Intent.ACTION_VIEW);
		    i.setData(Uri.parse(url));
		    startActivity(i);
		    return true;
		}
	    });
	mAboutSources = (Preference) prefSet.findPreference(ABOUT_SOURCES);
	findPreference(ABOUT_SOURCES).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String url = "http://github.com/leonnib4/packages_apps_Settings";
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
	mOldApp2sdPref.setChecked(fileExists("/system/sd/app"));
	mDalvik2sdPref     = (CheckBoxPreference) prefSet.findPreference(DALVIK2SD_PREF);
	mDalvik2sdPref.setOnPreferenceChangeListener(this);
	mDalvik2sdPref.setEnabled(extfsIsMounted);
	mDalvik2sdPref.setChecked(fileExists("/system/sd/dalvik-cache"));
	mData2sdPref       = (CheckBoxPreference) prefSet.findPreference(DATA2SD_PREF);
	mData2sdPref.setOnPreferenceChangeListener(this);
	mData2sdPref.setEnabled(extfsIsMounted);
	mData2sdPref.setChecked(fileExists("/system/sd/data"));
	mMedia2sdPref      = (CheckBoxPreference) prefSet.findPreference(MEDIA2SD_PREF);
	mMedia2sdPref.setOnPreferenceChangeListener(this);
	mMedia2sdPref.setEnabled(extfsIsMounted);
	mMedia2sdPref.setChecked(fileExists("/system/sd/media"));

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
			extfsIsMounted = (fileExists("/dev/block/mmcblk0p2") && fileExists("/system/sd"));
			mOldApp2sdPref.setChecked(fileExists("/system/sd/app"));
			mDalvik2sdPref.setChecked(fileExists("/system/sd/dalvik-cache"));
			mData2sdPref.setChecked(fileExists("/system/sd/data"));
			mMedia2sdPref.setChecked(fileExists("/system/sd/media"));
			return true;
		    }
		});

	/**
	 *  Defaults
	 */

	mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
	if (preference == mApp2sdPref) {
	    String[] commands = {
		"pm setInstallLocation " + objValue
	    };
	    sendshell(commands, false, "Activating stock app2sd...");
	}
	else if (preference == mUiSoundsPref) {
	    String[] commands = { "nouisounds" };
	    if (mUiSoundsPref.isChecked() == false)
		sendshell(commands, false, getResources().getString(R.string.deactivating_ui_sounds));
	    else
		sendshell(commands, false, getResources().getString(R.string.activating_ui_sounds));
	}
	else if (preference == mBatteryPercentPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.BATTERY_PERCENTAGE_STATUS_ICON, mBatteryPercentPref.isChecked() ? 0 : 1);
	    toast(getResources().getString(R.string.should_reboot));
	}
	else if (preference == mHIconPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.SHOW_H_ICON, mHIconPref.isChecked() ? 0 : 1);
	    toast(getResources().getString(R.string.should_reboot));
	}
	else if (preference == mHideClockPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.SHOW_STATUS_CLOCK, mHideClockPref.isChecked() ? 1 : 0);
	    toast(getResources().getString(R.string.should_reboot));
	}
	else if (preference == mShowDbmPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.SHOW_STATUS_DBM, mShowDbmPref.isChecked() ? 0 : 1);
	    toast(getResources().getString(R.string.should_reboot));
	}
	else if (preference == mAmPmPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.SHOW_TWELVE_HOUR_CLOCK_PERIOD, mAmPmPref.isChecked() ? 1 : 0);
	    toast(getResources().getString(R.string.should_reboot));
	}
	else if (preference == mMusicControlPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_MUSIC_CONTROLS, mMusicControlPref.isChecked() ? 0 : 1);
	    toast(getResources().getString(R.string.should_reboot));
	}
	else if (preference == mAlwaysMusicControlPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_ALWAYS_MUSIC_CONTROLS, mAlwaysMusicControlPref.isChecked() ? 0 : 1);
	    toast(getResources().getString(R.string.should_reboot));
	}
	else if (preference == mPulseScreenOnPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_SCREEN_ON, mPulseScreenOnPref.isChecked() ? 0 : 1);
	    toast(getResources().getString(R.string.should_reboot));
	}
	else if (preference == mTrackballWakePref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_WAKE_SCREEN, mTrackballWakePref.isChecked() ? 0 : 1);
	}
	else if (preference == mTrackballUnlockPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_UNLOCK_SCREEN, mTrackballUnlockPref.isChecked() ? 0 : 1);
	}
	else if (preference == mTrackballHangPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_HANG_UP, mTrackballHangPref.isChecked() ? 0 : 1);
	}
	else if (preference == mRotation90Pref || preference == mRotation180Pref || preference == mRotation270Pref) {
	    int mode = 0;
	    if (mRotation90Pref.isChecked()) mode += 1;
	    if (mRotation180Pref.isChecked()) mode += 2;
	    if (mRotation270Pref.isChecked()) mode += 4;
	    if (preference == mRotation90Pref) mode += 1;
	    if (preference == mRotation180Pref) mode += 2;
	    if (preference == mRotation270Pref) mode += 4;
	    Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION_MODE, mode);
	    toast(getResources().getString(R.string.should_reboot));
	}
	else if (preference == mRenderEffectPref)
	    writeRenderEffect(Integer.valueOf(objValue.toString()));
	else if (preference == mPowerPromptPref)
	    Settings.System.putInt(getContentResolver(), Settings.System.POWER_DIALOG_PROMPT, mPowerPromptPref.isChecked() ? 0 : 1);
	else if (preference == mCalculatorPref)
	    return removeSystemApp(mCalculatorPref, "Calculator", "Calculator.apk");
	else if (preference == mCarHomePref)
	    return removeSystemApp(mCarHomePref, "CarHome", "CarHomeGoogle.apk", "CarHomeLauncher.apk");
	else if (preference == mEmailPref)
	    return removeSystemApp(mEmailPref, "Email", "Email.apk");
	else if (preference == mFacebookPref)
	    return removeSystemApp(mFacebookPref, "Facebook", "Facebook.apk");
	else if (preference == mGoogleTalkPref)
	    return removeSystemApp(mGoogleTalkPref, "Talk", "Talk.apk");
	else if (preference == mGoogleVoicePref)
	    return removeSystemApp(mFacebookPref, "Google Voice", "googlevoice.apk");
	else if (preference == mStkPref)
	    return removeSystemApp(mStkPref, "Sim Toolkit", "Stk.apk");
	else if (preference == mTwitterPref)
	    return removeSystemApp(mTwitterPref, "Twitter", "Twitter.apk");
	else if (preference == mYouTubePref)
	    return removeSystemApp(mYouTubePref, "YouTube", "YouTube.apk");
	else if (preference == mFileManagerPref)
	    return removeSystemApp(mFileManagerPref, "FileManager", "FileManager.apk");
	else if (preference == mTerminalPref)
	    return removeSystemApp(mTerminalPref, "Terminal", "Terminal.apk");
	else if (preference == mBootanimPref) {
	    String[] commands = {
		REMOUNT_RW,
		"busybox wget -q " + REPO_ADDONS + objValue.toString() + " -O /data/local/tmp/bootanimation.zip" +
		" && busybox mv /data/local/tmp/bootanimation.zip /system/media/bootanimation.zip",
		REMOUNT_RO
	    };
	    sendshell(commands, true, getResources().getString(R.string.downloading_installing) + "bootanimation.zip...");
	}
	else if (preference == mHtcImePref) {
	    if (mHtcImePref.isChecked() == false) {
		if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 0) {
		    toastLong(getResources().getString(R.string.third_part_apps));
		    Intent intent = new Intent();
		    intent.setAction(Settings.ACTION_APPLICATION_SETTINGS);
		    startActivity(intent);
		    return false;
		}
		String[] commands = {
		    "busybox wget -q " + REPO_ADDONS + "clicker.apk -O /data/local/tmp/clicker.apk" +
		    " && pm install -r /data/local/tmp/clicker.apk ; busybox rm -f /data/local/tmp/clicker.apk",
		    "busybox wget -q " + REPO_ADDONS + "htc_ime.apk -O /data/local/tmp/htc_ime.apk" +
		    " && pm install -r /data/local/tmp/htc_ime.apk ; busybox rm -f /data/local/tmp/htc_ime.apk"
		};
		sendshell(commands, false, getResources().getString(R.string.downloading_installing) + " HTC_IME...");
	    }
	    else {
		String[] commands = {
		    "pm uninstall com.htc.clicker",
		    "pm uninstall jonasl.ime"
		};
		sendshell(commands, false, getResources().getString(R.string.removing) + " HTC_IME...");
	    }
	}
	else if (preference == mCpuLedPref)
	    return installOrRemoveAddon(mCpuLedPref, "cpu_led.apk", false, "CPU Led", "com.britoso.cpustatusled");
	else if (preference == mOldApp2sdPref)
	    return activate2sd(mOldApp2sdPref, "a2sd");
	else if (preference == mDalvik2sdPref)
	    return activate2sd(mDalvik2sdPref, "dalvik2sd");
	else if (preference == mData2sdPref)
	    return activate2sd(mData2sdPref, "data2sd");
	else if (preference == mMedia2sdPref)
	    return activate2sd(mMedia2sdPref, "media2sd");
	else
	    Log.e(TAG, "PreferenceChange: This element have no defined action!");

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
	else if (preference == mDbmColorPref) {
	    ColorPickerDialog cp = new ColorPickerDialog(this, mDbmColorListener, readDbmColor());
	    cp.show();
	}
	else if (preference == mDateColorPref) {
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
	else
	    Log.e(TAG, "TreeClick: This element have no defined action!");
	return true;
    }

    ColorPickerDialog.OnColorChangedListener mBatteryColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.BATTERY_PERCENTAGE_STATUS_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mDateFontColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.DATE_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
	    }
	};

    private int readDateFontColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.DATE_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -16777216;
	}
    }

    ColorPickerDialog.OnColorChangedListener mPlmnLabelColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.PLMN_LABEL_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mSpnLabelColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.SPN_LABEL_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mNotifTickerColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NEW_NOTIF_TICKER_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mNotifCountColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_COUNT_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mNoNotifColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NO_NOTIF_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mClearLabelColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.CLEAR_BUTTON_LABEL_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mOngoingNotifColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.ONGOING_NOTIF_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mLatestNotifColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.LATEST_NOTIF_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mNotifItemTitleColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TITLE_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mNotifItemTextColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TEXT_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mNotifItemTimeColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TIME_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
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

    ColorPickerDialog.OnColorChangedListener mDbmColorListener = new ColorPickerDialog.OnColorChangedListener() {
	    public void colorChanged(int color) {
		Settings.System.putInt(getContentResolver(), Settings.System.DBM_COLOR, color);
		toast(getResources().getString(R.string.should_reboot));
	    }
	};

    private int readDbmColor() {
	try {
	    return Settings.System.getInt(getContentResolver(), Settings.System.DBM_COLOR);
	}
	catch (SettingNotFoundException e) {
	    return -16777216;
	}
    }

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
			popup(getResources().getString(R.string.error), getResources().getString(R.string.download_install_error));
		    if (reboot == true)
			mHandler.post(mNeedReboot);
		}
	    };
	t.start();
	return true;
    }

    /**
     *  Methods for render effects
     */

    private void updateFlingerOptions() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                flinger.transact(1010, data, reply, 0);
                int v;
                v = reply.readInt();
                v = reply.readInt();
                v = reply.readInt();
                v = reply.readInt();
                v = reply.readInt();
                mRenderEffectPref.setValue(String.valueOf(v));
                reply.recycle();
                data.recycle();
            }
        } catch (RemoteException ex) {
        }
    }

    private void writeRenderEffect(int id) {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeInt(id);
                flinger.transact(1014, data, null, 0);
                data.recycle();
            }
        } catch (RemoteException ex) {
        }
    }

    /**
     *  Methods for updates and patches
     */

    public boolean askToUpgrade(final String ui_current, final int latest, final String ui_latest, final String update) {
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle(getResources().getString(R.string.leo_updater))
	    .setMessage(getResources().getString(R.string.rom_outdated)
			.replaceFirst("%b%", ui_current)
			.replaceFirst("%B%", ui_latest) + "\n"
			+ getResources().getString(R.string.like_to_upgrade))
	    .setCancelable(false)
	    .setPositiveButton(getResources().getString(R.string.yeah),
			       new DialogInterface.OnClickListener() {
				   public void onClick(DialogInterface dialog, int id) {
				       toastLong(getResources().getString(R.string.downloading_upgrade));
				       String url = REPO_ROM + update;
				       String[] commands = {
					   "busybox wget -q " + url + ".md5" +
					   " -O /data/local/tmp/" + update + ".md5"
				       };
				       sendshell(commands, false, null);
				       Intent i = new Intent(Intent.ACTION_VIEW);
				       i.setData(Uri.parse(url));
				       startActivity(i);
				   }
			       })
	    .setNegativeButton(getResources().getString(R.string.not_now),
			       new DialogInterface.OnClickListener() {
				   public void onClick(DialogInterface dialog, int id) {
				   }
			       });
	AlertDialog alert = builder.create();
	alert.show();
	return true;
    }

    final Runnable mApplyPatch = new Runnable() {
	    public void run() {
		if (fileExists("/data/local/patch")) {
		    final int patch = PATCH;
		    String[] commands = {
			"/data/local/patch",
			REMOUNT_RW,
			"busybox sed -i 's/ro.modversion=" + getSystemValue(SYS_PROP_MOD_PATCH) + "/ro.modversion=" + patch + "/' /system/build.prop",
			REMOUNT_RO,
			"busybox rm -f /data/local/patch"
		    };
		    sendshell(commands, true, "Applying patch #" + PATCH + "...");
		    setStringSummary(ROM_NAME_VERSION_PREF, getRomName() + "  /  " + getRomVersion() + "  /  patch" + (patch - Integer.parseInt(removeChar(getRomVersion(), '.')) * 10));
		} else {
		    popup(getResources().getString(R.string.error),
			  getResources().getString(R.string.download_install_error));
		}

	    }
	};

    public boolean applyPatch(final int latest, final String ui_latest) {
	PATCH = latest;
	patience = ProgressDialog.show(LeoParts.this, "",
				       getResources().getString(R.string.getting_patch) + latest + "...",
				       true);
	Thread t = new Thread() {
		public void run() {
		    String[] commands = {
			"busybox wget -q " + REPO_PATCH + "patch-" + PATCH + " -O /data/local/patch" +
			" && busybox chmod 755 /data/local/patch"
		    };
		    ShellInterface shell = new ShellInterface(commands);
		    shell.start();
		    while (shell.isAlive())
			{
			    try {
				Thread.sleep(500);
			    }
			    catch (InterruptedException e) {
			    }
			}
		    patience.cancel();
		    if (shell.interrupted())
			popup(getResources().getString(R.string.error),
			      getResources().getString(R.string.download_install_error));
		    else
			mHandler.post(mApplyPatch);
		}
	    };
	t.start();
	return true;
    }

    public void askToPatch(final String ui_current, final int latest, final String ui_latest) {
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle(getResources().getString(R.string.leo_updater))
	    .setMessage(getResources().getString(R.string.patch_available).replaceFirst("%b%", ui_current).replaceFirst("%P%", ui_latest))
	    .setCancelable(false)
	    .setPositiveButton(getResources().getString(R.string.grab_it),
			       new DialogInterface.OnClickListener() {
				   public void onClick(DialogInterface dialog, int id) {
				       applyPatch(latest, ui_latest);
				   }
			       })
	    .setNegativeButton(getResources().getString(R.string.dont_care),
			       new DialogInterface.OnClickListener() {
				   public void onClick(DialogInterface dialog, int id) {
				   }
			       });
	AlertDialog alert = builder.create();
	alert.show();
    }

    private void GoodMd5() {
	String commands[] = {
		"busybox mv /sdcard/download/" + UPDATE + " /sdcard/" + UPDATE,
		REMOUNT_RW,
		"mkdir -p /cache/recovery",
		"echo 'boot-recovery' > /cache/recovery/command",
		"echo '--update_package=SDCARD:" + UPDATE + "' >> /cache/recovery/command",
		REMOUNT_RO,
		"reboot recovery"
	};
	sendshell(commands, false, getResources().getString(R.string.will_flash));
    };

    private void BadMd5() {
	String commands[] = {
	    REMOUNT_RW,
	    "busybox rm -f /sdcard/download/" + UPDATE,
	    "busybox rm -f /data/local/tmp/" + UPDATE + ".md5",
	    "busybox rm -f /data/local/tmp/leofroyo.md5",
	    REMOUNT_RO
	};
	sendshell(commands, false, getResources().getString(R.string.bad_md5));
    }

    final Runnable mCheckMd5 = new Runnable() {
	    public void run() {
		if (fileExists("/data/local/tmp/leofroyo.md5"))
		    GoodMd5();
		else
		    BadMd5();
	    }
	};

    final Runnable mFlashUpgrade = new Runnable() {
	    public void run() {
		Thread t = new Thread() {
			public void run() {
			    String commands[] = {
				"busybox rm -f /cache/recovery/command",
				"cmp /data/local/tmp/" + UPDATE + ".md5 /data/local/tmp/leofroyo.md5 || rm /data/local/tmp/leofroyo.md5"
			    };
			    ShellInterface shell = new ShellInterface(commands);
			    shell.start();
			    while (shell.isAlive()) {
				try {
				    Thread.sleep(500);
				}
				catch (InterruptedException e) {
				}
			    }
			    if (shell.interrupted())
				BadMd5();
			    else
				mHandler.post(mCheckMd5);
			}
		    };
		toastLong(getResources().getString(R.string.calc_md5) + " 2/2");
		t.start();
	    }
	};

    private void prepareUpgrade(final String update) {
	Thread t = new Thread() {
		public void run() {
		    String commands[] = { "cd /sdcard/download && md5sum " + update + " > /data/local/tmp/leofroyo.md5" };
		    ShellInterface shell = new ShellInterface(commands);
		    shell.start();
		    while (shell.isAlive()) {
			try {
			    Thread.sleep(500);
			}
			catch (InterruptedException e) {
			}
		    }
		    if (shell.interrupted())
			BadMd5();
		    else
			mHandler.post(mFlashUpgrade);
		}
	    };
	toastLong(getResources().getString(R.string.calc_md5) + " 1/2");
	t.start();
    }

    final Runnable mBuildDownloaded = new Runnable() {
	    public void run() {
		patience.cancel();
		File file = new File("/data/local/tmp/version");
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		DataInputStream dis = null;
		String build = getSystemValue(SYS_PROP_MOD_PATCH);
		try {
		    fis = new FileInputStream(file);
		    bis = new BufferedInputStream(fis);
		    dis = new DataInputStream(bis);
		    while (dis.available() != 0) {
			build = dis.readLine();
			break ;
		    }
		    fis.close();
		    bis.close();
		    dis.close();
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		// latest
		final int latest = Integer.parseInt(removeChar(removeChar(build, '.'), 'p'));
		final String ui_latest = build.charAt(0) + "." + build.charAt(1) + "." + build.charAt(2) + "-patch" + build.charAt(3);
		setStringSummary(ROM_UPDATE_PREF, " " + getResources().getString(R.string.latest) + ": " + ui_latest);
		// current
		final int current = Integer.parseInt(removeChar(getRomVersion(), '.') + getRomPatch());
		final String ui_current = getRomVersion() + "-patch" + getRomPatch();
		// check
		Log.i(TAG, "latest: " + ui_latest + " / " + latest);
		Log.i(TAG, "current: " + ui_current + " / " + current);
		if (current == latest)
		    popup(getResources().getString(R.string.leo_updater),
			  getResources().getString(R.string.rom_uptodate)
			  .replaceFirst("%b%", ui_current));
		else if (current/10 < latest/10) {
		    UPDATE = getRomName() + "_"
			+ (latest/1000) + "."
			+ (latest/100) % 10 + "."
			+ (latest/10) % 10 + "-noradio-signed.zip";
		    if (fileExists("/data/local/tmp/" + UPDATE + ".md5"))
			prepareUpgrade(UPDATE);
		    else
		    	askToUpgrade(ui_current, latest, ui_latest, UPDATE);
		}
		else if (current < latest) {
		    askToPatch(ui_current, latest, ui_latest);
		}
		else
		    popup(getResources().getString(R.string.leo_updater),
			  getResources().getString(R.string.would_be_proud));
	    }
	};

    /**
     *  Methods for popups
     */

    public void toast(final CharSequence message) {
	Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
	toast.show();
    }

    public void toastLong(final CharSequence message) {
	Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
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
	    .setPositiveButton(getResources().getString(R.string.yes),
			       new DialogInterface.OnClickListener() {
				   public void onClick(DialogInterface dialog, int id) {
				       String[] commands = { "reboot" };
				       sendshell(commands, false, getResources().getString(R.string.rebooting));
				   }
			       })
	    .setNegativeButton(getResources().getString(R.string.no),
			       new DialogInterface.OnClickListener() {
				   public void onClick(DialogInterface dialog, int id) {
				   }
			       });
	AlertDialog alert = builder.create();
	alert.show();
    }

    /**
     *  Methods for apps & addons
     */

    public boolean removeSystemApp(final CheckBoxPreference preference, final String name, final String apk1, final String apk2) {
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle(getResources().getString(R.string.confirm))
	    .setMessage(getResources().getString(R.string.sure_remove) + " " + name + "?")
	    .setCancelable(false)
	    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
			String[] commands = {
			    REMOUNT_RW,
			    "busybox rm -f /system/app/" + apk1,
			    "busybox rm -f /system/app/" + apk2,
			    REMOUNT_RO
			};
			sendshell(commands, false, getResources().getString(R.string.removing) + " " + name + "...");
			preference.setEnabled(false);
		    }
		})
	    .setNegativeButton("No", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
			preference.setChecked(true);
		    }
		});
	AlertDialog alert = builder.create();
	alert.show();
	return true;
    }

    public boolean removeSystemApp(final CheckBoxPreference preference, final String name, final String apk1) {
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle(getResources().getString(R.string.confirm))
	    .setMessage(getResources().getString(R.string.sure_remove) + " " + name + "?")
	    .setCancelable(false)
	    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
			String[] commands = {
			    REMOUNT_RW,
			    "busybox rm -f /system/app/" + apk1,
			    REMOUNT_RO
			};
			sendshell(commands, false, getResources().getString(R.string.removing) + " " + name + "...");
			preference.setEnabled(false);
		    }
		})
	    .setNegativeButton("No", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
			preference.setChecked(true);
		    }
		});
	AlertDialog alert = builder.create();
	alert.show();
	return true;
    }

    public boolean installOrRemoveAddon(CheckBoxPreference preference, final String src, final boolean reboot, final String name, final String activity) {
	boolean have = preference.isChecked();
	if (!have) {
	    if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 0) {
		toast(getResources().getString(R.string.third_part_apps));
		Intent intent = new Intent();
		intent.setAction(Settings.ACTION_APPLICATION_SETTINGS);
		startActivity(intent);
		return false;
	    }
	    String[] commands = {
	    	"busybox wget -q " + src + " -O /data/local/tmp/" + activity + ".apk" +
	    	" && pm install -r /data/local/tmp/" + activity + ".apk ; busybox rm -f /data/local/tmp/" + activity + ".apk"
	    };
	    sendshell(commands, false, getResources().getString(R.string.downloading_installing) + " " + name + "...");
	}
	else {
	    String[] commands = {
		"pm uninstall " + activity
	    };
	    sendshell(commands, false, getResources().getString(R.string.removing) + " " + name + "...");
	}
	return true;
    }

    /**
     *  Methods for storage
     */

    public boolean activate2sd(final CheckBoxPreference preference, final String script) {
	boolean have = preference.isChecked();
	if (!have) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setTitle(getResources().getString(R.string.warning))
		.setMessage(getResources().getString(R.string.low_class_sdcard))
		.setCancelable(false)
		.setPositiveButton(getResources().getString(R.string.have_a_good_one),
				   new DialogInterface.OnClickListener() {
				       public void onClick(DialogInterface dialog, int id) {
					   String[] commands = {
					       script + " on"
					   };
					   sendshell(commands, false, getResources().getString(R.string.move_sdcard));
				       }
				   })
		.setNegativeButton(getResources().getString(R.string.cancel),
				   new DialogInterface.OnClickListener() {
				       public void onClick(DialogInterface dialog, int id) {
					   preference.setChecked(false);
				       }
				   });
	    AlertDialog alert = builder.create();
	    alert.show();
	} else {
	    String[] commands = {
		script + " off",
	    };
	    sendshell(commands, false, getResources().getString(R.string.move_phone));
	}
	return true;
    }

    private String ObtainFSPartSize(String PartitionPath) {
	String retstr;
	File extraPath = new File(PartitionPath);
	StatFs extraStat = new StatFs(extraPath.getPath());
	long eBlockSize = extraStat.getBlockSize();
	long eTotalBlocks = extraStat.getBlockCount();
	retstr = Formatter.formatFileSize(this, (eTotalBlocks * eBlockSize) - (extraStat.getAvailableBlocks() * eBlockSize));
	retstr += "  " + getResources().getString(R.string.used_out_of) + "  ";
	retstr += Formatter.formatFileSize(this, eTotalBlocks * eBlockSize);
	return retstr;
    }

    private void SetupFSPartSize() {
	try {
	    mSystemSize.setSummary(ObtainFSPartSize        ("/system"));
	    mDataSize.setSummary(ObtainFSPartSize          ("/data"));
	    mCacheSize.setSummary(ObtainFSPartSize         ("/cache"));
	    mSDCardFATSize.setSummary(ObtainFSPartSize     ("/sdcard"));
	    if (extfsIsMounted == true)
		mSDCardEXTSize.setSummary(ObtainFSPartSize ("/system/sd"));
	    else
		mSDCardEXTSize.setEnabled(false);
	} catch (IllegalArgumentException e) {
	    e.printStackTrace();
	}
    }

    /**
     *  Methods for files and formats
     */

    public static String removeChar(String s, char c) {
	String r = "";
	for (int i = 0; i < s.length(); i ++)
	    if (s.charAt(i) != c)
		r += s.charAt(i);
	return r;
    }

    public boolean fileExists(String filename) {
	File f = new File(filename);
	return f.exists();
    }

    public void setStringSummary(String preference, String value) {
	try {
	    findPreference(preference).setSummary(value);
	} catch (RuntimeException e) {
	    findPreference(preference).setSummary(" " + getResources().getString(R.string.unavailable));
	}
    }

    public String getSystemValue(String property) {
	try {
	    return SystemProperties.get(property, "?");
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
		return " " + getResources().getString(R.string.unavailable);
	    } else {
		return (new StringBuilder(m.group(1).substring(0, m.group(1).indexOf('-')))
			.append("\n")
			.append(m.group(2))
			.toString());
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	    return " " + getResources().getString(R.string.unavailable);
	}
    }

    /**
     *  Methods to parse ROM infos
     */

    public String getRomName() { // .+
	String name = getSystemValue(SYS_PROP_MOD_VERSION);
	name = name.substring(0, name.indexOf('_'));
        return (name == null || name.length() == 0 ? "?" : name);
    }

    public String getRomVersion() { // [0-9]\.[0-9]\.[0-9]
	String version = getSystemValue(SYS_PROP_MOD_VERSION);
	version = version.substring(version.indexOf('_') + 1, version.length());
	if (version.contains("-"))
	    version = version.substring(0, version.indexOf('-'));
        return (version == null || version.length() == 0 ? "?" : version);
    }

    public String getRomPatch() { // version + [0-9]+ = [0-9]{4}
	String patch = getSystemValue(SYS_PROP_MOD_PATCH);
	if (patch != null && patch.length() == 4)
	    return patch.substring(3, 4);
        return getResources().getString(R.string.unavailable);
    }

    public int getRomBeta() { // -BETA[0-9]?
	String beta = getSystemValue(SYS_PROP_MOD_VERSION);
	if (isRomBeta()) {
	    beta = beta.substring(beta.indexOf('-') + 5, beta.indexOf('-') + 6);
	    return Integer.parseInt(beta);
	}
	return 0;
    }

    public boolean isRomBeta() {
	String beta = getSystemValue(SYS_PROP_MOD_VERSION);
	return beta.contains("-BETA");
    }

}
