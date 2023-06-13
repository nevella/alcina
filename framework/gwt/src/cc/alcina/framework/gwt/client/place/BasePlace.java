package cc.alcina.framework.gwt.client.place;

import java.io.Serializable;

import com.google.common.base.Preconditions;
import com.google.gwt.place.shared.Place;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;

@Reflected
@Registration(BasePlace.class)
public abstract class BasePlace extends Place
		implements Serializable, Registration.Ensure {
	public static String tokenFor(BasePlace p) {
		return RegistryHistoryMapper.get().getToken(p);
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

	public void go() {
		Preconditions.checkState(Client.get() != null);
		Registry.impl(PlaceNavigator.class).go(this);
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

	public String toNameString() {
		return toString();
	}

	@Override
	public String toString() {
		return Ax.format("%s : %s", getClass().getSimpleName(),
				toTokenString());
	}

	public String toTitleString() {
		String category = getClass().getSimpleName().replaceFirst("Place$", "");
		return category;
	}

	public String toTokenString() {
		return tokenFor(this);
	}

	public String toTokenStringWithoutAppPrefix() {
		return RegistryHistoryMapper.get()
				.removeAppPrefixAndLeadingSlashes(toTokenString());
	}

	@Registration.Singleton
	public static class BasePlaceAbsoluteHrefSupplier {
		public String getHref(BasePlace basePlace) {
			return null;
		}
	}

	@Reflected
	@Registration.Singleton
	public static class HrefProvider {
		public static BasePlace.HrefProvider get() {
			return Registry.impl(BasePlace.HrefProvider.class);
		}

		public String toHrefString(BasePlace basePlace) {
			return "#" + BasePlace.tokenFor(basePlace);
		}
	}

	public interface PlaceNavigator {
		void go(Place place);
	}
}
