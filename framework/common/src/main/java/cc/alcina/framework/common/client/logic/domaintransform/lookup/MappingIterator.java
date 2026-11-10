package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Iterator;
import java.util.function.Function;

public class MappingIterator<I, O> implements Iterator<O> {
	private Iterator<I> source;

	private Function<I, O> mapping;

	public MappingIterator(Iterator<I> source, Function<I, O> mapping) {
		this.source = source;
		this.mapping = mapping;
	}

	@Override
	public boolean hasNext() {
		return source.hasNext();
	}

	@Override
	public O next() {
		return mapping.apply(source.next());
	}
}
