package com.newrelic.gtm.usage.handler;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.newrelic.gtm.usage.Labels;
import com.newrelic.gtm.usage.MapAppsToLabels;
import com.newrelic.gtm.usage.api.util.InsightsUpdate;
import com.newrelic.gtm.usage.event.ApmDuEvent;

public class ApmDuFacetHandler extends AbstractFacetHandler  {

	/**
	 * Determines APM Data Unit usage and inserts custom events into Insights for usage showback.
	 */
	@Override
	public String handleRequest(Object input, Context context) {
		
		context.getLogger().log("Input: " + input);
		
		// Parses parameters, initializes Insights api variables
		initializeRequestVariables(input);
		generateBatchId();
		
		initalizeScriptReportEvent(ApmDuEvent.getApmDuEventType());
		
		// Retrieves app to label mapping.
		MapAppsToLabels appLabelMap = new MapAppsToLabels(lambdaParams, scriptEvent);
		HashMap<Integer, Labels> appLabelsMap = appLabelMap.mapForDUs();
		
		// Gets usage for each app
		HashMap<Integer, ApmDuEvent> appUsageMap = queryUsage(appLabelsMap);
		
		// Posts usage to Insights
		postUsage(appUsageMap);

		finalizeScriptReportEventAndSend();
		
		return batchId;
	}

	/**
	 * Batches usage events and sends to Insights.
	 * @param appUsageMap
	 */
	private void postUsage(HashMap<Integer, ApmDuEvent> appUsageMap) {
		
		
		// Iterates through each app and collects usage for that app
        int apmDuEvents = 0;
        
        // Batch for performance - may optimize elsewhere later
        JSONArray importArr = new JSONArray();
		
        int batch = 0;
        int batch_max = 100; // Max 1MB per post
        

    	for(Integer appId : appUsageMap.keySet()) {
    		    		
			// Adds usage event to Insights
    		ApmDuEvent usageEvent = appUsageMap.get(appId);
			importArr.put(usageEvent.getJSONObject());
			batch++;
			apmDuEvents++;
			
			// Checks if batch is ready to go
			if(batch >= batch_max) {
								
				String temp = importArr.toString();
	    		System.out.println("Sending usage event array of "+temp.getBytes().length +" bytes");
				InsightsUpdate.postAppUsageToInsights(lambdaParams,importArr);
				batch = 0;
				importArr = new JSONArray();				
			}			
		}
    	
    	// Sends last batch
    	if(!importArr.isEmpty()) {
    		InsightsUpdate.postAppUsageToInsights(lambdaParams,importArr);
    		String temp = importArr.toString();
    		System.out.println("Sending usage event array of "+temp.getBytes().length +" bytes");
    	}
        	
		// Reports on number of usage events created
		scriptEvent.put("usageEventsCreated", apmDuEvents);
	}
	
	/**
	 * Gets usage for each app.  Batches queries for performance.
	 */
	private HashMap<Integer, ApmDuEvent> queryUsage(HashMap<Integer, Labels> appLabelsMap) {

		Double allApmDataUnits = queryTotalDataUnits();

		int batch = 0;
		int batchMax = 10;

		ArrayList<Integer> appIds = new ArrayList<Integer>();
		HashMap<Integer, ApmDuEvent> appUsageMap = new HashMap<Integer, ApmDuEvent>();

		for (Integer appId : appLabelsMap.keySet()) {

			appIds.add(appId);

			ApmDuEvent usageEvent = new ApmDuEvent();
			usageEvent.addBatchId(batchId);
			usageEvent.addUsageDay(lambdaParams.getUsageDay());
			usageEvent.addAppId(appId);
			usageEvent.addLabels(appLabelsMap.get(appId));
			usageEvent.addAllApmEvents(allApmDataUnits);

			// Only usage and appName are missing now

			appUsageMap.put(appId, usageEvent);
			batch++;

			if (batch >= batchMax) {
				addUsage(appUsageMap, appIds);
				batch = 0;
				appIds.clear();
			}
		}

		addUsage(appUsageMap, appIds);

		return appUsageMap;
	}
	
	private Double queryTotalDataUnits() {
		
		// Gets overall number of Insights events to use for percentage reporting.
		String query = insightsQuery.appApmDataUnitCount();
		String result = insightsQuery.executeQuery(insightsQueryEndpoint, queryKey, query);
		Double totalInsightsEvents = new JSONObject(result)
				.getJSONArray("results")
				.getJSONObject(0).getDouble("count");
		
		return totalInsightsEvents;
	}
	
	/**
	 * Gets usage for appLists in list and enriches usage events in map. 
	 * @param appUsageMap  
	 * @param appIds
	 */
	private void addUsage(HashMap<Integer, ApmDuEvent> appUsageMap, ArrayList<Integer> appIds) {
		
		// Query for the usage.
		String query = insightsQuery.appApmDataUnitCount(appIds);
		String result = insightsQuery.executeQuery(insightsQueryEndpoint, queryKey, query);
		
		JSONArray facets = new JSONObject(result).getJSONArray("facets");
		for(int i=0; i< facets.length(); i++) {
			JSONObject facet = facets.getJSONObject(i);
			JSONArray facetNames = facet.getJSONArray("name");
			JSONArray results = facet.getJSONArray("results");
			
			String appName = facetNames.getString(1);
			Integer appId = facetNames.getInt(0);
			Double appInsightsEvents = results.getJSONObject(0).getDouble("count");
			ApmDuEvent event = appUsageMap.get(appId);
			event.addConsumingAccountId(lambdaParams.getAccountId());
			event.addAppName(appName);
			event.addApmEvents(appInsightsEvents);
			
		}

	}
}
