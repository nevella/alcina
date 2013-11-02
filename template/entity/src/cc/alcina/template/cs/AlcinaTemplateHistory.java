package cc.alcina.template.cs;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

@RegistryLocation(registryPoint = AlcinaHistory.class, implementationType = ImplementationType.SINGLETON)
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


	public static AlcinaTemplateHistory get() {
		return (AlcinaTemplateHistory) Registry.impl(AlcinaHistory.class);
	}
}
