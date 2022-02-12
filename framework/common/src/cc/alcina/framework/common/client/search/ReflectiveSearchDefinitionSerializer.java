package cc.alcina.framework.common.client.search;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer.DeserializerOptions;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer.SerializerOptions;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;

public class ReflectiveSearchDefinitionSerializer
		implements SearchDefinitionSerializer {
	public static final String CONTEXT_GUARANTEE_LAST_DEF_NOT_CHANGED = ReflectiveSearchDefinitionSerializer.class
			.getName() + ".CONTEXT_GUARANTEE_LAST_DEF_NOT_CHANGED";

	public static final String RS0 = "rs0_";

	public static boolean flatTreeSerializationEnabled = true;

	public static String escapeJsonForUrl(String str) {
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

	public static String unescapeJsonForUrl(String serializedDef) {
		StringBuilder sb = new StringBuilder();
		String unescapeMap = ".{}[]: \",&";
		for (int idx = 0; idx < serializedDef.length(); idx++) {
			char c = serializedDef.charAt(idx);
			if (c == '.') {
				char c2 = serializedDef.charAt(++idx);
				int index = ((int) c2) - 48;
				if (index < unescapeMap.length()) {
					sb.append(unescapeMap.charAt(index));
				} else {
					throw new IllegalArgumentException("Not json serialized");
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	Map<String, Class> abbrevLookup = new LinkedHashMap<>();

	Map<Class, String> reverseAbbrevLookup = new LinkedHashMap<>();

	private SearchDefinition lastDef;

	private String lastStringDef;

	private int lastDefCount;

	public ReflectiveSearchDefinitionSerializer() {
	}

	@Override
	public <SD extends SearchDefinition> SD deserialize(
			Class<? extends SearchDefinition> clazz, String serializedDef) {
		Exception flatTreeDeserializationException = null;
		if (clazz != null
				&& (serializedDef.contains("=") || serializedDef.isEmpty())
				&& canFlatTreeSerialize(clazz)) {
			try {
				return (SD) FlatTreeSerializer.deserialize(clazz, serializedDef,
						new DeserializerOptions().withShortPaths(true));
			} catch (Exception e) {
				Ax.simpleExceptionOut(e);
				flatTreeDeserializationException = e;
			}
		}
		ensureLookups();
		try {
			if (serializedDef.startsWith(RS0)) {
				serializedDef = serializedDef.substring(RS0.length());
			}
			serializedDef = unescapeJsonForUrl(serializedDef);
			SearchDefinition def = Registry.impl(AlcinaBeanSerializer.class)
					.registerLookups(abbrevLookup, reverseAbbrevLookup)
					.deserialize(serializedDef);
			SearchDefinition defaultInstance = Reflections
					.newInstance(def.getClass());
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
			return (SD) def;
		} catch (Exception e) {
			GWT.log("Exception in reflective search", e);
			e.printStackTrace();
			AlcinaTopics.notifyDevWarning(e);
			return null;
		}
	}

	@Override
	public synchronized String serialize(SearchDefinition def) {
		if (lastDef == def) {
			if (LooseContext.is(CONTEXT_GUARANTEE_LAST_DEF_NOT_CHANGED)) {
				return lastStringDef;
			} else {
				lastDefCount++;
				if (lastDefCount == 10) {
					logger.warn(
							"Possibly need to run with context guarantee - {}",
							lastDef);
				}
			}
		} else {
			lastDef = def;
			lastDefCount = 0;
		}
		lastStringDef = serialize0(lastDef);
		return lastStringDef;
	}

	private void ensureLookups() {
		if (abbrevLookup.isEmpty()) {
			Registry.query().withKeys(SearchDefinitionSerializationInfo.class)
					.untypedRegistrations().forEach(clazz -> {
						SearchDefinitionSerializationInfo info = Reflections
								.at(clazz).annotation(
										SearchDefinitionSerializationInfo.class);
						if (info == null) {
							return;
						}
						if (abbrevLookup.containsKey(info.value())) {
							throw new RuntimeException(
									"Searchdef serialization abbreviation collision:"
											+ info.value());
						}
						abbrevLookup.put(info.value(), clazz);
						reverseAbbrevLookup.put(clazz, info.value());
					});
		}
	}

	private String serialize0(SearchDefinition def) {
		Exception flatTreeException = null;
		if (flatTreeSerializationEnabled
				&& canFlatTreeSerialize(def.getClass())) {
			try {
				return FlatTreeSerializer.serialize(def,
						new SerializerOptions().withTopLevelTypeInfo(false)
								.withShortPaths(true).withSingleLine(true)
								.withElideDefaults(true)
								.withTestSerialized(true));
			} catch (Exception e) {
				e.printStackTrace();
				flatTreeException = e;
			}
		}
		ensureLookups();
		try {
			def = def.cloneObject();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		SearchDefinition defaultInstance = Reflections
				.newInstance(def.getClass());
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
		if (flatTreeException != null) {
			// if (GWT.isClient()) {
			// Client.commonRemoteService().logClientError(Ax.format(
			// "ReflectiveSearchDefinitionSerializer.FlatTreeException: %s %s",
			// CommonUtils.toSimpleExceptionMessage(flatTreeException),
			// str), LogMessageType.CLIENT_EXCEPTION.toString(),
			// new VoidCallback());
			// } else {
			// flatTreeException.printStackTrace();
			// }
			flatTreeException.printStackTrace();
		}
		return RS0 + escapeJsonForUrl(str);
	}

	protected boolean
			canFlatTreeSerialize(Class<? extends SearchDefinition> defClass) {
		return Reflections.at(defClass).has(TypeSerialization.class);
	}
}
