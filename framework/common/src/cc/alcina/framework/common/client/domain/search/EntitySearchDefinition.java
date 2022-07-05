package cc.alcina.framework.common.client.domain.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.search.OrderGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TruncatedObjectCriterion;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
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

	public static abstract class DefaultIdOrder extends EntitySearchDefinition {
		public DefaultIdOrder() {
			getSearchOrders().addOrder(new DisplaySearchOrder("id"), true);
		}

		@Override
		public TreeSerializable.Customiser treeSerializationCustomiser() {
			return new Customiser(this);
		}

		@Override
		public boolean provideIsDefaultSortOrder() {
			return getSearchOrders().isEmpty()
					|| getSearchOrders().provideIsIdAscDisplayOrder();
		}

		protected static class Customiser<D extends EntitySearchDefinition.DefaultIdOrder>
				extends SearchDefinition.Customiser<D> {
			public Customiser(D serializable) {
				super(serializable);
			}

			@Override
			public void onBeforeTreeDeserialize() {
				super.onBeforeTreeDeserialize();
				serializable.getSearchOrders().clear();
			}

			@Override
			public void onAfterTreeDeserialize() {
				super.onAfterTreeDeserialize();
				if (serializable.getSearchOrders().isEmpty()) {
					serializable.getSearchOrders()
							.addOrder(new DisplaySearchOrder("id"), true);
				}
			}

			@Override
			public void onBeforeTreeSerialize() {
				super.onBeforeTreeSerialize();
				if (serializable.getSearchOrders()
						.provideIsIdAscDisplayOrder()) {
					serializable.getSearchOrders().clear();
				}
			}

			@Override
			public void onAfterTreeSerialize() {
				super.onAfterTreeSerialize();
				if (serializable.getSearchOrders().isEmpty()) {
					serializable.getSearchOrders()
							.addOrder(new DisplaySearchOrder("id"), true);
				}
			}
		}
	}
}
