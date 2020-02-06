package com.newrelic.gtm.usage.handler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.newrelic.gtm.usage.LambdaParameters;
import com.newrelic.gtm.usage.api.util.InsightsQuery;
import com.newrelic.gtm.usage.api.util.InsightsUpdate;
import com.newrelic.gtm.usage.api.util.NrURL;
import com.newrelic.gtm.usage.event.ScriptReportEvent;

public abstract class AbstractFacetHandler implements RequestHandler<Object, String> {

	protected LambdaParameters lambdaParams = new LambdaParameters();
	protected ScriptReportEvent scriptEvent = new ScriptReportEvent(); 
	protected InsightsQuery insightsQuery = null;
	protected String insightsQueryEndpoint = null;
	protected String queryKey = null;
	protected String batchId;
	
	
	protected AbstractFacetHandler() {
	}
	
	protected void initalizeScriptReportEvent(String eventType) {
		scriptEvent.addScriptType(eventType);
		scriptEvent.addBatchId(batchId);
	    scriptEvent.put("timestamp_start", new Date());
	    scriptEvent.put("consumingAccountId", lambdaParams.getAccountId());
	    scriptEvent.put("usageDay", lambdaParams.getUsageDay());
	}
	
	protected void finalizeScriptReportEventAndSend() {
		scriptEvent.setScriptDuration();
		
		JSONArray importArr = new JSONArray();
		importArr.put(scriptEvent.getJSONObject());
		InsightsUpdate.postAppUsageToInsights(lambdaParams,importArr);
	}

	protected void generateBatchId() {

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Calendar cal = Calendar.getInstance();  
        batchId = dateFormat.format(cal.getTime());	      	
	}	

	protected void initializeRequestVariables(Object input) {
		lambdaParams.populate(input);
		insightsQuery = new InsightsQuery(lambdaParams);
		insightsQueryEndpoint = NrURL.insightsQuery(lambdaParams.getAccountId());
		queryKey = lambdaParams.getQueryInsightsKey();		
		
	}

}
