package cc.alcina.framework.gwt.client.place;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

import com.google.gwt.place.shared.Place;
import com.totsp.gwittir.client.beans.annotations.Introspectable;

@Introspectable
@RegistryLocation(registryPoint = BasePlace.class)
public abstract class BasePlace extends Place {
	public static <P extends BasePlace> boolean areEquivalent(P place,
			P lastPlace, BasePlaceTokenizer<P> placeTokenizer) {
		if (place == null || lastPlace == null) {
			return false;
		}
		return placeTokenizer.getToken(place).equals(
				placeTokenizer.getToken(lastPlace));
	}
}
