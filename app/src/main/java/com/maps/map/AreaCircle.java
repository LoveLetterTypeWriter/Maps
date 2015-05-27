package com.maps.map;

import com.google.android.gms.maps.model.LatLng;

/**
 * AreaCircle is used whenever an event uses a circle rather than a
 * zip code for denoting the area that it is affects. Each AreaCircle
 * must have a center and a radius. It may also have an address associated
 * with it if the geocoder in DataBaseHelper finds one that corresponds to
 * the lat/lon.
 * 
 */


public class AreaCircle implements AreaInfo {
	
	double radius;
	LatLng point;
	String address;
	
	//The address is optional
	
	public AreaCircle(double lat, double lon, double radius, String address){
		this.radius = radius;
		point = new LatLng(lat, lon);
		if(address!=null)
			this.address = address;	
	}

	@Override
	public String getType() {
		return "circle";
	}

	@Override
	public double getRadius() {
		return radius;
	}

	@Override
	public LatLng getCenter() {
		return point;
	}
	
	@Override
	public String toString(){
		return getType() + " " + getCenter().latitude + " " + 
				getCenter().longitude + " " + getRadius() + " " + getAddress();
	}
	
	@Override
	public String getAddress(){
		return address;
	}

}
