package com.maps.map;

import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.*;
import android.location.Address;
import android.location.Geocoder;
import android.content.ContentValues;
import android.content.Context;

import com.memetix.mst.language.Language;

/**
 * This class sets up the database that stores the emergency events and
 * stores the information that translates the messages into human-readable form.
 * For example, if the message contains "BZW", it'll convert it to the right thing.
 *
 * This class can be used to store data in English or Spanish. This is very much a
 * proof of concept. The Spanish translations were just made using GoogleTranslate
 * and are probably not accurate at all.
 *
 * In order to access the Spanish version of the app, you have to set the language
 * in Android's Settings. In KitKat, you do this by going to Settings and clicking
 * on Language & Input and then clicking on "Language".
 *
 * Note that the Spanish language database is separate from the English language
 * database, so events received while using one language will not show up in the
 * other language's database. This is something that should be changed eventually
 * and perhaps language should be set in the application settings, not the OS.
 *
 * By default, the application uses English.
 *
 * In order to understand how this all works, you may want to take a look at the
 * database files. They are under assets (English is emergency_maps and Spanish is
 * es_emergency_maps). In order to view the databases, you can use a program like this:
 * http://sqlitebrowser.org
 *
 */

public class DataBaseHelper extends SQLiteOpenHelper{

	private static String DB_NAME = "emergency_maps";
	private static String ES_DB_NAME = "es_emergency_maps";
	private String curr_db_name = "emergency_maps";

	//the current database version should be updated every time the schema is updated
	private static int DATABASE_VERSION = 11;

	private SQLiteDatabase myDataBase;

	private final Context myContext;

	private final Geocoder geocoder;

	//The constructor fills this in with Android's system path of your application database.
	private File DB_PATH;


	String locale;

	/**
	 * Constructor
	 * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
	 * @param context
	 */
	public DataBaseHelper(Context context) {
		super(context, DB_NAME, null, DATABASE_VERSION);

		locale = context.getResources().getConfiguration().locale.getDisplayName();
		myContext = context;

		//set this to Spanish if the locale set in the Android preferences is a Spanish speaking one
		if(locale.startsWith("es"))
			curr_db_name = ES_DB_NAME;

		DB_PATH = myContext.getDatabasePath(curr_db_name);
		this.geocoder = new Geocoder(myContext, Locale.US);
	}

	/**
	 * Creates a empty database on the system and rewrites it with your own database.
	 * */
	public void createDataBase() throws IOException{

		boolean dbExist = checkDataBase();

		if(dbExist){
			//do nothing - database already exists
		}else{

			//By calling this method an empty database will be created into the default system path
			//of your application. Then we will be able to overwrite that database with our database.
			this.getWritableDatabase();
			copyDataBase();
		}

	}

	/**
	 * Check if the database already exist to avoid re-copying the file each time you open the application.
	 * @return true if it exists, false if it doesn't
	 */
	public boolean checkDataBase(){

		SQLiteDatabase checkDB = null;

		String myPath;

		try{
			myPath = DB_PATH.getAbsolutePath();
			checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

		}catch(SQLiteCantOpenDatabaseException e){

			//database does't exist yet.

		}

		if(checkDB != null){
			if(checkDB.getVersion()!=DATABASE_VERSION){
				System.out.println("Installed database has version " + checkDB.getVersion() +
						" but database version should be " + DATABASE_VERSION);
				checkDB.close();
				return false;
			}
			checkDB.close();
		}

		return (checkDB != null) ? true : false;
	}

	/**
	 * Copies your database from your local assets-folder to the just created empty database in the
	 * system folder, from where it can be accessed and handled.
	 * This is done by transferring bytestream.
	 */

	public void copyDataBase() {

		//Open your local db as the input stream
		InputStream myInput = null;
		try {
			myInput = myContext.getAssets().open(curr_db_name);
		} catch (IOException e) {
			System.out.println("Couldn't get local db open");
			e.printStackTrace();
		}

		// Path to the just created empty db
		File outFileName = myContext.getDatabasePath(curr_db_name);
		System.out.println("db is " + DB_PATH.getAbsolutePath());

		//Open the empty db as the output stream
		OutputStream myOutput = null;
		try {
			myOutput = new FileOutputStream(outFileName);
		} catch (FileNotFoundException e) {
			System.out.println("Couldn't open empty db");
			e.printStackTrace();
		}

		//transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		try {
			while ((length = myInput.read(buffer))>0){
				myOutput.write(buffer, 0, length);
			}
		} catch (IOException e) {
			System.out.println("Couldn't move bytes from input to output");
			e.printStackTrace();
		}

		//Close the streams
		try {
			myOutput.flush();
			myOutput.close();
			myInput.close();
		} catch (IOException e) {
			System.out.println("Can't close streams");
			e.printStackTrace();
		}

	}

	public void openDataBase() throws SQLException{

		//Open the database
		String myPath = DB_PATH.getAbsolutePath();
		myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
	}

	public void updateVersion(){
		if(myDataBase!=null)
			myDataBase.setVersion(DATABASE_VERSION);
	}

	@Override
	public synchronized void close() {

		if(myDataBase != null)
			myDataBase.close();

		super.close();

	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	/**
	 * Call this method when we get a new message about an event.
	 * This will parse the message and decide whether to add an event
	 * to the database, delete one, update one, or do nothing.
	 *
	 * The parsing could stand to be more sophisticated.
	 *
	 * TTL is ignored for the purposes of the DataBaseHelper.
	 *
	 * Message formats:
	 *
	 * ALERT/UPDATE: Alert contains the alerting data for an event.
	 * Update contains any changes that should be made to an alert.
	 *
	 * ID (2 hex characters)
	 * Status [actual, exercise, system, test] (1 character)
	 * Message Type [alert, update] (1 character)
	 * References (2 hex characters, ignored in the case of the insert message)
	 * Category (3 character EAS code)
	 * Response Type [shelter, evacuate, prepare, execute, monitor, assess, none] (1 character)
	 * Urgency [immediate, expected] (1 character)
	 * Severity [extreme, severe] (1 character)
	 * Certainty [observed, likely] (1 character)
	 * Effective Date [MM-dd-yyyy HH:mm] (16 characters)
	 * Expires Date [MM-dd-yyyy HH:mm] (16 characters)
	 * Area Type [zone, circle] (1 character)
	 * Zone [zip code] (5 characters) OR Circle [latitude, longitude, radius] (~20 characters) <-- depends on area type
	 * TTL (up to 10 characters, but most likely 1 or 2)
	 *
	 * ADDITION: Contains free form text that can be added onto an event. Informs users of additional information
	 * (e.g. a website to go to).
	 *
	 * ID (2 hex characters)
	 * Status [actual, exercise, system, test] (1 character)
	 * Message Type [addition] (1 character)
	 * References (2 hex characters)
	 * Message (up to 80 characters, may include HTML link with more info)
	 * TTL (up to 10 characters, but most likely 1 or 2)
	 *
	 * CANCEL: Tells the database to delete a specific event.
	 *
	 * ID (2 hex characters)
	 * Status [actual, exercise, system, test] (1 character)
	 * Message Type [cancel] (1 character)
	 * References (2 hex characters)
	 * TTL (up to 10 characters, but most likely 1 or 2)
	 *
	 * For more info, see the "overview of the emergency maps" document. (Feel free to email clustig@uci.edu if you need it.)
	 * The document does not include TTL in the spec, so keep that in mind when you read it.
	 *
	 */

	public EmergencyEvent queryEvent(String event){

		Scanner s = new Scanner(event);
		s.useLocale(Locale.US);
		Cursor c;

		String status = "Could not find status";
		String sender = "Could not find sender";
		String msg_type = "Could not find message type";
		String category = "Could not find category";
		String event_level = "Could not find event level";
		String response_type = "Could not find response type";
		String urgency = "Could not find urgency";
		String severity = "Could not find severity";
		String certainty = "Could not find certainty";
		String area_type = "Could not find area type";
		@SuppressWarnings("unused")
		String area_info = "Could not find area info";

		String status_s = "";
		String certainty_s = "";
		String urgency_s = "";

		String status_c = "#FFFFFF";
		String certainty_c = "#FFFFFF";
		String urgency_c = "#FFFFFF";
		String severity_c = "#FFFFFF";

		try{

			long origin = s.nextLong();
			c = myDataBase.query("sender", new String[] {"descriptor"}, "_id=" + origin, null, null, null, null);
			if(c.moveToFirst()) {
				sender = c.getString(c.getColumnIndex("descriptor"));
			}
			c.close();

			Date received = new Date(s.nextLong());

			/* The idea behind this is that if we have the same id number but from a different phone number
			* we should have a way to differentiate them.  furthermore, we don't want the user to know what phone
			* number sent the message, so we obfuscate it this way
			*/

			int id = Integer.parseInt(s.next(), 16);
			id = (int)(id ^ (origin & 0xFFFFF));

			c = myDataBase.query("status", new String[] {"descriptor", "speech_text", "color"}, "_id=" + s.nextInt(), null, null, null, null);
			if(c.moveToFirst()) {
				status = c.getString(c.getColumnIndex("descriptor"));
				status_s = c.getString(c.getColumnIndex("speech_text"));
				status_c = c.getString(c.getColumnIndex("color"));
			}
			c.close();

			c = myDataBase.query("msg_type", new String[] {"descriptor"}, "_id=" + s.nextInt(), null, null, null, null);
			if(c.moveToFirst()) {
				msg_type = c.getString(c.getColumnIndex("descriptor"));
			}
			c.close();

			System.out.println("msg_type " + msg_type);

			/* If we decide to delete/cancel an event or add new information
			 * to an event, then we need to know which event this one is
			 * referencing. For new events (msg_type = alert), this is a throw
			 * away field.
			 */

			int references = Integer.parseInt(s.next(), 16);
			references = (int)(references ^ (origin & 0xFFFFF));

			/* If the msg_type is cancel, then we don't need to get more information,
			 * we just have to delete the event from the database.
			 * If the msg_type is addition, then we just add the additional information
			 * to the event.
			 * See the comment before this method to understand the format of the messages.
			 */

			if(msg_type.equals("cancel")) {
				EmergencyEvent deleted = deleteEvent(references);
				s.close();
				return deleted;
			} else if(msg_type.equals("addition")) {
				try {
					addExtraInfo(references, s.nextLine(), received);
				} catch (Exception e){

				}
				s.close();
				return getSpecificEvent("" + references);
			}

			c = myDataBase.query("category", new String[] {"event_description",  "event_level"}, "code='" + s.next() + "'", null, null, null, null);
			if(c.moveToFirst()) {
				category = c.getString(c.getColumnIndex("event_description"));
				event_level = c.getString(c.getColumnIndex("event_level"));
			}
			c.close();

			c = myDataBase.query("response_type", new String[] {"descriptor"}, "_id='" + s.nextInt() + "'", null, null, null, null);
			if(c.moveToFirst()) {
				response_type = c.getString(c.getColumnIndex("descriptor"));
			}
			c.close();

			c = myDataBase.query("urgency", new String[] {"descriptor", "speech_text", "color"}, "_id='" + s.nextInt() + "'", null, null, null, null);
			if(c.moveToFirst()) {
				urgency = c.getString(c.getColumnIndex("descriptor"));
				urgency_s = c.getString(c.getColumnIndex("speech_text"));
				urgency_c = c.getString(c.getColumnIndex("color"));
			}
			c.close();

			c = myDataBase.query("severity", new String[] {"descriptor", "color"}, "_id='" + s.nextInt() + "'", null, null, null, null);
			if(c.moveToFirst()) {
				severity = c.getString(c.getColumnIndex("descriptor"));
				severity_c = c.getString(c.getColumnIndex("color"));
			}
			c.close();

			c = myDataBase.query("certainty", new String[] {"descriptor", "speech_text", "color"}, "_id='" + s.nextInt() + "'", null, null, null, null);
			if(c.moveToFirst()) {
				certainty = c.getString(c.getColumnIndex("descriptor"));
				certainty_s = c.getString(c.getColumnIndex("speech_text"));
				certainty_c = c.getString(c.getColumnIndex("color"));
			}
			c.close();

			//If we're unable to parse the dates or they don't make sense, then exit
			String effective = s.next() + " " + s.next();
			DateFormat df = new SimpleDateFormat("M-d-y H:m", Locale.US);
			Date effective_date = df.parse(effective, new ParsePosition(0));
			if(effective_date == null) {
				s.close();
				return null;
			}

			String expires = s.next() + " " + s.next();
			Date expires_date = df.parse(expires, new ParsePosition(0));
			if(expires_date == null) {
				s.close();
				return null;
			}

			if(effective_date.after(expires_date)) {
				s.close();
				return null;
			}

			c = myDataBase.query("area_type", new String[] {"descriptor"}, "_id='" + s.nextInt() + "'", null, null, null, null);
			if(c.moveToFirst()) {
				area_type = c.getString(c.getColumnIndex("descriptor"));
			}
			c.close();

			//The area type can either be a circle with a set center and radius or it can be a zip code.
			AreaInfo ai = null;

			if(area_type.equals("zone")){
				int zip = s.nextInt();
				area_info = "" + zip;
				ai = new AreaZip(zip, geocoder);

			} else if(area_type.equals("circle")) {
				double lat = s.nextDouble();
				double lon = s.nextDouble();
				double radius = s.nextDouble();

				String address = "";

				//try to figure out the city this address correlates to
				try {
					List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
					if (addresses != null && !addresses.isEmpty()) {
						address = addresses.get(0).getLocality();
					}

				} catch (IOException e) {
					// Auto-generated catch block
					e.printStackTrace();
				}

				ai = new AreaCircle(lat, lon, radius, address);
				area_info = lat + " " + lon + " " + radius;
			}

			/* If the message is update, this means we're going to essentially replace the old
			 * entry with the new one. So, say I send message 12 which is an update to message 10.
			 * By the time we're done parsing message 12, we should change the id to 10 so that
			 * when we run the updateEvent() function, it will update message 10 with the new data.
			 */

			if(msg_type.equals("update"))
				id = references;


			//NEW: Set up speaker string
			String speaker_string = status_s + " " + category + " " + certainty_s + " " + urgency_s + " ";



			EmergencyEvent e = new EmergencyEvent(id, sender, received, status, msg_type,
					category, event_level, response_type, urgency, severity,
					certainty, effective_date, expires_date, ai, myContext, null, speaker_string,
					certainty_c, severity_c, status_c, urgency_c);

			if(msg_type.equals("alert"))
				insertIntoDb(e);
			else if(msg_type.equals("update"))
				updateEvent(e);

			s.close();

			return e;

		} catch (InputMismatchException e){
			System.out.println("Formatted incorrectly" + " " + e.getMessage());
			return null;
		}
	}

	private void addExtraInfo(int id, String extra_info, Date received){
		EmergencyEvent e = getSpecificEvent(""+ id);
		e.setReceived(received);
		e.setMsgType("addition");
		/* The "extra info" is free form so if it's not in the right language we should
	     * translate it using Bing Translate */
		if(locale.startsWith("es"))
			extra_info = BingTranslate.bingTranslate(extra_info, Language.SPANISH);
		e.addExtraInfo(extra_info);
		//update the event in the database so it includes this new information.
		updateEvent(e);
	}

	//Prepare the event for entry into the database.

	private ContentValues createContentValues(EmergencyEvent e){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

		ContentValues cv = new ContentValues();
		cv.put("_id", e.getId());
		cv.put("sender", e.getSender());
		cv.put("received", df.format(e.getReceived()));
		cv.put("event_level", e.getEventLevel());
		cv.put("status", e.getStatus());
		cv.put("msg_type", e.getType());
		cv.put("category", e.getCategory());
		cv.put("response_type", e.getResponseType());
		cv.put("urgency", e.getUrgency());
		cv.put("severity", e.getSeverity());
		cv.put("certainty", e.getCertainty());
		cv.put("effective", df.format(e.getEffective()));
		cv.put("expires", df.format(e.getExpires()));
		cv.put("info", e.getExtraInfo());
		cv.put("area_type", e.getAreaType());
		cv.put("area_radius", e.getRadius());
		cv.put("area_lat", e.getPoint().latitude);
		cv.put("area_lon", e.getPoint().longitude);
		cv.put("address", e.getAddress());
		cv.put("display", e.getDisplay());
		cv.put("speaker_string", e.getNotificationSpeech());
		cv.put("certainty_color", e.getCertaintyColor());
		cv.put("severity_color", e.getSeverityColor());
		cv.put("status_color", e.getStatusColor());
		cv.put("urgency_color", e.getUrgencyColor());

		return cv;
	}

	//Inserts an event into the database.

	private void insertIntoDb(EmergencyEvent e){
		System.out.println("Inserting into db");
		ContentValues cv = createContentValues(e);

		myDataBase.insert("events", null, cv);
	}

	public ArrayList<EmergencyEvent> getAllEvents(){
		Cursor c = myDataBase.query("events", null, null, null, null, null, null);
		return getEventsFromQuery(c);
	}

	//This gets all current events out of the database.

	public ArrayList<EmergencyEvent> getAllValidEvents(){
		Cursor c = myDataBase.query("events", null,
				"julianday(expires) > julianday('now', 'localtime') AND julianday(effective) < julianday('now', 'localtime')",
				null, null, null, null, null);
		return getEventsFromQuery(c);
	}

	public ArrayList<EmergencyEvent> getAllFutureEvents(){
		Cursor c = myDataBase.query("events", null,
				"julianday(effective) > julianday('now', 'localtime')",
				null, null, null, null, null);
		return getEventsFromQuery(c);
	}

	public ArrayList<EmergencyEvent> getAllPastEvents(){
		Cursor c = myDataBase.query("events", null,
				"julianday(expires) < julianday('now', 'localtime')",
				null, null, null, null, null);
		return getEventsFromQuery(c);
	}

	//This gets just one event out of the database, identified by id.

	public EmergencyEvent getSpecificEvent(String id){
		Cursor c = myDataBase.query("events", null, "_id='" + id + "'", null, null, null, null);
		ArrayList<EmergencyEvent> e = getEventsFromQuery(c);
		if(e.size()>0)
			return e.get(0);
		else
			return null;
	}

	//Generates an ArrayList of EmergencyEvents based on a query

	private ArrayList<EmergencyEvent> getEventsFromQuery(Cursor c){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
		ArrayList<EmergencyEvent> e = new ArrayList<EmergencyEvent>();
		c.moveToFirst();
		while (c.isAfterLast() == false) {
			AreaInfo ai = new AreaCircle(c.getDouble(c.getColumnIndex("area_lat")),
					c.getDouble(c.getColumnIndex("area_lon")),
					c.getDouble(c.getColumnIndex("area_radius")),
					c.getString(c.getColumnIndex("address")));

			Date received = df.parse(c.getString(c.getColumnIndex("received")), new ParsePosition(0));
			Date expires = df.parse(c.getString(c.getColumnIndex("expires")), new ParsePosition(0));
			Date effective = df.parse(c.getString(c.getColumnIndex("effective")), new ParsePosition(0));

			//Not the most elegant way to convert the display field. This just says if the event should show up on the map

			Boolean displayBool = null;
			String display = c.getString(c.getColumnIndex("display"));
			if(display!=null){
				if(display.equals("0"))
					displayBool = false;
				else if(display.equals("1"))
					displayBool = true;
			}

			EmergencyEvent ee = new EmergencyEvent(
					c.getInt(c.getColumnIndex("_id")),
					c.getString(c.getColumnIndex("sender")),
					received,
					c.getString(c.getColumnIndex("status")),
					c.getString(c.getColumnIndex("msg_type")),
					c.getString(c.getColumnIndex("category")),
					c.getString(c.getColumnIndex("event_level")),
					c.getString(c.getColumnIndex("response_type")),
					c.getString(c.getColumnIndex("urgency")),
					c.getString(c.getColumnIndex("severity")),
					c.getString(c.getColumnIndex("certainty")),
					effective,
					expires,
					ai,
					myContext,
					displayBool,
					c.getString(c.getColumnIndex("speaker_string")),
					c.getString(c.getColumnIndex("certainty_color")),
					c.getString(c.getColumnIndex("severity_color")),
					c.getString(c.getColumnIndex("status_color")),
					c.getString(c.getColumnIndex("urgency_color")));
			ee.addExtraInfo(c.getString(c.getColumnIndex("info")));
			e.add(ee);

			c.moveToNext();
		}
		c.close();
		return e;
	}

	//deletes event and then puts the new, updated one in

	public void updateEvent(EmergencyEvent e){
		System.out.println("Updating in db");
		deleteEvent(e.getId());
		insertIntoDb(e);
	}

	public EmergencyEvent deleteEvent(int id){
		System.out.println("Deleting from db: " + id);
		EmergencyEvent e = getSpecificEvent("" + id);
		if(e == null) {
			return null;
		}
		e.setMsgType("cancel");
		myDataBase.delete("events", "_id='" + id + "'", null);
		return e;
	}

	//says whether the number this message was received from is authorized to send emergency messages

	public boolean isNumberVerified(long number){
		Cursor c = myDataBase.query("sender", null, "_id='" + number + "'", null, null, null, null);
		c.moveToFirst();
		boolean verified = !c.isAfterLast();
		c.close();
		return verified;
	}
}