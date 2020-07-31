package cc.alcina.extras.dev.console.remote.client.common.resources;

import cc.alcina.framework.gwt.client.dirndl.StyleType;

public enum RemoteConsoleStylesSearchResults implements StyleType {
	SEARCH_RESULTS, HEAD, RESULT;
	public enum RemoteStylesSearchResultsHead implements StyleType {
		TITLE, SUBTITLE
	}

	public enum RemoteStylesSearchResultsResult implements StyleType {
		TITLE, SUBTITLE, EXTRACT
	}
}
