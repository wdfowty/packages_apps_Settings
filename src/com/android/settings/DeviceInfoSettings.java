/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.util.Config;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceInfoSettings extends PreferenceActivity {

    private static final String TAG = "DeviceInfoSettings";
    private static final boolean LOGD = false || Config.LOGD;

    private static final String KEY_CONTAINER = "container";
    private static final String KEY_TEAM = "team";
    private static final String KEY_CONTRIBUTORS = "contributors";
    private static final String KEY_TERMS = "terms";
    private static final String KEY_LICENSE = "license";
    private static final String KEY_COPYRIGHT = "copyright";
    private static final String PROPERTY_URL_SAFETYLEGAL = "ro.url.safetylegal";

    @Override
    protected void onCreate(Bundle icicle) {
	super.onCreate(icicle);

	addPreferencesFromResource(R.xml.device_info_settings);

	// Remove Safety information preference if PROPERTY_URL_SAFETYLEGAL is not set
	removePreferenceIfPropertyMissing(getPreferenceScreen(), "safetylegal",
		PROPERTY_URL_SAFETYLEGAL);

	/*
	 * Settings is a generic app and should not contain any device-specific
	 * info.
	 */

	// These are contained in the "container" preference group
	PreferenceGroup parentPreference = (PreferenceGroup) findPreference(KEY_CONTAINER);
	Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_TERMS,
		Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
	Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_LICENSE,
		Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
	Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_COPYRIGHT,
		Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
	Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_TEAM,
		Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);

	// These are contained by the root preference screen
	parentPreference = getPreferenceScreen();
	Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_CONTRIBUTORS,
		Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);

	findPreference("build_number").setSummary(Build.DISPLAY);
    }

    private void removePreferenceIfPropertyMissing(PreferenceGroup preferenceGroup,
	    String preference, String property ) {
	if (SystemProperties.get(property).equals(""))
	{
	    // Property is missing so remove preference from group
	    try {
		preferenceGroup.removePreference(findPreference(preference));
	    } catch (RuntimeException e) {
		Log.d(TAG, "Property '" + property + "' missing and no '"
			+ preference + "' preference");
	    }
	}
    }

}
