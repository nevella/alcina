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

import java.util.stream.Collectors;
import java.util.Collections;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * Represents a set of unique objects.
 * <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Set.html">[Sun
 * docs]</a>
 *
 * @param <E>
 *            element type.
 */
@JsType
public interface Set<E> extends Collection<E> {
	@JsIgnore
	@Override
	default Spliterator<E> spliterator() {
		return Spliterators.spliterator(this, Spliterator.DISTINCT);
	}

	/*
	 * non-performant, but for JDK compatibility only
	 */
	static <E> Set<E> of(E... elements) {
		List<E> list = Arrays.asList(elements);
		return toSet(list);
	}

	private static <E> Set<E> toSet(List<E> list) {
		return Collections
				.unmodifiableSet(list.stream().collect(Collectors.toSet()));
	}
}
