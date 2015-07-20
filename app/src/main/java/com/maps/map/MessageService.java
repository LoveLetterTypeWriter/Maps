package com.maps.map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by 1089C on 6/4/2015.
 */
public class MessageService extends Service {

    //TODO stop this service when the user specifies for the service to be turned off

    EmergencyEvent latest_event;
    LatLng latest_point;
    EventManager em;
    ResultReceiver mReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        em = EventManager.getInstance(this); //TODO have the eventmanager be created from the global variable
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //initialize receiver
        mReceiver = intent.getParcelableExtra("receiver");
        // Whenever we start this program, we want to make sure that we initialize the map properly so we send this Intent.
        registerReceiver(eventReceiver, new IntentFilter("EMERGENCY.RECEIVED"));
        Intent i = new Intent ("EMERGENCY.RECEIVED");
        sendBroadcast(i);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(eventReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Listens for event messages
    private BroadcastReceiver eventReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle b = intent.getExtras();

            if(b!=null) {
                if(b.getString("message")!=null) {
                    latest_event = em.queryEvent(b.getString("message"));

					/*
					 * em.queryEvent() should always return an Emergency Event
					 * unless there was something wrong with the message and it
					 * did not parse correctly. Even in the case of a deletion,
					 * it should still return an event.
					 */

                    if(latest_event!=null){
                        String message = b.getString("message");
                        String[] messagesplit = message.split(" ");
                        //get the last word in the message which should be the TTL
                        int ttl = Integer.valueOf(messagesplit[messagesplit.length-1]);

		    			/* If the TTL is greater than 0, then we should try to send this
		    			 * message onto any other devices in range.
		    			 */

                        if(ttl>0) {
                            Intent i = new Intent ("EMERGENCY.SEND");
                            i.putExtra("message", b.getString("message"));

		    				/* this is messy, but it replaces the TTL in the message with
		    				 * one that is one less than the original
		    				 */

                            ttl = ttl-1;
                            message = message.replaceAll("\\d$", Integer.toString(ttl));
                            i.putExtra("message", message);
                            sendBroadcast(i);
                        }

                        //notify the user
                        makeNotification(latest_event);


                        //if an event just got canceled, just refresh the map
                        if(!latest_event.getType().equals("cancel")) {
                            mReceiver.send(2,null);
                        } else { //otherwise, zoom in on the new/updated event on the map
                            Bundle pointBundle = new Bundle();
                            latest_point = latest_event.getPoint();
                            b.putDouble("lat",latest_point.latitude);
                            b.putDouble("long",latest_point.longitude);
                            mReceiver.send(1,pointBundle);
                        }
                    }

                } else if(b.getString("id")!=null) { //this happens when you click on a notification--you get sent to that particular event
                    latest_event = em.getSpecificEvent(b.getString("id"));
                    if(latest_event!=null) {
                        Bundle pointBundle = new Bundle();
                        latest_point = latest_event.getPoint();
                        b.putDouble("lat",latest_point.latitude);
                        b.putDouble("long",latest_point.longitude);
                        mReceiver.send(1, pointBundle);
                    } else { //the event has been deleted either by the user or by a "cancel" message, so there is no event to show anymore
                        Toast.makeText(context, context.getString(R.string.Deleted), Toast.LENGTH_LONG).show();
                    }
                }

            } else { //if we were sent an Intent with no extras, we can assume that we just need to refresh the map
                mReceiver.send(2, null);
            }
        }
    };

    //Notifies the user with a specific sound and vibration according to specifications, as well as a text notification
    public void makeNotification(EmergencyEvent e) {

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        Intent notificationIntent = new Intent(this, TabMainActivity.class);
        notificationIntent.putExtra("id", "" + e.getId());
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                (int)System.currentTimeMillis(), notificationIntent, 0);
        String output = "";
        String type = "";

        if(latest_event.getType().equals("update") || latest_event.getType().equals("addition")) {
            type += this.getString(R.string.updated) + " ";
        } else if(latest_event.getType().equals("cancel")){
            type += this.getString(R.string.Cancelled) + " ";
        }else{
            type += this.getString(R.string.n) + " ";
        }

        Notification n;

        //show the correct notification icon depending on the event level

        if(e.getEventLevel().equals("ADV"))
            //n = new Notification(R.drawable.green_drop,
            //		type + this.getString(R.string.emergency), System.currentTimeMillis());
            n = new Notification.Builder(this)
                    .setContentText(this.getString(R.string.emergency))
                    .setSmallIcon(R.drawable.green_drop)
                    .setWhen(System.currentTimeMillis())
                    .build();
        else if(e.getEventLevel().equals("WCH"))
            //n = new Notification(R.drawable.yellow_drop,
            //		type + this.getString(R.string.emergency), System.currentTimeMillis());
            n = new Notification.Builder(this)
                    .setContentText(this.getString(R.string.emergency))
                    .setSmallIcon(R.drawable.yellow_drop)
                    .setWhen(System.currentTimeMillis())
                    .build();
        else
            //n = new Notification(R.drawable.red_drop,
            //	type + this.getString(R.string.emergency), System.currentTimeMillis());
            n = new Notification.Builder(this)
                    .setContentText(this.getString(R.string.emergency))
                    .setSmallIcon(R.drawable.red_drop)
                    .setWhen(System.currentTimeMillis())
                    .build();

        output += type + this.getString(R.string.emergency) + ": " + latest_event.getCategory()
                + " " + latest_event.getAddress();

        n.setLatestEventInfo(this, e.getCategory(), output, contentIntent);

        //make the EAS noise
        n.sound = Uri.parse("android.resource://com.maps.map/" + R.raw.eas);

        n.flags = Notification.FLAG_AUTO_CANCEL;

        //followed the vibration information in the CMAS spec
        long[] pattern = {
                0, 2000, 500, 1000, 500, 1000,
                500, 2000, 500, 1000, 500, 1000,
                500, 2000, 500, 1000, 500, 1000};
        n.vibrate = pattern;
        mNotificationManager.notify((int)System.currentTimeMillis(), n);
    }
}
