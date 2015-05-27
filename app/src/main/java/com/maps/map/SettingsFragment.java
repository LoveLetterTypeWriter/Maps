package com.maps.map;

import android.preference.PreferenceFragment;
import android.os.Bundle;

/**
 * Created by 1089C on 3/21/2015.
 */
public class SettingsFragment extends PreferenceFragment{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}

