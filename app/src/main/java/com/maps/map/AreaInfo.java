package com.maps.map;

import com.google.android.gms.maps.model.LatLng;

/**
 * This is just an interface for the different types of area
 * data we have to store. Events can either be restricted to a
 * zip code or a circle.
 * 
 */

public interface AreaInfo {
	String getType();
	double getRadius();
	LatLng getCenter();
	String toString();
	String getAddress();
}
