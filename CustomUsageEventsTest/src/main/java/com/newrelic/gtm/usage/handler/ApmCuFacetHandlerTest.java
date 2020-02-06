package com.newrelic.gtm.usage.handler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.newrelic.gtm.usage.LambdaParameters;
import com.newrelic.gtm.usage.api.util.InsightsQuery;
import com.newrelic.gtm.usage.api.util.NrURL;
import com.newrelic.gtm.usage.testutil.LambdaParamGenerator;
import com.newrelic.gtm.usage.testutil.TestContext;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class ApmCuFacetHandlerTest {

    private static Object input;
    private static LambdaParamGenerator generator = new LambdaParamGenerator();

    @BeforeClass
    public static void createInput() throws IOException {   
    	input = generator.testParam1().toString();    			
    }

    private Context createContext() {
        TestContext ctx = new TestContext();

        // TODO: customize your context here if needed.
        ctx.setFunctionName("Your Function Name");

        return ctx;
    }

    @Test
    public void testApmCuFacetHandler() {
        ApmCuFacetHandler handler = new ApmCuFacetHandler();
        Context ctx = createContext();

        String batchId = handler.handleRequest(input, ctx);
        
        System.out.println("Result batchId = "+batchId);

        // Validates the numbers add up
        assertSumsMatch(batchId);
       
    }
    
    /**
     * Asserts sum of data units across events equals sum from Transaction event type.
     * May need to revisit timing after making inserts asynchronous.  For now, it's a good test of logic. 
     * @TODO Check percentage sum as well.
     */
    private void assertSumsMatch(String batchId) {
    	
    	LambdaParameters params = new LambdaParameters();
    	params.populate(input);
    	InsightsQuery insightsQuery = new InsightsQuery(params);
    	
		String insightsEndpoint = NrURL.insightsQuery(params.getAccountId());
		String queryKey = params.getQueryInsightsKey();
		String query = insightsQuery.nrUsageApmComputeUnits();
		
		String result = insightsQuery.executeQuery(insightsEndpoint, queryKey, query);
		BigDecimal totalComputeUnits = new BigDecimal(new JSONObject(result)
				.getJSONArray("results")
				.getJSONObject(0).getDouble("sum"));
		totalComputeUnits = totalComputeUnits.setScale(0, RoundingMode.DOWN);

		
		insightsEndpoint = NrURL.insightsQuery(params.getDestinationAccountId());
		query = insightsQuery.hostUsageSumUsageEvents(batchId);		
		
		result = insightsQuery.executeQuery(insightsEndpoint, generator.getDestinationQueryKey1(), query);
		BigDecimal totalComputeUnitsFromCus = new BigDecimal(new JSONObject(result)
				.getJSONArray("results")
				.getJSONObject(0).getDouble("sum"));
		totalComputeUnitsFromCus = totalComputeUnitsFromCus.setScale(0, RoundingMode.DOWN);
				
		Assert.assertFalse("Total data units should not be 0", 0 == totalComputeUnitsFromCus.compareTo(new BigDecimal(0.0)));
		Assert.assertTrue("TotalInsightsEvents should be same as sum of Insights events from ApmDuEvents. totalComputeUnitsFromCus = " +
				totalComputeUnitsFromCus+", totalComputeUnits = "+totalComputeUnits,
				0 == totalComputeUnitsFromCus.compareTo(totalComputeUnits));		
		
    }
}
