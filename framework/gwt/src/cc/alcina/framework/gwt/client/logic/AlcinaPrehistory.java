package cc.alcina.framework.gwt.client.logic;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.dom.client.NativeEvent;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

@Reflected
@Registration.Singleton
public class AlcinaPrehistory {
	private Map<String, PreHistoryHandler> preHistoryHandlerMap;

	public void handlePreHistory(NativeEvent event, AlcinaHistoryItem item) {
		String preHistory = item.getPreHistory();
		if (preHistoryHandlerMap == null) {
			preHistoryHandlerMap = new LinkedHashMap<String, PreHistoryHandler>();
			Registry.query(PreHistoryHandler.class).implementations()
					.forEach(h -> h.register(preHistoryHandlerMap));
		}
		if (CommonUtils.isNotNullOrEmpty(preHistory)
				&& !WidgetUtils.isNewTabModifier(event)) {
			PreHistoryHandler handler = preHistoryHandlerMap.get(preHistory);
			if (handler != null) {
				handler.handle(event, item);
			} else {
				System.out
						.println("Prehistory handler not found: " + preHistory);
			}
			event.stopPropagation();
			event.preventDefault();
		}
	}

	@Reflected
	@Registration(PreHistoryHandler.class)
	public abstract static class PreHistoryHandler<I extends AlcinaHistoryItem> {
		public abstract void handle(NativeEvent event, I item);

		public abstract String key();

		public void register(Map<String, PreHistoryHandler> toMap) {
			toMap.put(key(), this);
		}
	}
}
