package cc.alcina.framework.gwt.client.place;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Multimap;

@ClientInstantiable
@RegistryLocation(registryPoint = RegistryHistoryMapper.class, implementationType = ImplementationType.SINGLETON)
public class RegistryHistoryMapper implements PlaceHistoryMapper {
	public static RegistryHistoryMapper get() {
		return Registry.impl(RegistryHistoryMapper.class);
	}

	Multimap<String, List<BasePlaceTokenizer>> tokenizersByPrefix = new Multimap<>();

	Map<Class, BasePlaceTokenizer> tokenizersByPlace = new LinkedHashMap<>();

	Map<Class<? extends Entity>, BasePlaceTokenizer> tokenizersByModelClass = new LinkedHashMap<>();

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
		return getAppPrefix() + token;
	}

	public synchronized BasePlaceTokenizer getTokenizer(Place place) {
		if (place == null || tokenizersByPlace.isEmpty()) {
			return null;
		}
		return tokenizersByPlace.get(place.getClass());
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
				tokenizersByModelClass.put(tokenizer.getModelClass(),
						tokenizer);
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
		if (i_token.startsWith(getAppPrefix())) {
			i_token = i_token.substring(getAppPrefix().length());
		}
		if (i_token.startsWith("/")) {
			i_token = i_token.substring(1);
		}
		String token = i_token;
		if (!copy) {
			System.out.println("get place:" + token);
		}
		String[] split = token.split("/");
		String top = split[0];
		Optional<BasePlaceTokenizer> o_tokenizer = tokenizersByPrefix
				.getAndEnsure(top).stream()
				.filter(tokenizer -> tokenizer.handles(token)).findFirst();
		if (!o_tokenizer.isPresent() && top.length() > 1 && split.length > 1) {
			top = split[0] + "/" + split[1];
			o_tokenizer = tokenizersByPrefix.getAndEnsure(top).stream()
					.filter(tokenizer -> tokenizer.handles(token)).findFirst();
		}
		Place place = o_tokenizer.isPresent()
				? o_tokenizer.get().getPlace(token)
				: null;
		if (place == null) {
			// handle doc internal hrefs
			place = lastPlace;
		}
		lastPlace = place;
		return place;
	}
}