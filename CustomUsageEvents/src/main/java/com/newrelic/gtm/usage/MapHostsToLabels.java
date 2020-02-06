package com.newrelic.gtm.usage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.newrelic.gtm.usage.api.util.InsightsQuery;
import com.newrelic.gtm.usage.api.util.NrURL;
import com.newrelic.gtm.usage.event.ScriptReportEvent;

public class MapHostsToLabels {
	
	private LambdaParameters lambdaParams;
	private ScriptReportEvent scriptEvent;
	private InsightsQuery insightsQuery;
	
	public MapHostsToLabels(LambdaParameters lambdaParams, ScriptReportEvent scriptEvent) {
		this.lambdaParams = lambdaParams;
		this.scriptEvent = scriptEvent;
		this.insightsQuery = new InsightsQuery(lambdaParams);
	}
	
	/**
	 * Maps hosts to labels. 
	 * @param lambdaParams  lamdba function parameters
	 * @param appLabelsMap app to label mapping
	 * @return HashMap where key=hostId, value=Label (collection of category key value pairs)
	 */
	public HashMap<String, Labels> map(HashMap<Integer, Labels> appLabelsMap) {
		
		HashMap<String, Labels> hostLabelMap = new HashMap<String, Labels>();
		String insightsEndpoint = NrURL.insightsQuery(lambdaParams.getAccountId());
		String queryKey = lambdaParams.getQueryInsightsKey();
		
        int batch = 0;
        int batchMax = 10;
        ArrayList<Integer> appIds = new ArrayList<Integer>();
        
		// Iterates through each appId and gets hosts with usage that day 
		
		for (Map.Entry<Integer, Labels> entry : appLabelsMap.entrySet()) {
			
			Integer appId = entry.getKey();
			appIds.add(appId);
			batch++;
			
			if(batch >= batchMax) {
				addHostLabelMapping(hostLabelMap, appLabelsMap, appIds);
				appIds.clear();
				batch = 0;
			}
		}
		
		// Last batch
		if(!appIds.isEmpty()) {
			addHostLabelMapping(hostLabelMap, appLabelsMap, appIds);
		}
				
		scriptEvent.put("HostsWithLabels", hostLabelMap.keySet().size());
		
		// Adds in empty labels for hosts that have usage but weren't found in mapping application labels to hosts
		ArrayList<String> allHosts = allHosts(insightsEndpoint, queryKey);
		for (String hostId : allHosts) {
			if (!hostLabelMap.containsKey(hostId)) {
				hostLabelMap.put(hostId, new Labels());
			}
		}
		
		System.out.println("Host to label mapping complete");

		return hostLabelMap;
	}
	
	/**
	 * Unique hosts from NrDailyUsage.
	 * @param insightsEndpoint
	 * @param queryKey
	 * @return
	 */
	public ArrayList<String> allHosts(String insightsEndpoint, String queryKey){
		
		ArrayList<String> allHosts = new ArrayList<String>();
		
		// Verify that there are not too many records.  The code may will need to be modified and retested if there are. 
		String query = insightsQuery.nrUsageUniqueHostIdCount();
		String result = insightsQuery.executeQuery(insightsEndpoint, queryKey, query);
		int uniqueHostCount =  new JSONObject(result).getJSONArray("results").getJSONObject(0).getInt("uniqueCount");
		if(uniqueHostCount >= InsightsQuery.MAX_UNIQUES - 50) {
			throw new RuntimeException("The number of unique hosts is too close to the limit of "+InsightsQuery.MAX_UNIQUES);
		}
		
		// Queries unique hosts
		query = insightsQuery.nrUsageUniqueHostIds();
		result = insightsQuery.executeQuery(insightsEndpoint, queryKey, query);
		JSONArray hostIdArray = new JSONObject(result).getJSONArray("results").getJSONObject(0)
				.getJSONArray("members");
		
		for (int i = 0; i < hostIdArray.length(); i++) {
			allHosts.add((String) hostIdArray.get(i));
			
		}
		
		// Now that we have all hosts, record the count so we know it matches
		scriptEvent.put("HostsWithUsage", allHosts.size());
		
		return allHosts;
	}
	
	/**
	 * Adds given label to host, check if host already exists in map and merges labels if necessary.
	 */
	private void addLabelToHost(String hostId, HashMap<String, Labels> hostLabelMap, Labels labels) {
		if(hostLabelMap.containsKey(hostId)) {
			Labels existing = hostLabelMap.get(hostId);
			Labels merged = mergeLabels(existing, labels);
			hostLabelMap.put(hostId, merged);
		} else {
			hostLabelMap.put(hostId, labels);
		}
		
	}
	
    /**
     * Handles complexity of merging a label (collection of name value pairs)
     */
    private Labels mergeLabels(Labels existing, Labels newLabel) {
    	
    	ArrayList<String> duplicateCategories = new ArrayList<String>();
    	
    	// For each name value pair in the new label, check to see if the same values already exist
    	for(String category:newLabel.getCategories()) {
    		if (existing.getLabelMap().containsKey(category)) {
    			
    			// Category exists.  Does the value already exist?
    			ArrayList<String> newValues = newLabel.getLabelMap().get(category);
    			ArrayList<String> existingValues = existing.getLabelMap().get(category);
    			
    			for(String newValue : newValues) {
    				if(!existingValues.contains(newValue)) {
    					existingValues.add(newValue);
    					
    					if(!duplicateCategories.contains(category)) {
    						duplicateCategories.add(category);
    					}
    					
    				}
    			}
    			
    		}
    	}
    	
    	scriptEvent.put("CategoriesMultipleValues", duplicateCategories);
    	
    	
    	return existing;
    }
	
	/**
	 * Queries list of appIds, finds host mapping for each, then uses app to label mapping to create host to label mapping.
	 * @param hostLabelMap  Total list of host labels map so far.
	 * @param appLabelMap App label mapping to use to enrich host labels map.
	 * @param appIds  List of appIds to query for.
	 */
	private void addHostLabelMapping(HashMap<String, Labels> hostLabelMap, HashMap<Integer, Labels> appLabelsMap, ArrayList<Integer> appIds) {
		
		String insightsEndpoint = NrURL.insightsQuery(lambdaParams.getAccountId());
		String queryKey = lambdaParams.getQueryInsightsKey();
		String query = insightsQuery.nrUsageUniqueHostIdsForAppId(appIds);
		String result = insightsQuery.executeQuery(insightsEndpoint, queryKey, query);
		
		JSONArray facets = new JSONObject(result).getJSONArray("facets");
		
		// Iterates through batched appId to hostId results
		for(int i=0; i< facets.length(); i++) {
			JSONObject facet = facets.getJSONObject(i);
			Integer appId = facet.getInt("name");
			JSONArray hostIdArray = facet.getJSONArray("results").getJSONObject(0)
			.getJSONArray("members");
			
			try {
				if (hostIdArray.length() > 0) {
					// Iterates through hosts associated with appId and adds host label mapping for each one
					for (int j = 0; j < hostIdArray.length(); j++) {
						String hostId = (String) hostIdArray.get(j);
						addLabelToHost(hostId, hostLabelMap, appLabelsMap.get(appId));
					}
				} else {
					System.out.println("No usage events found for appId = " + appId);
				}
			} catch (JSONException e) {

				e.printStackTrace();
			}
		}
		
		
	}
}
