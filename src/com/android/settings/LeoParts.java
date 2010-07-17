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
** limitations under the License.
*/

package com.android.settings;

import com.android.settings.ShellInterface;

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
    private static final String ROM_NAME_VERSION_PREF = "rom_name_version";
    private static final String ROM_SYSTEM_BUILD_PREF = "rom_system_build";
    private static final String ROM_RADIO_PREF = "rom_radio";
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
    private static final String PULSE_SCREEN_ON_PREF = "pulse_screen_on";
    private CheckBoxPreference mPulseScreenOnPref;
    private static final String HIDE_CLOCK_PREF = "hide_clock";
    private CheckBoxPreference mHideClockPref;
    private static final String TRACKBALL_WAKE_PREF = "trackball_wake";
    private CheckBoxPreference mTrackballWakePref;
    private static final String TRACKBALL_UNLOCK_PREF = "trackball_unlock";
    private CheckBoxPreference mTrackballUnlockPref;

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

	setStringSummary(ROM_NAME_VERSION_PREF, getRomName() + "  /  " + (isRomBeta() ? getRomVersion() + "-BETA" + getRomBeta() : getRomVersion() )+ "  /  patch" + getRomPatch());
	setStringSummary(ROM_SYSTEM_BUILD_PREF, "Android " + Build.VERSION.RELEASE + "  /  " + Build.ID + " " +
			 (fileExists("/system/framework/framework.odex") ? "" : "de") + "odex'ed  /  " + getFormattedFingerprint());
	setStringSummary(ROM_RADIO_PREF, getSystemValue("gsm.version.baseband"));
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
	mPulseScreenOnPref = (CheckBoxPreference) prefSet.findPreference(PULSE_SCREEN_ON_PREF);
	mPulseScreenOnPref.setOnPreferenceChangeListener(this);
	mHideClockPref = (CheckBoxPreference) prefSet.findPreference(HIDE_CLOCK_PREF);
	mHideClockPref.setOnPreferenceChangeListener(this);
	mTrackballWakePref = (CheckBoxPreference) prefSet.findPreference(TRACKBALL_WAKE_PREF);
	mTrackballWakePref.setOnPreferenceChangeListener(this);
	mTrackballUnlockPref = (CheckBoxPreference) prefSet.findPreference(TRACKBALL_UNLOCK_PREF);
	mTrackballUnlockPref.setOnPreferenceChangeListener(this);

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
	    notify("You should reboot for the changes to take effect.");
	}
	else if (preference == mPulseScreenOnPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_SCREEN_ON, mPulseScreenOnPref.isChecked() ? 0 : 1);
	    notify("You should reboot for the changes to take effect.");
	}
	else if (preference == mHideClockPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.SHOW_STATUS_CLOCK, mHideClockPref.isChecked() ? 1 : 0);
	    notify("You should reboot for the changes to take effect.");
	}
	else if (preference == mTrackballWakePref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_WAKE_SCREEN, mTrackballWakePref.isChecked() ? 0 : 1);
	    notify("You should reboot for the changes to take effect.");
	}
	else if (preference == mTrackballUnlockPref) {
	    Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_UNLOCK_SCREEN, mTrackballUnlockPref.isChecked() ? 0 : 1);
	    notify("You should reboot for the changes to take effect.");
	}

	// always let the preference setting proceed.
	return true;
    }

    @Override
	public void onResume() {
	super.onResume();
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

    public void notify(final CharSequence message) {
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
		mSDCardEXTSize.setSummary(ObtainFSPartSize ("/system/sd"));
		mSDCardEXTSize.setEnabled(false);
	    }
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
