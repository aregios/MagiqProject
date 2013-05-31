package gr.ionio.magiq;

import java.io.File;


public final class QueryData {
	
	private String imageFile = ""; 									//Image file absolute name
	private long imageFileSize = 0;									//Image file size
	private long imageFileModified = 0;								//Image file last modified
	
	private int distance = 0; 										//Distance value
	private double latitude = 0.0d;									//Latitude
	private double longitude = 0.0d;								//Longitude
	
	private int maxTests = 0;										//Maximum image tests ( = maximum geographical points to compare for image matching)
	private int compareMethod = 0;									//Image compare method
	private int reduceFactor = 0;									//Reduce colors factor
	private double threshold = 0.0;									//Results threshold

    
	//Image file
	public String getImageFile(){ 
		return this.imageFile; 
	} 
   
	public void setImageFile(String imageFile){ 
	    this.imageFile = imageFile; 
	    File image = new File(imageFile);
	    this.imageFileSize = image.length();
	    this.imageFileModified = image.lastModified();
	}
    
    
	//Distance
	public int getDistance(){ 
		return this.distance; 
	} 
   
	public void setDistance(int distance){ 
		this.distance = distance; 
	}
    
    
	//Latitude value
	public double getLatitude(){ 
		return this.latitude; 
	} 
  
	public void setLatitude(double latitude){ 
		this.latitude = latitude; 
	} 
  
    
	//Longitude
	public double getLongitude(){ 
		return this.longitude; 
	} 
  
	public void setLongitude(double longitude){ 
		this.longitude = longitude; 
	}
    
    
	//Max tests
	public int getMaxTests(){ 
		return this.maxTests; 
	} 
   
	public void setMaxTests(int maxTests){ 
		this.maxTests = maxTests; 
	}

	
	//Compare method
	public int getCompareMethod(){ 
		return this.compareMethod; 
	} 
   
	public void setCompareMethod(int compareMethod){ 
		this.compareMethod = compareMethod; 
	}
    
    
	//Reduce factor
	public int getReduceFactor(){ 
		return this.reduceFactor; 
	} 
   
	public void setReduceFactor(int reduceFactor){ 
		this.reduceFactor = reduceFactor; 
	}
    
    
	//Result threshold
	public double getThreshold(){ 
		return this.threshold; 
	} 
	  
	public void setThreshold(double threshold){ 
		this.threshold = threshold; 
	}
	  
	    
	@Override
	public String toString() {
		String sret = "QueryData: Image file = '%s' Dist = %d Lat = %6f Long = %6f MT = %d CM = %d RF = %d Thr = %2f";
		return String.format(sret, imageFile, distance, latitude, longitude, maxTests, compareMethod, reduceFactor, threshold);   	
	}
    
    
	//Return a unique string for current instance
	public String getLocalID() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(imageFileSize);
		stringBuilder.append(imageFileModified);
		stringBuilder.append(distance);
		stringBuilder.append(latitude);
		stringBuilder.append(longitude);
		stringBuilder.append(maxTests);
		stringBuilder.append(compareMethod);
		stringBuilder.append(reduceFactor);
		stringBuilder.append(threshold);
		return stringBuilder.toString();
	}
	
} //QueryData