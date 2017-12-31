package cc.alcina.framework.entity.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface IToCsvRow<T> extends Function<T, List<String>> {
	default List<ArrayList<String>> doConvert(List<T> objects,
			boolean withTotals) {
		List list = (List) objects.stream().map(r -> apply(r))
				.collect(Collectors.toList());
		if (withTotals) {
			doTotal(objects, list);
		}
		return list;
	}

	default void doTotal(List<T> objects, List list) {
	}

	List<String> headers();

	default void setCustom(Object custom) {
	}

	default String suggestFileName(String prefix) {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss");
		return String.format("%s-%s", prefix, df.format(new Date()));
	}
}