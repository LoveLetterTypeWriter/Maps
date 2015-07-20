package com.maps.map;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.MapView;

//New imports
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import android.support.v4.app.FragmentActivity;

//@SuppressWarnings( "deprecation" )

/**
 * This is the class where the majority of the work takes place.
 * Ultimately, some of this work should be off loaded to other classes and/or
 * put into a Service.
 * 
 * This code was written by someone who has very little experience with Android
 * programming and it was originally written using the v1 Maps API. It's been
 * "updated" to work with v2, but there's still a lot of deprecated stuff.
 * 
 * This class updates the map, passes messages into the database, and
 * also sends messages over Wifi Direct if necessary.
 *
 */

public class MapRouteActivity extends FragmentActivity implements
	GoogleApiClient.ConnectionCallbacks,
	GoogleApiClient.OnConnectionFailedListener,
	LocationListener,
	OnMarkerClickListener {
	
		protected Drawable person, advisory, watch, warning;
		
		protected LocationManager locationManager;

        MapView mapView;
        GoogleMap map; 
        Geocoder geocoder;
        LatLng curr_location;
        SharedPreferences sharedPrefs;
		GoogleApiClient mLocationClient;
        EventManager em;
		Intent serviceIntent;
		Timer myTimer; //TODO this is just a test timer, remove
        

        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                
                setContentView(R.layout.maps);
                
                //Sadly, I don't remember why this was put in here.
                
                if (android.os.Build.VERSION.SDK_INT > 9) {
                	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                	StrictMode.setThreadPolicy(policy); 
                	}
                
                //This won't work properly if Google Play is not available.
                
                // Getting Google Play availability status
                int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
                // Showing status
                if(status!=ConnectionResult.SUCCESS){ // Google Play Services are not available
                    int requestCode = 10;
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
                    dialog.show();
         
                }else { // Google Play Services are available
         
                    // Getting reference to the SupportMapFragment of activity_main.xml
                    SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
         
                    // Getting GoogleMap object from the fragment
                    map = fm.getMap();
                    map.setOnMarkerClickListener(this);
         
                    // Enabling MyLocation Layer of Google Map
                    map.setMyLocationEnabled(true);
                }

				//Set up the location client
                mLocationClient =new GoogleApiClient.Builder(this)
						.addApi(LocationServices.API)
						.addConnectionCallbacks(this)
						.addOnConnectionFailedListener(this)
						.build();


                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1,1,this);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1,1,this);

                
                sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                sharedPrefs.registerOnSharedPreferenceChangeListener(prefs);
                
                geocoder = new Geocoder(this, Locale.US);
	         	
	         	em = EventManager.getInstance(this); //TODO have the eventmanager be created from the global variable
	         	
	         	//see handler code further down for an explanation
				mReceiver.send(0,null);
                map.setTrafficEnabled(true);
                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

				//resultreceiver initialization
				serviceIntent = new Intent(this, MessageService.class);
				serviceIntent.putExtra("receiver",mReceiver);
				startService(serviceIntent);
                
        	 	//every 10 minutes refresh the map and list
                
                //Timer myTimer = new Timer();
        	    //myTimer.schedule(new TimerTask() {
        	    //    @Override
        	    //    public void run() {
        	    //    	Intent i = new Intent ("EMERGENCY.RECEIVED");
        	    //        sendBroadcast(i);
        	    //    }
        	    //}, 0, 600000);
        	    
                /* I am leaving this commented code in here because you may find it useful for testing
                 * with a physical device. You can use this code to have the device receive an alert message
                 * every minute (or whatever amount of time you choose). If you use this code you should probably
                 * comment out the Timer... 0, 6000000); bit above.
                 * 
                 * The other device that you test with (at least, I'm assuming you may want to use this to test
                 * out Wifi Direct, because that's what I did), should have this code commented out. Then you can
                 * have device 1 receive an alert every minute and make sure that device 2 gets it through Wifi Direct.
                 * 
                 */
                
        	    myTimer = new Timer();
        	    myTimer.schedule(new TimerTask() {
        	        @Override
        	        public void run() {
        	        	Intent i = new Intent ("EMERGENCY.RECEIVED");
        	        	String str = "15555215557 ";
                		str += System.currentTimeMillis() + " ";
                        str += "15 1 1 3 BZW 1 1 1 1 10-25-2014 23:23 10-28-2014 23:44 3 33.6 -117.8 5 3";
        	        	i.putExtra("message", str);
        	            sendBroadcast(i);
        	        }
        	    }, 0, 20000);
        }        
        


        @Override
    	public void onStart() {
    		super.onStart();
			//TODO timer testing, remove later
			myTimer = new Timer();
			myTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					Intent i = new Intent("EMERGENCY.RECEIVED");
					String str = "15555215557 ";
					str += System.currentTimeMillis() + " ";
					str += "15 1 1 3 BZW 1 1 1 1 10-25-2014 23:23 10-28-2014 23:44 3 33.6 -117.8 5 3";
					i.putExtra("message", str);
					sendBroadcast(i);
				}
			}, 0, 20000);
    		mLocationClient.connect();
    	}
    	
    	@Override 
    	public void onStop() {
			mLocationClient.disconnect();
			//TODO timer testing, remove later
			if (myTimer != null)
			{
				myTimer.cancel();
				myTimer = null;
			}
    	  	super.onStop();
    	}

		@Override
		public void onDestroy() {
			stopService(serviceIntent);
			//TODO timer testing, remove later
			if (myTimer != null)
			{
				myTimer.cancel();
				myTimer = null;
			}
			super.onDestroy();
		}
        
        //If the user clicks on a marker, then we should bring up the event details.
        
        @Override
        public boolean onMarkerClick(final Marker marker) { 
        	
        	if(marker.getSnippet()!=null) {
	    		Intent i = new Intent();
	    		i.setClass(this, EventDetailsActivity.class);
	    		i.putExtra("id", "" + marker.getSnippet());
	    		startActivity(i);
	    		return true;
        	}
        	return false;
        }
        
        /*
         * Whenever our location has changed, we should send a message to our handler so it centers on the new location.
         * If you want to test this out with an emulator, follow the instructions here:
         * http://stackoverflow.com/questions/2279647/how-to-emulate-gps-location-in-the-android-emulator
         * An example of what you might want to send is:
         * geo fix -117.8 33.6
         * 
         * Alternatively, if you're using Eclipse you can use the emulator control to set the location.
         */
        
        @Override
        public void onLocationChanged(Location location) {
            curr_location = new LatLng(location.getLatitude(), location.getLongitude());
			mReceiver.send(0,null);
		}

	//This updates the map based on messages that are sent
	private ResultReceiver mReceiver = new ResultReceiver(null) {

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			em.refresh();

			//this Intent will tell the event list to also refresh
			Intent j = new Intent ("REFRESH.RECEIVED");
			sendBroadcast(j);

			map.clear();

			switch (resultCode){
				//if the message is type 0 just zoom in on the person's current location
				case 0:

					if(curr_location != null) {
						map.moveCamera(CameraUpdateFactory.newLatLngZoom(curr_location, 12));
					}
					break;

				//if the message is type 1 just zoom in on the most recently added event
				case 1:
					LatLng point = new LatLng(resultData.getDouble("lat"),resultData.getDouble("long"));
					if (point != null)
					{
						map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 12));
						Log.d("OKAY", "point is alive we won");
					}
					else
					{
						Log.d("OKAY","point died rip");
					}
					break;
			}

			//add all the markers and circles for the items
			ArrayList<EmergencyEvent> e = em.getAll();

			//only show events that are supposed to be displayed (either because they are current or the user selected to show them)
			for(int i = 0; i < e.size(); i++){
				EmergencyEvent curr = e.get(i);
				if(!curr.isDisplayed()) {
					continue;
				}
				LatLng p = curr.getPoint();

				CircleOptions oc = (new MapCircleOverlay(p, curr.getRadius(), curr.getEventLevel(), map.getCameraPosition().zoom)).getCircleOverlay();

				if(curr.getEventLevel().equals("ADV")) {
					map.addMarker(new MarkerOptions()
							.position(p)
							.title(curr.getCategory())
							.snippet(""+curr.getId())
							.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
				} else if (curr.getEventLevel().equals("WCH")) {
					map.addMarker(new MarkerOptions()
							.position(p)
							.title(curr.getCategory())
							.snippet(""+curr.getId())
							.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
				} else {
					map.addMarker(new MarkerOptions()
							.position(p)
							.title(curr.getCategory())
							.snippet(""+curr.getId())
							.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
				}
				map.addCircle(oc);
			}

			if(curr_location != null) {
				map.addMarker(new MarkerOptions()
						.position(curr_location)
						.title("You")
						.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
			}
		}

	};

	
	//update the map if the map mode or traffic mode gets changed
	SharedPreferences.OnSharedPreferenceChangeListener prefs = 
	new SharedPreferences.OnSharedPreferenceChangeListener() {
		
		  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			    if(key.equals("map_mode")){
			    	if(prefs.getBoolean("map_mode", true)) {
			    		map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			    	} else {
			    		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			    	}
			    }
			    if (key.equals("traffic_mode")){
		    		map.setTrafficEnabled(prefs.getBoolean("traffic_mode", true));
		    	}
		  }
	};

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		
	}

	@Override
	public void onConnected(Bundle arg0) {
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		
	}

	@Override
	public void onConnectionSuspended(int i) {

	}

}
