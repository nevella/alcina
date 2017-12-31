package cc.alcina.framework.gwt.client.place;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.SearchDefinition;
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

	public <T extends GenericBasePlace> T putId(long id) {
		this.id = id;
		return (T) this;
	}

	public <T extends GenericBasePlace> T putId(String stringId) {
		return putId(Long.parseLong(stringId));
	}

	public String stringId() {
		return String.valueOf(id);
	}

	protected abstract SD createSearchDefinition();
}
