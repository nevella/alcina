/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package java.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static javaemul.internal.InternalPreconditions.checkCriticalElement;
import static javaemul.internal.InternalPreconditions.checkCriticalNotNull;
import static javaemul.internal.InternalPreconditions.checkNotNull;

/**
 * See
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html">
 * the official Java API doc</a> for details.
 *
 * @param <T>
 *            type of the wrapped reference
 */
public final class Optional<T> {
	@SuppressWarnings("unchecked")
	public static <T> Optional<T> empty() {
		return (Optional<T>) EMPTY;
	}

	public static <T> Optional<T> of(T value) {
		return new Optional<>(checkCriticalNotNull(value));
	}

	public static <T> Optional<T> ofNullable(T value) {
		return value == null ? empty() : of(value);
	}

	private static final Optional<?> EMPTY = new Optional<>(null);

	private final T ref;

	private Optional(T ref) {
		this.ref = ref;
	}

	public boolean isPresent() {
		return ref != null;
	}

	public T get() {
		checkCriticalElement(isPresent());
		return ref;
	}

	public void ifPresent(Consumer<? super T> consumer) {
		if (isPresent()) {
			consumer.accept(ref);
		}
	}

	public Optional<T> filter(Predicate<? super T> predicate) {
		checkNotNull(predicate);
		if (!isPresent() || predicate.test(ref)) {
			return this;
		}
		return empty();
	}

	public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
		checkNotNull(mapper);
		if (isPresent()) {
			return ofNullable(mapper.apply(ref));
		}
		return empty();
	}

	public <U> Optional<U> flatMap(Function<? super T, Optional<U>> mapper) {
		checkNotNull(mapper);
		if (isPresent()) {
			return checkNotNull(mapper.apply(ref));
		}
		return empty();
	}

	public T orElse(T other) {
		return isPresent() ? ref : other;
	}

	public T orElseGet(Supplier<? extends T> other) {
		return isPresent() ? ref : other.get();
	}

	public <X extends Throwable> T
			orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		if (isPresent()) {
			return ref;
		}
		throw exceptionSupplier.get();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Optional)) {
			return false;
		}
		Optional<?> other = (Optional<?>) obj;
		return Objects.equals(ref, other.ref);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(ref);
	}

	@Override
	public String toString() {
		return isPresent() ? "Optional.of(" + String.valueOf(ref) + ")"
				: "Optional.empty()";
	}

	/**
	 * If a value is not present, returns {@code true}, otherwise {@code false}.
	 *
	 * @return {@code true} if a value is not present, otherwise {@code false}
	 * @since 11
	 */
	public boolean isEmpty() {
		return ref == null;
	}

	/**
	 * If a value is present, returns a sequential {@link Stream} containing
	 * only that value, otherwise returns an empty {@code Stream}.
	 *
	 * @apiNote This method can be used to transform a {@code Stream} of
	 *          optional elements to a {@code Stream} of present value elements:
	 * 
	 *          <pre>
	 * {@code
	 *     Stream<Optional<T>> os = ..
	 *     Stream<T> s = os.flatMap(Optional::stream)
	 * }
	 * </pre>
	 *
	 * @return the optional value as a {@code Stream}
	 * @since 9
	 */
	public Stream<T> stream() {
		if (!isPresent()) {
			return Stream.empty();
		} else {
			return Stream.of(ref);
		}
	}

	/**
	 * If a value is present, returns the value, otherwise throws
	 * {@code NoSuchElementException}.
	 *
	 * @return the non-{@code null} value described by this {@code Optional}
	 * @throws NoSuchElementException
	 *             if no value is present
	 * @since 10
	 */
	public T orElseThrow() {
		if (ref == null) {
			throw new NoSuchElementException("No value present");
		}
		return ref;
	}
}
