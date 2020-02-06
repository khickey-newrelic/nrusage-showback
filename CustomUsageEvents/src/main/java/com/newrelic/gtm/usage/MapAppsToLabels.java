package com.newrelic.gtm.usage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.newrelic.gtm.usage.api.util.InsightsQuery;
import com.newrelic.gtm.usage.api.util.NrURL;
import com.newrelic.gtm.usage.event.ScriptReportEvent;

public class MapAppsToLabels {
	
	private LambdaParameters lambdaParams;
	private ScriptReportEvent scriptEvent;
	private InsightsQuery insightsQuery;
	
	public MapAppsToLabels(LambdaParameters params, ScriptReportEvent scriptEvent) {
		this.lambdaParams = params;
		this.scriptEvent = scriptEvent;
		this.insightsQuery = new InsightsQuery(lambdaParams);
	}

	/**
	 * Returns Hashmap of apps and labels where: <ul>
	 * <li>AppId is associated with labels AND AppId appears in Insights APM Events OR</li>
	 * <li>AppId appears in Insights APM Events.  In this case, app with have a Label with an empty list.
	 * </ul>
	 * @return
	 */
	public  HashMap<Integer, Labels> mapForInsightsRetention(InsightsRetentionDates retentionDates) {
		int page = 1;
		Boolean loop = true;
		
		// Maps appIds to labels
		HashMap<Integer, Labels> appLabelMap = new HashMap<Integer, Labels>();
		
		
		ArrayList<Integer> appIdList = getAppListFromInsightsEvents(retentionDates);

		
		// Iterates through each page of data
		// Builds a the HashMap of appId to label mapping
		while (loop) {

			String labelsApiResult = getNewRelicLabels(NrURL.labelApi() + "?page=" + Integer.toString(page),
					lambdaParams.getNRapiKey());

			try {

				JSONObject labelsJsonObjectResult = new JSONObject(labelsApiResult);
				JSONArray labelsJsonArray = labelsJsonObjectResult.getJSONArray("labels");
				int length = labelsJsonArray.length();

				if (length == 0) {
					loop = false;
				}

				for (int i = 0; i < length; i++) { // for each label
					JSONObject labelJson = labelsJsonArray.getJSONObject(i);
					
					// Finds all applicable applications
					JSONArray applications = labelJson.getJSONObject("links").getJSONArray("applications"); 
					
					// Iterates through each applicable application and adds label mapping
					for (int j = 0; j < applications.length(); j++) {
						
						Integer appId = applications.getInt(j);
						
						// Adds appId to Map only if app exists in NrDailyUsage
						if(appIdList.contains(appId)) {
							String category = labelJson.getString("category");
							String name = labelJson.getString("name");
							if (appLabelMap.containsKey(appId)) {
								appLabelMap.get(appId).addLabel(category, name);
								
							} else {
								appLabelMap.put(appId, new Labels(category, name));
								
							}
						} 						
					}

				}

			} catch (JSONException e) {

				loop = false;
				e.printStackTrace();
			}
			page++;
		}
		
		// Report on apps that have usage but no labels and add to map
		int appsWithUsageNoLabels = 0;
		for(Integer appId : appIdList) {
			if(!appLabelMap.containsKey(appId)) {
				appLabelMap.put(appId, new Labels());
				appsWithUsageNoLabels++;
			}
		}
		
		scriptEvent.put("AppsMissingLabels", appsWithUsageNoLabels);
		System.out.println("Application to label mapping complete");
				
		return appLabelMap;
	}
	/**
	 * Returns Hashmap of apps and labels where: <ul>
	 * <li>AppId is associated with labels AND AppId appears in Insights APM Events OR</li>
	 * <li>AppId appears in Insights APM Events.  In this case, app with have a Label with an empty list.
	 * </ul>
	 * @return
	 */
	public  HashMap<Integer, Labels> mapForDUs() {
		int page = 1;
		Boolean loop = true;
		
		// Maps appIds to labels
		HashMap<Integer, Labels> appLabelMap = new HashMap<Integer, Labels>();
		
		
		ArrayList<Integer> appIdList = getAppListFromInsightsEvents();

		
		// Iterates through each page of data
		// Builds a the HashMap of appId to label mapping
		while (loop) {

			String labelsApiResult = getNewRelicLabels(NrURL.labelApi() + "?page=" + Integer.toString(page),
					lambdaParams.getNRapiKey());

			try {

				JSONObject labelsJsonObjectResult = new JSONObject(labelsApiResult);
				JSONArray labelsJsonArray = labelsJsonObjectResult.getJSONArray("labels");
				int length = labelsJsonArray.length();

				if (length == 0) {
					loop = false;
				}

				for (int i = 0; i < length; i++) { // for each label
					JSONObject labelJson = labelsJsonArray.getJSONObject(i);
					
					// Finds all applicable applications
					JSONArray applications = labelJson.getJSONObject("links").getJSONArray("applications"); 
					
					// Iterates through each applicable application and adds label mapping
					for (int j = 0; j < applications.length(); j++) {
						
						Integer appId = applications.getInt(j);
						
						// Adds appId to Map only if app exists in NrDailyUsage
						if(appIdList.contains(appId)) {
							String category = labelJson.getString("category");
							String name = labelJson.getString("name");
							if (appLabelMap.containsKey(appId)) {
								appLabelMap.get(appId).addLabel(category, name);
								
							} else {
								appLabelMap.put(appId, new Labels(category, name));
								
							}
						} 						
					}

				}

			} catch (JSONException e) {

				loop = false;
				e.printStackTrace();
			}
			page++;
		}
		
		// Report on apps that have usage but no labels and add to map
		int appsWithUsageNoLabels = 0;
		for(Integer appId : appIdList) {
			if(!appLabelMap.containsKey(appId)) {
				appLabelMap.put(appId, new Labels());
				appsWithUsageNoLabels++;
			}
		}
		
		scriptEvent.put("AppsMissingLabels", appsWithUsageNoLabels);
		System.out.println("Application to label mapping complete");
				
		return appLabelMap;
	}

	/**
	 * Returns Hashmap of apps and labels, provided app has entry in NrDailyUsage.
	 * @return
	 */
	public  HashMap<Integer, Labels> mapForCUs() {
		int page = 1;
		Boolean loop = true;
		
		// Maps appIds to labels
		HashMap<Integer, Labels> appLabelMap = new HashMap<Integer, Labels>();
		
		
		ArrayList<Integer> appIdList = getAppListFromNrDailyUsage();

		
		// Iterates through each page of data
		// Builds a the HashMap of appId to label mapping
		while (loop) {

			String labelsApiResult = getNewRelicLabels(NrURL.labelApi() + "?page=" + Integer.toString(page),
					lambdaParams.getNRapiKey());

			try {

				JSONObject labelsJsonObjectResult = new JSONObject(labelsApiResult);
				JSONArray labelsJsonArray = labelsJsonObjectResult.getJSONArray("labels");
				int length = labelsJsonArray.length();

				if (length == 0) {
					loop = false;
				}

				for (int i = 0; i < length; i++) { // for each label
					JSONObject labelJson = labelsJsonArray.getJSONObject(i);
					
					// Finds all applicable applications
					JSONArray applications = labelJson.getJSONObject("links").getJSONArray("applications"); 
					
					// Iterates through each applicable application and adds label mapping
					for (int j = 0; j < applications.length(); j++) {
						
						Integer appId = applications.getInt(j);
						
						// Adds appId to Map only if app exists in NrDailyUsage
						if(appIdList.contains(appId)) {
							String category = labelJson.getString("category");
							String name = labelJson.getString("name");
							if (appLabelMap.containsKey(appId)) {
								appLabelMap.get(appId).addLabel(category, name);
								
							} else {
								appLabelMap.put(appId, new Labels(category, name));
								
							}
						} 						
					}

				}

			} catch (JSONException e) {

				loop = false;
				e.printStackTrace();
			}
			page++;
		}
		
		// Report on apps that have usage but no labels
		int appsWithUsageNoLabels = 0;
		for(Integer appId : appIdList) {
			if(!appLabelMap.containsKey(appId)) {
				appsWithUsageNoLabels++;
			}
		}
		
		scriptEvent.put("AppsMissingLabels", appsWithUsageNoLabels);
		
		System.out.println("Application to label mapping complete");
				
		return appLabelMap;
	}

	private String getNewRelicLabels(String NRurl, String apiKey) {

		String NRjson = "";

		try {

			URL url = new URL(NRurl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("X-Api-Key", apiKey);
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output = "";

			while ((output = br.readLine()) != null) {

				NRjson = NRjson + output;
			}
			conn.disconnect();

		} catch (MalformedURLException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		}

		return NRjson;

	}
	
	/**
	 * Gets a list of appIds currently trying to map for computeUnit facets.  Not all apps with labels necessarily 
	 * have usage. For example, apps in dev and qa may not run every day. 
	 * @return
	 */
	private ArrayList<Integer> getAppListFromNrDailyUsage() {
		
		// Gets a list of appIds currently trying to map.  Not all apps with labels necessarily have usage (ones 
		// in dev and qa may not run every day. 
		
		String insightsEndpoint = NrURL.insightsQuery(lambdaParams.getAccountId());
		String queryKey = lambdaParams.getQueryInsightsKey();
		
		// Check that we are not exceeding maximums
		
		String query = insightsQuery.nrUsageUniqueApplicationsCount();
		String result = insightsQuery.executeQuery(insightsEndpoint, queryKey, query);
		int uniqueAppCount =  new JSONObject(result).getJSONArray("results").getJSONObject(0).getInt("uniqueCount");
		if(uniqueAppCount >= InsightsQuery.MAX_UNIQUES - 50) {
			throw new RuntimeException("The number of unique applications is too close to the limit of "+InsightsQuery.MAX_UNIQUES);
		}
		
		// Retrieves unique applications 
		
		query = insightsQuery.nrUsageUniqueApplications();
		result = insightsQuery.executeQuery(insightsEndpoint, queryKey, query);
		
		JSONArray appIdArray = new JSONObject(result).getJSONArray("results").getJSONObject(0)
				.getJSONArray("members");
		ArrayList<Integer> appIdList = new ArrayList<Integer>();
		for(int i=0; i< appIdArray.length(); i++) {
			appIdList.add(appIdArray.getInt(i));
		}
		scriptEvent.put("AppsWithUsage", appIdArray.length());
		
		return appIdList;
	}
	
	/**
	 * Gets a list of appIds currently trying to map for data unit facets.  Not all apps with labels necessarily 
	 * have usage. For example, apps in dev and qa may not run every day. 
	 * @return
	 */
	private ArrayList<Integer> getAppListFromInsightsEvents() {
		
		String insightsEndpoint = NrURL.insightsQuery(lambdaParams.getAccountId());
		String queryKey = lambdaParams.getQueryInsightsKey();
		String query = insightsQuery.dataUnitUniqueApps();  // This does not include lambda events
		String result = insightsQuery.executeQuery(insightsEndpoint, queryKey, query);
		
		JSONArray appIdArray = new JSONObject(result).getJSONArray("results").getJSONObject(0)
				.getJSONArray("members");
		ArrayList<Integer> appIdList = new ArrayList<Integer>();
		for(int i=0; i< appIdArray.length(); i++) {
			appIdList.add(appIdArray.getInt(i));
		}
		scriptEvent.put("AppsWithEvents", appIdArray.length());
		
		return appIdList;
	}
	
	private ArrayList<Integer> getAppListFromInsightsEvents(InsightsRetentionDates retentionDates) {
		
		String insightsEndpoint = NrURL.insightsQuery(lambdaParams.getAccountId());
		String queryKey = lambdaParams.getQueryInsightsKey();
		String query = insightsQuery.insightsUniqueApplications(retentionDates.earliestStartDay());  
		String result = insightsQuery.executeQuery(insightsEndpoint, queryKey, query);
		
		JSONArray appIdArray = new JSONObject(result).getJSONArray("results").getJSONObject(0)
				.getJSONArray("members");
		ArrayList<Integer> appIdList = new ArrayList<Integer>();
		for(int i=0; i< appIdArray.length(); i++) {
			appIdList.add(appIdArray.getInt(i));
		}
		scriptEvent.put("AppsWithEvents", appIdArray.length());
		
		return appIdList;
	}
}
