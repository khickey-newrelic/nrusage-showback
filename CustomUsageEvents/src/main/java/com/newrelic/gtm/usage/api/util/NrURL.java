package com.newrelic.gtm.usage.api.util;

public class NrURL {
	
	// Labels API v2
	public static String labelApi() {
		return "https://api.newrelic.com/v2/labels.json";
	}
	
	// Insert events in to Insights
	public static String insightsImport(String accountId) {
		return "https://insights-collector.newrelic.com/v1/accounts/" + accountId + "/events";		
	}
	
	// Query insights
	public static String insightsQuery(String accountId) {
		return "https://insights-api.newrelic.com/v1/accounts/" + accountId + "/query?nrql=";
	}

}
