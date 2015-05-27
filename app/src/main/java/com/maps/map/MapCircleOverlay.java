package com.maps.map;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import android.graphics.Color;

/**
 * This creates a circle for an event displayed on the map.
 * Advisory is green.
 * Watch is yellow.
 * Warning is red.
 *
 */

public class MapCircleOverlay {

	private CircleOptions circleOptions;

	public static final double MILES_TO_METERS = 1609.34;

	public MapCircleOverlay(LatLng point, double radius, String event_level, float zoom) {
		
		//radius is given to us in miles, but CircleOptions expects meters
	    circleOptions = new CircleOptions().center(point).radius(radius * MILES_TO_METERS);
	
	    //Change the color of the circles depending on the event level
	    if(event_level.equals("ADV")) {
	    	circleOptions.strokeColor(Color.argb(128, 0, 128, 0));
	    	circleOptions.fillColor(Color.argb(64, 0, 128, 0));
	    } else if (event_level.equals("WCH")){
	    	circleOptions.strokeColor(Color.argb(128, 255, 2555, 0));
	    	circleOptions.fillColor(Color.argb(64, 255, 255, 0));
	    } else {
	    	circleOptions.strokeColor(Color.argb(128, 255, 0, 0));
	    	circleOptions.fillColor(Color.argb(64, 255, 0, 0));
	    }
	    
	    circleOptions.strokeWidth(2);
	}

	public CircleOptions getCircleOverlay(){
		return circleOptions;
	}
}