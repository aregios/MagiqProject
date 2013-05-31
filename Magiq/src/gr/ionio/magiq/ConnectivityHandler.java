package gr.ionio.magiq;

import gr.ionio.magiq.AppLogger.LogEx;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

enum ConnectionType { CONNECTION_WIFI, CONNECTION_ANY }

public final class ConnectivityHandler {

	private static final String TAG = "ConnectivityHandler";												//Log entry tag
	private static final boolean DEBUG_BYPASS_CHECKS = false;  //***ALWAYS SET TO FALSE***								//Set true to bypass network checks (debug only) 
	
	private String netSettings = "";																		//Network setting activity  
	private Activity hostActivity = null; 																	//Host activity reference	
	private ConnectivityManager connectivityManager = null;													//Connectivity manager instance
	
	
	//Constructor - pass host activity reference
	public ConnectivityHandler(Activity hostActivity) {
		this.hostActivity = hostActivity;	
	}

	
	//Release references to objects 
	public void onDestroy() {
		this.hostActivity = null;
		this.connectivityManager = null;
	}
	

	//Query service status and launch Network settings if necessary
	public boolean queryServiceStatus(ConnectionType useConnectionType) {		    
		try {
			
			boolean isAvailable = false;
			boolean isConnected = false;
			NetworkInfo networkInfo = null;
			
			connectivityManager  = (ConnectivityManager)hostActivity.getSystemService(Context.CONNECTIVITY_SERVICE);	//Acquire a reference to the system Connectivity Manager
			if (useConnectionType == ConnectionType.CONNECTION_WIFI) {																								  
				netSettings = Settings.ACTION_WIFI_SETTINGS;
				networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);						//Query exclusively for WIFI connection
			} else {
				netSettings = Settings.ACTION_WIRELESS_SETTINGS;
				networkInfo = connectivityManager.getActiveNetworkInfo();												//Query for any network connection
			}
			
			isAvailable = (networkInfo != null && networkInfo.isAvailable() == true) || (DEBUG_BYPASS_CHECKS); 			//Check for available connectivity - DEBUG_BYPASS_CHECKS short-circuits to always true
			if (isAvailable == false) {
				AlertDialog.Builder adb = new AlertDialog.Builder(hostActivity);										//New Alert dialog
				
				String sAppName = hostActivity.getString(R.string.app_name);
				String sMessage = hostActivity.getString(R.string.prompt_connection_settings);
				adb.setMessage(String.format(sMessage, sAppName));														//Setup dialog message
				
				String sYes = hostActivity.getString(R.string.dialog_yes);
				adb.setPositiveButton(sYes, new DialogInterface.OnClickListener() {										//Setup Yes button
				public void onClick(DialogInterface dialog, int id) {
					Intent intent = new Intent(netSettings);					
					hostActivity.startActivity(intent);																	//Show system settings dialog to activate WiFi/3G connection		                    
				    }
				});
				
				String sNo = hostActivity.getString(R.string.dialog_no);												//Setup No button
				adb.setNegativeButton(sNo, null);        		
				adb.create().show();																					//Create and display dialog
			}
			
			isConnected = (networkInfo != null && networkInfo.isConnected() == true) || (DEBUG_BYPASS_CHECKS);			//Check for active network connection - DEBUG_BYPASS_CHECKS short-circuits to always true        	
			if (isAvailable == true && isConnected == false) {
				AlertDialog.Builder adb = new AlertDialog.Builder(hostActivity);										//New Alert dialog
				
				String sAppName = hostActivity.getString(R.string.app_name);
				String sMessage = hostActivity.getString(R.string.error_connection_failed);
				adb.setMessage(String.format(sMessage, sAppName));														//Setup dialog message
				
				String sOK = hostActivity.getString(R.string.dialog_ok);
				adb.setNeutralButton(sOK, null);																		//Setup OK button
				adb.create().show();																					//Create and display dialog
			}
			        	
			return (isAvailable && isConnected);																		//Return active connection status to caller
			
	    } catch(Exception ex) {
	    	LogEx.e(TAG, ex.toString(), ex);		
	    	return false;
		}							
	}

} //ConnectivityHandler