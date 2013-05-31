package gr.ionio.magiq;

import gr.ionio.magiq.AppCommon.Utility;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;


public final class AppLogger {

	public static class LogEx {
	
		private static final String MEDIA_MOUNTED = "mounted" ; 
		private static final String DIRECTORY_LOGS = "Logs";
		
		private static File logFile = null;														//Log file object
		private static int maxLogSize = 0;														//Max log file size (Bytes)
		private static boolean enabled = false;													//Enabled flag	
		
		
		//Attach Activity to logger
		public static void attach(Activity hostActivity) {
			if (logFile == null) {			
				try {
					logFile = getLogFile(hostActivity, "app.log");								//Set log file object			
					maxLogSize = 10 * 1024 * 1024;												//Set log file max size in Bytes ( = 10MB)
				} catch (Exception ex) {
					logFile = null;
					maxLogSize = 0;
				}
			}
		}
		
		
		//Enabled flag
		public static boolean getEnabled() {
			return LogEx.enabled;
		}
		
		public static void setEnabled(boolean enabled) {
			LogEx.enabled = enabled;
		}
		
		
		//Error logging
		public static synchronized void e(String tag, String msg) {
			LogEx.e(tag, msg, null);		
		}
	
		public static synchronized void e(String tag, String msg, Throwable ex) {
			if (enabled) 
				Log.e(tag, msg, ex);															//Write error to console			
			
			if (enabled && logFile != null) 
				writeFile("E", tag, msg, ex);													//Write error to log file
		}
	
		
		//Info logging
		public static synchronized void i(String tag, String msg) {
			LogEx.i(tag, msg, null);
		}
		
		public static synchronized void i(String tag, String msg, Throwable ex) {
			if (enabled) 
				Log.i(tag, msg, ex);															//Write info to console			
			
			if (enabled && logFile != null) 
				writeFile("I", tag, msg, ex);													//Write info to log file
		}
		
		
		//Core file function
		private static void writeFile(String level, String tag, String msg, Throwable ex) {
			try {
				
				String stackTrace = "";
				if (ex != null) {
					Writer stackTraceWriter = new StringWriter();
				    PrintWriter printWriter = new PrintWriter(stackTraceWriter);
				    ex.printStackTrace(printWriter);
				    stackTrace = stackTraceWriter.toString();
				    printWriter.close();
				}
				
				String timestamp = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.ENGLISH).format(new Date());
				String line = String.format("%s \t %s \t %s \t %s \t %s \n", level, timestamp, tag, msg, stackTrace);
				
				RandomAccessFile randFile = new RandomAccessFile(logFile, "rwd");
				if (maxLogSize > 0 && (logFile.length() + line.length()) >= maxLogSize) {
					randFile.setLength(0);
				} else {
					randFile.seek(randFile.length());
				}
				randFile.writeBytes(line);
				randFile.close();
				
			} catch (IOException ioex) {
				Log.e(tag, ioex.toString(), ioex);
			}
		}
		
			
		//Returns an absolute file name for saving an application log
		private static File getLogFile(Activity hostActivity, String logFileName) {
			
			//Check external storage state
			if (Environment.getExternalStorageState().equals(MEDIA_MOUNTED) == false) {
				return null;
			}
			
			//Specify directory name (External storage must be mounted AND application must have WRITE_EXTERNAL_STORAGE permission)
			File logPath = Utility.getExternalFilesDir(hostActivity, DIRECTORY_LOGS);   
			if (logPath == null) {
				return null;
			}
			
			//Create directory structure
			if (logPath.exists() == false) {     
			    if (logPath.mkdirs() == false) { 
			        return null;
			    }
			}
			
			//Return log file object
			return new File(logPath, logFileName);	
		}
	
	}//LogEx
	
} //AppLogger