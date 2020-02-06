package com.newrelic.gtm.usage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.newrelic.gtm.usage.api.util.NrURL;
import com.newrelic.gtm.usage.event.ScriptReportEvent;
import com.newrelic.gtm.usage.testutil.LambdaParamGenerator;

/**
 * A simple test harness for testing Host mapping functions, which were problematic during development.
 */
public class HostLabelMapTest {

	private static LambdaParameters  lambdaParams = new LambdaParameters();
	private static LambdaParamGenerator generator = new LambdaParamGenerator();

    @BeforeClass
    public static void createInput() throws IOException {
    	
    	lambdaParams.populate(generator.testParam1());
    }

    /**
     * Checks that all hosts method returns no duplicates.
     */
    @Test
    public void testHostMap_AllHosts() {
    	
        MapHostsToLabels hostMap = new MapHostsToLabels(lambdaParams, new ScriptReportEvent());
        String insightsEndpoint = NrURL.insightsQuery(lambdaParams.getAccountId());
        String queryKey = lambdaParams.getQueryInsightsKey();
        
        ArrayList <String> allHosts = hostMap.allHosts(insightsEndpoint, queryKey);
        
        Assert.assertEquals(false, hasDuplicates(allHosts));
    }
    
	private boolean hasDuplicates(ArrayList<String> allHosts) {
		boolean result = false;
			
		int hostIdCount = 0;
		
		Set<String> set = new HashSet<String>(allHosts);
			
		ArrayList<String> uniqueHosts = new ArrayList<String>();
		for(String hostId:allHosts) {
			
			System.out.println("["+hostId+"]");
			if(uniqueHosts.contains(hostId.trim())) {
				System.out.println("Duplicate host: "+hostId);
				result = true;
			}
			else {
				uniqueHosts.add(hostId.trim());
				hostIdCount++;
			}
		}
		
		System.out.println("Set size ="+set.size());
		System.out.println("AllHosts size = "+allHosts.size());
		System.out.println("Host id count = "+hostIdCount);
		
		return result;
	}
}
