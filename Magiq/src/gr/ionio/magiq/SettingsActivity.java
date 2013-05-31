package gr.ionio.magiq;

import gr.ionio.magiq.AppCommon.Keys;
import gr.ionio.magiq.AppCommon.Utility;
import gr.ionio.magiq.AppLogger.LogEx;

import java.util.Hashtable;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class SettingsActivity extends Activity implements OnItemSelectedListener {

	private static final String TAG = "SettingsActivity";										//Log entry tag
	private Hashtable<String, String> loadedValues = new Hashtable<String, String>();			//Keep loaded values until exit
	
	private static final int IMCS_COMP_CORREL = 0;
	private static final int IMCS_COMP_CHISQR = 1;
	private static final int IMCS_COMP_INTERSECT = 2;
	private static final int IMCS_COMP_BHATTACHARYYA = 3;
	private static final int IMCS_COMP_EMD_L1 = 4;
	private static final int IMCS_COMP_EMD_L2 = 5;
	
	private static final int IMCS_RES_TYPE_VALUE = 1;
	private static final int IMCS_RES_TYPE_PERCENT = 100;
	
	private static final int IMCS_RF_MIN_EMD = 8;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
	
		//Setup Compare Method spinner
		SpinnerItem compareMethods[] = {
			new SpinnerItem("Correlation",            IMCS_COMP_CORREL,        IMCS_RES_TYPE_PERCENT),
			new SpinnerItem("Chi-square",             IMCS_COMP_CHISQR,        IMCS_RES_TYPE_VALUE),
			new SpinnerItem("Intersection",           IMCS_COMP_INTERSECT,     IMCS_RES_TYPE_PERCENT),
			new SpinnerItem("Bhattacharyya distance", IMCS_COMP_BHATTACHARYYA, IMCS_RES_TYPE_PERCENT),
			new SpinnerItem("EMD Manhattan distance", IMCS_COMP_EMD_L1,        IMCS_RES_TYPE_VALUE),
			new SpinnerItem("EMD Euclidean distance", IMCS_COMP_EMD_L2,        IMCS_RES_TYPE_VALUE)
		};
	
		ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<SpinnerItem>(this, android.R.layout.simple_spinner_item, compareMethods);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		Spinner spinr = (Spinner)findViewById(R.id.spinCompareMethod);
		spinr.setAdapter(adapter);		
		spinr.setOnItemSelectedListener(this);
	
	
		//Setup Reduce Factor spinner
		SpinnerItem reduceFactors[] = {
			new SpinnerItem("None",  0),
			new SpinnerItem("2 per channel",  2),
			new SpinnerItem("4 per channel",  4),
			new SpinnerItem("8 per channel",  8),
			new SpinnerItem("16 per channel",  16),
			new SpinnerItem("32 per channel",  32),
			new SpinnerItem("64 per channel",  64)
		};
		
		adapter = new ArrayAdapter<SpinnerItem>(this, android.R.layout.simple_spinner_item, reduceFactors);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		spinr = (Spinner)findViewById(R.id.spinReduceFactor);
		spinr.setAdapter(adapter);		
		spinr.setOnItemSelectedListener(this);
	}

	
	@Override
	protected void onStart() {
		super.onStart();
		Resources res = getResources();
		
		//Host
		String defString = getString(R.string.default_host);
		String valString = Utility.getPersistedString(this, Keys.Host, defString);
		EditText txt = (EditText)this.findViewById(R.id.textHost);
		txt.setText(valString);
		loadedValues.put(Keys.Host, txt.getText().toString());
		
		
		//Connection timeout
		int defInteger = res.getInteger(R.integer.default_connection_timeout);
		int valInteger = Utility.getPersistedInteger(this, Keys.ConnectionTimeout, defInteger);
		txt = (EditText)this.findViewById(R.id.textConnTimeout);
		txt.setText(String.valueOf(valInteger));	
		loadedValues.put(Keys.ConnectionTimeout, txt.getText().toString());
		
		
		//Enable logging
		boolean defBoolean = res.getBoolean(R.bool.default_enable_logging);
		boolean valBoolean = Utility.getPersistedBoolean(this, Keys.EnableLogging, defBoolean);
		CheckBox chk = (CheckBox)this.findViewById(R.id.chkEnableLogging);
		chk.setChecked(valBoolean);
		LogEx.setEnabled(chk.isChecked());
		
		
		//Enable image compression
		defBoolean = res.getBoolean(R.bool.default_image_compression);
		valBoolean = Utility.getPersistedBoolean(this, Keys.ImageCompression, defBoolean);
		chk = (CheckBox)this.findViewById(R.id.chkImageCompression);
		chk.setChecked(valBoolean);
		
		
		//Compressed image quality
		defInteger = res.getInteger(R.integer.default_image_quality);
		valInteger = Utility.getPersistedInteger(this, Keys.ImageQuality, defInteger); 
		txt = (EditText)this.findViewById(R.id.textImageQuality);
		txt.setText(String.valueOf(valInteger));
		txt.setEnabled(chk.isChecked());
		loadedValues.put(Keys.ImageQuality, txt.getText().toString());
		
		
		//Max image comparisons
		defInteger = res.getInteger(R.integer.default_max_tests);
		valInteger = Utility.getPersistedInteger(this, Keys.MaxTests, defInteger);
		txt = (EditText)this.findViewById(R.id.textMaxTests);
		txt.setText(String.valueOf(valInteger));	
		loadedValues.put(Keys.MaxTests, txt.getText().toString());
		
		
		//Compare method
		defInteger = res.getInteger(R.integer.default_compare_method);
		valInteger = Utility.getPersistedInteger(this, Keys.CompareMethod, defInteger);
		Spinner spinr = (Spinner)this.findViewById(R.id.spinCompareMethod);
		spinr.setSelection(getIndexOfValue(spinr, valInteger));
		
		
		//Reduce factor
		defInteger = res.getInteger(R.integer.default_reduce_factor);
		valInteger = Utility.getPersistedInteger(this, Keys.ReduceFactor, defInteger);
		spinr = (Spinner)this.findViewById(R.id.spinReduceFactor);
		spinr.setSelection(getIndexOfValue(spinr, valInteger));
		
		
		//Results threshold
		defString = getString(R.string.default_threshold);
		valString = Utility.getPersistedString(this, Keys.Threshold, defString);
		txt = (EditText)this.findViewById(R.id.textThreshold);
		txt.setText(valString);
		loadedValues.put(Keys.Threshold, txt.getText().toString());	
	}
	
	
	@Override
	protected void onStop() {
		super.onStop();
		
		//Host
		EditText txt = (EditText)this.findViewById(R.id.textHost);
		String valString = validateTextSetting(Keys.Host, txt.getText().toString());
		Utility.setPersistedString(this, Keys.Host, valString);
		
		
		//Connection timeout
		txt = (EditText)this.findViewById(R.id.textConnTimeout);
		valString = validateTextSetting(Keys.ConnectionTimeout, txt.getText().toString());
		Utility.setPersistedInteger(this, Keys.ConnectionTimeout, Integer.parseInt(valString));
		
		
		//Enable logging
		CheckBox chk = (CheckBox)this.findViewById(R.id.chkEnableLogging);
		Utility.setPersistedBoolean(this, Keys.EnableLogging, chk.isChecked());	
		LogEx.setEnabled(chk.isChecked());
		
		
		//Enable image compression
		chk = (CheckBox)this.findViewById(R.id.chkImageCompression);
		Utility.setPersistedBoolean(this, Keys.ImageCompression, chk.isChecked());	
		
		
		//Compressed image quality
		txt = (EditText)this.findViewById(R.id.textImageQuality);
		valString = validateTextSetting(Keys.ImageQuality, txt.getText().toString());
		Utility.setPersistedInteger(this, Keys.ImageQuality, Integer.parseInt(valString));
		
		
		//Max image comparisons
		txt = (EditText)this.findViewById(R.id.textMaxTests);
		valString = validateTextSetting(Keys.MaxTests, txt.getText().toString());
		Utility.setPersistedInteger(this, Keys.MaxTests, Integer.parseInt(valString));
		
		
		//Compare method
		Spinner spinr = (Spinner)this.findViewById(R.id.spinCompareMethod);
		SpinnerItem valItem = (SpinnerItem)spinr.getSelectedItem(); 
		Utility.setPersistedInteger(this, Keys.CompareMethod, valItem.getValue());
		
		
		//Reduce factor
		spinr = (Spinner)this.findViewById(R.id.spinReduceFactor);
		valItem = (SpinnerItem)spinr.getSelectedItem();
		Utility.setPersistedInteger(this, Keys.ReduceFactor, valItem.getValue());
		
		
		//Results threshold
		txt = (EditText)this.findViewById(R.id.textThreshold);
		valString = validateTextSetting(Keys.Threshold, txt.getText().toString());
		Utility.setPersistedString(this, Keys.Threshold, valString);
	}

	
	//Validate Keys values
	private String validateTextSetting(String key, String value) {
			
		if ((key.equals(Keys.Host) || key.equals(Keys.ConnectionTimeout) || key.equals(Keys.MaxTests)) && value.equals("")) {
			return loadedValues.get(key);
		} 
		
		if ((key.equals(Keys.ConnectionTimeout) || key.equals(Keys.MaxTests)) && Integer.parseInt(value) == 0) {
			return loadedValues.get(key);
		} 
		
		if ((key.equals(Keys.ImageQuality)) && (Integer.parseInt(value) < 30 || Integer.parseInt(value) > 90)) {
			return loadedValues.get(key);
		} 
		
		if (key.equals(Keys.Threshold)) {		
			Spinner spinnerMethod = (Spinner)this.findViewById(R.id.spinCompareMethod);
			SpinnerItem selectedMethod = (SpinnerItem)spinnerMethod.getSelectedItem();
			
			if ((selectedMethod.getResType() == IMCS_RES_TYPE_VALUE) && (Float.parseFloat(value) < 0.0)) {
				return loadedValues.get(key);
			}
			
			if ((selectedMethod.getResType() == IMCS_RES_TYPE_PERCENT) && (Float.parseFloat(value) < 0.0 || Float.parseFloat(value) >= 1.0)) {
				return loadedValues.get(key);
			}			
		}
		
		return value;
	}
	
	
	//Check box click handler
	public void onCheckboxClicked(View view) {
		CheckBox chk = (CheckBox)view;		
		switch (chk.getId()) {
			case R.id.chkImageCompression:
				EditText txt = (EditText)this.findViewById(R.id.textImageQuality);
				txt.setEnabled(chk.isChecked());
				break;
		}
	}


	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		try {
			
			switch(parent.getId()) {													//Check spinner
				case R.id.spinCompareMethod: 
				{	
					Spinner spinnerMethod = (Spinner)parent;
					SpinnerItem selectedMethod = (SpinnerItem)spinnerMethod.getSelectedItem();
					
					Spinner spinnerFactor = (Spinner)this.findViewById(R.id.spinReduceFactor);	
					SpinnerItem selectedFactor = (SpinnerItem)spinnerFactor.getSelectedItem();
					
					if (selectedMethod.getValue() == IMCS_COMP_EMD_L1 || selectedMethod.getValue() == IMCS_COMP_EMD_L2) {
						if (selectedFactor.getValue() < IMCS_RF_MIN_EMD) {
							spinnerFactor.setSelection(getIndexOfValue(spinnerFactor, IMCS_RF_MIN_EMD));
						}	
					}
					
					TextView txvThreshold = (TextView)this.findViewById(R.id.labelThreshold);
					if (selectedMethod.getResType() == IMCS_RES_TYPE_PERCENT) {
						txvThreshold.setText(String.format(getString(R.string.setting_threshold), "over"));
					} else {
						txvThreshold.setText(String.format(getString(R.string.setting_threshold), "less than"));
					}					
					break;
				}
					
				case R.id.spinReduceFactor: 
				{
					Spinner spinnerMethod = (Spinner)this.findViewById(R.id.spinCompareMethod);
					SpinnerItem selectedMethod = (SpinnerItem)spinnerMethod.getSelectedItem();
					
					Spinner spinnerFactor = (Spinner)parent;
					SpinnerItem selectedFactor = (SpinnerItem)spinnerFactor.getSelectedItem();
					
					if (selectedMethod.getValue() == IMCS_COMP_EMD_L1 || selectedMethod.getValue() == IMCS_COMP_EMD_L2) {
						if (selectedFactor.getValue() < IMCS_RF_MIN_EMD) {
							spinnerFactor.setSelection(getIndexOfValue(spinnerFactor, IMCS_RF_MIN_EMD));
						}
					}
					break;
				}
			}
			
		} catch(Exception ex) {
			LogEx.e(TAG, ex.toString(), ex);		
			Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_LONG).show();					
		}		
	}


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	
	}
	
	
	@SuppressWarnings("unchecked")
	private int getIndexOfValue(Spinner spinr, int value) {
		
		ArrayAdapter<SpinnerItem> adapter = (ArrayAdapter<SpinnerItem>)spinr.getAdapter();
		for(int i = 0; i < adapter.getCount(); i++) {
			SpinnerItem item = (SpinnerItem)adapter.getItem(i);
			if (item.getValue() == value) {
				return i;	
			}	        
		}
		
		return 0;
	}
	
	
	//Data holder class for Spinner items
	private final class SpinnerItem {
		
		private String description = "";
		private int value = 0;
		private int resType = IMCS_RES_TYPE_VALUE;
		
		
		public SpinnerItem(String description, int value) {
			this.description = description;
			this.value = value;
		}
		
		public SpinnerItem(String description, int value, int resType) {
			this.description = description;
			this.value = value;
			this.resType = resType;
		}
		
		
		@SuppressWarnings("unused")
		public String getDescription() {
			return this.description;
		}
		
		@SuppressWarnings("unused")
		public void setDescription(String description) {
			this.description = description;
		}
		
		
		public int getValue() {
			return this.value;
		}
		
		@SuppressWarnings("unused")
		public void setValue(int value) {
			this.value = value;
		}
		
		
		public int getResType() {
			return this.resType;
		}
		
		@SuppressWarnings("unused")
		public void ResType(int resType) {
			this.resType = resType;
		}
		
		
		//Used by ArrayAdapter
		public String toString() {
			return this.description;
		}
			
	} //SpinnerItem

	
} //SettingsActivity