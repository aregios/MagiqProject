package gr.ionio.magiq;

public final class DataRecord {

	private String imageFile = ""; 
	private String description = ""; 
	private double latitude = 0.0d;
	private double longitude = 0.0d;
	
	  
	//Image file
	public String getImageFile(){ 
		return this.imageFile; 
	} 
	   
	public void setImageFile(String imageFile){ 
		this.imageFile = imageFile; 
	}
	    
	    
	//Description
	public String getDescription(){ 
		return this.description; 
	} 
	   
	public void setDescription(String description){ 
		this.description = description; 
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
	    
	    
	@Override
	public String toString() {
		String sret = "DataRecord: Image file = '%s', Descr = '%s', Lat = %4f, Long = %4f ";
		return String.format(sret, imageFile, description, latitude, longitude);   	
	}

} //DataRecord