package gr.ionio.magiq;

public final class CompressParams {

	private String imageFile = "";
	private int imageQuality = 0;
	
	
	public String getImageFile() {
		return this.imageFile;
	}
	
	public void setImageFile(String value) {
		this.imageFile = value;
	}
	
	
	public int getImageQuality() {
		return this.imageQuality;
	}
	
	public void setImageQuality(int value) {
		this.imageQuality = value;
	}
	
	
	@Override
	public String toString() {
		String sret = "CompressParams: Image file = '%s', Quality = %d";
		return String.format(sret, imageFile, imageQuality);   	
	}

    
} //CompressParameters