package com.newrelic.gtm.usage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONObject;

import com.newrelic.gtm.usage.api.util.InsightsQuery;
import com.newrelic.gtm.usage.api.util.NrURL;

public class InsightsRetentionDates {
	// Included retention in days
	private int includedApmRetention = 8; 
	private int includedApmErrorRetention = 8;
	
	private int totalApmRetention = 8; 
	private int totalApmErrorRetention = 8;
	
    private String apmTotalStartDay = "";
    private String apmIncludedStartDay = "";
    private String apmErrTotalStartDay = "";
    private String apmErrIncludedStartDay = "";
    private String yesterday ="";
    
    /** 
	 * Constructor sets important dates.
	 */
	public InsightsRetentionDates(LambdaParameters lambdaParams) {
		InsightsQuery insightsQuery = new InsightsQuery(lambdaParams);
		String insightsQueryEndpoint = NrURL.insightsQuery(lambdaParams.getAccountId());
		String queryKey = lambdaParams.getQueryInsightsKey();	

		// APM Event Namespace
		String query = insightsQuery.licensedRetentionAPM();
		String result = insightsQuery.executeQuery(insightsQueryEndpoint, queryKey, query);
		
		JSONArray results = new JSONObject(result).getJSONArray("results").getJSONObject(0).getJSONArray("events");
		JSONObject event = results.getJSONObject(0);
		totalApmRetention = event.getInt("insightsTotalRetentionInHours")/24;
		includedApmRetention = event.getInt("insightsIncludedRetentionInHours")/24;
		
		// APM Errors Event Namespace
		query = insightsQuery.licensedRetentionAPMErrors();
		result = insightsQuery.executeQuery(insightsQueryEndpoint, queryKey, query);
		
		results = new JSONObject(result).getJSONArray("results").getJSONObject(0).getJSONArray("events");
		event = results.getJSONObject(0);
		totalApmErrorRetention = event.getInt("insightsTotalRetentionInHours")/24;
		includedApmErrorRetention = event.getInt("insightsIncludedRetentionInHours")/24;
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);    
        yesterday = dateFormat.format(cal.getTime());
        
        // Go back an extra day in each case.  The assumption is that this scripts is run shortly after midnight GMT
        
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1-totalApmRetention);  
        apmTotalStartDay = dateFormat.format(cal.getTime());
        
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1-includedApmRetention);  
        apmIncludedStartDay = dateFormat.format(cal.getTime());
        
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1-totalApmErrorRetention);  
        apmErrTotalStartDay = dateFormat.format(cal.getTime());
        
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1-includedApmErrorRetention);  
        apmErrIncludedStartDay = dateFormat.format(cal.getTime());
	}

	public String getApmTotalStartDay() {
		return apmTotalStartDay;
	}

	public String getApmIncludedStartDay() {
		return apmIncludedStartDay;
	}

	public String getApmErrTotalStartDay() {
		return apmErrTotalStartDay;
	}

	public String getApmErrIncludedStartDay() {
		return apmErrIncludedStartDay;
	}

	public String getYesterday() {
		return yesterday;
	}
	
	public String earliestStartDay() {
		if(totalApmRetention > totalApmErrorRetention) {
			return apmTotalStartDay;
		}
		return apmErrTotalStartDay;
	}
	
	
}
