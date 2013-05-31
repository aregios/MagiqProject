package gr.ionio.magiq;

import gr.ionio.magiq.AppLogger.LogEx;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


public class ResultsActivity extends Activity {

	private static final String TAG = "ResultsActivity";
	public static final String ParamRequestURL = "RequestURL";
	private WaitDataTask taskWait = null;																	//Asynchronous task reference
	
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_results);
		
		try {
		    	
			Intent intent = getIntent();																	//Get caller intent
			String requestURL = intent.getStringExtra(ParamRequestURL);										//Get caller parameters (the results URL)
			LogEx.i(TAG, String.format("onCreate(): request URL = %s", requestURL));        	
			
			WebView wbv = (WebView)findViewById(R.id.webResults);
			WebSettings wbvs = wbv.getSettings();
			wbvs.setJavaScriptEnabled(true);																//Allow Javascript execution within web page 
			wbvs.setBuiltInZoomControls(false);																//Suppress built in zoom controls (they get into the way and interfere with scale setting
			wbv.setInitialScale(100);																		//Set web page scale to 100%
			wbv.setWebViewClient(new WebViewClient(){														//Create a WebClient object instance to modify WebView behavior  
			
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url){							//Set the WebView instance as the default viewer for ALL web pages
					view.loadUrl(url);																		//Open subsequent hyper links into this web view control
					return true;
				}
				
				@Override
				public void onPageFinished(WebView view, String url) {										//Intercept web page loading completion
					if (taskWait != null) taskWait.forceCancelTask();										//Terminate wait task
				}
				
				@Override
				public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
					LogEx.e(TAG, String.format("Error %d: %s, url:&s", errorCode, description, failingUrl));
				}
				    
			});
			
			wbv.loadUrl(requestURL);            
			taskWait = new WaitDataTask();																	//Create new task instance
			taskWait.execute();																				//Launch asynchronous task and return
				
		} catch (Exception ex) {
			LogEx.e(TAG, ex.toString(), ex);			
			Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_LONG).show();					
		}
	}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		WebView wbv = (WebView)findViewById(R.id.webResults);
		wbv.restoreState(savedInstanceState);
	}

    
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		WebView wbv = (WebView)findViewById(R.id.webResults);
		wbv.saveState(outState);
	}
    
    
	@Override
	protected void onStop() {
		super.onStop();
		if (taskWait != null) taskWait.forceCancelTask();													//Terminate wait task
	}

    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(this);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

    
	@Override
	public void onBackPressed () {																			//Change back button behavior (browsing history back)
		WebView wbv = (WebView) findViewById(R.id.webResults);
		if (wbv.isFocused() && wbv.canGoBack()) {
			wbv.goBack();       
		} else {
			super.onBackPressed();
		}
	}
    
    
    
	//Asynchronous task for waiting results web page
	private class WaitDataTask extends AsyncTask<Void, Void, Void> {
	
		private static final String TAG = "WaitDataTask";	
		private ProgressDialog progressDialog = new ProgressDialog(ResultsActivity.this);  
		
		
		@Override
		protected void onPreExecute() {                                      //****
			progressDialog.setMessage(ResultsActivity.this.getString(R.string.info_waiting_results));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, ResultsActivity.this.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {							
				public void onClick(DialogInterface dialog, int id) {
					LogEx.i(TAG, "onPreExecute(): Issuing cancel request...");
					WaitDataTask.this.cancel(true);															//Cancel asynchronous task from progress dialog cancel button
				}
			}); 
			progressDialog.show();
		}
		
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				
				LogEx.i(TAG, "doInBackground(): Waiting for results...");
				while (isCancelled() == false) {}															//Loop waiting
					
			} catch (Exception ex) {
				LogEx.e(TAG, ex.toString(), ex);
			}
			
			return null;
		}      
		
		
		@Override
		protected void onPostExecute(Void result) {
			progressDialog.dismiss();																		//Close progress dialog
			LogEx.i(TAG, "onPostExecute(): Results page loaded.");
		}
		
		@Override
		protected void onCancelled(Void result) {
			progressDialog.dismiss();																		//Close progress dialog        	
			WebView wbv = (WebView)findViewById(R.id.webResults); 
			wbv.stopLoading();																				//Cancel page loading
			LogEx.i(TAG, "onCancelled(): Results page cancelled.");
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
		
		}
		
		
		//Force cancel from outer class
		public void forceCancelTask() {
			progressDialog.dismiss();																		//Close progress dialog
			if (this.getStatus() != Status.FINISHED) {														//Check if running or pending
				this.cancel(true);																			//Force cancel
			}
		}
	    
	} //WaitDataTask
    

} //ResultsActivity