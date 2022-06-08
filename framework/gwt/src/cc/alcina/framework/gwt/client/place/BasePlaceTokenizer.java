package cc.alcina.framework.gwt.client.place;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchDefinitionSerializer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;

@Reflected
@Registration(BasePlaceTokenizer.class)
public abstract class BasePlaceTokenizer<P extends Place>
		implements PlaceTokenizer<P>, Registration.Ensure {
	/**
	 * If true, the token will be a combined path (slash-separated) and
	 * querystring (?) string, with path mapping to parts and querystring to
	 * params
	 */
	public static boolean pathQuerystring;

	protected StringBuilder tokenBuilder;

	protected String[] parts;

	protected StringMap params = new StringMap();

	 boolean mutable;

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
	public long getLongWrapperParameter(String key) {
		String value = params.get(key);
		return value == null ? null : CommonUtils.friendlyParseLong(value);
	}

	@Override
	public P getPlace(String token) {
		Preconditions.checkState(mutable);
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
		Preconditions.checkState(mutable);
		tokenBuilder = new StringBuilder();
		addTokenPart(getPrefix());
		getToken0(place);
		if (!params.isEmpty()) {
			String hash = AlcinaHistory.toHash(params, encodedValues());
			hash = getParameterPartPrefix() + hash;
			addTokenPart(hash);
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

	protected void addTokenPart(Long l) {
		if (l == null) {
			return;
		}
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

	// for subclasses, to e.g. add a ? before the params string map
	protected String getParameterPartPrefix() {
		return "";
	}

	protected abstract P getPlace0(String token);

	protected abstract void getToken0(P place);

	protected void parseMap(String s) {
		params = AlcinaHistory.fromHash(s);
	}

	protected SearchDefinitionSerializer searchDefinitionSerializer() {
		return Registry.impl(SearchDefinitionSerializer.class);
	}
	 BasePlaceTokenizer mutableInstance() {
		 BasePlaceTokenizer instance = Reflections.newInstance(getClass());
		 instance.mutable=true;
		 return instance;
	}
}
