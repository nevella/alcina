package cc.alcina.framework.gwt.client.place;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentSingleton;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.ClientTopics;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.place.EntityPlaceTokenizer;

@Reflected
@EnvironmentSingleton
@Registration.Singleton
public class RegistryHistoryMapper implements PlaceHistoryMapper {
	public static RegistryHistoryMapper get() {
		return Registry.impl(RegistryHistoryMapper.class);
	}

	/*
	 * All tokenizers are really templates, the instances in the maps should not
	 * be used to serialize/deserialize
	 */
	Multimap<String, List<BasePlaceTokenizer>> tokenizersByPrefix = new Multimap<>();

	Map<Class, BasePlaceTokenizer> tokenizersByPlace = new LinkedHashMap<>();

	Map<Class<? extends Bindable>, BasePlaceTokenizer> tokenizersByModelClass = new LinkedHashMap<>();

	Map<Enum, BasePlace> placesBySubPlace = new LinkedHashMap<>();

	public RegistryHistoryMapper() {
		this(null);
	}

	Class<? extends Place> permittedPlaceSupertype;

	public RegistryHistoryMapper(
			Class<? extends Place> permittedPlaceSupertype) {
		this.permittedPlaceSupertype = permittedPlaceSupertype;
		ensurePlaceLookup();
	}

	private String cleanGwtCodesvr(String token) {
		return token.replaceFirst("[?&](gwt.codesvr=127.0.0.1:\\d+|gwt.l)$",
				"");
	}

	public <T extends Place> T copyPlace(T place) {
		String token = getToken(place);
		try {
			// should never throw
			return (T) getPlace(token);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	// only during constructor
	void ensurePlaceLookup() {
		listTokenizers().forEach(tokenizer -> {
			tokenizersByPrefix.add(tokenizer.getPrefix(), tokenizer);
			tokenizersByPlace.put(tokenizer.getTokenizedClass(), tokenizer);
			if (tokenizer.isCanonicalModelClassTokenizer()) {
				if (tokenizer instanceof EntityPlaceTokenizer) {
					tokenizersByModelClass.put(
							((EntityPlaceTokenizer) tokenizer).getModelClass(),
							tokenizer);
				}
				if (tokenizer instanceof BindablePlaceTokenizer) {
					tokenizersByModelClass
							.put(((BindablePlaceTokenizer) tokenizer)
									.getModelClass(), tokenizer);
				}
			}
			tokenizer.register(tokenizersByModelClass);
		});
		listPlaces().filter(p -> p instanceof SubPlace)
				.forEach(place -> placesBySubPlace
						.put(((SubPlace) place).getSub(), place));
	}

	public boolean equalPlaces(Place place1, Place place2) {
		return getToken(place1).equals(getToken(place2));
	}

	protected String getAppPrefix() {
		return "";
	}

	public Class<? extends Entity>
			getEntityClass(Class<? extends EntityPlace> placeClass) {
		return ((EntityPlaceTokenizer) tokenizersByPlace.get(placeClass))
				.getModelClass();
	}

	/**
	 * Call this if the token (serialized form) is not trusted (e.g. a browser
	 * url, say) and thus quite possibly will throw
	 */
	@Override
	public Place getPlace(String token) throws UnparseablePlaceException {
		return parseAndReturnPlace(token);
	}

	public synchronized Place getPlaceByModelClass(Class<?> clazz) {
		return tokenizersByModelClass.containsKey(clazz)
				? tokenizersByModelClass.get(clazz).createDefaultPlace()
				: null;
	}

	public synchronized BasePlace getPlaceBySubPlace(Enum value) {
		return placesBySubPlace.get(value).copy();
	}

	public Optional<Place> getPlaceIfParseable(String token) {
		try {
			return Optional.ofNullable(parseAndReturnPlace(token));
		} catch (UnparseablePlaceException e) {
			new ClientTopics.DevMessage("unparesable place", token).publish();
			return Optional.empty();
		} catch (Exception e) {
			new ClientTopics.DevMessage("unparesable place", token).publish();
			return Optional.empty();
		}
	}

	/**
	 * Call this if the token (serialized form) is trusted (not a browser url,
	 * say) and thus not expected to throw - *or* if an unparseable place is
	 * broken (e.g. during deserialization)
	 */
	public Place getPlaceOrThrow(String token) {
		try {
			return getPlace(token);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	@Override
	public synchronized String getToken(Place place) {
		if (place == null || tokenizersByPlace.isEmpty()
				|| place == Place.NOWHERE) {
			return "";
		}
		String token = getTokenizerByClass(place).mutableInstance()
				.getToken(place);
		return getAppPrefix().isEmpty() ? token : getAppPrefix() + "/" + token;
	}

	public synchronized BasePlaceTokenizer getTokenizer(Place place) {
		if (place == null || tokenizersByPlace.isEmpty()) {
			return null;
		}
		return getTokenizerByClass(place).mutableInstance();
	}

	private BasePlaceTokenizer getTokenizerByClass(Place place) {
		Class<? extends Place> clazz = place.getClass();
		{
			BasePlaceTokenizer tokenizer = tokenizersByPlace.get(clazz);
			if (tokenizer != null) {
				return tokenizer;
			}
		}
		{
			BasePlaceTokenizer tokenizer = tokenizersByPlace
					.get(clazz.getSuperclass());
			if (tokenizer != null && tokenizer.handlesPlaceSubclasses()) {
				return tokenizer;
			}
		}
		return null;
	}

	protected Stream<BasePlace> listPlaces() {
		return Registry.query(BasePlace.class).implementations();
	}

	protected Stream<BasePlaceTokenizer> listTokenizers() {
		return Registry.query(BasePlaceTokenizer.class).implementations()
				.filter(t -> permittedPlaceSupertype == null
						|| Reflections.isAssignableFrom(permittedPlaceSupertype,
								t.getTokenizedClass()));
	}

	/**
	 * On startup, apps should catch the UnparseablePlaceException and sub null.
	 * But generally it's better to force the app to explicitly handle
	 * unparseable places than just 'null'
	 *
	 * @throws UnparseablePlaceException
	 */
	protected synchronized Place parseAndReturnPlace(String o_token)
			throws UnparseablePlaceException {
		String token = cleanGwtCodesvr(
				removeAppPrefixAndLeadingSlashes(o_token));
		String[] split = token.split("/");
		String top = split[0];
		Optional<BasePlaceTokenizer> o_tokenizer = tokenizersByPrefix
				.getAndEnsure(top).stream()
				.filter(tokenizer -> tokenizer.handles(token)).findFirst();
		if (!o_tokenizer.isPresent() && tokenizersByPrefix.containsKey("")) {
			o_tokenizer = tokenizersByPrefix.getAndEnsure("").stream()
					.filter(tokenizer -> tokenizer.handles(token)).findFirst();
		}
		if (!o_tokenizer.isPresent() && top.length() > 1 && split.length > 1) {
			top = split[0] + "/" + split[1];
			o_tokenizer = tokenizersByPrefix.getAndEnsure(top).stream()
					.filter(tokenizer -> tokenizer.handles(token)).findFirst();
		}
		Place place = o_tokenizer.isPresent()
				? o_tokenizer.get().mutableInstance().getPlace(token)
				: null;
		if (place == null) {
			throw new UnparseablePlaceException(o_token);
			// nope - client must handle null
			// if (GWT.isClient()) {
			// // handle doc internal hrefs
			// place = lastPlace;
			// }
			// }
			// lastPlace = place;
		}
		return place;
	}

	public String removeAppPrefixAndLeadingSlashes(String tokenString) {
		String appPrefix = getAppPrefix();
		if (tokenString.startsWith("/")) {
			tokenString = tokenString.substring(1);
		}
		if (appPrefix.length() > 0) {
			String matchesPattern = Ax.format("/?%s(/.*|$)", appPrefix);
			if (tokenString.matches(matchesPattern)) {
				tokenString = tokenString.substring(appPrefix.length());
			}
		}
		if (tokenString.startsWith("/")) {
			tokenString = tokenString.substring(1);
		}
		return tokenString;
	}

	public void removeTokenizer(Predicate<BasePlaceTokenizer> matcher) {
		tokenizersByModelClass.entrySet()
				.removeIf(e -> matcher.test(e.getValue()));
		tokenizersByPlace.entrySet().removeIf(e -> matcher.test(e.getValue()));
		tokenizersByPrefix.values()
				.forEach(l -> l.removeIf(tokenizer -> matcher.test(tokenizer)));
	}
}
