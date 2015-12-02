package cc.alcina.framework.entity.util;

import java.util.List;
import java.util.function.Function;

public interface IToCsvRow<T> extends Function<T, List<String>> {
	List<String> headers();
}