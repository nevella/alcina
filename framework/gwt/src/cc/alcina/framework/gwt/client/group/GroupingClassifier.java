package cc.alcina.framework.gwt.client.group;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.search.grouping.GroupedResult.GroupKey;
import cc.alcina.framework.gwt.client.cell.ColumnsBuilder;

public interface GroupingClassifier<V, T extends Comparable>
		extends Function<V, T> {
	public static <V, E extends Enum> GroupingClassifier<V, E> enumClassifier(
			Function<V, E> enumClassifer,
			Function<E, ? extends GroupKey> groupKeyMapper,
			Class<E> enumClass) {
		return new GroupingClassifier<V, E>() {
			@Override
			public List<E> allKeys(List<V> values) {
				return Arrays.asList(enumClass.getEnumConstants());
			}

			@Override
			public E classify(V v) {
				return enumClassifer.apply(v);
			}

			@Override
			public GroupKey groupKey(E t) {
				return groupKeyMapper.apply(t);
			}
		};
	}

	public static <V> GroupingClassifier<V, String>
			singleValueClassifier(String columnName) {
		return new GroupingClassifier<V, String>() {
			@Override
			public List<String> allKeys(List<V> values) {
				return Arrays.asList(columnName);
			}

			@Override
			public String classify(V v) {
				return columnName;
			}
		};
	}

	public T classify(V v);

	default List<T> allKeys(List<V> values) {
		return values.stream().map(this::classify).distinct().sorted()
				.collect(Collectors.toList());
	}

	@Override
	default T apply(V v) {
		return classify(v);
	}

	default void buildColumn(
			ColumnsBuilder<GroupingMapper.GroupingMapperRow> builder) {
		// not needed for row builder - and unimplemented (can override) for
		// columnbuilder
		throw new UnsupportedOperationException();
	}

	default GroupKey groupKey(T t) {
		return null;
	}
}