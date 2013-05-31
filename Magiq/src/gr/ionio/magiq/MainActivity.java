package gr.ionio.magiq;

import gr.ionio.magiq.AppCommon.Entities;
import gr.ionio.magiq.AppCommon.Keys;
import gr.ionio.magiq.AppCommon.ServerSide;
import gr.ionio.magiq.AppCommon.Utility;
import gr.ionio.magiq.AppLogger.LogEx;

import java.io.File;
import java.net.URL;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.entity.mime.HttpMultipartMode;
import ch.boye.httpclientandroidlib.entity.mime.MultipartEntity;
import ch.boye.httpclientandroidlib.entity.mime.content.FileBody;
import ch.boye.httpclientandroidlib.entity.mime.content.StringBody;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.util.EntityUtils;

public class MainActivity extends Activity {
	
	private static final String TAG = "MainActivity";
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	
	private int uiState = 0;																				//UI state
	private String imageFile = "";																			//Image file
	private LocationHandler locationHandler = new LocationHandler(this);									//Location handler instance
	private ConnectivityHandler connectivityHandler = new ConnectivityHandler(this);						//Network connectivity handler instance
	
	private SubmitQueryTask taskSubmit = null;																//Asynchronous task reference
	private CompressImageTask taskCompress = null;															//Asynchronous task reference
	
	private String queryLocalID = "";																		//Last successfully submitted query local ID
	private String queryRemoteID = "";																		//Last successfully submitted query remote ID
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupLogger();        																				//Setup logger
		setContentView(R.layout.activity_main);																//Setup layout
		setUIState(0);																						//Initialize UI state
	}
    
 
	@Override
	protected void onStart() {
		super.onStart();
		locationHandler.onStart();																			//Start location monitoring
		
		EditText txt = (EditText)this.findViewById(R.id.textDistance);
		String defString = getString(R.string.default_distance);    	
		txt.setText(Utility.getPersistedString(this, Keys.Distance, defString));							//Restore persisted distance text
	}
    
 
	@Override
	protected void onRestoreInstanceState(Bundle instanceState) {
		super.onRestoreInstanceState(instanceState);
		
		queryLocalID = Utility.getString(instanceState.getString(Keys.QueryLocalID), "");					//Read saved query local ID
		queryRemoteID = Utility.getString(instanceState.getString(Keys.QueryRemoteID), "");					//Read saved query remote ID
		
		imageFile = Utility.getString(instanceState.getString(Keys.ImageFile), "");							//Read saved image file
		uiState = instanceState.getInt(Keys.UIState, 0);													//Read saved UI state code
		setUIState(uiState);																				//Restore UI to saved state
	} 
    
	
	@Override
	protected void onSaveInstanceState(Bundle instanceState) {
		super.onSaveInstanceState(instanceState);
		
		instanceState.putString(Keys.QueryLocalID, queryLocalID);          									//Save query local ID
		instanceState.putString(Keys.QueryRemoteID, queryRemoteID);          								//Save query remote ID
		
		instanceState.putString(Keys.ImageFile, imageFile);          										//Save image file
		instanceState.putInt(Keys.UIState, uiState);														//Save UI state code
	}
  

	@Override
	protected void onStop() {
		super.onStop();
		locationHandler.onStop();																			//Stop listening for location updates
		
		if (taskSubmit != null) taskSubmit.forceCancelTask();												//Terminate submit task
		if (taskCompress != null) taskCompress.forceCancelTask();											//Terminate compression task
		 
		EditText txt = (EditText)this.findViewById(R.id.textDistance);
		String valString = txt.getText().toString();
		Utility.setPersistedString(this, Keys.Distance, valString);											//Save distance text to persisted storage    
	}
    
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		locationHandler.onDestroy();
		connectivityHandler.onDestroy();
	}
    
    
	//Logger setup
	private void setupLogger() {
		LogEx.attach(this);																					//Attach activity to logger
		Resources res = getResources();																		//Get resources reference
		boolean defBoolean = res.getBoolean(R.bool.default_enable_logging);
		boolean valBoolean = Utility.getPersistedBoolean(this, Keys.EnableLogging, defBoolean);
		LogEx.setEnabled(valBoolean);																		//Read persisted setting    
	}
    

	//UI state transitions
	private void setUIState(int nNewState) {  	
		try {
		
			uiState = nNewState;																			//Update UI State variable
			switch (uiState) {
				case 1:			//Active state (1)		        
				{
					TableRow pane = (TableRow)this.findViewById(R.id.paneBegin);
					pane.setVisibility(View.INVISIBLE);														//Hide start message
					
					pane = (TableRow)this.findViewById(R.id.paneDistance);	
					pane.setVisibility(View.VISIBLE);														//Show distance controls
					
					pane = (TableRow)this.findViewById(R.id.paneCoords1);	
					pane.setVisibility(View.VISIBLE);														//Show latitude controls
					
					pane = (TableRow)this.findViewById(R.id.paneCoords2);	
					pane.setVisibility(View.VISIBLE);														//Show longitude controls
					
					TextView txtv = (TextView)this.findViewById(R.id.textLatitude);
					txtv.setText(String.format("%.6f", locationHandler.getLatitude()));						//Set latitude text from location handler
					
					txtv = (TextView)this.findViewById(R.id.textLongitude);
					txtv.setText(String.format("%.6f", locationHandler.getLongitude()));					//Set longitude text from location handler
					
					Bitmap myBitmap = BitmapFactory.decodeFile(imageFile);									//Load bitmap from image file
					ImageView im = (ImageView)this.findViewById(R.id.imageMain);
					im.setImageBitmap(myBitmap);															//Load image view from bitmap
					
					Button btn = (Button)this.findViewById(R.id.buttonSubmit);
					btn.setEnabled(true);																	//Enable Submit button
					break;
				}
				
				default:		//Initial state	(0)			
				{
					TableRow pane = (TableRow)this.findViewById(R.id.paneBegin);
					pane.setVisibility(View.VISIBLE);														//Show start message
					
					pane = (TableRow)this.findViewById(R.id.paneDistance);	
					pane.setVisibility(View.INVISIBLE);														//Hide distance controls
					
					pane = (TableRow)this.findViewById(R.id.paneCoords1);	
					pane.setVisibility(View.INVISIBLE);														//Hide latitude controls
					
					pane = (TableRow)this.findViewById(R.id.paneCoords2);	
					pane.setVisibility(View.INVISIBLE);														//Hide longitude controls
					
					TextView txtv = (TextView)this.findViewById(R.id.textLatitude);
					txtv.setText(getString(R.string.default_coord));										//Set default value
					
					txtv = (TextView)this.findViewById(R.id.textLongitude);
					txtv.setText(getString(R.string.default_coord));										//Set default value
					
					ImageView im = (ImageView)this.findViewById(R.id.imageMain);
					im.setImageResource(R.drawable.no_image);												//Load default image from resource
					
					Button btn = (Button)this.findViewById(R.id.buttonSubmit);
					btn.setEnabled(false);																	//Disable Submit button
					break;
				}
			}
			
		} catch(Exception ex) {
			LogEx.e(TAG, ex.toString(), ex);			
			Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_LONG).show();					
		}	   
	}

   
   
	//Capture image click
	public void onCaptureClick(View v) {
		try {
			
			if (locationHandler.queryServiceStatus() == true) {												//Check for GPS service status
				if (locationHandler.queryAvailableLocation() == true) {										//Check for available location data and wait is necessary 
					if (Utility.isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {					//Check if there is a camera application available
						imageFile = Utility.getImageFile(this, getString(R.string.app_name)); 		    	//Generate a file name to save the image to
						Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);				  		//Create Intent to take a picture and return control to the calling application
						intent.putExtra(MediaStore.EXTRA_OUTPUT, Utility.convertFiletoUri(imageFile)); 		//Pass the image file URI (URI object NOT path string!)
						startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);		  		//Start the image capture Activity
					}
				}
			}
			
		} catch(Exception ex) {
			LogEx.e(TAG, ex.toString(), ex);
			Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_LONG).show();					
		}	   
	}

    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		try {
			
			if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
				switch (resultCode) {
					case RESULT_OK:																						//Camera activity succeeded
						LogEx.i(TAG, "onActivityResult(): Image capture succeeded");
						Resources res = getResources();																	//Get resources reference
						
						boolean defBoolean = res.getBoolean(R.bool.default_image_compression);
						if (Utility.getPersistedBoolean(this, Keys.ImageCompression, defBoolean)) {						//If image compression enabled	
							int defImageQuality = res.getInteger(R.integer.default_image_quality);
							int valImageQuality = Utility.getPersistedInteger(this, Keys.ImageQuality, defImageQuality);
							
							CompressParams params = new CompressParams();												//Task parameters
							params.setImageFile(imageFile);																//Image to compress
							params.setImageQuality(valImageQuality);													//Compression quality parameter
							
							taskCompress = new CompressImageTask(); 
							taskCompress.execute(params);																//Compress image asynchronously
								
						} else {
							setUIState(1);																				//Set new UI state
						}
						break;
					
					case RESULT_CANCELED:																				//User canceled camera activity
						LogEx.i(TAG, "onActivityResult(): Image capture cancelled by user");
						Toast.makeText(this, getString(R.string.info_cancelled_by_user), Toast.LENGTH_SHORT).show();
						break;
					
					default:																							//Unknown activity result
						LogEx.i(TAG, "onActivityResult(): Image capture failed");			
						Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_LONG).show();
						break;
				}
			}
			
		} catch(Exception ex) {
			LogEx.e(TAG, ex.toString(), ex);			
			Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_LONG).show();					
		}
	}
    
    
	//Show more criteria
	public void onMoreClick(View v) {
		Intent settings = new Intent(MainActivity.this, SettingsActivity.class);						
		startActivity(settings);																						//Show settings    	
	}
    

	//Submit query
	public void onSubmitClick(View v) {
		try {
				
			if (connectivityHandler.queryServiceStatus(ConnectionType.CONNECTION_ANY) == true) {						//Check for any active connection status
			LogEx.i(TAG, "onSubmitClick(): Ready to send.");
			
			QueryData queryData = new QueryData();																		//Create new query
			queryData.setImageFile(imageFile);																			//Set image file (absolute name)
			    		
			EditText txt = (EditText)this.findViewById(R.id.textDistance);
			queryData.setDistance(Integer.parseInt(txt.getText().toString().trim()));									//Set distance
			
			TextView txv = (TextView)this.findViewById(R.id.textLatitude);
			queryData.setLatitude(Double.parseDouble(txv.getText().toString()));										//Set latitude
			
			txv = (TextView)this.findViewById(R.id.textLongitude);
			queryData.setLongitude(Double.parseDouble(txv.getText().toString()));										//Set longitude
			
			
			//Read query data from settings
			Resources res = getResources();
			int defInteger = res.getInteger(R.integer.default_max_tests);
			queryData.setMaxTests(Utility.getPersistedInteger(this, Keys.MaxTests, defInteger));						//Set max tests						
			
			defInteger = res.getInteger(R.integer.default_compare_method);
			queryData.setCompareMethod(Utility.getPersistedInteger(this, Keys.CompareMethod, defInteger));				//Set compare method
			
			defInteger = res.getInteger(R.integer.default_reduce_factor);
			queryData.setReduceFactor(Utility.getPersistedInteger(this, Keys.ReduceFactor, defInteger));				//Set reduce factor
			
			String defString = getString(R.string.default_threshold);
			queryData.setThreshold(Double.parseDouble(Utility.getPersistedString(this, Keys.Threshold, defString)));	//Set results threshold
			
			
			LogEx.i(TAG, String.format("onSubmitClick(): Submit query %s", queryData.toString()));
			taskSubmit = new SubmitQueryTask();
			taskSubmit.execute(queryData);																				//Submit query asynchronously
		
		} else {
			LogEx.i(TAG, "onSubmitClick(): Unable to send.");
		}
			
		} catch (NumberFormatException ex) {
			LogEx.e(TAG, ex.toString(), ex);		
			Toast.makeText(this, getString(R.string.error_distance_integer), Toast.LENGTH_LONG).show();    		
		
		} catch(Exception ex) {
			LogEx.e(TAG, ex.toString(), ex);		
			Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_LONG).show();					
		}
	}



	//Submit query to server asynchronously
	private class SubmitQueryTask extends AsyncTask<QueryData, Void, ActionResult> {
	
		private static final String TAG = "SubmitQueryTask";
		private ProgressDialog progressDialog = new ProgressDialog(MainActivity.this); 
		
		
		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(getString(R.string.info_sending_data));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {							
				public void onClick(DialogInterface dialog, int id) {
					LogEx.i(TAG, "onPreExecute(): Issuing cancel request...");
					SubmitQueryTask.this.cancel(true);																	//Cancel asynchronous task from progress dialog cancel button
				}
			}); 
			progressDialog.show();		
		}
		
		@Override
		protected ActionResult doInBackground(QueryData... params) {
			try {
				
				String clientID = Utility.getUUID(MainActivity.this);													//Read unique identifier  
				Resources res = getResources();																			//Get resources reference
				
				String defHost = res.getString(R.string.default_host);
				String valHost = Utility.getPersistedString(MainActivity.this, Keys.Host, defHost);						//Get host from persisted storage
				
				int defConnTimeout = res.getInteger(R.integer.default_connection_timeout);
				int valConnTimeout = 1000 * Utility.getPersistedInteger(MainActivity.this, Keys.ConnectionTimeout, defConnTimeout);	//Get connection timeout from persisted storage and convert to milliseconds
				
				HttpClient httpClient = new DefaultHttpClient();       	
				HttpParams httpParams = httpClient.getParams();
				httpParams.setParameter("http.protocol.content-charset", "UTF-8");            	
				httpParams.setParameter("http.connection.timeout", Integer.valueOf(valConnTimeout));
				
				URL url = new URL("http", valHost, "/sorcerer/submitquery.php");            							//Create URL from parts (submitdata.php)
				HttpPost httpPost = new HttpPost(url.toURI());															//HTTP Post method
				httpPost.addHeader("Connection", "keep-alive");
				
				QueryData queryData = params[0];            															//Get data from call parameters
				String localID = queryData.getLocalID();
				
				if (queryLocalID.equals(localID) == false) {															//Compare current query ID with last successfully submitted query ID
					LogEx.i(TAG, String.format("doInBackground(): Sending %s to %s. Timeout=%dms, UID=%s", queryData.toString(), valHost, valConnTimeout, clientID));
					
					MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
					File imageFile = new File(queryData.getImageFile());
					FileBody fbd = new FileBody(imageFile);																//Add file data
					mpEntity.addPart(Entities.imagedata, fbd);
					
					StringBody sbd = new StringBody(imageFile.getName());												//Add file name (stripped name from absolute name)
					mpEntity.addPart(Entities.imagefile, sbd);
					
					sbd = new StringBody(String.valueOf(queryData.getLatitude()));  									//Add latitude          	            	
					mpEntity.addPart(Entities.latitude, sbd);
					
					sbd = new StringBody(String.valueOf(queryData.getLongitude()));  									//Add longitude      	            	
					mpEntity.addPart(Entities.longitude, sbd);
					
					sbd = new StringBody(String.valueOf(queryData.getDistance()));  									//Add distance     	            	
					mpEntity.addPart(Entities.distance, sbd);
					
					sbd = new StringBody(String.valueOf(queryData.getMaxTests()));										//Add image tests limit            	
					mpEntity.addPart(Entities.maxtests, sbd);
					
					sbd = new StringBody(String.valueOf(queryData.getCompareMethod()));									//Add compare method            	
					mpEntity.addPart(Entities.comparemethod, sbd);
					
					sbd = new StringBody(String.valueOf(queryData.getReduceFactor()));									//Add reduce factor            	
					mpEntity.addPart(Entities.reducefactor, sbd);
					
					sbd = new StringBody(String.valueOf(queryData.getThreshold()));										//Add results threshold            	
					mpEntity.addPart(Entities.threshold, sbd);
					
					sbd = new StringBody(clientID);																		//Add client identifier             	
					mpEntity.addPart(Entities.clientid, sbd);
					
					httpPost.setEntity(mpEntity);
					HttpResponse httpResponse = httpClient.execute(httpPost);											//Upload data (blocking function)
					
					int resCode = httpResponse.getStatusLine().getStatusCode();											//Get response status code            	
					String resReason = httpResponse.getStatusLine().getReasonPhrase();									//Get response description
					LogEx.i(TAG, String.format("Server returned code: %s (%s)", resCode, resReason));					//Report server response
					
					if (resCode != 200) {																				//Accept only code 200 as request fulfilled 
						return new ActionResult(ActionStatus.ACTION_STATUS_ERROR, resReason);							//Return ERROR status to onPostExecute() (HTTP error)
					}
					
					HttpEntity httpEntity = httpResponse.getEntity(); 													//Get response body
					if (httpEntity == null) {
						return new ActionResult(ActionStatus.ACTION_STATUS_ERROR, "No message body");					//Return ERROR status to onPostExecute() (No message body)
					}
					
					String resBody = EntityUtils.toString(httpEntity);     												//Get response body
					LogEx.i(TAG, String.format("Server replied: %s", resBody));
					
					String remoteID = ServerSide.parseID(resBody);														//Read returned ID (not null indicates success)
					if (remoteID == null) {
						String remoteError = ServerSide.parseError(resBody);											//Read returned error
						if (remoteError == null) {
							return new ActionResult(ActionStatus.ACTION_STATUS_ERROR, "Unexpected server resonse");		//Return ERROR status to onPostExecute() (Unknown server side error)
						} else {
							return new ActionResult(ActionStatus.ACTION_STATUS_ERROR, remoteError);						//Return ERROR status to onPostExecute() (Specific server side error)
						}
					}
					
					queryLocalID = localID;																				//Update last successfully submitted query local id
					queryRemoteID = remoteID;																			//Update last successfully submitted query remote id
					
					LogEx.i(TAG, String.format("doInBackground(): Requesting query execution ID=%s", queryRemoteID));
				} else {
					LogEx.i(TAG, String.format("doInBackground(): Requesting cached query execution ID=%s", queryRemoteID));
				}
				
				int swpx = Utility.getScreenWidth(MainActivity.this); 													//Get screen width in pixels for use with the results page
				url = new URL("http", valHost, String.format("/sorcerer/results.php?id=%s&sw=%s&cid=%s", queryRemoteID, swpx, clientID));	//Construct results URL
				return new ActionResult(ActionStatus.ACTION_STATUS_OK, url.toString());									//Return OK status AND results URL string to onPostExecute()
				
			} catch (Exception ex) {
				LogEx.e(TAG, ex.toString(), ex);
				return new ActionResult(ActionStatus.ACTION_STATUS_ERROR, ex.toString());								//Return ERROR status to onPostExecute() (exception occurred)
			}
		}      
        
		@Override
		protected void onPostExecute(ActionResult result) {
			progressDialog.dismiss();																					//Close progress dialog
			LogEx.i(TAG, "onPostExecute(): Task completed.");         
			
			if (result.getStatus() == ActionStatus.ACTION_STATUS_OK) {													//Check submit data status
				Intent intent = new Intent(MainActivity.this, ResultsActivity.class);									//Setup intent
				intent.putExtra(ResultsActivity.ParamRequestURL, result.getValue());									//Pass request URL string to results activity
				startActivity(intent);
			} else {
				Toast.makeText(MainActivity.this, getString(R.string.error_data_transfer_failed), Toast.LENGTH_LONG).show();        		
			}
		}
		
		@Override
		protected void onCancelled(ActionResult result) {
			progressDialog.dismiss();																					//Close progress dialog
			LogEx.i(TAG, "onCancelled(): Task cancelled.");
			Toast.makeText(MainActivity.this, getString(R.string.info_cancelled_by_user), Toast.LENGTH_SHORT).show();
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
			
		}
		
		
		//Force cancel from outer class
		public void forceCancelTask() {
			progressDialog.dismiss();																					//Close wait dialog
			if (this.getStatus() != Status.FINISHED) {																	//If running or pending
				this.cancel(true);																						//Force cancel
			}
		}
	
	} //SubmitQueryTask
    
	    
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_mode_training:
				Intent intent = new Intent(MainActivity.this, TrainingActivity.class);						
				startActivity(intent);																		//Start application instance in training mode
			
				finish();																					//Terminate normal mode application instance
				return true;
			
			case R.id.menu_settings:
				Intent settings = new Intent(MainActivity.this, SettingsActivity.class);						
				startActivity(settings);																	//Show settings                
				return true;
			
			case R.id.menu_about:
				Intent about = new Intent(MainActivity.this, AboutActivity.class);						
				startActivity(about);																		//Show about screen                
				return true;
			            
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	
	
	//Asynchronous task for compressing image
	private class CompressImageTask extends AsyncTask<CompressParams, Void, Boolean> {
		
		private static final String TAG = "CompressImageTask";
		private ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);  
		
		
		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(getString(R.string.info_compressing_image));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.show();
		}
		
		@Override
		protected Boolean doInBackground(CompressParams... params) {
			boolean result = false;
				
			try {

				CompressParams compressParams = params[0];  															//Get data from call parameters
				LogEx.i(TAG, String.format("doInBackground(): Compressing image %s", compressParams.toString()));
				
				long sizeBefore = Utility.getFileSize(compressParams.getImageFile());									//Read image size before compression
				result = Utility.compressImage(compressParams.getImageFile(), compressParams.getImageQuality());		//Compress image with given quality
				long sizeAfter = Utility.getFileSize(compressParams.getImageFile());									//Read image size after compression
				
				if (result == true) {
					LogEx.i(TAG, String.format("doInBackground(): Image compression succeeded (size before: %d, size after: %d)", sizeBefore, sizeAfter));
				} else {
					LogEx.i(TAG, String.format("doInBackground(): Image compression failed (size before: %d, size after: %d)", sizeBefore, sizeAfter));
				}
				
			} catch (Exception ex) {
				LogEx.e(TAG, ex.toString(), ex);
			}
			
			return result;																								//Return result
		}      

		@Override
		protected void onPostExecute(Boolean result) {
			progressDialog.dismiss();																					//Close progress dialog
			LogEx.i(TAG, "onPostExecute(): Task completed.");
			
			setUIState(1);																								//Set new UI state
			if (result == false) {
				Toast.makeText(MainActivity.this, getString(R.string.error_image_compression_failed), Toast.LENGTH_LONG).show();
			}
		}
	
		@Override
		protected void onCancelled(Boolean result) {
			progressDialog.dismiss();																					//Close progress dialog        
			LogEx.i(TAG, "onCancelled(): Task cancelled.");
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
	    
	} //CompressImageTask  

} //MainActivity