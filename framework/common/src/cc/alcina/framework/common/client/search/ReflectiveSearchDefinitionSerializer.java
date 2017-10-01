package cc.alcina.framework.common.client.search;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.AlcinaTopics;

public class ReflectiveSearchDefinitionSerializer
		implements SearchDefinitionSerializer {
	private static final String RS0 = "rs0_";

	Map<String, Class> abbrevLookup = new LinkedHashMap<>();

	Map<Class, String> reverseAbbrevLookup = new LinkedHashMap<>();

	private SearchDefinition lastDef;

	private String lastStringDef;

	public ReflectiveSearchDefinitionSerializer() {
	}

	@Override
	public SearchDefinition deserialize(String serializedDef) {
		if (serializedDef.startsWith(RS0)) {
			serializedDef = serializedDef.substring(RS0.length());
		}
		ensureLookups();
		try {
			serializedDef = unescapeJsonForUrl(serializedDef);
			SearchDefinition def = Registry.impl(AlcinaBeanSerializer.class)
					.registerLookups(abbrevLookup, reverseAbbrevLookup)
					.deserialize(serializedDef);
			def.resetLookups();
			SearchDefinition defaultInstance = Reflections.classLookup()
					.newInstance(def.getClass());
			defaultInstance.resetLookups();
			defaultInstance.getCriteriaGroups().forEach(cg -> {
				CriteriaGroup ocg = def.criteriaGroup(cg.getClass());
				if (ocg == null) {
					def.getCriteriaGroups().add(cg);
				}
			});
			defaultInstance.getOrderGroups().forEach(cg -> {
				CriteriaGroup ocg = def.orderGroup(cg.getClass());
				if (ocg == null) {
					def.getOrderGroups().add(cg);
				}
			});
			return def;
		} catch (Exception e) {
			GWT.log("Exception in reflective search", e);
			AlcinaTopics.notifyDevWarning(e);
			return null;
		}
	}

	@Override
	public synchronized String serialize(SearchDefinition def) {
		if (lastDef == def) {
			return lastStringDef;
		}
		lastDef = def;
		lastStringDef = serialize0(lastDef);
		return lastStringDef;
	}

	private void ensureLookups() {
		if (abbrevLookup.isEmpty()) {
			List<Class> classes = Registry.get()
					.lookup(SearchDefinitionSerializationInfo.class);
			for (Class clazz : classes) {
				SearchDefinitionSerializationInfo info = Reflections
						.classLookup().getAnnotationForClass(clazz,
								SearchDefinitionSerializationInfo.class);
				if (info == null) {
					continue;
				}
				if (abbrevLookup.containsKey(info.value())) {
					throw new RuntimeException(
							"Searchdef serialization abbreviation collision:"
									+ info.value());
				}
				abbrevLookup.put(info.value(), clazz);
				reverseAbbrevLookup.put(clazz, info.value());
			}
		}
	}

	private String escapeJsonForUrl(String str) {
		StringBuilder sb = new StringBuilder();
		for (int idx = 0; idx < str.length(); idx++) {
			char c = str.charAt(idx);
			switch (c) {
			case '.':
				sb.append(".0");
				break;
			case '{':
				sb.append(".1");
				break;
			case '}':
				sb.append(".2");
				break;
			case '[':
				sb.append(".3");
				break;
			case ']':
				sb.append(".4");
				break;
			case ':':
				sb.append(".5");
				break;
			case ' ':
				sb.append(".6");
				break;
			case '"':
				sb.append(".7");
				break;
			case ',':
				sb.append(".8");
				break;
			case '&':
				sb.append(".9");
				break;
			default:
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private String serialize0(SearchDefinition def) {
		ensureLookups();
		try {
			def = def.cloneObject();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		SearchDefinition defaultInstance = Reflections.classLookup()
				.newInstance(def.getClass());
		defaultInstance.resetLookups();
		def.getCriteriaGroups().removeIf(cg -> {
			CriteriaGroup ocg = defaultInstance.criteriaGroup(cg.getClass());
			return ocg != null && ocg.equivalentTo(cg);
		});
		// order is important for order groups
		def.getOrderGroups().removeIf(cg -> {
			CriteriaGroup ocg = defaultInstance.orderGroup(cg.getClass());
			return ocg != null && ocg.equivalentTo(cg)
					&& ocg.getCriteria().isEmpty();
		});
		String str = Registry.impl(AlcinaBeanSerializer.class)
				.registerLookups(abbrevLookup, reverseAbbrevLookup)
				.serialize(def);
		return RS0 + escapeJsonForUrl(str);
	}

	private String unescapeJsonForUrl(String serializedDef) {
		StringBuilder sb = new StringBuilder();
		String unescapeMap = ".{}[]: \",&";
		for (int idx = 0; idx < serializedDef.length(); idx++) {
			char c = serializedDef.charAt(idx);
			if (c == '.') {
				char c2 = serializedDef.charAt(++idx);
				sb.append(unescapeMap.charAt(((int) c2) - 48));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
