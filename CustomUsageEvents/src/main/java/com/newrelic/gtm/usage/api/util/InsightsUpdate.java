package com.newrelic.gtm.usage.api.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;

import com.newrelic.gtm.usage.LambdaParameters;

/**
 * Inserts events into Insights.  
 * @TODO Would making this its own lambda function improve performance?
 * @author khickey
 *
 */
public class InsightsUpdate {
	
	/**
	 * 
	 * @param lambdaParams
	 * @param importArr
	 * Can we re-use this connection somehow to save on overhead?
	 */
	public static void postAppUsageToInsights(LambdaParameters lambdaParams, JSONArray importArr) {

		String NRjson = "";
		try {
			
			String accountId = lambdaParams.getDestinationAccountId();
			String insertKey = lambdaParams.getInsertInsightsKey();

			URL url = new URL(NrURL.insightsImport(accountId));
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			//conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("X-Insert-Key", insertKey);
			
			OutputStream os = conn.getOutputStream();
			os.write(importArr.toString().getBytes("UTF-8"));
			os.flush();
			
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output="";
			
			while ((output = br.readLine()) != null) {
				NRjson = NRjson + output;
				//System.out.println(output + "\n");
			}
			conn.disconnect();

		  } catch (MalformedURLException e) {

			e.printStackTrace();

		  } catch (IOException e) {

			e.printStackTrace();

		  }
		
	}
}
