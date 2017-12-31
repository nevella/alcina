package cc.alcina.framework.gwt.client.logic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.NativeEvent;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

@RegistryLocation(registryPoint = AlcinaPrehistory.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class AlcinaPrehistory {
	private Map<String, PreHistoryHandler> preHistoryHandlerMap;

	public void handlePreHistory(NativeEvent event, AlcinaHistoryItem item) {
		String preHistory = item.getPreHistory();
		if (preHistoryHandlerMap == null) {
			preHistoryHandlerMap = new LinkedHashMap<String, PreHistoryHandler>();
			List<Class> lookup = Registry.get().lookup(PreHistoryHandler.class);
			for (Class clazz : lookup) {
				PreHistoryHandler handler = (PreHistoryHandler) Reflections
						.classLookup().newInstance(clazz);
				handler.register(preHistoryHandlerMap);
			}
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

	@ClientInstantiable
	@RegistryLocation(registryPoint = PreHistoryHandler.class)
	public abstract static class PreHistoryHandler<I extends AlcinaHistoryItem> {
		public abstract void handle(NativeEvent event, I item);

		public abstract String key();

		public void register(Map<String, PreHistoryHandler> toMap) {
			toMap.put(key(), this);
		}
	}
}
