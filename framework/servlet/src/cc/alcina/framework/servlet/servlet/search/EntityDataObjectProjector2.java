package cc.alcina.framework.servlet.servlet.search;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.persistence.ManyToMany;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.EntityMultipleDataObjectDecorator;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.EntitySingleDataObjectDecorator;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.OneToManyMultipleSummary;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
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

	/*
	 * add the immediate entity reachables of either the single-entity or entity
	 * collection
	 */
	public <T> T project(T object) throws Exception {
		Set<Entity> seeds = (Set) CommonUtils.wrapInCollection(object).stream()
				.collect(Collectors.toSet());
		Stack<Entity> traverse = new Stack<>();
		traverse.addAll(seeds);
		while (!traverse.isEmpty()) {
			Entity entity = traverse.pop();
			projectable.add(entity);
			GraphProjection projection = new GraphProjection();
			List<Field> nonPrimitive = projection
					.getNonPrimitiveOrDataFieldsForClass(entity.getClass());
			for (Field field : nonPrimitive) {
				if (Entity.class.isAssignableFrom(field.getType())) {
					Entity toOneRel = (Entity) field.get(entity);
					if (toOneRel != null) {
						if (seeds.add(toOneRel)) {
							projectable.add(toOneRel);
							traverse.push(toOneRel);
						}
					}
				}
				if (Collection.class.isAssignableFrom(field.getType())) {
					Collection collection = (Collection) field.get(entity);
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
				String providerMethodName = value
						.getCollectionAccessorMethodName();
				Method method = context.sourceOwner.getClass()
						.getMethod(providerMethodName, new Class[0]);
				Collection<? extends Entity> collection = (Collection<? extends Entity>) method
						.invoke(context.sourceOwner, new Object[0]);
				OneToManyMultipleSummary summary = new OneToManyMultipleSummary(
						(Entity) context.sourceOwner, collection,
						value.getEntityClass());
				return (T) summary;
			}
			T result = super.filterData(original, projected, context,
					graphProjection);
			return result;
		}
	}
}
