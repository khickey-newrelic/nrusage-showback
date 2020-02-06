package com.newrelic.gtm.usage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;

import org.json.JSONException;
import org.json.JSONObject;

public class LambdaParameters {

	private String accountId;
	private String NRapiKey;
	private String queryInsightsKey;	
	private String insertInsightsKey;
	private String destinationAccountId;  // Optional.  Specify only if creating custom events in a different account from source data.
	private String usageDay;  // Optional.  'today', 'yesterday' or day in 'yyyy-MM-dd'.  Defaults to yesterday. Not applicable for Insights storage usage, which always goes off of yesterday.
	private String timeZone; // Optional.  +/-hhhh  Defaults to -0500.  Not applicable for Insights storage usage.
	
	public LambdaParameters() {
		super();
	}

	public void populate(Object input) {
		try {
			
			JSONObject params = null;
			
			if(input.getClass() == String.class) {
				params = new JSONObject(input.toString());
			} else if(input.getClass() == JSONObject.class) {
				params = (JSONObject) input;
			} else {
				params = new JSONObject((LinkedHashMap)input);
			}
			
			accountId = params.getString("account_id");
			NRapiKey = params.getString("api_key");
			queryInsightsKey = params.getString("query_key");		
			insertInsightsKey = params.getString("insert_key");
			
			// Destination account id is optional.  If not set, assume destination is same as origin.
			if(params.has("destination_account_id")){
				destinationAccountId = params.getString("destination_account_id");
			} else {
				destinationAccountId = accountId;
			}
			
			if(params.has("usageDay")) {
				String usageDayValue = params.getString("usageDay");
				if(usageDayValue.equalsIgnoreCase("today")) {
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			        Calendar cal = Calendar.getInstance();  
			        usageDay = dateFormat.format(cal.getTime());
					
				} else if (usageDayValue.equalsIgnoreCase("yesterday")) {
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			        Calendar cal = Calendar.getInstance();
			        cal.add(Calendar.DATE, -1);    
			        usageDay = dateFormat.format(cal.getTime());
				} else {
					usageDay = usageDayValue;
				}
				
			} else {
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		        Calendar cal = Calendar.getInstance();
		        cal.add(Calendar.DATE, -1);    
		        usageDay = dateFormat.format(cal.getTime());
			}
			
			if(params.has("timeZone")) {
				timeZone=params.getString("timeZone");
			}else {
				timeZone="-0500"; 
			}
							
		} catch (JSONException e) {
			
			System.out.println("Please enter account_id,insert_key,query_key and api_key. destination_account_id,usageDay,timeZone are optional\n");
			e.printStackTrace();
		}
	}

	public String getAccountId() {
		return accountId;
	}
	
	public String getDestinationAccountId() {
		return destinationAccountId;
	}

	public String getNRapiKey() {
		return NRapiKey;
	}

	public String getQueryInsightsKey() {
		return queryInsightsKey;
	}

	public String getInsertInsightsKey() {
		return insertInsightsKey;
	}

	public String getUsageDay() {
		return usageDay;
	}

	public String getTimeZone() {
		return timeZone;
	}	
	
}
