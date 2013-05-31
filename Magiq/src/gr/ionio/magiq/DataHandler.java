package gr.ionio.magiq;
  
import gr.ionio.magiq.AppLogger.LogEx;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


//Data Handler listener interface
interface DataHandlerListener {
	public void onRecordcountChange(int recordCount);									//Callback method on data record count change	
}


public final class DataHandler extends SQLiteOpenHelper {

	private static final String TAG = "DataHandler";									//Log entry tag
	private static final String DATABASE_NAME = "Magiq.db"; 							//Database Name     
	private static final int DATABASE_VERSION = 1; 										//Database Version 
	
	private static final String TABLE_DATA = "data";									//Data table 
	private static final String COL_IMAGEFILE = "imagefile"; 							//Image file column name
	private static final String COL_DECSRIPTION = "description";						//Image optional description column name
	private static final String COL_LATITUDE = "latitude"; 								//Latitude column name
	private static final String COL_LONGITUDE = "longitude";							//Longitude column name
	
	private int recordCount = 0;														//Data table record count
	private DataHandlerListener dataHandlerListener = null; 							//Reference to data handler listener object 
    

	//Public constructor
	public DataHandler(Context context, DataHandlerListener dataHandlerListener) { 
	    super(context, DATABASE_NAME, null, DATABASE_VERSION); 
	    this.dataHandlerListener = dataHandlerListener;
	} 
  
    
	//Release references to objects 
	public void onDestroy() {
		this.dataHandlerListener = null;
	}


	@Override
	public void onCreate(SQLiteDatabase db) { 	    
		String ddl = "CREATE TABLE %s (%s TEXT PRIMARY KEY, %s TEXT NULL, %s REAL, %s REAL)";
		ddl = String.format(ddl, TABLE_DATA, COL_IMAGEFILE, COL_DECSRIPTION, COL_LATITUDE, COL_LONGITUDE);
		LogEx.i(TAG, String.format("Created SQLite database: %s", ddl));
		db.execSQL(ddl);
	} 
  

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { 
		//Not implemented (not planning to change database structure)
	} 

       
	//Initialize record count
	public void countRecords() {
		SQLiteDatabase db = null;
		try {
			db = this.getReadableDatabase(); 											//Open database for read
			String sql = String.format("SELECT COUNT(*) FROM %s", TABLE_DATA);			//Create SQL query
			Cursor cursor = db.rawQuery(sql, null); 									//Run count query
			cursor.moveToFirst();
			recordCount = cursor.getInt(0);												//Get record count
			cursor.close();																//Close cursor
			if (dataHandlerListener != null) {
				dataHandlerListener.onRecordcountChange(recordCount);					//Notify client of record count change
			}		
		} finally {
			if (db != null && db.isOpen()) {
				db.close(); 															//Close database
			}
		}
	}
    
    
	//Add single record into data table
	public void insertRecord(DataRecord record) {     	
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase(); 											//Open database for write 		
			ContentValues values = new ContentValues(); 
			values.put(COL_IMAGEFILE, record.getImageFile()); 
			values.put(COL_DECSRIPTION, record.getDescription());
			values.put(COL_LATITUDE, record.getLatitude()); 
			values.put(COL_LONGITUDE, record.getLongitude());
			db.insert(TABLE_DATA, null, values); 										//Insert record into data table
			recordCount = recordCount + 1;												//Increment record count
			if (dataHandlerListener != null) {
				dataHandlerListener.onRecordcountChange(recordCount);					//Notify client of record count change
			}		
		} finally {
			if (db != null && db.isOpen()) {
				db.close(); 															//Close database
			}
		}		
	} 
  
    
	//Delete single record from data table 
	public void deleteRecord(DataRecord record) { 
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase(); 																					//Open database for write 		
			String sql = String.format("DELETE FROM %s WHERE %s = '%s'", TABLE_DATA, COL_IMAGEFILE, record.getImageFile());		//Create SQL statement - Key is string value!
			db.execSQL(sql);																									//Execute delete SQL statement
			
			//*** Removed because of multi threading side-effect
			//recordCount = recordCount - 1;																					//Decrement record count
			//if (dataHandlerListener != null) {
			//	dataHandlerListener.onRecordcountChange(recordCount);															//Notify client of record count change
			//}
			
		} finally {
			if (db != null && db.isOpen()) {
				db.close(); 																									//Close database
			}
		}    
	} 
    
        
	//Get all records
	public List<DataRecord> getAllRecords() {
		List<DataRecord> data = new ArrayList<DataRecord>();
		SQLiteDatabase db = null;
		try {
			db = this.getReadableDatabase(); 											//Open database for read    		
			String sql = String.format("SELECT * FROM %s", TABLE_DATA);					//Create SQL statement
			Cursor cursor = db.rawQuery(sql, null); 									//Run query to fetch all records from data table
			while (cursor.moveToNext()) {												//Read next record from data table
				DataRecord dataRecord = new DataRecord();								//Package data into DataRecord object
				dataRecord.setImageFile(cursor.getString(0));
				dataRecord.setDescription(cursor.getString(1));
				dataRecord.setLatitude(cursor.getDouble(2));
				dataRecord.setLongitude(cursor.getDouble(3));        	
				data.add(dataRecord);													//Add record into list
			}
			cursor.close();    		
		} finally {
			if (db != null && db.isOpen()) {
				db.close(); 															//Close database
			}
		}
		return data;																	//Return data
	}
    
    
} //DataHandler