package cc.alcina.framework.common.client.domain.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.search.OrderGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.TruncatedObjectCriterion;
import cc.alcina.framework.common.client.serializer.flat.PropertySerialization;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;

/*
 * I'd call this CrudSearch...but that'd be mean
 */
public abstract class EntitySearchDefinition extends BindableSearchDefinition {
	// return editable dataobjects (either the entity class or a dataobject
	// extending it)
	private boolean returnSingleDataObjectImplementations;

	@Override
	@PropertySerialization(ignore = true)
	public Set<OrderGroup> getOrderGroups() {
		return super.getOrderGroups();
	}

	public boolean isReturnSingleDataObjectImplementations() {
		return this.returnSingleDataObjectImplementations;
	}

	public List<EntityPlace> provideFilterPlaces() {
		List<EntityPlace> places = new ArrayList<>();
		if (allCriteria().size() != 1) {
			return places;
		}
		for (SearchCriterion sc : allCriteria()) {
			if (sc instanceof TruncatedObjectCriterion) {
				TruncatedObjectCriterion criterion = (TruncatedObjectCriterion) sc;
				if (criterion.getId() != 0) {
					places.add(EntityPlace.forClassAndId(
							criterion.getObjectClass(), criterion.getId()));
				}
			}
		}
		return places;
	}

	public boolean provideIsDefaultSortOrder() {
		return getSearchOrders().isEmpty();
	}

	@Override
	public Class<? extends Bindable> queriedBindableClass() {
		return queriedEntityClass();
	}

	public abstract <C extends Entity> Class<C> queriedEntityClass();

	public void setReturnSingleDataObjectImplementations(
			boolean returnSingleDataObjectImplementations) {
		this.returnSingleDataObjectImplementations = returnSingleDataObjectImplementations;
	}
}
