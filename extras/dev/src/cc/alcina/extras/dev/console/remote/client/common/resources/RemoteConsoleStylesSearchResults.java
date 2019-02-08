package cc.alcina.extras.dev.console.remote.client.common.resources;

import cc.alcina.framework.gwt.client.lux.LuxStylesType;

public enum RemoteConsoleStylesSearchResults implements LuxStylesType {
    SEARCH_RESULTS, HEAD, RESULT;
    public enum RemoteStylesSearchResultsHead implements LuxStylesType {
        TITLE, SUBTITLE
    }

    public enum RemoteStylesSearchResultsResult implements LuxStylesType {
        TITLE, SUBTITLE, EXTRACT
    }
}
