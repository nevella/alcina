package cc.alcina.framework.gwt.client.place;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinitionSerializer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

@ClientInstantiable
@RegistryLocation(registryPoint = BasePlaceTokenizer.class)
public abstract class BasePlaceTokenizer<P extends Place> implements
		PlaceTokenizer<P> {
	public String getPrefix() {
		String s = getTokenizedClass().getSimpleName().replaceFirst(
				"(.+)Place", "$1");
		return s.toLowerCase();
	}

	public abstract Class<P> getTokenizedClass();

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

	protected abstract P getPlace0(String token);

	protected StringBuilder tokenBuilder;

	protected String[] parts;

	@Override
	public String getToken(P place) {
		tokenBuilder = new StringBuilder();
		addTokenPart(getPrefix());
		getToken0(place);
		if (params != null && !params.isEmpty()) {
			addTokenPart(AlcinaHistory.toHash(params));
		}
		return tokenBuilder.toString();
	}

	protected void addTokenPart(String part) {
		if (tokenBuilder.length() > 0) {
			tokenBuilder.append("/");
		}
		tokenBuilder.append(part);
	}

	private StringMap params;

	protected void parseMap(String s) {
		params = AlcinaHistory.fromHash(s);
	}

	public int getIntParameter(String key) {
		String value = params.get(key);
		return value == null ? 0 : CommonUtils.friendlyParseInt(value);
	}

	public long getLongParameter(String key) {
		String value = params.get(key);
		return value == null ? 0 : CommonUtils.friendlyParseLong(value);
	}

	protected void initOutParams() {
		params = new StringMap();
	}

	public boolean getBooleanParameter(String key) {
		String value = params.get(key);
		return value == null ? false : value.equals("t")
				|| Boolean.parseBoolean(value);
	}

	public String getStringParameter(String key) {
		return params.get(key);
	}

	protected abstract void getToken0(P place);

	protected SearchDefinitionSerializer searchDefinitionSerializer() {
		return Registry.impl(SearchDefinitionSerializer.class);
	}

	protected <E extends Enum> E enumValue(Class<E> clazz, String value) {
		return CommonUtils.getEnumValueOrNull(clazz, value, true, null);
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
}
