package gr.ionio.magiq;

import gr.ionio.magiq.AppCommon.Entities;
import gr.ionio.magiq.AppCommon.Keys;
import gr.ionio.magiq.AppCommon.ServerSide;
import gr.ionio.magiq.AppCommon.Utility;
import gr.ionio.magiq.AppLogger.LogEx;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

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

public class TrainingActivity extends Activity implements DataHandlerListener {
	
	private static final String TAG = "TrainingActivity";
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	
	private int uiState = 0;																				//UI state
	private String imageFile = "";																			//Image file
	private DataHandler dataHandler = null;																	//SQLite local database
	private LocationHandler locationHandler = new LocationHandler(this);									//Location handler instance
	private ConnectivityHandler connectivityHandler = new ConnectivityHandler(this);						//Network connectivity handler instance
	
	private SubmitDataTask taskSubmit = null;																//Asynchronous task reference
	private CompressImageTask taskCompress = null;															//Asynchronous task reference
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupLogger();        																				//Setup logger
		setContentView(R.layout.activity_training);															//Setup layout
		setUIState(0);																						//Initialize UI state
		
		dataHandler = new DataHandler(this, this);															//SQLite local database
		dataHandler.countRecords();																			//Create the SQLite local database if it doesn't exist and count records
	}
    
 
	@Override
	protected void onStart() {
		super.onStart();
		locationHandler.onStart();																			//Start location monitoring
	}
    
 
	@Override
	protected void onRestoreInstanceState(Bundle instanceState) {
		super.onRestoreInstanceState(instanceState);
		
		imageFile = Utility.getString(instanceState.getString(Keys.ImageFile), "");							//Read saved image file
		uiState = instanceState.getInt(Keys.UIState, 0);													//Read saved UI state code
		setUIState(uiState);																				//Restore UI to saved state
	} 
    
	
	@Override
	protected void onSaveInstanceState(Bundle instanceState) {
		super.onSaveInstanceState(instanceState);
		
		instanceState.putString(Keys.ImageFile, imageFile);          										//Save image file
		instanceState.putInt(Keys.UIState, uiState);														//Save UI state code
	}
  

	@Override
	protected void onStop() {
		super.onStop();
		locationHandler.onStop();																			//Stop listening for location updates
		
		if (taskSubmit != null) taskSubmit.forceCancelTask();												//Terminate submit task
		if (taskCompress != null) taskCompress.forceCancelTask();											//Terminate compression task
	}
    
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		dataHandler.onDestroy();
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
					
					pane = (TableRow)this.findViewById(R.id.paneDescription);	
					pane.setVisibility(View.VISIBLE);														//Show description controls
					
					pane = (TableRow)this.findViewById(R.id.paneCoords1);	
					pane.setVisibility(View.VISIBLE);														//Show latitude controls
					
					pane = (TableRow)this.findViewById(R.id.paneCoords2);	
					pane.setVisibility(View.VISIBLE);														//Show longitude controls
					
					EditText txted = (EditText)this.findViewById(R.id.textDescription);
					txted.setText("");																		//Reset description text
					
					TextView txtv = (TextView)this.findViewById(R.id.textLatitude);
					txtv.setText(String.format("%.6f", locationHandler.getLatitude()));						//Set latitude text from location handler
					
					txtv = (TextView)this.findViewById(R.id.textLongitude);
					txtv.setText(String.format("%.6f", locationHandler.getLongitude()));					//Set longitude text from location handler
					
					Bitmap myBitmap = BitmapFactory.decodeFile(imageFile);									//Load bitmap from image file
					ImageView im = (ImageView)this.findViewById(R.id.imageMain);
					im.setImageBitmap(myBitmap);															//Load image view from bitmap
					
					Button btn = (Button)this.findViewById(R.id.buttonSave);								 
					btn.setEnabled(true);																	//Enable Save button
					break;
				}
				
				default:		//Initial state	(0)			
				{
					TableRow pane = (TableRow)this.findViewById(R.id.paneBegin);
					pane.setVisibility(View.VISIBLE);														//Show start message
					
					pane = (TableRow)this.findViewById(R.id.paneDescription);	
					pane.setVisibility(View.INVISIBLE);														//Hide description controls
					
					pane = (TableRow)this.findViewById(R.id.paneCoords1);	
					pane.setVisibility(View.INVISIBLE);														//Hide latitude controls
					
					pane = (TableRow)this.findViewById(R.id.paneCoords2);	
					pane.setVisibility(View.INVISIBLE);														//Hide longitude controls
					
					EditText txted = (EditText)this.findViewById(R.id.textDescription);
					txted.setText("");																		//Reset description text
					
					TextView txtv = (TextView)this.findViewById(R.id.textLatitude);
					txtv.setText(getString(R.string.default_coord));										//Set default value
					
					txtv = (TextView)this.findViewById(R.id.textLongitude);
					txtv.setText(getString(R.string.default_coord));										//Set default value
					
					ImageView im = (ImageView)this.findViewById(R.id.imageMain);
					im.setImageResource(android.R.color.transparent);										//Reset ImageView image
					im.setImageResource(R.drawable.no_image);												//Load default image from resource
					
					Button btn = (Button)this.findViewById(R.id.buttonSave);								 
					btn.setEnabled(false);																	//Disable Save button
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
    
    
	//Save data into local database
	public void onSaveClick(View v) {
		try {
			
			String imageFileCopy = Utility.getImageFile(this, ""); 											//Generate a file name to copy the image to
			if (Utility.copyFile(imageFile, imageFileCopy)) {												//Copy saved image to new file
			
				DataRecord dataRecord = new DataRecord();													//Create new database record instance
				dataRecord.setImageFile(imageFileCopy);														//Set image file (absolute name)
				    		
				EditText txt = (EditText)this.findViewById(R.id.textDescription);
				dataRecord.setDescription(txt.getText().toString().trim());									//Set description text
				
				TextView txv = (TextView)this.findViewById(R.id.textLatitude);
				dataRecord.setLatitude(Double.parseDouble(txv.getText().toString()));						//Set latitude
				
				txv = (TextView)this.findViewById(R.id.textLongitude);
				dataRecord.setLongitude(Double.parseDouble(txv.getText().toString()));						//Set longitude
				
				LogEx.i(TAG, String.format("onSaveClick(): Insert record %s", dataRecord.toString()));
				dataHandler.insertRecord(dataRecord);														//Insert record into SQLite local database
				
				Button btn = (Button)this.findViewById(R.id.buttonSave);								 
				btn.setEnabled(false);																		//On successful saving disable Save button to prevent duplicate records (data input mode only)
				        
				Toast.makeText(this, getString(R.string.info_data_saved), Toast.LENGTH_SHORT).show();	
			}
			
		} catch(Exception ex) {
			LogEx.e(TAG, ex.toString(), ex);		
			Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_LONG).show();					
		}
	}
    
    
	//On record count change handler
	public void onRecordcountChange(int recordCount) {
		Button btn = (Button)this.findViewById(R.id.buttonSubmit);
		if (recordCount > 0) {
			btn.setEnabled(true);
			btn.setText(String.format(getString(R.string.button_submit_number), recordCount));
		} else {
			btn.setEnabled(false);
			btn.setText(getString(R.string.button_submit));
		}
	}
    
    
    
	//Submit data
	@SuppressWarnings("unchecked")
	public void onSubmitClick(View v) {
		try {
				
			if (connectivityHandler.queryServiceStatus(ConnectionType.CONNECTION_WIFI) == true) {			//Check for WiFi connection status
				LogEx.i(TAG, "onSubmitClick(): Ready to send.");
				
				List<DataRecord> uploadData = dataHandler.getAllRecords();									//Get stored data from SQLite local database
				taskSubmit = new SubmitDataTask(); 
				taskSubmit.execute(uploadData);																//Submit data asynchronously
			
			} else {
				LogEx.i(TAG, "onSubmitClick(): Unable to send.");        			
			}
			
		} catch(Exception ex) {
			LogEx.e(TAG, ex.toString(), ex);		
			Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_LONG).show();					
		}
	}



	//Submit data to server asynchronously
	private class SubmitDataTask extends AsyncTask<List<DataRecord>, Void, ActionResult> {
	
		private static final String TAG = "SubmitDataTask";
		private ProgressDialog progressDialog = new ProgressDialog(TrainingActivity.this);
		
		
		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(getString(R.string.info_sending_data));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {							
				public void onClick(DialogInterface dialog, int id) {
					LogEx.i(TAG, "onPreExecute(): Issuing cancel request...");
					SubmitDataTask.this.cancel(true);																	//Cancel asynchronous task from progress dialog cancel button
				}
			}); 
			progressDialog.show();		
		}
		
		@Override
		protected ActionResult doInBackground(List<DataRecord>... params) {
			try {
					
				String clientID = Utility.getUUID(TrainingActivity.this);												//Read unique identifier            	
				Resources res = getResources();																			//Get resources reference
				
				String defHost = res.getString(R.string.default_host);
				String valHost = Utility.getPersistedString(TrainingActivity.this, Keys.Host, defHost);					//Get host from persisted storage
				
				int defConnTimeout = res.getInteger(R.integer.default_connection_timeout);
				int valConnTimeout = 1000 * Utility.getPersistedInteger(TrainingActivity.this, Keys.ConnectionTimeout, defConnTimeout);	//Get connection timeout from persisted storage and convert to milliseconds
				
				HttpClient httpClient = new DefaultHttpClient();       	
				HttpParams httpParams = httpClient.getParams();
				httpParams.setParameter("http.protocol.content-charset", "UTF-8");            	
				httpParams.setParameter("http.connection.timeout", Integer.valueOf(valConnTimeout));
				
				URL url = new URL("http", valHost, "/sorcerer/submitdata.php");            								//Create URL from parts (submitdata.php)
				HttpPost httpPost = new HttpPost(url.toURI());															//HTTP Post method
				httpPost.addHeader("Connection", "keep-alive");
				
				List<DataRecord> uploadData = params[0];																//Get data from call parameters
				Iterator<DataRecord> iterator = uploadData.iterator();													//Get list iterator
				
				while (isCancelled() == false && iterator.hasNext() == true) {											//Read iterator state and task cancellation flag
					DataRecord dataRecord = iterator.next();															//Read iterator item
					LogEx.i(TAG, String.format("doInBackground(): Sending %s to %s. Timeout=%dms, UID=%s", dataRecord.toString(), valHost, valConnTimeout, clientID));	
					
					boolean fileExists = Utility.queryFile(dataRecord.getImageFile());									//Check if file exists before deletion attempt
					if (fileExists == true) {  					
						
						MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
						File imageFile = new File(dataRecord.getImageFile());
						FileBody fbd = new FileBody(imageFile);															//Add file data
						mpEntity.addPart(Entities.imagedata, fbd);
						
						StringBody sbd = new StringBody(imageFile.getName());											//Add file name (stripped name from absolute name)
						mpEntity.addPart(Entities.imagefile, sbd);
						
						sbd = new StringBody(String.valueOf(dataRecord.getLatitude()));  								//Add latitude          	            	
						mpEntity.addPart(Entities.latitude, sbd);
						
						sbd = new StringBody(String.valueOf(dataRecord.getLongitude()));  								//Add longitude      	            	
						mpEntity.addPart(Entities.longitude, sbd);
						
						sbd = new StringBody(String.valueOf(dataRecord.getDescription()));  							//Add distance     	            	
						mpEntity.addPart(Entities.description, sbd);
						
						sbd = new StringBody(clientID);																	//Add client identifier            	
						mpEntity.addPart(Entities.clientid, sbd);
						
						httpPost.setEntity(mpEntity);
						HttpResponse httpResponse = httpClient.execute(httpPost);										//Upload data (blocking function)
						
						int resCode = httpResponse.getStatusLine().getStatusCode();										//Get response status code            	
						String resReason = httpResponse.getStatusLine().getReasonPhrase();								//Get response description
						LogEx.i(TAG, String.format("Server returned code: %s (%s)", resCode, resReason));				//Report server response
						
						if (resCode != 200) {																			//Accept only code 200 as request fulfilled 
							return new ActionResult(ActionStatus.ACTION_STATUS_ERROR, resReason);						//Return ERROR status to onPostExecute() (HTTP error)
						}
						
						HttpEntity httpEntity = httpResponse.getEntity(); 												//Get response body
						if (httpEntity == null) {
							return new ActionResult(ActionStatus.ACTION_STATUS_ERROR, "No message body");				//Return ERROR status to onPostExecute() (No message body)
						}
						
						String resBody = EntityUtils.toString(httpEntity);     											//Get response body
						LogEx.i(TAG, String.format("Server replied: %s", resBody));
						
						String remoteID = ServerSide.parseID(resBody);													//Read returned ID (not null indicates success)
						if (remoteID == null) {
							String remoteError = ServerSide.parseError(resBody);										//Read returned error
							if (remoteError == null) {
								return new ActionResult(ActionStatus.ACTION_STATUS_ERROR, "Unexpected server resonse");	//Return ERROR status to onPostExecute() (Unknown server side error)
							} else {
								return new ActionResult(ActionStatus.ACTION_STATUS_ERROR, remoteError);					//Return ERROR status to onPostExecute() (Specific server side error)
							}
						}
					}
					
					boolean fileDeleted = Utility.deleteFile(dataRecord.getImageFile());								//Delete file and return result
					if (fileExists == false || fileDeleted == true) {													//Check if the file did not exist on SD card (orphan database record) OR it was successfully deleted... 
						dataHandler.deleteRecord(dataRecord);															//Proceed to delete record from SQLite local database
					}
				}
				
				return new ActionResult(ActionStatus.ACTION_STATUS_OK);													//Return OK status to onPostExecute()
				
			} catch (Exception ex) {
				LogEx.e(TAG, ex.toString(), ex);
				return new ActionResult(ActionStatus.ACTION_STATUS_ERROR, ex.toString());								//Return ERROR status to onPostExecute() (exception occurred)
			}
		}      
        
		@Override
		protected void onPostExecute(ActionResult result) {
			progressDialog.dismiss();																					//Close progress dialog
			LogEx.i(TAG, "onPostExecute(): Task completed.");         
			
			dataHandler.countRecords();																					//Count records and update UI accordingly
			if (result.getStatus() == ActionStatus.ACTION_STATUS_OK) {													//Check submit data status
				Toast.makeText(TrainingActivity.this, getString(R.string.info_task_completed), Toast.LENGTH_SHORT).show();        	
			} else {
				Toast.makeText(TrainingActivity.this, getString(R.string.error_data_transfer_failed), Toast.LENGTH_LONG).show();        		
			}
		}
		
		@Override
		protected void onCancelled(ActionResult result) {
			progressDialog.dismiss();																					//Close progress dialog
			LogEx.i(TAG, "onCancelled(): Task cancelled.");
			
			dataHandler.countRecords();																					//Count records and update UI accordingly
			Toast.makeText(TrainingActivity.this, getString(R.string.info_cancelled_by_user), Toast.LENGTH_SHORT).show();
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
	
	} //SubmitDataTask  
    
	    
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.activity_training, menu);
		return true;
	}
	    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_mode_normal:
				Intent intent = new Intent(TrainingActivity.this, MainActivity.class);
				startActivity(intent);																		//Start application instance in normal mode
			
				finish();																					//Terminate training mode application instance
				return true;
			
			case R.id.menu_settings:
				Intent settings = new Intent(TrainingActivity.this, SettingsActivity.class);						
				startActivity(settings);																	//Show settings                
				return true;
			
			case R.id.menu_about:
				Intent about = new Intent(TrainingActivity.this, AboutActivity.class);						
				startActivity(about);																		//Show about screen                
				return true;
			            
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	
	
	//Asynchronous task for compressing image
	private class CompressImageTask extends AsyncTask<CompressParams, Void, Boolean> {
		
		private static final String TAG = "CompressImageTask";
		private ProgressDialog progressDialog = new ProgressDialog(TrainingActivity.this);  
		
		
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
				Toast.makeText(TrainingActivity.this, getString(R.string.error_image_compression_failed), Toast.LENGTH_LONG).show();
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

} //TrainingActivity