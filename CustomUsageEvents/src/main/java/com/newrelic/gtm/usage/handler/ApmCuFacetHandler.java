package com.newrelic.gtm.usage.handler;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.newrelic.gtm.usage.Labels;
import com.newrelic.gtm.usage.MapAppsToLabels;
import com.newrelic.gtm.usage.MapHostsToLabels;
import com.newrelic.gtm.usage.api.util.InsightsUpdate;
import com.newrelic.gtm.usage.event.ApmCuEvent;

/**
 * Parses NrDailyUsage into Host-Label mapping.  An event is inserted into Insights for each host usage along with APM labels.
 * Inserts an Insights event at the end of execution reporting details of handleRequest.
 * @TODO This queries hosts in NrDailyUsage twice - once for a unique list, another for usage.  This can be combined in one 
 * query, but be mindful of multiple entries for a single host.
 */
public class ApmCuFacetHandler extends AbstractFacetHandler {
	
	private static final int MAX_BYTES = 1000000;  // Following the convention that 1 MB is a million bytes
	private static final int MAX_BATCH = 500; // Maximum batch size

	@Override
	public String handleRequest(Object input, Context context) {
		context.getLogger().log("Input: " + input + "\n");
		
		// Parses parameters, initializes Insights api variables
		initializeRequestVariables(input);
		generateBatchId();
		
		initalizeScriptReportEvent(ApmCuEvent.getApmCuEventType());
	    
	    /**
	     *  Baseline reporting data for qualifying results. 
	     */
		
		String query = insightsQuery.nrUsageApmComputeUnits();
		String result = insightsQuery.executeQuery(insightsQueryEndpoint, queryKey, query);
		Double computeUnits = new JSONObject(result).getJSONArray("results").getJSONObject(0).getDouble("sum");
		scriptEvent.put("ComputeUnitsAllHosts", computeUnits);
		
		/** End of baseline data */
		
		// Retrieves app to label mapping.
		MapAppsToLabels appLabelMap = new MapAppsToLabels(lambdaParams, scriptEvent);
		HashMap<Integer, Labels> appLabelsMap = appLabelMap.mapForCUs();

		// Iterates through each appId and gets hosts with usage that day. Assigns
		// labels to hosts
		MapHostsToLabels mapHostsToLabels = new MapHostsToLabels(lambdaParams, scriptEvent);
		HashMap<String, Labels> hostLabelsMap = mapHostsToLabels.map(appLabelsMap);
		
		// Gets usage for each app
		HashMap<String, ApmCuEvent> hostUsageMap = queryUsage(hostLabelsMap);
		
		// Posts usage to Insights
		postUsage(hostUsageMap);
		
		finalizeScriptReportEventAndSend();

		return batchId;
	}
	
	/**
	 * Gets usage for each host.  Batches queries for performance.
	 */
	private HashMap<String, ApmCuEvent> queryUsage(HashMap<String, Labels> hostLabelsMap) {

		int batch = 0;
		int batchMax = 10;

		ArrayList<String> hostIds = new ArrayList<String>();
		HashMap<String, ApmCuEvent> hostUsageMap = new HashMap<String, ApmCuEvent>();

		for (String hostId : hostLabelsMap.keySet()) {

			hostIds.add(hostId);

			ApmCuEvent usageEvent = new ApmCuEvent();
			usageEvent.addBatchId(batchId);
			usageEvent.addUsageDay(lambdaParams.getUsageDay());
			usageEvent.addHostId(hostId);
			usageEvent.addLabels(hostLabelsMap.get(hostId));


			// Only usage is missing now

			hostUsageMap.put(hostId, usageEvent);
			batch++;

			if (batch >= batchMax) {
				addUsage(hostUsageMap, hostIds);
				batch = 0;
				hostIds.clear();
			}
		}

		addUsage(hostUsageMap, hostIds);

		return hostUsageMap;
	}
	
	/**
	 * Batches usage events and sends to Insights.
	 * @param hostUsageMap
	 */
	private void postUsage(HashMap<String, ApmCuEvent> hostUsageMap) {
		
		
		// Iterates through each app and collects usage for that app
        int apmCuEvents = 0;
        
        // Batch for performance - may optimize elsewhere later
        JSONArray importArr = new JSONArray();
		
        int batch = 0;

    	for(String hostId : hostUsageMap.keySet()) {
    		    		
			// Adds usage event to Insights
    		ApmCuEvent usageEvent = hostUsageMap.get(hostId);
			importArr.put(usageEvent.getJSONObject());
			batch++;
			apmCuEvents++;
			
			// Checks if batch is ready to go
			if(batch >= MAX_BATCH) {
								
				String temp = importArr.toString();
	    		if(temp.getBytes().length >= MAX_BYTES) {
	    			throw new RuntimeException("Cannot submit events to Insights.  Payload is too large.  Maximum is "+MAX_BYTES);
	    		}
	    		
				InsightsUpdate.postAppUsageToInsights(lambdaParams,importArr);
				batch = 0;
				importArr = new JSONArray();	
				System.out.println(apmCuEvents +" events submitted");
			}			
		}
    	
    	// Sends last batch
    	if(!importArr.isEmpty()) {
    		
    		
    		String temp = importArr.toString();
    		if(temp.getBytes().length >= MAX_BYTES) {
    			throw new RuntimeException("Cannot submit events to Insights.  Payload is too large.  Maximum is "+MAX_BYTES);
    		}
    		InsightsUpdate.postAppUsageToInsights(lambdaParams,importArr);
    		System.out.println(apmCuEvents +" events submitted");
    	}
        	
		// Reports on number of usage events created
		scriptEvent.put("usageEventsCreated", apmCuEvents);
	}
	
	
	/**
	 * Gets usage for appLists in list and enriches usage events in map. 
	 * @param hostUsageMap HostToUsage map to enrich.
	 * @param hostIds
	 */
	private void addUsage(HashMap<String, ApmCuEvent> hostUsageMap, ArrayList<String> hostIds) {
		
		// Query for the usage.
		String query = insightsQuery.hostUsageSum(hostIds);
		String result = insightsQuery.executeQuery(insightsQueryEndpoint, queryKey, query);
		
		JSONArray facets = new JSONObject(result).getJSONArray("facets");
		for(int i=0; i< facets.length(); i++) {
			
			JSONObject facet = facets.getJSONObject(i);
			
			JSONArray facetNames = facet.getJSONArray("name");
			String hostId = facetNames.getString(0);
			String consumingAccountId = facetNames.getString(1);
			
			JSONArray results = facet.getJSONArray("results");		
			Double hostCus = results.getJSONObject(0).getDouble("sum");

			ApmCuEvent event = hostUsageMap.get(hostId);
			event.addConsumingAccountId(consumingAccountId);
			event.addComputeUnits(hostCus);
			
		}

	}

}
