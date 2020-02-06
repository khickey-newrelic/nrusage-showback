package com.newrelic.gtm.usage.testutil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.json.JSONObject;

/**
 * Generates JSON Objects to pass to lambda functions.
 *
 */
public class LambdaParamGenerator {
	
	private Properties properties;
	
	public LambdaParamGenerator() {
		try {
			String paramFile = System.getProperty("paramFile");
			if(paramFile == null) {
				throw new RuntimeException("Property 'paramFile' is invalid or missing");
			}
		  
			properties = new Properties();		

			InputStream inputStream = new FileInputStream(paramFile);

			properties.load(inputStream);			

		}catch(FileNotFoundException fe) {
			System.out.println("File not found");
			fe.printStackTrace();
		}catch(IOException ie) {
			System.out.println("IO Exception");
			ie.printStackTrace();
		} 
		
	}
	
	public JSONObject testParam1() {
		
    	return readFromFile();
	}
	
	public JSONObject today() {
		JSONObject tempObj = testParam1();
		
    	tempObj.put("usageDay", "today"); 
    	tempObj.put("timeZone", "-0100"); 
    	
    	
    	return tempObj;
	}
	
	public JSONObject yesterday() {
		JSONObject tempObj = testParam1();
		
    	tempObj.put("usageDay", "yesterday"); 
    	tempObj.put("timeZone", "-0200"); 
    	
    	
    	return tempObj;
	}
	
	public JSONObject otherDay() {
		JSONObject tempObj = testParam1();
		
    	tempObj.put("usageDay", "2020-01-12");  
    	tempObj.put("timeZone", "-0700"); 
    	
    	
    	return tempObj;
	}
	
	private JSONObject readFromFile() {
		JSONObject tempObj = new JSONObject();			
			
		tempObj.put("account_id", properties.getProperty("test1.account_id"));
    	tempObj.put("api_key", properties.getProperty("test1.api_key")); 
    	tempObj.put("query_key", properties.getProperty("test1.query_key")); 
    	
    	tempObj.put("destination_account_id", properties.getProperty("test1.destination_account_id")); 
    	tempObj.put("insert_key", properties.getProperty("test1.insert_key"));		  
		
		return tempObj;
	}
	
	public JSONObject defaultDay() {
		JSONObject tempObj = testParam1();
    	
    	return tempObj;
	}
	
	public String getDestinationQueryKey1() {
		String destinationQueryKey = properties.getProperty("test1.destination_query_key");
		return destinationQueryKey;
	}

}
