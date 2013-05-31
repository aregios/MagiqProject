package gr.ionio.magiq;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;

public final class AppCommon {

	
	//Settings Keys
	public static class Keys {		
		public static final String UIState = "UIState";
		public static final String ImageFile = "ImageFile";
		public static final String Distance = "Distance";

		public static final String MaxTests = "MaxTests";		
		public static final String CompareMethod = "CompareMethod";
		public static final String ReduceFactor = "ReduceFactor";
		public static final String Threshold = "Threshold";		
		
		public static final String UUID ="UUID";
		public static final String Host = "Host";
		public static final String ConnectionTimeout = "ConnectionTimeout";		
		public static final String EnableLogging = "EnableLogging";
		public static final String ImageCompression = "ImageCompression";
		public static final String ImageQuality = "ImageQuality";
		
		public static final String QueryLocalID = "QueryLocalID";
		public static final String QueryRemoteID = "QueryRemoteID";
	}
	
	
	//Helper methods
	public static class Utility {

		private static final String MEDIA_MOUNTED = "mounted" ; 
		private static final String DIRECTORY_PICTURES = "Pictures";
		
		private static final String PREFERENCES = "Shared";
		private static final int MODE_PRIVATE = 0; 		
		
		
		//Get application unique ID
		public static String getUUID(Activity hostActivity) {
			String uniqueID = getPersistedString(hostActivity, Keys.UUID, null);
			if (uniqueID == null) {
				uniqueID = UUID.randomUUID().toString();
				setPersistedString(hostActivity, Keys.UUID, uniqueID);
			}
			return uniqueID;
		}

		
		
		//Get string from persisted storage (Shared across Activities)
		public static String getPersistedString(Activity hostActivity, String key, String defaultValue) {
			SharedPreferences settings = hostActivity.getSharedPreferences(PREFERENCES, MODE_PRIVATE);
			if (settings != null) {
				return settings.getString(key, defaultValue);
			} else { 
				return defaultValue;		        			        
			}
		}
		
		//Set string into persisted storage (Shared across Activities)
		public static void setPersistedString(Activity hostActivity, String key, String value) {		
			SharedPreferences settings = hostActivity.getSharedPreferences(PREFERENCES, MODE_PRIVATE);
			if (settings != null) {
				SharedPreferences.Editor editor = settings.edit();
			    editor.putString(key, value);
			    editor.commit();
			}
		}

		
		//Get integer value from persisted storage (Shared across Activities)
		public static int getPersistedInteger(Activity hostActivity, String key, int defaultValue) {
			SharedPreferences settings = hostActivity.getSharedPreferences(PREFERENCES, MODE_PRIVATE);
			if (settings != null) {
				return settings.getInt(key, defaultValue);
			} else { 
				return defaultValue;		        			        
			}
		}
		
		//Set integer value into persisted storage (Shared across Activities)
		public static void setPersistedInteger(Activity hostActivity, String key, int value) {		
			SharedPreferences settings = hostActivity.getSharedPreferences(PREFERENCES, MODE_PRIVATE);
			if (settings != null) {
				SharedPreferences.Editor editor = settings.edit();
			    editor.putInt(key, value);
			    editor.commit();
			}
		}

		
		//Get float value from persisted storage (Shared across Activities)
		public static float getPersistedFloat(Activity hostActivity, String key, float defaultValue) {
			SharedPreferences settings = hostActivity.getSharedPreferences(PREFERENCES, MODE_PRIVATE);
			if (settings != null) {
				return settings.getFloat(key, defaultValue);
			} else { 
				return defaultValue;		        			        
			}
		}
		
		//Set float value into persisted storage (Shared across Activities)
		public static void setPersistedFloat(Activity hostActivity, String key, float value) {		
			SharedPreferences settings = hostActivity.getSharedPreferences(PREFERENCES, MODE_PRIVATE);
			if (settings != null) {
				SharedPreferences.Editor editor = settings.edit();
			    editor.putFloat(key, value);
			    editor.commit();
			}
		}

		
		//Get boolean value from persisted storage (Shared across Activities)
		public static boolean getPersistedBoolean(Activity hostActivity, String key, boolean defaultValue) {
			SharedPreferences settings = hostActivity.getSharedPreferences(PREFERENCES, MODE_PRIVATE);
			if (settings != null) {
				return settings.getBoolean(key, defaultValue);
			} else { 
				return defaultValue;		        			        
			}
		}
		
		//Set boolean value into persisted storage (Shared across Activities)
		public static void setPersistedBoolean(Activity hostActivity, String key, boolean value) {		
			SharedPreferences settings = hostActivity.getSharedPreferences(PREFERENCES, MODE_PRIVATE);
			if (settings != null) {
				SharedPreferences.Editor editor = settings.edit();
			    editor.putBoolean(key, value);
			    editor.commit();
			}
		}


		
		//Queries for the existence of an action on the system
		//Original version taken from http://android-developers.blogspot.gr/2009/01/can-i-use-this-intent.html
		public static boolean isIntentAvailable(Activity hostActivity, String action) {
		    PackageManager packageManager = hostActivity.getPackageManager();
		    Intent intent = new Intent(action);
		    List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		    return (list.size() > 0);
		}
		
		
		
		//Returns an absolute file name for saving an image - if passed empty string as name it auto-generates name
		public static String getImageFile(Activity hostActivity, String fileName) {				
			
			//Check external storage state
			if (Environment.getExternalStorageState().equals(MEDIA_MOUNTED) == false) {
				return "";
			}
			
			//Specify directory name (External storage must be mounted AND application must have WRITE_EXTERNAL_STORAGE permission)
			File imagePath = Utility.getExternalFilesDir(hostActivity, DIRECTORY_PICTURES);   
			if (imagePath == null) {
				return "";
			}
			
			//Create directory structure
			if (imagePath.exists() == false) {     
			    if (imagePath.mkdirs() == false) { 
			    	return "";
			    }
			}
			
			//Generate file name 			
			if (fileName.equals("")) {
				fileName = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH).format(new Date());
			}
			
			//Return absolute file name			
			File imageFile = new File(imagePath, fileName.concat(".jpg"));
			return imageFile.getAbsolutePath();	
		}

		
		
		//Compress a JPEG image 
		public static boolean compressImage(String fileName, int quality) {
			String fileNameTmp = String.format("%s.tmp", fileName);
			File fileOut = new File(fileNameTmp);
			
			try {
				FileOutputStream fsOut = new FileOutputStream(fileOut);	
				Bitmap image = BitmapFactory.decodeFile(fileName);
				image.compress(Bitmap.CompressFormat.JPEG, quality, fsOut);
				fsOut.flush();
				fsOut.close();
			
			} catch (Exception ex) {
				return false;
			}
			
			if (fileOut.exists() == false) {
				return false;
			}
			
			File fileIn = new File(fileName);
			if (fileIn.exists() == false || fileIn.delete() == false) {
				return false;
			}
			
			return fileOut.renameTo(fileIn);
		}
		
		
		
		//Return a file's URI
		public static Uri convertFiletoUri(String fileName) {
			return Uri.fromFile(new File(fileName));
		}
		
		
		
		//Copy a file 
		public static boolean copyFile(String fileSource, String fileTarget) throws FileNotFoundException, IOException {
			boolean retvalue = false;
			FileInputStream fsin = null;
			FileOutputStream fsout = null;
			
			try {
				fsin = new FileInputStream(fileSource);
				fsout = new FileOutputStream(fileTarget);
			
				byte[] buffer = new byte[1024];
			    int length;
			    while ((length = fsin.read(buffer)) > 0) {
			        fsout.write(buffer, 0, length);
			    }
			    fsout.flush();
				retvalue = true;
			}
			catch (Exception ex) { 
				retvalue = false;
			}
			finally {
				if (fsin != null)
					fsin.close();
				
				if (fsout != null)
					fsout.close();				
			}
			
			return retvalue;
		}
		
		
		
		//Delete a file
		public static boolean deleteFile(String fileName) {
			boolean retvalue = false;
			try {
				File file = new File(fileName);
				retvalue = file.delete();
			}
			catch (Exception ex) { 
				retvalue = false;
			}
			return retvalue;
		}
			


		//Check if a file exists
		public static boolean queryFile(String fileName) {
			boolean retvalue = false;
			try {
				File file = new File(fileName);
				retvalue = file.exists();
			}
			catch (Exception ex) { 
				retvalue = false;
			}
			return retvalue;
		}
		
	
		
		//Return file size
		public static long getFileSize(String fileName) {
			File file = new File(fileName);
			return file.length();
		}
	
		
		
		//Get the screen's width (used into results web page - returns smaller dimension)
		public static int getScreenWidth(Activity hostActivity) {					
			DisplayMetrics displaymetrics = new DisplayMetrics();
			hostActivity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			int screenHeight = displaymetrics.heightPixels;
			int screenWidth = displaymetrics.widthPixels;			
			return (screenWidth <= screenHeight) ? screenWidth : screenHeight;  
		}

		
		
		//Check for null and if so return default value. Used for compatibility in platforms < API 10
		public static String getString(String value, String defaultValue) {
			if (value != null && value.equals("") == false )
				return value;
			else
				return defaultValue;
		}
		
		
		
		//Return external files directory. Used for compatibility in platforms with API < 8
		public static File getExternalFilesDir(Activity hostActivity, String type) {
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
				return getExternalFilesDirNewAPI(hostActivity, type);
			else
				return getExternalFilesDirOldAPI(hostActivity, type);
		}
		
		//Return path the right way
		@TargetApi(Build.VERSION_CODES.FROYO)
		private static File getExternalFilesDirNewAPI(Activity hostActivity, String type) {
			return hostActivity.getExternalFilesDir(type);
		}
		
		//Return path the old and cumbersome way
		private static File getExternalFilesDirOldAPI(Activity hostActivity, String type) {			
			String packageName = hostActivity.getPackageName();
			File extPathRoot = Environment.getExternalStorageDirectory(); 
			String extPathString = extPathRoot.getAbsolutePath().concat("/Android/data/").concat(packageName).concat("/files/").concat(type);
			return new File(extPathString);
		}
				
	} //Utility
	
	
	
	//HTTP Entities
	public static class Entities {
		public static final String imagefile ="imagefile";							//Image file name
		public static final String imagedata ="imagedata";							//Image data (file body)		
		public static final String description ="description";						//Description
		public static final String latitude ="latitude";							//Latitude
		public static final String longitude ="longitude";							//Longitude
		public static final String distance ="distance";							//Distance
		public static final String maxtests ="maxtests";							//Max tests
		public static final String comparemethod ="comparemethod";					//Compare method
		public static final String reducefactor ="reducefactor";					//Reduce factor
		public static final String threshold ="threshold";							//Threshold
		public static final String clientid ="clientid";							//Source id
	}
	
	
	
	//Server side helper class
	public static class ServerSide {
		
		private static final String responseTagID = "#ID";							//Response tag on success: #IDxxxx#ID
		private static final String responseTagERROR = "#ERROR";					//Response tag on failure: #ERRORsssss#ERROR
		
		
		//Parse response string for ID using regular expression
		public static String parseID(String responseBody) {
			String patternString = String.format("(%s)([0-9]{1,10})(%s)", responseTagID, responseTagID) ;  	//if regular expression finds a match this breaks down to 3 groups
			//Log.d(TAG,  String.format("Pattern = %s", patternString));
			Pattern pattern = Pattern.compile(patternString);
			Matcher regEx = pattern.matcher(responseBody);
			if (regEx.find()) {
				//Log.d(TAG, "Match found!");
				//for (int i = 0; i<= regEx.groupCount(); i++) {
				//	Log.d(TAG, String.format("%d, %s", i, regEx.group(i)) );				
				//}
				return regEx.group(2).toString();
			} 				
			
			return null;
		}
		
		
		//Parse response string for Error using regular expression
		public static String parseError(String responseBody) {
			String patternString = String.format("(%s)(.+)(%s)", responseTagERROR, responseTagERROR) ;  	//if regular expression finds a match this breaks down to 3 groups
			//Log.d(TAG,  String.format("Pattern = %s", patternString));
			Pattern pattern = Pattern.compile(patternString);
			Matcher regEx = pattern.matcher(responseBody);
			if (regEx.find()) {
				//Log.d(TAG, "Match found!");
				//for (int i = 0; i<= regEx.groupCount(); i++) {
				//	Log.d(TAG, String.format("%d, %s", i, regEx.group(i)) );				
				//}
				return regEx.group(2);
			} 				
			
			return null;
		}
		
	} //ServerSide 	
	
} //AppCommon