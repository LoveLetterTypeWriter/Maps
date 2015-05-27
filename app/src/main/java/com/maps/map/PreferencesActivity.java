package com.maps.map;

import android.os.Bundle;
import android.preference.PreferenceActivity;

@SuppressWarnings( "deprecation" )


/**
 * This Activity shows the preferences that the user can select.
 * 
 * Currently this is just:
 * whether to show the satellite view on the map or not
 * whether to show or hide the traffic on the map
 * 
 * This activity is no longer being used
 *
 */

public class PreferencesActivity extends PreferenceActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);
	}

}
