package cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor;

import com.google.gwt.user.client.ui.SuggestOracle;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;

public interface AppSuggestion extends SuggestOracle.Suggestion {
	String provideFirst();

	Class<? extends ModelEvent> modelEvent();

	String url();

	String secondary();

	Object eventData();

	AppSuggestionCategory category();
}