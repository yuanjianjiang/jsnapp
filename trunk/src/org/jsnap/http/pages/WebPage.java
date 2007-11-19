package org.jsnap.http.pages;

import org.jsnap.http.base.HttpRequest;

public interface WebPage {
	public static final String CATEGORY_INFORMATION = "Information"; 
	public static final String CATEGORY_STATISTICS = "Statistics";
	public static final String CATEGORY_MANAGEMENT = "Management";

	public String key();
	public String name();
	public String category();
	public String stylesheet();
	public boolean administrative();
	public WebResponse data(HttpRequest request);
}
