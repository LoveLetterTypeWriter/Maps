package com.maps.map;


import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;


/**
 * Created by 1089C on 5/13/2015.
 */
public class SpeakerActivity extends Activity implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private boolean ready = false;

    public SpeakerActivity(Context context){
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS){
            tts.setLanguage(Locale.US);
            ready = true;
            Log.d("SPEAK","Init success");
        }
        else
        {
            ready = false;
        }
    }

    public void speak(String text){

        // Speak only if the TTS is ready
        // and the user has allowed speech
        if(ready) {
            Log.d("SPEAK","Starting speech with "+text);
            HashMap<String, String> hash = new HashMap<String,String>();
            hash.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                    String.valueOf(AudioManager.STREAM_NOTIFICATION));
            tts.speak(text, TextToSpeech.QUEUE_ADD, hash);
        }
    }

    public void pause(int duration){
        tts.playSilence(duration, TextToSpeech.QUEUE_ADD, null);
    }

    // Free up resources
    public void destroy(){
        tts.shutdown();
    }
}
