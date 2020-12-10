package cc.alcina.framework.servlet.servlet.search;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.ManyToMany;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.EntityMultipleDataObjectDecorator;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.EntitySingleDataObjectDecorator;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.OneToManyMultipleSummary;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.projection.CollectionProjectionFilter;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.ModelSearchResults;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/*
 * Deliberately does not extend SearchResultProjector. To use this, create a subclass of SearchResultProjector (with a higher registry priority) and delegate to this
 */
public class EntityDataObjectProjector2 {
	ReferenceOpenHashSet<Entity> projectable = new ReferenceOpenHashSet<>();

	public <T> T project(T object) throws Exception {
		if (object instanceof Collection) {
			((Collection) object).forEach(o -> projectable.add((Entity) o));
		} else {
			Entity e = (Entity) object;
			projectable.add(e);
			GraphProjection projection = new GraphProjection();
			List<Field> nonPrimitive = projection
					.getNonPrimitiveOrDataFieldsForClass(e.getClass());
			for (Field field : nonPrimitive) {
				if (Entity.class.isAssignableFrom(field.getType())) {
					projectable.add((Entity) field.get(e));
				}
				if (Collection.class.isAssignableFrom(field.getType())) {
					Collection collection = (Collection) field.get(e);
					if (collection != null) {
						collection.stream().filter(o -> o instanceof Entity)
								.forEach(o -> projectable.add((Entity) o));
					}
				}
			}
		}
		return GraphProjections.defaultProjections()
				.dataFilter(new DataFilter()).project(object);
	}

	public class DataFilter extends CollectionProjectionFilter {
		@Override
		public <T> T filterData(T original, T projected,
				GraphProjectionContext context, GraphProjection graphProjection)
				throws Exception {
			Field field = context.field;
			if (original instanceof Entity) {
				if (!projectable.contains(original)) {
					return null;
				}
			} else if (field != null
					&& OneToManyMultipleSummary.class == field.getType()) {
				OneToManyMultipleSummary value = (OneToManyMultipleSummary) original;
				String providerMethodName = value.getCollectionAccessorMethodName();
				Method method = context.sourceOwner.getClass()
						.getMethod(providerMethodName, new Class[0]);
				Collection<? extends Entity> collection = (Collection<? extends Entity>) method
						.invoke(context.sourceOwner, new Object[0]);
				OneToManyMultipleSummary summary = new OneToManyMultipleSummary(
						(Entity) context.sourceOwner, collection,value.getEntityClass());
				return (T) summary;
			}
			 T result = super.filterData(original, projected, context,
					graphProjection);
			 return result;
		}
	}
}
