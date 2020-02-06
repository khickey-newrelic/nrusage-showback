package com.newrelic.gtm.usage.event;

import java.util.Set;

/** 
 * Event type for counting apm data units where the host to label mapping is derived from apm label mapping.
 */
public class ApmDuEvent extends AbstractUsageEvent{
	
	private static final String EVENT_TYPE = "LabelApmDuUsage";

	private static final String APM_EVENT_COUNT = "apmEventCount";
	private static final String ALL_APM_EVENT_COUNT = "allApmEventCount";
	private static final String PERCENT = "percentEventUsage";

	
	public ApmDuEvent() {
		super(EVENT_TYPE);
	}
	
	public static String getApmDuEventType() {
		return EVENT_TYPE;
	}
	
	public static String getApmEventCountKey() {
		return APM_EVENT_COUNT;
	}
	
	public static String getPercentKey() {
		return PERCENT;
	}
	
	
	public void addAppId(Integer value) {
		super.add("appId", value);
	}
	
	public void addAppName(String value) {
		super.add("appName", value);
	}
	
	public void addApmEvents(Double value) {
		super.add(APM_EVENT_COUNT, value);
		addPercentEvents();
	}
	
	public void addAllApmEvents(Double value) {
		super.add(ALL_APM_EVENT_COUNT, value);
		addPercentEvents();
	}
	
	private void addPercentEvents() {
		Set<String> keys = getJSONObject().keySet();
		if (keys.contains(APM_EVENT_COUNT) &&
			keys.contains(ALL_APM_EVENT_COUNT) &&
			(Double) getJSONObject().get(ALL_APM_EVENT_COUNT) > 0.0){
				super.add(PERCENT, (Double)getJSONObject().get(APM_EVENT_COUNT)/(Double)getJSONObject().get(ALL_APM_EVENT_COUNT)*100);
			}
		
	}
}
