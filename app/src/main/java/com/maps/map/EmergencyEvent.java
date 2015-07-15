package com.maps.map;

import java.util.Date;
import android.content.Context;
import com.google.android.gms.maps.model.LatLng;

/**
 * This holds all the information about a specific emergency.
 * This does use deprecated code, so if you're updating this application
 * you may want to change this.
 * 
 */

//@SuppressWarnings( "deprecation" )

public class EmergencyEvent {

	private int id;
	private String status;
	private String msg_type;
	private String sender;
	private Date received;
	private String category;
	private String event_level;
	private String response_type;
	private String urgency;
	private String severity;
	private String certainty;
	private Date effective;
	private Date expires;
	private String extra_info;
	private AreaInfo area_info;
	private String address;
	private Context context;
	private Boolean display;
	private String speaker_string;
	private String certainty_color;
	private String severity_color;
	private String status_color;
	private String urgency_color;
	
	public EmergencyEvent(int id, String sender, Date received, String status, String msg_type,
			String category, String event_level, String response_type, String urgency, 
			String severity, String certainty, Date effective,
			Date expires, AreaInfo area_info, Context context, Boolean display, String speaker_string,
			String certainty_color, String severity_color, String status_color, String urgency_color){
		
		this.id = id;
		this.status = status;
		this.sender = sender;
		this.received = received;
		this.msg_type = msg_type;
		this.category = category;
		this.event_level = event_level;
		this.response_type = response_type;
		this.urgency = urgency;
		this.severity = severity;
		this.certainty = certainty;
		this.effective = effective;
		this.expires = expires;
		this.area_info = area_info;
		this.address = area_info.getAddress();
		this.display = display;
		this.context = context;
		this.speaker_string = speaker_string;
		this.certainty_color = certainty_color;
		this.severity_color = severity_color;
		this.status_color = status_color;
		this.urgency_color = urgency_color;
	}
	
	public void addExtraInfo(String extra_info){
		this.extra_info = extra_info;
	}
	
	public String toString(){
		String output = category + "\n" + getDates();
		if(address != null && address.length() > 0)if(address!=null)
			output += "\n" + address;
		return output;
	}
	
	public String toHtmlString(boolean cctext_setting){
		String long_event_level;
		String long_event_level_color;
		if(event_level.equals("ADV")) {
			long_event_level = context.getString(R.string.advisory);
			long_event_level_color = "#66FF33";
		} else if (event_level.equals("WCH")) {
			long_event_level = context.getString(R.string.watch);
			long_event_level_color = "#FFCC00";
		} else {
			long_event_level = context.getString(R.string.warning);
			long_event_level_color = "#FF0000";
		}

		String certainty_field_color_tag = "<font>";
		String event_level_color_tag = "<font>";
		String severity_field_color_tag = "<font>";
		String status_field_color_tag = "<font>";
		String urgency_field_color_tag = "<font>";

		if (cctext_setting)
		{
			certainty_field_color_tag = "<font color=\"" + certainty_color + "\">";
			event_level_color_tag = "<font color=\"" + long_event_level_color + "\">";
			severity_field_color_tag = "<font color=\"" + severity_color + "\">";
			status_field_color_tag = "<font color=\"" + status_color + "\">";
			urgency_field_color_tag = "<font color=\"" + urgency_color + "\">";
		}

		String event = "<b>" + context.getString(R.string.id) + ":</b> <font>" + id
				+ "</font><br><b>" + context.getString(R.string.status) + ":</b> " + status_field_color_tag + status
				+ "</font><br><b>" + context.getString(R.string.sender) + ":</b> <font>" + sender
				+ "</font><br><b>" + context.getString(R.string.received) + ":</b> <font>" + received.toLocaleString()
				+ "</font><br><b>" + context.getString(R.string.category) + ":</b> <font>" + category
				+ "</font><br><b>" + context.getString(R.string.event_level) + ":</b> " + event_level_color_tag + long_event_level
				+ "</font><br><b>" + context.getString(R.string.response_type) + ":</b> <font>" + response_type
				+ "</font><br><b>" + context.getString(R.string.urgency) + ":</b> " + urgency_field_color_tag + urgency
				+ "</font><br><b>" + context.getString(R.string.severity) + ":</b> " + severity_field_color_tag + severity
				+ "</font><br><b>" + context.getString(R.string.certainty) + ":</b> " + certainty_field_color_tag + certainty
				+ "</font><br><b>" + context.getString(R.string.effective) + ":</b> <font>" + effective.toLocaleString()
				+ "</font><br><b>" + context.getString(R.string.expires) + ":</b> <font>" + expires.toLocaleString() + "</font>";
		if(extra_info != null && extra_info.length() > 0)
			event += "<br><b>" + context.getString(R.string.info) + ":</b> " + extra_info;
		if(address != null && address.length() > 0)
			event += "<br><b>" + context.getString(R.string.address) + ":</b> " + address;
		return event;
	}
	
	public String getDates(){
		return effective.toLocaleString() + " - " + expires.toLocaleString();
	}
	
	public String getAreaType(){
		return area_info.getType();
	}
	
	public LatLng getPoint(){
		return area_info.getCenter();
	}
	
	public double getRadius(){
		return area_info.getRadius();
	}
	
	public int getId(){
		return id;
	}
	
	public String getStatus(){
		return status;
	}
	
	public String getCategory(){
		return category;
	}
	
	public String getEventLevel(){
		return event_level;
	}
	
	public String getResponseType(){
		return response_type;
	}
	
	public String getUrgency(){
		return urgency;
	}
	
	public String getSeverity(){
		return severity;
	}
	
	public String getCertainty(){
		return certainty;
	}
	
	public Date getEffective(){
		return effective;
	}
	
	public Date getExpires(){
		return expires;
	}
	
	public String getExtraInfo(){
		if(extra_info != null && extra_info.length() > 0)
			return extra_info;
		else
			return "";
	}
	
	public String getAddress(){
		if(address != null && address.length() > 0)
			return address;
		else
			return "";
	}
	
	public Boolean getDisplay(){
		return display;
	}
	
	public boolean isDisplayed(){
		/* The user can set whether an event is displayed in the UI.
		 * however, if they have not set this for this particular event, 
		 * then by default we should only show it if it is an event that is
		 * happening currently. Events that have already happened or are
		 * scheduled for the future will not be shown.
		 */
		if(display==null)
			return isCurrent();
		return display;
	}
	
	public void setDisplay(Boolean display){
		this.display = display;
	}
	
	public boolean isCurrent(){
		Date current = new Date();
		return current.after(effective) && current.before(expires);
	}

	public String getSender() {
		return sender;
	}

	public Date getReceived() {
		return received;
	}

	public String getType() {
		return msg_type;
	}

	public void setReceived(Date received) {
		this.received = received;
	}

	public void setMsgType(String msg_type) {
		this.msg_type = msg_type;
		
	}

    //NEW: Speaker stuff
	public String getNotificationSpeech() {
        return speaker_string;
    }

	public String getSelectionSpeech() {
		return speaker_string + response_type;
	}

	public String getCertaintyColor() {return certainty_color;}

	public String getSeverityColor() {return severity_color;}

	public String getStatusColor() {return status_color;}

	public String getUrgencyColor() {return urgency_color;}
}
