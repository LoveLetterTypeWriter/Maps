package com.maps.map;

import java.util.ArrayList;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


/**
 * The EmergencyEventListActivity sets up the list that the user
 * sees when they click on the emergencies tab. This class divides
 * the events into current, past, and future based on when they'll
 * be active. It uses SeparatedListActivity to put separators in
 * the list--there may be another way to do this, but when this 
 * was written there was not.
 * 
 * This class also defines the behavior for when an item is selected
 * either through a normal click (in which case more info about the event
 * comes up) or a long click (in which case a context menu comes up).
 *
 */

public class EmergencyEventListActivity extends ListActivity {
	
	DataBaseHelper myDbHelper;
	SharedPreferences sharedPrefs;
	EventManager em;
	String langTo;
	SeparatedListAdapter adapter; 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  
	  registerReceiver(myReceiver, new IntentFilter("REFRESH.RECEIVED"));
	  em = EventManager.getInstance(this);
	   
	  setUpAdapter();
	  ListView lv = getListView();
	  lv.setTextFilterEnabled(true);

	  //if we click on an item in the list, make a popup with more info
	  lv.setOnItemClickListener(new OnItemClickListener() {
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	      makePopup(((EmergencyEvent)parent.getItemAtPosition(position)).getId());
	    }
	  });
	  
	  registerForContextMenu(lv);
	  
	}
	
	//Sends an intent to the EventDetailsActivity to open a popup for this event
	private void makePopup(int id){
		
		Intent i = new Intent();
		i.setClass(this, EventDetailsActivity.class);
		i.putExtra("id", "" + id);
		startActivity(i);

	}
	
	//adds events to the right parts of the list (current, future, past)
	private void setUpAdapter(){
    	
		ArrayList<EmergencyEvent> e = em.getCurrent();
		
	   	String[] events = new String[e.size()];
	   	
	   	for(int i = 0; i<e.size(); i++){
	   		events[i] = e.get(i).getId() + " " + e.get(i).getCategory() + " " + e.get(i).getAddress();
	   	}
	   	
	   	 adapter = new SeparatedListAdapter(this);
	   	 
	   	 adapter.addSection(this.getString(R.string.current_events),
	   			 new ArrayAdapter<EmergencyEvent>(this, R.layout.list_item, e));
	   	 
	   	e = em.getFuture();
	   	events = new String[e.size()];
	   	for(int i = 0; i<e.size(); i++){
	   		events[i] = e.get(i).getId() + " " + e.get(i).getCategory() + " " + e.get(i).getAddress();
	   	}
	   	adapter.addSection(this.getString(R.string.future_events),
	   			new ArrayAdapter<EmergencyEvent>(this, R.layout.list_item, e));
	   	
	   	e = em.getPast();
	   	events = new String[e.size()];
	   	for(int i = 0; i<e.size(); i++){
	   		events[i] = e.get(i).getId() + " " + e.get(i).getCategory() + " " + e.get(i).getAddress();
	   	}
	   	adapter.addSection(this.getString(R.string.past_events),
	   			new ArrayAdapter<EmergencyEvent>(this, R.layout.list_item, e));
	   	
	   	 setListAdapter(adapter);
	}
	
	//This checks to see if we need to refresh the UI--if so, we should make sure the list is up to date. 
	private BroadcastReceiver myReceiver = new BroadcastReceiver() {        
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	setUpAdapter();
	    }
	};
	
	/*
	 * If we click on a list item for a long time, it'll bring up a context menu.
	 * The list of items that are shown in the context menu are in arrays.xml
	 * 
	 * The context menu has the options:
	 * display on map
	 * delete
	 * cancel
	 * 
	 * We will only check "display on map" if the user has selected to show this
	 * event or the event is current.
	 * 
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
	    menu.setHeaderTitle(this.getString(R.string.HeaderTitle));
	    String[] menuItems = getResources().getStringArray(R.array.menu);
	    
	    for (int i = 0; i<menuItems.length; i++) {
	      menu.add(Menu.NONE, i, i, menuItems[i]);
	      
	      if(menuItems[i].equals(this.getString(R.string.DisplayonMap))) {
	    	  menu.getItem(i).setCheckable(true);
	    	  menu.getItem(i).setChecked(((EmergencyEvent)adapter.getItem(info.position)).isDisplayed());
	      }
	    }
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		 AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		 int menuItemIndex = item.getItemId();
		 String[] menuItems = getResources().getStringArray(R.array.menu);
		 String menuItemName = menuItems[menuItemIndex];
		 
		 EmergencyEvent e = (EmergencyEvent)adapter.getItem(info.position);
		 if(menuItemName.equals("Cancel")){
			 return true;
		 } else if(menuItemName.equals(this.getString(R.string.Delete))) {
			 em.deleteEvent(e);
		 } else if(menuItemName.equals(this.getString(R.string.DisplayonMap))){
			 e.setDisplay(!e.isDisplayed());
			 em.update(e);
		 }
	  
		  /* This will force the map and the list to update what they are displaying
		  so if you delete an item or change whether it's displayed on the map, it'll show it */
		  Intent i = new Intent ("EMERGENCY.RECEIVED");
	      sendBroadcast(i);
		  
		  return true;
		}
}
 