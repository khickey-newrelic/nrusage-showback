package com.newrelic.gtm.usage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.gtm.usage.testutil.LambdaParamGenerator;

/**
 * A simple test harness for testing Host mapping functions, which were problematic during development.
 */
public class UsageDayTest {

	private static LambdaParameters  lambdaParams = new LambdaParameters();
	private static LambdaParamGenerator generator = new LambdaParamGenerator();
	
	/** Check today as usageDay parameter */
    @Test
    public void testUsageDayToday() {
    	
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();   
        String yesterday = dateFormat.format(cal.getTime());

    	lambdaParams.populate(generator.today());
    	
        lambdaParams.getUsageDay();
        Assert.assertTrue("UsageDay "+lambdaParams.getUsageDay()+" is not the same as expected day for today",
        		yesterday.equals(lambdaParams.getUsageDay()));
        Assert.assertTrue("Timezone "+lambdaParams.getTimeZone()+" is not the same as -0100",
        		new String("-0100").equals(lambdaParams.getTimeZone()));

    }

	/** Check yesterday as usageDay parameter */
    @Test
    public void testUsageDayYesterday() {
    	
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);    
        String yesterday = dateFormat.format(cal.getTime());

    	lambdaParams.populate(generator.yesterday());
    	
        lambdaParams.getUsageDay();
        Assert.assertTrue("UsageDay "+lambdaParams.getUsageDay()+" is not the same as expected day for yesterday",
        		yesterday.equals(lambdaParams.getUsageDay()));
        Assert.assertTrue("Timezone "+lambdaParams.getTimeZone()+" is not the same as -0200",
        		new String("-0200").equals(lambdaParams.getTimeZone()));

    }
    
	/** Check specified day as usageDay parameter */
    @Test
    public void testUsageDaySpecificDay() {
    	
		String expectedUsageDay = "2020-01-12";

    	lambdaParams.populate(generator.otherDay());
    	
        lambdaParams.getUsageDay();
        Assert.assertTrue("UsageDay "+lambdaParams.getUsageDay()+" is not the same as the expected day",
        		expectedUsageDay.equals(lambdaParams.getUsageDay()));
        Assert.assertTrue("Timezone "+lambdaParams.getTimeZone()+" is not the same as -0700",
        		new String("-0700").equals(lambdaParams.getTimeZone()));

    }
    
	/** Check specified day as usageDay parameter */
    @Test
    public void testUsageDayDefault() {
    	
    	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();   
        cal.add(Calendar.DATE, -1);   
        String yesterday = dateFormat.format(cal.getTime());

    	lambdaParams.populate(generator.testParam1());
    	
        lambdaParams.getUsageDay();
        Assert.assertTrue("UsageDay "+lambdaParams.getUsageDay()+" is not the same as the expected day "+yesterday,
        		yesterday.equals(lambdaParams.getUsageDay()));
        Assert.assertTrue("Timezone "+lambdaParams.getTimeZone()+" is not the same as -0500",
        		new String("-0500").equals(lambdaParams.getTimeZone()));

    }
    

}
