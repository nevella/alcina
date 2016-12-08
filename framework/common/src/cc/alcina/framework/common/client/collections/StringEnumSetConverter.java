package cc.alcina.framework.common.client.collections;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class StringEnumSetConverter<E extends Enum>
		extends BidiConverter<String, Set<E>> {
	private Class<E> clazz;

	public StringEnumSetConverter(Class<E> clazz) {
		this.clazz = clazz;
	}

	@Override
	public Set<E> leftToRight(String a) {
		if (a == null) {
			return null;
		}
		return (Set<E>)Arrays.stream(a.split(",")).map(e -> (E) Enum.valueOf(clazz, e))
				.collect(Collectors.toSet());
	}

	@Override
	public String rightToLeft(Set<E> b) {
		if (b == null) {
			return null;
		}
		return b.stream().map(Object::toString)
				.collect(Collectors.joining(","));
	}
}
