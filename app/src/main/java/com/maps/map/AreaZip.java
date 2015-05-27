package com.maps.map;

import java.io.IOException;
import java.util.List;
import android.location.Address;
import android.location.Geocoder;
import com.google.android.gms.maps.model.LatLng;

/**
 * AreaZip is the class that stores data for
 * events that are restricted to a specific zip code.
 * See note below for why the radius is currently
 * hardcoded to 3.7. This should probably be changed
 * if this ever goes into production.
 *
 */

public class AreaZip implements AreaInfo {
	
	LatLng point;
	int zip;
	
	public AreaZip(int zip, Geocoder geocoder){
		
		double lat, lon;
		
		this.zip = zip;
        
		//try to translate this zip code into a location
        try {
   		  List<Address> addresses = geocoder.getFromLocationName("" + zip, 1);
   		  if (addresses != null && !addresses.isEmpty()) {
   		  	Address a = addresses.get(0);
   		    lat = a.getLatitude();
   		    lon = a.getLongitude();
   		    point = new LatLng(lat, lon);
   		  }
        } catch (IOException e) {
     		  // handle exception
     	}
	}

	@Override
	public String getType() {
		return "zip";
	}

	/* yes actual zip codes vary wildly in size so this is NOT accurate
	the only FREE information I could get about the average size of a zip
	code is for New York state: http://www.ncbi.nlm.nih.gov/pmc/articles/PMC1762013/
	but if you do the math it comes out to a radius of 3.7 miles
	which is a little silly anyway considering that zip codes have all
	sorts of irregular shapes */
	
	@Override
	public double getRadius() {
		return 3.7;
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
		return "" + zip;
	}

}
