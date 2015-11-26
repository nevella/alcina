package cc.alcina.framework.gwt.client.place;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;

@RegistryLocation(registryPoint=RegistryHistoryMapper.class,implementationType=ImplementationType.SINGLETON)
public class RegistryHistoryMapper implements PlaceHistoryMapper {
	Map<String, BasePlaceTokenizer> tokenizersByPrefix = new LinkedHashMap<>();

	Map<Class, BasePlaceTokenizer> tokenizersByPlace = new LinkedHashMap<>();

	public RegistryHistoryMapper() {
		ensurePlaceLookup();
	}

	private void ensurePlaceLookup() {
		List<BasePlaceTokenizer> impls = Registry
				.impls(BasePlaceTokenizer.class);
		for (BasePlaceTokenizer tokenizer : impls) {
			tokenizersByPrefix.put(tokenizer.getPrefix(), tokenizer);
			tokenizersByPlace.put(tokenizer.getTokenizedClass(), tokenizer);
		}
	}

	@Override
	public Place getPlace(String token) {
		String top = token.split("/")[0];
		BasePlaceTokenizer tokenizer = tokenizersByPrefix.get(top);
		return tokenizer == null ? null : tokenizer.getPlace(token);
	}

	@Override
	public String getToken(Place place) {
		return tokenizersByPlace.get(place.getClass()).getToken(place);
	}
}