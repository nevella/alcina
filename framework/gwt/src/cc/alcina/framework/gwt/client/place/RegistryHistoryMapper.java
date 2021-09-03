package cc.alcina.framework.gwt.client.place;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.place.EntityPlaceTokenizer;

@ClientInstantiable
@RegistryLocation(registryPoint = RegistryHistoryMapper.class, implementationType = ImplementationType.SINGLETON)
public class RegistryHistoryMapper implements PlaceHistoryMapper {
	public static RegistryHistoryMapper get() {
		return Registry.impl(RegistryHistoryMapper.class);
	}

	Multimap<String, List<BasePlaceTokenizer>> tokenizersByPrefix = new Multimap<>();

	Map<Class, BasePlaceTokenizer> tokenizersByPlace = new LinkedHashMap<>();

	Map<Class<? extends Bindable>, BasePlaceTokenizer> tokenizersByModelClass = new LinkedHashMap<>();

	Map<Enum, BasePlace> placesBySubPlace = new LinkedHashMap<>();

	private Place lastPlace;

	boolean initialised = false;

	public RegistryHistoryMapper() {
		ensurePlaceLookup();
	}

	public <T extends Place> T copyPlace(T place) {
		String token = getToken(place);
		return (T) getPlace(token, true);
	}

	public boolean equalPlaces(Place place1, Place place2) {
		return getToken(place1).equals(getToken(place2));
	}

	public Class<? extends Entity>
			getEntityClass(Class<? extends EntityPlace> placeClass) {
		return ((EntityPlaceTokenizer) tokenizersByPlace.get(placeClass))
				.getModelClass();
	}

	@Override
	public Place getPlace(String token) {
		return getPlace(token, false);
	}

	public synchronized Place getPlaceByModelClass(Class<?> clazz) {
		return tokenizersByModelClass.containsKey(clazz)
				? tokenizersByModelClass.get(clazz).createDefaultPlace()
				: null;
	}

	public synchronized BasePlace getPlaceBySubPlace(Enum value) {
		return placesBySubPlace.get(value).copy();
	}

	@Override
	public synchronized String getToken(Place place) {
		if (place == null || tokenizersByPlace.isEmpty()) {
			return "";
		}
		String token = tokenizersByPlace.get(place.getClass()).getToken(place);
		return getAppPrefix().isEmpty() ? token : getAppPrefix() + "/" + token;
	}

	public synchronized BasePlaceTokenizer getTokenizer(Place place) {
		if (place == null || tokenizersByPlace.isEmpty()) {
			return null;
		}
		return tokenizersByPlace.get(place.getClass());
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

	private synchronized void ensurePlaceLookup() {
		if (initialised) {
			return;
		}
		initialised = true;
		List<BasePlaceTokenizer> impls = Registry
				.impls(BasePlaceTokenizer.class);
		for (BasePlaceTokenizer tokenizer : impls) {
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
		}
		List<BasePlace> places = Registry.impls(BasePlace.class);
		for (BasePlace place : places) {
			if (place instanceof SubPlace) {
				placesBySubPlace.put(((SubPlace) place).getSub(), place);
			}
		}
	}

	protected String getAppPrefix() {
		return "";
	}

	protected synchronized Place getPlace(String i_token, boolean copy) {
		i_token = removeAppPrefixAndLeadingSlashes(i_token);
		String token = i_token;
		if (!copy) {
			// System.out.println("get place:" + token);
		}
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
				? o_tokenizer.get().getPlace(token)
				: null;
		if (place == null) {
			if (GWT.isClient()) {
				// handle doc internal hrefs
				place = lastPlace;
			}
		}
		lastPlace = place;
		return place;
	}
}