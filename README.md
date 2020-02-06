# newrelic-usage-showback
This repository contains two eclipse projects:<ul>
<li>CustomUsageEvents</li>
   Contains AWS functions that create custom but simplied NrDailyUsageEvents - that show how usage pertains to application labels
   This allows customers to map APM compute units or APM data units back to cost centers, departments, environments, and development teams.
<li>CustomUsageEventTest</li>
   Junit code that exercises the handlers without having to deploy them as lambda functions.  Handler tests verify totals against orginal
   NrDailyUsage numbers or Insights totals to verify accuracy   
</ul>   
   
<h1>Handlers in CustomUsageEvents</h1>
<h2>ApmCuFacetHandler</h2>
<ul>
   <li>Relevant for customers that measure APM usage by compute units, which is the most common use case.</li>
   <li>Uses labels rest api to obtain mapping from labels to applications.</li>
   <li>Uses NRDailyUsage (productLine='APM' usageType='Application') to map applications to hostId, and therefore labels to hostId</li>
   <li>Creates an LabelApmCuUsage for every NRDailyUsage event where productLine='APM' usageType='Host'</li>
   </ul>
<h2>ApmDuFacetHandler</h2>
   <ul>
   <li>Relevant for customers that measure APM usge by data units - counting Transaction, TransactionError, Span events.  This is not
   a typical licensing model and does not apply to most customers.</li>
   <li>Uses labels rest api to obtain mapping from labels to applications.</li>
   <li>For each application, queries the total number of Transaction, TransactionError, Span events for the given day</li>
   <li>Creates a LabelApmDuUsage for each application with those event types</li>
</ul>
   
<h1>How to use</h1>
<p>
   Each handler class is intended to be deployed as a lambda function.  It is possible to create a lambda function directly 
   from the handler within eclipse if using the correct AWS plugin. </li>
   Each handler is defaulted to run usage from the previous day, with the intention of it being run after midnight locally. 
   This is 
   something that can be set within AWS.
   NrDailyUsage events are generally created just before 12am GMT.  Check your account - you can verify the typical time of 
   day by using the data explorer in the classic UI.
   If running the same day, make sure to leave enough time after the NrDailyUsage events populate. 
</p>   
   
 <h1>Parameters</h1>
   <h2>Mandatory</h2>
   <ul>
   <li>account_id # Source account where apps are running</li>
   <li>api_key    # Rest api for source account</li>
   <li>query_key  # Insights query key</li>
   <li>insert_key # Insights insert key</li>
   </ul>
   
   <h2>Optional parameters</h2>
   <ul>
   <li>destination_account_id   # Useful if source accounts are sub-accounts but prefer to have custom usage events in master</li>
   <li>usage_day  # Defaults to previous day.  Valid parameters are "yesterday", "today", and the date in yyyy-MM-dd format</li>
   <li>timeZone   # Timezone in +/- hhhhh format.  Default is -0500, which is US Eastern standard time</li>
   </ul>
   
<h1>How to use - Test projects</h1>
<ul>
   <li>Handler tests can be run in elipses using the "Run As"->Junit test.</li>
   <li>Edit the run configuration to add a -DparamFile to the
      VM arguments to specify the file containing input.  eg. -DparamFile="sampleParams.txt"</li>
   <li>Set additional parameter destination_query_key.  This allows the test script to check totals in both source and destination accounts to verify they match</li>
   

