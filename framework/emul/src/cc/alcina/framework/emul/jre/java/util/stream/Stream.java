package java.util.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;

public interface Stream<T> {
	Iterator<T> iterator();

	default <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
		Objects.requireNonNull(mapper);
		List<R> result = new ArrayList<R>();
		for (Iterator<T> itr = iterator(); itr.hasNext();) {
			result.add(mapper.apply(itr.next()));
		}
		return (Stream<R>) new CollectionStream<R>(result);
	}

	default <R, A> R collect(Collector<? super T, A, R> collector){
		return collector.collect((Stream)this);
	}
}