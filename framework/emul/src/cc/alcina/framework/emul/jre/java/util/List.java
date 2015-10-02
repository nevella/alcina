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

import java.util.Arrays;
import java.util.Comparator;
import java.util.ListIterator;

/**
 * Represents a sequence of objects. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/List.html">[Sun docs]</a>
 * 
 * @param <E> element type
 */
public interface List<E> extends Collection<E> {

  boolean add(E o);

  void add(int index, E element);

  boolean addAll(Collection<? extends E> c);

  boolean addAll(int index, Collection<? extends E> c);

  void clear();

  boolean contains(Object o);

  boolean containsAll(Collection<?> c);

  boolean equals(Object o);

  E get(int index);

  int hashCode();

  int indexOf(Object o);

  boolean isEmpty();

  Iterator<E> iterator();

  int lastIndexOf(Object o);

  ListIterator<E> listIterator();

  ListIterator<E> listIterator(int from);

  E remove(int index);

  boolean remove(Object o);

  boolean removeAll(Collection<?> c);

  boolean retainAll(Collection<?> c);

  E set(int index, E element);

  int size();

  List<E> subList(int fromIndex, int toIndex);

  Object[] toArray();

  <T> T[] toArray(T[] array);
  @SuppressWarnings({"unchecked", "rawtypes"})
  default void sort(Comparator<? super E> c) {
      Object[] a = this.toArray();
      Arrays.sort(a, (Comparator) c);
      ListIterator<E> i = this.listIterator();
      for (Object e : a) {
          i.next();
          i.set((E) e);
      }
  }
}
