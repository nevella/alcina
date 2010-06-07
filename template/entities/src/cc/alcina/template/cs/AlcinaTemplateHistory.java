package cc.alcina.template.cs;

import cc.alcina.framework.common.client.logic.template.AlcinaTemplate;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

@AlcinaTemplate
public class AlcinaTemplateHistory extends AlcinaHistory implements
		ValueChangeHandler<String> {
	public static final String BOOKMARKS_TAB = "bookmarks";

	@Override
	protected void initTokenDisplayNames() {
		if (tokenDisplayNames.size() == 0) {
			tokenDisplayNames.put(BOOKMARKS_TAB, "Bookmarks");
		}
	}

	public static class AlcinaTemplateHistoryEventInfo extends HistoryInfoBase {
	}

	public void onValueChange(ValueChangeEvent<String> event) {
		onHistoryChanged(event.getValue());
	}

	public AlcinaTemplateHistoryEventInfo parseToken(String historyToken) {
		AlcinaTemplateHistoryEventInfo info = (AlcinaTemplateHistoryEventInfo) super
				.parseToken(historyToken);
		return info;
	}

	@Override
	public HistoryInfoBase createHistoryInfo() {
		return new AlcinaTemplateHistoryEventInfo();
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
