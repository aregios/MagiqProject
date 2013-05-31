package gr.ionio.magiq;

enum ActionStatus {ACTION_STATUS_OK, ACTION_STATUS_ERROR}

public final class ActionResult {

	private String value = ""; 
	private ActionStatus status = ActionStatus.ACTION_STATUS_OK; 
	
	
	//Constructors
	public ActionResult(ActionStatus status){  
		this.status = status;    
	} 
    
	public ActionResult(ActionStatus status, String value){ 
		this.status = status;
		this.value = value;     	
	} 
	    
    
	//Action value
	public String getValue(){ 
		return this.value; 
	} 
   
	public void setValue(String value){ 
		this.value = value; 
	}
    
	
	//Action status
	public ActionStatus getStatus(){ 
		return this.status; 
	} 
   
	public void setStatus(ActionStatus status){ 
		this.status = status; 
	}
    
} //ActionResult