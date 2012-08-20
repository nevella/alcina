package cc.alcina.framework.gwt.client.logic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

import com.google.gwt.dom.client.NativeEvent;

public class AlcinaPrehistory {
	private Map<String, PreHistoryHandler> preHistoryHandlerMap;

	private AlcinaPrehistory() {
		super();
	}

	private static AlcinaPrehistory theInstance;

	public static AlcinaPrehistory get() {
		if (theInstance == null) {
			theInstance = new AlcinaPrehistory();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public void handlePreHistory(NativeEvent event, AlcinaHistoryItem item) {
		String preHistory = item.getPreHistory();
		if (preHistoryHandlerMap == null) {
			preHistoryHandlerMap = new LinkedHashMap<String, PreHistoryHandler>();
			List<Class> lookup = Registry.get().lookup(PreHistoryHandler.class);
			for (Class clazz : lookup) {
				PreHistoryHandler handler = (PreHistoryHandler) CommonLocator
						.get().classLookup().newInstance(clazz);
				handler.register(preHistoryHandlerMap);
			}
		}
		if (CommonUtils.isNotNullOrEmpty(preHistory)
				&& !WidgetUtils.isNewTabModifier(event)) {
			PreHistoryHandler handler = preHistoryHandlerMap.get(preHistory);
			if (handler != null) {
				handler.handle(event, item);
			} else {
				System.out.println("Prehistory handler not found: "
						+ preHistory);
			}
			event.stopPropagation();
			event.preventDefault();
		}
	}

	@ClientInstantiable
	@RegistryLocation(registryPoint = PreHistoryHandler.class, j2seOnly = false)
	public abstract static class PreHistoryHandler<I extends AlcinaHistoryItem> {
		public abstract String key();

		public void register(Map<String, PreHistoryHandler> toMap) {
			toMap.put(key(), this);
		}

		public abstract void handle(NativeEvent event, I item);
	}
}
