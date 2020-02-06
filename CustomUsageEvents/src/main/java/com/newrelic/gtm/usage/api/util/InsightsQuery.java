package com.newrelic.gtm.usage.api.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import com.newrelic.gtm.usage.LambdaParameters;
import com.newrelic.gtm.usage.event.ApmCuEvent;
import com.newrelic.gtm.usage.event.ApmDuEvent;

/**
 * Builds Insights queries to be used in determining usage.
 *
 * @TODO Move away from '1 day ago' to 00:00:00 to 23:59:00' previous day
 * @TODO Events are events from that day.  This does not match the usage breakdown
 * @TODO Modify event counting to not include last 8 days as this is included retention
 */
public class InsightsQuery {
	
	private String usageDay;
	private String timeZone;
	
	public InsightsQuery(String usageDay, String timeZone) {
		super();
		this.usageDay = usageDay;
		this.timeZone = timeZone;
	}
	
	public InsightsQuery(LambdaParameters lambdaParameters) {
		this.usageDay = lambdaParameters.getUsageDay();
		this.timeZone = lambdaParameters.getTimeZone();
	}

	/** The documented limit for uniques is 10000. Test with larger data sets before approaching 10,000. 
	 * The default maximum number of results returned is 1000. */ 
	public static final int MAX_UNIQUES = 5000;
	
	/** 
	 * APM Event Storage Usage Queries.
	 */
	
	
	public String licensedRetentionAPM() {
		return "SELECT insightsIncludedRetentionInHours, insightsTotalRetentionInHours from NrDailyUsage where insightsEventNamespace='APM' since 30 hours ago";
	}
	
	public String licensedRetentionAPMErrors() {
		return "SELECT insightsIncludedRetentionInHours, insightsTotalRetentionInHours from NrDailyUsage where insightsEventNamespace='APM Errors' since 30 hours ago";
	}
	
	public String insightsUniqueApplications(String startDay) {
		return "SELECT uniques(appId, "+MAX_UNIQUES+ ") from Transaction, TransactionError";// SINCE '"+startDay+"'";
	}
	
	public String allApmEvents(String startDay) {
		return "SELECT count(*) from Transaction";// SINCE '"+startDay+"'";
	}
	
	public String allApmErrEvents(String startDay) {
		return "SELECT count(*) from TransactionError";// SINCE '"+startDay+"'";
	}
	
	public String allApmEvents(ArrayList<Integer> appIds, String startDay) {
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("SELECT count(*) from Transaction where appId in (");
		boolean isFirst = true;
		for(Integer appId:appIds) {
			if(!isFirst) {
				buffer.append(",");
			}
			buffer.append("'").append(appId).append("'");
			isFirst = false;
			
		}
		buffer.append(") facet appId, appName");
		buffer.append(" SINCE '"+startDay+"'");
		System.out.println("allApmEvents"+buffer.toString());
		return buffer.toString();
		
	}
	
	public String allApmErrEvents(ArrayList<Integer> appIds, String startDay) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("SELECT count(*) from TransactionError where appId in (");
		boolean isFirst = true;
		for(Integer appId:appIds) {
			if(!isFirst) {
				buffer.append(",");
			}
			buffer.append("'").append(appId).append("'");
			isFirst = false;
			
		}
		buffer.append(") facet appId, appName");
		buffer.append(" SINCE '"+startDay+"'");
		return buffer.toString();
	}

	/** 
	 * Total APM Events stored.
	 * @TODO Factor in 8 days of free retention
	 */
	public String apmEventCount() {
		return "SELECT count(*) from Transaction, TransactionError";
	}
	
	/** 
	 * Total APM Events stored for a particular app.
	 * @TODO Factor in 8 days of free retention
	 */
	public String appApmEventCount(Integer appId) {
		return "SELECT count(*) from Transaction,TransactionError where appId =" + appId + " facet appName";
	}
	
	/**
	 * Data Unit Queries
	 */
		
	
	/** 
	 * APM Events for a day that count toward Data Unit Usage
	 * @param appId apm App id
	 */
	public String appApmDataUnitCount(ArrayList<Integer> appIds) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("SELECT count(*) from Transaction,TransactionError, Span where appId in (");
		boolean isFirst = true;
		for(Integer appId:appIds) {
			if(!isFirst) {
				buffer.append(",");
			}
			buffer.append("'").append(appId).append("'");
			isFirst = false;
			
		}
		buffer.append(") facet appId, appName");
		buffer.append(timePeriod());
		return buffer.toString();
	}
	
	/**
	 * All APM Events for a day that count toward Data Unit Usage.
	 * Lambda functions have appId = 0.  This query filters them out. 
	 */
	public String appApmDataUnitCount() {
		return "SELECT count(*) from Transaction, TransactionError, Span  where appId != 0"+timePeriod();
	}

	/**
	 * All APM Events for a day that count toward Data Unit Usage.
	 * Lambda functions have appId = 0.  This query filters them out. 
	 */
	public String dataUnitUniqueApps() {
		return "SELECT uniques(appId,"+MAX_UNIQUES+") from Transaction, TransactionError, Span where appId != 0"+timePeriod();
	}
	
	public String sumDataUnitFromDuEvents(String batchId) {
		return "SELECT sum("+ ApmDuEvent.getApmEventCountKey()
			+"), sum("+ApmDuEvent.getPercentKey()
			+")*100 from "+ApmDuEvent.getApmDuEventType()
			+" where batchId = '"+batchId+"'";
	}
	
	/**
	 * NRDailyUsage Queries - Sums
	 */

	
	/** 
	 * Host usage for a list of hosts.  Results faceted by hostId.
	 * @param hostIds List of host ids
	 */
	public String hostUsageSum(ArrayList<String> hostIds) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("SELECT sum(apmComputeUnits) from NrDailyUsage  where productLine='APM'");
		buffer.append(" and usageType='Host' and hostId in (");
		
		boolean isFirst = true;
		for(String hostId:hostIds) {
			if(!isFirst) {
				buffer.append(",");
			}
			buffer.append("'").append(hostId).append("'");
			isFirst = false;
			
		}
		buffer.append(") facet hostId, consumingAccountId");
		buffer.append(timePeriod());
		return buffer.toString();
	}
	
	/**
	 * Total of apm compute units for account. 
	 */
	public String nrUsageApmComputeUnits() {
		return "SELECT sum(apmComputeUnits) from NrDailyUsage where productLine='APM' and usageType='Host' "
				+ timePeriod();
	}
	
	/**
	 * Host usage for day.  
	 * @return
	 */
	public String hostUsageSumUsageEvents(String batchId) {
		return "SELECT sum(computeUnits) from "+ApmCuEvent.getApmCuEventType()
		+" where batchId = '"+batchId+"'";	
	}
	
	/**
	 * NRDailyUsage Queries - Uniques
	 */

	/**
	 * Gets all the hosts associated with an app from NrDailyUsage
	 * @param appId
	 * @return
	 * @TODO Change from 1 day to 24 hours on day before
	 */
	public String nrUsageUniqueHostIdsForAppId(Integer appId) {
		return "SELECT uniques(hostId) from NrDailyUsage where productLine='APM' and usageType='Application' "
				+ " and apmAppId =" + appId 
				+ timePeriod();
	}
	
	/**
	 * Gets all the hosts associated with an app from NrDailyUsage
	 * @param appId
	 * @return
	 * @TODO Change from 1 day to 24 hours on day before
	 */
	public String nrUsageUniqueHostIdsForAppId(ArrayList<Integer> appIds) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("SELECT uniques(hostId) from NrDailyUsage where productLine='APM' and usageType='Application' ");
		buffer.append(" and apmAppId in (");
		boolean isFirst = true;
		for(Integer appId:appIds) {
			if(!isFirst) {
				buffer.append(",");
			}
			buffer.append("'").append(appId).append("'");
			isFirst = false;
			
		}
		buffer.append(") facet apmAppId");
		buffer.append(timePeriod());
		
		return buffer.toString();
	}
	
	/**
	 * Counts unique hosts.  This may be an approximate number. 
	 */
	public String nrUsageUniqueHostIdCount() {
		return "SELECT uniqueCount(hostId) from NrDailyUsage where productLine='APM' and usageType='Host' "
				+ timePeriod();
	}
	
	/**
	 * Gets unique APM hosts from NrDailyUsage. 
	 */
	public String nrUsageUniqueHostIds() {
		return "SELECT uniques(hostId,"+MAX_UNIQUES+") from NrDailyUsage where productLine='APM' and usageType='Host' "
				+ timePeriod();
	}
	
	/** 
	 * Counts unique applications.  This may be an approximate number. 
	 */
	public String nrUsageUniqueApplicationsCount() {
		return "SELECT uniqueCount(apmAppId) from NrDailyUsage where productLine='APM' and usageType='Application'"
				+ timePeriod();
	}
	
	/** 
	 * Gets all unique app ids in NrDailyUsage for 1 day.
	 * @return
	 */
	public String nrUsageUniqueApplications() {
		return "SELECT uniques(apmAppId,"+MAX_UNIQUES+") from NrDailyUsage where productLine='APM' and usageType='Application'"
				+ timePeriod();
	}
	
	/**
	 * Executes Insights query
	 * @param urlEndpoint Insights url including customer rpm id
	 * @param queryKey Insights query api key
	 * @param query Insights query
	 * @return
	 */

	public String executeQuery(String urlEndpoint, String queryKey, String query) {

		String insightsResults = "";
		try {

			URL url = new URL(urlEndpoint + URLEncoder.encode(query, "UTF-8"));
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("X-Query-Key", queryKey);
			if (conn.getResponseCode() != 200) {
				System.out.println("Query execution failure:");
				System.out.println(" url = "+urlEndpoint);
				System.out.println(" queryKey ="+queryKey);
				System.out.println(" query="+query);
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output = "";

			while ((output = br.readLine()) != null) {

				insightsResults = insightsResults + output;
				// System.out.println(output + "\n");
			}
			conn.disconnect();

		} catch (MalformedURLException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		}

		return insightsResults;

	}
	
	public String timePeriod() {
		
		String startHour = "00:00:00";
		String endHour = "23:59:59";

        return " SINCE '" + usageDay + " "+startHour+ timeZone + "'" +
        		" UNTIL '" + usageDay + " "+endHour+ timeZone + "'";		
	}
	
	/** 
	 * Used with Insights Event Counting.  Timezone doesn't make sense here.
	 */
	public String timePeriod(String startDay, String endDay) {
		
		String startHour = "00:00:00";
		String endHour = "23:59:59";

        return " SINCE '" + startDay + " "+startHour+"'" +
        		" UNTIL '" + endDay + " "+endHour+ "'";		
	}
	

}
