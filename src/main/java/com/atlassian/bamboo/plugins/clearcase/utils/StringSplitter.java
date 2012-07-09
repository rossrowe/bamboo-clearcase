package com.atlassian.bamboo.plugins.clearcase.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Split string into Array of String based on delimiter passed, 
 * default delimiter splits by lines. 
 * <p>
 * The item by default are return in the order found.  You can have
 * items returned in sorted order by setting sorted to true.
 */
public class StringSplitter extends AbstractTokenProcessor {

	private List<String> items = new ArrayList<String>();
	private boolean sorted = false;
	
	private boolean processed = false;
	
	public StringSplitter(String data, String delimExp, boolean sorted) {
		super(data, delimExp);
		this.sorted = sorted;
	}

	public boolean isSorted() {
		return sorted;
	}

	/**
	 * Always sort the items.
	 * @param sorted
	 */
	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	public StringSplitter(String data) {
		super(data);
	}

	/* (non-Javadoc)
	 * @see au.gov.dva.bamboo.cc.repository.AbstractTokenProcessor#processToken(java.lang.String)
	 */
	@Override
	protected void processToken(String token) {
		items.add(token);
	}

	/**
	 * Process the string into it parts now and sort if required.
	 * if process has already occurred calling this method does nothing.
	 */
	private void processNow()
	{
		if(!processed )
		{
			process();
			if(sorted)
			{
				Collections.sort(items);
			}
			processed =true;
		}
	}
	
	/**
	 * A reference to list of items is returned in order found or
	 * sorted if {@link #isSorted()} is true.  If the data has not
	 * been procesed calling this method cause it to occur now.
	 * 
	 * @return the split strings.
	 */
	public List<String> getItems() {
		processNow();
		return items;
	}
}
