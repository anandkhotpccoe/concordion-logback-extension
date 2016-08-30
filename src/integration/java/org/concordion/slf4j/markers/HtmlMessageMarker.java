package org.concordion.slf4j.markers;

import org.slf4j.helpers.ConcordionMarker;

public class HtmlMessageMarker extends ConcordionMarker {
	private static final long serialVersionUID = 5412731321120168078L;
	public static final String MARKER_NAME = "HTML_MESSAGE"; 
	
	private String format;

	public HtmlMessageMarker(String format) {
		super(MARKER_NAME);

		this.format = format;
	}

	public String getFormat() {
		return format;
	}
}