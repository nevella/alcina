package cc.alcina.framework.gwt.client.place;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;
import cc.alcina.framework.gwt.client.entity.search.BindableSearchDefinition;

@RegistryLocation(registryPoint = BindablePlace.class)
public abstract class BindablePlace<SD extends BindableSearchDefinition>
		extends BasePlace implements PlaceWithSearchDefinition<SD> {
	public long fromId;

	public long id;

	public SD def;

	public BindablePlace() {
		def = createSearchDefinition();
	}

	@Override
	public SD getSearchDefinition() {
		return def;
	}

	@Override
	public boolean provideIsDefaultDefs() {
		BindablePlace o = Reflections.classLookup().newInstance(getClass());
		return HasEquivalenceHelper.argwiseEquivalent(def, o.def);
	}

	public <T extends BindablePlace> T withId(long id) {
		this.id = id;
		return (T) this;
	}

	public <T extends BindablePlace> T withId(String stringId) {
		return withId(Long.parseLong(stringId));
	}

	public <T extends BindablePlace> T withHasId(HasId hasId) {
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

	public static BindablePlace forClass(Class clazz) {
		return (BindablePlace) RegistryHistoryMapper.get()
				.getPlaceByModelClass(clazz);
	}
}
