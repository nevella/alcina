package cc.alcina.framework.gwt.client.cell;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface GroupingClassifier<V, T extends Comparable>
		extends Function<V, T> {
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
}