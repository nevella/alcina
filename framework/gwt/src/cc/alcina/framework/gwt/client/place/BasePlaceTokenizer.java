package cc.alcina.framework.gwt.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinitionSerializer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;

@ClientInstantiable
@RegistryLocation(registryPoint = BasePlaceTokenizer.class)
public abstract class BasePlaceTokenizer<P extends Place>
		implements PlaceTokenizer<P> {
	protected StringBuilder tokenBuilder;

	protected String[] parts;

	private StringMap params;

	boolean added = false;

	public P copyPlace(P place) {
		String token = getToken(place);
		return getPlace(token);
	}

	public Place createDefaultPlace() {
		return Reflections.classLookup().newInstance(getTokenizedClass());
	}

	public boolean getBooleanParameter(String key) {
		String value = params.get(key);
		return value == null ? false
				: value.equals("t") || Boolean.parseBoolean(value);
	}

	public int getIntParameter(String key) {
		String value = params.get(key);
		return value == null ? 0 : CommonUtils.friendlyParseInt(value);
	}

	public long getLongParameter(String key) {
		String value = params.get(key);
		return value == null ? 0 : CommonUtils.friendlyParseLong(value);
	}

	public Class<? extends HasIdAndLocalId> getModelClass(){
		return null;
	}

	@Override
	public P getPlace(String token) {
		parts = token.split("/");
		try {
			return getPlace0(token);
		} catch (Exception e) {
			e.printStackTrace();
			return getPlace(getPrefix());
		}
	}

	public String getPrefix() {
		String s = getTokenizedClass().getSimpleName().replaceFirst("(.+)Place",
				"$1");
		return s.toLowerCase();
	}

	public String getStringParameter(String key) {
		return params.get(key);
	}

	@Override
	public String getToken(P place) {
		tokenBuilder = new StringBuilder();
		params = null;
		added = false;
		addTokenPart(getPrefix());
		getToken0(place);
		if (params != null && !params.isEmpty()) {
			addTokenPart(AlcinaHistory.toHash(params));
		}
		return tokenBuilder.toString();
	}

	public abstract Class<P> getTokenizedClass();

	public boolean handles(String token) {
		return true;
	}

	public void setParameter(String key, Object value) {
		if (value instanceof Number) {
			if (((Number) value).longValue() == 0) {
				value = null;
			}
		}
		if (value instanceof Boolean) {
			if (((Boolean) value) == false) {
				value = null;
			}
		}
		params.put(key, value == null ? null : value.toString());
	}

	public void setParameter(String key, Object value, boolean explicitBlanks) {
		params.put(key, value == null ? null : value.toString());
	}

	protected void addTokenPart(long l) {
		addTokenPart(String.valueOf(l));
	}

	protected void addTokenPart(String part) {
		if (part == null) {
			return;
		}
		if (added) {
			tokenBuilder.append("/");
		}
		tokenBuilder.append(part);
		added = true;
	}

	protected <E extends Enum> E enumValue(Class<E> clazz, String value) {
		return enumValue(clazz, value, null);
	}
	protected <E extends Enum> E enumValue(Class<E> clazz, String value,
			E defaultValue) {
		return CommonUtils.getEnumValueOrNull(clazz, value, true, defaultValue);
	}

	protected abstract P getPlace0(String token);

	protected abstract void getToken0(P place);

	protected void initOutParams() {
		params = new StringMap();
	}

	protected void parseMap(String s) {
		params = AlcinaHistory.fromHash(s);
	}

	protected SearchDefinitionSerializer searchDefinitionSerializer() {
		return Registry.impl(SearchDefinitionSerializer.class);
	}
}
