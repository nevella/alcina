package cc.alcina.framework.common.client.collections;

import com.totsp.gwittir.client.beans.Converter;

class CloneProjector<T extends PublicCloneable> implements Converter<T, T> {
	@Override
	public T convert(T original) {
		return (T) ((PublicCloneable) original).clone();
	}
}
