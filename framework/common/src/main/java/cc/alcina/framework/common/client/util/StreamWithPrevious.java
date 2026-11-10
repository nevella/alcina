package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.function.BiConsumer;

public class StreamWithPrevious<T> {
	public static <T> StreamWithPrevious<T> of(Collection<T> collection) {
		return new StreamWithPrevious<>(collection);
	}

	Collection<T> collection;

	StreamWithPrevious(Collection<T> collection) {
		this.collection = collection;
	}

	public void forEach(BiConsumer<T, T> previousCurrentConsumer) {
		Ref<T> previous = Ref.of(null);
		collection.forEach(e -> {
			previousCurrentConsumer.accept(previous.get(), e);
			previous.set(e);
		});
	}
}
