package cc.alcina.framework.common.client.collections;

import java.util.List;
import java.util.stream.Collectors;

public interface PublicCloneable<T> {
	static <T extends PublicCloneable<T>> List<T> clone(List<T> source) {
		return source.stream().map(o -> new CloneProjector<>().apply(o))
				.collect(Collectors.toList());
	}

	public T clone();
}
