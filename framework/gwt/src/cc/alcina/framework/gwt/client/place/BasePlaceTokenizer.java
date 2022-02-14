package cc.alcina.framework.gwt.client.place;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchDefinitionSerializer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;

@ClientInstantiable
@Registration(BasePlaceTokenizer.class)
public abstract class BasePlaceTokenizer<P extends Place>
		implements PlaceTokenizer<P> {
	protected StringBuilder tokenBuilder;

	protected String[] parts;

	protected StringMap params;

	public P copyPlace(P place) {
		String token = getToken(place);
		return getPlace(token);
	}

	public Place createDefaultPlace() {
		return Reflections.newInstance(getTokenizedClass());
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

	@Override
	public P getPlace(String token) {
		parts = token.split("/");
		try {
			return getPlace0(token);
		} catch (Exception e) {
			if (e.getClass() == RuntimeException.class || !GWT.isScript()) {
				// key collisions etc
				throw e;
			} else {
				e.printStackTrace();
				return getPlace(getPrefix());
			}
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
		addTokenPart(getPrefix());
		getToken0(place);
		if (params != null && !params.isEmpty()) {
			addTokenPart(AlcinaHistory.toHash(params, encodedValues()));
		}
		return tokenBuilder.toString();
	}

	public abstract Class<P> getTokenizedClass();

	public boolean handles(String token) {
		return true;
	}

	public boolean isCanonicalModelClassTokenizer() {
		return true;
	}

	public void register(
			Map<Class<? extends Entity>, BasePlaceTokenizer> tokenizersByModelClass) {
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

	protected void addTokenPart(Enum part) {
		if (part == null) {
			return;
		}
		addTokenPart(part.toString().toLowerCase());
	}

	protected void addTokenPart(long l) {
		addTokenPart(String.valueOf(l));
	}

	protected void addTokenPart(String part) {
		if (part == null) {
			return;
		}
		if (tokenBuilder.length() > 0) {
			tokenBuilder.append("/");
		}
		tokenBuilder.append(part);
	}

	protected List<String> encodedValues() {
		return Collections.emptyList();
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
