package cc.alcina.framework.common.client.logic.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.FromObjectKeyValueMapper;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class EntityHelper {
	public static String asDomainPoint(HasId hi) {
		if (hi == null) {
			return null;
		}
		if (hi instanceof Entity) {
			Entity entity = (Entity) hi;
			return Ax.format("%s : %s / %s",
					CommonUtils.simpleClassName(entity.getClass()),
					entity.getId(), entity.getLocalId());
		}
		return Ax.format("%s : %s ", CommonUtils.simpleClassName(hi.getClass()),
				hi.getId());
	}

	public static <T extends Entity> SortedSet<T>
			combineAndOrderById(boolean reverse, Collection<T>... collections) {
		TreeSet<T> join = new TreeSet<T>();
		for (Collection<T> collection : collections) {
			join.addAll(collection);
		}
		return reverse ? join.descendingSet() : join;
	}

	public static int compare(Entity o1, Entity o2) {
		int i = o1.getClass().getName().compareTo(o2.getClass().getName());
		if (i != 0) {
			return i;
		}
		i = CommonUtils.compareLongs(o1.getId(), o2.getId());
		if (i != 0) {
			return i;
		}
		i = CommonUtils.compareLongs(o1.getLocalId(), o2.getLocalId());
		if (i != 0) {
			return i;
		}
		return CommonUtils.compareInts(o1.hashCode(), o2.hashCode());
	}

	public static int compareLocalsHigh(Entity o1, Entity o2) {
		int i = o1.getClass().getName().compareTo(o2.getClass().getName());
		if (i != 0) {
			return i;
		}
		i = CommonUtils.compareLongs(o1.getLocalId(), o2.getLocalId());
		if (i != 0) {
			return i;
		}
		i = CommonUtils.compareLongs(o1.getId(), o2.getId());
		if (i != 0) {
			return i;
		}
		return CommonUtils.compareInts(o1.hashCode(), o2.hashCode());
	}

	public static int compareNoLocals(Entity o1, Entity o2) {
		int i = o1.getClass().getName().compareTo(o2.getClass().getName());
		if (i != 0) {
			return i;
		}
		return CommonUtils.compareLongs(o1.getId(), o2.getId());
	}

	public static boolean equals(Entity o1, Entity o2) {
		if (o1 == null) {
			return o2 == null;
		}
		if (o2 == null) {
			return false;
		}
		if (o1.entityClass() != o2.entityClass()) {
			return false;
		}
		if (o1.getId() == 0 && o1.getLocalId() == 0) {
			return o1 == o2;
		}
		if (o1.getId() != 0 || o2.getId() != 0) {
			return o1.getId() == o2.getId();
		}
		return o2.getLocalId() == o1.getLocalId();
	}

	public static <T extends HasId> T getById(Collection<T> values, long id) {
		for (T value : values) {
			if (value.getId() == id) {
				return value;
			}
		}
		return null;
	}

	public static Long getIdOrNull(HasId hi) {
		return hi == null ? null : hi.getId();
	}

	public static long getIdOrZero(HasId hi) {
		return hi == null ? 0 : hi.getId();
	}

	public static long getIdOrZero(Optional<? extends Entity> o_entity) {
		return o_entity.isPresent() ? o_entity.get().getId() : 0;
	}

	public static Predicate<Entity> idFilter(String value) {
		if (Ax.isBlank(value)) {
			return entity -> entity != null;
		}
		Set<Long> longs = TransformManager.idListToLongSet(value);
		return entity -> entity != null && longs.contains(entity.getId());
	}

	public static String strGetIdOrZero(HasId hasId) {
		return String.valueOf(getIdOrZero(hasId));
	}

	public static String toEntityString(Entity entity) {
		if (entity == null) {
			return null;
		} else {
			return Ax.format("%s: %s", entity.getClass().getSimpleName(),
					entity.getId());
		}
	}

	public static <T extends Entity> Map<Long, T>
			toIdMap(Collection<T> entities) {
		return (Map<Long, T>) CollectionFilters
				.map((Collection<Entity>) entities, new EntityToIdMapper());
	}

	public static <E extends Entity> Collector<E, ?, Set<Long>> toIdSet() {
		return new ToIdSetCollector<>();
	}

	public static Set<Long> toIdSet(Collection<? extends Entity> entities) {
		return toIdSet(entities, new LinkedHashSet<Long>());
	}

	public static Set<Long> toIdSet(Collection<? extends Entity> entities,
			Set<Long> set) {
		for (Entity entity : entities) {
			set.add(entity.getId());
		}
		return set;
	}

	public static String toIdString(Collection<? extends Entity> entities) {
		StringBuffer sb = new StringBuffer();
		for (Entity entity : entities) {
			if (sb.length() != 0) {
				sb.append(",");
			}
			sb.append(entity.getId());
		}
		return sb.toString();
	}

	public static class EntityToIdMapper
			extends FromObjectKeyValueMapper<Long, Entity> {
		@Override
		public Long getKey(Entity o) {
			return o.getId();
		}
	}

	private static class ToIdSetCollector<E extends Entity>
			implements java.util.stream.Collector<E, Set<Long>, Set<Long>> {
		public ToIdSetCollector() {
		}

		@Override
		public BiConsumer<Set<Long>, E> accumulator() {
			return (set, t) -> set.add(t.getId());
		}

		@Override
		public Set<java.util.stream.Collector.Characteristics>
				characteristics() {
			return EnumSet.of(Characteristics.IDENTITY_FINISH);
		}

		@Override
		public BinaryOperator<Set<Long>> combiner() {
			return (left, right) -> {
				left.addAll(right);
				return left;
			};
		}

		@Override
		public Function<Set<Long>, Set<Long>> finisher() {
			return null;
		}

		@Override
		public Supplier<Set<Long>> supplier() {
			return () -> new LinkedHashSet<>();
		}
	}
}