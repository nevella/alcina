package cc.alcina.template.cs;

import cc.alcina.framework.gwt.client.logic.AlcinaHistory;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

public class AlcinaTemplateHistory extends
		AlcinaHistory<AlcinaTemplateHistoryItem> implements
		ValueChangeHandler<String> {
	public static final String BOOKMARKS_TAB = "bookmarks";

	@Override
	protected void initTokenDisplayNames() {
		if (tokenDisplayNames.size() == 0) {
			tokenDisplayNames.put(BOOKMARKS_TAB, "Bookmarks");
		}
	}

	public void onValueChange(ValueChangeEvent<String> event) {
		onHistoryChanged(event.getValue());
	}

	@Override
	public AlcinaTemplateHistoryItem createHistoryInfo() {
		return new AlcinaTemplateHistoryItem();
	}

	private AlcinaTemplateHistory() {
		super();
	}

	private static AlcinaTemplateHistory theInstance;

	public static AlcinaTemplateHistory get() {
		if (theInstance == null) {
			theInstance = new AlcinaTemplateHistory();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}
}
