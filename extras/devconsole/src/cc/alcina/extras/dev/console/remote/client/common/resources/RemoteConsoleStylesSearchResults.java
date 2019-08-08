package cc.alcina.extras.dev.console.remote.client.common.resources;

import cc.alcina.framework.gwt.client.lux.LuxStyleType;

public enum RemoteConsoleStylesSearchResults implements LuxStyleType {
    SEARCH_RESULTS, HEAD, RESULT;
    public enum RemoteStylesSearchResultsHead implements LuxStyleType {
        TITLE, SUBTITLE
    }

    public enum RemoteStylesSearchResultsResult implements LuxStyleType {
        TITLE, SUBTITLE, EXTRACT
    }
}
