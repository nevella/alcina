package cc.alcina.framework.gwt.client.place;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchDefinitionSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;

@Reflected
@Registration(BasePlaceTokenizer.class)
public abstract class BasePlaceTokenizer<P extends BasePlace>
		implements PlaceTokenizer<P>, Registration.Ensure {
	protected StringBuilder tokenBuilder;

	protected String[] parts;

	protected StringMap params = new StringMap();

	boolean mutable;

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
		if (Ax.isBlank(part)) {
			return;
		}
		if (tokenBuilder.length() > 0) {
			tokenBuilder.append("/");
		}
		tokenBuilder.append(part);
	}

	public P copyPlace(P place) {
		String token = getToken(place);
		return getPlace(token);
	}

	public Place createDefaultPlace() {
		return Reflections.newInstance(getTokenizedClass());
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

	// for subclasses, to e.g. add a ? before the params string map
	protected String getParameterPartPrefix() {
		return "";
	}

	@Override
	public P getPlace(String token) {
		return getPlace1(token, false);
	}

	P getPlace1(String token, boolean retry) {
		Preconditions.checkState(mutable);
		String[] fragments = token.split("//");
		token = fragments[0];
		parts = token.split("/");
		try {
			P place0 = getPlace0(token);
			for (int idx = 1; idx < fragments.length; idx++) {
				BasePlace fragmentPlace = (BasePlace) RegistryHistoryMapper
						.get().parseAndReturnPlace(fragments[idx]);
				place0.fragments.add(fragmentPlace);
			}
			return place0;
		} catch (Exception e) {
			if (e.getClass() == RuntimeException.class || !GWT.isScript()
					|| !retry) {
				// key collisions etc
				throw WrappedRuntimeException.wrap(e);
			} else {
				e.printStackTrace();
				return getPlace1(getPrefix(), true);
			}
		}
	}

	protected abstract P getPlace0(String token);

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
		String token = tokenBuilder.toString();
		if (place.fragments.size() > 0) {
			token = Stream
					.concat(Stream.of(token), place.fragments.stream()
							.map(fragment -> RegistryHistoryMapper.get()
									.getTokenizerByClass(fragment)
									.mutableInstance().getToken(fragment)))
					.collect(Collectors.joining("//"));
		}
		return token;
	}

	protected abstract void getToken0(P place);

	public Class<P> getTokenizedClass() {
		return Reflections.at(getClass()).firstGenericBound();
	}

	public boolean handles(String token) {
		return true;
	}

	/**
	 * @return true if the tokenizer instantiates place subclasses based on a
	 *         token parameter
	 */
	protected boolean handlesPlaceSubclasses() {
		return false;
	}

	public boolean hasDefaultPlace() {
		return !Reflections.at(getTokenizedClass()).isAbstract();
	}

	public boolean isCanonicalModelClassTokenizer() {
		return true;
	}

	BasePlaceTokenizer mutableInstance() {
		BasePlaceTokenizer instance = Reflections.newInstance(getClass());
		instance.mutable = true;
		return instance;
	}

	protected void parseMap(String s) {
		params = AlcinaHistory.fromHash(s);
	}

	public void register(
			Map<Class<? extends Entity>, BasePlaceTokenizer> tokenizersByModelClass) {
	}

	protected SearchDefinitionSerializer searchDefinitionSerializer() {
		return Registry.impl(SearchDefinitionSerializer.class);
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
		if (value == null) {
			params.remove(key);
		} else {
			params.put(key, value.toString());
		}
	}

	public void setParameter(String key, Object value, boolean explicitBlanks) {
		params.put(key, value == null ? null : value.toString());
	}
}
