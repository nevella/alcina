package cc.alcina.framework.entity.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import cc.alcina.framework.common.client.domain.search.ListCollector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = ListCollector.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
public class ListCollectorJvm extends ListCollector {
	static final Set<Collector.Characteristics> CHARACTERLESS = Collections
			.unmodifiableSet(EnumSet.noneOf(Collector.Characteristics.class));

	@Override
	public <T> Collector<T, ?, List<T>> toList() {
		return new CollectorImpl<>((Supplier<SpinyBuffer<T>>) SpinyBuffer::new,
				SpinyBuffer::add, (left, right) -> {
					left.addAll(right);
					return left;
				}, SpinyBuffer::toArrayList, CHARACTERLESS);
	}

	/**
	 * Simple implementation class for {@code Collector}.
	 *
	 * @param <T>
	 *            the type of elements to be collected
	 * @param <R>
	 *            the type of the result
	 */
	static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
		private final Supplier<A> supplier;

		private final BiConsumer<A, T> accumulator;

		private final BinaryOperator<A> combiner;

		private final Function<A, R> finisher;

		private final Set<Characteristics> characteristics;

		CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator,
				BinaryOperator<A> combiner, Function<A, R> finisher,
				Set<Characteristics> characteristics) {
			this.supplier = supplier;
			this.accumulator = accumulator;
			this.combiner = combiner;
			this.finisher = finisher;
			this.characteristics = characteristics;
		}

		@Override
		public BiConsumer<A, T> accumulator() {
			return accumulator;
		}

		@Override
		public Set<Characteristics> characteristics() {
			return characteristics;
		}

		@Override
		public BinaryOperator<A> combiner() {
			return combiner;
		}

		@Override
		public Function<A, R> finisher() {
			return finisher;
		}

		@Override
		public Supplier<A> supplier() {
			return supplier;
		}
	}

	static class SpinyBuffer<T> {
		ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

		ConcurrentLinkedQueue<SpinyBuffer<T>> buffers = new ConcurrentLinkedQueue<>();

		boolean addedOther = false;

		private AtomicInteger elementSize = new AtomicInteger(0);

		int size = elementSize.get();

		public SpinyBuffer() {
		}

		public void add(T t) {
			elementSize.incrementAndGet();
			queue.add(t);
		}

		public void addAll(SpinyBuffer<T> other) {
			addedOther = true;
			buffers.add(other);
		}

		private void addToList(List<T> result) {
			result.addAll(queue);
			for (SpinyBuffer<T> buffer : buffers) {
				buffer.addToList(result);
			}
		}

		int size() {
			return elementSize.get()
					+ buffers.stream().mapToInt(SpinyBuffer::size).sum();
		}

		List<T> toArrayList() {
			List<T> result = new ArrayList<>(size());
			addToList(result);
			return result;
		}
	}
}
