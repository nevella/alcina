package cc.alcina.framework.gwt.client.place;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.gwt.place.shared.Place;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.cmp.help.HelpPlace;

/**
 * <p>
 * An abstract class with most of the funtionality needed to construct simple
 * places - locations in the application
 * <p>
 * For more complex places, look at {@link BindablePlace} and the serialization
 * of the {@link SearchDefinition} there - using tree (or reflective)
 * serialization to serialize place parameters gives you the capacity to express
 * any route in a parseable, url-safe form
 * <p>
 * An interesting wrinkle is the {@link #fragments} idea. To model a location in
 * a UI app with a help system, you really need two places - main app location,
 * help system location. The fragments provide the location modelling and
 * marshalling for that, the UI synchronization is handled by the [TODO]
 * xxxActivityMapper. Example urls would be (picked here from the Alcina
 * sequence app):
 * <ul>
 * <li><code>http://dalc:31009/seq#sequence/selectedElementIdx=1//help/topic.blah</code>
 * (in-app help, topic blah)
 * <li><code>http://dalc:31009/seq#help/topic.blah</code> (standalone help,
 * topic blah)
 * </ul>
 * <p>
 * Note that fragments are separated by <code>//</code>
 */
@Reflected
@Registration(BasePlace.class)
public abstract class BasePlace extends Place
		implements Serializable, Registration.Ensure {
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

	public static String tokenFor(BasePlace p) {
		return RegistryHistoryMapper.get().getToken(p);
	}

	@Property.Not
	public List<BasePlace> fragments = new ArrayList<>();

	private boolean refreshed;

	public class Fragments {
		public void remove(Class<? extends BasePlace> fragmentType) {
			fragments.removeIf(p -> p.getClass() == fragmentType);
		}

		public <BP extends BasePlace> BP ensure(Class<BP> fragmentType) {
			return get(fragmentType).orElseGet(() -> {
				BP newPlace = Reflections.newInstance(fragmentType);
				fragments.add(newPlace);
				return newPlace;
			});
		}

		public <BP extends BasePlace> Optional<BP> get(Class<BP> fragmentType) {
			return fragments.stream().filter(p -> p.getClass() == fragmentType)
					.map(p -> (BP) p).findFirst();
		}

		public boolean has(Class<HelpPlace> fragmentType) {
			return get(fragmentType).isPresent();
		}
	}

	public Fragments fragments() {
		return new Fragments();
	}

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

	/**
	 * For copying resources which are constant when updating only parts of a
	 * place (see EntityPlace)
	 */
	public void updateFrom(BasePlace outgoingPlace) {
	}
}
