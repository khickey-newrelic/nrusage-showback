package com.newrelic.gtm.usage.event;

import java.util.ArrayList;

import org.json.JSONObject;

import com.newrelic.gtm.usage.Labels;

/** 
 * Event type for counting apmCuUnits where the host to label mapping is derived from apm label mapping.
 */

public abstract class AbstractUsageEvent {

	
	protected String eventType = "";
	protected JSONObject obj;
	
	protected AbstractUsageEvent(String eventType) {
		super();
		obj = new JSONObject();
		obj.put("eventType", eventType);
	}
	
	/**
	 * Setters for common attributes.
	 */

	/**
	 * When usage events apply to.
	 * @param value
	 */
	public void addUsageDay(String value) {
		obj.put("usageDay", value);
	}

	/**
	 * Unique identifier.  Useful for testing or quality control.
	 */
	public void addBatchId(String value) {
		obj.put("batchId", value);
	}
	
	/**
	 * Consuming account id.  For when script is run against master account and there are subaccounts.
	 */
	
	/**
	 * Unique identifier.  Useful for testing or quality control.
	 */
	public void addConsumingAccountId(String value) {
		obj.put("consumingAccountId", value);
	}
	
	/**
	 * Adds collection of labels.
	 */
	public void addLabels(Labels labels) {
		ArrayList<String> categories = labels.getCategories();
    	
	    for(String category: categories) {
	    		obj.put(category, labels.getNames(category));
	    }
		
	}	
	
	/**
	 * Adds value
	 * @param key String identifier
	 * @param value value to add
	 */
	protected void add(String key, Object value) {
		obj.put(key, value);
	}
	
	/**
	 * Getters
	 */
	
	/**
	 * EventType
	 * @return
	 */
	public String getEventType() {
		return eventType;
	}
	
	/**
	 * Key value pairs that make up usage event.
	 */
	public JSONObject getJSONObject() {
		return obj;
	}
}
