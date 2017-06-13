package com.agroch.travelassistlite;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class MapItem extends OverlayItem {

	private int    id        = 0;
	

	private String pictPath  = "";
	private String timeStamp = Util.convertTimestamp(System.currentTimeMillis());
	private String type      = "";
	private String address   = "";
	
	public static final String TYPE_PICTURE  = "picture";
    public static final String TRACK_POINT 	 = "track";
    public static final String START_POINT 	 = "start_point";
    public static final String END_POINT 	 = "end_point";
	
	
	

	public MapItem(GeoPoint arg0, String arg1, String arg2) {
		super(arg0, arg1, arg2);
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPictPath() {
		return pictPath;
	}

	public void setPictPath(String pictPath) {
		this.pictPath = pictPath;
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public void setAddress( String addr){
		address = addr;
	}
	
	public String getAddress() {
		return address;
	}
	

}
