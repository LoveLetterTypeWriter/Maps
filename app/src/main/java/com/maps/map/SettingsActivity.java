package com.maps.map;


import android.app.Activity;
import android.os.Bundle;
/**
 * Created by 1089C on 3/21/2015.
 */
public class SettingsActivity extends Activity {
    //Settings constants go here
    public static final String KEY_V_IMP_MODE = "v_impaired_mode";
    public static final String KEY_A_IMP_MODE = "a_impaired_mode";
    public static final String MAP_MODE = "map_mode";
    public static final String TRAFFIC_MODE = "traffic_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
