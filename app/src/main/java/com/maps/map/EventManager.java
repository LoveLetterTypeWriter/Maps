package com.maps.map;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.database.SQLException;


/**
 * This is a singleton class that the rest of the classes use to
 * get the most up to date list of the events.
 * This class provides an extra abstraction for the database and
 * manages the current lists of events as well.
 * 
 * A singleton class may be better written using some feature
 * of Java that I'm unfamiliar with.
 * 
 */

public class EventManager {

	ArrayList<EmergencyEvent> current;
	ArrayList<EmergencyEvent> future;
	ArrayList<EmergencyEvent> past;
	ArrayList<EmergencyEvent> all;
	
	Context context;
	
	private static EventManager instance = null;
	
	DataBaseHelper myDbHelper;
	
	protected EventManager(Context context) {
	   this.context = context;
	   this.myDbHelper = new DataBaseHelper(context);
	   refresh();
    }
   
   /* This ensures that there is only one EventManger instance.
    * This should be called by other classes instead of the
    * EventManager constructor.
    */
	
   public static EventManager getInstance(Context context) {
	   if(instance == null) {
    		instance = new EventManager(context);
    	}
    	return instance;
   }
   
    //This gets the most up to date info about the events from the DB.
    
   public void refresh(){
	   myDbHelper.close();
	   myDbHelper = new DataBaseHelper(context);
   
	   try {
		   myDbHelper.createDataBase();
	   } catch (IOException ioe) {
		   throw new Error("Unable to create database");
	   }

	   try {
		   myDbHelper.openDataBase();
		   myDbHelper.updateVersion();
	   } catch(SQLException sqle) {
		   throw sqle;
	   }
	
	   current = myDbHelper.getAllValidEvents();
	   future = myDbHelper.getAllFutureEvents();
	   past = myDbHelper.getAllPastEvents();
   
	   all = new ArrayList<EmergencyEvent>();
	   all.addAll(current);
	   all.addAll(future);
	   all.addAll(past);
   }
   
   public ArrayList<EmergencyEvent> getCurrent(){
	   return current;
   }
   
   public ArrayList<EmergencyEvent> getFuture(){
	   return future;
   }
   
   public ArrayList<EmergencyEvent> getPast(){
	   return past;
   }
   
   public ArrayList<EmergencyEvent> getAll(){
	   return all;
   }
   
   /* The broker/server can send updates to events
    * to correct details that may be wrong or have changed.
    * The EventManager sends them to the database and then
    * updates the UI. 
    */
   
   public void update(EmergencyEvent e){
	   myDbHelper.updateEvent(e);
	   refresh();
	   return;
   }
   
   /* This sends an event to the database. Usually this
    * results in it being added if it's never been seen before.
    */
   
   public EmergencyEvent queryEvent(String q){
	   EmergencyEvent e = myDbHelper.queryEvent(q);
	   refresh();
	   return e;
   }
   
   //Finds a specific event from the database.
   
   public EmergencyEvent getSpecificEvent(String id){
	   return myDbHelper.getSpecificEvent(id);
   }
   
   // Deletes an event from the database
   
   public void deleteEvent(EmergencyEvent e){
	   myDbHelper.deleteEvent(e.getId());
	   refresh();
	   return;
   }
   
   /* Check to see if this phone number is in our database
    * as someone who is allowed to send out cell broadcasts.
    * Right now this is just set to the phone number 15555215556
    * 
    * See SMSReceiver for the reasons why we're just using
    * SMS instead of a cell broadcast. This should be changed in
    * future versions if it's going to go into production at all.
    */
   public boolean checkNumber(long number){
	   return myDbHelper.isNumberVerified(number);
   }

}
