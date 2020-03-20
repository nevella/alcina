/*
 * Copyright 2007 Google Inc.
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

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import java.util.Comparator.NaturalOrderComparator;

/**
 * An interface used a basis for implementing custom ordering. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Comparator.html">[Sun
 * docs]</a>
 * 
 * @param <T>
 *            the type to be compared.
 */
public interface Comparator<T> {
	int compare(T a, T b);

	boolean equals(Object other);

	public static <T, U extends Comparable<? super U>> Comparator<T> comparing(
			Function<? super T, ? extends U> keyExtractor) {
		Objects.requireNonNull(keyExtractor);
		return (Comparator<T> & Serializable) (c1, c2) -> keyExtractor
				.apply(c1).compareTo(keyExtractor.apply(c2));
	}

	default <U extends Comparable<? super U>> Comparator<T> thenComparing(
			Function<? super T, ? extends U> keyExtractor) {
		return thenComparing(comparing(keyExtractor));
	}

	default Comparator<T> thenComparing(Comparator<? super T> other) {
		Objects.requireNonNull(other);
		return (Comparator<T> & Serializable) (c1, c2) -> {
			int res = compare(c1, c2);
			return (res != 0) ? res : other.compare(c1, c2);
		};
	}
	default Comparator<T> reversed() {
        return Collections.reverseOrder(this);
    }
	public static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
        return Collections.reverseOrder();
    }
	enum NaturalOrderComparator implements Comparator<Comparable<Object>> {
        INSTANCE;

        @Override
        public int compare(Comparable<Object> c1, Comparable<Object> c2) {
            return c1.compareTo(c2);
        }

        @Override
        public Comparator<Comparable<Object>> reversed() {
            return Comparator.reverseOrder();
        }
    }
	
    public static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
        return (Comparator<T>) NaturalOrderComparator.INSTANCE;
    }
}
