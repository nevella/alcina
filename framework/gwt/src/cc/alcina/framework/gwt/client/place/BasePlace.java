package cc.alcina.framework.gwt.client.place;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

import java.io.Serializable;

import com.google.gwt.place.shared.Place;
import com.totsp.gwittir.client.beans.annotations.Introspectable;

@Introspectable
@RegistryLocation(registryPoint = BasePlace.class)
public abstract class BasePlace extends Place implements Serializable {
	private static String tokenFor(BasePlace p) {
		return Registry.impl(RegistryHistoryMapper.class).getToken(p);
	}

	private boolean refreshed;

	public <T extends BasePlace> T copy() {
		RegistryHistoryMapper mapper = Registry
				.impl(RegistryHistoryMapper.class);
		return (T) mapper.copyPlace(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass() == getClass()) {
			BasePlace other = (BasePlace) obj;
			if (isRefreshed() || other.isRefreshed()) {
				return obj == this;
			} else {
				return tokenFor(other).equals(tokenFor(this));
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return tokenFor(this).hashCode();
	}

	public boolean isRefreshed() {
		return this.refreshed;
	}

	public void setRefreshed(boolean refreshed) {
		this.refreshed = refreshed;
	}

	public String toTokenString() {
		return tokenFor(this);
	}
}
