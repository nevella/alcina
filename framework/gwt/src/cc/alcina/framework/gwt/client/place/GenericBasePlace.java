package cc.alcina.framework.gwt.client.place;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;

@RegistryLocation(registryPoint = GenericBasePlace.class)
public abstract class GenericBasePlace<SD extends SearchDefinition>
		extends BasePlace implements PlaceWithSearchDefinition<SD> {
	public long fromId;

	public long id;

	public SD def;

	public GenericBasePlace() {
		def = createSearchDefinition();
	}

	@Override
	public SD getSearchDefinition() {
		return def;
	}

	public boolean provideIsDefaultDefs() {
		GenericBasePlace o = Reflections.classLookup().newInstance(getClass());
		return HasEquivalenceHelper.argwiseEquivalent(def, o.def);
	}

	public <T extends GenericBasePlace> T withId(long id) {
		this.id = id;
		return (T) this;
	}

	public <T extends GenericBasePlace> T withId(String stringId) {
		return withId(Long.parseLong(stringId));
	}

	public <T extends GenericBasePlace> T withHasId(HasId hasId) {
		return withId(hasId == null ? 0 : hasId.getId());
	}

	public String stringId() {
		return String.valueOf(id);
	}

	protected abstract SD createSearchDefinition();

	@Override
	public String toString() {
		return Ax.format("%s:%s",
				getClass().getSimpleName().replaceFirst("(.+)Place", "$1"), id);
	}
}
