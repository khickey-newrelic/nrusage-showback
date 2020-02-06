package com.newrelic.gtm.usage.event;

import java.util.Date;

import org.json.JSONObject;

public class ScriptReportEvent {
	
	private static final String eventType = "NRScriptComplete";
	private JSONObject obj;
	
	public ScriptReportEvent() {
		super();
		obj = new JSONObject();
		obj.put("eventType", eventType);
	}
	
	public void addScriptType(String value) {
		obj.put("scriptType", value);
	}
	
	public void addBatchId(String value) {
		obj.put("batchId", value);
	}		
	
	public void put(String key, Object value) {
		obj.put(key, value);
	}
	
	public JSONObject getJSONObject() {
		return obj;
	}
	
	public void setScriptDuration() {
		Date startTime = (Date)obj.get("timestamp_start");
		Date now = new Date();
		long difference = now.getTime() - startTime.getTime();
		System.out.println("Difference in ms: "+difference);
		long diffSeconds = difference / 1000;
		obj.put("duration", diffSeconds);
	}

}
