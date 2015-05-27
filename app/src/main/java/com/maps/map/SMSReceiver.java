package com.maps.map;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

/**
 * This class listens for an SMS message and passes it into the program if
 * it comes from an approved sender (ie a valid source for broadcast messages).
 * 
 * Why are we using SMS for this instead of broadcast messages?
 * Well, there is pretty much no information out there about how to actually use
 * cell broadcasts. So, it's quite unclear how to program something like that!
 * 
 * This is as close as I could get to something relevant:
 * http://stackoverflow.com/questions/7118378/how-to-get-cell-broadcast-message
 * and
 * http://source-android.frandroid.com/frameworks/base/telephony/java/android/telephony/SmsCbMessage.java
 * 
 * It is also unclear how one would test such a thing since it's not like you can just get 
 * a cell broadcast tower.
 * 
 * But there are ways to test out SMS messages. Two main ways come to mind:
 * 
 * 1) use telnet or some IDEs. For example, Eclipse has "emulator control" which can be used for this purpose.
 * If you end up using telnet, this link may be helpful:
 * http://stackoverflow.com/questions/4325669/sending-and-receiving-text-using-android-emulator
 * Make sure the sender phone number is 15555215556.
 * An example of what you might send is:
 * sms send 15555215556 15 1 1 3 BZW 1 1 1 1 10-25-2014 23:23 10-28-2014 23:44 3 33.6 -117.8 5
 * 
 * 2) use another device (emulated or real). If you do that, then uncomment out the "if(msgs[i]..." line
 * and do comment out the "if(em.checkNumber(..." line. This will allow you to send a message from another device
 * and bypass the whole checking to see if it's from an approved sender.
 * 
 */

public class SMSReceiver extends BroadcastReceiver{
	
	EventManager em = EventManager.getInstance(null);
	
    public void onReceive(Context context, Intent intent)                                                                                                         
    {

        Bundle bundle = intent.getExtras();        
        SmsMessage[] msgs = null;
        String str = "";            
        if (bundle != null)
        {

            //retrieve the SMS message received
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];            
            for (int i=0; i<msgs.length; i++){
                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                
                /* NOTE: I have tried to take out a lot of commented code, but the below line may be useful
                 * for anyone who inherits this code.
                 */
                
                //if(msgs[i].getOriginatingAddress().equals("15555215556")) {
                
                /*
                 * Only process this if the number is in the database, otherwise pass the message to the OS.
                 * 
                 * Note: Originally this code bypassed the inbox. It would only get sent to the OS if they
                 * weren't from an approved sender for this application.
                 * 
                 * Google no longer lets you bypass the inbox. So, these texts will be seen by the user
                 * regardless.
                 */
                if(em.checkNumber(Long.parseLong(msgs[i].getOriginatingAddress()))){
                	
            		abortBroadcast();
            		str = "";
            		str += msgs[i].getOriginatingAddress() + " ";
            		str += msgs[i].getTimestampMillis() + " ";
	                str += msgs[i].getMessageBody().toString();
	                Intent in = new Intent();
	                in.putExtra("message", str);
	                
	                //this will open the application if it's not open already
	                in.setClass(context, TabMainActivity.class);
	                try {
	    				PendingIntent.getActivity(context, (int) System.currentTimeMillis(), in, 0).send();
	    			} catch (CanceledException e) {
	    				// Auto-generated catch block
	    				e.printStackTrace();
	    			}
                }
            }
        }
        
    }
}