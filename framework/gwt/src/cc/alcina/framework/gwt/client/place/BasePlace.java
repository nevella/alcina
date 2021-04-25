package cc.alcina.framework.gwt.client.place;

import java.io.Serializable;

import com.google.gwt.place.shared.Place;
import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;

@Introspectable
@ClientInstantiable
@RegistryLocation(registryPoint = BasePlace.class)
public abstract class BasePlace extends Place implements Serializable {
	public static String tokenFor(BasePlace p) {
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

	public String toAbsoluteHrefString() {
		return Registry.impl(BasePlaceAbsoluteHrefSupplier.class).getHref(this);
	}

	public String toHrefString() {
		return HrefProvider.get().toHrefString(this);
	}
	
	@RegistryLocation(registryPoint=HrefProvider.class,implementationType = ImplementationType.SINGLETON)
	public static class HrefProvider{
		public static BasePlace.HrefProvider get(){
			return Registry.impl(BasePlace.HrefProvider.class);
		}

		public String toHrefString(BasePlace basePlace) {
			return "#" + BasePlace.tokenFor(basePlace);
		}
	}

	@Override
	public String toString() {
		return Ax.format("%s : %s", getClass().getSimpleName(),
				toTokenString());
	}

	public String toTitleString() {
		String category = getClass().getSimpleName().replaceFirst("(.*)Place",
				"$1");
		return category;
	}

	public String toTokenString() {
		return tokenFor(this);
	}

	@RegistryLocation(registryPoint = BasePlaceAbsoluteHrefSupplier.class, implementationType = ImplementationType.SINGLETON)
	public static class BasePlaceAbsoluteHrefSupplier {
		public String getHref(BasePlace basePlace) {
			return null;
		}
	}

	public String toNameString() {
		return toString();
	}
}
