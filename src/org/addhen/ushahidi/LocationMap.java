package org.addhen.ushahidi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.addhen.ushahidi.AddIncident.MyLocationListener;
import org.addhen.ushahidi.data.IncidentsData;
import org.addhen.ushahidi.data.UshahidiDatabase;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public class LocationMap extends MapActivity {
	private MapView mapView = null;
	private MapController mapController;
	public static Geocoder gc;
	private GeoPoint defaultLocation;
	private double latitude;
	private double longitude;
	private List<IncidentsData> mNewIncidents;
	private List<IncidentsData> mOldIncidents;
	private Button btnReset;
	private Button btnSave;
	private Button btnFind;
	private Bundle bundle = new Bundle();
	public List<Address> foundAddresses;
	private String locationName;
	private String title;
	private String date;
	private String description;
	private String location;
	private String categories;
	private String media;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_map);
		
		mapView = (MapView) findViewById(R.id.location_map);
		locationName ="";
		
		foundAddresses = new ArrayList<Address>();
		gc = new Geocoder(this);
		
		btnSave = (Button) findViewById(R.id.btn_save);
		
		btnSave.setOnClickListener( new View.OnClickListener(){
			public void onClick( View v ) {
				
				bundle.putDouble("latitude", latitude);
				bundle.putDouble("longitude", longitude);
				bundle.putString("location", locationName);
				
				//this will launch an extra activity
				/*Intent intent = new Intent( LocationMap.this,AddIncident.class);
				intent.putExtra("locations",bundle);
				startActivityForResult(intent,1);*/
				
				//Solution pass the data to the calling activity
				Intent intent = new Intent();
				intent.putExtra("locations",bundle);
				setResult( RESULT_OK, intent );
				finish();
			}
		});
		
		btnFind = (Button) findViewById(R.id.btn_find);
		btnFind.setOnClickListener( new View.OnClickListener(){
			public void onClick( View v ) {
				Toast.makeText(LocationMap.this, "Finding you...", Toast.LENGTH_SHORT).show();
				updateLocation();
			}
		});
		
		mapController = mapView.getController();
		
		mOldIncidents = new ArrayList<IncidentsData>();
		mNewIncidents  = showIncidents("All");
		
		if( mNewIncidents.size() > 0 ) {
			latitude = Double.parseDouble( mNewIncidents.get(0).getIncidentLocLatitude());
			longitude = Double.parseDouble( mNewIncidents.get(0).getIncidentLocLongitude());
		}
		
		defaultLocation = getPoint( latitude, longitude);
		centerLocation(defaultLocation);
		
		btnReset = (Button) findViewById(R.id.btn_reset);
		btnReset.setOnClickListener( new View.OnClickListener(){
			public void onClick( View v ) {
				centerLocation(defaultLocation);
			}
		});
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	private void placeMarker( int markerLatitude, int markerLongitude ) {
		
		Drawable marker = getResources().getDrawable( R.drawable.marker);
		 
		marker.setBounds(0, 0, marker.getIntrinsicWidth(),
				 marker.getIntrinsicHeight());
		mapView.getController().setZoom(12);

		mapView.setBuiltInZoomControls(true);
		mapView.getOverlays().add(new MapMarker(marker,
				    markerLatitude, markerLongitude));
	}
	
	public GeoPoint getPoint(double lat, double lon) {
	    return(new GeoPoint((int)(lat*1000000.0), (int)(lon*1000000.0)));
	}
	
	private void centerLocation(GeoPoint centerGeoPoint) {
		
		mapController.animateTo(centerGeoPoint);
		
		//locName = getLocationFromLatLon(centerGeoPoint.getLatitudeE6(),centerGeoPoint.getLongitudeE6());
		
		GeocodeTask geocode = new GeocodeTask();
		geocode.execute(Double.valueOf(centerGeoPoint.getLatitudeE6()),
				Double.valueOf(centerGeoPoint.getLongitudeE6()));
		
		if( locationName == "" ) {
			
    		Toast.makeText(LocationMap.this, "No location found", Toast.LENGTH_SHORT).show();
    	
		}else {
    			
    		//locationName = locName;
    		Toast.makeText(LocationMap.this, "Location "+locationName, Toast.LENGTH_SHORT).show();
    	
		}
		
		placeMarker(centerGeoPoint.getLatitudeE6(), centerGeoPoint.getLongitudeE6());
	}
	
	/**
	 * get the real location name from
	 * the latitude and longitude.
	 */
	private String getLocationFromLatLon( double lat, double lon ) {
		
		try {
			
    		foundAddresses = gc.getFromLocation( latitude, longitude, 5 );
    		
    		Address address = foundAddresses.get(0);
    		
    		return address.getLocality();
		
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	
	// get incidents from the db
	public List<IncidentsData> showIncidents( String by ) {
		Cursor cursor;
	
		if( by.equals("All")) 
			cursor = UshahidiApplication.mDb.fetchAllIncidents();
		else
			cursor = UshahidiApplication.mDb.fetchIncidentsByCategories(by);
		  
		if (cursor.moveToFirst()) {
			int idIndex = cursor.getColumnIndexOrThrow( 
					UshahidiDatabase.INCIDENT_ID);
			int titleIndex = cursor.getColumnIndexOrThrow(
					  UshahidiDatabase.INCIDENT_TITLE);
			int dateIndex = cursor.getColumnIndexOrThrow(
					  UshahidiDatabase.INCIDENT_DATE);
			int verifiedIndex = cursor.getColumnIndexOrThrow(
					  UshahidiDatabase.INCIDENT_VERIFIED);
			int locationIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.INCIDENT_LOC_NAME);
			  
			int descIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.INCIDENT_DESC);
			  
			int categoryIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.INCIDENT_CATEGORIES);
			  
			int mediaIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.INCIDENT_MEDIA);
			  
			int latitudeIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.INCIDENT_LOC_LATITUDE);
			  
			int longitudeIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.INCIDENT_LOC_LONGITUDE);
			  
			  
			do {
				  
				IncidentsData incidentData = new IncidentsData();
				mOldIncidents.add( incidentData );
				  
				int id = Util.toInt(cursor.getString(idIndex));
				incidentData.setIncidentId(id);
				  
				title = Util.capitalizeString(cursor.getString(titleIndex));
				incidentData.setIncidentTitle(title);
				  
				description = cursor.getString(descIndex);
				incidentData.setIncidentDesc(description);
				  
				categories = cursor.getString(categoryIndex);
				incidentData.setIncidentCategories(categories);
				  
				location = cursor.getString(locationIndex);
				incidentData.setIncidentLocLongitude(location);
				  
				date = Util.joinString("Date: ",Util.formatDate("yyyy-MM-dd hh:mm:ss", cursor.getString(dateIndex), "MMMM dd, yyyy 'at' hh:mm:ss aaa" ));
				incidentData.setIncidentDate(date);			  
				  
				media = cursor.getString(mediaIndex);
				incidentData.setIncidentMedia(media);
				  
				  
				incidentData.setIncidentVerified(Util.toInt(cursor.getString(verifiedIndex) ));
				  
				incidentData.setIncidentLocLatitude(cursor.getString(latitudeIndex));
				incidentData.setIncidentLocLongitude(cursor.getString(longitudeIndex));
				  
				  
			} while (cursor.moveToNext());
		}
	    
		cursor.close();
		return mOldIncidents;
	    
	}
	
	//update the device current location
	private void updateLocation() {
		MyLocationListener listener = new MyLocationListener(); 
        LocationManager manager = (LocationManager) 
    getSystemService(Context.LOCATION_SERVICE); 
        long updateTimeMsec = 1000L; 
        
        //DIPO Fix
        List<String> providers = manager.getProviders(true);
        boolean gps_provider = false, network_provider = false;
        
        for (String name : providers) {
        	if (name.equals(LocationManager.GPS_PROVIDER)) gps_provider = true;
        	if (name.equals(LocationManager.NETWORK_PROVIDER)) network_provider = true;        	
        }
        
        //Register for GPS location if enabled or if neither is enabled
        if( gps_provider || (!gps_provider && !network_provider) ) {
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
   updateTimeMsec, 500.0f, 
		    listener);
		} else if (network_provider) {
			manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
   updateTimeMsec, 500.0f, 
		    listener); 
		}
	}
	
	// get the current location of the user
	public class MyLocationListener implements LocationListener { 
	    public void onLocationChanged(Location location) { 
	    	double latitude = 0;
	    	double longitude = 0;
	    	String locName = "";
	    	
	    	if (location != null) { 
	    		//Dipo Fix
	  	        //Stop asking for updates when location has been retrieved
	    		((LocationManager)getSystemService(Context.LOCATION_SERVICE)).removeUpdates(this);
	  	      
	    		latitude = location.getLatitude(); 
	  	        longitude = location.getLongitude(); 
	
	  	      locName = getLocationFromLatLon(latitude,longitude);
	  	        centerLocation(getPoint(latitude, longitude));
	  	        
	  	        //This is already handled in the centerLocation method.
		    	/*
		    	if( locName.equals("") ) {
		    			
		    		Toast.makeText(LocationMap.this, "No location found", Toast.LENGTH_SHORT).show();
		    	}else {
		    			
		    		locationName = locName;
		    		Toast.makeText(LocationMap.this, "Location "+locName, Toast.LENGTH_SHORT).show();
		    	}*/
	  	    }	     
	    } 
	    public void onProviderDisabled(String provider) { 
	    	Toast.makeText(LocationMap.this.getBaseContext(), 
	    			"A location can not currently be determined. Enable more " +
	    			"location sources i.e. GPS Satellites in Security and Location " +
	    			"Settings.", Toast.LENGTH_LONG).show(); 
	    } 
	    public void onProviderEnabled(String provider) { 
	      // TODO Auto-generated method stub 
	    } 
	    public void onStatusChanged(String provider, int status, Bundle extras) 
	    { 
	      // TODO Auto-generated method stub 
	    } 
	}
	
	//thread class
	private class GeocodeTask extends AsyncTask <Double, Void, String> {
		
		protected String localityName;
		
		@Override 
		protected String doInBackground(Double... params) {
			localityName = getLocationFromLatLon(params[0], params[1]);
			return localityName;
		}
		
		@Override
		protected void onPostExecute(String result)
		{
			
			if( result.equals(""))
				locationName = "";
			else
				locationName = result;
		}

		
	}
	
	private class MapMarker extends ItemizedOverlay<OverlayItem> {
		
		private List<OverlayItem> locations =new ArrayList<OverlayItem>();
		private Drawable marker;
		private OverlayItem myOverlayItem;
		private boolean MoveMap = false;
		
		public MapMarker( Drawable defaultMarker, int LatitudeE6, int LongitudeE6 ) {
			super(defaultMarker);
			
			this.marker = defaultMarker;
			
			// create locations of interest
			GeoPoint myPlace = new GeoPoint(LatitudeE6,LongitudeE6);
			
			myOverlayItem = new OverlayItem(myPlace, "Location ", "Location");
			
			locations.add(myOverlayItem);
			   
			populate();
			   
		}
		@Override
		protected OverlayItem createItem(int i) {
			return locations.get(i);
		}

		@Override
		public int size() {
			return locations.size();
		}

		@Override
		public void draw(Canvas canvas, MapView mapView,
				boolean shadow) {
			super.draw(canvas, mapView, shadow);
		   
			boundCenterBottom(marker);
		}
		
		/*@Override
		protected boolean onTap(int index) {
			Toast.makeText(LocationMap.this, "Location ", Toast.LENGTH_SHORT).show();
		  return true;
		}*/
		

		@Override
		public boolean onTouchEvent(MotionEvent motionEvent, MapView mapview) {
			
			int Action = motionEvent.getAction();
			if (Action == MotionEvent.ACTION_UP){
			
				/*if(!MoveMap)
				{
					
					Projection proj = mapView.getProjection();
					GeoPoint loc = proj.fromPixels((int)motionEvent.getX(), (int)motionEvent.getY());
		              
					//remove the last marker
					mapView.getOverlays().remove(0);  
					centerLocation(loc);
					
				}*/
				MoveMap = false;
		    
		   }
		   else if (Action == MotionEvent.ACTION_DOWN) {
			   if(!MoveMap ) {
				   Projection proj = mapView.getProjection();
				   GeoPoint loc = proj.fromPixels((int)motionEvent.getX(), (int)motionEvent.getY());
	              
				   //remove the last marker
			   		mapView.getOverlays().remove(0);  
			   		centerLocation(loc);
			   }
			   
			   MoveMap = false;

		   }
		   else if (Action == MotionEvent.ACTION_MOVE){
			   
			   MoveMap = true;
		   }

		   return super.onTouchEvent(motionEvent, mapview);
		   
		}
	}
}
