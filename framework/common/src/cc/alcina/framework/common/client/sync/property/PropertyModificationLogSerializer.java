package cc.alcina.framework.common.client.sync.property;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.GwtCloneable;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;

public class PropertyModificationLogSerializer {
	public PropertyModificationLogSerializer() {
	}

	public String serialize(PropertyModificationLog log) {
		if (log == null) {
			return null;
		}
		ensureLookups();
		return Registry.impl(AlcinaBeanSerializer.class)
				.registerLookups(abbrevLookup, reverseAbbrevLookup)
				.serialize(log);
	}

	public PropertyModificationLog deserialize(String string) {
		if (string == null) {
			return new PropertyModificationLog();
		}
		ensureLookups();
		return Registry.impl(AlcinaBeanSerializer.class)
				.registerLookups(abbrevLookup, reverseAbbrevLookup)
				.deserialize(string);
	}

	Map<String, Class> abbrevLookup = new LinkedHashMap<>();

	Map<Class, String> reverseAbbrevLookup = new LinkedHashMap<>();

	private void ensureLookups() {
		ensureLookups("mods", PropertyModificationLog.class);
		ensureLookups("mod", PropertyModificationLogItem.class);
	}

	private void ensureLookups(String abbrev, Class clazz) {
		abbrevLookup.put(abbrev, clazz);
		reverseAbbrevLookup.put(clazz, abbrev);
	}
}
