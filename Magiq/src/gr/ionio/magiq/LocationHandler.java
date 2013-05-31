package gr.ionio.magiq;

import gr.ionio.magiq.AppLogger.LogEx;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;


public final class LocationHandler implements LocationListener {
		
	private static final String TAG = "LocationHandler";													//Log entry tag
	private static final int THRESHOLD_TIME = 1000 * 60 ;													//Time to consider old the location fix (sec)
	private static final int THRESHOLD_ACCURACY = 50;														//Value to consider inaccurate the location fix (m)
	
	private Activity hostActivity = null; 																	//Host activity reference
	private Location currentLocation = null;																//Current location object
	private LocationManager locationManager = null;															//Location Manager object
	private WaitDataTask taskWait = null;																	//Asynchronous task reference
	
	
	//Constructor - pass host activity reference
	public LocationHandler(Activity hostActivity) {
		this.hostActivity = hostActivity;
	}
	
	
	//Release references to objects 
	public void onDestroy() {
		this.hostActivity = null;
		this.locationManager = null;
	}

	
	//Return location handler's current Latitude
	public double getLatitude() {
		if (currentLocation == null)
			return 0.0d;
		else
			return currentLocation.getLatitude();
	}
	    
	    
	//Return location handler's current Longitude 
	public double getLongitude() {
	if (currentLocation == null)
		return 0.0d;
	else
		return currentLocation.getLongitude();
	}


	//Query service status and launch GPS settings if necessary
	public boolean queryServiceStatus() {
		try {
			
			locationManager = (LocationManager)hostActivity.getSystemService(Context.LOCATION_SERVICE);					//Acquire a reference to the system Location Manager
			boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER); 						//Check for GPS
			if (gpsEnabled == false) {
				AlertDialog.Builder adb = new AlertDialog.Builder(hostActivity);										//New Alert dialog
			
				String sAppName = hostActivity.getString(R.string.app_name);
				String sMessage = hostActivity.getString(R.string.prompt_location_settings);
				adb.setMessage(String.format(sMessage, sAppName));														//Setup dialog message
				
				String sYes = hostActivity.getString(R.string.dialog_yes);
				adb.setPositiveButton(sYes, new DialogInterface.OnClickListener() {										//Setup Yes button
				public void onClick(DialogInterface dialog, int id) {
					Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);					
					hostActivity.startActivity(intent);																//Show location system settings dialog to activate GPS
				    }
				});
				
				String sNo = hostActivity.getString(R.string.dialog_no);												//Setup No button
					adb.setNegativeButton(sNo, null);
					adb.create().show();
			}
			
			return gpsEnabled;																							//Return GPS enabled status to caller
		    	
		} catch(Exception ex) {
			LogEx.e(TAG, ex.toString(), ex);		
			return false;
		}							
	}
	
	
	//Start location monitoring - call this on activity onStart()
	public void onStart() {
		locationManager = (LocationManager)hostActivity.getSystemService(Context.LOCATION_SERVICE);						//Acquire a reference to the system Location Manager
		boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER); 							//Check for GPS
		if (gpsEnabled == true) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 5, this);							//Listen for location updates from GPS
			currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);						//Get last known location
		}
	}
	
		
	//Stop location monitoring - call this on activity onStop()
	public void onStop() {
		locationManager.removeUpdates(this);
		if (taskWait != null) taskWait.forceCancelTask();																//Terminate wait task
	}
	
	
	//Method not implemented	
	public void onProviderDisabled(String provider) {  
		//Called when the GPS provider is turned off (user turning off the GPS on the phone)  
	}  
	
	//Method not implemented
	public void onProviderEnabled(String provider) {  
		//Called when the GPS provider is turned on (user turning on the GPS on the phone)  
	}  
	
	//Method not implemented
	public void onStatusChanged(String provider, int status, Bundle extras) {  
		//Called when the status of the GPS provider changes  
	}  

	    
	//Called when GPS provides new location
	public void onLocationChanged(Location newLocation) {  
		try {
			
			if (isBetterLocation(newLocation, currentLocation)) {
				currentLocation = newLocation;		    		
				//LogEx.i(TAG, String.format("Updated: Latitude: %6f Longitude: %6f", currentLocation.getLatitude(), currentLocation.getLongitude()));
			}
			
		} catch(Exception ex) {
			LogEx.e(TAG, ex.toString(), ex);				
		}
	}

    
	//Determines whether one Location reading is better than the current Location fix
	//Original version taken from http://developer.android.com/guide/topics/location/strategies.html
	private boolean isBetterLocation(Location newLocation, Location currentLocation) {

		//A new location is always better than no location
		if (currentLocation == null) {
			return true;
		}
		
		//Check whether the new location fix is newer or older
		long timeDelta = newLocation.getTime() - currentLocation.getTime();
		boolean isSignificantlyNewer = (timeDelta > THRESHOLD_TIME);
		boolean isSignificantlyOlder = (timeDelta < -THRESHOLD_TIME);
		boolean isNewer = (timeDelta > 0);
		
		//If it's been more than THRESHOLD_TIME since the current location, use the new location because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
		//If the new location is more than THRESHOLD_TIME older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}
		
		//Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (newLocation.getAccuracy() - currentLocation.getAccuracy());
		boolean isLessAccurate = (accuracyDelta > 0);
		boolean isMoreAccurate = (accuracyDelta < 0);
		boolean isSignificantlyLessAccurate = (accuracyDelta > THRESHOLD_ACCURACY);
		
		//Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(newLocation.getProvider(), currentLocation.getProvider());
		
		//Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		
		return false;
	}

	//Checks whether two providers are the same
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return (provider2 == null);
		}
		return provider1.equals(provider2);
	}
    
    
	//Query for available location data and wait if necessary - blocks UI thread with wait dialog
	public boolean queryAvailableLocation() {
		boolean hasAvailableLocation = testCurrentLocation(); 															//Check for valid location data
		LogEx.i(TAG, String.format("queryAvailableLocation(): %s", String.valueOf(hasAvailableLocation)));
		if (hasAvailableLocation == false) {																			//Currently there aren't valid location data 
			taskWait = new WaitDataTask();																				//Create new task instance
			taskWait.execute();																							//Launch asynchronous task and return
		} else {
			LogEx.i(TAG, String.format("Latitude: %6f Longitude: %6f", currentLocation.getLatitude(), currentLocation.getLongitude()));
		}
		return hasAvailableLocation;
	}
    
    
	//Has location query
	private boolean testCurrentLocation() {
		return (currentLocation != null && (currentLocation.getLatitude() != 0.0d || currentLocation.getLongitude() != 0.0d));
	}

	
	//Asynchronous task for waiting location data
	private class WaitDataTask extends AsyncTask<Void, Void, Void> {

		private static final String TAG = "WaitDataTask";	
		private ProgressDialog progressDialog = new ProgressDialog(hostActivity);  
    	
    	
		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(hostActivity.getString(R.string.info_waiting_location_data));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, hostActivity.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {							
				public void onClick(DialogInterface dialog, int id) {
					LogEx.i(TAG, "onPreExecute(): Issuing cancel request...");
					WaitDataTask.this.cancel(true);																		//Cancel asynchronous task from progress dialog cancel button
		        }
		    }); 
			progressDialog.show();
		}
    	
    	
		@Override
		protected Void doInBackground(Void... params) {
			try {
				
				LogEx.i(TAG, "doInBackground(): Waiting for location data...");
				while (isCancelled() == false &&  LocationHandler.this.testCurrentLocation() == false) {}				//Loop reading location data and cancellation flag
				
			} catch (Exception ex) {
				LogEx.e(TAG, ex.toString(), ex);
			}
		
			return null;
		}      

        
		@Override
		protected void onPostExecute(Void result) {
			progressDialog.dismiss();
			LogEx.i(TAG, "onPostExecute(): Location data available.");
		}

		@Override
		protected void onCancelled(Void result) {
			progressDialog.dismiss();
			LogEx.i(TAG, "onCancelled(): Waiting for location data cancelled.");
		}
        
		@Override
		protected void onProgressUpdate(Void... values) {
		
		}
        
        
		//Force cancel from outer class
		public void forceCancelTask() {
			progressDialog.dismiss();																					//Close progress dialog
			if (this.getStatus() != Status.FINISHED) {																	//Check if running or pending
				this.cancel(true);																						//Force cancel
			}
		}
	    
	} //WaitDataTask

    
} //LocationHandler