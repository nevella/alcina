package cc.alcina.framework.common.client.domain.search;

import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.OrderGroup;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

/*
 * I'd call this CrudSearch...but that'd be mean
 */
public abstract class EntitySearchDefinition extends BindableSearchDefinition {
	@Override
	@PropertySerialization(ignore = true)
	public Set<OrderGroup> getOrderGroups() {
		return super.getOrderGroups();
	}

	@Override
	public Class<? extends Bindable> queriedBindableClass() {
		return queriedEntityClass();
	}

	public abstract <C extends Entity> Class<C> queriedEntityClass();

	public static abstract class DefaultIdOrder extends EntitySearchDefinition {
		public DefaultIdOrder() {
			getSearchOrders().addOrder(new DisplaySearchOrder("id"), true);
		}

		@Override
		public boolean provideIsDefaultSortOrder() {
			return getSearchOrders().isEmpty()
					|| getSearchOrders().provideIsIdAscDisplayOrder();
		}

		@Override
		public TreeSerializable.Customiser treeSerializationCustomiser() {
			return new Customiser(this);
		}

		protected static class Customiser<D extends EntitySearchDefinition.DefaultIdOrder>
				extends SearchDefinition.Customiser<D> {
			public Customiser(D serializable) {
				super(serializable);
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
			public void onAfterTreeSerialize() {
				super.onAfterTreeSerialize();
				if (serializable.getSearchOrders().isEmpty()) {
					serializable.getSearchOrders()
							.addOrder(new DisplaySearchOrder("id"), true);
				}
			}

			@Override
			public void onBeforeTreeDeserialize() {
				super.onBeforeTreeDeserialize();
				serializable.getSearchOrders().clear();
			}

			@Override
			public void onBeforeTreeSerialize() {
				super.onBeforeTreeSerialize();
				if (serializable.getSearchOrders()
						.provideIsIdAscDisplayOrder()) {
					serializable.getSearchOrders().clear();
				}
			}
		}
	}

	public List<Entity> provideFilteringEntities() {
		return Registry.impl(FilteringEntitiesProvider.class)
				.provideFilteringEntities(this);
	}

	@Registration.Self
	public interface FilteringEntitiesProvider {
		List<Entity> provideFilteringEntities(
				EntitySearchDefinition entitySearchDefinition);
	}
}
