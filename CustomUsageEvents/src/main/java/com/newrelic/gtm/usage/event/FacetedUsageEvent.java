package com.newrelic.gtm.usage.event;

import java.util.ArrayList;

import org.json.JSONObject;

import com.newrelic.gtm.usage.Labels;

public class FacetedUsageEvent {

	private static final String eventType = "LabelDailyUsageV5";
	private JSONObject obj;
	
	public FacetedUsageEvent() {
		super();
		obj = new JSONObject();
		obj.put("eventType", eventType);
	}
	
	public static String getEventType() {
		return eventType;
	}
	
	public void addCategories(Labels labels) {
		ArrayList<String> categories = labels.getCategories();
    	
	    for(String category: categories) {
	    		obj.put(category, labels.getNames(category));
	    }
		
	}
	
	public void setAppName(String value) {
		obj.put("appName", value);
	}
	
	public void addApmEventStorageAllEvents(double value) {
		obj.put("apmEventStorageAllEvents", value);
	}
	
	public void add(String key, double value) {
		obj.put(key, value);
	}
	
	public JSONObject getJSONObject() {
		return obj;
	}

}
