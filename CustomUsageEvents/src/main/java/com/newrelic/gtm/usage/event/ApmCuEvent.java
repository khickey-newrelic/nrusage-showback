package com.newrelic.gtm.usage.event;

/** 
 * Event type for counting apmCuUnits where the host to label mapping is derived from apm label mapping.
 */

public class ApmCuEvent extends AbstractUsageEvent{

	private static final String EVENT_TYPE = "LabelApmCuUsage";
	
	public static String getApmCuEventType() {
		return EVENT_TYPE;
	}
	
	public ApmCuEvent() {
		super(EVENT_TYPE);
	}
	
	public void addHostId(String value) {
		super.add("hostId", value);
	}
	
	public void addComputeUnits(double value) {
		super.add("computeUnits", value);
	}

}
