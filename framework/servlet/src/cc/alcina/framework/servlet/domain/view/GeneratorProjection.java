package cc.alcina.framework.servlet.domain.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.BaseProjection;
import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder;
import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder.BplDelegateMapCreatorStd;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.entity.persistence.mvcc.BaseProjectionSupportMvcc.Object2ObjectHashMapCreator;
import cc.alcina.framework.entity.persistence.mvcc.BaseProjectionSupportMvcc.TreeMapCreatorNonTransactional;
import cc.alcina.framework.gwt.client.dirndl.model.TreePath;
import cc.alcina.framework.servlet.domain.view.LiveTree.GeneratorContext;
import cc.alcina.framework.servlet.domain.view.LiveTree.LiveNode;

/*
 * bridges projections and generators - for an object x which maps to y1, y2, y3
 * - ensure the requisite nodes via path traversal and generation
 */
public class GeneratorProjection<E> {
	Class<E> clazz;

	Projection projection;

	List<Function<E, ?>> mappings = new ArrayList<>();

	List<LiveTree.NodeGenerator> generators = new ArrayList<>();

	List<Class<?>> types = new ArrayList<>();

	public GeneratorProjection(Class<E> clazz) {
		this.clazz = clazz;
	}

	public <G extends GeneratorProjection<E>, T> G addMapping(
			Function<E, T> mapping, LiveTree.NodeGenerator generator,
			Class<T> type) {
		mappings.add(mapping);
		generators.add(generator);
		types.add(type);
		return (G) this;
	}

	public <G extends GeneratorProjection<E>> G init() {
		projection = new Projection();
		return (G) this;
	}

	public MultikeyMap<E> lastMap(E forObject) {
		Object[] keys = projection.project(forObject);
		Object[] nodeKeys = Arrays.copyOf(keys, size());
		return projection.asMap(nodeKeys);
	}

	public void process(GeneratorContext context, LiveNode root, E data,
			boolean add) {
		// we need the full projection each time (since livetree add/remove
		// tracking works via [sum of changes])
		try {
			Object[] projected = projection.deltaReturnProjected(data, add);
			LiveNode cursor = root;
			for (int idx = 0; idx < size(); idx++) {
				Object segment = projected[idx];
				TreePath<LiveNode> path = cursor.ensureChildPath(context,
						generators.get(idx), segment);
				context.deltaChildWithGenerator(cursor, segment,
						generators.get(idx), add);
				cursor = path.getValue();
			}
		} catch (Exception e) {
			e.printStackTrace();
			root.addExceptionChild(data, e);
		}
	}

	private int size() {
		return mappings.size();
	}

	class Projection extends BaseProjection<E> {
		public Projection() {
			super(clazz);
		}

		@Override
		public Class<? extends E> getListenedClass() {
			throw new UnsupportedOperationException();
		}

		private Class<?> getType(int i) {
			return i == size() ? clazz : GeneratorProjection.this.types.get(i);
		}

		@Override
		protected MultikeyMap<E> createLookup() {
			CollectionCreators.MapCreator[] sortingCreators = IntStream
					.range(0, getDepth())
					.mapToObj(i -> Comparable.class.isAssignableFrom(getType(i))
							? new TreeMapCreatorNonTransactional()
							: new Object2ObjectHashMapCreator())
					.collect(Collectors.toList())
					.toArray(new CollectionCreators.MapCreator[getDepth()]);
			return new BaseProjectionLookupBuilder(this)
					.withMapCreators(sortingCreators)
					.withDelegateMapCreator(new BplDelegateMapCreatorStd())
					.createMultikeyMap();
		}

		@Override
		protected int getDepth() {
			return size() + 1;
		}

		@Override
		protected Object[] project(E t) {
			Preconditions.checkNotNull(t);
			Object[] array = new Object[mappings.size() + 2];
			for (int idx = 0; idx < mappings.size(); idx++) {
				array[idx] = mappings.get(idx).apply(t);
			}
			array[mappings.size()] = t;
			array[mappings.size() + 1] = t;
			return array;
		}
	}
}
