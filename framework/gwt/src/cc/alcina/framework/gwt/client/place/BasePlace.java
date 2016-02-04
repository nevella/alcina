package cc.alcina.framework.gwt.client.place;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

import com.google.gwt.place.shared.Place;
import com.totsp.gwittir.client.beans.annotations.Introspectable;

@Introspectable
@RegistryLocation(registryPoint = BasePlace.class)
public abstract class BasePlace extends Place {
	private static String tokenFor(BasePlace p) {
		return Registry.impl(RegistryHistoryMapper.class).getToken(p);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass() == getClass()) {
			return tokenFor((BasePlace) obj).equals(tokenFor(this));
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return tokenFor(this).hashCode();
	}
}
