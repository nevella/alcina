package java.util.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;
import java.util.stream.Stream;

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
	
	default <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper){
		Objects.requireNonNull(mapper);
		List<R> result = new ArrayList<R>();
		for (Iterator<T> itr = iterator(); itr.hasNext();) {
			Stream<? extends R> stream = mapper.apply(itr.next());
			stream.forEach(result::add);
		}
		return (Stream<R>) new CollectionStream<R>(result);
	}

	default <R, A> R collect(Collector<? super T, A, R> collector) {
		return collector.collect((Stream) this);
	}

	default Stream<T> filter(Predicate<? super T> predicate) {
		List<T> result = new ArrayList<T>();
		for (Iterator<T> itr = iterator(); itr.hasNext();) {
			T t = itr.next();
			if (predicate.test(t)) {
				result.add(t);
			}
		}
		return (Stream<T>) new CollectionStream<T>(result);
	}

	static class LimitPredicate<T> implements Predicate<T> {
		long limit;

		public LimitPredicate(long limit) {
			this.limit = limit;
		}

		public boolean test(T t) {
			if (limit > 0) {
				limit--;
				return true;
			} else {
				return false;
			}
		}
	}

	static class SkipPredicate<T> implements Predicate<T> {
		long skip;

		public SkipPredicate(long skip) {
			this.skip = skip;
		}

		public boolean test(T t) {
			if (skip > 0) {
				skip--;
				return false;
			} else {
				return true;
			}
		}
	}

	default Stream<T> limit(long limit) {
		return filter(new LimitPredicate(limit));
	}

	default Stream<T> skip(long skip) {
		return filter(new SkipPredicate(skip));
	}

	default Stream<T> distinct() {
		Set<T> distinct = new LinkedHashSet<T>();
		for (Iterator<T> itr = iterator(); itr.hasNext();) {
			T t = itr.next();
			distinct.add(t);
		}
		return (Stream<T>) new CollectionStream<T>(distinct);
	}

	default long count() {
		long count = 0;
		for (Iterator<T> itr = iterator(); itr.hasNext();) {
			itr.next();
			count++;
		}
		return count;
	}

	default void forEach(Consumer<? super T> action) {
		for (Iterator<T> itr = iterator(); itr.hasNext();) {
			T t = itr.next();
			action.accept(t);
		}
	}

	default Stream<T> sorted(Comparator<? super T> comparator) {
		List<T> list = ((CollectionStream<T>) this).asList();
		list.sort(comparator);
		return new CollectionStream(list);
	}

	default Optional<T> max(Comparator<? super T> comparator) {
		T max = null;
		for (Iterator<T> itr = iterator(); itr.hasNext();) {
			T t = itr.next();
			if (max == null || comparator.compare(max, t) < 0) {
				max = t;
			}
		}
		return Optional.ofNullable(max);
	}

	default Stream<T> sorted() {
		List<T> list = ((CollectionStream<T>) this).asList();
		Collections.sort(list);
		return new CollectionStream(list);
	}

	public static <E> Stream<E> concat(Stream<E> stream1, Stream<E> stream2) {
		List<E> list = ((CollectionStream<E>) stream1).asList();
		list.addAll(((CollectionStream<E>) stream2).asList());
		return new CollectionStream(list);
	}

	public static <E> Stream<E> empty() {
		return new CollectionStream(new ArrayList<E>());
	}

	default Optional<T> findFirst() {
		Iterator<T> itr = iterator();
		return Optional.ofNullable(itr.hasNext() ? itr.next() : null);
	}

	default boolean anyMatch(Predicate<? super T> predicate) {
		for (Iterator<T> itr = iterator(); itr.hasNext();) {
			T t = itr.next();
			if (predicate.test(t)) {
				return true;
			}
		}
		return false;
	}
	default boolean noneMatch(Predicate<? super T> predicate) {
        return !anyMatch(predicate);
    }

	default boolean allMatch(Predicate<? super T> predicate) {
		for (Iterator<T> itr = iterator(); itr.hasNext();) {
			T t = itr.next();
			if (!predicate.test(t)) {
				return false;
			}
		}
		return true;
	}

	default Optional<T> reduce(BinaryOperator<T> accumulator) {
		T value = null;
		boolean foundAny = false;
		T result = null;
		for (Iterator<T> itr = iterator(); itr.hasNext();) {
			T t = itr.next();
			if (!foundAny) {
				foundAny = true;
				result = t;
			} else
				result = accumulator.apply(result, t);
		}
		return foundAny ? Optional.of(result) : Optional.empty();
	}
}