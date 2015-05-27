package com.maps.map;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TabHost;

@SuppressWarnings( "deprecation" )

/**
 * This is the Activity that is initially opened when the
 * program launches. It is what creates all of the tabs
 * at the top of the program. It also listens for intents from
 * the SMSReceiver, which it then sends to the rest of the program.
 * 
 * The program initially starts out with the map tab open.
 * The other tabs are the list of emergencies and the preferences tab.
 *
 * This uses deprecated code and should probably be updated at some point.
 */

public class TabMainActivity extends TabActivity { 
	
	TabHost mTabHost;
    public GlobalActivity g;
    private final int CHECK_CODE = 0x1;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mTabHost = getTabHost();
        g = (GlobalActivity)getApplication();
        
        Intent i = new Intent().setClass(this, MapRouteActivity.class);
        mTabHost.addTab(mTabHost.newTabSpec("map_tab").setIndicator(this.getString(R.string.map)).setContent(i));
        mTabHost.getTabWidget().getChildAt(0).getLayoutParams().height = 100;
        
        i = new Intent().setClass(this, EmergencyEventListActivity.class);
        mTabHost.addTab(mTabHost.newTabSpec("list_tab").setIndicator(this.getString(R.string.emergencies)).setContent(i));
        mTabHost.getTabWidget().getChildAt(1).getLayoutParams().height = 100;
        
        i = new Intent().setClass(this, SettingsActivity.class);
        mTabHost.addTab(mTabHost.newTabSpec("prefs_tab").setIndicator(this.getString(R.string.preferences)).setContent(i));
        mTabHost.getTabWidget().getChildAt(2).getLayoutParams().height = 100;
        
        
        //on launch, sets the current tab to the map
        mTabHost.setCurrentTab(0);
        
        Intent intent = new Intent(this, WifiBroadcastService.class);
        startService(intent);

        //Check to see if TTS engine is installed
        //TODO have the check occur in the sharedPreferencesChanged listener in MainActivity
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, CHECK_CODE);
    }
    
    //When there is an emergency received, change the tab to the map tab and pass it the intent
	@Override
	public void onNewIntent(Intent intent){
		//Both the map and the list will get this intent

        Intent i = new Intent ("EMERGENCY.RECEIVED");
        
        if(intent.getExtras()!=null) {
        	i.putExtras(intent.getExtras());
        }
        sendBroadcast(i);
        
        //opens the map
    	mTabHost.setCurrentTab(0);
	}

    //NEW: Speaker Stuff

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CHECK_CODE){
            if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){
                Log.d("ACTIVITY", "Create speaker");
                if (g == null)
                {
                    Log.d("ACTIVITY","WTF");
                }
                else
                {
                    g.speaker = new SpeakerActivity(this);
                    g.speaker.speak("Testing one two three");
                }

            }else {
                Log.d("ACTIVITY","Req install");
                Intent install = new Intent();
                install.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(install);
            }
        }
    }
}
