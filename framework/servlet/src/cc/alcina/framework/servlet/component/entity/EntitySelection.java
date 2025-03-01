package cc.alcina.framework.servlet.component.entity;

import java.util.Comparator;
import java.util.Objects;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables.Single.PropertyValues;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class EntitySelection extends AbstractSelection<Entity> {
	public EntitySelection(Selection parent, Entity entity) {
		super(parent, entity, String.valueOf(entity.getId()));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EntitySelection) {
			EntitySelection o = (EntitySelection) obj;
			return Objects.equals(get(), o.get());
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(get());
	}

	static class View extends AbstractSelection.View<EntitySelection> {
		@Override
		public Model getExtended(EntitySelection selection) {
			return new Container(new EntityExtended(selection));
		}

		static class Container extends Model.All {
			Model model;

			Container(Model model) {
				this.model = model;
			}
		}

		static class EntityExtended extends Model.All {
			String id;

			@Directed.Transform(Tables.Single.class)
			Tables.Single.PropertyValues propertyValues = new Tables.Single.PropertyValues();

			EntityExtended(EntitySelection selection) {
				Entity entity = selection.get();
				if (entity == null) {
					return;
				}
				entity.domain().ensurePopulated();
				id = entity.toStringId();
				Reflections.at(selection.entityType()).properties().stream()
						.sorted(Comparator.comparing(Property::getName))
						.map(p -> new PropertyValue(selection, p))
						.forEach(pv -> pv.addTo(propertyValues));
			}

			static class PropertyValue extends Model.Fields
					implements DomEvents.Click.Handler {
				@Directed
				String value;

				private EntitySelection selection;

				private Property property;

				PropertyValue(EntitySelection selection, Property property) {
					this.selection = selection;
					this.property = property;
					Entity entity = selection.get();
					Object v = property.get(entity);
					value = String.valueOf(v);
					// trim done by the ui
					// Ax.trim(String.valueOf(v), 300);
				}

				void addTo(PropertyValues propertyValues) {
					propertyValues.add(property, this);
				}

				@Override
				public void onClick(Click event) {
					Ax.out("click :: %s", selection.get());
				}
			}
		}
	}

	@Registration(value = { Selection.RowView.class, EntitySelection.class })
	public static class RowViewImpl<S extends EntitySelection>
			extends AbstractSelection.RowView.Default<S> {
		@Override
		public Bindable provideBindable() {
			return selection.get();
		}
	}

	public Class<? extends Entity> entityType() {
		return (Class<? extends Entity>) Domain
				.resolveEntityClass(get().getClass());
	}
}