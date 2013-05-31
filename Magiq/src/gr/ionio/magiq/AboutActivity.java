package gr.ionio.magiq;

import gr.ionio.magiq.AppLogger.LogEx;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.app.Activity;

public class AboutActivity extends Activity {

	private static final String TAG = "AboutActivity";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
	
		try {
			
			WebView wbv = (WebView)findViewById(R.id.webAbout);
			WebSettings wbvs = wbv.getSettings(); 
			wbvs.setBuiltInZoomControls(false);																//Suppress built in zoom controls (they get into the way and interfere with scale setting
			wbv.setInitialScale(100);																		//Set web page scale to 100%
			wbv.setWebViewClient(new WebViewClient(){														//Create a WebClient object instance to modify WebView behavior  
		
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url){							//Set the WebView instance as the default viewer for ALL web pages
					view.loadUrl(url);																		//Open subsequent hyper links into this web view control
					return true;
				}
			
				@Override
				public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
					LogEx.e(TAG, String.format("Error %d: %s, url:&s", errorCode, description, failingUrl));
				}
			    
			});
			
			wbv.loadUrl("file:///android_asset/about.html"); 
		
		} catch (Exception ex) {
			LogEx.e(TAG, ex.toString(), ex);			
			Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_LONG).show();					
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		WebView wbv = (WebView)findViewById(R.id.webAbout);
		wbv.restoreState(savedInstanceState);
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		WebView wbv = (WebView)findViewById(R.id.webAbout);
		wbv.saveState(outState);
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
		WebView wbv = (WebView) findViewById(R.id.webAbout);
		if (wbv.isFocused() && wbv.canGoBack()) {
			wbv.goBack();       
		} else {
		super.onBackPressed();
		}
	}
    
    
} //AboutActivity
