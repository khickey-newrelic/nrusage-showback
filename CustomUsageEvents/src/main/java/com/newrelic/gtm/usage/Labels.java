package com.newrelic.gtm.usage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class Labels {

	/** Label name and one or more values */
	HashMap<String, ArrayList<String>> labels;

	public Labels(String category, String name) {		
		this.labels = new HashMap<String, ArrayList<String>>();
		this.labels.put(category, new ArrayList<String>());
		this.labels.get(category).add(name);		
	}
	
	public Labels() {		
		this.labels = new HashMap<String, ArrayList<String>>();		
	}
	
	public ArrayList<String> getCategories() {
		
		ArrayList<String> categories = new ArrayList<String>();
		for(Map.Entry<String, ArrayList<String>> category : this.labels.entrySet()) {
			categories.add(category.getKey());
		}
		
		return categories;
	}
	
	public String getNames(String category) {
		
		ArrayList<String> names = new ArrayList<String>();
		names = this.labels.get(category);
		StringJoiner nameOut = new StringJoiner(",");
		for(String name: names) {
    			nameOut.add(name);
		}
		
		return nameOut.toString();
	}
	
	public void addLabel ( String category, String name) {
		
		if(this.labels.containsKey(category)) {
			this.labels.get(category).add(name);
		} else {
			this.labels.put(category, new ArrayList<String>());
			this.labels.get(category).add(name);
		}
	}
	
	public HashMap<String, ArrayList<String>> getLabelMap(){
		return labels;
	}

}
