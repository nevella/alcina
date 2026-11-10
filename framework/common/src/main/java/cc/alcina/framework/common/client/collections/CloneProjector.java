package cc.alcina.framework.common.client.collections;

import java.util.function.Function;

class CloneProjector<T extends PublicCloneable> implements Function<T, T> {
	@Override
	public T apply(T original) {
		return (T) ((PublicCloneable) original).clone();
	}
}
