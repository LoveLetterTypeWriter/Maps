package com.maps.map;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * Displays the event details in a quasi-popup layout.
 * Note that there is a popup class but it was simpler to use
 * an Activity. However, this isn't done very cleanly and
 * it would be better to redo this at some point.
 * 
 * This comes up if the user clicks on an event displayed on the
 * map (if they click the marker for the event) or if they click on
 * an item in the event list.
 * 
 * If the user clicks above or below the event details it will
 * close the event details.
 * 
 * Perhaps it should be that the user can click anywhere to dismiss it.
 * Or perhaps it would be better to just make this a "normal" Activity
 * and have the user have to hit the back button to close out of this.
 * 
 */

public class EventDetailsActivity extends Activity {
    private GlobalActivity g;
    SharedPreferences sharedPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        g = (GlobalActivity)getApplication();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean voiceover_setting = sharedPrefs.getBoolean(SettingsActivity.VOICEOVER_SETTING, false);
        boolean cctext_setting = sharedPrefs.getBoolean(SettingsActivity.COLORCODEDTEXT_SETTING, false);
            
        setContentView(R.layout.popup_layout);
            
        String id = getIntent().getExtras().getString("id");
            
        EventManager em = EventManager.getInstance(this);
    	    
        EmergencyEvent e = em.getSpecificEvent(id);
    	    
        TextView tv = (TextView) findViewById(R.id.popup_content);
    	    
        //HTML formatting looks much better than a normal String
        tv.setText(Html.fromHtml(e.toHtmlString(cctext_setting)));

        //NEW: Speaker stuff
        if (voiceover_setting && g.speaker != null)
        {
            g.speaker.speak(e.getSelectionSpeech());
        }

        /* If the user clicked either above or below the event details
    	activity then it closes the event details. Take a look at
    	popup_layout.xml if this is confusing. */
    	    
        View v = (View) findViewById(R.id.view_to_listen_for_touch);
    	    
        v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                	finish();
                }
            });
    	    
        v = (View) findViewById(R.id.view_to_listen_for_touch2);
    	    
        v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                	finish();
                }
            });
    	    
    }
}
